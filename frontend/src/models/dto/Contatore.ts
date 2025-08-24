import {Modificatore} from "./Modificatore";

export interface Contatore {
    id: string;
    nome: string;
    valore: number;
    max: number;
    modificatori: Modificatore[];
}