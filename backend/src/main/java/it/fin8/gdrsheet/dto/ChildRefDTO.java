package it.fin8.gdrsheet.dto;

import it.fin8.gdrsheet.def.TipoItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Riferimento leggero (id + nome + tipo) a un item collegato come figlio di un altro item. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChildRefDTO {
    private Integer id;
    private String nome;
    private TipoItem tipo;
}
