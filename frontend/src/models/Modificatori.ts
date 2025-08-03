// Modificatore generico (usato sia in caratteristiche che in tiri salvezza, abilità, CA…)
export interface Modificatore {
    id: number | null;
    statId: string;
    valore: number;
    formula: string | null;
    nota: string | null;
    tipo: string | null;
    sempreAttivo: boolean;
    item: string | null;
    // campi opzionali usati solo in alcuni contesti:
    diClasse?: boolean;
    classe?: string | null;
}

// Caratteristica base
export interface Caratteristica {
    id: string;
    label: string;
    base: number;
    valore: number;
    modificatore: number;
    modificatori: Modificatore[];
}

// Tiro salvezza
export interface TiroSalvezza {
    id: string;
    label: string;
    idBase: string;      // es. "COS", "DES"…
    modBase: number;
    modificatore: number;
    modificatori: Modificatore[];
}

// Dati di abilità “statici”
export interface AbilitaInfo {
    id: string;
    nome: string;
    modificatore: number;
    modificatori: Modificatore[];
    addestramento: boolean;
}

// Rank entry dentro `rank.ranks`
export interface RankEntry {
    id: number;
    statId: string;
    valore: number;
    nota: string | null;
    tipo: string;            // “RANK”
    sempreAttivo: boolean;
    diClasse: boolean;
    classe: string | null;
    item: string;
    modificatore: number;
}

// Struttura di `rank`
export interface RankBundle {
    valore: number;
    modificatore: number;
    ranks: RankEntry[];
}

// Ogni elemento di `abilita`
export interface AbilitaRecord {
    abilita: AbilitaInfo;
    rank: RankBundle;
    base: Caratteristica | null;
    show: boolean;
}

// Classe armatura
export interface ClasseArmatura {
    id: string;
    nome: string;
    modificatore: number;
    modificatori: Modificatore[];
}

export interface Statistiche {
    id: number;
    nome: string;
    caratteristiche: Caratteristica[];
    tiriSalvezza: TiroSalvezza[];
    abilita: AbilitaRecord[];
    classeArmatura: ClasseArmatura[];
}
