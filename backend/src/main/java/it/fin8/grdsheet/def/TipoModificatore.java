package it.fin8.grdsheet.def;

import lombok.Getter;

@Getter
public enum TipoModificatore {
    RANK("Rank"),
    VALORE("Valore"),
    MOD("Modificatore"),
    CA_DEVIAZIONE("Deviazione"),
    CA_SCHIVARE("Schivare"),
    CA_ARMOR("Armatura"),
    CA_SHIELD("Scudo"),
    CA_MAGIC("Magico"),
    CA_NATURALE("Naturale"),
    BASE("Base");

    private final String label;

    TipoModificatore(String label) {
        this.label = label;
    }

}
