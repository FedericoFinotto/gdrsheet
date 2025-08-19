package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.dto.*;
import it.fin8.gdrsheet.entity.Collegamento;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.entity.ItemLabel;
import it.fin8.gdrsheet.entity.Personaggio;
import it.fin8.gdrsheet.mapper.ItemMapper;
import it.fin8.gdrsheet.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ItemMapper itemMapper;
    @Autowired
    private UtilService utilService;
    @Autowired
    private PersonaggioRepository personaggioRepository;
    @Autowired
    private ItemLabelRepository itemLabelRepository;
    @Autowired
    EntityManager em;
    @Autowired
    private CollegamentoLabelRepository collegamentoLabelRepository;
    @Autowired
    private CollegamentoRepository collegamentoRepository;

    public Item switchItemState(Integer itemId, Integer personaggioId) {
        Item itm = itemRepository.findItemById(itemId);

        if (itm.getPersonaggio() == null) {
            Collegamento link = utilService.findRightConnectionLink(itm, personaggioId)
                    .orElseThrow(() -> new RuntimeException("Impossibile definire collegamento oggetto"));

            toggleDisabled(() -> link.getLabel(Constants.ITEM_LABEL_DISABILITATO), val -> link.setLabel(Constants.ITEM_LABEL_DISABILITATO, val));
            collegamentoRepository.save(link);

        } else {
            toggleDisabled(() -> itm.getLabel(Constants.ITEM_LABEL_DISABILITATO), val -> itm.setLabel(Constants.ITEM_LABEL_DISABILITATO, val));
            itemRepository.save(itm);
        }

        return itm;
    }

    private static void toggleDisabled(Supplier<String> getter, Consumer<String> setter) {
        String v = getter.get();
        v = (v == null) ? "" : v.trim();
        setter.accept(v.isEmpty() || v.equals(Constants.ITEM_LABEL_DISABILITATO_VALORE_FALSE) ? Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE : Constants.ITEM_LABEL_DISABILITATO_VALORE_FALSE);
    }

    public List<SpellBookIncantesimoDTO> getListIncantesimiByClasseAndLevel(Integer idClasse, Integer livello) {
        Item classe = itemRepository.findItemById(idClasse);
        String spellClasse = utilService.getItemLabel(classe, Constants.ITEM_LABEL_LISTA_INCANTESIMI);

        List<ItemLivelloDTO> incantesimi = itemRepository.findIncantesimiByLabelAndMaxLivello(spellClasse, livello);
        return incantesimi.stream().map(x -> itemMapper.toIncantesimoDTO(classe, x)).toList();
    }

    public void updatePreparedForCharacterAndLevel(UpdatePreparedRequest request) {
        Personaggio personaggio = personaggioRepository.findPersonaggioById(request.getIdPersonaggio());
        Item preparedSpell = personaggio.getItems().stream().filter(x -> x.getNome().equals(Constants.ITEM_INCANTESIMI_PREPARATI)).findFirst().orElse(null);
        if (preparedSpell == null) return;

        List<Collegamento> spellPresenti = preparedSpell.getChild().stream()
                .filter(child -> {
                    String spellList = utilService.getCollegamentoLabel(child, Constants.COLLEGAMENTO_LABEL_LISTA_INCANTESIMI);
                    String livello = utilService.getCollegamentoLabel(child, Constants.COLLEGAMENTO_LABEL_LIVELLO);
                    return spellList.equals(request.getSpellList()) &&
                            request.getLivello().equals(Integer.parseInt(livello));
                })
                .toList();

        List<Collegamento> spellDaPreparare = request.getPrepared().entrySet().stream()
                .filter(sp -> sp.getValue() != 0)
                .map(x -> itemMapper.toNewCollegamentoIncantesimo(x, preparedSpell.getId(), request.getLivello().toString(), request.getSpellList(), em))
                .toList();

        List<Collegamento> spellDaEliminare = spellPresenti.stream().filter(x -> !x.contenutoIn(spellDaPreparare)).toList();
        List<Collegamento> spellDaAggiungere = spellDaPreparare.stream().filter(x -> !x.contenutoIn(spellPresenti)).toList();
        List<Collegamento> spellDaAggiornare = spellPresenti.stream().filter(x -> x.contenutoIn(spellDaPreparare))
                .peek(x -> spellDaPreparare.stream().filter(x::stessoCollegamento).findFirst().ifPresent(y -> {
                    x.setLabel(Constants.COLLEGAMENTO_LABEL_N_PREPARATI, y.getLabel(Constants.COLLEGAMENTO_LABEL_N_PREPARATI));
                    x.setLabel(Constants.COLLEGAMENTO_LABEL_N_USATI, y.getLabel(Constants.COLLEGAMENTO_LABEL_N_USATI));
                })).toList();

        collegamentoRepository.deleteAll(spellDaEliminare);
        collegamentoRepository.saveAll(spellDaAggiungere);
        collegamentoRepository.saveAll(spellDaAggiornare);

    }

    public void updateSpellUsage(UpdateSpellUsageRequest request) {
        Personaggio personaggio = personaggioRepository.findPersonaggioById(request.getIdPersonaggio());
        if (personaggio == null) throw new RuntimeException("Personaggio non trovato");
        Item preparedSpell = personaggio.getItems().stream().filter(x -> x.getNome().equals(Constants.ITEM_INCANTESIMI_PREPARATI)).findFirst().orElse(null);
        if (preparedSpell == null) throw new RuntimeException("Incantesimi non presenti");
        Collegamento spell = preparedSpell.getChild().stream().filter(x -> Objects.equals(x.getItemTarget().getId(), request.getSpellId())).findFirst().orElse(null);
        if (spell == null) throw new RuntimeException("Incantesimo non presente");

        spell.setLabel(Constants.COLLEGAMENTO_LABEL_N_USATI, request.getNewUsage().toString());
        collegamentoRepository.save(spell);
    }

    @Transactional
    public Item updateSpell(Integer id, UpdateSpellRequest request) {
        Item itm = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Item non trovato"));

        // --- aggiorna campi base ---
        if (request.getNome() != null) itm.setNome(request.getNome());
        if (request.getDescrizione() != null) itm.setDescrizione(request.getDescrizione());

        // --- SVUOTA labels IN PLACE (no setLabels(...)) ---
        if (itm.getLabels() != null) {
            Iterator<ItemLabel> it = itm.getLabels().iterator();
            while (it.hasNext()) {
                ItemLabel l = it.next();
                it.remove();         // rimuove dalla collection
                l.setItem(null);     // rompe la back-ref -> orphanRemoval
            }
        }

        // --- LABELS "singole" (usa setLabel che gestisce update/crea) ---
        putSingleLabel(itm, "TEMPO_SP", request.getTempo());
        putSingleLabel(itm, "RANGE_SP", request.getRange());
        putSingleLabel(itm, "DURATA_SP", request.getDurata());

        // TS: normalizza "None" -> "Nessuno" come fallback
        String ts = request.getTs();
        if (ts != null && ts.trim().equalsIgnoreCase("None")) ts = "Nessuno";
        putSingleLabel(itm, "TS_SP", ts);

        // --- COMPONENTI (multi-riga: una label per valore) ---
        if (request.getComponenti() != null) {
            for (String comp : request.getComponenti()) {
                addLabelRow(itm, "COMP_SP", comp);
            }
        }

        // --- CLASSI / DOMINI SP_* (multi-riga) ---
        if (request.getClassi() != null) {
            for (ClassLevelDTO c : request.getClassi()) {
                if (c.getClasse() != null && c.getLivello() != null) {
                    addLabelRow(itm, c.getClasse(), String.valueOf(c.getLivello()));
                }
            }
        }

        // --- PATCH dirette (singole chiavi) ---
        if (request.getLabelsPatch() != null) {
            request.getLabelsPatch().forEach((k, v) -> putSingleLabel(itm, k, v));
        }

        return itemRepository.save(itm);
    }

    /**
     * Crea/aggiorna una label "singola" (chiavi univoche). Se value null/blank -> rimuove.
     */
    private void putSingleLabel(Item item, String key, String value) {
        if (key == null) return;
        if (value == null || value.trim().isEmpty()) {
            item.removeLabel(key); // tuo helper nell'entità
        } else {
            item.setLabel(key, value); // tuo helper: update se esiste, altrimenti crea
        }
    }

    /**
     * Aggiunge una riga label (per chiavi multi-valore: COMP_SP, SP_*). Ignora null/blank.
     */
    private void addLabelRow(Item item, String key, String value) {
        if (key == null || value == null || value.trim().isEmpty()) return;
        ItemLabel nl = new ItemLabel();
        nl.setItem(item);      // back-ref
        nl.setLabel(key);
        nl.setValore(value.trim());
        item.getLabels().add(nl); // orphanRemoval + cascade ALL farà il resto
    }


}

