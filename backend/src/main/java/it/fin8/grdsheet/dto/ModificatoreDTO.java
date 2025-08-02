package it.fin8.grdsheet.dto;

import it.fin8.grdsheet.def.TipoModificatore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModificatoreDTO {
    private Integer id;

    private String statId;

    private Integer valore;

    private String formula;

    private String nota;

    private TipoModificatore tipo;

    private Boolean sempreAttivo;

    private String item;
}
