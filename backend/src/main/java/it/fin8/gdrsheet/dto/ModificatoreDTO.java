package it.fin8.gdrsheet.dto;

import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.def.TipoModificatore;
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

    private Boolean permanente;

    private String item;

    private TipoItem tipoItem;
}
