package it.fin8.gdrsheet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Payload di salvataggio di un item LIVELLO dal LivelloEditor.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateLivelloRequest {

    private Integer livelloId;
    private Integer personaggioId;

    /**
     * Livello del personaggio (label LVL).
     */
    private Integer livello;

    /**
     * Incrementi di caratteristica scelti al livello: statId -> valore (modificatori BASE).
     */
    private Map<String, Integer> caratteristiche;

    /**
     * Id dell'item CLASSE associato (label CLASSE).
     */
    private Integer classeId;

    /**
     * Nome della maledizione (label MLDZN), null per rimuoverla.
     */
    private String maledizioneNome;

    /**
     * Livelli di classe coperti da questo livello (label LVL_CLASSE, comma separated).
     */
    private List<Integer> livelliClasse;

    /**
     * Punti spesi nelle abilità a questo livello (modificatori RANK).
     */
    private List<RangoSpesoDTO> ranghi;

    /**
     * Contenuti del livello selezionati (item concessi e modificatori concessi
     * dagli avanzamenti di classe). Null = non toccare i grants esistenti.
     */
    private List<GrantSelezionatoDTO> grantsSelezionati;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RangoSpesoDTO {
        /**
         * Id della stat abilità (es. "ASCOLTARE").
         */
        private String abilitaId;
        private Integer punti;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GrantSelezionatoDTO {
        /**
         * Formato: "item-&lt;idItem&gt;" oppure "mod-&lt;idModificatore&gt;".
         */
        private String id;
        private String tipo; // ITEM | MOD
        private Integer livello;
        private String descrizione;
    }
}
