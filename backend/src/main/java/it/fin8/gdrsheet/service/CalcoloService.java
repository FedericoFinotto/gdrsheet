package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.dto.CaratteristicaDTO;
import it.fin8.gdrsheet.dto.DatiPersonaggioDTO;
import it.fin8.gdrsheet.dto.InfoClasseDTO;
import it.fin8.gdrsheet.dto.InfoLivelliDTO;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.mapper.StatMapper;
import it.fin8.gdrsheet.repository.ItemRepository;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class CalcoloService {

    @Autowired
    private StatMapper statMapper;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UtilService utilService;


    private static final Pattern PATTERN_PLACEH = Pattern.compile("@\\w+");
    private static final Pattern PATTERN_DICE = Pattern.compile("\\d+d\\d+(?:\\d+)?");
    private static final Pattern PATTERN_NUMBER = Pattern.compile("^[+-]?\\d+$");

    // Funzioni di arrotondamento utilizzabili nelle formule
    private static final Function FN_ECCESSO = new Function("ECCESSO", 1) {
        @Override public double apply(double... args) { return Math.ceil(args[0]); }
    };
    private static final Function FN_DIFETTO = new Function("DIFETTO", 1) {
        @Override public double apply(double... args) { return Math.floor(args[0]); }
    };
    private static final Function FN_TRONCATO = new Function("TRONCATO", 1) {
        @Override public double apply(double... args) { return (double) (long) args[0]; }
    };
    private static final Function[] FUNZIONI = {FN_ECCESSO, FN_DIFETTO, FN_TRONCATO};

    /**
     * Metodo base: sostituisce nella formula le chiavi della mappa (es. "@CAR", "$1983_QTA")
     * con i rispettivi valori, poi valuta l'espressione numerica risultante.
     * Le chiavi più lunghe vengono sostituite prima per evitare match parziali
     * (es. "@PESO_TOTALE" prima di "@PESO").
     */
    // Prefisso "cd"/"CD" scritto per errore dentro al campo formula (es. "cd 10+@LVL"): capita
    // spesso perché il campo si chiama già "Formula CD", non è una variabile riconosciuta e
    // rompe la valutazione facendola tornare a 0 in silenzio. "CD" non è più una stat valida
    // (la vecchia "CD Incantesimi" è stata rimossa), quindi si può togliere in sicurezza.
    private static final Pattern PATTERN_PREFISSO_CD = Pattern.compile("^\\s*cd\\s+", Pattern.CASE_INSENSITIVE);

    public String calcola(String formula, Map<String, String> valori) {
        if (formula == null || formula.isBlank()) return "0";

        // 1. Sostituisci le variabili (chiavi più lunghe prima)
        String replaced = PATTERN_PREFISSO_CD.matcher(formula).replaceFirst("");
        List<Map.Entry<String, String>> entries = new ArrayList<>(valori.entrySet());
        entries.sort((a, b) -> b.getKey().length() - a.getKey().length());
        for (Map.Entry<String, String> e : entries) {
            if (e.getValue() != null) {
                replaced = replaced.replace(e.getKey(), e.getValue());
            }
        }

        // 2. Estrai e salva la parte col dado (es: 1d8)
        Matcher diceMatcher = PATTERN_DICE.matcher(replaced);
        String dicePart = "";
        if (diceMatcher.find()) {
            dicePart = diceMatcher.group();
            replaced = replaced.replace(dicePart, "");
        }

        // 3. Pulizia formula numerica (rimuove spazi)
        String numericExpr = replaced.replaceAll("\\s+", "");

        long result = 0;
        if (!numericExpr.isBlank()) {
            try {
                double eval = new ExpressionBuilder(numericExpr)
                        .functions(FUNZIONI)
                        .build().evaluate();
                result = (long) Math.floor(eval);
            } catch (Exception e) {
            }
        }

        // 4. Ritorna la formula finale
        if (!dicePart.isBlank()) {
            return result != 0 ? dicePart + (result > 0 ? "+" : "") + result : dicePart;
        } else {
            return String.valueOf(result);
        }
    }

    /**
     * Wrapper: converte la lista di CaratteristicaDTO in mappa chiave→valore
     * (chiave = "@" + id, valore = modificatore come stringa) e chiama il metodo base.
     */
    public String calcola(String formula, List<CaratteristicaDTO> caratteristiche) {
        if (formula == null || formula.isBlank()) return "0";
        Map<String, String> valori = new LinkedHashMap<>();
        for (CaratteristicaDTO c : caratteristiche) {
            if (c.getId() != null) {
                valori.put("@" + c.getId(), String.valueOf(c.getModificatore() != null ? c.getModificatore() : 0));
            }
        }
        return calcola(formula, valori);
    }

    /**
     * Overload per DTO personaggio
     */
    public String calcola(String formula, DatiPersonaggioDTO dati) {
        List<CaratteristicaDTO> caratteristiche = new ArrayList<>(dati.getCaratteristiche());
        if (dati.getBonusAttacco() != null) {
            caratteristiche.addAll(dati.getBonusAttacco().stream()
                    .map(statMapper::toCaratteristicaDTO)
                    .toList());
        }
        if (dati.getAttributi() != null) {
            caratteristiche.addAll(dati.getAttributi().stream()
                    .map(statMapper::toCaratteristicaDTO)
                    .toList());
        }

        // Variabili anagrafiche/peso disponibili nelle formule: PESO, ETA, ALTEZZA, PESO_TOTALE
        variabiliPersonaggio(dati.getInfo(), dati.getPesoTotale())
                .forEach((k, v) -> caratteristiche.add(new CaratteristicaDTO(k, k, null, v, null, null)));

        List<Item> initialRoots = itemRepository.findAllByPersonaggioIdWithChild(dati.getId());
        caratteristiche.add(new CaratteristicaDTO("LVL", "Livello", null, Integer.parseInt(String.valueOf(getLivelli(initialRoots).getLivello())), null, null));
        // DV (Dadi Vita totali): esclusa di proposito dalla lista "attributi" generica (gestita a
        // parte in dadiVita), va aggiunta esplicitamente qui per essere usabile nelle formule.
        if (dati.getDadiVita() != null && dati.getDadiVita().getTotale() != null) {
            caratteristiche.add(new CaratteristicaDTO("DV", "Dadi Vita", null, dati.getDadiVita().getTotale(), null, null));
        }

        return calcola(formula, caratteristiche);
    }

    /**
     * Variabili "anagrafiche" del personaggio usabili nelle formule (@PESO,
     * @ETA, @ALTEZZA, @PESO_TOTALE). Valori arrotondati all'intero. Le voci
     * mancanti/non numeriche vengono omesse.
     */
    public static Map<String, Integer> variabiliPersonaggio(Map<String, String> info, Double pesoTotale) {
        Map<String, Integer> v = new LinkedHashMap<>();
        if (pesoTotale != null) v.put("PESO_TOTALE", (int) Math.round(pesoTotale));
        if (info != null) {
            putIfNum(v, "PESO", info.get("PESO"));
            putIfNum(v, "ALTEZZA", info.get("ALTEZZA"));
            putIfNum(v, "ETA", info.get("ETA"));
        }
        return v;
    }

    private static void putIfNum(Map<String, Integer> map, String key, String raw) {
        if (raw == null || raw.isBlank()) return;
        try {
            map.put(key, (int) Math.round(Double.parseDouble(raw.trim().replace(',', '.'))));
        } catch (NumberFormatException ignored) {
        }
    }

    public InfoLivelliDTO getLivelli(List<Item> initialRoots) {
        InfoLivelliDTO out = new InfoLivelliDTO();
        if (initialRoots == null || initialRoots.isEmpty()) {
            out.setLivello(0);
            out.setClassi(Collections.emptyList());
            return out;
        }

        // Filtri inline (senza Predicate)
        List<Item> livelliTotali = initialRoots.stream()
                .filter(Objects::nonNull)
                .filter(i -> TipoItem.LIVELLO.equals(i.getTipo()))
                .filter(x -> !utilService.parseBooleanFromString(
                        x.getLabel(Constants.ITEM_LABEL_DISABILITATO),
                        Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE,
                        Constants.ITEM_LABEL_DISABILITATO_VALORE_FALSE))
                .toList();

        List<Item> livelliAttivi = initialRoots.stream()
                .filter(Objects::nonNull)
                .filter(i -> TipoItem.LIVELLO.equals(i.getTipo()))
                .filter(x -> !utilService.parseBooleanFromString(
                        x.getLabel(Constants.ITEM_LABEL_DISABILITATO),
                        Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE,
                        Constants.ITEM_LABEL_DISABILITATO_VALORE_FALSE))
                .filter(x -> x.getLabel(Constants.ITEM_LABEL_MALEDIZIONE) == null)
                .toList();

        out.setLivello(Math.max(0, livelliAttivi.size() - 1));

        if (livelliTotali.isEmpty()) {
            out.setClassi(Collections.emptyList());
            return out;
        }

        // Map<Item, Set<Item>> diretta (1 stream)
        Map<Item, Set<Item>> perClasse = livelliTotali.stream()
                .filter(livello -> livello.getLabel(Constants.ITEM_LABEL_CLASSE) != null)
                .collect(Collectors.groupingBy(
                        livello -> {
                            try {
                                return itemRepository.findItemById(
                                        Integer.parseInt(livello.getLabel(Constants.ITEM_LABEL_CLASSE)));
                            } catch (NumberFormatException e) {
                                return null;
                            }
                        },
                        LinkedHashMap::new,
                        Collectors.toSet()  // HashSet sicuro
                ))
                .entrySet().stream()
                .filter(e -> e.getKey() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        // DTOs
        List<InfoClasseDTO> classi = perClasse.entrySet().stream()
                .map(e -> {
                    InfoClasseDTO dto = new InfoClasseDTO();

                    // Tutti i livelli → Set<Integer> (ignora livelli senza LVL_CLASSE valido)
                    Set<Integer> livelliSet = e.getValue().stream()
                            .map(x -> parseIntOrNull(x.getLabel(Constants.ITEM_LIVELLO_LVL_CLASSE)))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

                    Integer livelloMax = livelliSet.stream()
                            .max(Integer::compareTo).orElse(0);

                    Integer livelloTotale = e.getValue().size();

                    // Livelli NON maledetti per questa classe
                    Integer livelloNonMaledetto = (int) livelliAttivi.stream()
                            .filter(x -> e.getKey().getId().toString()
                                    .equals(x.getLabel(Constants.ITEM_LABEL_CLASSE)))
                            .count();

                    // NUOVO: Max tra i NON maledetti
                    Integer livelloMaxNonMaledetto = livelliAttivi.stream()
                            .filter(x -> e.getKey().getId().toString()
                                    .equals(x.getLabel(Constants.ITEM_LABEL_CLASSE)))
                            .map(x -> parseIntOrNull(x.getLabel(Constants.ITEM_LIVELLO_LVL_CLASSE)))
                            .filter(Objects::nonNull)
                            .max(Integer::compareTo)
                            .orElse(0);

                    dto.setClasse(e.getKey());
                    dto.setLivelli(livelliSet);
                    dto.setLivelloMax(livelloMax);
                    dto.setLivelloTotale(livelloTotale);
                    dto.setLivelloNonMaledetto(livelloNonMaledetto);
                    dto.setLivelloMaxNonMaledetto(livelloMaxNonMaledetto);
                    return dto;
                })
                .sorted(Comparator.comparing(
                        x -> x.getClasse().getNome(),
                        Comparator.nullsLast(String::compareToIgnoreCase)
                ))
                .toList();

        out.setClassi(classi);
        return out;
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
