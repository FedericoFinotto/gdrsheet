// Ogni elemento di `abilita`
import {Caratteristica} from "./Caratteristica";

import {Modificatore} from "./Modificatore";
import {Rank} from "./Rank";

export interface Abilita {
    abilita: ABILITA;
    rank: RANK;
    base: Caratteristica | null;
    show: boolean;
}

export interface ABILITA {
    id: string;
    nome: string;
    modificatore: number;
    modificatori: Modificatore[];
    addestramento: boolean;
}

export interface RANK {
    valore: number;
    modificatore: number;
    ranks: Rank[];
}