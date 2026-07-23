package it.fin8.gdrsheet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.dto.NotaDTO;
import it.fin8.gdrsheet.dto.QuestDTO;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.entity.ItemLabel;
import it.fin8.gdrsheet.entity.Party;
import it.fin8.gdrsheet.entity.Personaggio;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.repository.ItemRepository;
import it.fin8.gdrsheet.repository.PartyRepository;
import it.fin8.gdrsheet.repository.PersonaggioRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Quest: nuovo tipo di item organizzato ad albero (sotto-quest = child collegati di
 * tipo QUEST). Nessuna nuova tabella: l'ambito (personaggio/party/mondo) e lo stato di
 * completamento vivono in ItemLabel/Item.personaggio/Item.mondo, riusando i meccanismi
 * già esistenti per gli altri tipi di item. Le note (generiche, su qualunque item) hanno
 * una propria visibilità: le sotto-quest ereditano ai fini del filtro il personaggio
 * "proprietario" della quest radice del loro albero (le sotto-quest non hanno un proprio
 * ambito, sono raggiungibili solo tramite il collegamento dalla quest padre).
 */
@Service
public class QuestService {

    private final ItemRepository itemRepository;
    private final PersonaggioRepository personaggioRepository;
    private final PartyRepository partyRepository;
    private final AuthzService authzService;
    private final ObjectMapper objectMapper;

    public QuestService(ItemRepository itemRepository, PersonaggioRepository personaggioRepository,
                         PartyRepository partyRepository, AuthzService authzService, ObjectMapper objectMapper) {
        this.itemRepository = itemRepository;
        this.personaggioRepository = personaggioRepository;
        this.partyRepository = partyRepository;
        this.authzService = authzService;
        this.objectMapper = objectMapper;
    }

    /** Quest visibili a un personaggio: prima quelle del party, poi quelle del mondo, poi le proprie. */
    public List<QuestDTO> getQuestPersonaggio(Integer idPersonaggio, Utente utente) {
        Personaggio pg = personaggioRepository.findPersonaggioById(idPersonaggio);
        if (pg == null) throw new RuntimeException("Personaggio non trovato: " + idPersonaggio);

        List<QuestDTO> result = new ArrayList<>();
        if (pg.getParty() != null) {
            for (Item i : itemRepository.findQuestByPartyId(String.valueOf(pg.getParty().getId()))) {
                result.add(tagRoot(buildDTO(i, null, utente), "PARTY", null));
            }
            if (pg.getParty().getMondo() != null) {
                for (Item i : itemRepository.findQuestByMondoId(pg.getParty().getMondo().getId())) {
                    result.add(tagRoot(buildDTO(i, null, utente), "MONDO", null));
                }
            }
        }
        for (Item i : itemRepository.findQuestByPersonaggioId(idPersonaggio)) {
            result.add(tagRoot(buildDTO(i, pg, utente), "PERSONAGGIO", pg));
        }
        return result;
    }

    /**
     * Quest visibili a un party: prima quelle del party, poi quelle del mondo, poi le quest
     * personali di OGNI personaggio del party (in ordine alfabetico di personaggio) — visibili
     * qui in sola lettura: le note con visibilità OWNER restano filtrate in base a chi sta
     * effettivamente guardando.
     */
    public List<QuestDTO> getQuestParty(Integer idParty, Utente utente) {
        Party party = partyRepository.findById(idParty)
                .orElseThrow(() -> new RuntimeException("Party non trovato: " + idParty));

        List<QuestDTO> result = new ArrayList<>();
        for (Item i : itemRepository.findQuestByPartyId(String.valueOf(idParty))) {
            result.add(tagRoot(buildDTO(i, null, utente), "PARTY", null));
        }
        if (party.getMondo() != null) {
            for (Item i : itemRepository.findQuestByMondoId(party.getMondo().getId())) {
                result.add(tagRoot(buildDTO(i, null, utente), "MONDO", null));
            }
        }
        for (Personaggio membro : personaggioRepository.findAllByParty_IdOrderByNomeAsc(idParty)) {
            for (Item i : itemRepository.findQuestByPersonaggioId(membro.getId())) {
                result.add(tagRoot(buildDTO(i, membro, utente), "PERSONAGGIO", membro));
            }
        }
        return result;
    }

    private QuestDTO tagRoot(QuestDTO dto, String ambito, Personaggio personaggio) {
        dto.setAmbito(ambito);
        dto.setPersonaggioNome(personaggio != null ? personaggio.getNome() : null);
        return dto;
    }

    private QuestDTO buildDTO(Item item, Personaggio rootOwner, Utente utente) {
        List<Item> figliItem = (item.getChild() == null ? List.<it.fin8.gdrsheet.entity.Collegamento>of() : item.getChild())
                .stream()
                .map(it.fin8.gdrsheet.entity.Collegamento::getItemTarget)
                .filter(t -> TipoItem.QUEST.equals(t.getTipo()))
                .toList();
        List<QuestDTO> figliDTO = figliItem.stream().map(f -> buildDTO(f, rootOwner, utente)).toList();

        boolean completata = "1".equals(item.getLabel(Constants.ITEM_LABEL_QUEST_COMPLETATA));
        int completati;
        int totali;
        if (figliDTO.isEmpty()) {
            completati = completata ? 1 : 0;
            totali = 1;
        } else {
            completati = figliDTO.stream().mapToInt(QuestDTO::getCompletati).sum();
            totali = figliDTO.stream().mapToInt(QuestDTO::getTotali).sum();
        }

        List<NotaDTO> note = parseNote(item).stream()
                .filter(n -> authzService.canViewVisibilita(utente, rootOwner, n.getVisibilita()))
                .toList();
        List<String> inCarico = parseInCarico(item);

        return new QuestDTO(item.getId(), item.getNome(), item.getDescrizione(), completata, completati, totali, note, inCarico, figliDTO, null, null);
    }

    /** Righe ItemLabel IN_CARICO (testo libero), vuote escluse. */
    private List<String> parseInCarico(Item item) {
        if (item.getLabels() == null) return List.of();
        List<String> result = new ArrayList<>();
        for (ItemLabel l : item.getLabels()) {
            if (!Constants.ITEM_LABEL_QUEST_IN_CARICO.equals(l.getLabel())) continue;
            if (l.getValore() != null && !l.getValore().isBlank()) result.add(l.getValore().trim());
        }
        return result;
    }

    /** Ogni riga ItemLabel NOTA contiene un JSON {testo, visibilita}; righe malformate vengono ignorate. */
    private List<NotaDTO> parseNote(Item item) {
        if (item.getLabels() == null) return List.of();
        List<NotaDTO> result = new ArrayList<>();
        for (ItemLabel l : item.getLabels()) {
            if (!Constants.ITEM_LABEL_NOTA.equals(l.getLabel())) continue;
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = objectMapper.readValue(l.getValore(), Map.class);
                String testo = String.valueOf(parsed.getOrDefault("testo", ""));
                String visibilita = String.valueOf(parsed.getOrDefault("visibilita", ""));
                result.add(new NotaDTO(testo, "null".equals(visibilita) ? "" : visibilita));
            } catch (Exception ignored) {
                // valore non JSON (dato legacy o corrotto): ignora la nota
            }
        }
        return result;
    }

    /** Inverte lo stato di completamento di una quest foglia (senza sotto-quest). */
    public void toggleCompletata(Integer idItem) {
        Item itm = itemRepository.findItemById(idItem);
        if (itm == null) throw new RuntimeException("Quest non trovata: " + idItem);
        boolean cur = "1".equals(itm.getLabel(Constants.ITEM_LABEL_QUEST_COMPLETATA));
        itm.setLabel(Constants.ITEM_LABEL_QUEST_COMPLETATA, cur ? "0" : "1");
        itemRepository.save(itm);
    }

    /**
     * Sostituisce integralmente le righe IN_CARICO di una quest (righe vuote escluse): modifica
     * rapida dalla scheda/vista quest, senza passare dall'editor completo dell'item.
     */
    public void setInCarico(Integer idItem, List<String> valori) {
        Item itm = itemRepository.findItemById(idItem);
        if (itm == null) throw new RuntimeException("Quest non trovata: " + idItem);
        itm.removeLabel(Constants.ITEM_LABEL_QUEST_IN_CARICO);
        if (itm.getLabels() == null) itm.setLabels(new ArrayList<>());
        for (String v : valori) {
            if (v == null || v.isBlank()) continue;
            ItemLabel l = new ItemLabel();
            l.setItem(itm);
            l.setLabel(Constants.ITEM_LABEL_QUEST_IN_CARICO);
            l.setValore(v.trim());
            itm.getLabels().add(l);
        }
        itemRepository.save(itm);
    }
}
