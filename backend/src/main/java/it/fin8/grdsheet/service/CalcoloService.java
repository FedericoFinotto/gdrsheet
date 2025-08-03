package it.fin8.grdsheet.service;

import it.fin8.grdsheet.dto.CaratteristicaDTO;
import it.fin8.grdsheet.dto.DatiPersonaggioDTO;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CalcoloService {
    private static final Pattern PATTERN_PLACEH = Pattern.compile("@\\w+");
    // match singolo tiro di dado, eventualmente con un +offset o -offset
    private static final Pattern PATTERN_DICE = Pattern.compile("\\d+d\\d+(?:[+-]\\d+)?");
    private static final Pattern PATTERN_NUMBER = Pattern.compile("^[+-]?\\d+$");

    public String calcola(String formula, List<CaratteristicaDTO> caratteristiche) {
        if (formula == null || formula.isBlank()) {
            return "0";
        }

        // 1) sostituisci @ID con il modificatore
        String eval = formula;
        Matcher m0 = PATTERN_PLACEH.matcher(eval);
        while (m0.find()) {
            String ph = m0.group();
            String id = ph.substring(1);
            int mod = caratteristiche.stream()
                    .filter(c -> id.equals(c.getId()))
                    .map(c -> c.getModificatore() != null ? c.getModificatore() : 0)
                    .findFirst().orElse(0);
            eval = eval.replace(ph, Integer.toString(mod));
        }
        // togli spazi
        eval = eval.replaceAll("\\s+", "");

        // 2) estrai i dice token
        Matcher md = PATTERN_DICE.matcher(eval);
        List<String> diceTokens = new ArrayList<>();
        while (md.find()) {
            diceTokens.add(md.group());
        }
        // rimuovi tutti i dice token per isolare la parte numerica
        String numericExpr = PATTERN_DICE.matcher(eval).replaceAll("");
        // ripulisci eventuali + iniziali/finali
        numericExpr = numericExpr.replaceAll("(^\\+|\\+$)", "");

        // 3) valuta la parte numerica (se non vuota)
        long numericResult = 0;
        if (!numericExpr.isBlank()) {
            // se è un numero puro
            if (PATTERN_NUMBER.matcher(numericExpr).matches()) {
                numericResult = Long.parseLong(numericExpr);
            } else {
                try {
                    numericResult = Math.round(
                            new ExpressionBuilder(numericExpr).build().evaluate()
                    );
                } catch (Exception ex) {
                    // in caso di errore, fallback a 0
                    numericResult = 0;
                }
            }
        }

        // 4) ricompone il risultato
        String result;
        if (diceTokens.isEmpty()) {
            // nessun dado, restituisci solo il numerico
            result = Long.toString(numericResult);
        } else {
            // unisci tutti i dice con '+'
            String diceExpr = String.join("+", diceTokens);
            if (numericResult != 0) {
                result = diceExpr + "+" + numericResult;
            } else {
                result = diceExpr;
            }
        }
        return result;
    }

    /**
     * Overload per DTO personaggio
     */
    public String calcola(String formula, DatiPersonaggioDTO dati) {
        return calcola(formula, dati.getCaratteristiche());
    }
}

