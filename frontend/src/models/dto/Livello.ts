import {Item} from "./Item";

export interface Livello extends Item {
    livello: number;
    classe: string;
    classeId: number | null;
    maledizione: string;
    livelliClasse: number[];
    gradi: number | null;
}