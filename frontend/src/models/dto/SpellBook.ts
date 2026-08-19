import {Item} from "./Item";

/** Incantesimo presente nello spellbook, con info di preparazione/uso */
export interface SpellBookIncantesimo extends Item {
    cd: string;            // CD come stringa (es. "17" o formula)
    livello: number;       // livello incantesimo 0..9
    idClasse: number;      // id classe
    classe: string;        // codice/etichetta classe (es. "DRUIDO")
    spellList: string;     // codice lista (es. "SP_DRUID")
    nPrepared: number;     // numero preparati
    nUsed: number;         // numero usati
    alwaysPrep: boolean;   // sempre preparato
    componenti: string[]; // componenti richieste
    tempo?: string;        // TEMPO_SP grezzo: per l'icona azione nella riga (vedi SpellBook.mostraSimboliAzioni)
}

/** Raggruppa gli incantesimi per livello con slot e bonus */
export interface SpellBookLivello {
    livello: number;                         // 0..9
    slot: number;                            // slot disponibili per il livello
    conosciuti?: number;                     // incantesimi conosciuti per il livello, se la sezione li traccia separatamente
    // true = la sezione traccia gli slot usati a questo livello con un contatore dedicato
    // (SPELL_<n>_SLOT_CONTATORE): mostrare uno stepper usati/slot invece del solo numero statico.
    slotConContatore?: boolean;
    slotUsati?: number;                      // slot già usati a questo livello, se slotConContatore
    bonus: string[];                         // note/bonus (es. domini, oggetti)
    incantesimi: SpellBookIncantesimo[];  // incantesimi del livello
}

/** Spellbook di una classe (es. Druido) con i livelli */
export interface SpellBook {
    idClasse: number;
    nomeClasse: string;
    fonteTipo?: string;                // TipoItem della fonte (CLASSE, OGGETTO, ...)
    spellList: string;                // codice lista (es. "SP_DRUID")
    casterLevel?: number;              // classi: livello effettivo (+prestigio); oggetti: valore fisso da label
    caratteristica?: string;           // stat id usata per la CD (es. "INT")
    cd?: number;                       // 10 + casterLevel + modificatore caratteristica
    mostraSimboliAzioni?: boolean;     // Mondo.mostraSimboliAzioni della fonte (icona invece di testo)
    // Mondo.mostraCasterLevel della fonte: se false, non mostrare "CL: X" (il CL resta comunque
    // usato per calcolare CD/slot/mana). Assente = true (comportamento storico), non solo false.
    mostraCasterLevel?: boolean;
    // Indice "n" della sezione (SPELL_<n>...) sulla fonte (idClasse): serve per aggiornare il
    // contatore slot usati di un livello (setSlotUsatiPerLivello). Assente sulle sezioni legacy.
    sezioneIndice?: number;
    // Sezione con "classe di riferimento" (solo oggetti): niente pool di slot separato, "slot" è
    // già il numero di incantesimi conosciuti/disponibili a quel livello — non mostrare "Slot: X",
    // solo "preparati/disponibili".
    soloConosciuti?: boolean;
    // Sistema incantesimi del mondo (Mondo.sistemaIncantesimi): "SLOT" (default) o "MANA". Se
    // MANA, formulaManaTotale è la formula del pool (valutata come le formule bonus slot, stesse
    // variabili) e manaUsati il consumo già tracciato per questa sezione (un solo contatore).
    sistemaIncantesimi?: string;
    formulaManaTotale?: string;
    manaUsati?: number;
    livelli: SpellBookLivello[];   // inizializzato a []
    spurii?: SpellBookIncantesimo[];   // incantesimi non da lista/catalogo, utilizzi propri o di gruppo
}




