package it.fin8.gdrsheet.def;

import lombok.Getter;

@Getter
public enum TipoPermessoPersonaggio {
    PROPRIETARIO("Proprietario"),
    VISUALIZZATORE("Visualizzatore");

    private final String label;

    TipoPermessoPersonaggio(String label) {
        this.label = label;
    }
}
