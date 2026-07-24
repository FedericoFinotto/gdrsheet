package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.StatDefault;
import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.def.TipoModificatore;
import it.fin8.gdrsheet.def.TipoStat;
import it.fin8.gdrsheet.dto.*;
import it.fin8.gdrsheet.entity.*;
import it.fin8.gdrsheet.mapper.ItemMapper;
import it.fin8.gdrsheet.mapper.ModificatoreMapper;
import it.fin8.gdrsheet.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class PersonaggioService {

    @Autowired
    private PersonaggioRepository personaggioRepository;

    @Autowired
    private PersonaggioLabelRepository personaggioLabelRepository;

    @Autowired
    private jakarta.persistence.EntityManagerFactory entityManagerFactory;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ModificatoreRepository modificatoreRepository;

    @Autowired
    private StatValueRepository statValueRepository;

    @Autowired
    private StatRepository statRepository;

    @Autowired
    private ItemLabelRepository itemLabelRepository;

    @Autowired
    public ModificatoreMapper modificatoreMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ModificatoriService modificatoriService;

    @Autowired
    private CollegamentoRepository collegamentoRepository;

    @Autowired
    private AvanzamentoRepository avanzamentoRepository;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private PartyService partyService;

    @Autowired
    private CalcoloService calcoloService;

    @Autowired
    private UtilService utilService;

    @Autowired
    private AuthzService authzService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private PersonaggioCacheService personaggioCacheService;

    private static final String CACHE_MODIFICATORI = "personaggioModificatori";
    private static final String CACHE_ITEMS = "personaggioItems";

    /**
     * Chiave della cache items: dipende dalla CLASSE di visibilità dell'utente (ADMIN/MASTER/
     * OWNER/GIOCATORE), non dal singolo utente — sono due utenti GIOCATORE qualsiasi (non
     * proprietari) a vedere esattamente lo stesso ItemsDTO, quindi condividono la stessa entry
     * invece di sprecarne una a testa (vedi AuthzService.visibilityClass).
     */
    private String itemsCacheKey(Integer id, Utente utente) {
        Integer partyId = personaggioRepository.findPartyIdById(id);
        String visClass = authzService.visibilityClass(utente, id, partyId);
        return id + ":" + visClass;
    }

    /**
     * Flattens iteratively the item hierarchy into a list.
     */
    private record FlattenEntry(Item item, String incomingScelta) {}

    private static Set<String> sceltaSetFrom(String scelta) {
        if (scelta == null || scelta.isBlank()) return Set.of();
        return new HashSet<>(Arrays.asList(scelta.split(",")));
    }

    private List<Item> flattenItems(Collection<Item> rootItems) {
        return flattenItems(rootItems, null);
    }

    /**
     * Raccoglie ricorsivamente (a qualunque profondità) gli id dei discendenti di un CONTENITORE
     * con INVENTARIO_SEPARATO=1, associandoli all'id del contenitore stesso in {@code out}.
     * Un item già assegnato a un contenitore separato (il più esterno incontrato per primo,
     * nell'ordine di {@code allPersonaggioItems.getItems()}) non viene riassegnato: caso raro di
     * contenitori-separati annidati, non serve gestirlo oltre a evitare comportamenti incoerenti.
     */
    private void raccogliDiscendentiSeparati(Item container, Set<Integer> visitati, Map<Integer, Integer> out, Integer containerId) {
        if (container.getChild() == null) return;
        for (Collegamento col : container.getChild()) {
            Item target = col.getItemTarget();
            if (target == null || !visitati.add(target.getId())) continue;
            boolean nascosto = utilService.parseBooleanFromString(col.getLabel(Constants.ITEM_LABEL_NASCOSTO),
                    Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE, Constants.ITEM_LABEL_DISABILITATO_VALORE_FALSE);
            if (nascosto) continue;
            out.putIfAbsent(target.getId(), containerId);
            raccogliDiscendentiSeparati(target, visitati, out, containerId);
        }
    }

    /**
     * Mappa (itemId discendente -> id del contenitore "separato" che lo contiene) per tutti i
     * CONTENITORE con INVENTARIO_SEPARATO=1 (es. la Stiva di una NAVE), a qualunque profondità.
     * Usata in getDatiPersonaggio per: (a) escludere questi item dalla flatten "meccanica"
     * (modificatori, spellbook, contatori) — un item nella Stiva è carico, non dotazione, e non
     * deve propagare nulla al personaggio anche se abilitato, esattamente come se non fosse
     * collegato affatto (più forte di "disabilitato", che invece resta comunque nella lista ma
     * senza effetti); (b) calcolarne comunque il peso a parte, sommandolo direttamente invece di
     * farlo passare dall'euristica a "capienza" di calcolaPeso (pensata per zaini con un peso
     * dichiarato "a pieno carico", non per sommare un contenuto reale arbitrario).
     */
    private Map<Integer, Integer> mappaContenitoriSeparati(List<Item> items) {
        Map<Integer, Integer> out = new HashMap<>();
        for (Item itm : items) {
            if (TipoItem.CONTENITORE.equals(itm.getTipo()) && utilService.parseBooleanFromString(
                    itm.getLabel(Constants.LABEL_INVENTARIO_SEPARATO),
                    Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE, Constants.ITEM_LABEL_DISABILITATO_VALORE_FALSE)) {
                raccogliDiscendentiSeparati(itm, new HashSet<>(), out, itm.getId());
            }
        }
        return out;
    }

    /**
     * Lista di destinazione per un ItemDTO: quella normale per il suo tipo, oppure la sezione
     * del contenitore "separato" a cui appartiene (creata al volo la prima volta).
     */
    private List<ItemDTO> destinazioneLista(List<ItemDTO> normale, Integer idContenitoreSeparato,
                                            Map<Integer, InventarioSeparatoDTO> separati, Map<Integer, Item> itemById) {
        if (idContenitoreSeparato == null) return normale;
        return separati.computeIfAbsent(idContenitoreSeparato, cid -> {
            Item contenitore = itemById.get(cid);
            return new InventarioSeparatoDTO(cid, contenitore != null ? contenitore.getNome() : "?", new ArrayList<>());
        }).getItems();
    }

    /**
     * Traversal DFS degli item del personaggio.
     * @param fruttiSenzaModOut se non null, viene popolato con gli ID dei FRUTTO la cui scelta
     *                          non include "MOD" (i loro modificatori non vanno calcolati).
     */
    private List<Item> flattenItems(Collection<Item> rootItems, Set<Integer> fruttiSenzaModOut) {
        List<Item> result = new ArrayList<>();
        Deque<FlattenEntry> stack = new ArrayDeque<>();
        for (Item root : rootItems) stack.push(new FlattenEntry(root, null));

        while (!stack.isEmpty()) {
            FlattenEntry entry = stack.pop();
            Item cur = entry.item();
            String incomingScelta = entry.incomingScelta();

            result.add(cur);

            // Traccia scelta MOD per FRUTTO (null/vuoto/ALL = prendi tutto = includi MOD)
            if (TipoItem.FRUTTO.equals(cur.getTipo()) && fruttiSenzaModOut != null) {
                Set<String> sc = sceltaSetFrom(incomingScelta);
                boolean hasMod = sc.isEmpty() || sc.contains("ALL") || sc.contains("MOD");
                if (!hasMod) fruttiSenzaModOut.add(cur.getId());
            }

            boolean isDisabled = utilService.parseBooleanFromString(cur.getLabel(Constants.ITEM_LABEL_DISABILITATO), Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE, Constants.ITEM_LABEL_DISABILITATO_VALORE_FALSE);

            if (!isDisabled) {
                List<Item> competenze = findCompetenze(cur);
//                List<Item> lingue = findLingue(cur);

                if (cur.getChild() != null) {
                    // Per i FRUTTO: filtra le FORME in base alla scelta (null/vuoto/ALL = tutte)
                    Set<String> sc = TipoItem.FRUTTO.equals(cur.getTipo()) ? sceltaSetFrom(incomingScelta) : null;
                    // null sc = non siamo in un FRUTTO; empty o ALL = prendi tutto = non filtrare
                    Set<String> sceltaSet = (sc != null && !sc.isEmpty() && !sc.contains("ALL")) ? sc : null;

                    // Indice 1-based delle FORME (ordinate per id)
                    List<Collegamento> formaLinks = sceltaSet != null
                            ? cur.getChild().stream()
                                .filter(c -> TipoItem.FORMA.equals(c.getItemTarget().getTipo()))
                                .sorted(Comparator.comparing(c -> c.getItemTarget().getId()))
                                .toList()
                            : List.of();

                    for (Collegamento col : cur.getChild().stream()
                            .filter(x -> !x.getItemTarget().getTipo().equals(TipoItem.CLASSE)).toList()) {
                        boolean isColDisabled = utilService.parseBooleanFromString(col.getLabel(Constants.ITEM_LABEL_DISABILITATO), Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE, Constants.ITEM_LABEL_DISABILITATO_VALORE_FALSE);
                        boolean isColNascosto = utilService.parseBooleanFromString(col.getLabel(Constants.ITEM_LABEL_NASCOSTO), Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE, Constants.ITEM_LABEL_DISABILITATO_VALORE_FALSE);
                        // Collegamento NASCOSTO: non visibile da fuori, saltalo del tutto (nascondere implica disabilitare).
                        // Per le FORMA resta comunque nell'indice formaLinks, così FORMA_N delle altre non slitta.
                        if (isColNascosto) {
                            continue;
                        }
                        if (isColDisabled) {
                            col.getItemTarget().setLabel(Constants.ITEM_LABEL_DISABILITATO, Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE);
                        }

                        // Se stiamo in un FRUTTO con scelta, salta le FORMA non selezionate
                        if (sceltaSet != null && TipoItem.FORMA.equals(col.getItemTarget().getTipo())) {
                            int idx = formaLinks.indexOf(col);
                            if (idx < 0 || !sceltaSet.contains("FORMA_" + (idx + 1))) continue;
                        }

                        if (cur.getTipo().equals(TipoItem.LIVELLO)) {
                            if (!List.of(TipoItem.CLASSE, TipoItem.MALEDIZIONE).contains(col.getItemTarget().getTipo())) {
                                // Passa la scelta se il target è un FRUTTO
                                String childScelta = TipoItem.FRUTTO.equals(col.getItemTarget().getTipo()) ? col.getScelta() : null;
                                stack.push(new FlattenEntry(col.getItemTarget(), childScelta));
                            }
                        } else {
                            String childScelta = TipoItem.FRUTTO.equals(col.getItemTarget().getTipo()) ? col.getScelta() : null;
                            stack.push(new FlattenEntry(col.getItemTarget(), childScelta));
                        }
                    }
                }

                if (competenze != null) {
                    for (Item c : competenze) stack.push(new FlattenEntry(c, null));
                }
//                if (lingue != null) {
//                    for (Item l : lingue) stack.push(new FlattenEntry(l, null));
//                }
            }
        }

        return result;
    }


    public ItemsDTO getAllPersonaggioItemsDTOByIdPersonaggio(Integer id, Utente utente, Map<Integer, Integer> utilizziTotaleFormula) {
        return getAllPersonaggioItemsDTOByIdPersonaggio(id, utente, utilizziTotaleFormula, getAllPersonaggioItemsByIdPersonaggio(id));
    }

    /**
     * Punto d'ingresso cache-aware per /items: se la cache ha già il risultato per
     * (personaggioId, utente) lo restituisce subito, senza toccare il DB. Altrimenti calcola il
     * flatten UNA volta e, con l'albero item ancora "caldo" in questa stessa richiesta/sessione,
     * lo riusa per pre-scaldare anche la cache di getDatiPersonaggio — così la chiamata "gemella"
     * (/modificatori, o viceversa) che arriva subito dopo lo trova già pronto invece di rifare da
     * zero la stessa traversata costosa.
     */
    public ItemsDTO getAllPersonaggioItemsDTOByIdPersonaggio(Integer id, Utente utente) {
        Cache itemsCache = cacheManager.getCache(CACHE_ITEMS);
        String key = itemsCacheKey(id, utente);
        if (itemsCache != null) {
            ItemsDTO cached = itemsCache.get(key, ItemsDTO.class);
            if (cached != null) return cached;
        }

        Cache modCache = cacheManager.getCache(CACHE_MODIFICATORI);
        AllPersonaggioItems allPersonaggioItems = getAllPersonaggioItemsByIdPersonaggio(id);
        DatiPersonaggioDTO dati = modCache != null ? modCache.get(id, DatiPersonaggioDTO.class) : null;
        if (dati == null) {
            dati = getDatiPersonaggio(id, allPersonaggioItems);
            if (modCache != null) modCache.put(id, dati);
        }

        ItemsDTO result = getAllPersonaggioItemsDTOByIdPersonaggio(id, utente, dati.getUtilizziTotaleFormula(), allPersonaggioItems);
        if (itemsCache != null) itemsCache.put(key, result);
        return result;
    }

    /**
     * Overload che accetta il flatten (AllPersonaggioItems) già calcolato dal chiamante,
     * per evitare di ripeterlo quando è già disponibile (es. endpoint /items, che lo
     * calcola una volta sola e lo condivide con getDatiPersonaggio).
     */
    public ItemsDTO getAllPersonaggioItemsDTOByIdPersonaggio(Integer id, Utente utente, Map<Integer, Integer> utilizziTotaleFormula, AllPersonaggioItems allPersonaggioItems) {
        ItemsDTO itemsDTO = new ItemsDTO();
        Personaggio personaggio = personaggioRepository.findPersonaggioById(id);

        // Calcola gli id degli item collegati direttamente via FromCompendio (scollegabili)
        Item fromCompendio = itemRepository.findItemByNomeAndPersonaggio_Id(Constants.ITEM_FROM_COMPENDIO, id);
        Set<Integer> scollegabiliIds = new HashSet<>();
        if (fromCompendio != null) {
            collegamentoRepository.findAllByItemSource_Id(fromCompendio.getId())
                    .forEach(c -> scollegabiliIds.add(c.getItemTarget().getId()));
        }

        // Carica utilizzi per-personaggio in bulk (una sola query)
        List<Integer> allItemIds = allPersonaggioItems.getItems().stream().map(Item::getId).toList();
        Map<Integer, Integer> utilizziUsatiMap = new HashMap<>();
        Map<Integer, Integer> utilizziTotaleMap = new HashMap<>();
        if (!allItemIds.isEmpty()) {
            for (it.fin8.gdrsheet.entity.ItemLabel ul : itemLabelRepository
                    .findByLabelAndItem_IdInAndPersonaggio_Id(Constants.LABEL_UTILIZZI_USATI, allItemIds, id)) {
                try { utilizziUsatiMap.put(ul.getItem().getId(), Integer.parseInt(ul.getValore())); }
                catch (Exception ignored) {}
            }
            // Somma qty da collegamento e avanzamento:
            // - se almeno uno ha qty esplicito → somma i qty (null conta come 1)
            // - se tutti null ma connessioni > 1 → conta le occorrenze
            // - se connessione unica senza qty → non mostrare il contatore
            for (Object[] row : collegamentoRepository.sumQtyByTargets(allItemIds, allItemIds)) {
                if (row[1] == null) continue;
                int targetId = ((Number) row[0]).intValue();
                int totale = ((Number) row[1]).intValue();
                if (totale > 0) utilizziTotaleMap.merge(targetId, totale, Integer::sum);
            }
            // utilizziTotale da formulaQty: val > 0 = counter visibile; val <= 0 = item bloccato (utilizziTotale=0)
            utilizziTotaleFormula.forEach((targetId, val) -> {
                if (val > 0) utilizziTotaleMap.merge(targetId, val, Integer::sum);
                else utilizziTotaleMap.putIfAbsent(targetId, 0);
            });
            // NB: gli avanzamento (CLASSE→PRIVILEGIO) sono solo template di classe.
            // Il qty viene copiato sul collegamento (LIVELLO→PRIVILEGIO) dal LivelloEditor.
            // Contare entrambi produrrebbe un doppio conteggio.
        }

        // QTA per-personaggio: sovrascrive la QTA globale dell'item per questo personaggio
        Map<Integer, Integer> qtaPersoMap = new HashMap<>();
        if (!allItemIds.isEmpty()) {
            for (ItemLabel ql : itemLabelRepository.findByLabelAndItem_IdInAndPersonaggio_Id(
                    Constants.LABEL_QTA, allItemIds, id)) {
                Integer v = parseIntOrNull(ql.getValore());
                if (ql.getItem() != null && v != null) qtaPersoMap.put(ql.getItem().getId(), v);
            }
        }

        // Lista piatta di TUTTE le trasformazioni (frutti-figlie incluse): raggruppata a fine metodo,
        // separando quelle indipendenti (in itemsDTO.trasformazioni) da quelle di un FRUTTO (in
        // FruttoDTO.trasformazioni).
        List<TrasformazioneDTO> flatTrasformazioni = new ArrayList<>();

        // Contenuti dei CONTENITORE con INVENTARIO_SEPARATO=1: dirottati più sotto dalle liste
        // normali per tipo verso una sezione a parte (es. la Stiva di una NAVE). Il contenitore
        // stesso resta comunque visibile nella lista Contenitori: solo il suo contenuto è qui.
        Map<Integer, Integer> separatoContainerIdByItem = mappaContenitoriSeparati(allPersonaggioItems.getItems());
        Map<Integer, Item> itemById = new HashMap<>();
        for (Item itm : allPersonaggioItems.getItems()) itemById.put(itm.getId(), itm);
        Map<Integer, InventarioSeparatoDTO> inventariSeparati = new LinkedHashMap<>();

        Set<Integer> itemIdGiaAggiunti = new HashSet<>();
        for (Item itm : allPersonaggioItems.getItems()) {
            // filtro di visibilità (label VISIBILITA): nasconde l'item a chi non è autorizzato
            if (!authzService.canViewVisibilita(utente, personaggio, itm.getLabel(Constants.ITEM_LABEL_VISIBILITA))) {
                continue;
            }
            // Item strutturali interni: visibili solo agli admin
            if (!authzService.isAdmin(utente) &&
                    (Constants.ITEM_FROM_COMPENDIO.equals(itm.getNome()) ||
                     Constants.ITEM_INCANTESIMI_PREPARATI.equals(itm.getNome()))) {
                continue;
            }
            // deduplicazione: se l'item ha utilizzi sommati da più sorgenti, mostrarlo una volta sola
            if (utilizziTotaleMap.containsKey(itm.getId()) && !itemIdGiaAggiunti.add(itm.getId())) {
                continue;
            }
            Integer uUsati = utilizziUsatiMap.get(itm.getId());
            Integer uTotale = utilizziTotaleMap.get(itm.getId());
            Integer sepId = separatoContainerIdByItem.get(itm.getId());
            if (sepId != null) {
                // dentro un CONTENITORE "separato" (es. la Stiva di una NAVE): mostrato SOLO in
                // quella sezione, come voce generica qualunque sia il tipo reale — niente
                // Frutti/Abilità/Talenti/Attacchi/Spellbook/ecc. Coerente con l'esclusione dalla
                // "mechanical" flatten in getDatiPersonaggio: è carico, non dotazione, "come se
                // non fosse collegato al personaggio" (il peso però continua a contare altrove).
                ItemDTO dto = itemMapper.toDTO(itm, uTotale, uUsati);
                if (qtaPersoMap.containsKey(itm.getId())) dto.setQuantita(qtaPersoMap.get(itm.getId()));
                destinazioneLista(null, sepId, inventariSeparati, itemById).add(dto);
                continue;
            }
            if (TipoItem.ABILITA.equals(itm.getTipo())) {
                itemsDTO.getAbilita().add(itemMapper.toDTO(itm, uTotale, uUsati));
            }
            if (TipoItem.TALENTO.equals(itm.getTipo())) {
                itemsDTO.getTalenti().add(itemMapper.toDTO(itm, uTotale, uUsati));
            }
            if (TipoItem.OGGETTO.equals(itm.getTipo())) {
                ItemDTO dto = itemMapper.toDTO(itm, uTotale, uUsati);
                if (qtaPersoMap.containsKey(itm.getId())) dto.setQuantita(qtaPersoMap.get(itm.getId()));
                itemsDTO.getOggetti().add(dto);
            }
            if (TipoItem.CONSUMABILE.equals(itm.getTipo())) {
                ItemDTO dto = itemMapper.toDTO(itm, uTotale, uUsati);
                if (qtaPersoMap.containsKey(itm.getId())) dto.setQuantita(qtaPersoMap.get(itm.getId()));
                itemsDTO.getConsumabili().add(dto);
            }
            if (TipoItem.ARMA.equals(itm.getTipo())) {
                ItemDTO dto = itemMapper.toDTO(itm, uTotale, uUsati);
                if (qtaPersoMap.containsKey(itm.getId())) dto.setQuantita(qtaPersoMap.get(itm.getId()));
                itemsDTO.getArmi().add(dto);
            }
            if (TipoItem.MUNIZIONE.equals(itm.getTipo())) {
                ItemDTO dto = itemMapper.toDTO(itm, uTotale, uUsati);
                if (qtaPersoMap.containsKey(itm.getId())) dto.setQuantita(qtaPersoMap.get(itm.getId()));
                itemsDTO.getMunizioni().add(dto);
            }
            if (TipoItem.EQUIPAGGIAMENTO.equals(itm.getTipo())) {
                ItemDTO dto = itemMapper.toDTO(itm, uTotale, uUsati);
                if (qtaPersoMap.containsKey(itm.getId())) dto.setQuantita(qtaPersoMap.get(itm.getId()));
                itemsDTO.getEquipaggiamento().add(dto);
            }
            if (TipoItem.RAZZA.equals(itm.getTipo())) {
                itemsDTO.getRazze().add(itemMapper.toDTO(itm, uTotale, uUsati));
            }
            if (TipoItem.ATTACCO.equals(itm.getTipo())) {
                itemsDTO.getAttacchi().add(itemMapper.toAttaccoDTO(itm));
            }
            if (TipoItem.LIVELLO.equals(itm.getTipo())) {
                itemsDTO.getLivelli().add(itemMapper.toLivelloDTO(itm));
            }
            if (TipoItem.MALEDIZIONE.equals(itm.getTipo())) {
                itemsDTO.getMaledizioni().add(itemMapper.toDTO(itm, uTotale, uUsati));
            }
            if (TipoItem.VEICOLO.equals(itm.getTipo())) {
                itemsDTO.getVeicoli().add(itemMapper.toDTO(itm, uTotale, uUsati));
            }
            if (TipoItem.TRASFORMAZIONE.equals(itm.getTipo())) {
                flatTrasformazioni.add(itemMapper.toTrasformazioneDTO(itm));
            }
            if (TipoItem.COMP.equals(itm.getTipo())) {
                ItemDTO dto = itemMapper.toDTO(itm, uTotale, uUsati);
                dto.setScollegabile(scollegabiliIds.contains(itm.getId()));
                itemsDTO.getCompetenze().add(dto);
            }
            if (TipoItem.LINGUA.equals(itm.getTipo())) {
                ItemDTO dto = itemMapper.toDTO(itm, uTotale, uUsati);
                dto.setScollegabile(scollegabiliIds.contains(itm.getId()));
                itemsDTO.getLingue().add(dto);
            }
            if (TipoItem.IDOLO.equals(itm.getTipo())) {
                itemsDTO.getIdoli().add(itemMapper.toDTO(itm, uTotale, uUsati));
            }
            if (TipoItem.CONTENITORE.equals(itm.getTipo())) {
                ItemDTO dto = itemMapper.toDTO(itm, uTotale, uUsati);
                if (qtaPersoMap.containsKey(itm.getId())) dto.setQuantita(qtaPersoMap.get(itm.getId()));
                itemsDTO.getContenitori().add(dto);
            }
            if (TipoItem.FRUTTO.equals(itm.getTipo())) {
                itemsDTO.getFrutti().add(itemMapper.toFruttoDTO(itm, uTotale, uUsati));
            }
            if (TipoItem.FORMA.equals(itm.getTipo())) {
                itemsDTO.getForme().add(itemMapper.toDTO(itm, uTotale, uUsati));
            }
            if (TipoItem.PRIVILEGIO.equals(itm.getTipo())) {
                itemsDTO.getPrivilegi().add(itemMapper.toDTO(itm, uTotale, uUsati));
            }
            if (TipoItem.ALTRO.equals(itm.getTipo())) {
                ItemDTO dto = itemMapper.toDTO(itm, uTotale, uUsati);
                if (qtaPersoMap.containsKey(itm.getId())) dto.setQuantita(qtaPersoMap.get(itm.getId()));
                itemsDTO.getAltro().add(dto);
            }
            if (TipoItem.PATTO.equals(itm.getTipo())) {
                ItemDTO dto = itemMapper.toDTO(itm, uTotale, uUsati);
                if (qtaPersoMap.containsKey(itm.getId())) dto.setQuantita(qtaPersoMap.get(itm.getId()));
                itemsDTO.getPatti().add(dto);
            }
            if (TipoItem.NOTIZIA.equals(itm.getTipo())) {
                itemsDTO.getNotizie().add(itemMapper.toDTO(itm, uTotale, uUsati));
            }
        }
        itemsDTO.setInventariSeparati(new ArrayList<>(inventariSeparati.values()));

        for (InfoClasseDTO classe : allPersonaggioItems.getLivelli().getClassi()) {
            // Nuovo schema a sezioni (SPELL_<n>): una spellbook per sezione.
            List<SezioneIncantesimi> sezioni = parseSezioniIncantesimi(classe.getClasse());
            if (!sezioni.isEmpty()) {
                int baseTot = classe.getLivelloTotale() != null ? classe.getLivelloTotale() : 0;
                int baseEff = classe.getLivelloNonMaledetto() != null ? classe.getLivelloNonMaledetto() : 0;
                for (SezioneIncantesimi sez : sezioni) {
                    // sezione di solo avanzamento (liste tutte "+...") non genera spellbook proprio
                    if (sez.liste().stream().allMatch(l -> l != null && l.startsWith("+"))) continue;

                    // avanzamento da altre classi (liste "+<lista>" di classi da prestigio)
                    Set<String> listeSez = new HashSet<>(sez.liste());
                    int extraTot = 0, extraEff = 0;
                    for (InfoClasseDTO other : allPersonaggioItems.getLivelli().getClassi()) {
                        if (other.getClasse() == null
                                || Objects.equals(other.getClasse().getId(), classe.getClasse().getId())) continue;
                        boolean avanza = tutteLeListe(other.getClasse()).stream()
                                .anyMatch(ol -> ol != null && ol.startsWith("+") && listeSez.contains(ol.substring(1)));
                        if (avanza) {
                            extraTot += other.getLivelloTotale() != null ? other.getLivelloTotale() : 0;
                            extraEff += other.getLivelloNonMaledetto() != null ? other.getLivelloNonMaledetto() : 0;
                        }
                    }

                    SpellBookDTO sb = generateSpellBookSezione(
                            classe.getClasse(), baseTot + extraTot, baseEff + extraEff, id, sez, false);
                    if (sb != null && !sb.getLivelli().isEmpty()) itemsDTO.getSpellbooks().add(sb);
                }
                itemsDTO.getClassi().add(itemMapper.toClasseDTO(classe));
                continue;
            }

            String spellBookId = classe.getClasse().getLabel(Constants.ITEM_LABEL_LISTA_INCANTESIMI);
            if (spellBookId != null) {
                if (!spellBookId.startsWith("+")) {
                    Integer livelloTotale = classe.getLivelloTotale();
                    Integer livelloNonMaledetto = classe.getLivelloNonMaledetto();
                    List<InfoClasseDTO> aggiunte = allPersonaggioItems.getLivelli().getClassi().stream()
                            .filter(x -> {
                                String label = x.getClasse().getLabel(Constants.ITEM_LABEL_LISTA_INCANTESIMI);
                                return label != null && label.contains("+" + spellBookId);
                            })
                            .toList();
                    for (InfoClasseDTO info : aggiunte) {
                        livelloTotale += info.getLivelloTotale();
                        livelloNonMaledetto += info.getLivelloNonMaledetto();
                    }
                    SpellBookDTO spellbook = generateSpellBook(classe.getClasse(), livelloTotale, livelloNonMaledetto, id);
                    if (spellbook != null) {
                        itemsDTO.getSpellbooks().add(spellbook);
                    }
                }
            }
            itemsDTO.getClassi().add(itemMapper.toClasseDTO(classe));
        }

        // Spellbook da item generici (non CLASSE/RAZZA): stesse label SPELL_<n>* delle classi, ma
        // slot/conosciuti sono un numero fisso (l'item non ha livelli), niente progressione.
        for (Item itm : allPersonaggioItems.getItems()) {
            if (TipoItem.CLASSE.equals(itm.getTipo()) || TipoItem.RAZZA.equals(itm.getTipo())) continue;
            if (itemMapper.isDisabled(itm)) continue;
            if (separatoContainerIdByItem.containsKey(itm.getId())) continue; // carico, non dotazione
            for (SezioneIncantesimi sez : parseSezioniIncantesimi(itm)) {
                // sezione personalizzata: liste vuota è normale (nessun filtro per lista), non va saltata
                if (!sez.personalizzata() && sez.liste().stream().allMatch(l -> l != null && l.startsWith("+"))) continue;
                SpellBookDTO sb = generateSpellBookSezione(itm, 0, 0, id, sez, true);
                if (sb != null && !sb.getLivelli().isEmpty()) itemsDTO.getSpellbooks().add(sb);
            }
        }

        // Arricchisce ogni ItemDTO con i suoi figli ATTACCO (es. armi) e raggruppa le trasformazioni
        // — precalcolati qui in UNA query batch così il frontend non deve fare chiamate di dettaglio
        // separate (e non "rimescola" la UI dopo il primo render). NB: niente itm.getChild() qui,
        // perché per gli item non-root del flatten non è eager-fetched e scatenerebbe un lazy-load
        // per item.
        Map<Integer, List<ChildRefDTO>> figliAttacchiMap = new HashMap<>();
        Map<Integer, List<ChildRefDTO>> figliFruttoMap = new HashMap<>();
        if (!allItemIds.isEmpty()) {
            for (Object[] row : collegamentoRepository.findFigliByTipo(allItemIds,
                    List.of(TipoItem.ATTACCO, TipoItem.TRASFORMAZIONE, TipoItem.FORMA))) {
                Integer sourceId = (Integer) row[0];
                ChildRefDTO ref = new ChildRefDTO((Integer) row[1], (String) row[2], (TipoItem) row[3]);
                Map<Integer, List<ChildRefDTO>> target = TipoItem.ATTACCO.equals(ref.getTipo())
                        ? figliAttacchiMap : figliFruttoMap;
                target.computeIfAbsent(sourceId, k -> new ArrayList<>()).add(ref);
            }
            for (Collegamento c : collegamentoRepository.findEffettiByItemSourceIds(allItemIds)) {
                // Lo stato disabilitato di un EFFETTO viene scritto da switchItemState sul
                // COLLEGAMENTO (oggetto base -> effetto), non sull'item stesso: va letto da lì.
                boolean effettoDisabled = utilService.parseBooleanFromString(
                        c.getLabel(Constants.ITEM_LABEL_DISABILITATO),
                        Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE,
                        Constants.ITEM_LABEL_DISABILITATO_VALORE_FALSE);
                itemsDTO.getEffetti().add(new EffettoDTO(
                        c.getItemSource().getId(), c.getItemSource().getNome(),
                        c.getItemTarget().getId(), c.getItemTarget().getNome(),
                        c.getLabel(Constants.COLLEGAMENTO_LABEL_CONDIZIONE),
                        effettoDisabled));
            }
        }
        Stream.of(itemsDTO.getAbilita(), itemsDTO.getTalenti(), itemsDTO.getOggetti(), itemsDTO.getConsumabili(),
                        itemsDTO.getArmi(), itemsDTO.getMunizioni(), itemsDTO.getEquipaggiamento(),
                        itemsDTO.getClassi(), itemsDTO.getRazze(), itemsDTO.getMaledizioni(), itemsDTO.getCompetenze(),
                        itemsDTO.getLingue(), itemsDTO.getIdoli(), itemsDTO.getContenitori(),
                        itemsDTO.getForme(), itemsDTO.getPrivilegi(), itemsDTO.getAltro(), itemsDTO.getNotizie(),
                        itemsDTO.getPatti(), itemsDTO.getVeicoli())
                .flatMap(List::stream)
                .forEach(dto -> {
                    dto.setFigliAttacchi(figliAttacchiMap.getOrDefault(dto.getId(), List.of()));
                    // Item concessi da ADD_CLASSE_<n>_ITEMS: nessun vero Collegamento parent, quindi
                    // il prefisso (se il carrier lo imposta) va applicato qui, non da ItemMapper.
                    if (dto.getPrefissoOggetti() == null) {
                        String prefissoVirtuale = allPersonaggioItems.getPrefissoVirtualeById().get(dto.getId());
                        if (prefissoVirtuale != null) dto.setPrefissoOggetti(prefissoVirtuale);
                    }
                });
        // I FRUTTO sono List<FruttoDTO> (tipo diverso da List<ItemDTO>): gestiti a parte.
        itemsDTO.getFrutti().forEach(dto -> dto.setFigliAttacchi(figliAttacchiMap.getOrDefault(dto.getId(), List.of())));

        // Raggruppa le trasformazioni: per ogni FRUTTO, le sue forme (gruppo esplicito "FORMA") e le
        // sue trasformazioni figlie (gruppo naturale) vanno in FruttoDTO.trasformazioni; il resto
        // (trasformazioni indipendenti, non figlie di alcun frutto) va in itemsDTO.trasformazioni,
        // raggruppato allo stesso modo.
        Map<Integer, ItemDTO> formeById = itemsDTO.getForme().stream()
                .collect(Collectors.toMap(ItemDTO::getId, x -> x, (a, b) -> a));
        Map<Integer, TrasformazioneDTO> trasfById = flatTrasformazioni.stream()
                .collect(Collectors.toMap(TrasformazioneDTO::getId, x -> x, (a, b) -> a));
        Set<Integer> trasfClaimateDaFrutti = new HashSet<>();

        for (FruttoDTO frutto : itemsDTO.getFrutti()) {
            List<ChildRefDTO> figli = figliFruttoMap.getOrDefault(frutto.getId(), List.of());
            List<TrasformazioneDTO> formaGroup = new ArrayList<>();
            Map<String, List<TrasformazioneDTO>> trasfGroups = new LinkedHashMap<>();
            for (ChildRefDTO ref : figli) {
                if (TipoItem.FORMA.equals(ref.getTipo())) {
                    ItemDTO forma = formeById.get(ref.getId());
                    if (forma == null) continue;
                    TrasformazioneDTO asTrasf = new TrasformazioneDTO();
                    asTrasf.setId(forma.getId());
                    asTrasf.setNome(forma.getNome());
                    asTrasf.setTipo(forma.getTipo());
                    asTrasf.setDisabled(forma.getDisabled());
                    asTrasf.setGruppo("FORMA");
                    formaGroup.add(asTrasf);
                } else if (TipoItem.TRASFORMAZIONE.equals(ref.getTipo())) {
                    TrasformazioneDTO trasf = trasfById.get(ref.getId());
                    if (trasf == null) continue;
                    trasfClaimateDaFrutti.add(trasf.getId());
                    String g = trasf.getGruppo() != null ? trasf.getGruppo() : "";
                    trasfGroups.computeIfAbsent(g, k -> new ArrayList<>()).add(trasf);
                }
            }
            List<GruppoTrasformazioniDTO> gruppi = new ArrayList<>();
            if (!formaGroup.isEmpty()) {
                formaGroup.sort(Comparator.comparing(TrasformazioneDTO::getNome, String.CASE_INSENSITIVE_ORDER));
                gruppi.add(new GruppoTrasformazioniDTO("FORMA", formaGroup));
            }
            trasfGroups.forEach((g, list) -> {
                list.sort(Comparator.comparing(TrasformazioneDTO::getNome, String.CASE_INSENSITIVE_ORDER));
                gruppi.add(new GruppoTrasformazioniDTO(g, list));
            });
            frutto.setTrasformazioni(gruppi);
        }

        // Trasformazioni indipendenti (non figlie di alcun frutto), raggruppate per "gruppo"
        Map<String, List<TrasformazioneDTO>> gruppiIndipendenti = flatTrasformazioni.stream()
                .filter(t -> !trasfClaimateDaFrutti.contains(t.getId()))
                .collect(Collectors.groupingBy(
                        t -> t.getGruppo() != null ? t.getGruppo() : "",
                        LinkedHashMap::new,
                        Collectors.toList()));
        gruppiIndipendenti.forEach((g, list) -> {
            list.sort(Comparator.comparing(TrasformazioneDTO::getNome, String.CASE_INSENSITIVE_ORDER));
            itemsDTO.getTrasformazioni().add(new GruppoTrasformazioniDTO(g, list));
        });

        return itemsDTO;
    }

    /**
     * Ricerca "profonda" tra TUTTI gli item di un personaggio (qualsiasi tipo: privilegi,
     * razza, abilità, talenti, competenze, oggetti…). Il match avviene su nome, sul valore
     * di una qualsiasi label o sulla nota di un qualsiasi modificatore.
     */
    public List<ItemSearchResultDTO> searchItemsPersonaggio(Integer idPersonaggio, String q) {
        if (q == null || q.trim().isEmpty()) return List.of();
        String needle = q.trim().toLowerCase();
        Personaggio pg = personaggioRepository.findPersonaggioById(idPersonaggio);
        String pgNome = pg != null ? pg.getNome() : null;

        List<Item> items = getAllPersonaggioItemsByIdPersonaggio(idPersonaggio).getItems();
        Map<Integer, Item> unici = new LinkedHashMap<>();
        for (Item it : items) if (it.getId() != null) unici.putIfAbsent(it.getId(), it);

        List<ItemSearchResultDTO> out = new ArrayList<>();
        for (Item it : unici.values()) {
            String match = matchItem(it, needle);
            if (match != null) {
                out.add(new ItemSearchResultDTO(
                        it.getId(), it.getNome(),
                        it.getTipo() != null ? it.getTipo().name() : null,
                        idPersonaggio, pgNome, match,
                        Boolean.TRUE.equals(it.isDisabled())));
            }
        }
        return out;
    }

    /** Ricerca profonda su tutti i personaggi di un party. */
    public List<ItemSearchResultDTO> searchItemsParty(Integer partyId, String q) {
        if (q == null || q.trim().isEmpty()) return List.of();
        List<ItemSearchResultDTO> out = new ArrayList<>();
        for (Personaggio pg : personaggioRepository.findAllByParty_IdOrderByNomeAsc(partyId)) {
            out.addAll(searchItemsPersonaggio(pg.getId(), q));
        }
        return out;
    }

    /**
     * Saghe (milestone) necessarie per salire di livello, in base al livello attuale.
     * ≤10:1, 11-13:3, 14-16:4, 17-18:5, 19-20:6, 21-25:7, 26-30:8; poi +1 ogni 5 livelli.
     */
    public static int saghePerLivello(int livello) {
        if (livello <= 10) return 1;
        if (livello <= 13) return 3;
        if (livello <= 16) return 4;
        if (livello <= 18) return 5;
        if (livello <= 20) return 6;
        if (livello <= 25) return 7;
        if (livello <= 30) return 8;
        return 8 + (int) Math.ceil((livello - 30) / 5.0);
    }

    private static String matchItem(Item it, String needle) {
        if (it.getNome() != null && it.getNome().toLowerCase().contains(needle)) return "nome";
        if (it.getLabels() != null) {
            for (ItemLabel l : it.getLabels()) {
                if (l.getValore() != null && l.getValore().toLowerCase().contains(needle))
                    return "label " + l.getLabel();
            }
        }
        if (it.getModificatori() != null) {
            for (Modificatore m : it.getModificatori()) {
                if (m.getNota() != null && m.getNota().toLowerCase().contains(needle))
                    return "nota";
            }
        }
        return null;
    }

    public AllPersonaggioItems getAllPersonaggioItemsByIdPersonaggio(Integer idPersonaggio) {
        AllPersonaggioItems result = new AllPersonaggioItems();

        // Fetch Items principali con un solo join-fetch su child
        List<Item> initialRoots = itemRepository.findAllByPersonaggioIdWithChild(idPersonaggio);

        // Determina le classi e livelli dai soli rootItems
        result.setLivelli(calcoloService.getLivelli(initialRoots));

        // Raccogli root items aggiuntive dagli avanzamenti di classe
        List<Item> advanceRoots = new ArrayList<>();

        for (InfoClasseDTO classe : result.getLivelli().getClassi()) {
            Item entity = classe.getClasse();
            Set<Integer> livelliClasse = classe.getLivelli();
            if (entity != null && livelliClasse != null && !livelliClasse.isEmpty()) {
                advanceRoots.add(entity);
            }
        }

        result.setItems(
                flattenItems(
                        Stream.of(initialRoots, advanceRoots)
                                .filter(obj -> true)
                                .flatMap(Collection::stream)
                                .toList(),
                        result.getFruttiSenzaMod()
                )
        );

        // Livelli di classe extra concessi dagli item (label ADD_CLASSE_<n> = +1 livello,
        // valore = id della classe). Aggiornano solo le InfoClasseDTO della classe.
        applyAddClasseLevels(result);

        return result;
    }

    /**
     * Aggiunge alle classi i livelli extra concessi dagli item del personaggio.
     * Le label sono indicizzate a triplette per indice {@code <n>}:
     * <ul>
     *   <li>{@code ADD_CLASSE_<n>} = id della classe;</li>
     *   <li>{@code ADD_CLASSE_<n>_VALUE} = numero di livelli da aggiungere;</li>
     *   <li>{@code ADD_CLASSE_<n>_ITEMS} = "1": concede anche i Privilegi di Classe (Avanzamento
     *       CLASSE->item, esclusi i template AVANZAMENTO) dei livelli virtuali aggiunti — calcolo a
     *       runtime, mai persistito, appaiono nell'inventario come item veri;</li>
     *   <li>{@code ADD_CLASSE_<n>_BONUS} = "1": concede anche BAB/Tempra/Riflessi/Volontà dei
     *       livelli virtuali aggiunti, leggendo gli item template AVANZAMENTO della classe
     *       (stessa fonte della Tabella livelli), cappati al numero di livelli reale della classe.</li>
     * </ul>
     * Gli item disabilitati non concedono livelli. I livelli aggiunti contano come non maledetti
     * (validi per gli incantesimi). Più item possono aggiungere livelli alla stessa classe: si
     * sommano, ognuno sul range di livelli subito successivo a quello già raggiunto.
     */
    private void applyAddClasseLevels(AllPersonaggioItems result) {
        final String prefix = Constants.ITEM_LABEL_ADD_CLASSE_PREFIX;
        final String suffixValore = Constants.ITEM_LABEL_ADD_CLASSE_VALUE_SUFFIX;
        final String suffixValoreAlias = Constants.ITEM_LABEL_ADD_CLASSE_VALUE_SUFFIX_ALIAS;
        final String suffixItems = Constants.ITEM_LABEL_ADD_CLASSE_ITEMS_SUFFIX;
        final String suffixBonus = Constants.ITEM_LABEL_ADD_CLASSE_BONUS_SUFFIX;

        List<InfoClasseDTO> classi = new ArrayList<>(result.getLivelli().getClassi());
        // "prossimo livello libero" per classe: parte dal max reale, cresce con ogni item che
        // aggiunge livelli virtuali (così più item sulla stessa classe si sommano su range distinti)
        Map<Integer, Integer> prossimoLivello = new HashMap<>();
        List<Item> daAggiungereAlFlatten = new ArrayList<>();
        boolean trovatoAlmenoUno = false;

        for (Item itm : result.getItems()) {
            if (itm == null || itm.getLabels() == null) continue;
            boolean disabled = utilService.parseBooleanFromString(
                    itm.getLabel(Constants.ITEM_LABEL_DISABILITATO),
                    Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE,
                    Constants.ITEM_LABEL_DISABILITATO_VALORE_FALSE);
            if (disabled) continue;

            for (ItemLabel l : itm.getLabels()) {
                String key = l.getLabel();
                if (key == null || !key.startsWith(prefix)) continue;
                String rest = key.substring(prefix.length());
                if (rest.endsWith(suffixValore) || rest.endsWith(suffixValoreAlias)
                        || rest.endsWith(suffixItems) || rest.endsWith(suffixBonus)) continue;

                Integer idClasse = parseIntOrNull(l.getValore());
                if (idClasse == null) continue;
                String livelliString = itm.getLabel(prefix + rest + suffixValore);
                if (livelliString == null) livelliString = itm.getLabel(prefix + rest + suffixValoreAlias);
                Integer extra = livelliString != null ? parseIntOrNull(livelliString) : null;
                if (extra == null || extra <= 0) continue;
                trovatoAlmenoUno = true;

                InfoClasseDTO info = classi.stream()
                        .filter(c -> c.getClasse() != null && idClasse.equals(c.getClasse().getId()))
                        .findFirst()
                        .orElse(null);
                if (info == null) {
                    // la classe non ha ancora livelli base: creane una nuova
                    Item classe = itemRepository.findItemById(idClasse);
                    if (classe == null) continue;
                    info = new InfoClasseDTO();
                    info.setClasse(classe);
                    info.setLivelli(new HashSet<>());
                    info.setLivelloTotale(0);
                    info.setLivelloNonMaledetto(0);
                    info.setLivelloMax(0);
                    info.setLivelloMaxNonMaledetto(0);
                    classi.add(info);
                }

                int start = prossimoLivello.getOrDefault(idClasse, info.getLivelloMax() != null ? info.getLivelloMax() : 0) + 1;
                int end = start + extra - 1;
                prossimoLivello.put(idClasse, end);

                Set<Integer> livelli = info.getLivelli() != null ? new HashSet<>(info.getLivelli()) : new HashSet<>();
                for (int lv = start; lv <= end; lv++) livelli.add(lv);
                info.setLivelli(livelli);
                info.setLivelloTotale((info.getLivelloTotale() != null ? info.getLivelloTotale() : 0) + extra);
                info.setLivelloNonMaledetto((info.getLivelloNonMaledetto() != null ? info.getLivelloNonMaledetto() : 0) + extra);
                info.setLivelloMax(end);
                info.setLivelloMaxNonMaledetto((info.getLivelloMaxNonMaledetto() != null ? info.getLivelloMaxNonMaledetto() : 0) + extra);

                boolean grantItems = "1".equals(itm.getLabel(prefix + rest + suffixItems));
                boolean grantBonus = "1".equals(itm.getLabel(prefix + rest + suffixBonus));
                if (!grantItems && !grantBonus) continue;

                Item classe = info.getClasse();
                if (classe == null || classe.getAvanzamento() == null) continue;
                Integer numLivelliClasse = parseIntOrNull(classe.getLabel(Constants.ITEM_LABEL_NUM_LIVELLI_CLASSE));
                int cappedEnd = Math.min(end, numLivelliClasse != null ? numLivelliClasse : 20);

                if (grantItems) {
                    // Privilegi: si accumulano un livello alla volta, vanno concessi tutti quelli
                    // nel range (a differenza di BAB/TS sotto, non sono valori cumulativi).
                    // L'item carrier "diventa" il genitore virtuale ai fini del chip prefisso.
                    String prefissoCarrier = itm.getLabel(Constants.ITEM_LABEL_PREFISSO_OGGETTI);
                    for (Avanzamento av : classe.getAvanzamento()) {
                        int lv = av.getLivello();
                        if (lv < start || lv > cappedEnd) continue;
                        Item target = av.getItemTarget();
                        if (target != null && !TipoItem.AVANZAMENTO.equals(target.getTipo())) {
                            daAggiungereAlFlatten.add(target);
                            if (prefissoCarrier != null && !prefissoCarrier.isBlank()) {
                                result.getPrefissoVirtualeById().put(target.getId(), prefissoCarrier.trim());
                            }
                        }
                    }
                }
                if (grantBonus) {
                    // BAB/Tempra/Riflessi/Volontà sono valori CUMULATIVI per livello (la riga del
                    // livello N contiene già il totale fino a N, non un incremento): va preso solo
                    // il template del livello più alto nel range concesso, non la somma di tutti.
                    Item migliore = classe.getAvanzamento().stream()
                            .filter(av -> av.getLivello() >= start && av.getLivello() <= cappedEnd)
                            .filter(av -> av.getItemTarget() != null && TipoItem.AVANZAMENTO.equals(av.getItemTarget().getTipo()))
                            .max(Comparator.comparingInt(Avanzamento::getLivello))
                            .map(Avanzamento::getItemTarget)
                            .orElse(null);
                    if (migliore != null) daAggiungereAlFlatten.add(migliore);
                }
            }
        }

        if (!daAggiungereAlFlatten.isEmpty()) {
            result.getItems().addAll(daAggiungereAlFlatten);
        }
        if (!trovatoAlmenoUno) return;

        // riordina per nome classe, coerente con CalcoloService.getLivelli
        classi.sort(Comparator.comparing(
                c -> c.getClasse() != null ? c.getClasse().getNome() : null,
                Comparator.nullsLast(String::compareToIgnoreCase)));
        result.getLivelli().setClassi(classi);
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }


    /**
     * Ottiene i dati del personaggio, includendo items da avanzamenti di classe in base al livello.
     * Unifica i flatten in un solo passaggio per migliorare le prestazioni. Cache-aware: legge da
     * cache se presente, senza pre-scaldare gli items (non conosce l'utente qui — vedi l'overload
     * con Utente usato da /modificatori per quello).
     */
    public DatiPersonaggioDTO getDatiPersonaggio(Integer id) {
        Cache modCache = cacheManager.getCache(CACHE_MODIFICATORI);
        if (modCache != null) {
            DatiPersonaggioDTO cached = modCache.get(id, DatiPersonaggioDTO.class);
            if (cached != null) return cached;
        }
        DatiPersonaggioDTO dati = getDatiPersonaggio(id, getAllPersonaggioItemsByIdPersonaggio(id));
        if (modCache != null) modCache.put(id, dati);
        return dati;
    }

    /**
     * Punto d'ingresso cache-aware per /modificatori: come sopra, ma con l'albero item ancora
     * "caldo" in questa stessa richiesta pre-scalda ANCHE la cache items per lo stesso utente —
     * così se /items viene chiamato subito dopo (come fa il frontend ad ogni caricamento
     * personaggio) trova già tutto pronto invece di rifare da zero la stessa traversata costosa.
     */
    public DatiPersonaggioDTO getDatiPersonaggio(Integer id, Utente utente) {
        Cache modCache = cacheManager.getCache(CACHE_MODIFICATORI);
        if (modCache != null) {
            DatiPersonaggioDTO cached = modCache.get(id, DatiPersonaggioDTO.class);
            if (cached != null) return cached;
        }

        AllPersonaggioItems allPersonaggioItems = getAllPersonaggioItemsByIdPersonaggio(id);
        DatiPersonaggioDTO dati = getDatiPersonaggio(id, allPersonaggioItems);
        if (modCache != null) modCache.put(id, dati);

        if (utente != null) {
            Cache itemsCache = cacheManager.getCache(CACHE_ITEMS);
            String key = itemsCacheKey(id, utente);
            if (itemsCache != null && itemsCache.get(key) == null) {
                ItemsDTO items = getAllPersonaggioItemsDTOByIdPersonaggio(id, utente, dati.getUtilizziTotaleFormula(), allPersonaggioItems);
                itemsCache.put(key, items);
            }
        }
        return dati;
    }

    /**
     * Overload che accetta il flatten (AllPersonaggioItems) già calcolato dal chiamante,
     * per evitare di ripeterlo quando è già disponibile (es. endpoint /items, che lo
     * calcola una volta sola e lo condivide con getAllPersonaggioItemsDTOByIdPersonaggio).
     */
    public DatiPersonaggioDTO getDatiPersonaggio(Integer id, AllPersonaggioItems allPersonaggioItemsSheet) {
        // 1) Carica Personaggio base
        Personaggio p = personaggioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Personaggio non trovato"));

        List<Item> allItems = allPersonaggioItemsSheet.getItems();
        Set<Integer> fruttiSenzaModSheet = allPersonaggioItemsSheet.getFruttiSenzaMod();
        // Item dentro un CONTENITORE "separato" (es. la Stiva di una NAVE): carico, non dotazione,
        // esclusi anche se abilitati — come se non fossero collegati al personaggio (il peso
        // continua comunque a contare, sommato più sotto in modo diretto).
        Map<Integer, Integer> mappaContenitoriSeparati = mappaContenitoriSeparati(allItems);
        Set<Integer> dentroContenitoriSeparati = mappaContenitoriSeparati.keySet();
        List<Item> filteredItems = new ArrayList<>();
        for (Item item : allItems) {
            if (dentroContenitoriSeparati.contains(item.getId())) continue;
            boolean isDisabled = false;
            for (ItemLabel lbl : item.getLabels()) {
                if (Constants.ITEM_LABEL_DISABILITATO.equalsIgnoreCase(lbl.getLabel()) && Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE.equals(lbl.getValore())) {
                    isDisabled = true;
                    break;
                }
            }
            if (!isDisabled) {
                filteredItems.add(item);
            }
        }
        List<Item> livelloItems = filteredItems.stream().filter(x -> x.getTipo().equals(TipoItem.LIVELLO)).toList();
        Integer livelloPersonaggio = livelloItems.size() - 1;
        Map<String, List<AbilitaClasseDTO>> abilitaClassePerLivello = new HashMap<>();
        // Le classi uniche (con relativo accesso al DB) sono identiche per ogni livello dello stesso
        // personaggio: calcolate UNA SOLA VOLTA invece di essere rifatte ad ogni iterazione.
        List<Item> classiUnicheDaLivelli = livelloItems.isEmpty()
                ? List.of()
                : modificatoriService.getClassiUnicheDaLivelli(id);
        for (Item livello : livelloItems) {
            try {
                abilitaClassePerLivello.put(
                        livello.getLabel(Constants.ITEM_LIVELLO_LVL),
                        modificatoriService.getAbilitaClasse(
                                classiUnicheDaLivelli,
                                Integer.parseInt(livello.getLabel(Constants.ITEM_LABEL_CLASSE)))
                );
            } catch (Exception ignored) {
            }
        }

        // 6) Fetch Modificatori (escludi i FRUTTO la cui scelta non include MOD)
        List<Integer> itemIds = filteredItems.stream().map(Item::getId).toList();
        List<Integer> itemIdsPerMod = fruttiSenzaModSheet.isEmpty()
                ? itemIds
                : filteredItems.stream().map(Item::getId)
                    .filter(iid -> !fruttiSenzaModSheet.contains(iid)).toList();
        List<Modificatore> allMods = modificatoreRepository.findAllByItemIdIn(itemIdsPerMod);
        List<Modificatore> modificatoriPerLivello = elaboraModificatoriStatLivello(allMods);
        allMods = allMods.stream()
                .filter(x -> !(x.getItem().getTipo().equals(TipoItem.LIVELLO)
                        && Constants.listOfUniqueByClassStats.contains(x.getStat().getId())))
                .toList();

        allMods = Stream.concat(allMods.stream(), modificatoriPerLivello.stream())
                .toList();

        List<ItemLabel> taglia = new ArrayList<>(itemLabelRepository.findByLabelAndItem_IdIn(Constants.ITEM_LABEL_TAGLIA, itemIds));
        // ADD_TAGLIA: incrementi/decrementi della taglia base da item attivi
        taglia.addAll(itemLabelRepository.findByLabelAndItem_IdIn(Constants.ITEM_LABEL_ADD_TAGLIA, itemIds));
        // SET manuale del personaggio (personaggio_label TAGLIA), come label senza item
        if (p.getLabels() != null) {
            p.getLabels().stream()
                    .filter(l -> Constants.ITEM_LABEL_TAGLIA.equals(l.getLabel())
                            && l.getValore() != null && !l.getValore().isBlank())
                    .findFirst()
                    .ifPresent(l -> taglia.add(new ItemLabel(null, null, Constants.ITEM_LABEL_TAGLIA, l.getValore(), null)));
        }

        // Cache id-classe -> Item per i modificatori dei LIVELLO (evita una query findById
        // per OGNI modificatore di tipo LIVELLO nel mapper: ne possono esserci molti).
        Map<Integer, Item> classiCacheModificatori = livelloItems.isEmpty()
                ? Map.of()
                : itemRepository.findItemsByIds(
                        livelloItems.stream()
                                .map(x -> x.getLabel(Constants.ITEM_LABEL_CLASSE))
                                .filter(Objects::nonNull)
                                .map(PersonaggioService::parseIntOrNull)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList()
                ).stream().collect(Collectors.toMap(Item::getId, x -> x, (a, b) -> a));

        // 7) Raggruppa Modificatori e Rank in DTO
        Map<String, List<ModificatoreDTO>> modsDtoByStat = allMods.stream()
                .filter(m -> !TipoModificatore.RANK.equals(m.getTipo()))
                .collect(Collectors.groupingBy(
                        m -> m.getStat().getId(),
                        Collectors.mapping(m -> modificatoreMapper.toDTO(m, classiCacheModificatori), Collectors.toList())
                ));
        Map<String, List<RankDTO>> ranksDtoByStat = allMods.stream()
                .filter(m -> TipoModificatore.RANK.equals(m.getTipo()))
                .collect(Collectors.groupingBy(
                        m -> m.getStat().getId(),
                        Collectors.mapping(x -> modificatoreMapper.toRankDTO(x, abilitaClassePerLivello, livelloPersonaggio), Collectors.toList())
                ));

        // 8) Fetch StatValues con join fetch di Stat
        List<StatValue> stats = statValueRepository.findAllByPersonaggioIdWithStat(id);

        // 9) Costruisci DTO
        DatiPersonaggioDTO dto = new DatiPersonaggioDTO(p);

        List<ItemLabel> itemCounters = itemLabelRepository.findByLabelLikeAndItem_IdIn("$V_%", itemIds);
        List<ItemLabel> itemModifiers = itemLabelRepository.findByLabelLikeAndItem_IdIn("$M_%", itemIds);
        List<ContatoreItemDTO> itemCounterList = new ArrayList<>(itemCounters.stream()
                .map(sv -> modificatoriService.calcolaContatoreItem(sv, itemModifiers))
                .toList());

        // Espone la quantità (label QTA) come variabile $QTA usabile nei modificatori dell'item.
        // Priorità: QTA per-personaggio > QTA globale dell'item.
        Map<Integer, Integer> qtaByItem = new HashMap<>();
        for (ItemLabel q : itemLabelRepository.findByLabelAndItem_IdInAndPersonaggio_Id(Constants.LABEL_QTA, itemIds, id)) {
            Integer v = parseIntOrNull(q.getValore());
            if (q.getItem() != null && v != null) qtaByItem.put(q.getItem().getId(), v);
        }
        List<Integer> itemIdsSenzaQta = itemIds.stream().filter(iid -> !qtaByItem.containsKey(iid)).toList();
        if (!itemIdsSenzaQta.isEmpty()) {
            for (ItemLabel q : itemLabelRepository.findByLabelAndItem_IdInAndPersonaggioIsNull(Constants.LABEL_QTA, itemIdsSenzaQta)) {
                Integer v = parseIntOrNull(q.getValore());
                if (q.getItem() != null && v != null) qtaByItem.put(q.getItem().getId(), v);
            }
        }
        for (Integer iid : itemIds) {
            itemCounterList.add(new ContatoreItemDTO(iid + "_QTA", qtaByItem.getOrDefault(iid, 1)));
        }

        // Info anagrafiche + peso totale, esposti anche come variabili nelle formule
        // (@PESO, @ETA, @ALTEZZA, @PESO_TOTALE) tramite la lista dei contatori item.
        Map<String, String> info = new LinkedHashMap<>();
        if (p.getLabels() != null) {
            for (PersonaggioLabel l : p.getLabels()) {
                if (Constants.PERSONAGGIO_INFO_LABELS.contains(l.getLabel())) {
                    info.put(l.getLabel(), l.getValore());
                } else if (Constants.LABEL_TIPO_PERSONAGGIO.equals(l.getLabel())) {
                    dto.setTipoPersonaggio(l.getValore());
                }
            }
        }
        // MILESTONE_TO (saghe per salire di livello) è CALCOLATA dal livello (label LIVELLO),
        // non impostata a mano: sovrascrive l'eventuale valore salvato.
        int livAtteso = 1;
        try {
            String lv = info.get(Constants.LABEL_LIVELLO);
            if (lv != null && !lv.isBlank()) livAtteso = Integer.parseInt(lv.trim());
        } catch (NumberFormatException ignored) {}
        info.put(Constants.LABEL_MILESTONE_TO, String.valueOf(saghePerLivello(livAtteso)));
        // Il peso è completamente indipendente dal flag INVENTARIO_SEPARATO (quel flag è solo
        // display/meccanica, vedi mappaContenitoriSeparati/filteredItems sopra): calcolaPeso vede
        // TUTTI gli item raggiunti dal flatten, Stiva e suo contenuto inclusi, e li tratta come
        // qualunque altro item/contenitore del personaggio con la stessa identica euristica.
        double pesoTotale = partyService.calcolaPeso(p, allItems.stream().map(Item::getId).toList());
        double pesoMonete = partyService.calcolaPesoMonete(partyService.calcolaSoldi(p.getId()));
        dto.setPesoMonete(Math.round(pesoMonete * 100) / 100.0);
        CalcoloService.variabiliPersonaggio(info, pesoTotale)
                .forEach((k, v) -> itemCounterList.add(new ContatoreItemDTO(k, v)));

        dto.getContatoriItem().addAll(itemCounterList);

        // "variabili": raccoglie $id/@id per risolvere le formule di TUTTI i calcola* che seguono,
        // costruita una sola volta (non più ricostruita identica dentro ognuno). Le caratteristiche
        // non esistono ancora a questo punto (vengono calcolate subito sotto): le loro entry "@id"
        // vengono aggiunte alla STESSA istanza non appena carList è pronta. La taglia (che serve a
        // calcolaCaratteristica/calcolaClasseArmatura/calcolaBonusAttacco) è calcolata QUI, fuori da
        // quei metodi, una volta sola, e scritta subito in variabili invece di essere ricalcolata
        // identica in ciascuno.
        VariabiliDTO variabili = modificatoriService.costruisciVariabili(itemCounterList, List.of());
        int tagliaPerVariabili = modificatoriService.getTaglia(taglia);
        int tagliaBasePerVariabili = modificatoriService.getTagliaBase(taglia);
        variabili.set(Constants.VARIABILE_TAGLIA, tagliaPerVariabili);
        variabili.set(Constants.VARIABILE_TAGLIA_BASE, tagliaBasePerVariabili);
        variabili.set(Constants.VARIABILE_DIFFERENZA_TAGLIA, tagliaPerVariabili - tagliaBasePerVariabili);

        // Valuta formulaQty sui collegamenti usando i contatori già calcolati
        List<Collegamento> conFormula = collegamentoRepository.findWithFormulaQty(itemIds, itemIds);
        if (!conFormula.isEmpty()) {
            List<CaratteristicaDTO> counterCarList = itemCounterList.stream()
                    .map(ContatoreItemDTO::toCaratteristicaDTO)
                    .collect(Collectors.toList());
            for (Collegamento c : conFormula) {
                try {
                    String formula = c.getFormulaQty().replace("$", "@" + c.getItemSource().getId() + "_");
                    int val = Integer.parseInt(calcoloService.calcola(formula, counterCarList));
                    dto.getUtilizziTotaleFormula().merge(c.getItemTarget().getId(), val, Integer::sum);
                } catch (Exception ignored) {}
            }
        }

        // 9a) Calcolo Caratteristiche (sequenziale)
        List<CaratteristicaDTO> carList = new ArrayList<>(stats.stream()
                .filter(sv -> TipoStat.CAR.equals(sv.getStat().getTipo()))
                .map(sv -> modificatoriService.calcolaCaratteristica(
                        sv,
                        modsDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                        variabili
                ))
                .toList());
        // Rende LVL disponibile nelle formule delle statistiche
        carList.add(new CaratteristicaDTO("LVL", "Livello", null, livelloPersonaggio, null, null));
        dto.getCaratteristiche().addAll(carList);
        // Ora che le caratteristiche esistono, le loro entry "@id" entrano nella STESSA "variabili"
        // già in uso (non una mappa nuova): da qui in poi ogni calcola* le trova già pronte.
        variabili.setAll(carList.stream()
                .collect(Collectors.toMap(x -> "@".concat(x.getId()), x -> x.getModificatore().toString(), (a, b) -> a)));

        Optional<DadiVitaDTO> dvOpt = stats.stream()
                .filter(sv -> TipoStat.ATT.equals(sv.getStat().getTipo()) && "DV".equals(sv.getStat().getId()))
                .findFirst()
                .map(sv -> modificatoriService.calcolaDadiVita(
                        sv,
                        modsDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                        carList,
                        livelloItems,
                        variabili
                ));
        dvOpt.ifPresent(dto::setDadiVita);

        // 9b) Calcolo parallelo di Tiri Salvezza e Abilità
        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        try {
            // CAMBIA_CARATTERISTICA globale sui tiri salvezza (stat fittizia "tutti i TS").
            // DTO condivisi tra i calcoli paralleli: fisso valore=0 qui così applicaCalcoli non li muta concorrentemente.
            List<ModificatoreDTO> cambiaTsGlobali = modsDtoByStat
                    .getOrDefault(Constants.STAT_TUTTI_TS, Collections.emptyList()).stream()
                    .filter(m -> TipoModificatore.CAMBIA_CARATTERISTICA.equals(m.getTipo()))
                    .toList();
            cambiaTsGlobali.forEach(m -> { if (m.getValore() == null) m.setValore(0); });
            List<TiroSalvezzaDTO> tsList = pool.submit(() ->
                    stats.stream()
                            .filter(sv -> TipoStat.TS.equals(sv.getStat().getTipo()))
                            .map(sv -> modificatoriService.calcoloTiroSalvezza(
                                    sv,
                                    modsDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                                    variabili,
                                    carList,
                                    cambiaTsGlobali
                            ))
                            .toList()
            ).get();
            dto.getTiriSalvezza().addAll(tsList);

            // CAMBIA_CARATTERISTICA globale: modificatori sulla stat fittizia "tutte le abilità".
            // Sono DTO condivisi tra i calcoli paralleli: fisso valore=0 qui (single-thread) così
            // applicaCalcoli non li muta concorrentemente (l'override usa la formula, non il valore).
            List<ModificatoreDTO> cambiaAbilitaGlobali = modsDtoByStat
                    .getOrDefault(Constants.STAT_TUTTE_ABILITA, Collections.emptyList()).stream()
                    .filter(m -> TipoModificatore.CAMBIA_CARATTERISTICA.equals(m.getTipo()))
                    .toList();
            cambiaAbilitaGlobali.forEach(m -> { if (m.getValore() == null) m.setValore(0); });
            // Placeholder di famiglia (AB00/AR00/CO00/IN00): mai mostrati come abilità vere e proprie
            // (né in scheda né in Gestisci Gradi), valgono solo come bersaglio di Modificatore: un
            // Modificatore su AB00 si applica automaticamente a tutte le abilità semplici (e così via
            // per le altre famiglie), tramite placeholderPerAbilita().
            List<AbilitaDTO> abList = pool.submit(() ->
                    stats.stream()
                            .filter(sv -> TipoStat.AB.equals(sv.getStat().getTipo()))
                            .filter(sv -> !ModificatoriService.FAMIGLIA_GENERICA.containsKey(sv.getStat().getId()))
                            .map(sv -> {
                                String idStat = sv.getStat().getId();
                                List<ModificatoreDTO> modsAbilita = modsDtoByStat.getOrDefault(idStat, Collections.emptyList());
                                // le abilità non "rankable" non salgono di livello: non vanno considerate
                                // parte di "tutte le X" (placeholder di famiglia), né come bersaglio di
                                // Modificatore né come membro dell'espansione classe-abilità.
                                String placeholder = Boolean.FALSE.equals(sv.getStat().getRankable())
                                        ? null : ModificatoriService.placeholderPerAbilita(idStat);
                                if (placeholder != null) {
                                    List<ModificatoreDTO> modsFamiglia = modsDtoByStat.getOrDefault(placeholder, Collections.emptyList());
                                    if (!modsFamiglia.isEmpty()) {
                                        modsAbilita = Stream.concat(modsAbilita.stream(), modsFamiglia.stream()).toList();
                                    }
                                }
                                return modificatoriService.calcolaAbilita(
                                        sv,
                                        modsAbilita,
                                        ranksDtoByStat.getOrDefault(idStat, Collections.emptyList()),
                                        carList,
                                        cambiaAbilitaGlobali,
                                        variabili
                                );
                            })
                            .toList()
            ).get();
            dto.getAbilita().addAll(abList);

            List<ClasseArmaturaDTO> caList = pool.submit(() ->
                    stats.stream()
                            .filter(sv -> TipoStat.CA.equals(sv.getStat().getTipo()))
                            .map(sv -> modificatoriService.calcolaClasseArmatura(
                                    sv,
                                    modsDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                                    modsDtoByStat.getOrDefault("CA", Collections.emptyList()),
                                    carList, variabili
                            ))
                            .toList()
            ).get();
            dto.getClasseArmatura().addAll(caList);

            List<BonusAttaccoDTO> atkList = pool.submit(() ->
                    stats.stream()
                            .filter(sv -> TipoStat.ATK.equals(sv.getStat().getTipo()))
                            .map(sv -> modificatoriService.calcolaBonusAttacco(
                                    sv,
                                    modsDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                                    modsDtoByStat.getOrDefault("BAB", Collections.emptyList()),
                                    carList,
                                    variabili
                            ))
                            .toList()
            ).get();
            dto.getBonusAttacco().addAll(atkList);

            List<ContatoreDTO> countList = pool.submit(() ->
                    stats.stream()
                            .filter(sv -> TipoStat.COUNT.equals(sv.getStat().getTipo()))
                            .map(sv -> modificatoriService.calcolaContatore(
                                    sv,
                                    modsDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                                    carList,
                                    dto.getDadiVita(),
                                    livelloItems,
                                    variabili
                            ))
                            .toList()
            ).get();
            dto.getContatori().addAll(countList);

            List<AttributoDTO> attrList = pool.submit(() ->
                    stats.stream()
                            .filter(sv -> TipoStat.ATT.equals(sv.getStat().getTipo()) && !sv.getStat().getId().equals("GRADI") && !sv.getStat().getId().equals("DV"))
                            .map(sv -> modificatoriService.calcolaAttributo(
                                    sv,
                                    modsDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                                    carList,
                                    variabili
                            ))
                            .toList()
            ).get();
            dto.getAttributi().addAll(attrList);

        } catch (Exception e) {
            throw new RuntimeException("Errore nel calcolo parallelo", e);
        } finally {
            pool.shutdown();
        }

        modificatoriService.applicaSinergie(dto, carList);

        dto.setInfo(info);

        // Taglia: base dal personaggio, attuale con tutti i modificatori — già calcolata una volta
        // sola fuori dai calcola* e messa in "variabili" più sopra, qui solo riletta.
        int tagliaAttuale = variabili.getInt(Constants.VARIABILE_TAGLIA);
        int tagliaBase = variabili.getInt(Constants.VARIABILE_TAGLIA_BASE);
        dto.setTagliaAttuale(tagliaAttuale);
        dto.setTagliaBase(tagliaBase);

        // Il moltiplicatore taglia si applica SOLO al peso corporeo del personaggio (label PESO),
        // non agli item che porta. Il pesoTotale da calcolaPeso include già il peso corporeo.
        double pesoCorpo = p.getLabels() == null ? 0.0 : p.getLabels().stream()
                .filter(l -> Constants.LABEL_PESO.equals(l.getLabel()))
                .mapToDouble(l -> { try { return Double.parseDouble(l.getValore().replace(',', '.')); } catch (Exception e) { return 0; } })
                .sum();
        int diffTaglia = tagliaAttuale - tagliaBase;
        dto.setPesoSenzaTaglia(Math.round(pesoCorpo * 100) / 100.0);
        if (diffTaglia != 0) {
            double mult = Math.pow(8, Math.abs(diffTaglia));
            double pesoCorpoMod = diffTaglia > 0 ? pesoCorpo * mult : pesoCorpo / mult;
            pesoTotale = pesoTotale - pesoCorpo + pesoCorpoMod;
        }
        dto.setPesoTotale(Math.round(pesoTotale * 100) / 100.0);
        // Cache del peso effettivo in una personaggio_label, letta dalla lista party senza ricalcolare.
        // IMPORTANTE: si scrive in un EntityManager SEPARATO. Usare un repository @Transactional qui
        // farebbe il flush del contesto OSIV corrente, persistendo le mutazioni transitorie che
        // flattenItems applica agli item managed (setLabel(DISABLED,...)) e disabilitando in modo
        // permanente item che dovrebbero restare attivi.
        cachePesoEffettivo(p.getId(), String.valueOf(dto.getPesoTotale()));

        return dto;
    }

    /** Imposta/rimuove (value=null) una personaggio_label. Da salvare con save(p). */
    /**
     * Scrive/aggiorna la personaggio_label PESO_EFFETTIVO in un EntityManager DEDICATO e isolato,
     * con la sua transazione. Così il commit fa flush SOLO di questo contesto (vuoto), senza toccare
     * il contesto OSIV della richiesta corrente — dove flattenItems ha lasciato mutazioni transitorie
     * sugli item managed che NON devono essere persistite.
     */
    private void cachePesoEffettivo(Integer personaggioId, String valore) {
        jakarta.persistence.EntityManager em2 = entityManagerFactory.createEntityManager();
        jakarta.persistence.EntityTransaction tx = em2.getTransaction();
        try {
            tx.begin();
            int updated = em2.createQuery(
                            "UPDATE PersonaggioLabel l SET l.valore = :v WHERE l.personaggio.id = :pid AND l.label = :k")
                    .setParameter("v", valore)
                    .setParameter("pid", personaggioId)
                    .setParameter("k", Constants.LABEL_PESO_EFFETTIVO)
                    .executeUpdate();
            if (updated == 0) {
                PersonaggioLabel l = new PersonaggioLabel();
                l.setPersonaggio(em2.getReference(Personaggio.class, personaggioId));
                l.setLabel(Constants.LABEL_PESO_EFFETTIVO);
                l.setValore(valore);
                em2.persist(l);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                try { tx.rollback(); } catch (Exception ignored) {}
            }
        } finally {
            em2.close();
        }
    }

    /**
     * Aggiorna nome e info anagrafiche (personaggio_label) del personaggio.
     * Le label non presenti nella mappa vengono rimosse; quelle con valore
     * vuoto/null non vengono salvate.
     */
    @Transactional
    public void updateInfoPersonaggio(Integer id, String nome, Map<String, String> info) {
        Personaggio p = personaggioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Personaggio non trovato"));

        if (nome != null && !nome.isBlank()) {
            p.setNome(nome.trim());
        }

        if (p.getLabels() == null) {
            p.setLabels(new ArrayList<>());
        }

        // Valori desiderati (non vuoti) per le sole label anagrafiche gestite.
        Map<String, String> desiderate = new HashMap<>();
        if (info != null) {
            for (String key : Constants.PERSONAGGIO_INFO_LABELS) {
                String val = info.get(key);
                if (val != null && !val.isBlank()) desiderate.put(key, val.trim());
            }
        }

        // Aggiorna IN PLACE le label esistenti (niente remove+insert della stessa chiave,
        // che con il vincolo unico (id_personaggio, label) causerebbe una violazione perché
        // Hibernate esegue gli INSERT prima dei DELETE). Rimuove solo quelle svuotate.
        Set<String> gestite = new HashSet<>(Constants.PERSONAGGIO_INFO_LABELS);
        Set<String> giaPresenti = new HashSet<>();
        for (Iterator<PersonaggioLabel> it = p.getLabels().iterator(); it.hasNext(); ) {
            PersonaggioLabel l = it.next();
            if (!gestite.contains(l.getLabel())) continue;
            String nuovo = desiderate.get(l.getLabel());
            if (nuovo == null) {
                it.remove(); // valore cancellato → elimina (orphanRemoval)
            } else {
                l.setValore(nuovo); // aggiorna in place
                giaPresenti.add(l.getLabel());
            }
        }
        // Inserisce solo le chiavi non ancora presenti
        for (Map.Entry<String, String> e : desiderate.entrySet()) {
            if (giaPresenti.contains(e.getKey())) continue;
            PersonaggioLabel l = new PersonaggioLabel();
            l.setPersonaggio(p);
            l.setLabel(e.getKey());
            l.setValore(e.getValue());
            p.getLabels().add(l);
        }

        personaggioRepository.save(p);
        personaggioCacheService.invalidaPersonaggio(id);
    }

    public Boolean updateHP(UpdateHPRequest upd) {
        Personaggio p = personaggioRepository.findById(upd.getIdPersonaggio()).orElse(null);
        if (p == null) {
            return false;
        }
        StatValue hp = p.getStats().stream().filter(x -> x.getStat().getId().equals("PF")).findFirst().orElse(null);
        StatValue hpTemp = p.getStats().stream().filter(x -> x.getStat().getId().equals("PFTEMP")).findFirst().orElse(null);

        if (hpTemp == null || hp == null) {
            return false;
        }

        hp.setValore(upd.getPf());
        hpTemp.setValore(upd.getPfTemp());

        try {
            statValueRepository.save(hp);
            statValueRepository.save(hpTemp);
        } catch (Exception e) {
            return false;
        }

        personaggioCacheService.invalidaPersonaggio(upd.getIdPersonaggio());
        return true;
    }

    public Boolean updateContatore(UpdateCounterRequest upd) {
        Personaggio p = personaggioRepository.findById(upd.getIdPersonaggio()).orElse(null);
        if (p == null) {
            return false;
        }
        StatValue stat = p.getStats().stream().filter(x -> x.getStat().getId().equals(upd.getId())).findFirst().orElse(null);

        if (stat == null) {
            return false;
        }

        stat.setValore(upd.getValore());

        try {
            statValueRepository.save(stat);
        } catch (Exception e) {
            return false;
        }

        personaggioCacheService.invalidaPersonaggio(upd.getIdPersonaggio());
        return true;
    }

    private List<Item> findCompetenze(Item item) {
        ItemLabel competenze = item.getLabels().stream().filter(x -> x.getLabel().equals(Constants.ITEM_LABEL_LISTA_COMPETENZE)).findFirst().orElse(null);
        if (competenze != null) {
            List<Integer> competenzeIds = Stream.of(competenze.getValore().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .toList();
            return itemRepository.findItemsByIds(competenzeIds);
        }
        return new ArrayList<>();
    }

    private List<Item> findLingue(Item item) {
        ItemLabel lingue = item.getLabels().stream().filter(x -> x.getLabel().equals(Constants.ITEM_LABEL_LISTA_LINGUE)).findFirst().orElse(null);
        if (lingue != null) {
            List<Integer> lingueIds = Stream.of(lingue.getValore().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .toList();
            return itemRepository.findItemsByIds(lingueIds);
        }
        return new ArrayList<>();
    }

    private List<ItemLivelloDTO> findIncantesimi(Item item, Integer idPersonaggio) {
        ItemLabel spellDiClasse = item.getLabels().stream().filter(x -> x.getLabel().equals(Constants.ITEM_LABEL_LISTA_INCANTESIMI)).findFirst().orElse(null);
        if (spellDiClasse != null) {
            ItemLabel preparedSpell = itemLabelRepository.findByItemPersonaggioIdAndItemNome(idPersonaggio, Constants.ITEM_INCANTESIMI_PREPARATI).stream().filter(x -> x.getLabel().equals(spellDiClasse.getValore())).findFirst().orElse(null);
            if (preparedSpell != null) {
                List<Integer> incantesimiIds = Stream.of(preparedSpell.getValore().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Integer::parseInt)
                        .toList();
                List<ItemLivelloDTO> incantesimi = itemRepository.findIncantesimiWithLivelloByLabelAndIds(spellDiClasse.getValore(), incantesimiIds);

                if (!incantesimi.isEmpty()) {
                    incantesimi.forEach(itm -> {
                        itm.getItem().getLabels().add(new ItemLabel(null, null, Constants.ITEM_LABEL_LIVELLO_INCANTESIMO, itm.getLivello(), null));
                        itm.getItem().getLabels().add(new ItemLabel(null, null, Constants.ITEM_LABEL_CLASSE_INCANTESIMO, spellDiClasse.getValore(), null));
                    });
                    return incantesimi;
                }
            }
        }
        return new ArrayList<>();
    }

    /**
     * Trova l'avanzamento della classe per il livello richiesto. Se non esiste un
     * avanzamento esatto (es. livello oltre la tabella, per via dei livelli extra
     * concessi dagli item), ripiega sull'avanzamento più alto disponibile non
     * superiore al livello richiesto; se anche quello manca, sul più basso presente.
     */
    private Avanzamento findAvanzamentoPerLivello(Item classe, int livelloRichiesto) {
        List<Avanzamento> avz = classe.getAvanzamento().stream()
                .filter(y -> y.getItemTarget().getTipo().equals(TipoItem.AVANZAMENTO))
                .toList();
        if (avz.isEmpty()) return null;

        Avanzamento esatto = avz.stream()
                .filter(y -> y.getLivello() == livelloRichiesto)
                .findFirst().orElse(null);
        if (esatto != null) return esatto;

        Avanzamento piuAltoDisponibile = avz.stream()
                .filter(y -> y.getLivello() <= livelloRichiesto)
                .max(Comparator.comparingInt(Avanzamento::getLivello))
                .orElse(null);
        if (piuAltoDisponibile != null) return piuAltoDisponibile;

        return avz.stream().min(Comparator.comparingInt(Avanzamento::getLivello)).orElse(null);
    }

    /**
     * Sezione incantatore di un item: 1..N liste (sempre unite), una progressione
     * (preset standard o CUSTOM = slot dagli avanzamenti) e la formula degli slot bonus.
     */
    public record SezioneIncantesimi(List<String> liste, String progressione, String bonus, List<String> slot,
                                      boolean conosciutiSeparati, List<String> conosciuti,
                                      boolean personalizzata, List<String> incantesimiCustom) {}

    /**
     * Legge le sezioni incantatore "nuove" di un item dalle label indicizzate:
     * SPELL_&lt;n&gt; (CSV di liste), SPELL_&lt;n&gt;_PROG, SPELL_&lt;n&gt;_BONUS.
     * Ritorna lista vuota se l'item non usa il nuovo schema (resta il path legacy SPELL singola).
     */
    private List<SezioneIncantesimi> parseSezioniIncantesimi(Item item) {
        if (item.getLabels() == null) return List.of();
        // indici presenti: label che matchano SPELL_<n> o SPELL_<n>_<qualcosa> (es. _CUSTOM, _SLOT...).
        // Una sezione personalizzata non scrive mai la label base SPELL_<n> (non ha liste), quindi
        // l'indice va scoperto da QUALSIASI label della sezione, non solo da quella base.
        java.util.TreeSet<Integer> indici = new java.util.TreeSet<>();
        java.util.regex.Pattern idxPattern = java.util.regex.Pattern.compile("^SPELL_(\\d+)(_.*)?$");
        for (ItemLabel l : item.getLabels()) {
            String k = l.getLabel();
            if (k == null) continue;
            java.util.regex.Matcher m = idxPattern.matcher(k);
            if (m.matches()) indici.add(Integer.parseInt(m.group(1)));
        }
        List<SezioneIncantesimi> out = new ArrayList<>();
        for (Integer n : indici) {
            boolean personalizzata = "1".equals(item.getLabel("SPELL_" + n + "_CUSTOM"));
            String liste = item.getLabel("SPELL_" + n);
            // sezione standard: senza liste non ha senso (non filtrerebbe nulla); una sezione
            // personalizzata invece non usa liste, gli incantesimi sono agganciati direttamente
            if (!personalizzata && (liste == null || liste.isBlank())) continue;
            List<String> listeArr = liste == null ? List.of() : Arrays.stream(liste.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();
            if (!personalizzata && listeArr.isEmpty()) continue;
            String prog = item.getLabel("SPELL_" + n + "_PROG");
            String bonus = item.getLabel("SPELL_" + n + "_BONUS");
            String slotRaw = item.getLabel("SPELL_" + n + "_SLOT");
            List<String> slot = (slotRaw == null || slotRaw.isBlank())
                    ? List.of()
                    : Arrays.stream(slotRaw.split(";")).map(String::trim).toList();
            boolean conosciutiSeparati = "1".equals(item.getLabel("SPELL_" + n + "_HA_CONOSCIUTI"));
            String conosciutiRaw = item.getLabel("SPELL_" + n + "_CONOSCIUTI");
            List<String> conosciuti = (conosciutiRaw == null || conosciutiRaw.isBlank())
                    ? List.of()
                    : Arrays.stream(conosciutiRaw.split(";")).map(String::trim).toList();
            String incantesimiRaw = item.getLabel("SPELL_" + n + "_INCANTESIMI");
            List<String> incantesimiCustom = (incantesimiRaw == null || incantesimiRaw.isBlank())
                    ? List.of()
                    : Arrays.stream(incantesimiRaw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
            out.add(new SezioneIncantesimi(listeArr, prog, bonus, slot, conosciutiSeparati, conosciuti,
                    personalizzata, incantesimiCustom));
        }
        return out;
    }

    /** Tutte le liste incantesimi di una classe: dalle sezioni (SPELL_&lt;n&gt;) o, in fallback, dalla SPELL singola. */
    private List<String> tutteLeListe(Item classe) {
        List<SezioneIncantesimi> sez = parseSezioniIncantesimi(classe);
        if (!sez.isEmpty()) {
            return sez.stream().flatMap(s -> s.liste().stream()).toList();
        }
        String legacy = classe.getLabel(Constants.ITEM_LABEL_LISTA_INCANTESIMI);
        return legacy == null ? List.of() : List.of(legacy);
    }

    /**
     * Genera lo spellbook di una singola sezione incantatore. Gli slot provengono
     * dalla progressione preset (tabella standard) oppure, se CUSTOM/assente, dagli
     * avanzamenti della classe (come il path legacy). Gli incantesimi sono filtrati
     * sulle liste della sezione.
     */
    private SpellBookDTO generateSpellBookSezione(Item classe, int livelloTotale, int livelloEffettivo,
                                                  Integer idPersonaggio, SezioneIncantesimi sez, boolean fisso) {
        SpellBookDTO spellBook = new SpellBookDTO();
        spellBook.setIdClasse(classe.getId());
        spellBook.setNomeClasse(classe.getNome());
        spellBook.setFonteTipo(classe.getTipo() != null ? classe.getTipo().name() : null);
        spellBook.setSpellList(String.join(",", sez.liste()));
        spellBook.setSpurii(generateSpurii(classe, idPersonaggio));

        List<SpellBookIncantesimoDTO> incantesimi;
        if (sez.personalizzata()) {
            // Sezione personalizzata: incantesimi agganciati direttamente (id:livello), niente
            // filtro per lista/preparazione — sono sempre disponibili (come i domini di dominio).
            incantesimi = resolveIncantesimiCustom(sez);
        } else {
            Item preparedSpell = itemRepository.findItemByNomeAndPersonaggio_Id(Constants.ITEM_INCANTESIMI_PREPARATI, idPersonaggio);
            incantesimi = (preparedSpell == null || preparedSpell.getChild() == null)
                    ? new ArrayList<>()
                    : preparedSpell.getChild().stream().map(x -> itemMapper.toIncantesimoDTO(classe, x)).toList();
        }

        Set<String> liste = new HashSet<>(sez.liste());

        int[] slots;
        int[] slotsMax;
        if (fisso) {
            // Item (non classe): un numero fisso di slot, nessuna progressione per livello.
            slots = sez.slot() != null && !sez.slot().isEmpty() ? parseSlotRow(sez.slot().get(0)) : new int[0];
            slotsMax = slots;
        } else if (it.fin8.gdrsheet.def.ProgressioneIncantesimi.isPreset(sez.progressione())) {
            slots = it.fin8.gdrsheet.def.ProgressioneIncantesimi.slotsPerLivello(sez.progressione(), livelloEffettivo);
            slotsMax = it.fin8.gdrsheet.def.ProgressioneIncantesimi.slotsPerLivello(sez.progressione(), livelloTotale);
        } else if (sez.slot() != null && !sez.slot().isEmpty()) {
            // CUSTOM con tabella propria della sezione
            slots = slotArrayDaSezione(sez, livelloEffettivo);
            slotsMax = slotArrayDaSezione(sez, livelloTotale);
        } else {
            // CUSTOM legacy: slot dagli avanzamenti della classe
            slots = slotArrayDaAvanzamento(classe, livelloEffettivo);
            slotsMax = slotArrayDaAvanzamento(classe, livelloTotale);
        }
        if (slots.length == 0 || slotsMax.length == 0) return spellBook;

        int[] conosciutiMax = !sez.conosciutiSeparati() ? new int[0]
                : fisso
                    ? (sez.conosciuti() != null && !sez.conosciuti().isEmpty() ? parseSlotRow(sez.conosciuti().get(0)) : new int[0])
                    : conosciutiArrayDaSezione(sez, livelloTotale);

        for (int i = 0; i < slotsMax.length; i++) {
            // "dash" (nessun accesso, mai): solo per righe CUSTOM col sentinel; nei preset la riga
            // è già strutturalmente più corta e il loop non arriva a questo indice.
            if (slotsMax[i] == SLOT_DASH) continue;
            int slot = i < slots.length ? slots[i] : 0;
            if (slot == SLOT_DASH) slot = 0;

            SpellBookLivelloDTO liv = new SpellBookLivelloDTO();
            liv.setLivello(i);
            liv.setSlot(slot);
            if (i < conosciutiMax.length && conosciutiMax[i] != SLOT_DASH) {
                liv.setConosciuti(conosciutiMax[i]);
            }
            if (sez.bonus() != null && !sez.bonus().isBlank() && i > 0) {
                liv.getBonus().add(sez.bonus());
            }
            int fi = i;
            liv.getIncantesimi().addAll(incantesimi.stream()
                    .filter(x -> x.getLivello() == fi && (sez.personalizzata() || liste.contains(x.getSpellList())))
                    .toList());
            spellBook.getLivelli().add(liv);
        }
        return spellBook;
    }

    /** Sentinel per una cella "—" (nessun accesso, mai) nelle righe CUSTOM inserite a mano. */
    private static final int SLOT_DASH = Integer.MIN_VALUE;

    /** Un token CSV: "-"/vuoto = dash (nessun accesso), altrimenti il numero di slot/conosciuti. */
    private static int parseSlotToken(String raw) {
        String t = raw == null ? "" : raw.trim();
        if (t.isEmpty() || t.equals("-")) return SLOT_DASH;
        return Integer.parseInt(t);
    }

    private static int[] parseSlotRow(String riga) {
        if (riga == null || riga.isBlank()) return new int[0];
        try {
            String[] parts = riga.split(",");
            int[] r = new int[parts.length];
            for (int i = 0; i < parts.length; i++) r[i] = parseSlotToken(parts[i]);
            return r;
        } catch (Exception e) {
            return new int[0];
        }
    }

    /** Array slot dalla tabella custom della sezione per il dato livello di classe (1-based). */
    private int[] slotArrayDaSezione(SezioneIncantesimi sez, int livelloClasse) {
        int idx = livelloClasse - 1;
        if (idx < 0 || idx >= sez.slot().size()) return new int[0];
        return parseSlotRow(sez.slot().get(idx));
    }

    /** Array incantesimi conosciuti dalla tabella custom della sezione per il dato livello di classe (1-based). */
    private int[] conosciutiArrayDaSezione(SezioneIncantesimi sez, int livelloClasse) {
        int idx = livelloClasse - 1;
        if (sez.conosciuti() == null || idx < 0 || idx >= sez.conosciuti().size()) return new int[0];
        return parseSlotRow(sez.conosciuti().get(idx));
    }

    /** Array slot per livello incantesimo letto dall'avanzamento SP_SLOT della classe al dato livello. */
    private int[] slotArrayDaAvanzamento(Item classe, int livelloClasse) {
        Avanzamento av = findAvanzamentoPerLivello(classe, livelloClasse);
        if (av == null) return new int[0];
        ItemLabel sp = av.getItemTarget().getLabels().stream()
                .filter(x -> x.getLabel().equals(Constants.ITEM_LABEL_SPELL_SLOT)).findFirst().orElse(null);
        if (sp == null || sp.getValore() == null) return new int[0];
        return parseSlotRow(sp.getValore());
    }

    /**
     * Incantesimi spurii (non da lista/catalogo) di una fonte (classe o item): item figli marcati
     * con label SPURIO=1, ognuno coi propri utilizzi (UTILIZZI/UTILIZZI_USATI, stesso meccanismo
     * generico già usato per ogni altro item). Un "gruppo" con pool condiviso è semplicemente un
     * unico figlio spurio i cui incantesimi sono elencati nella sua descrizione.
     */
    private List<SpellBookIncantesimoDTO> generateSpurii(Item fonte, Integer idPersonaggio) {
        List<SpellBookIncantesimoDTO> out = new ArrayList<>();
        if (fonte.getChild() == null) return out;
        for (Collegamento c : fonte.getChild()) {
            Item entity = c.getItemTarget();
            if (entity == null || !"1".equals(entity.getLabel(Constants.ITEM_LABEL_SPURIO))) continue;
            if (itemMapper.isDisabled(entity)) continue;

            Integer uTotale = parseIntOrNull(entity.getLabel(Constants.LABEL_UTILIZZI));
            Integer uUsati = 0;
            List<ItemLabel> usatiRows = itemLabelRepository.findByLabelAndItem_IdInAndPersonaggio_Id(
                    Constants.LABEL_UTILIZZI_USATI, List.of(entity.getId()), idPersonaggio);
            if (!usatiRows.isEmpty()) {
                try { uUsati = Integer.parseInt(usatiRows.get(0).getValore()); } catch (Exception ignored) {}
            }

            SpellBookIncantesimoDTO dto = new SpellBookIncantesimoDTO();
            dto.setId(entity.getId());
            dto.setNome(entity.getNome());
            dto.setTipo(entity.getTipo());
            if (uTotale != null) {
                dto.setUtilizziTotale(uTotale);
                dto.setUtilizziUsati(uUsati);
            }
            out.add(dto);
        }
        return out;
    }

    /**
     * Incantesimi di una sezione personalizzata (item): elenco esplicito "id:livello" (label
     * SPELL_&lt;n&gt;_INCANTESIMI), incantesimi specifici cercati e aggiunti uno a uno invece di
     * riferirsi a una lista standard. Sempre disponibili (nessuna preparazione richiesta).
     */
    private List<SpellBookIncantesimoDTO> resolveIncantesimiCustom(SezioneIncantesimi sez) {
        List<SpellBookIncantesimoDTO> out = new ArrayList<>();
        if (sez.incantesimiCustom() == null) return out;
        for (String token : sez.incantesimiCustom()) {
            String[] parts = token.split(":");
            if (parts.length != 2) continue;
            try {
                int itemId = Integer.parseInt(parts[0].trim());
                int livello = Integer.parseInt(parts[1].trim());
                Item entity = itemRepository.findById(itemId).orElse(null);
                if (entity == null) continue;
                SpellBookIncantesimoDTO dto = new SpellBookIncantesimoDTO();
                dto.setId(entity.getId());
                dto.setNome(entity.getNome());
                dto.setTipo(entity.getTipo());
                dto.setLivello(livello);
                dto.setAlwaysPrep(true);
                dto.setComponenti(utilService.getItemLabels(entity, Constants.ITEM_LABEL_COMPONENTE));
                dto.setScuola(utilService.getItemLabel(entity, Constants.ITEM_LABEL_SCUOLA_SP));
                out.add(dto);
            } catch (Exception ignored) {}
        }
        return out;
    }

    private SpellBookDTO generateSpellBook(Item classe, Integer lvl, Integer livelloEffettivo, Integer idPersonaggio) {
        ItemLabel spellList = classe.getLabels().stream().filter(x -> x.getLabel().equals(Constants.ITEM_LABEL_LISTA_INCANTESIMI)).findFirst().orElse(null);
        if (spellList == null) return null;
        SpellBookDTO spellBook = new SpellBookDTO();
        spellBook.setIdClasse(classe.getId());
        spellBook.setNomeClasse(classe.getNome());
        spellBook.setFonteTipo(classe.getTipo() != null ? classe.getTipo().name() : null);
        spellBook.setSpellList(spellList.getValore());
        spellBook.setSpurii(generateSpurii(classe, idPersonaggio));
        String slotBonus = utilService.getItemLabel(classe, Constants.ITEM_LABEL_SPELL_SLOT_BONUS);

        Avanzamento avanzamentoTotale = findAvanzamentoPerLivello(classe, Math.toIntExact(lvl));
        Avanzamento avanzamento = findAvanzamentoPerLivello(classe, Math.toIntExact(livelloEffettivo));
        Item preparedSpell = itemRepository.findItemByNomeAndPersonaggio_Id(Constants.ITEM_INCANTESIMI_PREPARATI, idPersonaggio);
        List<SpellBookIncantesimoDTO> incantesimi = (preparedSpell == null || preparedSpell.getChild() == null)
                ? new ArrayList<>()
                : preparedSpell.getChild().stream().map(x -> itemMapper.toIncantesimoDTO(classe, x)).toList();
        if (avanzamento != null && avanzamentoTotale != null) {
            ItemLabel spellSlot = avanzamento.getItemTarget().getLabels().stream().filter(x -> x.getLabel().equals(Constants.ITEM_LABEL_SPELL_SLOT)).findFirst().orElse(null);
            ItemLabel spellSlotTotali = avanzamentoTotale.getItemTarget().getLabels().stream().filter(x -> x.getLabel().equals(Constants.ITEM_LABEL_SPELL_SLOT)).findFirst().orElse(null);
            if (spellSlot != null && spellSlotTotali != null) {
                int[] slots = parseSlotRow(spellSlot.getValore());
                int[] slotsMaxLvl = parseSlotRow(spellSlotTotali.getValore());

                for (int i = 0; i < slotsMaxLvl.length; i++) {
                    if (slotsMaxLvl[i] == SLOT_DASH) continue;
                    int slot = i < slots.length ? slots[i] : 0;
                    if (slot == SLOT_DASH) slot = 0;

                    SpellBookLivelloDTO spellBookLivello = new SpellBookLivelloDTO();
                    spellBookLivello.setLivello(i);
                    spellBookLivello.setSlot(slot);
                    if (slotBonus != null && i > 0) {
                        spellBookLivello.getBonus().add(slotBonus);
                    }
                    int finalI = i;
                    spellBookLivello.getIncantesimi().addAll(incantesimi.stream().filter(x -> x.getLivello() == finalI && x.getSpellList().equals(spellList.getValore())).toList());
                    spellBook.getLivelli().add(spellBookLivello);
                }

            }
        }
        return spellBook;
    }

    public void updateBaseStatValue(UpdateBaseStatValueRequest request) {
        Personaggio p = personaggioRepository.findById(request.getIdPersonaggio()).orElse(null);
        if (p == null) throw new RuntimeException("Personaggio non trovato");

        StatValue s = p.getStats().stream().filter(x -> x.getStat().getId().equals(request.getIdStat())).findFirst().orElse(null);
        if (s == null) throw new RuntimeException("Stat non trovato");

        s.setValore(request.getValore().toString());
        statValueRepository.save(s);
        personaggioCacheService.invalidaPersonaggio(request.getIdPersonaggio());
    }

    public void updateStatValue(UpdateStatValueRequest request) {
        Personaggio p = personaggioRepository.findById(request.getIdPersonaggio()).orElse(null);
        if (p == null) throw new RuntimeException("Personaggio non trovato");

        StatValue s = p.getStats().stream().filter(x -> x.getStat().getId().equals(request.getIdStat())).findFirst().orElse(null);
        if (s == null) throw new RuntimeException("Stat non trovato");

        if (request.getValore() != null) s.setValore(request.getValore());
        s.setFormula(request.getFormula() != null && !request.getFormula().isBlank() ? request.getFormula() : null);
        if (request.getModStatId() != null && !request.getModStatId().isBlank()) {
            it.fin8.gdrsheet.entity.Stat modStat = statRepository.findById(request.getModStatId()).orElse(null);
            s.setMod(modStat);
        } else {
            s.setMod(null);
        }
        statValueRepository.save(s);
        personaggioCacheService.invalidaPersonaggio(request.getIdPersonaggio());
    }

    /**
     * Garantisce che il personaggio abbia una riga in stat_value per ogni
     * stat_default del mondo di cui fa parte: quelle mancanti vengono create
     * con valore, mod e addestramento presi dal default.
     */
    @org.springframework.transaction.annotation.Transactional
    public void ensureStatValues(Integer idPersonaggio) {
        Personaggio personaggio = personaggioRepository.findPersonaggioById(idPersonaggio);
        if (personaggio == null) throw new RuntimeException("Personaggio non trovato");
        if (personaggio.getParty() == null || personaggio.getParty().getMondo() == null
                || personaggio.getParty().getMondo().getDefaultStats() == null) return;

        Set<String> esistenti = statValueRepository.findAllByPersonaggioId(idPersonaggio).stream()
                .map(sv -> sv.getStat().getId())
                .collect(Collectors.toSet());

        List<StatValue> daCreare = new ArrayList<>();
        Set<String> giaPreviste = new HashSet<>();
        for (StatDefault def : personaggio.getParty().getMondo().getDefaultStats()) {
            String statId = def.getStatId();
            if (statId == null || esistenti.contains(statId) || !giaPreviste.add(statId)) continue;

            Stat stat = statRepository.findById(statId).orElse(null);
            if (stat == null) continue; // default che punta a una stat inesistente: ignora

            StatValue sv = new StatValue();
            sv.setPersonaggio(personaggio);
            sv.setStat(stat);
            String vd = def.getValoreDefault();
            boolean isFormula = vd != null && !vd.isBlank() && (vd.contains("@") || vd.contains("$"));
            if (isFormula) {
                sv.setValore("0");
                sv.setFormula(vd.trim());
            } else {
                sv.setValore(vd != null && !vd.isBlank() ? vd.trim() : "0");
            }
            sv.setMod(def.getDefaultMod());
            sv.setClasse(false);
            sv.setAddestramento(Boolean.TRUE.equals(def.getAddestramento()));
            daCreare.add(sv);
        }

        if (!daCreare.isEmpty()) statValueRepository.saveAll(daCreare);
    }

    public Integer getPersonaggioIdDaLivello(Integer idLivello) {
        Item livello = itemRepository.findById(idLivello).orElseThrow(() -> new RuntimeException("Livello non trovato"));
        if (!livello.getTipo().equals(TipoItem.LIVELLO)) {
            throw new RuntimeException("L'item non e' un livello");
        }
        if (livello.getPersonaggio() != null) {
            return livello.getPersonaggio().getId();
        }
        // fallback per livelli storici non intestati: risali dai collegamenti parent
        if (livello.getParent() != null) {
            return livello.getParent().stream()
                    .map(c -> c.getItemSource().getPersonaggio())
                    .filter(Objects::nonNull)
                    .map(Personaggio::getId)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Livello non associato ad alcun personaggio"));
        }
        throw new RuntimeException("Livello non associato ad alcun personaggio");
    }

    public List<AbilitaDTO> getStatsDaPersonaggio(Integer idPersonaggio) {
        Personaggio personaggio = personaggioRepository.findById(idPersonaggio).orElseThrow(() -> new RuntimeException("Personaggio non trovato"));
        List<StatDefault> listaDefault = personaggio.getParty().getMondo().getDefaultStats();
        DatiPersonaggioDTO datiPersonaggio = getDatiPersonaggio(idPersonaggio);
        return datiPersonaggio.getAbilita();
    }

    public List<Item> getItemAssociabili(Integer idPersonaggio, TipoItem tipoItem) {
        return getItemAssociabili(idPersonaggio, tipoItem, null);
    }

    /**
     * Classi/razze associabili filtrate per nome: con un compendio di centinaia di classi
     * (import bulk da dndtools.org) restituire tutto senza filtro sarebbe troppo pesante da
     * mappare (labels/modificatori per ogni item) ad ogni apertura dell'editor livello.
     * q null/vuoto = comportamento storico, nessun filtro (usato da maledizioni/frutti,
     * compendi piccoli dove il filtro non serve).
     */
    public List<Item> getItemAssociabili(Integer idPersonaggio, TipoItem tipoItem, String q) {
        Personaggio personaggio = personaggioRepository.findById(idPersonaggio).orElseThrow(() -> new RuntimeException("Personaggio non trovato"));
        String needle = q == null ? "" : q.trim().toLowerCase();
        var stream = personaggio.getParty().getMondo().getItems().stream()
                .filter(x -> x.getTipo().equals(tipoItem))
                .filter(x -> needle.isEmpty() || x.getNome() != null && x.getNome().toLowerCase().contains(needle));
        if (!needle.isEmpty()) stream = stream.limit(50);
        return stream.toList();
    }

    public GradiDTO getGradi(Integer idPersonaggio, Integer livello, Integer idClasse, String livelli) {
        Item classe = itemRepository.findById(idClasse)
                .orElseThrow(() -> new RuntimeException("Classe non trovata"));

        int count = contaLivelliSelezionati(livelli);
        Integer total = computeGradi(classe, livello, idPersonaggio, count);

        List<String> formule = new ArrayList<>();
        String rank1 = classe.getLabel(Constants.ITEM_LABEL_RANK_PRIMO);
        String rank = classe.getLabel(Constants.ITEM_LABEL_RANK);
        if (livello != null && livello == 1 && rank1 != null && !rank1.isBlank()) formule.add(rank1);
        if (rank != null && !rank.isBlank()) formule.add(rank);

        return new GradiDTO(formule, total != null ? total : 0, livello + 3);
    }

    private int contaLivelliSelezionati(String livelli) {
        if (livelli == null || livelli.isBlank()) return 1;
        int n = (int) Arrays.stream(livelli.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .count();
        return Math.max(1, n);
    }

    /**
     * Calcola i gradi di un livello dalle formule della classe ({@code RANK_1}/{@code RANK})
     * con l'Intelligenza permanente corrente del personaggio.
     * <ul>
     *   <li>Livelli normali: {@code RANK} × numero di livelli-classe selezionati.</li>
     *   <li>Livello 1 del personaggio: il primo livello-classe usa {@code RANK_1}, gli
     *       eventuali altri selezionati usano {@code RANK}.</li>
     * </ul>
     * Ritorna null se non c'è una formula applicabile o il risultato non è numerico.
     */
    public Integer computeGradi(Item classe, Integer livello, Integer idPersonaggio, int numLivelliSelezionati) {
        if (classe == null) return null;
        String fRank = classe.getLabel(Constants.ITEM_LABEL_RANK);
        String fRank1 = classe.getLabel(Constants.ITEM_LABEL_RANK_PRIMO);
        boolean noRank = (fRank == null || fRank.isBlank());
        boolean noRank1 = (fRank1 == null || fRank1.isBlank());
        if (noRank && noRank1) return null;

        DatiPersonaggioDTO dati = getDatiPersonaggio(idPersonaggio);
        CaratteristicaDTO INT = dati.getCaratteristica("INT");
        INT.setModificatore(INT.getModificatorePermanente());
        List<CaratteristicaDTO> caratteristiche = List.of(INT);

        Integer rank = calcolaFormulaIntera(fRank, caratteristiche);
        Integer rank1 = calcolaFormulaIntera(fRank1, caratteristiche);

        int n = Math.max(1, numLivelliSelezionati);

        if (livello != null && livello == 1) {
            int primo = (rank1 != null) ? rank1 : (rank != null ? rank : 0);
            int altri = (rank != null) ? rank * (n - 1) : 0;
            return primo + altri;
        }

        if (rank == null) return null;
        return rank * n;
    }

    private Integer calcolaFormulaIntera(String formula, List<CaratteristicaDTO> caratteristiche) {
        if (formula == null || formula.isBlank()) return null;
        try {
            return Integer.parseInt(calcoloService.calcola(formula, caratteristiche));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<Modificatore> elaboraModificatoriStatLivello(List<Modificatore> allMods) {
        List<Modificatore> modificatoriSTAT = allMods.stream()
                .filter(x -> x.getItem().getTipo().equals(TipoItem.LIVELLO)
                        && Constants.listOfUniqueByClassStats.contains(x.getStat().getId()))
                .toList();

        Map<String, List<Modificatore>> byStat = modificatoriSTAT.stream()
                .collect(Collectors.groupingBy(m -> m.getStat().getId()));

        Map<String, Map<String, List<Modificatore>>> byStatAndNome = byStat.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().collect(
                                Collectors.groupingBy(m -> m.getItem().getLabel(Constants.ITEM_LABEL_CLASSE)))
                ));

        Map<String, Map<String, Modificatore>> maxLivelloByStatAndNome = new HashMap<>();

        for (String stat : byStatAndNome.keySet()) {
            Map<String, Modificatore> byNome = new HashMap<>();
            for (String nome : byStatAndNome.get(stat).keySet()) {
                Optional<Modificatore> maxLivello = byStatAndNome.get(stat).get(nome).stream()
                        .max(Comparator.comparingInt(
                                m -> Integer.parseInt(m.getItem().getLabel(Constants.ITEM_LIVELLO_LVL))));
                maxLivello.ifPresent(mod -> byNome.put(nome, mod));
            }
            maxLivelloByStatAndNome.put(stat, byNome);
        }

// Ottieni una lista piatta di tutti i modificatori selezionati

        return maxLivelloByStatAndNome.values().stream()
                .flatMap(mappaNome -> mappaNome.values().stream())
                .toList();
    }
}


