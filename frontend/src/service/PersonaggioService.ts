import api from './api';
import {ItemDB} from "../models/ItemDB";
import {AxiosResponse} from "axios";
import {Statistiche} from "../models/Modificatori";
import {
    CalcoloRequest,
    CalcoloResponse,
    UpdateContatoreRequest,
    UpdateHPRequest,
    UpdatePreparedPayload
} from "../models/Calcolo";

export const getPersonaggioById = (id) => {
    return api.get(`/personaggi/${id}`);
};

export const getModificatoriPersonaggioById = (id) => {
    return api.get(`/personaggi/modificatori/${id}`);
};

export const getAllPersonaggioItemsDTOByIdPersonaggio = (id) => {
    return api.get(`/personaggi/items/${id}`);
};

export const getAllIncantesimiByClasseAndLivello = (idClasse: number, livello: number) => {
    return api.get(`/item/incantesimi/${idClasse}/${livello}`);
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

export function switchItemState(id: number): Promise<AxiosResponse<ItemDB>> {
    return api
        .get<ItemDB>(`/item/switch-state/${id}`);
}

export function updateHP(idPersonaggio: number, pf: string, pfTemp: string): Promise<AxiosResponse<Boolean>> {
    const payload: UpdateHPRequest = {
        idPersonaggio,
        pf,
        pfTemp
    };
    return api
        .post<Boolean>('/personaggi/update-hp', payload)
}

export function updateContatore(idPersonaggio: number, statId: string, valore: string): Promise<AxiosResponse<Boolean>> {
    const payload: UpdateContatoreRequest = {
        idPersonaggio,
        id: statId,
        valore
    };
    return api
        .post<Boolean>('/personaggi/update-counter', payload)
}

export function updatePreparedSpells(payload: UpdatePreparedPayload) {
    return api.post('/item/incantesimi/prepara', payload);
}

export function updateSpellUsage(payload) {
    return api.post('/item/incantesimi/update-spellusage', payload);
}