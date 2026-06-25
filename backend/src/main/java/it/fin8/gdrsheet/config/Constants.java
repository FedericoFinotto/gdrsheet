package it.fin8.gdrsheet.config;

import java.util.List;

public final class Constants {
    private Constants() {
    }

    public static final List<String> listOfUniqueByClassStats = List.of("BAB", "TMP", "VLT", "RFL");

    public static final String TIPO_ITEM_ABILITA = "ABILITA";
    public static final String TIPO_ITEM_TALENTO = "TALENTO";
    public static final String TIPO_ITEM_OGGETTO = "OGGETTO";
    public static final String TIPO_ITEM_CONSUMABILE = "CONSUMABILE";
    public static final String TIPO_ITEM_ARMA = "ARMA";
    public static final String TIPO_ITEM_MUNIZIONE = "MUNIZIONE";
    public static final String TIPO_ITEM_EQUIPAGGIAMENTO = "EQUIPAGGIAMENTO";
    public static final String TIPO_ITEM_PERSONAGGIO = "PERSONAGGIO";
    public static final String TIPO_ITEM_ALTRO = "ALTRO";
    public static final String TIPO_ITEM_CLASSE = "CLASSE";
    public static final String TIPO_ITEM_RAZZA = "RAZZA";
    public static final String TIPO_ITEM_ATTACCO = "ATTACCO";

    public static final String ITEM_INCANTESIMI_PREPARATI = "PreparedSpell";
    public static final String ITEM_FROM_COMPENDIO = "FromCompendio";
    public static final String ITEM_LABEL_LIVELLO_INCANTESIMO = "CLIVELLO";
    public static final String ITEM_LABEL_CLASSE_INCANTESIMO = "CCLASSE";
    public static final String ITEM_LABEL_ATTACCO_TIRO_PER_COLPIRE = "TPC";
    public static final String ITEM_LABEL_ATTACCO_DANNI = "TPD";
    public static final String ITEM_LABEL_ATTACCO_TIRO_SALVEZZA = "TTS";
    public static final String ITEM_LABEL_ATTACCO_RANGE = "TRANGE";
    public static final String ITEM_LABEL_ATTACCO_TIPO_DANNI = "TDANNO";
    public static final String ITEM_LABEL_DISABILITATO = "DISABLED";
    public static final String ITEM_LABEL_DISABILITATO_VALORE_TRUE = "1";
    public static final String ITEM_LABEL_DISABILITATO_VALORE_FALSE = "0";
    public static final String ITEM_LABEL_LISTA_INCANTESIMI = "SPELL";
    public static final String ITEM_LABEL_COMPONENTE = "COMP_SP";
    public static final String ITEM_LABEL_SCUOLA_SP = "SCUOLA_SP"; // scuola/sottoscuole/descrittori incantesimo
    public static final String ITEM_LABEL_ABILITA_CLASSE = "ABCLASSE";
    public static final String ITEM_LABEL_LISTA_COMPETENZE = "COMP";
    public static final String ITEM_LABEL_LISTA_LINGUE = "LINGUE";
    public static final String ITEM_LABEL_COMPETENZA_RICHIESTA = "REQ_COMP";
    public static final String ITEM_LABEL_TAGLIA = "TAGLIA";       // SET: imposta la taglia a un valore
    public static final String ITEM_LABEL_ADD_TAGLIA = "ADD_TAGLIA"; // ADD: incrementa/decrementa la taglia base
    public static final String ITEM_LABEL_SPELL_SLOT = "SP_SLOT";
    public static final String ITEM_LABEL_SPELL_SLOT_BONUS = "SP_SLOT_BONUS";
    public static final String COLLEGAMENTO_LABEL_LIVELLO = "LIVELLO";
    public static final String COLLEGAMENTO_LABEL_LISTA_INCANTESIMI = "SP_LIST";
    public static final String COLLEGAMENTO_LABEL_N_USATI = "USED";
    public static final String COLLEGAMENTO_LABEL_N_PREPARATI = "PREPARED";
    public static final String COLLEGAMENTO_LABEL_ALWAYS_PREPARED = "ALWAYS";
    public static final String ITEM_LIVELLO_LVL = "LVL";
    public static final String ITEM_LIVELLO_LVL_CLASSE = "LVL_CLASSE";
    public static final String ITEM_LABEL_DADI_VITA = "DV";
    public static final String ITEM_LABEL_MALEDIZIONE = "MLDZN";
    public static final String ITEM_LABEL_PUNTI_FERITA = "PF";
    public static final String ITEM_LABEL_CLASSE = "CLASSE";
    public static final String ITEM_LABEL_GRUPPO_TRASFORMAZIONE = "GRP_TRASF";
    public static final String LABEL_PESO = "PESO"; // kg, su item_label e personaggio_label
    public static final String LABEL_QTA = "QTA";   // quantità item, moltiplica il peso
    public static final String LABEL_CC = "CC";     // conto corrente banca: G<idPersonaggio> | P<idParty>
    public static final String ITEM_LABEL_COMPENDIO = "COMPENDIO"; // visibilità nel compendio
    public static final String ITEM_LABEL_TIPO = "TIPO";           // tipo specifico item (es. BARRIERA)
    public static final String ITEM_TIPO_BARRIERA = "BARRIERA";
    public static final String ITEM_LABEL_BARR_MAX = "BARR_MAX";   // hp massimi della barriera
    public static final String ITEM_LABEL_BARR_CONS = "BARR_CONS"; // hp consumati della barriera
    public static final String ITEM_LABEL_FRUTTO_LVL = "$V_LVL";       // variabile livello del frutto
    public static final String ITEM_LABEL_FORMA_MOD_LVL = "$M_P_LVL";  // forma: imposta il livello del frutto padre
    public static final String ITEM_LABEL_ADD_CLASSE_PREFIX = "ADD_CLASSE_"; // ADD_CLASSE_<n> = +1 livello (valore = idClasse)
    public static final String ITEM_LABEL_ADD_CLASSE_VALUE_SUFFIX = "_VALUE";
    public static final String LABEL_CC_GIOCATORE_PREFIX = "G";
    public static final String LABEL_CC_PARTY_PREFIX = "P";
    public static final String TIPO_PERSONAGGIO_BANCA = "BANCA";

    public static final String RANK_MAX = "MAX";

    // Gradi/abilità: formule sulla CLASSE e valore congelato sul LIVELLO
    public static final String ITEM_LABEL_RANK_PRIMO = "RANK_1"; // formula gradi quando è il livello 1 del personaggio
    public static final String ITEM_LABEL_RANK = "RANK";         // formula gradi per tutti gli altri livelli
    public static final String ITEM_LABEL_GRADI_LIVELLO = "GRADI_LIVELLO"; // valore gradi congelato sul livello (non retroattivo)
    public static final String ITEM_LABEL_NUM_LIVELLI_CLASSE = "LIVELLI_CLASSE"; // numero di livelli della classe (default 20)
    public static final String ITEM_LABEL_EN_NAME = "EN_NAME";         // nome originale in inglese
    public static final String ITEM_LABEL_MANUALE = "MANUALE_SP";      // manuale di provenienza
    public static final String ITEM_LABEL_VISIBILITA = "VISIBILITA";   // visibilità item: vuoto=tutti, OWNER, MASTER
    public static final String ITEM_VISIBILITA_OWNER = "OWNER";        // solo proprietario + master + admin
    public static final String ITEM_VISIBILITA_MASTER = "MASTER";      // solo master + admin

    // Info anagrafiche del personaggio (personaggio_label)
    public static final List<String> PERSONAGGIO_INFO_LABELS = List.of(
            "LUOGO_NASCITA", "DATA_NASCITA", "RAZZA", "SESSO", "PELLE", "ETA",
            "ALTEZZA", "PESO", "CAPELLI", "OCCHI", "ALLINEAMENTO", "TAGLIA");
}

