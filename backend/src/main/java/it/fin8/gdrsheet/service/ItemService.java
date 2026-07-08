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
import java.util.function.Consumer;
import java.util.function.Supplier;
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
    private ItemLabelRepository itemLabelRepository;
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

        if (itm.getPersonaggio() == null) {
            Collegamento link = utilService.findRightConnectionLink(itm, personaggioId)
                    .orElseThrow(() -> new RuntimeException("Impossibile definire collegamento oggetto"));

            toggleDisabled(() -> link.getLabel(Constants.ITEM_LABEL_DISABILITATO), val -> link.setLabel(Constants.ITEM_LABEL_DISABILITATO, val));
            collegamentoRepository.save(link);

        } else {
            toggleDisabled(() -> itm.getLabel(Constants.ITEM_LABEL_DISABILITATO), val -> itm.setLabel(Constants.ITEM_LABEL_DISABILITATO, val));
            itemRepository.save(itm);
        }

        personaggioCacheService.invalidaPersonaggio(personaggioId);
        return itm;
    }

    /**
     * Stato disabilitato di un item nel contesto di un personaggio: per gli item del
     * compendio guarda il collegamento (FromCompendio), per quelli intestati l'item stesso.
     */
    public boolean isItemDisabled(Integer itemId, Integer personaggioId) {
        Item itm = itemRepository.findItemById(itemId);
        if (itm == null) return false;
        String stato;
        if (itm.getPersonaggio() == null && personaggioId != null) {
            stato = utilService.findRightConnectionLink(itm, personaggioId)
                    .map(l -> l.getLabel(Constants.ITEM_LABEL_DISABILITATO))
                    .orElse(null);
        } else {
            stato = itm.getLabel(Constants.ITEM_LABEL_DISABILITATO);
        }
        return Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE.equals(stato);
    }

    private static void toggleDisabled(Supplier<String> getter, Consumer<String> setter) {
        String v = getter.get();
        v = (v == null) ? "" : v.trim();
        setter.accept(v.isEmpty() || v.equals(Constants.ITEM_LABEL_DISABILITATO_VALORE_FALSE) ? Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE : Constants.ITEM_LABEL_DISABILITATO_VALORE_FALSE);
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
            if (TipoItem.LIVELLO.equals(request.getTipo())) {
                itm.setPersonaggio(pg);
            }
            if (pg.getParty() != null && pg.getParty().getMondo() != null) {
                Mondo mondo = pg.getParty().getMondo();
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

        if (request.getLabels() != null) {
            for (UpdateItemRequest.LabelRowDTO l : request.getLabels()) {
                addLabelRow(itm, l.getLabel(), l.getValore());
            }
        }

        Item saved = itemRepository.save(itm);
        applyModificatori(saved, request.getModificatori());
        applyAttacchi(saved, request.getAttacchi());
        applyChildren(saved, request.getChildren());

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
            // un item appena aggiunto nasce disabilitato: va abilitato esplicitamente
            link.setLabels(new ArrayList<>());
            link.setLabel(Constants.ITEM_LABEL_DISABILITATO, Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE);
            collegamentoRepository.save(link);
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
            Item fromCompendio = itemRepository.findItemByNomeAndPersonaggio_Id(Constants.ITEM_FROM_COMPENDIO, idPersonaggio);
            if (fromCompendio != null) {
                List<Collegamento> links = collegamentoRepository.findAllByItemTarget_Id(id).stream()
                        .filter(c -> Objects.equals(c.getItemSource().getId(), fromCompendio.getId()))
                        .toList();
                collegamentoRepository.deleteAll(links);
            }
            // ancora referenziato da altri item (altri personaggi, armi, classi...): non toccare
            if (!collegamentoRepository.findAllByItemTarget_Id(id).isEmpty()) {
                return;
            }
        }

        hardDelete(itm);
    }

    /**
     * Scollega un item dall'equipaggiamento del personaggio (rimuove il
     * collegamento dal suo FromCompendio) senza toccare l'item, che resta
     * nel compendio. Pensato per gli oggetti "persi".
     */
    @Transactional
    public void unlinkItem(Integer itemId, Integer idPersonaggio) {
        Item fromCompendio = itemRepository.findItemByNomeAndPersonaggio_Id(Constants.ITEM_FROM_COMPENDIO, idPersonaggio);
        if (fromCompendio == null) throw new RuntimeException("FromCompendio non trovato per il personaggio " + idPersonaggio);

        List<Collegamento> links = collegamentoRepository.findAllByItemTarget_Id(itemId).stream()
                .filter(c -> Objects.equals(c.getItemSource().getId(), fromCompendio.getId()))
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
        itemRepository.delete(itm); // le labels seguono in cascata
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
        link.setLabel(Constants.ITEM_LABEL_DISABILITATO, Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE);
        collegamentoRepository.save(link);
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

        Item saved = itemRepository.save(itm);
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

    public List<NotiziaDTO> getNotizieAttive() {
        LocalDateTime ora = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        return itemRepository.findAllNotizie().stream()
                .filter(n -> "1".equals(n.getLabel(Constants.LABEL_NOTIZIA_ABILITATA)))
                .filter(n -> {
                    String inizio = n.getLabel(Constants.LABEL_NOTIZIA_DATA_INIZIO);
                    String fine = n.getLabel(Constants.LABEL_NOTIZIA_DATA_FINE);
                    try {
                        if (inizio != null && !inizio.isBlank() && ora.isBefore(LocalDateTime.parse(inizio, fmt))) return false;
                        if (fine != null && !fine.isBlank() && ora.isAfter(LocalDateTime.parse(fine, fmt))) return false;
                    } catch (Exception ignored) {}
                    return true;
                })
                .map(n -> new NotiziaDTO(
                        n.getId(),
                        n.getNome(),
                        n.getDescrizione(),
                        n.getLabel(Constants.LABEL_NOTIZIA_DATA_INIZIO),
                        n.getLabel(Constants.LABEL_NOTIZIA_DATA_FINE)))
                .toList();
    }

    public List<Item> searchItems(String query, TipoItem tipo) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return List.of();
        return tipo == null
                ? itemRepository.findTop20ByNomeContainingIgnoreCaseOrderByNomeAsc(q)
                : itemRepository.findTop20ByNomeContainingIgnoreCaseAndTipoOrderByNomeAsc(q, tipo);
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

            if (r.getId() != null) {
                // aggiorna attacco esistente
                Item attacco = esistentiById.get(r.getId());
                if (attacco == null) continue; // id non figlio di questo item: ignora
                attacco.setNome(r.getNome().trim());
                putSingleLabel(attacco, Constants.ITEM_LABEL_ATTACCO_TIRO_PER_COLPIRE, r.getTpc());
                putSingleLabel(attacco, Constants.ITEM_LABEL_ATTACCO_DANNI, r.getTpd());
                putSingleLabel(attacco, Constants.ITEM_LABEL_ATTACCO_TIPO_DANNI, r.getTipoDanni());
                itemRepository.save(attacco);
            } else {
                // crea nuovo attacco + collegamento
                Item attacco = new Item();
                attacco.setNome(r.getNome().trim());
                attacco.setTipo(TipoItem.ATTACCO);
                attacco.setLabels(new ArrayList<>());
                addLabelRow(attacco, Constants.ITEM_LABEL_ATTACCO_TIRO_PER_COLPIRE, r.getTpc());
                addLabelRow(attacco, Constants.ITEM_LABEL_ATTACCO_DANNI, r.getTpd());
                addLabelRow(attacco, Constants.ITEM_LABEL_ATTACCO_TIPO_DANNI, r.getTipoDanni());
                Item savedAttacco = itemRepository.save(attacco);

                Collegamento link = new Collegamento();
                link.setItemSource(itm);
                link.setItemTarget(savedAttacco);
                collegamentoRepository.save(link);
            }
        }
    }

    /**
     * Allinea gli item collegati come child (esclusi gli ATTACCO) allo stato
     * richiesto: crea i collegamenti mancanti, elimina quelli non più presenti
     * (solo il collegamento, mai l'item target). Null = non toccare.
     */
    private void applyChildren(Item itm, List<UpdateItemRequest.ChildRefDTO> children) {
        if (children == null) return;

        record ChildInfo(Integer qty, String formulaQty, String scelta, boolean nascosto) {
        }
        Map<Integer, ChildInfo> desiderati = new HashMap<>();
        for (UpdateItemRequest.ChildRefDTO c : children)
            desiderati.put(c.getId(), new ChildInfo(c.getQty(), c.getFormulaQty(), c.getScelta(), Boolean.TRUE.equals(c.getNascosto())));

        List<Collegamento> linkAltri = (itm.getChild() != null ? itm.getChild() : List.<Collegamento>of()).stream()
                .filter(c -> !TipoItem.ATTACCO.equals(c.getItemTarget().getTipo()))
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
                boolean changed = !Objects.equals(existing.getQty(), info.qty())
                        || !Objects.equals(existing.getFormulaQty(), info.formulaQty())
                        || !Objects.equals(existing.getScelta(), info.scelta())
                        || existingNascosto != info.nascosto();
                if (changed) {
                    existing.setQty(info.qty());
                    existing.setFormulaQty(info.formulaQty());
                    existing.setScelta(info.scelta());
                    existing.setLabel(Constants.ITEM_LABEL_NASCOSTO, info.nascosto() ? Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE : null);
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
                collegamentoRepository.save(link);
            }
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
            itemRepository.save(livello);
        }
        personaggioCacheService.invalidaPersonaggio(request.getPersonaggioId());
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
        List<Modificatore> esistenti = itm.getModificatori() != null
                ? itm.getModificatori().stream().filter(m -> tipo.equals(m.getTipo())).toList()
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

}

