package it.fin8.grdsheet.def;

import lombok.Getter;

@Getter
public enum TipoModificatore {
    RANK("Rank"),
    VALORE("Valore"),
    DEVIAZIONE("Deviazione"),
    CA("CA");

    private final String label;

    TipoModificatore(String label) {
        this.label = label;
    }

}
