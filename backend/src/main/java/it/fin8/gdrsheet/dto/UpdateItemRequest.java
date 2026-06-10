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
     * Stato completo delle labels (chiave/valore, chiavi ripetibili).
     */
    private List<LabelRowDTO> labels;

    /**
     * Stato completo dei modificatori.
     */
    private List<ModificatoreRowDTO> modificatori;

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
