package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Riga di associazione (oggetto, tag, peso). La categoria è determinata dal tag e viene
 * restituita per comodità dell'interfaccia: in scrittura è ignorata.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemTagDTO {

    private Integer idTag;
    private String tag;
    private Integer idCategoria;
    private String categoria;
    private BigDecimal peso;

    /** Stato completo dei tag di un oggetto: sostituisce integralmente quelli esistenti. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private List<ItemTagDTO> tags;
    }
}
