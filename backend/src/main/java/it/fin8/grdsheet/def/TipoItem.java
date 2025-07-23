package it.fin8.grdsheet.def;

import lombok.Getter;

@Getter
public enum TipoItem {
    ABILITA("Abilità"),
    TALENTO("Talento"),
    OGGETTO("Oggetto"),
    CONSUMABILE("Consumabile"),
    ARMA("Arma"),
    MUNIZIONE("Munizione"),
    EQUIPAGGIAMENTO("Equipaggiamento"),
    PERSONAGGIO("Personaggio"),
    CLASSE("Classe"),
    RAZZA("Razza"),
    ATTACCO("Attacco"),
    ALTRO("Altro");

    private final String label;

    TipoItem(String label) {
        this.label = label;
    }

}
