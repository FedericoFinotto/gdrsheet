import api from './api';
import {AxiosResponse} from 'axios';
import {Quest} from '../models/dto/Quest';

export function getQuestPersonaggio(idPersonaggio: number): Promise<AxiosResponse<Quest[]>> {
    return api.get<Quest[]>(`/quest/personaggio/${idPersonaggio}`);
}

export function getQuestParty(idParty: number): Promise<AxiosResponse<Quest[]>> {
    return api.get<Quest[]>(`/quest/party/${idParty}`);
}

export function toggleQuestCompletata(idQuest: number): Promise<AxiosResponse<void>> {
    return api.post<void>(`/quest/${idQuest}/toggle`);
}

// Sostituisce integralmente le righe "In carico" di una quest (modifica rapida, senza editor).
export function setQuestInCarico(idQuest: number, valori: string[]): Promise<AxiosResponse<void>> {
    return api.put<void>(`/quest/${idQuest}/in-carico`, valori);
}
