package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.dto.CaratteristicaDTO;
import it.fin8.gdrsheet.dto.DatiPersonaggioDTO;
import it.fin8.gdrsheet.entity.Collegamento;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.mapper.StatMapper;
import it.fin8.gdrsheet.repository.ItemRepository;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class CalcoloService {

    @Autowired
    private StatMapper statMapper;

    @Autowired
    private ItemRepository itemRepository;

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
        Map<Item, Long> livelli = getLivelli(initialRoots);
        long livelloTotale = livelli.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        caratteristiche.add(new CaratteristicaDTO("LVL", "Livello", null, Integer.parseInt(String.valueOf(livelloTotale)), null));

        return calcola(formula, caratteristiche);
    }

    public Map<Item, Long> getLivelli(List<Item> initialRoots) {
        return initialRoots.stream()
                .filter(i -> TipoItem.LIVELLO.equals(i.getTipo()))
                .map(liv -> liv.getChild().stream()
                        .map(Collegamento::getItemTarget)
                        .filter(itemTarget -> TipoItem.CLASSE.equals(itemTarget.getTipo()))
                        .findFirst().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));
    }

}
