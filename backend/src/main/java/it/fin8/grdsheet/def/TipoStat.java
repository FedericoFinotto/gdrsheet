package it.fin8.grdsheet.def;

import lombok.Getter;

@Getter
public enum TipoStat {
    CAR("Caratteristica"),
    AB("Abilitá"),
    TS("Tiro Salvezza"),
    PF("Punti Ferita"),
    ATT("Attributo"),
    CA("Classe Armatura"),
    ATK("Attacco"),
    COUNT("Contatore");

    private final String label;

    TipoStat(String label) {
        this.label = label;
    }

}
