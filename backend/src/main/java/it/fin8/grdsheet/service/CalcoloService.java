package it.fin8.grdsheet.service;

import it.fin8.grdsheet.dto.CaratteristicaDTO;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CalcoloService {
    private static final Pattern REGEX_DADI = Pattern.compile("^\\d+d\\d+([+-]\\d+)?$");
    private static final Pattern REGEX_NUMERO = Pattern.compile("^[+-]?\\d+$");
    private static final Pattern PATTERN_PLACEHOLDER = Pattern.compile("@\\w+");

    /**
     * Restituisce:
     * - null se è un tiro di dado (tipo "1d6+3")
     * - un Integer (anche negativo) per gli altri casi
     * - 0 se formula vuota o in caso di errore
     */
    public Integer calcola(String formula, List<CaratteristicaDTO> caratteristiche) {
        if (formula == null || formula.isBlank()) {
            return 0;
        }

        // 1) sostituisci @id con il modificatore
        String eval = formula;
        Matcher m = PATTERN_PLACEHOLDER.matcher(formula);
        while (m.find()) {
            String placeholder = m.group();        // es. "@forza"
            String id = placeholder.substring(1);  // "forza"
            Optional<CaratteristicaDTO> opt = caratteristiche.stream()
                    .filter(c -> id.equals(c.getId()))
                    .findFirst();
            int mod = opt.map(c -> c.getModificatore() != null ? c.getModificatore() : 0)
                    .orElse(0);
            eval = eval.replaceAll(Pattern.quote(placeholder), Integer.toString(mod));
        }

        // 2) rimuovi spazi
        eval = eval.replaceAll("\\s+", "");

        // 3) se è un dado, restituisci null
        if (REGEX_DADI.matcher(eval).matches()) {
            return null;
        }

        // 4) se è un numero puro, parseInt
        if (REGEX_NUMERO.matcher(eval).matches()) {
            return Integer.valueOf(eval);
        }

        // 5) espressione aritmetica: usa exp4j
        try {
            Expression e = new ExpressionBuilder(eval).build();
            double result = e.evaluate();
            return (int) Math.round(result);
        } catch (Exception ex) {
            System.err.println("Errore nel valutare l'espressione: " + eval);
            ex.printStackTrace();
            return 0;
        }
    }
}
