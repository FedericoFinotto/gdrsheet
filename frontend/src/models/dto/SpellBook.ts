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
    // Indice "n" della sezione (SPELL_<n>...) sulla fonte (idClasse): serve per aggiornare il
    // contatore slot usati di un livello (setSlotUsatiPerLivello). Assente sulle sezioni legacy.
    sezioneIndice?: number;
    livelli: SpellBookLivello[];   // inizializzato a []
    spurii?: SpellBookIncantesimo[];   // incantesimi non da lista/catalogo, utilizzi propri o di gruppo
}




