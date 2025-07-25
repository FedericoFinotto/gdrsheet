package it.fin8.grdsheet.def;

import lombok.Getter;

@Getter
public enum TipoRuolo {
    ADMIN("ADMIN"),
    MASTER("MASTER"),
    EDITOR("EDITOR"),
    GIOCATORE("GIOCATORE");

    private final String label;

    TipoRuolo(String label) {
        this.label = label;
    }

}
