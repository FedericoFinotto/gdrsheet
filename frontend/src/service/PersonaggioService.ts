import api from './api';
import {ItemDB} from "../models/ItemDB";
import {AxiosResponse} from "axios";
import {Statistiche} from "../models/Modificatori";
import {CalcoloRequest, CalcoloResponse} from "../models/Calcolo";

export const getPersonaggioById = (id) => {
    return api.get(`/personaggi/${id}`);
};

export const getModificatoriPersonaggioById = (id) => {
    return api.get(`/personaggi/modificatori/${id}`);
};

export const getAllPersonaggioItemsDTOByIdPersonaggio = (id) => {
    return api.get(`/personaggi/items/${id}`);
};

export function getItem(id: number): Promise<AxiosResponse<ItemDB>> {
    return api
        .get<ItemDB>(`/item/${id}`);
}

export function calcolaFormula(formula: string, datiPersonaggio: Statistiche): Promise<AxiosResponse<CalcoloResponse>> {
    const payload: CalcoloRequest = {
        formula,
        datiPersonaggio
    };
    console.log('CHIAMATA calcolo formula', payload);
    return api
        .post<CalcoloResponse>('/calcolo/formula', payload)
}

