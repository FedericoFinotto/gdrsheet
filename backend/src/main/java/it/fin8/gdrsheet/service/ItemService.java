package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.def.TipoModificatore;
import it.fin8.gdrsheet.dto.*;
import it.fin8.gdrsheet.entity.*;
import it.fin8.gdrsheet.mapper.ItemMapper;
import it.fin8.gdrsheet.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
    private PartyRepository partyRepository;
    @Autowired
    private ItemLabelRepository itemLabelRepository;
    @Autowired
    private ItemTagService itemTagService;
    @Autowired
    EntityManager em;
    @Autowired
    private CollegamentoLabelRepository collegamentoLabelRepository;
    @Autowired
    private CollegamentoRepository collegamentoRepository;
    @Autowired
    private AvanzamentoRepository avanzamentoRepository;
    @Autowired
    private ModificatoreRepository modificatoreRepository;
    @Autowired
    private PersonaggioService personaggioService;

    @Autowired
    private PersonaggioCacheService personaggioCacheService;

    public Item switchItemState(Integer itemId, Integer personaggioId) {
        Item itm = itemRepository.findItemById(itemId);
        saveDisabledPerPersonaggio(itemId, personaggioId, !isItemDisabled(itemId, personaggioId));
        personaggioCacheService.invalidaPersonaggio(personaggioId);
        return itm;
    }

    /**
     * Stato disabilitato di un item nel contesto di un personaggio: SEMPRE una ItemLabel legata a
     * quel personaggio (mai il collegamento, mai la riga globale) — così vale anche per un
     * discendente di un item di compendio condiviso, non solo per il primo livello raggiunto
     * tramite FromCompendio.
     */
    public boolean isItemDisabled(Integer itemId, Integer personaggioId) {
        if (itemId == null || personaggioId == null) return false;
        return itemLabelRepository.findByItem_IdAndLabelAndPersonaggio_Id(itemId, Constants.ITEM_LABEL_DISABILITATO, personaggioId)
                .map(ItemLabel::getValore)
                .map(Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE::equals)
                .orElse(false);
    }

    /** Stesso pattern di saveQtaPerPersonaggio: una riga ItemLabel per (item, personaggio), mai globale. */
    private void saveDisabledPerPersonaggio(Integer itemId, Integer personaggioId, boolean disabled) {
        ItemLabel dl = itemLabelRepository
                .findByItem_IdAndLabelAndPersonaggio_Id(itemId, Constants.ITEM_LABEL_DISABILITATO, personaggioId)
                .orElseGet(() -> {
                    ItemLabel nl = new ItemLabel();
                    nl.setItem(em.getReference(Item.class, itemId));
                    nl.setPersonaggio(em.getReference(Personaggio.class, personaggioId));
                    nl.setLabel(Constants.ITEM_LABEL_DISABILITATO);
                    return nl;
                });
        dl.setValore(disabled ? Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE : Constants.ITEM_LABEL_DISABILITATO_VALORE_FALSE);
        itemLabelRepository.save(dl);
    }

    public List<SpellBookIncantesimoDTO> getListIncantesimiByClasseAndLevel(Integer idClasse, Integer livello, String spellList) {
        Item classe = itemRepository.findItemById(idClasse);

        // liste da interrogare: quelle passate (CSV della sezione) o, in fallback, la SPELL della classe
        List<String> liste;
        if (spellList != null && !spellList.isBlank()) {
            liste = java.util.Arrays.stream(spellList.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
        } else {
            String spellClasse = utilService.getItemLabel(classe, Constants.ITEM_LABEL_LISTA_INCANTESIMI);
            liste = spellClasse == null ? List.of() : List.of(spellClasse);
        }

        Map<Integer, SpellBookIncantesimoDTO> byId = new LinkedHashMap<>();
        for (String lista : liste) {
            for (ItemLivelloDTO x : itemRepository.findIncantesimiByLabelAndMaxLivello(lista, livello)) {
                SpellBookIncantesimoDTO dto = itemMapper.toIncantesimoDTO(classe, x);
                dto.setSpellList(lista); // lista effettiva di provenienza
                byId.putIfAbsent(dto.getId(), dto);
            }
        }
        return new ArrayList<>(byId.values());
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

        personaggioCacheService.invalidaPersonaggio(request.getIdPersonaggio());
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

        personaggioCacheService.invalidaPersonaggio(request.getIdPersonaggio());
    }

    @Transactional
    public Item updateSpell(Integer id, UpdateSpellRequest request) {
        Item itm = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Item non trovato"));

        // --- aggiorna campi base ---
        if (request.getNome() != null) itm.setNome(request.getNome());
        if (request.getDescrizione() != null) itm.setDescrizione(request.getDescrizione());
        if (request.getIdMondo() != null) {
            Mondo mondo = em.find(Mondo.class, request.getIdMondo());
            if (mondo != null) itm.setMondo(mondo);
        }
        if (request.getIdSistema() != null) {
            Sistema sistema = em.find(Sistema.class, request.getIdSistema());
            if (sistema != null) itm.setSistema(sistema);
        }

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

        Item saved = itemRepository.save(itm);
        personaggioCacheService.invalidaPerItem(saved.getId());
        return saved;
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

    private void saveQtaPerPersonaggio(Integer itemId, Integer personaggioId, String valore) {
        ItemLabel ql = itemLabelRepository
                .findByItem_IdAndLabelAndPersonaggio_Id(itemId, Constants.LABEL_QTA, personaggioId)
                .orElseGet(() -> {
                    ItemLabel nl = new ItemLabel();
                    nl.setItem(em.getReference(Item.class, itemId));
                    nl.setPersonaggio(em.getReference(Personaggio.class, personaggioId));
                    nl.setLabel(Constants.LABEL_QTA);
                    return nl;
                });
        ql.setValore(valore != null && !valore.trim().isEmpty() ? valore.trim() : "1");
        itemLabelRepository.save(ql);
    }

    /**
     * GET /item/{id} restituisce l'entity Item così com'è: Item.getLabel()/Item.labels sono
     * filtrati su id_personaggio IS NULL (vedi @Where su Item.labels), quindi vedono SEMPRE il
     * valore globale di compendio — mai quello scoped per un personaggio. Per QTA e
     * UTILIZZI_USATI questo produce un valore diverso da quello già mostrato in scheda/inventario
     * per lo stesso item dello stesso personaggio (che invece li risolve scoped, vedi
     * PersonaggioService.getAllPersonaggioItemsDTOByIdPersonaggio).
     * Qui si "stampa" (transitorio, non persistito — stesso pattern già usato per DISABLED in
     * PersonaggioService.flattenItems, sicuro perché non segue nessuna scrittura transazionale
     * nella stessa richiesta) il valore scoped sull'entity stessa, PRIMA di restituirla, così
     * l'editor (BaseItemEditor.vue) legge lo stesso valore della scheda.
     */
    public void stampaLabelScopedPerPersonaggio(Item itm, Integer idPersonaggio) {
        if (itm == null || itm.getId() == null || idPersonaggio == null) return;
        String qta = itemLabelRepository
                .findByItem_IdAndLabelAndPersonaggio_Id(itm.getId(), Constants.LABEL_QTA, idPersonaggio)
                .map(ItemLabel::getValore).orElse(null);
        String utilizziUsati = itemLabelRepository
                .findByItem_IdAndLabelAndPersonaggio_Id(itm.getId(), Constants.LABEL_UTILIZZI_USATI, idPersonaggio)
                .map(ItemLabel::getValore).orElse(null);
        // Contatori item ($V_<NOME>) mostrati/editabili in Mobile_DettaglioItem.vue (flag globale
        // SHOW_$V_<NOME>=1 sull'item): come QTA/UTILIZZI_USATI, il valore scoped per questo
        // personaggio va letto e "stampato" QUI, altrimenti Item.getLabel($V_<NOME>) vedrebbe
        // sempre e solo la riga globale (@Where su Item.labels) — lo stesso identico bug già
        // risolto per QTA, ma sui contatori $V_ (che il resto del backend, vedi
        // PersonaggioService.calcolaDatiPersonaggio, legge tuttora SENZA scoping: qui almeno la
        // GET di un singolo item mostra/scrive il valore giusto per il personaggio che la apre).
        List<String> nomiContatoriMostrati = itm.getLabels() == null ? List.of() : itm.getLabels().stream()
                .filter(l -> l.getLabel() != null && l.getLabel().startsWith("SHOW_$V_") && "1".equals(l.getValore()))
                .map(l -> l.getLabel().substring("SHOW_$V_".length()))
                .filter(n -> !n.isBlank())
                .distinct()
                .toList();
        Map<String, String> contatoriScoped = new LinkedHashMap<>();
        for (String nome : nomiContatoriMostrati) {
            itemLabelRepository.findByItem_IdAndLabelAndPersonaggio_Id(itm.getId(), "$V_" + nome, idPersonaggio)
                    .ifPresent(l -> contatoriScoped.put(nome, l.getValore()));
        }
        // Card SCELTE (Mobile_DettaglioItem.vue): per ogni sezione definita globalmente
        // (SCELTA_<n>_CANDIDATI, uguale per tutti), stampa la scelta di QUESTO personaggio
        // (SCELTA_<n>_FATTA, personaggio-scoped) — stesso identico schema dei contatori sopra.
        List<Integer> indiciSezioniScelte = itm.getLabels() == null ? List.of() : itm.getLabels().stream()
                .filter(l -> l.getLabel() != null && l.getLabel().startsWith("SCELTA_") && l.getLabel().endsWith("_CANDIDATI"))
                .map(l -> {
                    try {
                        return Integer.parseInt(l.getLabel().substring("SCELTA_".length(), l.getLabel().length() - "_CANDIDATI".length()));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, String> scelteScoped = new LinkedHashMap<>();
        for (Integer n : indiciSezioniScelte) {
            String labelKey = "SCELTA_" + n + "_FATTA";
            itemLabelRepository.findByItem_IdAndLabelAndPersonaggio_Id(itm.getId(), labelKey, idPersonaggio)
                    .ifPresent(l -> scelteScoped.put(labelKey, l.getValore()));
        }
        // stampa DOPO tutte le letture: nessuna query va eseguita tra una mutazione e l'altra,
        // altrimenti rischierebbe di far scattare un flush che le persiste per sbaglio.
        if (qta != null) itm.setLabel(Constants.LABEL_QTA, qta);
        if (utilizziUsati != null) itm.setLabel(Constants.LABEL_UTILIZZI_USATI, utilizziUsati);
        contatoriScoped.forEach((nome, valore) -> itm.setLabel("$V_" + nome, valore));
        scelteScoped.forEach(itm::setLabel);
    }

    /**
     * Registra la scelta di un personaggio per una sezione della card SCELTE (label
     * SCELTA_&lt;sezioneIndice&gt;_FATTA, personaggio-scoped — vedi stampaLabelScopedPerPersonaggio
     * per la lettura). sceltoId null = nessuna scelta fatta (rimuove la label esistente).
     */
    public void setScelta(Integer itemId, Integer personaggioId, int sezioneIndice, Integer sceltoId) {
        String labelKey = "SCELTA_" + sezioneIndice + "_FATTA";
        Optional<ItemLabel> esistente = itemLabelRepository
                .findByItem_IdAndLabelAndPersonaggio_Id(itemId, labelKey, personaggioId);
        if (sceltoId == null) {
            esistente.ifPresent(itemLabelRepository::delete);
            return;
        }
        ItemLabel label = esistente.orElseGet(() -> {
            ItemLabel nl = new ItemLabel();
            nl.setItem(em.getReference(Item.class, itemId));
            nl.setPersonaggio(em.getReference(Personaggio.class, personaggioId));
            nl.setLabel(labelKey);
            return nl;
        });
        label.setValore(String.valueOf(sceltoId));
        itemLabelRepository.save(label);
        personaggioCacheService.invalidaPersonaggio(personaggioId);
    }

    /* =====================================================================
     * Creazione / aggiornamento generico item
     * ===================================================================== */

    @Transactional
    public Item createItem(UpdateItemRequest request) {
        if (request.getNome() == null || request.getNome().trim().isEmpty())
            throw new RuntimeException("Nome obbligatorio");
        if (request.getTipo() == null)
            throw new RuntimeException("Tipo obbligatorio");

        Item itm = new Item();
        itm.setNome(request.getNome().trim());
        itm.setTipo(request.getTipo());
        itm.setDescrizione(request.getDescrizione());
        itm.setLabels(new ArrayList<>());

        // gli item nascono nel compendio: personaggio null, sistema/mondo del party.
        // Eccezione: i LIVELLO sono intestati direttamente al personaggio
        // (come FromCompendio e PreparedSpell).
        Personaggio pg = null;
        if (request.getIdPersonaggio() != null) {
            pg = personaggioRepository.findPersonaggioById(request.getIdPersonaggio());
            if (pg == null) throw new RuntimeException("Personaggio non trovato: " + request.getIdPersonaggio());
            if (TipoItem.LIVELLO.equals(request.getTipo())
                    || (isAmbitoTree(request.getTipo()) && "PERSONAGGIO".equals(request.getQuestScope()))) {
                itm.setPersonaggio(pg);
            }
            if (pg.getParty() != null && pg.getParty().getMondo() != null) {
                Mondo mondo = pg.getParty().getMondo();
                itm.setMondo(mondo);
                itm.setSistema(mondo.getSistema());
            }
        } else if (request.getIdParty() != null) {
            // creazione senza contesto personaggio (es. pagina Quest di un party): eredita
            // comunque mondo/sistema dal party, altrimenti una QUEST di ambito MONDO creata da
            // lì non sarebbe mai visibile (Item.mondo resterebbe null).
            Party party = em.find(Party.class, request.getIdParty());
            if (party != null && party.getMondo() != null) {
                Mondo mondo = party.getMondo();
                itm.setMondo(mondo);
                itm.setSistema(mondo.getSistema());
            }
        }

        // mondo/sistema espliciti dalla richiesta (hanno la precedenza)
        if (request.getIdMondo() != null) {
            Mondo mondo = em.find(Mondo.class, request.getIdMondo());
            if (mondo != null) itm.setMondo(mondo);
        }
        if (request.getIdSistema() != null) {
            Sistema sistema = em.find(Sistema.class, request.getIdSistema());
            if (sistema != null) itm.setSistema(sistema);
        }

        // QUEST/INFO radice di ambito PARTY: memorizza l'id del Party come label (nessuna nuova
        // tabella). L'id esplicito ha precedenza, altrimenti si usa il party del personaggio.
        if (isAmbitoTree(request.getTipo()) && "PARTY".equals(request.getQuestScope())) {
            Integer partyId = request.getIdParty() != null ? request.getIdParty()
                    : (pg != null && pg.getParty() != null ? pg.getParty().getId() : null);
            if (partyId != null) {
                String labelPartyAmbito = TipoItem.INFO.equals(request.getTipo())
                        ? Constants.ITEM_LABEL_INFO_PARTY : Constants.ITEM_LABEL_QUEST_PARTY;
                addLabelRow(itm, labelPartyAmbito, String.valueOf(partyId));
            }
        }

        if (request.getLabels() != null) {
            for (UpdateItemRequest.LabelRowDTO l : request.getLabels()) {
                addLabelRow(itm, l.getLabel(), l.getValore());
            }
        }

        Item saved = itemRepository.save(itm);
        applyModificatori(saved, request.getModificatori());
        applyAttacchi(saved, request.getAttacchi());
        applyChildren(saved, request.getChildren());
        if (TipoItem.NODO.equals(saved.getTipo())) {
            applyNodoTipo(saved, request.getNodoTipoItemId());
            applyNodoA(saved, request.getNodoA());
            applyNodoDa(saved, request.getNodoDa());
        }

        // bootstrap struttura di un frutto appena creato (variabile livello + 3 forme)
        if (TipoItem.FRUTTO.equals(saved.getTipo())) {
            bootstrapFrutto(saved);
        }

        // aggancio al personaggio tramite il suo FromCompendio (non per gli item intestati
        // direttamente, né quando l'item sarà collegato come child di un altro item)
        if (pg != null && saved.getPersonaggio() == null && !Boolean.TRUE.equals(request.getSkipFromCompendio())) {
            Item fromCompendio = ensureFromCompendio(pg.getId());
            Collegamento link = new Collegamento();
            link.setItemSource(fromCompendio);
            link.setItemTarget(saved);
            link.setLabels(new ArrayList<>());
            collegamentoRepository.save(link);
            // un item appena aggiunto nasce disabilitato: va abilitato esplicitamente
            saveDisabledPerPersonaggio(saved.getId(), pg.getId(), true);
        }

        if (pg != null) personaggioCacheService.invalidaPersonaggio(pg.getId());
        return saved;
    }

    /**
     * Inizializza la struttura di un frutto appena creato:
     * <ul>
     *   <li>una variabile di livello sul frutto ({@code $V_LVL = 0});</li>
     *   <li>tre forme collegate come child, ciascuna con un modificatore che imposta
     *       il livello del frutto padre ({@code $M_P_LVL = "=N"}).</li>
     * </ul>
     * Le forme di default vengono create solo se il frutto non ne ha già di proprie
     * (es. aggiunte a mano in fase di creazione).
     */
    private void bootstrapFrutto(Item frutto) {
        boolean changed = false;
        // variabili sul frutto (solo se non già presenti)
        if (frutto.getLabel(Constants.ITEM_LABEL_FRUTTO_LVL) == null) {
            addLabelRow(frutto, Constants.ITEM_LABEL_FRUTTO_LVL, "0");
            changed = true;
        }
        if (frutto.getLabel(Constants.ITEM_LABEL_FRUTTO_MOLT) == null) {
            addLabelRow(frutto, Constants.ITEM_LABEL_FRUTTO_MOLT, "0");
            changed = true;
        }
        if (changed) itemRepository.save(frutto);

        // se ci sono già forme collegate (es. aggiunte a mano), non ricreare la struttura
        // di default; uso il repository perché i collegamenti appena salvati da applyChildren
        // non sono riflessi nella collection in memoria del frutto.
        boolean haForme = collegamentoRepository.findAllByItemSource_Id(frutto.getId()).stream()
                .anyMatch(c -> TipoItem.FORMA.equals(c.getItemTarget().getTipo()));
        if (haForme) return;

        for (int n = 1; n <= 4; n++) {
            Item forma = new Item();
            forma.setNome("Forma " + n);
            forma.setTipo(TipoItem.FORMA);
            forma.setMondo(frutto.getMondo());
            forma.setSistema(frutto.getSistema());
            forma.setLabels(new ArrayList<>());
            addLabelRow(forma, Constants.ITEM_LABEL_FORMA_MOD_LVL, "=" + n);
            addLabelRow(forma, Constants.ITEM_LABEL_FORMA_MOD_MOLT, "=" + moltiplicatoreForma(n));
            Item savedForma = itemRepository.save(forma);

            Collegamento link = new Collegamento();
            link.setItemSource(frutto);
            link.setItemTarget(savedForma);
            collegamentoRepository.save(link);
        }
    }

    /** Moltiplicatore di default per la forma n-esima: 1, 2, 3, 9. */
    private static int moltiplicatoreForma(int n) {
        return n >= 4 ? 9 : n;
    }

    /**
     * Elimina un item. Se l'item è intestato a un personaggio (livelli,
     * FromCompendio, PreparedSpell) viene eliminato del tutto. Se è un item
     * di compendio e viene passato idPersonaggio, viene scollegato dal suo
     * FromCompendio; se a quel punto non è più referenziato da nessun altro
     * item, viene eliminato anche dal compendio.
     */
    @Transactional
    public void deleteItem(Integer id, Integer idPersonaggio) {
        Item itm = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Item non trovato"));

        // risolve i personaggi raggiungibili PRIMA di toccare il grafo Collegamento: dopo
        // l'eliminazione i collegamenti non ci sono più e la risalita non troverebbe più nulla.
        personaggioCacheService.invalidaPerItem(id);

        if (itm.getPersonaggio() != null) {
            hardDelete(itm);
            return;
        }

        if (idPersonaggio != null) {
            // scollega ovunque si trovi nell'inventario del personaggio, non solo come figlio
            // diretto del FromCompendio: anche dentro un CONTENITORE annidato (es. la Stiva).
            List<Integer> idsRaggiungibili = itemRepository.findReachableItemIds(idPersonaggio);
            List<Collegamento> links = collegamentoRepository.findAllByItemTarget_Id(id).stream()
                    .filter(c -> c.getItemSource() != null && idsRaggiungibili.contains(c.getItemSource().getId()))
                    .toList();
            collegamentoRepository.deleteAll(links);
            // ancora referenziato da altri item (altri personaggi, armi, classi...): non toccare
            if (!collegamentoRepository.findAllByItemTarget_Id(id).isEmpty()) {
                return;
            }
        }

        hardDelete(itm);
    }

    /**
     * Scollega un item dall'equipaggiamento del personaggio (rimuove il collegamento dal suo
     * FromCompendio, o da qualunque CONTENITORE annidato sotto di esso — es. la Stiva di una
     * NAVE) senza toccare l'item, che resta nel compendio. Pensato per gli oggetti "persi".
     */
    @Transactional
    public void unlinkItem(Integer itemId, Integer idPersonaggio) {
        List<Integer> idsRaggiungibili = itemRepository.findReachableItemIds(idPersonaggio);
        List<Collegamento> links = collegamentoRepository.findAllByItemTarget_Id(itemId).stream()
                .filter(c -> c.getItemSource() != null && idsRaggiungibili.contains(c.getItemSource().getId()))
                .toList();
        if (links.isEmpty()) throw new RuntimeException("L'item non fa parte dell'equipaggiamento del personaggio");

        collegamentoRepository.deleteAll(links);
        personaggioCacheService.invalidaPersonaggio(idPersonaggio);
    }

    private void hardDelete(Item itm) {
        if (itm.getModificatori() != null && !itm.getModificatori().isEmpty()) {
            modificatoreRepository.deleteAll(itm.getModificatori());
        }
        collegamentoRepository.deleteAll(collegamentoRepository.findAllByItemSource_Id(itm.getId()));
        collegamentoRepository.deleteAll(collegamentoRepository.findAllByItemTarget_Id(itm.getId()));
        em.createQuery("DELETE FROM Avanzamento a WHERE a.itemSource.id = :id OR a.itemTarget.id = :id")
                .setParameter("id", itm.getId())
                .executeUpdate();
        em.createQuery("DELETE FROM PermessiItem p WHERE p.idItem.id = :id")
                .setParameter("id", itm.getId())
                .executeUpdate();
        // Item.labels è filtrata da @Where(id_personaggio IS NULL): la cascata JPA copre solo le
        // label globali, quindi quelle personaggio-scoped (QTA, DISABLED, UTILIZZI_USATI...)
        // resterebbero orfane facendo fallire la FK fk_item_label_item. Le cancelliamo a parte.
        em.createQuery("DELETE FROM ItemLabel il WHERE il.item.id = :id AND il.personaggio IS NOT NULL")
                .setParameter("id", itm.getId())
                .executeUpdate();
        itemRepository.delete(itm); // le labels globali seguono in cascata
    }

    /** QUEST e INFO condividono lo stesso concetto di albero/ambito (vedi ItemService#createItem). */
    private boolean isAmbitoTree(TipoItem tipo) {
        return TipoItem.QUEST.equals(tipo) || TipoItem.INFO.equals(tipo);
    }

    /**
     * Elimina una QUEST e tutto il suo sottoalbero di sotto-quest, per TUTTI i giocatori che la
     * vedono (a prescindere dall'ambito personaggio/party/mondo): di ogni quest coinvolta vengono
     * cancellati anche modificatori, collegamenti, avanzamenti, permessi e item_label.
     * <p>
     * Gli eventuali figli NON di tipo QUEST (item di compendio collegati alla quest, es. una
     * ricompensa) vengono soltanto scollegati: sono potenzialmente condivisi con altri
     * personaggi/item e cancellarli sarebbe distruttivo oltre l'intenzione.
     */
    @Transactional
    public void deleteQuestTree(Integer id) {
        deleteAmbitoTree(id, TipoItem.QUEST);
    }

    /** Come {@link #deleteQuestTree}, ma per un INFO e il suo sottoalbero di sotto-info. */
    @Transactional
    public void deleteInfoTree(Integer id) {
        deleteAmbitoTree(id, TipoItem.INFO);
    }

    private void deleteAmbitoTree(Integer id, TipoItem tipoAtteso) {
        Item root = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Item non trovato: " + id));
        if (!tipoAtteso.equals(root.getTipo()))
            throw new RuntimeException("L'item " + id + " non è di tipo " + tipoAtteso);

        // raccolgo l'albero PRIMA di toccare i collegamenti: dopo la cancellazione la
        // navigazione non troverebbe più i figli
        List<Item> daEliminare = new ArrayList<>();
        raccogliSottoAlbero(root, tipoAtteso, daEliminare, new HashSet<>());

        // idem per la cache: la risalita ai personaggi passa dai collegamenti
        for (Item q : daEliminare) personaggioCacheService.invalidaPerItem(q.getId());

        // dalle foglie alla radice, così un eventuale vincolo residuo emerge sul figlio
        for (int i = daEliminare.size() - 1; i >= 0; i--) {
            hardDelete(daEliminare.get(i));
        }
    }

    /** Visita in preordine dell'albero delle sotto-quest/sotto-info, a prova di ciclo. */
    private void raccogliSottoAlbero(Item item, TipoItem tipo, List<Item> out, Set<Integer> visti) {
        if (item == null || !visti.add(item.getId())) return;
        out.add(item);
        if (item.getChild() == null) return;
        for (Collegamento c : item.getChild()) {
            Item figlio = c.getItemTarget();
            if (figlio != null && tipo.equals(figlio.getTipo())) {
                raccogliSottoAlbero(figlio, tipo, out, visti);
            }
        }
    }

    /**
     * Garantisce che il personaggio abbia il suo item "FromCompendio"
     * (l'unico item, insieme a livelli e PreparedSpell, intestato direttamente
     * al personaggio: tutto il resto è collegato come suo child).
     */
    @Transactional
    public Item ensureFromCompendio(Integer idPersonaggio) {
        Item fromCompendio = itemRepository.findItemByNomeAndPersonaggio_Id(Constants.ITEM_FROM_COMPENDIO, idPersonaggio);
        if (fromCompendio != null) return fromCompendio;

        Personaggio pg = personaggioRepository.findPersonaggioById(idPersonaggio);
        if (pg == null) throw new RuntimeException("Personaggio non trovato: " + idPersonaggio);

        Item itm = new Item();
        itm.setNome(Constants.ITEM_FROM_COMPENDIO);
        itm.setTipo(TipoItem.ALTRO);
        itm.setDescrizione(Constants.ITEM_FROM_COMPENDIO);
        itm.setPersonaggio(pg);
        itm.setLabels(new ArrayList<>());
        if (pg.getParty() != null && pg.getParty().getMondo() != null) {
            Mondo mondo = pg.getParty().getMondo();
            itm.setMondo(mondo);
            itm.setSistema(mondo.getSistema());
        }
        return itemRepository.save(itm);
    }

    /**
     * Garantisce che il personaggio abbia il suo item "PreparedSpell" (contenitore
     * degli incantesimi preparati, intestato direttamente al personaggio). Sana i
     * personaggi più vecchi creati senza questo item.
     */
    @Transactional
    public Item ensurePreparedSpell(Integer idPersonaggio) {
        Item preparedSpell = itemRepository.findItemByNomeAndPersonaggio_Id(Constants.ITEM_INCANTESIMI_PREPARATI, idPersonaggio);
        if (preparedSpell != null) return preparedSpell;

        Personaggio pg = personaggioRepository.findPersonaggioById(idPersonaggio);
        if (pg == null) throw new RuntimeException("Personaggio non trovato: " + idPersonaggio);

        Item itm = new Item();
        itm.setNome(Constants.ITEM_INCANTESIMI_PREPARATI);
        itm.setTipo(TipoItem.ALTRO);
        itm.setDescrizione(Constants.ITEM_INCANTESIMI_PREPARATI);
        itm.setPersonaggio(pg);
        itm.setLabels(new ArrayList<>());
        if (pg.getParty() != null && pg.getParty().getMondo() != null) {
            Mondo mondo = pg.getParty().getMondo();
            itm.setMondo(mondo);
            itm.setSistema(mondo.getSistema());
        }
        return itemRepository.save(itm);
    }

    /**
     * Collega un item esistente del compendio al personaggio tramite il suo FromCompendio.
     * L'item nasce disabilitato (come per createItem) e va abilitato esplicitamente.
     */
    @Transactional
    public void linkItem(Integer itemId, Integer idPersonaggio) {
        Item target = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item non trovato: " + itemId));

        Item fromCompendio = ensureFromCompendio(idPersonaggio);

        boolean giaPresente = collegamentoRepository.findAllByItemTarget_Id(itemId).stream()
                .anyMatch(c -> Objects.equals(c.getItemSource().getId(), fromCompendio.getId()));
        if (giaPresente) return; // idempotente

        Collegamento link = new Collegamento();
        link.setItemSource(fromCompendio);
        link.setItemTarget(target);
        link.setLabels(new ArrayList<>());
        collegamentoRepository.save(link);
        // un item appena collegato nasce disabilitato: va abilitato esplicitamente
        saveDisabledPerPersonaggio(target.getId(), idPersonaggio, true);
        personaggioCacheService.invalidaPersonaggio(idPersonaggio);
    }

    @Transactional
    public Item updateItem(Integer id, UpdateItemRequest request) {
        return updateItem(id, request, null);
    }

    @Transactional
    public Item updateItem(Integer id, UpdateItemRequest request, Integer idPersonaggio) {
        Item itm = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Item non trovato"));

        if (request.getNome() != null && !request.getNome().trim().isEmpty()) itm.setNome(request.getNome().trim());
        if (request.getDescrizione() != null) itm.setDescrizione(request.getDescrizione());

        // mondo/sistema aggiornabili anche in modifica
        if (request.getIdMondo() != null) {
            Mondo mondo = em.find(Mondo.class, request.getIdMondo());
            if (mondo != null) itm.setMondo(mondo);
        }
        if (request.getIdSistema() != null) {
            Sistema sistema = em.find(Sistema.class, request.getIdSistema());
            if (sistema != null) itm.setSistema(sistema);
        }

        // labels: stato completo -> svuota in place e ricrea (orphanRemoval).
        // QTA con idPersonaggio viene salvata come label per-personaggio (non globale).
        if (request.getLabels() != null) {
            if (itm.getLabels() != null) {
                Iterator<ItemLabel> it = itm.getLabels().iterator();
                while (it.hasNext()) {
                    ItemLabel l = it.next();
                    it.remove();
                    l.setItem(null);
                }
            } else {
                itm.setLabels(new ArrayList<>());
            }
            for (UpdateItemRequest.LabelRowDTO l : request.getLabels()) {
                if (Constants.LABEL_QTA.equals(l.getLabel()) && idPersonaggio != null) {
                    saveQtaPerPersonaggio(id, idPersonaggio, l.getValore());
                } else {
                    addLabelRow(itm, l.getLabel(), l.getValore());
                }
            }
        }

        applyModificatori(itm, request.getModificatori());
        applyAttacchi(itm, request.getAttacchi());
        applyChildren(itm, request.getChildren());
        if (TipoItem.NODO.equals(itm.getTipo())) {
            applyNodoTipo(itm, request.getNodoTipoItemId());
            applyNodoA(itm, request.getNodoA());
            applyNodoDa(itm, request.getNodoDa());
        }

        Item saved = itemRepository.save(itm);
        // se è un TAG e la sua categoria è cambiata, riallinea la colonna denormalizzata
        // su tutte le associazioni item_tag già esistenti
        if (TipoItem.TAG.equals(saved.getTipo())) itemTagService.riallineaCategoria(saved);
        personaggioCacheService.invalidaPerItem(saved.getId());
        return saved;
    }

    /**
     * Imposta gli hp consumati di una barriera (label BARR_CONS), clampato 0..BARR_MAX.
     */
    @Transactional
    public Item updateBarriera(Integer itemId, int consumato) {
        Item itm = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item non trovato"));
        if (!Constants.ITEM_TIPO_BARRIERA.equalsIgnoreCase(itm.getLabel(Constants.ITEM_LABEL_TIPO)))
            throw new RuntimeException("L'item non è una barriera");

        int max = parseIntOrZero(itm.getLabel(Constants.ITEM_LABEL_BARR_MAX));
        int cons = Math.max(0, Math.min(consumato, max));
        itm.setLabel(Constants.ITEM_LABEL_BARR_CONS, String.valueOf(cons));
        Item saved = itemRepository.save(itm);
        personaggioCacheService.invalidaPerItem(saved.getId());
        return saved;
    }

    private static int parseIntOrZero(String s) {
        if (s == null) return 0;
        try {
            return Math.max(0, Integer.parseInt(s.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Restituisce gli item che hanno questo item come child (i suoi "padri"),
     * escluso il FromCompendio del personaggio. Deduplicati per id.
     */
    public List<ItemDTO> getParents(Integer itemId) {
        Map<Integer, Item> byId = new LinkedHashMap<>();
        for (Collegamento c : collegamentoRepository.findAllByItemTarget_Id(itemId)) {
            Item src = c.getItemSource();
            if (src == null) continue;
            if (Constants.ITEM_FROM_COMPENDIO.equals(src.getNome())) continue;
            byId.putIfAbsent(src.getId(), src);
        }
        return byId.values().stream().map(itemMapper::toDTO).toList();
    }

    /**
     * Le notizie disabilitate non si vedono mai. Quelle abilitate ma non ancora iniziate
     * (dataInizio futura) restano escluse. Quelle abilitate con dataFine passata NON vengono
     * più escluse: sono "archiviata" (il frontend le mostra in una sezione separata) invece
     * di sparire del tutto.
     */
    public List<NotiziaDTO> getNotizie() {
        LocalDateTime ora = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        return itemRepository.findAllNotizie().stream()
                .filter(n -> "1".equals(n.getLabel(Constants.LABEL_NOTIZIA_ABILITATA)))
                .filter(n -> {
                    String inizio = n.getLabel(Constants.LABEL_NOTIZIA_DATA_INIZIO);
                    try {
                        if (inizio != null && !inizio.isBlank() && ora.isBefore(LocalDateTime.parse(inizio, fmt))) return false;
                    } catch (Exception ignored) {}
                    return true;
                })
                .map(n -> {
                    String fine = n.getLabel(Constants.LABEL_NOTIZIA_DATA_FINE);
                    boolean archiviata = false;
                    try {
                        archiviata = fine != null && !fine.isBlank() && ora.isAfter(LocalDateTime.parse(fine, fmt));
                    } catch (Exception ignored) {}
                    return new NotiziaDTO(
                            n.getId(),
                            n.getNome(),
                            n.getDescrizione(),
                            n.getLabel(Constants.LABEL_NOTIZIA_DATA_INIZIO),
                            fine,
                            archiviata);
                })
                .toList();
    }

    public List<Item> searchItems(String query, TipoItem tipo, Integer idMondo) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return List.of();
        var top20 = org.springframework.data.domain.PageRequest.of(0, 20);
        // Cerca sia nel nome sia nella label EN_NAME (nome originale inglese). idMondo confina la
        // ricerca al mondo selezionato: i mondi sono compartimenti stagni, niente item di altri
        // mondi tra i risultati (idMondo null = nessun filtro, solo per contesti senza un mondo).
        return tipo == null
                ? itemRepository.findTop20ByNomeOrEnNameContainingIgnoreCase(q, idMondo, top20)
                : itemRepository.findTop20ByNomeOrEnNameContainingIgnoreCaseAndTipo(q, tipo, idMondo, top20);
    }

    /** Alberi (valori distinti di ALBERO_NODO) tra i NODO di un mondo: lista da mostrare nella pagina "Alberi". */
    public List<String> getAlberiNodo(Integer idMondo) {
        return itemRepository.findAlberiNodo(idMondo);
    }

    /**
     * Grafo di un albero di NODO: un {@link NodoAlberoDTO} per nodo, con gli id dei figli "A" (già
     * ristretti a quelli nello stesso albero). Il frontend deriva le radici (nessun genitore, cioè
     * nessun id che compare come figlio altrove) e i livelli da questi archi.
     */
    public List<NodoAlberoDTO> getAlberoNodo(Integer idMondo, String albero) {
        List<Item> nodi = itemRepository.findNodiByMondoAndAlbero(idMondo, albero);
        Set<Integer> idsNelSet = nodi.stream().map(Item::getId).collect(Collectors.toSet());
        return nodi.stream().map(n -> {
            List<Collegamento> child = n.getChild() != null ? n.getChild() : List.of();
            List<Integer> figli = child.stream()
                    .filter(c -> TipoItem.NODO.equals(c.getItemTarget().getTipo()))
                    .map(c -> c.getItemTarget().getId())
                    .filter(idsNelSet::contains)
                    .toList();
            String tipoNome = child.stream()
                    .filter(c -> !TipoItem.NODO.equals(c.getItemTarget().getTipo()))
                    .map(c -> c.getItemTarget().getNome())
                    .findFirst().orElse(null);
            return new NodoAlberoDTO(n.getId(), n.getNome(), tipoNome, figli);
        }).toList();
    }

    /**
     * Allinea gli attacchi (item ATTACCO figli) allo stato richiesto:
     * aggiorna quelli con id, crea i nuovi (item + collegamento), elimina i
     * collegamenti non più presenti e l'item ATTACCO se non più referenziato.
     * Null = non toccare.
     */
    private void applyAttacchi(Item itm, List<UpdateItemRequest.AttaccoRowDTO> rows) {
        if (rows == null) return;

        List<Collegamento> linkAttacchi = (itm.getChild() != null ? itm.getChild() : List.<Collegamento>of()).stream()
                .filter(c -> TipoItem.ATTACCO.equals(c.getItemTarget().getTipo()))
                .toList();

        Map<Integer, Item> esistentiById = linkAttacchi.stream()
                .collect(Collectors.toMap(c -> c.getItemTarget().getId(), Collegamento::getItemTarget, (a, b) -> a));

        Set<Integer> richiesti = rows.stream()
                .map(UpdateItemRequest.AttaccoRowDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // elimina collegamenti (e attacchi orfani) non più presenti
        for (Collegamento link : linkAttacchi) {
            Item attacco = link.getItemTarget();
            if (richiesti.contains(attacco.getId())) continue;
            collegamentoRepository.delete(link);
            if (itm.getChild() != null) itm.getChild().remove(link); // allinea la collection in memoria
            // se l'attacco non è referenziato da nessun altro item, eliminalo
            long altriParent = attacco.getParent() == null ? 0
                    : attacco.getParent().stream().filter(p -> !Objects.equals(p.getId(), link.getId())).count();
            if (altriParent == 0 && attacco.getPersonaggio() == null) {
                if (attacco.getModificatori() != null) modificatoreRepository.deleteAll(attacco.getModificatori());
                itemRepository.delete(attacco);
            }
        }

        for (UpdateItemRequest.AttaccoRowDTO r : rows) {
            if (r.getNome() == null || r.getNome().trim().isEmpty()) continue;

            // un attacco risolve o con TPC o con TS, mai entrambi: pulisci l'altro set di label
            // in base a tipoRisoluzione, così cambiare modalità nell'editor non lascia residui.
            boolean isTs = "TS".equals(r.getTipoRisoluzione());
            String tpcVal = isTs ? null : r.getTpc();
            String ttsVal = isTs ? r.getTiroSalvezza() : null;
            String ttsCdVal = isTs ? r.getTiroSalvezzaCd() : null;

            Item attacco;
            if (r.getId() != null) {
                // aggiorna attacco esistente
                attacco = esistentiById.get(r.getId());
                if (attacco == null) continue; // id non figlio di questo item: ignora
                attacco.setNome(r.getNome().trim());
            } else {
                // crea nuovo attacco + collegamento
                attacco = new Item();
                attacco.setNome(r.getNome().trim());
                attacco.setTipo(TipoItem.ATTACCO);
                attacco.setLabels(new ArrayList<>());
            }

            putSingleLabel(attacco, Constants.ITEM_LABEL_ATTACCO_TIPO_RISOLUZIONE, r.getTipoRisoluzione());
            putSingleLabel(attacco, Constants.ITEM_LABEL_ATTACCO_TIRO_PER_COLPIRE, tpcVal);
            putSingleLabel(attacco, Constants.ITEM_LABEL_ATTACCO_TIRO_SALVEZZA, ttsVal);
            putSingleLabel(attacco, Constants.ITEM_LABEL_ATTACCO_TIRO_SALVEZZA_CD, ttsCdVal);
            // legacy: un attacco creato/salvato con questo nuovo modello non usa più TPD/TDANNO singoli
            attacco.removeLabel(Constants.ITEM_LABEL_ATTACCO_DANNI);
            attacco.removeLabel(Constants.ITEM_LABEL_ATTACCO_TIPO_DANNI);
            replaceMultiLabel(attacco, Constants.ITEM_LABEL_ATTACCO_DANNO, encodeDanni(r.getDanni()));

            if (r.getId() != null) {
                itemRepository.save(attacco);
            } else {
                Item savedAttacco = itemRepository.save(attacco);
                Collegamento link = new Collegamento();
                link.setItemSource(itm);
                link.setItemTarget(savedAttacco);
                collegamentoRepository.save(link);
            }
        }
    }

    private static final String DANNO_SEP = "␞"; // separatore invisibile, non capita mai in una formula/nome tipo

    private List<String> encodeDanni(List<UpdateItemRequest.DannoRowDTO> danni) {
        if (danni == null) return List.of();
        List<String> out = new ArrayList<>();
        for (UpdateItemRequest.DannoRowDTO d : danni) {
            if (d == null || d.getFormula() == null || d.getFormula().isBlank()) continue;
            out.add(d.getFormula().trim() + DANNO_SEP + (d.getTipo() == null ? "" : d.getTipo().trim()));
        }
        return out;
    }

    /** Sostituisce integralmente tutte le righe label con questa chiave con i nuovi valori (in ordine). */
    private void replaceMultiLabel(Item item, String key, List<String> values) {
        item.removeLabel(key);
        for (String v : values) addLabelRow(item, key, v);
    }

    /**
     * Allinea gli item collegati come child (esclusi gli ATTACCO) allo stato
     * richiesto: crea i collegamenti mancanti, elimina quelli non più presenti
     * (solo il collegamento, mai l'item target). Null = non toccare.
     */
    private void applyChildren(Item itm, List<UpdateItemRequest.ChildRefDTO> children) {
        if (children == null) return;

        record ChildInfo(Integer qty, String formulaQty, String scelta, boolean nascosto, String condizione) {
        }
        Map<Integer, ChildInfo> desiderati = new HashMap<>();
        for (UpdateItemRequest.ChildRefDTO c : children)
            desiderati.put(c.getId(), new ChildInfo(c.getQty(), c.getFormulaQty(), c.getScelta(), Boolean.TRUE.equals(c.getNascosto()), c.getCondizione()));

        // esclusi ATTACCO (gestiti da applyAttacchi) e NODO (struttura ad albero, gestita da
        // applyNodoTipo/applyNodoA qui sotto — non deve mai essere toccata da questo metodo,
        // anche se in futuro ITEM_COLLEGATI venisse abilitata per un NODO)
        List<Collegamento> linkAltri = (itm.getChild() != null ? itm.getChild() : List.<Collegamento>of()).stream()
                .filter(c -> !TipoItem.ATTACCO.equals(c.getItemTarget().getTipo()))
                .filter(c -> !TipoItem.NODO.equals(c.getItemTarget().getTipo()))
                .toList();

        List<Collegamento> daEliminare = linkAltri.stream()
                .filter(c -> !desiderati.containsKey(c.getItemTarget().getId()))
                .toList();
        collegamentoRepository.deleteAll(daEliminare);
        if (itm.getChild() != null) itm.getChild().removeAll(daEliminare);

        Map<Integer, Collegamento> giaPresenti = linkAltri.stream()
                .collect(Collectors.toMap(c -> c.getItemTarget().getId(), c -> c));

        for (Map.Entry<Integer, ChildInfo> entry : desiderati.entrySet()) {
            Integer targetId = entry.getKey();
            ChildInfo info = entry.getValue();
            if (Objects.equals(targetId, itm.getId())) continue; // no self-link
            if (giaPresenti.containsKey(targetId)) {
                Collegamento existing = giaPresenti.get(targetId);
                boolean existingNascosto = Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE.equals(existing.getLabel(Constants.ITEM_LABEL_NASCOSTO));
                String existingCondizione = existing.getLabel(Constants.COLLEGAMENTO_LABEL_CONDIZIONE);
                boolean changed = !Objects.equals(existing.getQty(), info.qty())
                        || !Objects.equals(existing.getFormulaQty(), info.formulaQty())
                        || !Objects.equals(existing.getScelta(), info.scelta())
                        || existingNascosto != info.nascosto()
                        || !Objects.equals(existingCondizione, info.condizione());
                if (changed) {
                    existing.setQty(info.qty());
                    existing.setFormulaQty(info.formulaQty());
                    existing.setScelta(info.scelta());
                    existing.setLabel(Constants.ITEM_LABEL_NASCOSTO, info.nascosto() ? Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE : null);
                    existing.setLabel(Constants.COLLEGAMENTO_LABEL_CONDIZIONE, info.condizione());
                    collegamentoRepository.save(existing);
                }
            } else {
                Item target = itemRepository.findById(targetId)
                        .orElseThrow(() -> new RuntimeException("Item da collegare non trovato: " + targetId));
                Collegamento link = new Collegamento();
                link.setItemSource(itm);
                link.setItemTarget(target);
                link.setQty(info.qty());
                link.setFormulaQty(info.formulaQty());
                link.setScelta(info.scelta());
                if (info.nascosto()) link.setLabel(Constants.ITEM_LABEL_NASCOSTO, Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE);
                if (info.condizione() != null) link.setLabel(Constants.COLLEGAMENTO_LABEL_CONDIZIONE, info.condizione());
                collegamentoRepository.save(link);
            }
        }
    }

    /**
     * Solo tipo NODO (card NODO_STRUTTURA): collegamento singolo al "contenuto" del nodo (un item
     * di qualunque tipo, incluso eventualmente un altro NODO). Cardinalità 1: il Collegamento con
     * target.tipo diverso da NODO tra i child di questo item (al massimo uno, per costruzione).
     * Null = rimuove il collegamento esistente (non "non toccare": il form invia sempre lo stato
     * corrente, coerente con come BaseItemEditor.vue costruisce il payload).
     */
    private void applyNodoTipo(Item itm, Integer targetId) {
        Collegamento esistente = (itm.getChild() != null ? itm.getChild() : List.<Collegamento>of()).stream()
                .filter(c -> !TipoItem.NODO.equals(c.getItemTarget().getTipo()))
                .findFirst().orElse(null);
        if (targetId == null) {
            if (esistente != null) {
                collegamentoRepository.delete(esistente);
                itm.getChild().remove(esistente);
            }
            return;
        }
        if (esistente != null && Objects.equals(esistente.getItemTarget().getId(), targetId)) return; // già corretto
        if (esistente != null) {
            collegamentoRepository.delete(esistente);
            itm.getChild().remove(esistente);
        }
        Item target = itemRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("Item da collegare non trovato: " + targetId));
        Collegamento link = new Collegamento();
        link.setItemSource(itm);
        link.setItemTarget(target);
        collegamentoRepository.save(link);
    }

    /**
     * Solo tipo NODO: nodi NODO successivi (verso cui si può andare da questo nodo) — stato
     * completo desiderato, stesso pattern "diff e riallinea" di {@link #applyChildren}, ma
     * ristretto ai Collegamento con target.tipo=NODO. Null = non toccare.
     */
    private void applyNodoA(Item itm, List<Integer> desideratiIds) {
        if (desideratiIds == null) return;
        List<Collegamento> attuali = (itm.getChild() != null ? itm.getChild() : List.<Collegamento>of()).stream()
                .filter(c -> TipoItem.NODO.equals(c.getItemTarget().getTipo()))
                .toList();
        Set<Integer> desiderati = new HashSet<>(desideratiIds);

        List<Collegamento> daEliminare = attuali.stream()
                .filter(c -> !desiderati.contains(c.getItemTarget().getId()))
                .toList();
        collegamentoRepository.deleteAll(daEliminare);
        if (itm.getChild() != null) itm.getChild().removeAll(daEliminare);

        Set<Integer> restanti = attuali.stream()
                .map(c -> c.getItemTarget().getId())
                .filter(desiderati::contains)
                .collect(Collectors.toSet());

        for (Integer targetId : desideratiIds) {
            if (Objects.equals(targetId, itm.getId())) continue; // no self-link
            if (restanti.contains(targetId)) continue;
            Item target = itemRepository.findById(targetId)
                    .orElseThrow(() -> new RuntimeException("Nodo da collegare non trovato: " + targetId));
            if (!TipoItem.NODO.equals(target.getTipo()))
                throw new RuntimeException("L'item " + targetId + " non è un NODO");
            Collegamento link = new Collegamento();
            link.setItemSource(itm);
            link.setItemTarget(target);
            collegamentoRepository.save(link);
        }
    }

    /**
     * Solo tipo NODO: nodi NODO predecessori (da cui si arriva a questo nodo). Non esiste una
     * colonna "Da" su questo item: editare questa lista scrive invece sull'ALTRO nodo, aggiungendo
     * o togliendo QUESTO item dalla SUA {@link #applyNodoA}. Stato completo desiderato — il
     * predecessore attuale si ottiene con una query sui parent (Collegamento con questo item come
     * target, sorgente di tipo NODO), non da una relazione diretta sull'item. Null = non toccare.
     */
    private void applyNodoDa(Item itm, List<Integer> desideratiIds) {
        if (desideratiIds == null) return;
        List<Collegamento> attualiInArrivo = collegamentoRepository.findAllByItemTarget_Id(itm.getId()).stream()
                .filter(c -> TipoItem.NODO.equals(c.getItemSource().getTipo()))
                .toList();
        Set<Integer> desiderati = new HashSet<>(desideratiIds);

        List<Collegamento> daEliminare = attualiInArrivo.stream()
                .filter(c -> !desiderati.contains(c.getItemSource().getId()))
                .toList();
        collegamentoRepository.deleteAll(daEliminare);

        Set<Integer> restanti = attualiInArrivo.stream()
                .map(c -> c.getItemSource().getId())
                .filter(desiderati::contains)
                .collect(Collectors.toSet());

        for (Integer sourceId : desideratiIds) {
            if (Objects.equals(sourceId, itm.getId())) continue; // no self-link
            if (restanti.contains(sourceId)) continue;
            Item source = itemRepository.findById(sourceId)
                    .orElseThrow(() -> new RuntimeException("Nodo da collegare non trovato: " + sourceId));
            if (!TipoItem.NODO.equals(source.getTipo()))
                throw new RuntimeException("L'item " + sourceId + " non è un NODO");
            Collegamento link = new Collegamento();
            link.setItemSource(source);
            link.setItemTarget(itm);
            collegamentoRepository.save(link);
        }
    }

    /**
     * Allinea i modificatori dell'item allo stato richiesto:
     * elimina quelli assenti, aggiorna quelli con id, crea quelli senza id.
     * Null = non toccare.
     */
    private void applyModificatori(Item itm, List<UpdateItemRequest.ModificatoreRowDTO> rows) {
        if (rows == null) return;

        List<Modificatore> esistenti = itm.getModificatori() != null
                ? new ArrayList<>(itm.getModificatori())
                : new ArrayList<>();

        Map<Integer, UpdateItemRequest.ModificatoreRowDTO> byId = rows.stream()
                .filter(r -> r.getId() != null)
                .collect(Collectors.toMap(UpdateItemRequest.ModificatoreRowDTO::getId, r -> r, (a, b) -> a));

        // elimina i non più presenti
        List<Modificatore> daEliminare = esistenti.stream()
                .filter(m -> !byId.containsKey(m.getId()))
                .toList();
        modificatoreRepository.deleteAll(daEliminare);
        if (itm.getModificatori() != null) itm.getModificatori().removeAll(daEliminare); // allinea la collection in memoria

        // aggiorna gli esistenti
        for (Modificatore m : esistenti) {
            UpdateItemRequest.ModificatoreRowDTO r = byId.get(m.getId());
            if (r == null) continue;
            m.setStat(findStat(r.getStatId()));
            if (r.getTipo() != null) m.setTipo(r.getTipo());
            if (r.getValore() != null) m.setValore(r.getValore());
            m.setNota(r.getNota());
            m.setSempreAttivo(r.getSempreAttivo());
            modificatoreRepository.save(m);
        }

        // crea i nuovi
        for (UpdateItemRequest.ModificatoreRowDTO r : rows) {
            if (r.getId() != null) continue;
            Modificatore m = new Modificatore();
            m.setItem(itm);
            m.setStat(findStat(r.getStatId()));
            m.setTipo(r.getTipo() != null ? r.getTipo() : TipoModificatore.MOD);
            m.setValore(r.getValore() != null ? r.getValore() : "0");
            m.setNota(r.getNota());
            m.setSempreAttivo(r.getSempreAttivo());
            modificatoreRepository.save(m);
        }
    }

    private Stat findStat(String statId) {
        if (statId == null || statId.trim().isEmpty()) throw new RuntimeException("Stat obbligatoria sul modificatore");
        Stat s = em.find(Stat.class, statId.trim());
        if (s == null) throw new RuntimeException("Stat non trovata: " + statId);
        return s;
    }

    /* =====================================================================
     * Salvataggio item LIVELLO
     * ===================================================================== */

    @Transactional
    public Item updateLivello(Integer id, UpdateLivelloRequest request) {
        Item livello = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Item non trovato"));
        if (!TipoItem.LIVELLO.equals(livello.getTipo()))
            throw new RuntimeException("L'item " + id + " non è di tipo LIVELLO");

        // --- labels base del livello ---
        putSingleLabel(livello, Constants.ITEM_LIVELLO_LVL,
                request.getLivello() == null ? null : String.valueOf(request.getLivello()));
        putSingleLabel(livello, Constants.ITEM_LABEL_CLASSE,
                request.getClasseId() == null ? null : String.valueOf(request.getClasseId()));
        putSingleLabel(livello, Constants.ITEM_LABEL_MALEDIZIONE, request.getMaledizioneNome());
        putSingleLabel(livello, Constants.ITEM_LABEL_DADI_VITA, request.getDv());
        // gradi congelati dal frontend (somma calcolata, eventualmente corretta a mano)
        if (request.getGradi() != null) {
            putSingleLabel(livello, Constants.ITEM_LABEL_GRADI_LIVELLO, String.valueOf(request.getGradi()));
        }

        String lvlClasse = (request.getLivelliClasse() == null || request.getLivelliClasse().isEmpty())
                ? null
                : request.getLivelliClasse().stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
        putSingleLabel(livello, Constants.ITEM_LIVELLO_LVL_CLASSE, lvlClasse);

        // --- caratteristiche -> modificatori BASE ---
        if (request.getCaratteristiche() != null) {
            Map<String, String> desiderati = request.getCaratteristiche().entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
            replaceModificatoriPerTipo(livello, TipoModificatore.BASE, desiderati);
        }

        // --- ranghi -> modificatori RANK ---
        if (request.getRanghi() != null) {
            Map<String, String> desiderati = request.getRanghi().stream()
                    .filter(r -> r.getAbilitaId() != null && r.getPunti() != null && r.getPunti() > 0)
                    .collect(Collectors.toMap(UpdateLivelloRequest.RangoSpesoDTO::getAbilitaId,
                            r -> String.valueOf(r.getPunti()), (a, b) -> a));
            replaceModificatoriPerTipo(livello, TipoModificatore.RANK, desiderati);
        }

        // --- contenuti del livello (grants) ---
        applyGrants(livello, request.getGrantsSelezionati(), request.getClasseId());

        // --- modificatori liberi (aggiunti a mano) ---
        applyModificatoriLiberi(livello, request.getModificatoriLiberi());

        // --- congela i gradi del livello (non retroattivo): se manca GRADI_LIVELLO,
        //     calcola dalla formula della classe (RANK_1/RANK) con l'INT attuale e salva.
        if (livello.getLabel(Constants.ITEM_LABEL_GRADI_LIVELLO) == null
                && request.getClasseId() != null
                && request.getLivello() != null
                && livello.getPersonaggio() != null) {
            Item classe = itemRepository.findById(request.getClasseId()).orElse(null);
            int numLivelli = (request.getLivelliClasse() != null && !request.getLivelliClasse().isEmpty())
                    ? request.getLivelliClasse().size() : 1;
            Integer gradi = personaggioService.computeGradi(classe, request.getLivello(), livello.getPersonaggio().getId(), numLivelli);
            if (gradi != null) {
                putSingleLabel(livello, Constants.ITEM_LABEL_GRADI_LIVELLO, String.valueOf(gradi));
            }
        }

        Item saved = itemRepository.save(livello);
        // i LIVELLO sono sempre intestati direttamente a un personaggio (mai condivisi)
        if (saved.getPersonaggio() != null) personaggioCacheService.invalidaPersonaggio(saved.getPersonaggio().getId());
        return saved;
    }

    /**
     * Aggiorna SOLO i modificatori RANK di un livello (pagina "Gestisci gradi"),
     * senza toccare labels, caratteristiche o contenuti concessi.
     */
    @Transactional
    public Item updateRanghiLivello(Integer id, UpdateLivelloRequest request) {
        Item livello = caricaLivello(id, null);
        applicaRanghi(livello, request.getRanghi());
        Item saved = itemRepository.save(livello);
        if (saved.getPersonaggio() != null) personaggioCacheService.invalidaPersonaggio(saved.getPersonaggio().getId());
        return saved;
    }

    /**
     * Aggiorna i ranghi di più livelli in un'unica transazione (salvataggio bulk
     * della pagina "Gestisci gradi"): o vanno a buon fine tutti, o nessuno.
     */
    @Transactional
    public void updateRanghiLivelliBulk(UpdateRanghiBulkRequest request) {
        if (request.getLivelli() == null) return;
        for (UpdateRanghiBulkRequest.LivelloRanghi l : request.getLivelli()) {
            if (l.getLivelloId() == null) continue;
            Item livello = caricaLivello(l.getLivelloId(), request.getPersonaggioId());
            applicaRanghi(livello, l.getRanghi());
            applicaSkillTrickRanghi(livello, l.getSkillTrick());
            itemRepository.save(livello);
        }
        reconcileSkillTricks(request.getPersonaggioId());
        personaggioCacheService.invalidaPersonaggio(request.getPersonaggioId());
    }

    private static final int SKILL_TRICK_PUNTI_SBLOCCO = 2;

    /**
     * Sostituisce i modificatori RANK sull'unica stat {@link Constants#STAT_SKILL_TRICK} di questo
     * livello: uno per ogni Skill Trick su cui il personaggio ha investito punti, distinti dal
     * campo "nota" (= id dell'item SKILL_TRICK nel compendio). A differenza di
     * {@link #applicaRanghi}, qui più modificatori condividono la stessa stat, quindi non si può
     * riusare {@link #replaceModificatoriPerTipo} (che sincronizza per statId).
     */
    private void applicaSkillTrickRanghi(Item livello, List<UpdateRanghiBulkRequest.SkillTrickPuntoDTO> punti) {
        if (punti == null) return; // null = non toccare (coerente con applicaRanghi/applyGrants)
        Map<String, String> desiderati = punti.stream()
                .filter(p -> p.getItemId() != null && p.getPunti() != null && p.getPunti() > 0)
                .collect(Collectors.toMap(p -> String.valueOf(p.getItemId()),
                        p -> String.valueOf(p.getPunti()), (a, b) -> a));

        List<Modificatore> esistenti = livello.getModificatori() != null
                ? livello.getModificatori().stream()
                    .filter(m -> TipoModificatore.RANK.equals(m.getTipo())
                            && m.getStat() != null && Constants.STAT_SKILL_TRICK.equals(m.getStat().getId()))
                    .toList()
                : List.of();

        Map<String, String> rimanenti = new HashMap<>(desiderati);
        for (Modificatore m : esistenti) {
            String nuovoValore = rimanenti.remove(m.getNota());
            if (nuovoValore == null) {
                modificatoreRepository.delete(m);
                if (livello.getModificatori() != null) livello.getModificatori().remove(m);
            } else if (!nuovoValore.equals(m.getValore())) {
                m.setValore(nuovoValore);
                modificatoreRepository.save(m);
            }
        }

        if (rimanenti.isEmpty()) return;
        Stat stat = findStat(Constants.STAT_SKILL_TRICK);
        for (Map.Entry<String, String> e : rimanenti.entrySet()) {
            Modificatore m = new Modificatore();
            m.setItem(livello);
            m.setStat(stat);
            m.setTipo(TipoModificatore.RANK);
            m.setValore(e.getValue());
            m.setNota(e.getKey());
            m.setSempreAttivo(true);
            modificatoreRepository.save(m);
        }
    }

    /**
     * Ogni volta che si salvano i ranghi (pagina "Gestisci gradi"), sincronizza gli Skill Trick
     * sbloccati: se un personaggio ha investito almeno {@value #SKILL_TRICK_PUNTI_SBLOCCO} punti
     * complessivi (su tutti i livelli) in un dato Skill Trick (modificatori RANK sulla stat
     * {@link Constants#STAT_SKILL_TRICK}, raggruppati per "nota" = id item), l'item corrispondente
     * viene collegato (Collegamento) al livello più alto in cui ha ranghi > 0 su quel trick, come
     * se fosse un contenuto concesso da quel livello. Se i punti scendono sotto la soglia, il
     * collegamento viene rimosso.
     */
    private void reconcileSkillTricks(Integer personaggioId) {
        if (personaggioId == null) return;
        List<Item> livelli = itemRepository.findAllByPersonaggio_IdAndTipo(personaggioId, TipoItem.LIVELLO);
        if (livelli.isEmpty()) return;

        Map<Integer, Integer> numeroLivello = new HashMap<>();
        for (Item liv : livelli) {
            int lv = 0;
            try {
                lv = Integer.parseInt(liv.getLabel(Constants.ITEM_LIVELLO_LVL));
            } catch (Exception ignored) {
            }
            numeroLivello.put(liv.getId(), lv);
        }

        Map<Integer, Integer> totali = new HashMap<>();   // itemId -> punti totali
        Map<Integer, Item> livelloMax = new HashMap<>();  // itemId -> livello più alto con punti > 0
        // Query diretta invece di liv.getModificatori(): quella collezione lazy può essere già
        // stata inizializzata (e quindi non riflettere i modificatori appena creati) da
        // applicaSkillTrickRanghi più sopra, nella STESSA transazione, per i livelli appena
        // salvati — Hibernate non risincronizza un OneToMany lazy già caricato quando l'entità
        // figlia viene persistita passando solo dal lato "owning" (Modificatore.item).
        List<Modificatore> mods = modificatoreRepository.findRankModificatoriSuStatByPersonaggioELivelli(
                personaggioId, Constants.STAT_SKILL_TRICK);
        for (Modificatore m : mods) {
            if (m.getNota() == null) continue;
            Item liv = m.getItem();
            int lv = numeroLivello.getOrDefault(liv.getId(), 0);
            Integer itemId;
            int punti;
            try {
                itemId = Integer.valueOf(m.getNota().trim());
                punti = Integer.parseInt(m.getValore());
            } catch (Exception e) {
                continue;
            }
            if (punti <= 0) continue;
            totali.merge(itemId, punti, Integer::sum);
            Item corrente = livelloMax.get(itemId);
            if (corrente == null || lv > numeroLivello.getOrDefault(corrente.getId(), 0)) {
                livelloMax.put(itemId, liv);
            }
        }

        for (Map.Entry<Integer, Integer> entry : totali.entrySet()) {
            Integer itemId = entry.getKey();
            int totale = entry.getValue();

            Collegamento esistente = null;
            for (Item liv : livelli) {
                if (liv.getChild() == null) continue;
                for (Collegamento c : liv.getChild()) {
                    if (c.getItemTarget() != null && itemId.equals(c.getItemTarget().getId())) {
                        esistente = c;
                        break;
                    }
                }
                if (esistente != null) break;
            }

            if (totale >= SKILL_TRICK_PUNTI_SBLOCCO) {
                if (esistente == null) {
                    Item livelloTarget = livelloMax.get(itemId);
                    Item target = itemRepository.findById(itemId).orElse(null);
                    if (target == null || livelloTarget == null) continue;
                    Collegamento c = new Collegamento();
                    c.setItemSource(livelloTarget);
                    c.setItemTarget(target);
                    collegamentoRepository.save(c);
                }
            } else if (esistente != null) {
                collegamentoRepository.delete(esistente);
            }
        }
    }

    /** Carica un item LIVELLO verificando il tipo e (se passato) l'appartenenza al personaggio. */
    private Item caricaLivello(Integer id, Integer personaggioId) {
        Item livello = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Item non trovato"));
        if (!TipoItem.LIVELLO.equals(livello.getTipo()))
            throw new RuntimeException("L'item " + id + " non è di tipo LIVELLO");
        if (personaggioId != null && (livello.getPersonaggio() == null
                || !personaggioId.equals(livello.getPersonaggio().getId())))
            throw new RuntimeException("Il livello " + id + " non appartiene al personaggio " + personaggioId);
        return livello;
    }

    /** Sostituisce i modificatori RANK del livello con i ranghi (punti > 0) indicati. */
    private void applicaRanghi(Item livello, List<UpdateLivelloRequest.RangoSpesoDTO> ranghi) {
        Map<String, String> desiderati = (ranghi == null ? List.<UpdateLivelloRequest.RangoSpesoDTO>of() : ranghi).stream()
                .filter(r -> r.getAbilitaId() != null && r.getPunti() != null && r.getPunti() > 0)
                .collect(Collectors.toMap(UpdateLivelloRequest.RangoSpesoDTO::getAbilitaId,
                        r -> String.valueOf(r.getPunti()), (a, b) -> a));
        replaceModificatoriPerTipo(livello, TipoModificatore.RANK, desiderati);
    }

    /**
     * Sostituisce i modificatori del tipo dato con la mappa statId -> valore.
     * I modificatori esistenti sulla stessa stat vengono aggiornati, gli altri
     * eliminati; quelli mancanti vengono creati.
     */
    private void replaceModificatoriPerTipo(Item itm, TipoModificatore tipo, Map<String, String> desiderati) {
        // La stat Skill Trick ha una semantica diversa (più modificatori per stat, distinti da
        // "nota": vedi applicaSkillTrickRanghi) e viene sincronizzata separatamente — non deve mai
        // essere toccata da questo metodo, pensato per una stat = un modificatore.
        List<Modificatore> esistenti = itm.getModificatori() != null
                ? itm.getModificatori().stream()
                    .filter(m -> tipo.equals(m.getTipo())
                            && (m.getStat() == null || !Constants.STAT_SKILL_TRICK.equals(m.getStat().getId())))
                    .toList()
                : List.of();

        Map<String, String> rimanenti = new HashMap<>(desiderati);

        boolean isRank = TipoModificatore.RANK.equals(tipo);
        boolean isBase = TipoModificatore.BASE.equals(tipo);

        for (Modificatore m : esistenti) {
            String statId = m.getStat().getId();
            String nuovoValore = rimanenti.remove(statId);
            if (nuovoValore == null) {
                modificatoreRepository.delete(m);
                if (itm.getModificatori() != null) itm.getModificatori().remove(m); // tieni allineata la collection in memoria
            } else {
                boolean changed = false;
                if (!nuovoValore.equals(m.getValore())) {
                    m.setValore(nuovoValore);
                    changed = true;
                }
                if ((isRank || isBase) && !Boolean.TRUE.equals(m.getSempreAttivo())) {
                    m.setSempreAttivo(true);
                    changed = true;
                }
                if (changed) modificatoreRepository.save(m);
            }
        }

        for (Map.Entry<String, String> e : rimanenti.entrySet()) {
            Modificatore m = new Modificatore();
            m.setItem(itm);
            m.setStat(findStat(e.getKey()));
            m.setTipo(tipo);
            m.setValore(e.getValue());
            if (isRank || isBase) m.setSempreAttivo(true);
            modificatoreRepository.save(m);
        }
    }

    /**
     * Allinea i contenuti concessi dal livello:
     * - grants ITEM ("item-&lt;id&gt;") -> collegamenti livello -> item
     * - grants MOD ("mod-&lt;id&gt;")   -> copia del modificatore sorgente sul livello
     * Null = non toccare. I collegamenti verso CLASSE/RAZZA/MALEDIZIONE e i
     * modificatori BASE/RANK non vengono toccati.
     */
    private void applyGrants(Item livello, List<UpdateLivelloRequest.GrantSelezionatoDTO> grants, Integer classeId) {
        if (grants == null) return;

        // Recupera qty definiti nella classe (fonte di verità), (livello, itemTargetId) → qty
        Map<String, Integer> classeQtyMap = new HashMap<>();
        if (classeId != null) {
            for (Avanzamento av : avanzamentoRepository.findAllByItemSource_Id(classeId)) {
                if (av.getQty() != null && av.getItemTarget() != null) {
                    classeQtyMap.put(av.getLivello() + "-" + av.getItemTarget().getId(), av.getQty());
                }
            }
        }

        Map<Integer, Integer> desiredItemIds = new HashMap<>(); // itemId -> qty
        Set<Integer> desiredModIds = new HashSet<>();
        for (UpdateLivelloRequest.GrantSelezionatoDTO g : grants) {
            Integer parsed = parseGrantId(g.getId());
            if (parsed == null) continue;
            if (g.getId().startsWith("item-")) {
                Integer qty = g.getQty();
                // se il frontend non ha mandato qty, recuperalo dalla definizione di classe
                if (qty == null && g.getLivello() != null) {
                    qty = classeQtyMap.get(g.getLivello() + "-" + parsed);
                }
                desiredItemIds.put(parsed, qty);
            } else if (g.getId().startsWith("mod-")) {
                desiredModIds.add(parsed);
            }
        }

        // --- collegamenti (item concessi) ---
        Set<TipoItem> tipiEsclusi = Set.of(TipoItem.CLASSE, TipoItem.RAZZA, TipoItem.MALEDIZIONE);
        List<Collegamento> children = livello.getChild() != null ? new ArrayList<>(livello.getChild()) : new ArrayList<>();

        List<Collegamento> daEliminare = children.stream()
                .filter(c -> !tipiEsclusi.contains(c.getItemTarget().getTipo()))
                .filter(c -> !desiredItemIds.containsKey(c.getItemTarget().getId()))
                .toList();
        collegamentoRepository.deleteAll(daEliminare);
        if (livello.getChild() != null) livello.getChild().removeAll(daEliminare); // allinea la collection in memoria

        Map<Integer, Collegamento> giaPresenti = children.stream()
                .collect(Collectors.toMap(c -> c.getItemTarget().getId(), c -> c, (a, b) -> a));
        for (Map.Entry<Integer, Integer> entry : desiredItemIds.entrySet()) {
            Integer itemId = entry.getKey();
            Integer qty = entry.getValue();
            if (giaPresenti.containsKey(itemId)) {
                Collegamento existing = giaPresenti.get(itemId);
                if (!Objects.equals(existing.getQty(), qty)) {
                    existing.setQty(qty);
                    collegamentoRepository.save(existing);
                }
            } else {
                Item target = itemRepository.findById(itemId)
                        .orElseThrow(() -> new RuntimeException("Item concesso non trovato: " + itemId));
                Collegamento c = new Collegamento();
                c.setItemSource(livello);
                c.setItemTarget(target);
                c.setQty(qty);
                collegamentoRepository.save(c);
            }
        }

        // --- modificatori concessi (copie sul livello) ---
        List<Modificatore> sorgenti = modificatoreRepository.findAllById(desiredModIds);

        // solo le COPIE da grant (id_sorgente valorizzato): i modificatori liberi
        // (id_sorgente null) non vanno toccati qui.
        List<Modificatore> copieEsistenti = livello.getModificatori() != null
                ? livello.getModificatori().stream()
                .filter(m -> m.getIdSorgente() != null)
                .collect(Collectors.toCollection(ArrayList::new))
                : new ArrayList<>();

        List<Modificatore> daCreare = new ArrayList<>();
        for (Modificatore src : sorgenti) {
            int idx = indexOfCopia(copieEsistenti, src);
            if (idx >= 0) {
                copieEsistenti.remove(idx); // già presente: la tolgo dalle candidabili all'eliminazione
            } else {
                daCreare.add(src);
            }
        }

        // le copie rimaste non corrispondono ad alcun grant selezionato
        modificatoreRepository.deleteAll(copieEsistenti);
        if (livello.getModificatori() != null) livello.getModificatori().removeAll(copieEsistenti); // allinea la collection in memoria

        for (Modificatore src : daCreare) {
            Modificatore copia = new Modificatore();
            copia.setItem(livello);
            copia.setStat(src.getStat());
            copia.setTipo(src.getTipo());
            copia.setValore(src.getValore());
            copia.setNota(src.getNota());
            copia.setSempreAttivo(src.getSempreAttivo());
            copia.setIdSorgente(src.getId());
            modificatoreRepository.save(copia);
        }
    }

    /**
     * Applica i modificatori "liberi" di un livello (aggiunti a mano, come su un
     * item qualunque). Gestisce solo i modificatori propri del livello con
     * id_sorgente null e tipo diverso da BASE/RANK: BASE, RANK e le copie da grant
     * non vengono toccati.
     */
    private void applyModificatoriLiberi(Item livello, List<UpdateItemRequest.ModificatoreRowDTO> rows) {
        if (rows == null) return;

        List<Modificatore> liberi = livello.getModificatori() != null
                ? livello.getModificatori().stream()
                .filter(m -> m.getIdSorgente() == null
                        && !TipoModificatore.BASE.equals(m.getTipo())
                        && !TipoModificatore.RANK.equals(m.getTipo()))
                .collect(Collectors.toCollection(ArrayList::new))
                : new ArrayList<>();

        Map<Integer, UpdateItemRequest.ModificatoreRowDTO> byId = rows.stream()
                .filter(r -> r.getId() != null)
                .collect(Collectors.toMap(UpdateItemRequest.ModificatoreRowDTO::getId, r -> r, (a, b) -> a));

        // elimina i liberi non più presenti
        List<Modificatore> daEliminare = liberi.stream()
                .filter(m -> !byId.containsKey(m.getId()))
                .toList();
        modificatoreRepository.deleteAll(daEliminare);
        if (livello.getModificatori() != null) livello.getModificatori().removeAll(daEliminare);

        // aggiorna gli esistenti
        for (Modificatore m : liberi) {
            UpdateItemRequest.ModificatoreRowDTO r = byId.get(m.getId());
            if (r == null) continue;
            m.setStat(findStat(r.getStatId()));
            m.setTipo(r.getTipo() != null ? r.getTipo() : TipoModificatore.MOD);
            if (r.getValore() != null) m.setValore(r.getValore());
            m.setNota(r.getNota());
            m.setSempreAttivo(r.getSempreAttivo());
            m.setPlaceholder(r.getPlaceholder());
            modificatoreRepository.save(m);
        }

        // crea i nuovi (id_sorgente null)
        for (UpdateItemRequest.ModificatoreRowDTO r : rows) {
            if (r.getId() != null) continue;
            Modificatore m = new Modificatore();
            m.setItem(livello);
            m.setStat(findStat(r.getStatId()));
            m.setTipo(r.getTipo() != null ? r.getTipo() : TipoModificatore.MOD);
            m.setValore(r.getValore() != null ? r.getValore() : "0");
            m.setNota(r.getNota());
            m.setSempreAttivo(r.getSempreAttivo());
            m.setPlaceholder(r.getPlaceholder());
            modificatoreRepository.save(m);
        }
    }

    private static int indexOfCopia(List<Modificatore> copie, Modificatore src) {
        for (int i = 0; i < copie.size(); i++) {
            Modificatore c = copie.get(i);
            if (Objects.equals(c.getStat().getId(), src.getStat().getId())
                    && Objects.equals(c.getTipo(), src.getTipo())
                    && Objects.equals(c.getValore(), src.getValore())) {
                return i;
            }
        }
        return -1;
    }

    private static Integer parseGrantId(String grantId) {
        if (grantId == null) return null;
        int dash = grantId.lastIndexOf('-');
        if (dash < 0 || dash == grantId.length() - 1) return null;
        try {
            return Integer.parseInt(grantId.substring(dash + 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Transactional
    public void resetUtilizzi(Integer personaggioId) {
        itemLabelRepository.deleteByLabelAndPersonaggio_Id(Constants.LABEL_UTILIZZI_USATI, personaggioId);
        personaggioCacheService.invalidaPersonaggio(personaggioId);
    }

    public void setUtilizziUsati(Integer itemId, Integer personaggioId, int usati) {
        ItemLabel label = itemLabelRepository
                .findByItem_IdAndLabelAndPersonaggio_Id(itemId, Constants.LABEL_UTILIZZI_USATI, personaggioId)
                .orElseGet(() -> {
                    ItemLabel nl = new ItemLabel();
                    nl.setItem(em.getReference(Item.class, itemId));
                    nl.setPersonaggio(em.getReference(Personaggio.class, personaggioId));
                    nl.setLabel(Constants.LABEL_UTILIZZI_USATI);
                    return nl;
                });
        label.setValore(String.valueOf(Math.max(0, usati)));
        itemLabelRepository.save(label);
        personaggioCacheService.invalidaPersonaggio(personaggioId);
    }

    /**
     * Azzera TUTTI gli slot incantesimo usati (contatore) di un personaggio, su qualunque
     * sezione/livello — stesso schema di resetUtilizzi, ma su un gruppo di label indicizzate
     * (SPELL_&lt;n&gt;_SLOT_USATI_&lt;livello&gt;) invece di una singola label esatta.
     */
    @Transactional
    public void resetSlotUsati(Integer personaggioId) {
        itemLabelRepository.deleteByLabelContainingAndPersonaggio_Id("_SLOT_USATI_", personaggioId);
        personaggioCacheService.invalidaPersonaggio(personaggioId);
    }

    /**
     * Slot usati per un livello di una sezione incantesimi che traccia gli slot con contatore
     * (SPELL_&lt;n&gt;_SLOT_CONTATORE) — stesso schema personaggio-scoped di setUtilizziUsati, ma su
     * una label indicizzata per sezione+livello (SPELL_&lt;n&gt;_SLOT_USATI_&lt;livello&gt;) invece che su
     * UTILIZZI_USATI, perché una fonte (classe/oggetto) può avere più sezioni e più livelli, non
     * un solo contatore. Vedi PersonaggioService.getSlotUsatiPerLivello per la lettura.
     */
    public void setSlotUsatiPerLivello(Integer itemId, Integer personaggioId, int sezioneIndice, int livello, int usati) {
        String labelKey = "SPELL_" + sezioneIndice + "_SLOT_USATI_" + livello;
        ItemLabel label = itemLabelRepository
                .findByItem_IdAndLabelAndPersonaggio_Id(itemId, labelKey, personaggioId)
                .orElseGet(() -> {
                    ItemLabel nl = new ItemLabel();
                    nl.setItem(em.getReference(Item.class, itemId));
                    nl.setPersonaggio(em.getReference(Personaggio.class, personaggioId));
                    nl.setLabel(labelKey);
                    return nl;
                });
        label.setValore(String.valueOf(Math.max(0, usati)));
        itemLabelRepository.save(label);
        personaggioCacheService.invalidaPersonaggio(personaggioId);
    }

    /**
     * Valore di un contatore item ($V_&lt;nome&gt;, es. "$V_CARICHE") per un personaggio — mostrato/
     * editabile in Mobile_DettaglioItem.vue quando l'item ha il flag globale SHOW_$V_&lt;nome&gt;=1
     * (vedi stampaLabelScopedPerPersonaggio per la lettura). Stesso schema personaggio-scoped di
     * setUtilizziUsati/setSlotUsatiPerLivello, ma senza clamp a un massimo: un $V_ è una variabile
     * generica usabile nelle formule (cariche, moltiplicatori, ecc.), non ha un "totale" implicito.
     */
    public void setContatoreItem(Integer itemId, Integer personaggioId, String nome, int valore) {
        String labelKey = "$V_" + nome;
        ItemLabel label = itemLabelRepository
                .findByItem_IdAndLabelAndPersonaggio_Id(itemId, labelKey, personaggioId)
                .orElseGet(() -> {
                    ItemLabel nl = new ItemLabel();
                    nl.setItem(em.getReference(Item.class, itemId));
                    nl.setPersonaggio(em.getReference(Personaggio.class, personaggioId));
                    nl.setLabel(labelKey);
                    return nl;
                });
        label.setValore(String.valueOf(valore));
        itemLabelRepository.save(label);
        personaggioCacheService.invalidaPersonaggio(personaggioId);
    }

}

