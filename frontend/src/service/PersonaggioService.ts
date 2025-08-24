import api from './api';
import {ItemDB} from "../models/entity/ItemDB";
import {AxiosResponse} from "axios";
import {CalcoloRequest} from "../models/dto/CalcoloRequest";
import {CalcoloResponse} from "../models/dto/CalcoloResponse";
import {UpdateHPRequest} from "../models/dto/UpdateHPRequest";
import {UpdateCounterRequest} from "../models/dto/UpdateCounterRequest";
import {UpdatePreparedRequest} from "../models/dto/UpdatePreparedRequest";
import {DatiPersonaggio} from "../models/dto/DatiPersonaggio";
import {AbilitaClasse} from "../models/dto/AbilitaClasse";
import {Items} from "../models/dto/Items";
import {SpellBookIncantesimo} from "../models/dto/SpellBook";
import {UpdateSpellRequest} from "../models/dto/UpdateSpellRequest";
import {UpdateBaseStatValueRequest} from "../models/dto/UpdateBaseStatValueRequest";
import {UpdateSpellUsageRequest} from "../models/dto/UpdateSpellUsageRequest";
import {Abilita} from "../models/dto/Abilita";
import {Item} from "../models/dto/Item";
import {Gradi} from "../models/dto/Gradi";

export function getModificatoriPersonaggioById(id: number): Promise<AxiosResponse<DatiPersonaggio>> {
    return api.get(`/personaggi/modificatori/${id}`);
}

export function getAllPersonaggioItemsDTOByIdPersonaggio(id: number): Promise<AxiosResponse<Items>> {
    return api.get(`/personaggi/items/${id}`);
}

export function getAllIncantesimiByClasseAndLivello(idClasse: number, livello: number): Promise<AxiosResponse<SpellBookIncantesimo>> {
    return api.get(`/item/incantesimi/${idClasse}/${livello}`);
}

export function getItem(id: number): Promise<AxiosResponse<ItemDB>> {
    return api
        .get<ItemDB>(`/item/${id}`);
}

export function calcolaFormula(formula: string, datiPersonaggio: DatiPersonaggio): Promise<AxiosResponse<CalcoloResponse>> {
    const payload: CalcoloRequest = {
        formula,
        datiPersonaggio
    };
    console.log('CHIAMATA calcolo formula', payload);
    return api
        .post<CalcoloResponse>('/calcolo/formula', payload)
}

export function switchItemState(id: number, idPersonaggio: number): Promise<AxiosResponse<ItemDB>> {
    return api
        .get<ItemDB>(`/item/switch-state/${idPersonaggio}/${id}`);
}

export function updateHP(idPersonaggio: number, pf: number, pfTemp: number): Promise<AxiosResponse<boolean>> {
    const payload: UpdateHPRequest = {
        idPersonaggio,
        pf: String(pf),
        pfTemp: String(pfTemp),
    };
    return api.post<boolean>('/personaggi/update-hp', payload);
}

export function updateContatore(idPersonaggio: number, statId: string, valore: string): Promise<AxiosResponse<Boolean>> {
    const payload: UpdateCounterRequest = {
        idPersonaggio,
        id: statId,
        valore
    };
    return api
        .post<Boolean>('/personaggi/update-counter', payload)
}

export function updatePreparedSpells(payload: UpdatePreparedRequest) {
    return api.post('/item/incantesimi/prepara', payload);
}

export function updateSpellUsage(payload: UpdateSpellUsageRequest) {
    return api.post('/item/incantesimi/update-spellusage', payload);
}

export function updateTemporaryModifier(payload: UpdateBaseStatValueRequest) {
    return api.post('/personaggi/stat/update-base', payload);
}

export function saveSpell(id: number, payload: UpdateSpellRequest): Promise<AxiosResponse<ItemDB>> {
    return api.post(`/item/editspell/${id}`, payload);
}

export function getIdPersonaggioFromLivello(id: number): Promise<AxiosResponse<number>> {
    return api.get(`/personaggi/id-personaggio-da-livello/${id}`);
}

export function getListaAbilitaPerPersonaggio(id: number): Promise<AxiosResponse<Abilita[]>> {
    return api.get(`/personaggi/stats/${id}`);
}

export function getListaClassiPerPersonaggio(id: number): Promise<AxiosResponse<Item[]>> {
    return api.get(`/personaggi/classi-associabili/${id}`);
}

export function getListaMaledizioniPerPersonaggio(id: number): Promise<AxiosResponse<Item[]>> {
    return api.get(`/personaggi/maledizioni-associabili/${id}`);
}

export function getAbilitaClasseByPersonaggioLivelloClasse(idPersonaggio: number, livello: number, idClasse: number): Promise<AxiosResponse<AbilitaClasse[]>> {
    return api.get(`/personaggi/abilita-classe/${idPersonaggio}/${livello}/${idClasse}`);
}

export function getGradiClasseByPersonaggioLivelloClasse(idPersonaggio: number, livello: number, idClasse: number, livelli: string): Promise<AxiosResponse<Gradi>> {
    return api.get(`/personaggi/gradi-livello/${idPersonaggio}/${livello}/${livelli}/${idClasse}`);
}

export function saveLivello(payload) {
    console.log(payload);
    return null;
}