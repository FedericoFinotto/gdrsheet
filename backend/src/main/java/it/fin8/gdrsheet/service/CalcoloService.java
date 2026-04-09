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

    public String calcola(String formula, List<CaratteristicaDTO> caratteristiche) {
        if (formula == null || formula.isBlank()) return "0";

        // 1. Sostituisci @XXX con i modificatori numerici
        String replaced = formula;
        Matcher m = PATTERN_PLACEH.matcher(replaced);
        while (m.find()) {
            String ph = m.group();        // esempio: @FOR
            String id = ph.substring(1); // esempio: FOR
            int mod = caratteristiche.stream()
                    .filter(c -> id.equalsIgnoreCase(c.getId()))
                    .map(c -> c.getModificatore() != null ? c.getModificatore() : 0)
                    .findFirst()
                    .orElse(0);
            replaced = replaced.replace(ph, String.valueOf(mod));
        }

        // 2. Estrai e salva la parte col dado (es: 1d8)
        Matcher diceMatcher = PATTERN_DICE.matcher(replaced);
        String dicePart = "";
        if (diceMatcher.find()) {
            dicePart = diceMatcher.group();
            replaced = replaced.replace(dicePart, ""); // rimuove TUTTE le occorrenze di quel dado
        }

        // 3. Pulizia formula numerica (rimuove spazi)
        String numericExpr = replaced.replaceAll("\\s+", "");

        long result = 0;
        if (!numericExpr.isBlank()) {
            try {
                double eval = new ExpressionBuilder(numericExpr).build().evaluate();
                result = (long) Math.floor(eval); // arrotonda sempre per difetto
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

        List<Item> initialRoots = itemRepository.findAllByPersonaggioIdWithChild(dati.getId());
        caratteristiche.add(new CaratteristicaDTO("LVL", "Livello", null, Integer.parseInt(String.valueOf(getLivelli(initialRoots).getLivello())), null, null));

        return calcola(formula, caratteristiche);
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

                    // Tutti i livelli → Set<Integer>
                    Set<Integer> livelliSet = e.getValue().stream()
                            .map(x -> Integer.parseInt(x.getLabel(Constants.ITEM_LIVELLO_LVL_CLASSE)))
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
                            .map(x -> Integer.parseInt(x.getLabel(Constants.ITEM_LIVELLO_LVL_CLASSE)))
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
}
