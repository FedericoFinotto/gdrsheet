package it.fin8.gdrsheet.config;

import java.util.List;

public final class Constants {
    private Constants() {
    }

    public static final List<String> listOfUniqueByClassStats = List.of("BAB", "TMP", "VLT", "RFL");

    // Stat fittizia: un CAMBIA_CARATTERISTICA su questa stat cambia la caratteristica base di TUTTE le abilità
    public static final String STAT_TUTTE_ABILITA = "ABTUTTE";
    // Stat fittizia: un CAMBIA_CARATTERISTICA su questa stat cambia la caratteristica base di TUTTI i tiri salvezza
    public static final String STAT_TUTTI_TS = "TSTUTTI";

    // Chiavi in VariabiliDTO per valori calcolati fuori dai calcola* di ModificatoriService (non
    // ricavabili da un item/caratteristica) e riletti al bisogno invece di essere ricalcolati.
    public static final String VARIABILE_TAGLIA = "@TAGLIA";      // taglia attuale, con tutti i modificatori
    public static final String VARIABILE_TAGLIA_BASE = "@TAGLIA_BASE"; // taglia base da anagrafica
    public static final String VARIABILE_DIFFERENZA_TAGLIA = "@DIFFERENZA_TAGLIA"; // TAGLIA - TAGLIA_BASE

    // Gruppi di personaggi in un party (personaggio_label)
    public static final String LABEL_GRUPPO = "GRUPPO";          // valore = id del gruppo
    public static final String LABEL_CAPOGRUPPO = "CAPOGRUPPO";  // "1" = capogruppo del suo gruppo

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
    // legacy (un solo danno per attacco): tenute solo per leggere dati vecchi, non più scritte
    public static final String ITEM_LABEL_ATTACCO_DANNI = "TPD";
    public static final String ITEM_LABEL_ATTACCO_TIPO_DANNI = "TDANNO";

    public static final String ITEM_LABEL_ATTACCO_TIRO_PER_COLPIRE = "TPC";
    public static final String ITEM_LABEL_ATTACCO_TIRO_SALVEZZA = "TTS";               // tipo di TS (Tempra/Riflessi/Volontà)
    public static final String ITEM_LABEL_ATTACCO_TIRO_SALVEZZA_CD = "TTS_CD";         // formula della CD, indipendente dal TPC
    public static final String ITEM_LABEL_ATTACCO_RANGE = "TRANGE";
    // Un attacco risolve o con TPC o con TS, mai entrambi: quale dei due è attivo.
    public static final String ITEM_LABEL_ATTACCO_TIPO_RISOLUZIONE = "ATK_TIPO"; // "TPC" | "TS"
    // Danni multipli per lo stesso attacco (es. spada fiammeggiante: base + fuoco): una riga per
    // danno, ciascuna "formula||tipo" (tipo può essere vuoto). Ordine = ordine di inserimento.
    public static final String ITEM_LABEL_ATTACCO_DANNO = "ATK_DANNO";
    public static final String ITEM_LABEL_DISABILITATO = "DISABLED";
    public static final String ITEM_LABEL_DISABILITATO_VALORE_TRUE = "1";
    public static final String ITEM_LABEL_DISABILITATO_VALORE_FALSE = "0";
    // NASCOSTO: label distinta da DISABLED. Nascondere implica anche disabilitare, ma è separata.
    public static final String ITEM_LABEL_NASCOSTO = "HIDDEN";
    public static final String ITEM_LABEL_LISTA_INCANTESIMI = "SPELL";
    public static final String ITEM_LABEL_COMPONENTE = "COMP_SP";
    public static final String ITEM_LABEL_SCUOLA_SP = "SCUOLA_SP"; // scuola/sottoscuole/descrittori incantesimo
    public static final String ITEM_LABEL_ABILITA_CLASSE = "ABCLASSE";
    public static final String ITEM_LABEL_LISTA_COMPETENZE = "COMP";
    public static final String ITEM_LABEL_LISTA_LINGUE = "LINGUE";
    public static final String ITEM_LABEL_COMPETENZA_RICHIESTA = "REQ_COMP";
    public static final String ITEM_LABEL_TAGLIA = "TAGLIA";       // SET: imposta la taglia del personaggio a un valore
    public static final String ITEM_LABEL_ADD_TAGLIA = "ADD_TAGLIA"; // ADD: incrementa/decrementa la taglia base del personaggio
    // Taglia FISICA dell'oggetto stesso (es. un'arma taglia Grande): puramente descrittiva,
    // non influisce sulla taglia del personaggio (a differenza di ITEM_LABEL_TAGLIA/ADD_TAGLIA sopra).
    public static final String ITEM_LABEL_TAGLIA_OGGETTO = "TAGLIA_OGGETTO";
    // Prefisso mostrato come chip prima del nome, nell'inventario, sugli item collegati
    // (child) di QUESTO oggetto — es. una faretra con prefisso "Freccia" mostra le sue
    // frecce come "Freccia: Freccia +1" nell'elenco oggetti.
    public static final String ITEM_LABEL_PREFISSO_OGGETTI = "PREFISSO_OGGETTI";

    // Info Veicolo: campi descrittivi (solo tipo VEICOLO).
    public static final String ITEM_LABEL_VEICOLO_VELOCITA = "VEICOLO_VELOCITA";
    public static final String ITEM_LABEL_SPELL_SLOT = "SP_SLOT";
    public static final String ITEM_LABEL_SPELL_SLOT_BONUS = "SP_SLOT_BONUS";
    public static final String COLLEGAMENTO_LABEL_LIVELLO = "LIVELLO";
    public static final String COLLEGAMENTO_LABEL_LISTA_INCANTESIMI = "SP_LIST";
    public static final String COLLEGAMENTO_LABEL_N_USATI = "USED";
    public static final String COLLEGAMENTO_LABEL_N_PREPARATI = "PREPARED";
    public static final String COLLEGAMENTO_LABEL_ALWAYS_PREPARED = "ALWAYS";
    /** Testo libero: condizione sotto cui l'item EFFETTO collegato si applica (es. "Indossato"). */
    public static final String COLLEGAMENTO_LABEL_CONDIZIONE = "CONDIZIONE";
    public static final String ITEM_LIVELLO_LVL = "LVL";
    public static final String ITEM_LIVELLO_LVL_CLASSE = "LVL_CLASSE";
    public static final String ITEM_LABEL_DADI_VITA = "DV";
    public static final String ITEM_LABEL_MALEDIZIONE = "MLDZN";
    public static final String ITEM_LABEL_PUNTI_FERITA = "PF";
    public static final String ITEM_LABEL_CLASSE = "CLASSE";
    public static final String ITEM_LABEL_GRUPPO_TRASFORMAZIONE = "GRP_TRASF";
    public static final String LABEL_PESO = "PESO"; // kg, su item_label e personaggio_label
    public static final String LABEL_QTA = "QTA";   // quantità item, moltiplica il peso
    public static final String LABEL_CAPIENZA = "CAPIENZA"; // peso max contenibile (kg), solo su CONTENITORE
    public static final String LABEL_INCLUDI_ARMI_ABILITATE        = "INCLUDI_ARMI_ABILITATE";        // flag su CONTENITORE: "1" include ARMA abilitate
    public static final String LABEL_INCLUDI_OGGETTI_ABILITATI    = "INCLUDI_OGGETTI_ABILITATI";    // flag su CONTENITORE: "1" include OGGETTO abilitati
    public static final String LABEL_INCLUDI_CONSUMABILI_ABILITATI = "INCLUDI_CONSUMABILI_ABILITATI"; // flag su CONTENITORE: "1" include CONSUMABILE abilitati
    // flag su CONTENITORE ("Tutto quello che pesa"): "1" include QUALSIASI item non disabilitato con peso,
    // di qualunque tipo (anche FRUTTO/IDOLO/PATTO: per questi il flag "disabilitato" non rappresenta
    // "indossato/riposto" ma un altro stato di gioco, quindi non si distingue "equipaggiato" in senso
    // stretto — semplicemente qualsiasi cosa pesi viene considerata "tenuta lì"). Implica gli altri INCLUDI_*.
    public static final String LABEL_INCLUDI_TUTTI_ABILITATI = "INCLUDI_TUTTI_ABILITATI";
    // flag su CONTENITORE: "1" = i suoi contenuti vanno mostrati in scheda come una sezione
    // separata (es. la Stiva di una NAVE) invece che sparsi nelle liste normali per tipo.
    // Puramente di visualizzazione: non influenza calcolaPeso.
    public static final String LABEL_INVENTARIO_SEPARATO = "INVENTARIO_SEPARATO";
    public static final String LABEL_UTILIZZI = "UTILIZZI";           // max utilizzi (globale sull'item)
    public static final String LABEL_UTILIZZI_USATI = "UTILIZZI_USATI"; // utilizzi consumati (per-personaggio)
    // "1" sull'item figlio = incantesimo spurio (non da lista/catalogo) di una sezione incantatore:
    // individuale (utilizzi propri) o gruppo (un unico figlio con contatore condiviso, nomi in descrizione)
    public static final String ITEM_LABEL_SPURIO = "SPURIO";
    public static final String LABEL_CC = "CC";     // conto corrente banca: G<idPersonaggio> | P<idParty>
    public static final String ITEM_LABEL_COMPENDIO = "COMPENDIO"; // visibilità nel compendio
    public static final String ITEM_LABEL_TIPO = "TIPO";           // tipo specifico item (es. BARRIERA)
    public static final String ITEM_TIPO_BARRIERA = "BARRIERA";
    public static final String ITEM_LABEL_BARR_MAX = "BARR_MAX";   // hp massimi della barriera
    public static final String ITEM_LABEL_BARR_CONS = "BARR_CONS"; // hp consumati della barriera
    public static final String ITEM_LABEL_FRUTTO_LVL = "$V_FORMA";       // variabile "forma" del frutto (ex LVL)
    public static final String ITEM_LABEL_FORMA_MOD_LVL = "$M_P_FORMA";  // forma: imposta la variabile FORMA del frutto padre
    public static final String ITEM_LABEL_FRUTTO_MOLT = "$V_MOLTIPLICATORE";      // variabile moltiplicatore del frutto
    public static final String ITEM_LABEL_FORMA_MOD_MOLT = "$M_P_MOLTIPLICATORE"; // forma: imposta il moltiplicatore del frutto padre
    public static final String ITEM_LABEL_ADD_CLASSE_PREFIX = "ADD_CLASSE_"; // ADD_CLASSE_<n> = +1 livello (valore = idClasse)
    public static final String ITEM_LABEL_ADD_CLASSE_VALUE_SUFFIX = "_VALUE";
    // Alias italiano accettato in lettura per lo stesso suffisso sopra (facile da digitare per errore).
    public static final String ITEM_LABEL_ADD_CLASSE_VALUE_SUFFIX_ALIAS = "_VALORE";
    // ADD_CLASSE_<n>_ITEMS = "1": concede anche i Privilegi di Classe (Talenti/Abilità/Privilegi/etc,
    // dagli Avanzamento CLASSE->item) dei livelli virtuali aggiunti da questo indice <n>. Calcolo a
    // runtime, mai persistito: gli item concessi appaiono nell'inventario come se fossero
    // virtualmente collegati all'item che porta la label.
    public static final String ITEM_LABEL_ADD_CLASSE_ITEMS_SUFFIX = "_ITEMS";
    // ADD_CLASSE_<n>_BONUS = "1": concede anche BAB/Tempra/Riflessi/Volontà dei livelli virtuali
    // aggiunti da questo indice <n>, leggendo la Tabella livelli della classe (cappata al numero
    // di livelli reale della classe).
    public static final String ITEM_LABEL_ADD_CLASSE_BONUS_SUFFIX = "_BONUS";
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

    // Info Razza: campi descrittivi (solo tipo RAZZA), puramente informativi come EN_NAME/MANUALE_SP.
    public static final String ITEM_LABEL_RAZZA_TAGLIA = "RAZZA_TAGLIA";
    public static final String ITEM_LABEL_RAZZA_VELOCITA = "RAZZA_VELOCITA";
    public static final String ITEM_LABEL_RAZZA_CARATTERISTICHE = "RAZZA_CARATTERISTICHE";
    public static final String ITEM_LABEL_RAZZA_LAP = "RAZZA_LAP";
    public static final String ITEM_LABEL_RAZZA_SPAZIO = "RAZZA_SPAZIO";
    public static final String ITEM_LABEL_RAZZA_PORTATA = "RAZZA_PORTATA";
    // Razza (import bulk da dndtools.org, vedi scripts/dndtools-scraper/scrape_races.py)
    public static final String ITEM_LABEL_RAZZA_LINGUE_AUTOMATICHE = "RAZZA_LINGUE_AUTOMATICHE";
    public static final String ITEM_LABEL_RAZZA_CLASSE_PREFERITA = "RAZZA_CLASSE_PREFERITA";
    public static final String ITEM_LABEL_RAZZA_LINGUE_BONUS = "RAZZA_LINGUE_BONUS";

    // Label da import bulk di compendio (es. Talenti da dndtools.org, vedi scripts/dndtools-scraper).
    // ITEM_LABEL_MANUALE (sopra) è riusata per il manuale di provenienza.
    public static final String ITEM_LABEL_PAGE = "PAGE";               // pagina nel manuale
    public static final String ITEM_LABEL_PREREQUISITE = "PREREQUISITE";
    public static final String ITEM_LABEL_REQUIRED_FOR = "REQUIRED_FOR";
    public static final String ITEM_LABEL_BENEFIT = "BENEFIT";
    public static final String ITEM_LABEL_LINK = "LINK";               // url pagina di origine
    public static final String ITEM_LABEL_CATEGORY = "CATEGORY";       // multi-valore: più righe per lo stesso item
    public static final String ITEM_LABEL_SPECIAL = "SPECIAL";
    public static final String ITEM_LABEL_NORMAL = "NORMAL";
    public static final String ITEM_LABEL_EXTRA = "EXTRA";             // fallback per sezioni non mappate esplicitamente

    // Descrittori Oggetto (flag booleani "1"/assente, editabili su qualunque item)
    public static final String ITEM_LABEL_MAGICO = "MAGICO";
    public static final String ITEM_LABEL_PSIONICO = "PSIONICO";
    public static final String ITEM_LABEL_DIVINO = "DIVINO";
    public static final String ITEM_LABEL_LEGGENDARIO = "LEGGENDARIO";
    public static final String ITEM_LABEL_UNICO = "UNICO";
    public static final String ITEM_LABEL_COSTO = "COSTO";
    public static final String ITEM_LABEL_MATERIALE = "MATERIALE";

    // Descrittori Abilità (flag booleani, possono essere attivi insieme)
    public static final String ITEM_LABEL_DESCR_STRAORDINARIA = "DESCR_STR";
    public static final String ITEM_LABEL_DESCR_MAGICA = "DESCR_MAG";
    public static final String ITEM_LABEL_DESCR_SOPRANNATURALE = "DESCR_SOP";
    public static final String ITEM_LABEL_DESCR_NATURALE = "DESCR_NAT";
    public static final String ITEM_LABEL_DESCR_DIVINA = "DESCR_DIV";

    // QUEST: stato di completamento (solo per quest foglia, senza sotto-quest), ambito party
    // (id del Party a cui è associata, quando non è né di un personaggio né di un intero mondo)
    // e note libere (multi-valore: una riga per nota).
    public static final String ITEM_LABEL_QUEST_COMPLETATA = "QUEST_COMPLETATA";
    public static final String ITEM_LABEL_QUEST_PARTY = "QUEST_PARTY";
    // "In carico": multi-valore, righe di testo libero (es. nome personaggio/party) mostrate come
    // chip in cima all'accordion della quest/sotto-quest in scheda.
    public static final String ITEM_LABEL_QUEST_IN_CARICO = "IN_CARICO";

    // Note: generiche, disponibili su qualunque tipo di item (non solo QUEST). Ogni riga
    // ItemLabel (chiave NOTA, multi-valore) contiene un JSON {testo, visibilita} — stessa
    // convenzione di ITEM_LABEL_VISIBILITA sopra (vuoto/OWNER/MASTER) ma per-nota anziché
    // per l'intero item.
    public static final String ITEM_LABEL_NOTA = "NOTA";

    public static final String LABEL_MILESTONE = "MILESTONE";
    public static final String LABEL_MILESTONE_TO = "MILESTONE_TO";
    public static final String LABEL_NOTIZIA_DATA_INIZIO = "DATA_INIZIO";
    public static final String LABEL_NOTIZIA_DATA_FINE = "DATA_FINE";
    public static final String LABEL_NOTIZIA_ABILITATA = "ABILITATA";

    // Livello "atteso" del personaggio: solo indicativo, non influenza i calcoli
    public static final String LABEL_LIVELLO = "LIVELLO";

    // Gradi Divini del personaggio: solo indicativo, non influenza i calcoli
    public static final String LABEL_GRADI_DIVINI = "GRADI_DIVINI";

    // Peso effettivo (kg) calcolato all'apertura della scheda e messo in cache come personaggio_label,
    // letto dalla lista party senza rifare il calcolo. Non modificabile a mano.
    public static final String LABEL_PESO_EFFETTIVO = "PESO_EFFETTIVO";

    // Tipo di personaggio (PG/NPC/NAVE/BANCA/STELLA/BASE): personaggio_label, PG = nessuna label.
    // Già usata da PartyService con una propria costante locale identica; qui serve a
    // PersonaggioService per esporla nella scheda (nascondere campi anagrafici sulle NAVE).
    public static final String LABEL_TIPO_PERSONAGGIO = "TIPO_PERSONAGGIO";

    // Portata (kg trasportabili) del personaggio: rilevante soprattutto per le NAVE (Barche).
    public static final String LABEL_PORTATA = "PORTATA";

    // Lunghezza/Larghezza (m) del personaggio: solo per le NAVE (Barche).
    public static final String LABEL_LUNGHEZZA = "LUNGHEZZA";
    public static final String LABEL_LARGHEZZA = "LARGHEZZA";

    public static final String MODIFICATORE_TEMP = "Temporaneo";

    // Info anagrafiche del personaggio (personaggio_label)
    public static final List<String> PERSONAGGIO_INFO_LABELS = List.of(
            "LUOGO_NASCITA", "DATA_NASCITA", "RAZZA", "SESSO", "PELLE", "ETA",
            "ALTEZZA", "PESO", "CAPELLI", "OCCHI", "ALLINEAMENTO", "TAGLIA",
            "MILESTONE", "MILESTONE_TO", "LIVELLO", "GRADI_DIVINI", "PORTATA",
            "LUNGHEZZA", "LARGHEZZA");
}

