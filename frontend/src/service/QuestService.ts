import api from './api';
import {AxiosResponse} from 'axios';
import {Nota, Quest} from '../models/dto/Quest';

// Parte pesante di una quest (rich text), esclusa dall'albero e scaricata all'apertura del nodo.
export interface QuestDettaglio {
    id: number;
    descrizione: string | null;
    note: Nota[];
}

// Voce della ricerca "collega una quest esistente": solo quest non già figlie di un'altra.
export interface QuestScelta {
    id: number;
    nome: string;
    ambito: string | null;
}

// archiviate: false (default) = solo le non archiviate; true = SOLO le archiviate (mai entrambe).
export function getQuestPersonaggio(idPersonaggio: number, archiviate = false): Promise<AxiosResponse<Quest[]>> {
    return api.get<Quest[]>(`/quest/personaggio/${idPersonaggio}`, {params: {archiviate}});
}

export function getQuestParty(idParty: number, archiviate = false): Promise<AxiosResponse<Quest[]>> {
    return api.get<Quest[]>(`/quest/party/${idParty}`, {params: {archiviate}});
}

export function getQuestDettaglio(idQuest: number): Promise<AxiosResponse<QuestDettaglio>> {
    return api.get<QuestDettaglio>(`/quest/${idQuest}/dettaglio`);
}

// Albero completo (radice + sotto-quest) a cui appartiene una quest qualunque, a qualunque
// profondità: usato dalla ricerca profonda per mostrare, cliccando su un risultato QUEST, lo
// stesso componente ad albero della pagina Quest invece del dettaglio item generico.
export function getQuestAlbero(idQuest: number): Promise<AxiosResponse<Quest>> {
    return api.get<Quest>(`/quest/${idQuest}/albero`);
}

export function searchQuestRadici(q: string, excludeId?: number): Promise<AxiosResponse<QuestScelta[]>> {
    return api.get<QuestScelta[]>('/quest/search-radici', {params: {q, excludeId}});
}

export function toggleQuestCompletata(idQuest: number): Promise<AxiosResponse<void>> {
    return api.post<void>(`/quest/${idQuest}/toggle`);
}

// Sostituisce integralmente le righe "In carico" di una quest (modifica rapida, senza editor).
export function setQuestInCarico(idQuest: number, valori: string[]): Promise<AxiosResponse<void>> {
    return api.put<void>(`/quest/${idQuest}/in-carico`, valori);
}

// Elimina la quest per TUTTI i giocatori che la vedono, con in cascata tutte le sotto-quest e i
// dati collegati (label, collegamenti, modificatori...). Solo master e admin.
export function deleteQuest(idQuest: number): Promise<AxiosResponse<void>> {
    return api.delete<void>(`/quest/${idQuest}`);
}
