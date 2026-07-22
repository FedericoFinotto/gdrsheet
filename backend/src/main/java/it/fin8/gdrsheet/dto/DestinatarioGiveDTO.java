package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Possibile destinazione di un "Dai a" (spostamento item tra personaggi dello stesso party):
 * il personaggio principale (contenitoreId/contenitoreNome null), oppure uno dei suoi CONTENITORE
 * con INVENTARIO_SEPARATO=1 (es. la Stiva di una NAVE).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DestinatarioGiveDTO {
    private Integer personaggioId;
    private String personaggioNome;
    private Integer contenitoreId;
    private String contenitoreNome;
}
