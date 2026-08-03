package it.fin8.gdrsheet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.dto.RandomizzatoreDTO;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.entity.ItemLabel;
import it.fin8.gdrsheet.entity.ItemTag;
import it.fin8.gdrsheet.entity.RandomizzatoreRun;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.repository.ItemRepository;
import it.fin8.gdrsheet.repository.ItemTagRepository;
import it.fin8.gdrsheet.repository.RandomizzatoreRunRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Estrazione pesata di un oggetto a partire dai tag, secondo la configurazione di un
 * item di tipo RANDOMIZZATORE.
 * <p>
 * L'estrazione avviene in due fasi:
 * <ol>
 *   <li>una sola query aggregata individua i candidati (oggetti che hanno tutti i tag scelti
 *       e almeno un tag per ognuna delle categorie richieste) col loro peso combinato, e da
 *       questi si estrae il vincitore;</li>
 *   <li>si leggono i tag del solo vincitore e, per ogni categoria richiesta, si estrae un tag
 *       pesato. I tag così estratti sono automaticamente coerenti tra loro perché provengono
 *       tutti dallo stesso oggetto.</li>
 * </ol>
 * Nessuna entity Item viene caricata per i candidati: si materializza solo il vincitore.
 */
@Service
public class RandomizzatoreService {

    private final ItemRepository itemRepository;
    private final ItemTagRepository itemTagRepository;
    private final RandomizzatoreRunRepository runRepository;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    public RandomizzatoreService(ItemRepository itemRepository,
                                 ItemTagRepository itemTagRepository,
                                 RandomizzatoreRunRepository runRepository,
                                 ObjectMapper objectMapper) {
        this.itemRepository = itemRepository;
        this.itemTagRepository = itemTagRepository;
        this.runRepository = runRepository;
        this.objectMapper = objectMapper;
    }

    /* =====================================================================
     * Configurazione
     * ===================================================================== */

    /**
     * Tutte le categorie con i rispettivi tag. Serve sia all'editor dei tag di un oggetto
     * sia all'editor del randomizzatore: entrambi devono proporre le stesse tendine.
     */
    @Transactional
    public List<RandomizzatoreDTO.CategoriaDTO> getCategorie() {
        List<Item> categorie = itemRepository.findItemsByTipo(TipoItem.CATEGORIA);
        if (categorie.isEmpty()) return List.of();
        List<Integer> ids = categorie.stream().map(Item::getId).toList();
        Map<Integer, List<RandomizzatoreDTO.RefDTO>> tagPerCategoria = caricaTagPerCategorie(ids);
        return categorie.stream()
                .sorted(Comparator.comparing(Item::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(c -> new RandomizzatoreDTO.CategoriaDTO(
                        c.getId(), c.getNome(),
                        tagPerCategoria.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    /** Elenco dei randomizzatori: serve a scegliere quali innescare da un oggetto. */
    public List<RandomizzatoreDTO.RefDTO> getElenco() {
        return itemRepository.findItemsByTipo(TipoItem.RANDOMIZZATORE).stream()
                .sorted(Comparator.comparing(Item::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(i -> new RandomizzatoreDTO.RefDTO(i.getId(), i.getNome()))
                .toList();
    }

    // transazionale: legge le labels lazy del randomizzatore. Funzionerebbe anche senza,
    // grazie all'open-in-view attivo di default, ma non è una cosa su cui appoggiarsi.
    @Transactional
    public RandomizzatoreDTO.ConfigDTO getConfig(Integer idRandomizzatore) {
        Item rand = caricaRandomizzatore(idRandomizzatore);

        List<Integer> idScelte = leggiIdDaLabel(rand, Constants.ITEM_LABEL_RAND_SCELTA);
        List<Integer> idPresenti = leggiIdDaLabel(rand, Constants.ITEM_LABEL_RAND_PRESENTE);
        if (idScelte.isEmpty() && idPresenti.isEmpty())
            throw new ResponseStatusException(BAD_REQUEST,
                    "Randomizzatore non configurato: nessuna categoria scelta né richiesta");

        // le categorie "presenti" non richiedono i tag lato UI (li sceglie l'estrazione),
        // ma vengono restituite comunque per mostrarle nella schermata
        Map<Integer, List<RandomizzatoreDTO.RefDTO>> tagPerCategoria = caricaTagPerCategorie(
                unione(idScelte, idPresenti));
        Map<Integer, String> nomiCategorie = caricaNomiCategorie(unione(idScelte, idPresenti));

        String combina = rand.getLabel(Constants.ITEM_LABEL_RAND_COMBINA);
        if (combina == null || combina.isBlank()) combina = Constants.RAND_COMBINA_PRODOTTO;

        return new RandomizzatoreDTO.ConfigDTO(
                rand.getId(),
                rand.getNome(),
                categorieDTO(idScelte, nomiCategorie, tagPerCategoria),
                categorieDTO(idPresenti, nomiCategorie, tagPerCategoria),
                combina.toUpperCase()
        );
    }

    /* =====================================================================
     * Estrazione
     * ===================================================================== */

    @Transactional
    public RandomizzatoreDTO.EsitoDTO estrai(Integer idRandomizzatore,
                                             RandomizzatoreDTO.EstrazioneRequest request,
                                             Utente utente) {
        Item rand = caricaRandomizzatore(idRandomizzatore);
        List<Integer> idScelte = leggiIdDaLabel(rand, Constants.ITEM_LABEL_RAND_SCELTA);

        List<Integer> tagScelti = request.getTagScelti() == null ? List.of() : request.getTagScelti();
        if (tagScelti.size() != idScelte.size())
            throw new ResponseStatusException(BAD_REQUEST,
                    "Vanno scelti " + idScelte.size() + " tag, ricevuti " + tagScelti.size());

        // Contesto delle scelte: categoria -> tag. Viene passato ai livelli annidati, così la
        // difficoltà (o qualunque altra categoria scelta) si propaga lungo tutta la catena
        // invece di essere richiesta di nuovo a ogni passo.
        Map<Integer, Integer> contesto = new LinkedHashMap<>();
        for (int i = 0; i < idScelte.size(); i++) contesto.put(idScelte.get(i), tagScelti.get(i));

        boolean combo = Constants.RAND_MODO_COMBO.equalsIgnoreCase(rand.getLabel(Constants.ITEM_LABEL_RAND_MODO));
        RandomizzatoreDTO.EsitoDTO esito = combo
                ? estraiCombo(rand, contesto, request.getIdMondo(), request.getIdSistema())
                : estraiRicorsivo(rand, contesto, request.getIdMondo(), request.getIdSistema(),
                        0, new int[]{Constants.RAND_NODI_MAX});
        // nei rami annidati un buco è tollerato (resta come avviso nell'albero), ma se è il
        // primo livello a non produrre nulla l'estrazione è fallita a tutti gli effetti — in
        // modalità combo "nulla" vuol dire nessun figlio (il contenitore stesso non ha mai un
        // idItem proprio, è normale che sia null)
        boolean vuoto = combo ? (esito.getFigli() == null || esito.getFigli().isEmpty()) : esito.getIdItem() == null;
        if (vuoto)
            throw new ResponseStatusException(NOT_FOUND,
                    esito.getAvviso() != null ? esito.getAvviso() : "Nessun oggetto corrisponde ai criteri scelti");

        salvaStorico(rand, utente, esito);
        return esito;
    }

    /**
     * Estrae da un randomizzatore e risolve ricorsivamente i randomizzatori innescati
     * dall'oggetto vincitore (label RAND_TRIGGER).
     *
     * @param contesto categoria -> tag già determinati ai livelli superiori: ereditati dai figli
     *                 quando richiedono la stessa categoria, così la catena resta coerente.
     * @param budget   contatore condiviso di nodi ancora estraibili (array per passaggio per riferimento)
     * @return null se questo ramo non ha prodotto nulla al primo livello
     */
    private RandomizzatoreDTO.EsitoDTO estraiRicorsivo(Item rand,
                                                       Map<Integer, Integer> contesto,
                                                       Integer idMondo, Integer idSistema,
                                                       int profondita, int[] budget) {
        List<Integer> idScelte = leggiIdDaLabel(rand, Constants.ITEM_LABEL_RAND_SCELTA);
        List<Integer> idPresenti = leggiIdDaLabel(rand, Constants.ITEM_LABEL_RAND_PRESENTE);
        List<Integer> categorieCoinvolte = unione(idScelte, idPresenti);
        if (categorieCoinvolte.isEmpty())
            return esitoVuoto(rand, "Randomizzatore non configurato");

        // per ogni categoria da scegliere: eredita dal contesto, altrimenti sorteggia d'ufficio
        List<Integer> tagScelti = new ArrayList<>();
        List<RandomizzatoreDTO.TagEstrattoDTO> scelti = new ArrayList<>();
        Map<Integer, List<RandomizzatoreDTO.RefDTO>> tagDisponibili = caricaTagPerCategorie(idScelte);
        Map<Integer, String> nomiCategorie = caricaNomiCategorie(idScelte);
        for (Integer idCat : idScelte) {
            Integer idTag = contesto.get(idCat);
            boolean automatico = false;
            if (idTag == null) {
                List<RandomizzatoreDTO.RefDTO> possibili = tagDisponibili.getOrDefault(idCat, List.of());
                if (possibili.isEmpty())
                    return esitoVuoto(rand, "La categoria '" + nomiCategorie.getOrDefault(idCat, String.valueOf(idCat))
                            + "' non ha tag: impossibile scegliere");
                idTag = possibili.get(random.nextInt(possibili.size())).getId();
                automatico = true;
            }
            final Integer idTagScelto = idTag;
            tagScelti.add(idTagScelto);
            String nomeTag = tagDisponibili.getOrDefault(idCat, List.of()).stream()
                    .filter(t -> Objects.equals(t.getId(), idTagScelto))
                    .map(RandomizzatoreDTO.RefDTO::getNome).findFirst().orElse(null);
            scelti.add(new RandomizzatoreDTO.TagEstrattoDTO(
                    idCat, nomiCategorie.getOrDefault(idCat, null), idTagScelto, nomeTag,
                    automatico ? true : null));
        }

        boolean prodotto = !Constants.RAND_COMBINA_SOMMA.equalsIgnoreCase(
                rand.getLabel(Constants.ITEM_LABEL_RAND_COMBINA));

        // Fase 1: candidati + peso combinato, in una sola query.
        // Le collection vuote non sono ammesse in un IN SQL: si usa un id impossibile come
        // segnaposto, tanto il conteggio atteso è 0 e la condizione resta sempre vera.
        List<Object[]> righe = itemTagRepository.trovaCandidati(
                nonVuota(tagScelti), tagScelti.size(),
                nonVuota(idPresenti), idPresenti.size(),
                categorieCoinvolte, idMondo, idSistema);

        if (righe.isEmpty())
            return esitoVuoto(rand, "Nessun oggetto corrisponde ai criteri");

        Integer idVincitore = (Integer) estraiPesato(righe, r -> pesoDi(r, prodotto))[0];

        // Fase 2: tag del solo vincitore, uno pesato per ogni categoria richiesta
        List<ItemTag> tagVincitore = itemTagRepository.findByItemIdFetch(idVincitore);
        Set<Integer> presentiSet = new HashSet<>(idPresenti);
        List<RandomizzatoreDTO.TagEstrattoDTO> estratti = new ArrayList<>();
        Map<Integer, List<ItemTag>> perCategoria = tagVincitore.stream()
                .filter(t -> presentiSet.contains(t.getCategoria().getId()))
                .collect(Collectors.groupingBy(t -> t.getCategoria().getId()));
        for (Integer idCat : idPresenti) {
            List<ItemTag> candidatiTag = perCategoria.get(idCat);
            if (candidatiTag == null || candidatiTag.isEmpty()) continue;
            ItemTag scelto = estraiPesato(candidatiTag, t -> t.getPeso().doubleValue());
            estratti.add(new RandomizzatoreDTO.TagEstrattoDTO(
                    scelto.getCategoria().getId(), scelto.getCategoria().getNome(),
                    scelto.getTag().getId(), scelto.getTag().getNome()));
        }

        Item vincitore = itemRepository.findItemById(idVincitore);
        RandomizzatoreDTO.EsitoDTO esito = new RandomizzatoreDTO.EsitoDTO(
                vincitore.getId(), vincitore.getNome(), vincitore.getDescrizione(),
                scelti, estratti, righe.size(), LocalDateTime.now());
        esito.setIdRandomizzatore(rand.getId());
        esito.setRandomizzatore(rand.getNome());
        budget[0]--;

        // ---- annidamento: i randomizzatori innescati dall'oggetto estratto ----
        List<Integer> daInnescare = leggiIdDaLabel(vincitore, Constants.ITEM_LABEL_RAND_TRIGGER);
        if (daInnescare.isEmpty()) return esito;

        if (profondita >= Constants.RAND_PROFONDITA_MAX) {
            esito.setAvviso("Catena interrotta: raggiunta la profondità massima ("
                    + Constants.RAND_PROFONDITA_MAX + ")");
            return esito;
        }

        // il contesto scende arricchito anche dei tag estratti qui, così i livelli sotto
        // restano coerenti con quanto già determinato sopra
        Map<Integer, Integer> contestoFiglio = new LinkedHashMap<>(contesto);
        for (RandomizzatoreDTO.TagEstrattoDTO t : scelti) contestoFiglio.put(t.getIdCategoria(), t.getIdTag());
        for (RandomizzatoreDTO.TagEstrattoDTO t : estratti) contestoFiglio.putIfAbsent(t.getIdCategoria(), t.getIdTag());

        List<RandomizzatoreDTO.EsitoDTO> figli = new ArrayList<>();
        for (Integer idFiglio : daInnescare) {
            if (budget[0] <= 0) {
                esito.setAvviso("Catena interrotta: raggiunto il numero massimo di estrazioni ("
                        + Constants.RAND_NODI_MAX + ")");
                break;
            }
            Item figlio = itemRepository.findItemById(idFiglio);
            if (figlio == null || !TipoItem.RANDOMIZZATORE.equals(figlio.getTipo())) {
                figli.add(esitoVuoto(null, "Randomizzatore collegato non valido (id " + idFiglio + ")"));
                continue;
            }
            RandomizzatoreDTO.EsitoDTO e = estraiRicorsivo(
                    figlio, contestoFiglio, idMondo, idSistema, profondita + 1, budget);
            if (e != null) figli.add(e);
        }
        if (!figli.isEmpty()) esito.setFigli(figli);
        return esito;
    }

    /**
     * Estrazione COMBO: ogni categoria in RAND_SCELTA viene risolta in modo indipendente (un
     * vincitore pesato tra gli item taggati in QUELLA SOLA categoria — nessun item deve
     * appartenere a più categorie insieme, a differenza della modalità SINGOLO). Se l'utente ha
     * fissato un tag per una categoria (vedi {@code contesto}), i candidati di quella categoria
     * si restringono a chi ha esattamente quel tag; altrimenti partecipano tutti gli item della
     * categoria, pesati.
     * <p>
     * Il risultato è un "contenitore" (idItem null: non esiste un oggetto unico che riassuma
     * tutte le categorie) con un figlio per categoria, ciascuno etichettato col nome della
     * categoria stessa invece che con quello del randomizzatore.
     */
    private RandomizzatoreDTO.EsitoDTO estraiCombo(Item rand, Map<Integer, Integer> contesto,
                                                    Integer idMondo, Integer idSistema) {
        List<Integer> idScelte = leggiIdDaLabel(rand, Constants.ITEM_LABEL_RAND_SCELTA);
        if (idScelte.isEmpty())
            return esitoVuoto(rand, "Randomizzatore combo non configurato: nessuna categoria in RAND_SCELTA");

        boolean prodotto = !Constants.RAND_COMBINA_SOMMA.equalsIgnoreCase(
                rand.getLabel(Constants.ITEM_LABEL_RAND_COMBINA));
        Map<Integer, String> nomiCategorie = caricaNomiCategorie(idScelte);

        List<RandomizzatoreDTO.EsitoDTO> figli = new ArrayList<>();
        for (Integer idCat : idScelte) {
            Integer tagFissato = contesto.get(idCat);
            List<Integer> tagScelti = tagFissato == null ? List.of() : List.of(tagFissato);

            List<Object[]> righe = itemTagRepository.trovaCandidati(
                    nonVuota(tagScelti), tagScelti.size(),
                    nonVuota(List.of()), 0,
                    List.of(idCat), idMondo, idSistema);

            String nomeCategoria = nomiCategorie.getOrDefault(idCat, "Categoria " + idCat);
            if (righe.isEmpty()) {
                RandomizzatoreDTO.EsitoDTO vuoto = esitoVuoto(rand,
                        "Nessun oggetto per la categoria '" + nomeCategoria + "'");
                vuoto.setRandomizzatore(nomeCategoria);
                figli.add(vuoto);
                continue;
            }

            Integer idVincitore = (Integer) estraiPesato(righe, r -> pesoDi(r, prodotto))[0];
            Item vincitore = itemRepository.findItemById(idVincitore);
            RandomizzatoreDTO.EsitoDTO figlio = new RandomizzatoreDTO.EsitoDTO(
                    vincitore.getId(), vincitore.getNome(), vincitore.getDescrizione(),
                    List.of(), List.of(), righe.size(), LocalDateTime.now());
            figlio.setIdRandomizzatore(rand.getId());
            figlio.setRandomizzatore(nomeCategoria);
            figli.add(figlio);
        }

        RandomizzatoreDTO.EsitoDTO esito = new RandomizzatoreDTO.EsitoDTO(
                null, rand.getNome(), null, List.of(), List.of(), 0, LocalDateTime.now());
        esito.setIdRandomizzatore(rand.getId());
        esito.setRandomizzatore(rand.getNome());
        esito.setFigli(figli);
        return esito;
    }

    /** Nodo "non risolto": tiene traccia del ramo mancato senza far fallire l'intera estrazione. */
    private RandomizzatoreDTO.EsitoDTO esitoVuoto(Item rand, String avviso) {
        RandomizzatoreDTO.EsitoDTO e = new RandomizzatoreDTO.EsitoDTO();
        e.setEseguitoIl(LocalDateTime.now());
        if (rand != null) {
            e.setIdRandomizzatore(rand.getId());
            e.setRandomizzatore(rand.getNome());
        }
        e.setAvviso(avviso);
        return e;
    }

    private void salvaStorico(Item rand, Utente utente, RandomizzatoreDTO.EsitoDTO esito) {
        RandomizzatoreRun run = new RandomizzatoreRun();
        run.setItem(rand);
        run.setUtente(utente);
        run.setEseguitoIl(esito.getEseguitoIl());
        try {
            run.setRisultato(objectMapper.writeValueAsString(esito));
        } catch (Exception e) {
            run.setRisultato(null);
        }
        runRepository.save(run);
        // potatura alla scrittura: niente job schedulato, la tabella non cresce mai
        runRepository.potaStorico(rand.getId(), Constants.RAND_STORICO_MAX);
    }

    /* =====================================================================
     * Storico
     * ===================================================================== */

    // transazionale: risolve l'utente lazy di ogni run (vedi nota su getConfig)
    @Transactional
    public List<RandomizzatoreDTO.StoricoDTO> getStorico(Integer idRandomizzatore, int limite) {
        return runRepository
                .findByItem_IdOrderByEseguitoIlDesc(idRandomizzatore,
                        PageRequest.of(0, Math.max(1, Math.min(limite, Constants.RAND_STORICO_MAX))))
                .stream()
                .map(r -> {
                    RandomizzatoreDTO.EsitoDTO esito = null;
                    try {
                        if (r.getRisultato() != null)
                            esito = objectMapper.readValue(r.getRisultato(), RandomizzatoreDTO.EsitoDTO.class);
                    } catch (Exception ignored) {
                    }
                    return new RandomizzatoreDTO.StoricoDTO(
                            r.getId(), r.getEseguitoIl(),
                            r.getUtente() != null ? r.getUtente().getUsername() : null,
                            esito);
                })
                .toList();
    }

    /* =====================================================================
     * Helper
     * ===================================================================== */

    private Item caricaRandomizzatore(Integer id) {
        Item rand = itemRepository.findItemById(id);
        if (rand == null)
            throw new ResponseStatusException(NOT_FOUND, "Randomizzatore non trovato");
        if (!TipoItem.RANDOMIZZATORE.equals(rand.getTipo()))
            throw new ResponseStatusException(BAD_REQUEST, "L'item non è un randomizzatore");
        return rand;
    }

    /** Legge una label multi-valore che contiene id di categoria, scartando i valori non numerici. */
    private List<Integer> leggiIdDaLabel(Item item, String label) {
        if (item.getLabels() == null) return List.of();
        List<Integer> out = new ArrayList<>();
        for (ItemLabel l : item.getLabels()) {
            if (!label.equals(l.getLabel()) || l.getValore() == null) continue;
            try {
                Integer id = Integer.valueOf(l.getValore().trim());
                if (!out.contains(id)) out.add(id);
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    private Map<Integer, List<RandomizzatoreDTO.RefDTO>> caricaTagPerCategorie(List<Integer> idCategorie) {
        if (idCategorie.isEmpty()) return Map.of();
        List<String> comeStringhe = idCategorie.stream().map(String::valueOf).toList();
        Map<Integer, List<RandomizzatoreDTO.RefDTO>> out = new LinkedHashMap<>();
        for (Object[] riga : itemRepository.findTagPerCategorie(
                Constants.ITEM_LABEL_TAG_CATEGORIA, comeStringhe)) {
            Item tag = (Item) riga[0];
            Integer idCat;
            try {
                idCat = Integer.valueOf(String.valueOf(riga[1]).trim());
            } catch (NumberFormatException e) {
                continue;
            }
            out.computeIfAbsent(idCat, k -> new ArrayList<>())
                    .add(new RandomizzatoreDTO.RefDTO(tag.getId(), tag.getNome()));
        }
        return out;
    }

    private Map<Integer, String> caricaNomiCategorie(List<Integer> idCategorie) {
        if (idCategorie.isEmpty()) return Map.of();
        return itemRepository.findItemsByIds(idCategorie).stream()
                .collect(Collectors.toMap(Item::getId, Item::getNome, (a, b) -> a));
    }

    private List<RandomizzatoreDTO.CategoriaDTO> categorieDTO(
            List<Integer> ids,
            Map<Integer, String> nomi,
            Map<Integer, List<RandomizzatoreDTO.RefDTO>> tagPerCategoria) {
        return ids.stream()
                .map(id -> new RandomizzatoreDTO.CategoriaDTO(
                        id,
                        nomi.getOrDefault(id, "Categoria " + id),
                        tagPerCategoria.getOrDefault(id, List.of())))
                .toList();
    }

    private static List<Integer> unione(List<Integer> a, List<Integer> b) {
        List<Integer> out = new ArrayList<>(a);
        for (Integer x : b) if (!out.contains(x)) out.add(x);
        return out;
    }

    /** Un IN SQL non ammette liste vuote: si usa un id impossibile come segnaposto. */
    private static Collection<Integer> nonVuota(Collection<Integer> ids) {
        return ids.isEmpty() ? List.of(-1) : ids;
    }

    /**
     * Peso combinato di una riga candidato: colonna 2 = somma, colonna 3 = prodotto.
     * Entrambe sono null quando non ci sono categorie scelte (nessun tag da combinare):
     * in quel caso tutti i candidati pesano uguale.
     */
    private static double pesoDi(Object[] riga, boolean prodotto) {
        Object v = prodotto ? riga[2] : riga[1];
        if (v == null) return 1d;
        double d = ((Number) v).doubleValue();
        return (Double.isFinite(d) && d > 0) ? d : 0d;
    }

    /** Estrazione pesata; con pesi tutti nulli degrada a uniforme. */
    private <T> T estraiPesato(List<T> elementi, ToDoubleFunction<T> peso) {
        double totale = 0;
        for (T e : elementi) totale += Math.max(0, peso.applyAsDouble(e));
        if (totale <= 0) return elementi.get(random.nextInt(elementi.size()));

        double soglia = random.nextDouble() * totale;
        double acc = 0;
        for (T e : elementi) {
            acc += Math.max(0, peso.applyAsDouble(e));
            if (soglia < acc) return e;
        }
        return elementi.get(elementi.size() - 1);
    }
}
