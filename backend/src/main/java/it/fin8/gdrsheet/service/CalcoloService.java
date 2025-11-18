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
import java.util.stream.Stream;

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

        // 1) Prendo SOLO i livelli non disabilitati
        List<Item> livelliAttivi = initialRoots.stream()
                .filter(Objects::nonNull)
                .filter(i -> TipoItem.LIVELLO.equals(i.getTipo()))
                .filter(x -> !utilService.parseBooleanFromString(x.getLabel(Constants.ITEM_LABEL_DISABILITATO), Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE, Constants.ITEM_LABEL_DISABILITATO_VALORE_FALSE) && x.getLabel(Constants.ITEM_LABEL_MALEDIZIONE) == null)
                .toList();

        // 2) livello = numero di livelli attivi
        out.setLivello(livelliAttivi.size() - 1);

        // 3) Per ogni livello attivo: trova le classi collegate e i valori LVL_CLASSE (lista di interi)
        //    Costruisci una mappa Classe -> TreeSet<Integer> dei livelli (ordinati, unici)
        Map<Item, Set<Integer>> perClasse = livelliAttivi.stream()
                .flatMap(livello -> {
                    // livelli dichiarati nella label LVL_CLASSE
                    List<Integer> lvls = utilService.parseStringToIntList(
                            livello.getLabel(Constants.ITEM_LIVELLO_LVL_CLASSE)
                    );
                    if (lvls.isEmpty()) return Stream.empty();

                    // tutte le classi collegate a questo livello
//                    List<Item> classi = Optional.ofNullable(livello.getChild())
//                            .orElseGet(Collections::emptyList)
//                            .stream()
//                            .map(Collegamento::getItemTarget)
//                            .filter(it -> TipoItem.CLASSE.equals(it.getTipo()))
//                            .toList();
//                    if (classi.isEmpty()) return Stream.empty();
                    String classeId = livello.getLabel(Constants.ITEM_LABEL_CLASSE);
                    List<Item> classi = List.of(itemRepository.findItemById(Integer.parseInt(classeId)));

                    // flat: (classe, livelloSingolo)
                    return classi.stream().flatMap(cl ->
                            lvls.stream().map(lv -> new AbstractMap.SimpleEntry<>(cl, lv)));
                })
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toCollection(TreeSet::new))
                ));

        // 4) Converto in List<InfoClasseDTO>
        List<InfoClasseDTO> classi = perClasse.entrySet().stream()
                .map(e -> {
                    InfoClasseDTO dto = new InfoClasseDTO();
                    dto.setClasse(e.getKey());
                    dto.setLivelli(e.getValue()); // già TreeSet -> ordinato e unico
                    return dto;
                })
                // opzionale: ordina per nome classe
                .sorted(Comparator.comparing(x -> x.getClasse().getNome(), Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        out.setClassi(classi);
        return out;
    }


}
