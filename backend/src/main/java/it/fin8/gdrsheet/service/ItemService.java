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
    private ModificatoreRepository modificatoreRepository;

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

        if (request.getLabels() != null) {
            for (UpdateItemRequest.LabelRowDTO l : request.getLabels()) {
                addLabelRow(itm, l.getLabel(), l.getValore());
            }
        }

        Item saved = itemRepository.save(itm);
        applyModificatori(saved, request.getModificatori());
        applyAttacchi(saved, request.getAttacchi());
        applyChildren(saved, request.getChildItemIds());
        return saved;
    }

    @Transactional
    public Item updateItem(Integer id, UpdateItemRequest request) {
        Item itm = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Item non trovato"));

        if (request.getNome() != null && !request.getNome().trim().isEmpty()) itm.setNome(request.getNome().trim());
        if (request.getDescrizione() != null) itm.setDescrizione(request.getDescrizione());

        // labels: stato completo -> svuota in place e ricrea (orphanRemoval)
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
                addLabelRow(itm, l.getLabel(), l.getValore());
            }
        }

        applyModificatori(itm, request.getModificatori());
        applyAttacchi(itm, request.getAttacchi());
        applyChildren(itm, request.getChildItemIds());

        return itemRepository.save(itm);
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
    private void applyChildren(Item itm, List<Integer> childItemIds) {
        if (childItemIds == null) return;

        Set<Integer> desiderati = new HashSet<>(childItemIds);

        List<Collegamento> linkAltri = (itm.getChild() != null ? itm.getChild() : List.<Collegamento>of()).stream()
                .filter(c -> !TipoItem.ATTACCO.equals(c.getItemTarget().getTipo()))
                .toList();

        List<Collegamento> daEliminare = linkAltri.stream()
                .filter(c -> !desiderati.contains(c.getItemTarget().getId()))
                .toList();
        collegamentoRepository.deleteAll(daEliminare);

        Set<Integer> giaPresenti = linkAltri.stream()
                .map(c -> c.getItemTarget().getId())
                .collect(Collectors.toSet());

        for (Integer targetId : desiderati) {
            if (giaPresenti.contains(targetId)) continue;
            if (Objects.equals(targetId, itm.getId())) continue; // no self-link
            Item target = itemRepository.findById(targetId)
                    .orElseThrow(() -> new RuntimeException("Item da collegare non trovato: " + targetId));
            Collegamento link = new Collegamento();
            link.setItemSource(itm);
            link.setItemTarget(target);
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
        applyGrants(livello, request.getGrantsSelezionati());

        return itemRepository.save(livello);
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

        for (Modificatore m : esistenti) {
            String statId = m.getStat().getId();
            String nuovoValore = rimanenti.remove(statId);
            if (nuovoValore == null) {
                modificatoreRepository.delete(m);
            } else if (!nuovoValore.equals(m.getValore())) {
                m.setValore(nuovoValore);
                modificatoreRepository.save(m);
            }
        }

        for (Map.Entry<String, String> e : rimanenti.entrySet()) {
            Modificatore m = new Modificatore();
            m.setItem(itm);
            m.setStat(findStat(e.getKey()));
            m.setTipo(tipo);
            m.setValore(e.getValue());
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
    private void applyGrants(Item livello, List<UpdateLivelloRequest.GrantSelezionatoDTO> grants) {
        if (grants == null) return;

        Set<Integer> desiredItemIds = new HashSet<>();
        Set<Integer> desiredModIds = new HashSet<>();
        for (UpdateLivelloRequest.GrantSelezionatoDTO g : grants) {
            Integer parsed = parseGrantId(g.getId());
            if (parsed == null) continue;
            if (g.getId().startsWith("item-")) desiredItemIds.add(parsed);
            else if (g.getId().startsWith("mod-")) desiredModIds.add(parsed);
        }

        // --- collegamenti (item concessi) ---
        Set<TipoItem> tipiEsclusi = Set.of(TipoItem.CLASSE, TipoItem.RAZZA, TipoItem.MALEDIZIONE);
        List<Collegamento> children = livello.getChild() != null ? new ArrayList<>(livello.getChild()) : new ArrayList<>();

        List<Collegamento> daEliminare = children.stream()
                .filter(c -> !tipiEsclusi.contains(c.getItemTarget().getTipo()))
                .filter(c -> !desiredItemIds.contains(c.getItemTarget().getId()))
                .toList();
        collegamentoRepository.deleteAll(daEliminare);

        Set<Integer> giaPresenti = children.stream()
                .map(c -> c.getItemTarget().getId())
                .collect(Collectors.toSet());
        for (Integer itemId : desiredItemIds) {
            if (giaPresenti.contains(itemId)) continue;
            Item target = itemRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Item concesso non trovato: " + itemId));
            Collegamento c = new Collegamento();
            c.setItemSource(livello);
            c.setItemTarget(target);
            collegamentoRepository.save(c);
        }

        // --- modificatori concessi (copie sul livello) ---
        List<Modificatore> sorgenti = modificatoreRepository.findAllById(desiredModIds);

        List<Modificatore> copieEsistenti = livello.getModificatori() != null
                ? livello.getModificatori().stream()
                .filter(m -> !TipoModificatore.BASE.equals(m.getTipo()) && !TipoModificatore.RANK.equals(m.getTipo()))
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

        for (Modificatore src : daCreare) {
            Modificatore copia = new Modificatore();
            copia.setItem(livello);
            copia.setStat(src.getStat());
            copia.setTipo(src.getTipo());
            copia.setValore(src.getValore());
            copia.setNota(src.getNota());
            copia.setSempreAttivo(src.getSempreAttivo());
            modificatoreRepository.save(copia);
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

}

