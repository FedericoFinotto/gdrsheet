package it.fin8.gdrsheet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.dto.InfoDTO;
import it.fin8.gdrsheet.dto.InfoDettaglioDTO;
import it.fin8.gdrsheet.dto.NotaDTO;
import it.fin8.gdrsheet.entity.Collegamento;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.entity.ItemLabel;
import it.fin8.gdrsheet.entity.Party;
import it.fin8.gdrsheet.entity.Personaggio;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.repository.CollegamentoRepository;
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
 * INFO: stesso concetto di albero/ambito delle QUEST (vedi QuestService), ma senza completamento
 * né "in carico" — il contenuto è la lista di note (già generica, ogni nota con una propria
 * visibilità), esattamente come per le quest.
 */
@Service
public class InfoService {

    private final ItemRepository itemRepository;
    private final PersonaggioRepository personaggioRepository;
    private final PartyRepository partyRepository;
    private final CollegamentoRepository collegamentoRepository;
    private final AuthzService authzService;
    private final ObjectMapper objectMapper;

    public InfoService(ItemRepository itemRepository, PersonaggioRepository personaggioRepository,
                        PartyRepository partyRepository, CollegamentoRepository collegamentoRepository,
                        AuthzService authzService, ObjectMapper objectMapper) {
        this.itemRepository = itemRepository;
        this.personaggioRepository = personaggioRepository;
        this.partyRepository = partyRepository;
        this.collegamentoRepository = collegamentoRepository;
        this.authzService = authzService;
        this.objectMapper = objectMapper;
    }

    /**
     * INFO visibili a un party: prima quelli del party, poi quelli del mondo, poi gli INFO
     * personali di OGNI personaggio del party (in ordine alfabetico, se presenti — nella UI
     * attuale un INFO si crea sempre da ambito PARTY/MONDO, questo resta per simmetria con le
     * quest e per non perdere eventuali INFO personali creati in passato) — visibili qui in sola
     * lettura: le note con visibilità ristretta restano filtrate in base a chi guarda davvero.
     */
    public List<InfoDTO> getInfoParty(Integer idParty, Utente utente, boolean archiviate) {
        Party party = partyRepository.findById(idParty)
                .orElseThrow(() -> new RuntimeException("Party non trovato: " + idParty));

        List<InfoDTO> result = new ArrayList<>();
        result.addAll(buildForest(itemRepository.findInfoByPartyIdArchiviata(String.valueOf(idParty), archiviate), "PARTY", null));
        if (party.getMondo() != null) {
            result.addAll(buildForest(itemRepository.findInfoByMondoIdArchiviata(party.getMondo().getId(), archiviate), "MONDO", null));
        }
        for (Personaggio membro : personaggioRepository.findAllByParty_IdOrderByNomeAsc(idParty)) {
            result.addAll(buildForest(itemRepository.findInfoByPersonaggioIdArchiviata(membro.getId(), archiviate), "PERSONAGGIO", membro));
        }
        return result;
    }

    /**
     * Costruisce l'albero di un gruppo di INFO radice con un numero di query proporzionale alla
     * PROFONDITÀ, non al numero di nodi (una query per livello per la struttura). Descrizione e
     * note NON sono incluse: sono la parte pesante, caricata solo all'apertura del nodo (vedi
     * {@link #getDettaglio}) — l'albero porta solo ciò che serve a disegnare un nodo chiuso.
     */
    private List<InfoDTO> buildForest(List<Item> radici, String ambito, Personaggio owner) {
        if (radici.isEmpty()) return List.of();

        Map<Integer, String> nomi = new LinkedHashMap<>();
        for (Item r : radici) nomi.put(r.getId(), r.getNome());

        Map<Integer, List<Integer>> figliPerPadre = new HashMap<>();
        List<Integer> livello = new ArrayList<>(nomi.keySet());
        while (!livello.isEmpty()) {
            List<Integer> prossimo = new ArrayList<>();
            for (Object[] arco : collegamentoRepository.findFigliByTipo(livello, List.of(TipoItem.INFO))) {
                Integer padre = (Integer) arco[0];
                Integer figlio = (Integer) arco[1];
                String nomeFiglio = (String) arco[2];
                figliPerPadre.computeIfAbsent(padre, k -> new ArrayList<>()).add(figlio);
                if (nomi.putIfAbsent(figlio, nomeFiglio) == null) prossimo.add(figlio);
            }
            livello = prossimo;
        }

        List<InfoDTO> out = new ArrayList<>();
        for (Item r : radici) {
            InfoDTO dto = buildNodo(r.getId(), nomi, figliPerPadre, new HashSet<>());
            dto.setAmbito(ambito);
            dto.setPersonaggioNome(owner != null ? owner.getNome() : null);
            out.add(dto);
        }
        return out;
    }

    /** Costruzione ricorsiva dalle mappe già in memoria (nessuna query), con guardia anti-ciclo. */
    private InfoDTO buildNodo(Integer id, Map<Integer, String> nomi, Map<Integer, List<Integer>> figliPerPadre,
                              Set<Integer> percorso) {
        List<InfoDTO> figli = new ArrayList<>();
        if (percorso.add(id)) {
            for (Integer f : figliPerPadre.getOrDefault(id, List.of())) {
                figli.add(buildNodo(f, nomi, figliPerPadre, percorso));
            }
            percorso.remove(id);
        }
        return new InfoDTO(id, nomi.get(id), null, List.of(), figli, null, null);
    }

    /** Descrizione e note di un singolo INFO: caricate solo quando l'utente lo apre. */
    public InfoDettaglioDTO getDettaglio(Integer idInfo, Utente utente) {
        Item itm = itemRepository.findItemById(idInfo);
        if (itm == null) throw new RuntimeException("Info non trovato: " + idInfo);
        Personaggio rootOwner = trovaRootOwner(itm);
        List<NotaDTO> note = parseNote(itm).stream()
                .filter(n -> authzService.canViewVisibilita(utente, rootOwner, n.getVisibilita()))
                .toList();
        return new InfoDettaglioDTO(itm.getId(), itm.getDescrizione(), note);
    }

    /**
     * Risale l'albero fino all'INFO radice: è lui a portare l'eventuale personaggio proprietario,
     * da cui dipende la visibilità OWNER delle note (i sotto-info non hanno un ambito proprio).
     */
    private Personaggio trovaRootOwner(Item info) {
        Item cur = info;
        Set<Integer> visti = new HashSet<>();
        while (cur != null && visti.add(cur.getId())) {
            if (cur.getPersonaggio() != null) return cur.getPersonaggio();
            Item padre = null;
            for (Collegamento c : collegamentoRepository.findAllByItemTarget_Id(cur.getId())) {
                if (c.getItemSource() != null && TipoItem.INFO.equals(c.getItemSource().getTipo())) {
                    padre = c.getItemSource();
                    break;
                }
            }
            cur = padre;
        }
        return null;
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
}
