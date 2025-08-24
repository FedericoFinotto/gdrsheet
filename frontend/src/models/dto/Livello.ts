import {Item} from "./Item";

export interface Livello extends Item {
    livello: number;
    classe: string;
    maledizione: string;
    livelliClasse: number[];
}