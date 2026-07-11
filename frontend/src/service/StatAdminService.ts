import api from './api';
import {AxiosResponse} from 'axios';
import {Stat} from '../models/entity/Stat';

export interface MondoOpt {
    id: number;
    descrizione: string;
}

export interface StatDefaultRow {
    id?: number;
    mondoId: number;
    statId: string;
    statLabel?: string;
    valoreDefault?: string | null;
    defaultModId?: string | null;
    defaultModLabel?: string | null;
    addestramento?: boolean;
}

export function getStatsAll(): Promise<AxiosResponse<Stat[]>> {
    return api.get<Stat[]>('/stats');
}

export function createStat(payload: { id: string; tipo: string; label: string; rankable?: boolean }): Promise<AxiosResponse<Stat>> {
    return api.post<Stat>('/stats', payload);
}

export function getMondiAdmin(): Promise<AxiosResponse<MondoOpt[]>> {
    return api.get<MondoOpt[]>('/stats/mondi');
}

export function getStatDefaults(mondoId: number): Promise<AxiosResponse<StatDefaultRow[]>> {
    return api.get<StatDefaultRow[]>(`/stats/default/${mondoId}`);
}

export function createStatDefault(payload: StatDefaultRow): Promise<AxiosResponse<StatDefaultRow>> {
    return api.post<StatDefaultRow>('/stats/default', payload);
}

export function updateStatDefault(id: number, payload: StatDefaultRow): Promise<AxiosResponse<StatDefaultRow>> {
    return api.put<StatDefaultRow>(`/stats/default/${id}`, payload);
}

export function deleteStatDefault(id: number): Promise<AxiosResponse<void>> {
    return api.delete<void>(`/stats/default/${id}`);
}
