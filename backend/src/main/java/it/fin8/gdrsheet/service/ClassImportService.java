package it.fin8.gdrsheet.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.def.TipoModificatore;
import it.fin8.gdrsheet.dto.ClassImportResultDTO;
import it.fin8.gdrsheet.dto.UpdateItemRequest;
import it.fin8.gdrsheet.entity.Avanzamento;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.entity.Stat;
import it.fin8.gdrsheet.repository.AvanzamentoRepository;
import it.fin8.gdrsheet.repository.ItemRepository;
import it.fin8.gdrsheet.repository.StatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Import bulk di Classi (e Classi di Prestigio) da un file JSONL prodotto da
 * scripts/dndtools-scraper/scrape_classes.py (classes.jsonl, o una sua versione
 * pre-filtrata sulle sole voci importabili — vedi "python scrape_classes.py --import-jsonl").
 * <p>
 * Per ogni classe crea: l'item CLASSE (con label MANUALE_SP/LINK e i nomi inglesi delle
 * class skills come label CLASS_SKILLS_EN, da mappare a mano su ABCLASSE in un secondo
 * momento), un item AVANZAMENTO per ciascun livello presente in tabella (con i modificatori
 * BAB/TMP/RFL/VLT e, per gli incantatori, la label SP_SLOT), e un item ABILITA per ciascuna
 * capacità di classe individuata, agganciata alla CLASSE tramite avanzamento al livello (o ai
 * livelli, se ricorre più volte) in cui viene ottenuta.
 * <p>
 * Le classi già presenti nel compendio (stesso nome, tipo CLASSE) vengono saltate, per essere
 * idempotente e non duplicare classi eventualmente già create a mano nel database.
 */
@Service
public class ClassImportService {

    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private AvanzamentoRepository avanzamentoRepository;
    @Autowired
    private StatRepository statRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public ClassImportResultDTO importClassesJsonl(MultipartFile file) {
        int totalRows = 0, classiCreate = 0, classiSaltate = 0, livelliCreati = 0, abilitaCreate = 0;
        List<ClassImportResultDTO.ImportRowErrorDTO> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                totalRows++;
                String nome = null;
                try {
                    Map<String, Object> record = objectMapper.readValue(line, new TypeReference<Map<String, Object>>() {});
                    nome = str(record.get("name"));
                    if (nome == null || nome.isBlank()) throw new RuntimeException("Nome mancante");

                    if (itemRepository.existsByNomeIgnoreCaseAndTipoAndPersonaggioIsNull(nome, TipoItem.CLASSE)) {
                        classiSaltate++;
                        continue;
                    }

                    Item classeItem = createClasseItem(record, nome);
                    classiCreate++;

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> advRows = (List<Map<String, Object>>) record.getOrDefault("advancementRows", List.of());
                    livelliCreati += createLevelItemsAndLinks(classeItem, nome, advRows, record);

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> features = (List<Map<String, Object>>) record.getOrDefault("classFeatures", List.of());
                    abilitaCreate += createAbilityItemsAndLinks(classeItem, record, features, advRows);
                } catch (Exception e) {
                    errors.add(new ClassImportResultDTO.ImportRowErrorDTO(totalRows, nome, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Errore lettura file: " + e.getMessage(), e);
        }

        return new ClassImportResultDTO(totalRows, classiCreate, classiSaltate, livelliCreati, abilitaCreate, errors);
    }

    private Item createClasseItem(Map<String, Object> record, String nome) {
        List<UpdateItemRequest.LabelRowDTO> labels = new ArrayList<>();
        addLabel(labels, Constants.ITEM_LABEL_MANUALE, str(record.get("rulebookListing")));
        addLabel(labels, Constants.ITEM_LABEL_LINK, str(record.get("link")));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> classSkills = (List<Map<String, Object>>) record.getOrDefault("classSkills", List.of());
        for (Map<String, Object> skill : classSkills) {
            addLabel(labels, "CLASS_SKILLS_EN", str(skill.get("Skill name")));
        }

        String descrizione = str(record.get("classFeaturesIntro"));

        UpdateItemRequest req = UpdateItemRequest.builder()
                .nome(nome)
                .tipo(TipoItem.CLASSE)
                .descrizione(descrizione)
                .labels(labels)
                .build();
        return itemService.createItem(req);
    }

    /** Un item AVANZAMENTO per livello, con BAB/TS e (per gli incantatori) SP_SLOT; ritorna quanti livelli ha creato. */
    private int createLevelItemsAndLinks(Item classeItem, String nomeClasse, List<Map<String, Object>> advRows, Map<String, Object> record) {
        int count = 0;
        List<String> spellCols = spellSlotColumns((List<String>) record.getOrDefault("advancementHeaders", List.of()));

        for (Map<String, Object> row : advRows) {
            Integer livello = parseLevel(str(row.get("Level")));
            if (livello == null) continue;

            List<UpdateItemRequest.LabelRowDTO> labels = new ArrayList<>();
            if (!spellCols.isEmpty()) {
                String slotCsv = spellCols.stream().map(c -> {
                    String v = str(row.get(c));
                    return toSlotToken(v);
                }).reduce((a, b) -> a + "," + b).orElse("");
                if (!slotCsv.isBlank()) addLabel(labels, Constants.ITEM_LABEL_SPELL_SLOT, slotCsv);
            }

            List<UpdateItemRequest.ModificatoreRowDTO> mods = new ArrayList<>();
            addModIfPresent(mods, "BAB", row.get("BAB"));
            addModIfPresent(mods, "TMP", row.get("Fort"));
            addModIfPresent(mods, "RFL", row.get("Ref"));
            addModIfPresent(mods, "VLT", row.get("Will"));

            UpdateItemRequest req = UpdateItemRequest.builder()
                    .nome(nomeClasse + " " + livello)
                    .tipo(TipoItem.AVANZAMENTO)
                    .labels(labels)
                    .modificatori(mods)
                    .build();
            Item livelloItem = itemService.createItem(req);
            linkAvanzamento(classeItem, livelloItem, livello);
            count++;
        }
        return count;
    }

    /** Un item ABILITA per capacità di classe, agganciato a ogni livello in cui compare in "Special". */
    private int createAbilityItemsAndLinks(Item classeItem, Map<String, Object> record, List<Map<String, Object>> features, List<Map<String, Object>> advRows) {
        Map<String, String> descByName = new LinkedHashMap<>();
        for (Map<String, Object> f : features) {
            descByName.put(str(f.get("name")), str(f.get("description")));
        }

        Map<String, Item> itemsByName = new LinkedHashMap<>();
        Map<String, List<Integer>> levelsByName = new LinkedHashMap<>();

        for (Map<String, Object> row : advRows) {
            Integer livello = parseLevel(str(row.get("Level")));
            if (livello == null) continue;
            @SuppressWarnings("unchecked")
            List<String> matched = (List<String>) row.getOrDefault("SpecialMatched", List.of());
            for (String featName : matched) {
                if (featName == null || featName.isBlank()) continue;
                levelsByName.computeIfAbsent(featName, k -> new ArrayList<>()).add(livello);
            }
        }

        int created = 0;
        // prima le capacità agganciate a un livello, poi quelle mai citate in "Special" (livello 1 di default)
        for (Map.Entry<String, List<Integer>> entry : levelsByName.entrySet()) {
            String featName = entry.getKey();
            Item abilita = itemsByName.computeIfAbsent(featName, k -> {
                UpdateItemRequest req = UpdateItemRequest.builder()
                        .nome(featName)
                        .tipo(TipoItem.ABILITA)
                        .descrizione(descByName.get(featName))
                        .labels(labelsManualeLink(record))
                        .build();
                return itemService.createItem(req);
            });
            for (Integer lvl : entry.getValue()) {
                linkAvanzamento(classeItem, abilita, lvl);
            }
        }
        created += itemsByName.size();

        for (String featName : descByName.keySet()) {
            if (itemsByName.containsKey(featName)) continue;
            UpdateItemRequest req = UpdateItemRequest.builder()
                    .nome(featName)
                    .tipo(TipoItem.ABILITA)
                    .descrizione(descByName.get(featName))
                    .labels(labelsManualeLink(record))
                    .build();
            Item abilita = itemService.createItem(req);
            linkAvanzamento(classeItem, abilita, 1);
            created++;
        }

        return created;
    }

    private List<UpdateItemRequest.LabelRowDTO> labelsManualeLink(Map<String, Object> record) {
        List<UpdateItemRequest.LabelRowDTO> labels = new ArrayList<>();
        addLabel(labels, Constants.ITEM_LABEL_MANUALE, str(record.get("rulebookListing")));
        addLabel(labels, Constants.ITEM_LABEL_LINK, str(record.get("link")));
        return labels;
    }

    private void linkAvanzamento(Item source, Item target, int livello) {
        Avanzamento a = new Avanzamento();
        a.setItemSource(source);
        a.setItemTarget(target);
        a.setLivello(livello);
        avanzamentoRepository.save(a);
    }

    private void addModIfPresent(List<UpdateItemRequest.ModificatoreRowDTO> mods, String statId, Object rawValue) {
        String value = str(rawValue);
        if (value == null || value.isBlank()) return;
        String primo = firstToken(value);
        if (primo == null || primo.isBlank()) return;
        mods.add(new UpdateItemRequest.ModificatoreRowDTO(null, statId, TipoModificatore.VALORE, primo, null, true));
    }

    /** BAB pieno riporta gli attacchi multipli come "+11/+6/+1": il modificatore VALORE vuole solo il primo. */
    private String firstToken(String value) {
        int slash = value.indexOf('/');
        return (slash >= 0 ? value.substring(0, slash) : value).trim();
    }

    private Integer parseLevel(String levelText) {
        if (levelText == null) return null;
        String digits = levelText.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return null;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String> spellSlotColumns(List<String> headers) {
        List<String> out = new ArrayList<>();
        for (String h : headers) {
            if (h != null && h.matches("^(0th|1st|2nd|3rd|[4-9]th)$")) out.add(h);
        }
        return out;
    }

    /** "-" per il dash (nessun accesso), il numero altrimenti — stesso formato di SP_SLOT già in uso. */
    private String toSlotToken(String raw) {
        if (raw == null) return "-";
        String t = raw.trim();
        if (t.isEmpty() || t.equals("—") || t.equals("-") || t.equals("�")) return "-";
        return t;
    }

    private void addLabel(List<UpdateItemRequest.LabelRowDTO> labels, String key, String value) {
        if (value == null || value.isBlank()) return;
        labels.add(new UpdateItemRequest.LabelRowDTO(key, value));
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
