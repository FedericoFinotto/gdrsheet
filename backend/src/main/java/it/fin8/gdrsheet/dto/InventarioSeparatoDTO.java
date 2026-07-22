package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Contenuto di un CONTENITORE con label INVENTARIO_SEPARATO=1: i suoi discendenti (a qualunque
 * profondità) vengono tolti dalle liste normali per tipo e raggruppati qui, per essere mostrati
 * in scheda come una sezione a parte (es. la Stiva di una NAVE).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InventarioSeparatoDTO {
    private Integer contenitoreId;
    private String contenitoreNome;
    private List<ItemDTO> items;
}
