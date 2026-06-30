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

    private Boolean sempreAttivo;

    private String item;

    private Integer itemId;

    private TipoItem tipoItem;

    public String itemIdInFormula() {
        // $<numericId>_VAR → @<numericId>_VAR (riferimento esplicito a un altro item, mantieni l'ID)
        String result = formula.replaceAll("\\$(\\d+)_", "@$1_");
        // $VAR → @<itemCorrente>_VAR (variabile dell'item stesso)
        return result.replace("$", "@" + itemId + "_");
    }
}
