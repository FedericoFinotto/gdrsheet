package it.fin8.gdrsheet.dto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Variabili ($id / @id) per risolvere le formule dei modificatori: costruita UNA SOLA VOLTA per
 * personaggio da PersonaggioService e condivisa (letta e scritta) da tutti i calcola* di
 * ModificatoriService, invece di essere ricostruita identica dentro ognuno. La taglia (attuale e
 * base, vedi Constants.VARIABILE_TAGLIA/VARIABILE_TAGLIA_BASE) è calcolata fuori da questi
 * metodi, una volta sola, e messa qui dentro come una variabile qualunque, non come un campo
 * dedicato. È una ConcurrentHashMap perché letta/scritta anche dai calcoli paralleli (CA, BAB...).
 */
public class VariabiliDTO {
    private final Map<String, String> valori = new ConcurrentHashMap<>();

    public String get(String chiave) {
        return valori.get(chiave);
    }

    /** Come {@link #get}, già parsato a int (0 se assente o non numerico). */
    public int getInt(String chiave) {
        try {
            return Integer.parseInt(valori.get(chiave));
        } catch (Exception e) {
            return 0;
        }
    }

    public void set(String chiave, String valore) {
        valori.put(chiave, valore);
    }

    public void set(String chiave, int valore) {
        valori.put(chiave, String.valueOf(valore));
    }

    public void setAll(Map<String, String> altri) {
        valori.putAll(altri);
    }

    /** Mappa grezza, per calcoloService.calcola(formula, Map). */
    public Map<String, String> mappa() {
        return valori;
    }

    /**
     * Legge il valore già in cache sotto {@code chiave}, oppure lo calcola con {@code calcolo} e
     * lo scrive prima di restituirlo: le chiamate successive con la stessa chiave lo ritrovano
     * già pronto invece di ricalcolarlo. Atomico (usa {@link ConcurrentHashMap#computeIfAbsent}),
     * quindi sicuro anche se richiesto in parallelo dallo stesso valore mancante.
     */
    public String getOrCalcola(String chiave, Supplier<String> calcolo) {
        return valori.computeIfAbsent(chiave, k -> calcolo.get());
    }
}
