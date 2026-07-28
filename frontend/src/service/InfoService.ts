import api from './api';
import {AxiosResponse} from 'axios';
import {Nota} from '../models/dto/Quest';
import {Info} from '../models/dto/Info';

// Parte pesante di un INFO (rich text), esclusa dall'albero e scaricata all'apertura del nodo.
export interface InfoDettaglio {
    id: number;
    descrizione: string | null;
    note: Nota[];
}

// archiviate: false (default) = solo i non archiviati; true = SOLO gli archiviati (mai entrambi).
export function getInfoParty(idParty: number, archiviate = false): Promise<AxiosResponse<Info[]>> {
    return api.get<Info[]>(`/info/party/${idParty}`, {params: {archiviate}});
}

export function getInfoDettaglio(idInfo: number): Promise<AxiosResponse<InfoDettaglio>> {
    return api.get<InfoDettaglio>(`/info/${idInfo}/dettaglio`);
}

// Elimina l'INFO per TUTTI i giocatori che lo vedono, con in cascata tutti i sotto-info e i dati
// collegati (label, collegamenti, modificatori...) — nessuna restrizione a master/admin, come per le quest.
export function deleteInfo(idInfo: number): Promise<AxiosResponse<void>> {
    return api.delete<void>(`/info/${idInfo}`);
}
