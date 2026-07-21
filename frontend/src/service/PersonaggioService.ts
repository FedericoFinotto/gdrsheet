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
import {Banca, Page, Soldi} from "../models/dto/Party";
import {Stat} from "../models/entity/Stat";

export function getModificatoriPersonaggioById(id: number): Promise<AxiosResponse<DatiPersonaggio>> {
    return api.get(`/personaggi/modificatori/${id}`);
}

export function updatePersonaggioInfo(id: number, nome: string, info: Record<string, string>): Promise<AxiosResponse<DatiPersonaggio>> {
    return api.post<DatiPersonaggio>(`/personaggi/${id}/info`, {nome, info});
}

export function getPreferito(id: number): Promise<AxiosResponse<boolean>> {
    return api.get<boolean>(`/personaggi/${id}/preferito`);
}

export function setPreferito(id: number, preferito: boolean): Promise<AxiosResponse<void>> {
    return api.put<void>(`/personaggi/${id}/preferito`, {preferito});
}

export function getAllPersonaggioItemsDTOByIdPersonaggio(id: number): Promise<AxiosResponse<Items>> {
    return api.get(`/personaggi/items/${id}`);
}

export function getAllIncantesimiByClasseAndLivello(idClasse: number, livello: number, spellList?: string): Promise<AxiosResponse<SpellBookIncantesimo>> {
    return api.get(`/item/incantesimi/${idClasse}/${livello}`, {params: spellList ? {spellList} : undefined});
}

export function getItem(id: number): Promise<AxiosResponse<ItemDB>> {
    return api
        .get<ItemDB>(`/item/${id}`);
}

export function getItemParents(id: number): Promise<AxiosResponse<Item[]>> {
    return api
        .get<Item[]>(`/item/${id}/parents`);
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

export function getItemDisabled(id: number, personaggio?: number): Promise<AxiosResponse<boolean>> {
    return api
        .get<boolean>(`/item/${id}/disabled`, {params: {personaggio}});
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

export function updateStatValue(payload: {idPersonaggio: number; idStat: string; valore: string; formula: string | null; modStatId: string | null}) {
    return api.post('/personaggi/stat/update', payload);
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

export function getListaClassiPerPersonaggio(id: number, q?: string): Promise<AxiosResponse<Item[]>> {
    return api.get(`/personaggi/classi-associabili/${id}`, {params: q ? {q} : {}});
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

// Salva SOLO i ranghi di un livello: il backend sostituisce i modificatori RANK
// senza toccare labels/caratteristiche/contenuti. Usato dalla pagina "Gestisci gradi".
export function saveRanghiLivello(
    livelloId: number,
    personaggioId: number,
    ranghi: Array<{ abilitaId: string; punti: number }>
): Promise<AxiosResponse<ItemDB>> {
    return api.post<ItemDB>(`/item/editranghi/${livelloId}`, {livelloId, personaggioId, ranghi});
}

// Salva i ranghi di più livelli in un'unica transazione (un solo persist).
export function saveRanghiBulk(
    personaggioId: number,
    livelli: Array<{ livelloId: number; ranghi: Array<{ abilitaId: string; punti: number }> }>
): Promise<AxiosResponse<void>> {
    return api.post<void>('/item/editranghi-bulk', {personaggioId, livelli});
}

export function createItem(payload: UpdateItemRequest): Promise<AxiosResponse<ItemDB>> {
    return api.post<ItemDB>('/item/create', payload);
}

export function updateItem(id: number, payload: UpdateItemRequest, idPersonaggio?: number): Promise<AxiosResponse<ItemDB>> {
    return api.post<ItemDB>(`/item/edit/${id}`, payload, {params: {idPersonaggio}});
}

export function deleteItem(id: number, idPersonaggio?: number): Promise<AxiosResponse<void>> {
    return api.delete<void>(`/item/${id}`, {params: {idPersonaggio}});
}

export function unlinkItem(id: number, idPersonaggio: number): Promise<AxiosResponse<void>> {
    return api.post<void>(`/item/unlink/${id}`, null, {params: {idPersonaggio}});
}

export function linkItem(id: number, idPersonaggio: number): Promise<AxiosResponse<void>> {
    return api.post<void>(`/item/link/${id}`, null, {params: {idPersonaggio}});
}

export function updateBarriera(id: number, consumato: number, idPersonaggio?: number): Promise<AxiosResponse<void>> {
    return api.post<void>(`/item/barriera/${id}`, null, {params: {consumato, idPersonaggio}});
}

export function searchItems(q: string, tipo?: string): Promise<AxiosResponse<Item[]>> {
    return api.get<Item[]>('/item/search', {params: {q, tipo}});
}

export function getCompendio(
    params: { nome?: string; tipo?: string; idMondo?: number | null; page?: number; size?: number } = {}
): Promise<AxiosResponse<Page<Item>>> {
    return api.get<Page<Item>>('/item/compendio', {params});
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

export function getContiPersonaggio(idPersonaggio: number): Promise<AxiosResponse<Banca[]>> {
    return api.get<Banca[]>(`/personaggi/${idPersonaggio}/conti`);
}

export function setUtilizziUsati(itemId: number, personaggioId: number, usati: number): Promise<AxiosResponse<void>> {
    return api.put<void>(`/item/${itemId}/utilizzi/${personaggioId}`, {usati});
}

export function resetUtilizzi(personaggioId: number): Promise<AxiosResponse<void>> {
    return api.delete<void>(`/item/utilizzi/${personaggioId}`);
}