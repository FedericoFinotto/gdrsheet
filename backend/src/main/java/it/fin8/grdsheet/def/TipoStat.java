package it.fin8.grdsheet.def;

import lombok.Getter;

@Getter
public enum TipoStat {
    CAR("Caratteristica"),
    AB("Abilitá"),
    TS("Tiro Salvezza"),
    PF("Punti Ferita");

    private final String label;

    TipoStat(String label) {
        this.label = label;
    }

}
