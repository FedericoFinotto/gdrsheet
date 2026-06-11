package it.fin8.gdrsheet.def;

import lombok.Getter;

@Getter
public enum TipoRuoloParty {
    MASTER("Master"),
    GIOCATORE("Giocatore");

    private final String label;

    TipoRuoloParty(String label) {
        this.label = label;
    }
}
