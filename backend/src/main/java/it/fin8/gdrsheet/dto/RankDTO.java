package it.fin8.gdrsheet.dto;

import it.fin8.gdrsheet.def.TipoModificatore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RankDTO {
    private Integer id;

    private String statId;

    private Integer valore;

    private String nota;

    private TipoModificatore tipo;

    private Boolean sempreAttivo;

    private Boolean diClasse;

    private String classe;

    public Integer getModificatore() {
        return diClasse ? valore : (int) Math.floor((double) (valore / 2));
    }

    private String item;
}
