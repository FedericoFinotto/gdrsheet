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
import java.util.LinkedHashMap;
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

    /** Quest visibili a un personaggio: le sue proprie + quelle del suo party + quelle del mondo del party. */
    public List<QuestDTO> getQuestPersonaggio(Integer idPersonaggio, Utente utente) {
        Personaggio pg = personaggioRepository.findPersonaggioById(idPersonaggio);
        if (pg == null) throw new RuntimeException("Personaggio non trovato: " + idPersonaggio);

        // radice -> personaggio "proprietario" ai fini del filtro sulle note (null = party/mondo, nessun proprietario)
        Map<Item, Personaggio> roots = new LinkedHashMap<>();
        itemRepository.findQuestByPersonaggioId(idPersonaggio).forEach(i -> roots.put(i, pg));
        if (pg.getParty() != null) {
            itemRepository.findQuestByPartyId(String.valueOf(pg.getParty().getId())).forEach(i -> roots.put(i, null));
            if (pg.getParty().getMondo() != null) {
                itemRepository.findQuestByMondoId(pg.getParty().getMondo().getId()).forEach(i -> roots.put(i, null));
            }
        }
        return roots.entrySet().stream().map(e -> buildDTO(e.getKey(), e.getValue(), utente)).toList();
    }

    /**
     * Quest visibili a un party: le sue proprie + quelle del mondo del party + le quest
     * personali di OGNI personaggio del party (visibili qui in sola lettura: le note con
     * visibilità OWNER restano filtrate in base a chi sta effettivamente guardando).
     */
    public List<QuestDTO> getQuestParty(Integer idParty, Utente utente) {
        Party party = partyRepository.findById(idParty)
                .orElseThrow(() -> new RuntimeException("Party non trovato: " + idParty));

        Map<Item, Personaggio> roots = new LinkedHashMap<>();
        itemRepository.findQuestByPartyId(String.valueOf(idParty)).forEach(i -> roots.put(i, null));
        if (party.getMondo() != null) {
            itemRepository.findQuestByMondoId(party.getMondo().getId()).forEach(i -> roots.put(i, null));
        }
        for (Personaggio membro : personaggioRepository.findAllByParty_IdOrderByNomeAsc(idParty)) {
            itemRepository.findQuestByPersonaggioId(membro.getId()).forEach(i -> roots.put(i, membro));
        }
        return roots.entrySet().stream().map(e -> buildDTO(e.getKey(), e.getValue(), utente)).toList();
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

        return new QuestDTO(item.getId(), item.getNome(), item.getDescrizione(), completata, completati, totali, note, figliDTO);
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
}
