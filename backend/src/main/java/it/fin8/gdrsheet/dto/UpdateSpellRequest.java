package it.fin8.gdrsheet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Payload di aggiornamento di uno Spell/Item di tipo INCANTESIMO.
 * Tutti i campi sono opzionali (PATCH-like): valorizza solo ciò che vuoi aggiornare.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateSpellRequest {

    /**
     * Nome incantesimo (IT)
     */
    @Size(max = 100)
    private String nome;

    /**
     * Descrizione in italiano (markdown/testo semplice)
     */
    private String descrizione;

    /**
     * Tempo di lancio normalizzato in IT (es. "1 azione standard", "1 round", "Almeno 10 minuti")
     */
    private String tempo;

    /**
     * Raggio/Portata normalizzata in IT e metri (es. "Vicino (7,5 m + 1,5 m/2 livelli)")
     */
    private String range;

    /**
     * Durata normalizzata in IT (es. "Istantanea", "1 ora/livello (D)", "Concentrazione + 1 round")
     */
    private String durata;

    /**
     * Tiro salvezza normalizzato in IT (es. "Volontà nega (innocuo) o Riflessi mezzi danni; vedi testo")
     */
    private String ts;

    /**
     * Componenti selezionate (es. ["V","S","M","F","DF","XP","X","COLDFIRE","FROSTFELL","B"])
     */
    private List<String> componenti;

    /**
     * Scuole (IT) es. ["Invocazione"]
     */
    private List<String> scuole;

    /**
     * Sottoscuole (IT) es. ["Creazione","Fantasma"]
     */
    private List<String> subscuole;

    /**
     * Descrittori (IT) es. ["Fuoco","Paura","Influenza mentale"]
     */
    private List<String> descrittori;

    /**
     * Classi/Domini con livello: lista di coppie {classe:"SP_*", livello:0..9}
     * Esempio: [{classe:"SP_WIZARD", livello:3}, {classe:"SP_FIRE", livello:2}]
     */
    private List<ClassLevelDTO> classi;

    /**
     * Patch dirette sulle label (facoltativo).
     * Usato per mantenere una rappresentazione compatta di SCUOLA_SP già formattata in IT.
     * Esempio: { "SCUOLA_SP": "Invocazione (Creazione) [Fuoco, Luce]" }
     */
    private Map<String, String> labelsPatch;


}
