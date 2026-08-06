package it.fin8.gdrsheet.def;

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
    ALTRO("Altro"),
    LIVELLO("Livello"),
    MALEDIZIONE("Maledizione"),
    INCANTESIMO("Incantesimo"),
    TRASFORMAZIONE("Trasformazione"),
    AVANZAMENTO("Avanzamento"),
    COMP("Competenza"),
    LINGUA("Lingua"),
    IDOLO("Idolo"),
    FRUTTO("Frutto"),
    FORMA("Forma"),
    PRIVILEGIO("Privilegio di Classe"),
    CONTENITORE("Contenitore"),
    PATTO("Patto"),
    NOTIZIA("Notizia"),
    EFFETTO("Effetto"),
    QUEST("Quest"),
    VEICOLO("Veicolo"),
    INFO("Info"),
    CATEGORIA("Categoria"),
    TAG("Tag"),
    RANDOMIZZATORE("Randomizzatore"),
    CASO("Caso"),
    SKILL_TRICK("Skill Trick"),
    IMMAGINE("Immagine")
    ;

    private final String label;

    TipoItem(String label) {
        this.label = label;
    }

}
