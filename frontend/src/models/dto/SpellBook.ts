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
    bonus: string[];                         // note/bonus (es. domini, oggetti)
    incantesimi: SpellBookIncantesimo[];  // incantesimi del livello
}

/** Spellbook di una classe (es. Druido) con i livelli */
export interface SpellBook {
    idClasse: number;
    nomeClasse: string;
    spellList: string;                // codice lista (es. "SP_DRUID")
    livelli: SpellBookLivello[];   // inizializzato a []
}




