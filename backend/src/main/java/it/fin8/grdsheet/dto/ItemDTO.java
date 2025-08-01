package it.fin8.grdsheet.dto;

import it.fin8.grdsheet.def.TipoItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ItemDTO {
    private Integer id;
    private String nome;
    private TipoItem tipo;
    private Boolean enabled;
}
