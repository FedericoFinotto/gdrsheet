import {Statistiche} from "./Modificatori";

export interface CalcoloRequest {
    formula: string;
    datiPersonaggio: Statistiche;
}

export interface CalcoloResponse {
    risultato: string;
    formula: string;
}

export interface UpdateHPRequest {
    idPersonaggio: number;
    pf: string;
    pfTemp: string;
}

export interface UpdateContatoreRequest {
    idPersonaggio: number;
    id: string;
    valore: string;
}

export type UpdatePreparedPayload = {
    idPersonaggio: number;
    idClasse: number;
    spellList: string;
    livello: number;
    prepared: Record<number, number>;
};