package it.fin8.gdrsheet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.def.TipoModificatore;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Payload generico di creazione/aggiornamento di un Item.
 * Labels e modificatori rappresentano lo stato completo desiderato:
 * se valorizzati, sostituiscono integralmente quelli esistenti.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateItemRequest {

    @Size(max = 100)
    private String nome;

    private String descrizione;

    /**
     * Tipo item: obbligatorio in creazione, ignorato in aggiornamento.
     */
    private TipoItem tipo;

    /**
     * Solo in creazione: personaggio a cui agganciare l'item. L'item viene
     * creato nel compendio (personaggio null, sistema/mondo del party) e
     * collegato come child del FromCompendio del personaggio.
     */
    private Integer idPersonaggio;

    /**
     * Solo in creazione: mondo e sistema a cui legare l'item. Se assenti si usa
     * il mondo/sistema del party del personaggio (se presente).
     */
    private Integer idMondo;
    private Integer idSistema;

    /**
     * Solo in creazione: se true, l'item NON viene agganciato al FromCompendio del
     * personaggio (resta comunque del mondo/sistema del party). Usato quando l'item
     * sarà collegato come child di un altro item (creazione "al volo" di un figlio),
     * per evitare il doppio collegamento.
     */
    private Boolean skipFromCompendio;

    /**
     * Stato completo delle labels (chiave/valore, chiavi ripetibili).
     */
    private List<LabelRowDTO> labels;

    /**
     * Stato completo dei modificatori.
     */
    private List<ModificatoreRowDTO> modificatori;

    /**
     * Stato completo degli attacchi (item ATTACCO figli, gestiti inline).
     * Null = non toccare.
     */
    private List<AttaccoRowDTO> attacchi;

    /**
     * Stato completo degli item collegati come child (esclusi gli ATTACCO,
     * gestiti da {@link #attacchi}). Null = non toccare.
     */
    private List<ChildRefDTO> children;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChildRefDTO {
        private Integer id;
        /** Utilizzi concessi da questo collegamento; null = 1. */
        private Integer qty;
        /** Formula per il calcolo degli utilizzi (es. "2*@LVL"). Se valorizzata ha precedenza su qty. */
        private String formulaQty;
        /** Cosa si prende da un FRUTTO: MOD, FORMA_1, FORMA_2, FORMA_3, FORMA_4. Null per altri tipi. */
        private String scelta;
        /** Se true il collegamento è nascosto (es. una FORMA): non visibile da fuori. Nascondere implica disabilitare. */
        private Boolean nascosto;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LabelRowDTO {
        private String label;
        private String valore;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AttaccoRowDTO {
        /**
         * Id dell'item ATTACCO esistente; null per i nuovi.
         */
        private Integer id;
        private String nome;
        private String tpc;
        private String tpd;
        private String tipoDanni;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModificatoreRowDTO {
        /**
         * Id del modificatore esistente; null per i nuovi.
         */
        private Integer id;
        private String statId;
        private TipoModificatore tipo;
        private String valore;
        private String nota;
        private Boolean sempreAttivo;
    }
}
