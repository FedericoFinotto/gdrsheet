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
import {UpdateItemRequest} from "../models/dto/UpdateItemRequest";
import {SaveLivelloPayload} from "../models/dto/UpdateLivelloRequest";
import {Soldi} from "../models/dto/Party";
import {Stat} from "../models/entity/Stat";

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

export function saveLivello(payload: SaveLivelloPayload): Promise<AxiosResponse<ItemDB>> {
    return api.post<ItemDB>(`/item/editlivello/${payload.livelloId}`, payload);
}

export function createItem(payload: UpdateItemRequest): Promise<AxiosResponse<ItemDB>> {
    return api.post<ItemDB>('/item/create', payload);
}

export function updateItem(id: number, payload: UpdateItemRequest): Promise<AxiosResponse<ItemDB>> {
    return api.post<ItemDB>(`/item/edit/${id}`, payload);
}

export function deleteItem(id: number, idPersonaggio?: number): Promise<AxiosResponse<void>> {
    return api.delete<void>(`/item/${id}`, {params: {idPersonaggio}});
}

export function unlinkItem(id: number, idPersonaggio: number): Promise<AxiosResponse<void>> {
    return api.post<void>(`/item/unlink/${id}`, null, {params: {idPersonaggio}});
}

export function searchItems(q: string, tipo?: string): Promise<AxiosResponse<Item[]>> {
    return api.get<Item[]>('/item/search', {params: {q, tipo}});
}

let statsCache: Stat[] | null = null;

export async function getStats(): Promise<Stat[]> {
    if (statsCache) return statsCache;
    const res = await api.get<Stat[]>('/stats');
    statsCache = res.data ?? [];
    return statsCache;
}

export function getSoldi(idPersonaggio: number): Promise<AxiosResponse<Soldi>> {
    return api.get<Soldi>(`/personaggi/${idPersonaggio}/soldi`);
}

export function updateSoldi(idPersonaggio: number, soldi: Soldi): Promise<AxiosResponse<Soldi>> {
    return api.post<Soldi>(`/personaggi/${idPersonaggio}/soldi`, soldi);
}