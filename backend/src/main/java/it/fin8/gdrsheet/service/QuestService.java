package it.fin8.gdrsheet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.dto.NotaDTO;
import it.fin8.gdrsheet.dto.QuestDTO;
import it.fin8.gdrsheet.dto.QuestDettaglioDTO;
import it.fin8.gdrsheet.dto.QuestSceltaDTO;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.entity.ItemLabel;
import it.fin8.gdrsheet.entity.Party;
import it.fin8.gdrsheet.entity.Personaggio;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.repository.CollegamentoRepository;
import it.fin8.gdrsheet.repository.ItemLabelRepository;
import it.fin8.gdrsheet.repository.ItemRepository;
import it.fin8.gdrsheet.repository.PartyRepository;
import it.fin8.gdrsheet.repository.PersonaggioRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final CollegamentoRepository collegamentoRepository;
    private final ItemLabelRepository itemLabelRepository;
    private final AuthzService authzService;
    private final ObjectMapper objectMapper;

    public QuestService(ItemRepository itemRepository, PersonaggioRepository personaggioRepository,
                         PartyRepository partyRepository, CollegamentoRepository collegamentoRepository,
                         ItemLabelRepository itemLabelRepository, AuthzService authzService,
                         ObjectMapper objectMapper) {
        this.itemRepository = itemRepository;
        this.personaggioRepository = personaggioRepository;
        this.partyRepository = partyRepository;
        this.collegamentoRepository = collegamentoRepository;
        this.itemLabelRepository = itemLabelRepository;
        this.authzService = authzService;
        this.objectMapper = objectMapper;
    }

    /**
     * Quest visibili a un personaggio: prima quelle del party, poi quelle del mondo, poi le
     * proprie. {@code archiviate}=false (default) mostra solo le non archiviate; true mostra SOLO
     * le archiviate — mai entrambe insieme, per il tastino "solo archiviate/solo attive".
     */
    public List<QuestDTO> getQuestPersonaggio(Integer idPersonaggio, Utente utente, boolean archiviate) {
        Personaggio pg = personaggioRepository.findPersonaggioById(idPersonaggio);
        if (pg == null) throw new RuntimeException("Personaggio non trovato: " + idPersonaggio);

        List<QuestDTO> result = new ArrayList<>();
        if (pg.getParty() != null) {
            result.addAll(buildForest(itemRepository.findQuestByPartyIdArchiviata(String.valueOf(pg.getParty().getId()), archiviate), "PARTY", null));
            if (pg.getParty().getMondo() != null) {
                result.addAll(buildForest(itemRepository.findQuestByMondoIdArchiviata(pg.getParty().getMondo().getId(), archiviate), "MONDO", null));
            }
        }
        result.addAll(buildForest(itemRepository.findQuestByPersonaggioIdArchiviata(idPersonaggio, archiviate), "PERSONAGGIO", pg));
        return result;
    }

    /**
     * Quest visibili a un party: prima quelle del party, poi quelle del mondo, poi le quest
     * personali di OGNI personaggio del party (in ordine alfabetico di personaggio) — visibili
     * qui in sola lettura: le note con visibilità OWNER restano filtrate in base a chi sta
     * effettivamente guardando.
     */
    public List<QuestDTO> getQuestParty(Integer idParty, Utente utente, boolean archiviate) {
        Party party = partyRepository.findById(idParty)
                .orElseThrow(() -> new RuntimeException("Party non trovato: " + idParty));

        List<QuestDTO> result = new ArrayList<>();
        result.addAll(buildForest(itemRepository.findQuestByPartyIdArchiviata(String.valueOf(idParty), archiviate), "PARTY", null));
        if (party.getMondo() != null) {
            result.addAll(buildForest(itemRepository.findQuestByMondoIdArchiviata(party.getMondo().getId(), archiviate), "MONDO", null));
        }
        for (Personaggio membro : personaggioRepository.findAllByParty_IdOrderByNomeAsc(idParty)) {
            result.addAll(buildForest(itemRepository.findQuestByPersonaggioIdArchiviata(membro.getId(), archiviate), "PERSONAGGIO", membro));
        }
        return result;
    }

    /**
     * Costruisce l'albero completo di un gruppo di quest radice con un numero di query
     * proporzionale alla PROFONDITÀ dell'albero, non al numero di nodi: prima la struttura
     * (una query per livello), poi completamento e "in carico" di tutti i nodi in una query sola.
     * <p>
     * Descrizione e note NON vengono incluse: sono la parte pesante (rich text) e servono solo
     * quando l'utente apre davvero quel nodo — le carica {@link #getDettaglio}. Restano invece
     * nell'albero i dati che servono a disegnare una riga chiusa: nome, conteggi e chip "in carico".
     */
    private List<QuestDTO> buildForest(List<Item> radici, String ambito, Personaggio owner) {
        if (radici.isEmpty()) return List.of();

        Map<Integer, String> nomi = new LinkedHashMap<>();
        for (Item r : radici) nomi.put(r.getId(), r.getNome());

        Map<Integer, List<Integer>> figliPerPadre = new HashMap<>();
        List<Integer> livello = new ArrayList<>(nomi.keySet());
        while (!livello.isEmpty()) {
            List<Integer> prossimo = new ArrayList<>();
            for (Object[] arco : collegamentoRepository.findFigliByTipo(livello, List.of(TipoItem.QUEST))) {
                Integer padre = (Integer) arco[0];
                Integer figlio = (Integer) arco[1];
                String nomeFiglio = (String) arco[2];
                figliPerPadre.computeIfAbsent(padre, k -> new ArrayList<>()).add(figlio);
                // un nodo già visto è un riaggancio (o un ciclo): non lo riespando
                if (nomi.putIfAbsent(figlio, nomeFiglio) == null) prossimo.add(figlio);
            }
            livello = prossimo;
        }

        Map<Integer, Boolean> completate = new HashMap<>();
        Map<Integer, List<String>> inCarico = new HashMap<>();
        for (Object[] l : itemLabelRepository.findLabelValuesByItemIds(
                List.of(Constants.ITEM_LABEL_QUEST_COMPLETATA, Constants.ITEM_LABEL_QUEST_IN_CARICO), nomi.keySet())) {
            Integer itemId = (Integer) l[0];
            String label = (String) l[1];
            String valore = (String) l[2];
            if (Constants.ITEM_LABEL_QUEST_COMPLETATA.equals(label)) {
                completate.put(itemId, "1".equals(valore));
            } else if (valore != null && !valore.isBlank()) {
                inCarico.computeIfAbsent(itemId, k -> new ArrayList<>()).add(valore.trim());
            }
        }

        List<QuestDTO> out = new ArrayList<>();
        for (Item r : radici) {
            QuestDTO dto = buildNodo(r.getId(), nomi, figliPerPadre, completate, inCarico, new HashSet<>());
            dto.setAmbito(ambito);
            dto.setPersonaggioNome(owner != null ? owner.getNome() : null);
            out.add(dto);
        }
        return out;
    }

    /** Costruzione ricorsiva dalle mappe già in memoria (nessuna query), con guardia anti-ciclo. */
    private QuestDTO buildNodo(Integer id, Map<Integer, String> nomi, Map<Integer, List<Integer>> figliPerPadre,
                               Map<Integer, Boolean> completate, Map<Integer, List<String>> inCarico,
                               Set<Integer> percorso) {
        List<QuestDTO> figli = new ArrayList<>();
        if (percorso.add(id)) {
            for (Integer f : figliPerPadre.getOrDefault(id, List.of())) {
                figli.add(buildNodo(f, nomi, figliPerPadre, completate, inCarico, percorso));
            }
            percorso.remove(id);
        }

        boolean completata = Boolean.TRUE.equals(completate.get(id));
        int completati;
        int totali;
        if (figli.isEmpty()) {
            completati = completata ? 1 : 0;
            totali = 1;
        } else {
            completati = figli.stream().mapToInt(QuestDTO::getCompletati).sum();
            totali = figli.stream().mapToInt(QuestDTO::getTotali).sum();
        }

        return new QuestDTO(id, nomi.get(id), null, completata, completati, totali,
                List.of(), inCarico.getOrDefault(id, List.of()), figli, null, null);
    }

    /** Descrizione e note di una singola quest: caricate solo quando l'utente la apre. */
    public QuestDettaglioDTO getDettaglio(Integer idQuest, Utente utente) {
        Item itm = itemRepository.findItemById(idQuest);
        if (itm == null) throw new RuntimeException("Quest non trovata: " + idQuest);
        Personaggio rootOwner = trovaRootOwner(itm);
        List<NotaDTO> note = parseNote(itm).stream()
                .filter(n -> authzService.canViewVisibilita(utente, rootOwner, n.getVisibilita()))
                .toList();
        return new QuestDettaglioDTO(itm.getId(), itm.getDescrizione(), note);
    }

    /**
     * Risale l'albero fino alla quest radice: è lei a portare l'eventuale personaggio proprietario,
     * da cui dipende la visibilità OWNER delle note (le sotto-quest non hanno un ambito proprio).
     */
    private Personaggio trovaRootOwner(Item quest) {
        Item cur = quest;
        Set<Integer> visti = new HashSet<>();
        while (cur != null && visti.add(cur.getId())) {
            if (cur.getPersonaggio() != null) return cur.getPersonaggio();
            Item padre = null;
            for (it.fin8.gdrsheet.entity.Collegamento c : collegamentoRepository.findAllByItemTarget_Id(cur.getId())) {
                if (c.getItemSource() != null && TipoItem.QUEST.equals(c.getItemSource().getTipo())) {
                    padre = c.getItemSource();
                    break;
                }
            }
            cur = padre;
        }
        return null;
    }

    /**
     * Albero completo (radice + tutte le sotto-quest) a cui appartiene una quest qualunque, a
     * qualunque profondità: usato dalla ricerca profonda per mostrare, cliccando su una quest
     * trovata, esattamente lo stesso componente ad albero della pagina Quest — risalendo fino
     * alla radice se il risultato è una sotto-quest (di qualunque livello).
     */
    public QuestDTO getQuestAlbero(Integer idQuest) {
        Item quest = itemRepository.findItemById(idQuest);
        if (quest == null || !TipoItem.QUEST.equals(quest.getTipo()))
            throw new RuntimeException("Quest non trovata: " + idQuest);

        Item root = trovaRootItem(quest);
        Personaggio owner = root.getPersonaggio();
        String ambito;
        if (owner != null) {
            ambito = "PERSONAGGIO";
        } else if (root.getLabel(Constants.ITEM_LABEL_QUEST_PARTY) != null) {
            ambito = "PARTY";
        } else {
            ambito = "MONDO";
        }

        List<QuestDTO> tree = buildForest(List.of(root), ambito, owner);
        return tree.isEmpty() ? null : tree.get(0);
    }

    /** Risale l'albero fino alla quest radice (nessuna quest genitore): stesso criterio delle query "radice". */
    private Item trovaRootItem(Item quest) {
        Item cur = quest;
        Set<Integer> visti = new HashSet<>();
        while (visti.add(cur.getId())) {
            Item padre = null;
            for (it.fin8.gdrsheet.entity.Collegamento c : collegamentoRepository.findAllByItemTarget_Id(cur.getId())) {
                if (c.getItemSource() != null && TipoItem.QUEST.equals(c.getItemSource().getTipo())) {
                    padre = c.getItemSource();
                    break;
                }
            }
            if (padre == null) return cur;
            cur = padre;
        }
        return cur;
    }

    /**
     * Quest radice collegabili come sotto-quest di un'altra. Solo radici: i discendenti di una
     * quest non sono mai radici, quindi la scelta è per costruzione a prova di ciclo.
     */
    public List<QuestSceltaDTO> searchQuestRadici(String q, Integer excludeId) {
        String needle = q == null ? "" : q.trim();
        if (needle.length() < 2) return List.of();
        List<QuestSceltaDTO> out = new ArrayList<>();
        for (Item i : itemRepository.searchQuestRadici(needle, org.springframework.data.domain.PageRequest.of(0, 20))) {
            if (excludeId != null && excludeId.equals(i.getId())) continue;
            out.add(new QuestSceltaDTO(i.getId(), i.getNome(), descriviAmbito(i)));
        }
        return out;
    }

    private String descriviAmbito(Item quest) {
        if (quest.getPersonaggio() != null) return "Personaggio: " + quest.getPersonaggio().getNome();
        if (quest.getLabel(Constants.ITEM_LABEL_QUEST_PARTY) != null) return "Party";
        if (quest.getMondo() != null) return "Mondo";
        return null;
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
                String v = "null".equals(visibilita) ? "" : visibilita;
                result.add(new NotaDTO(testo, v, authzService.descriviVisibilitaChips(v)));
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
