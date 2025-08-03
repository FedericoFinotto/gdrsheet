import {Statistiche} from "./Modificatori";

export interface CalcoloRequest {
    formula: string;
    datiPersonaggio: Statistiche;
}

export interface CalcoloResponse {
    risultato: string;
    formula: string;
}