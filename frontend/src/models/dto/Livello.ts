import {Item} from "./Item";

export interface Livello extends Item {
    livello: number;
    classe: string;
    classeId: number | null;
    maledizione: string;
    livelliClasse: number[];
    gradi: number | null;
    // Modificatore di Intelligenza che, nella formula RANK/RANK_1 della classe, produrrebbe
    // esattamente "gradi" (reverse-solve lato backend): null se non determinabile.
    intModEquivalente: number | null;
}