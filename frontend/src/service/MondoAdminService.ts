import api from './api';
import {AxiosResponse} from 'axios';
import {MondoOpt} from '../function/useMondoSistema';

export interface MasterMondo {
    utenteId: number;
    username: string;
    name: string;
}

// Tutti i mondi (admin): per la UI di gestione dei permessi per mondo.
export function getMondiAdmin(): Promise<AxiosResponse<MondoOpt[]>> {
    return api.get<MondoOpt[]>('/mondo');
}

// Mondi tra cui l'utente loggato può switchare: tutti per un admin, solo quelli di cui è master
// altrimenti. Usato dallo switcher mondo nel menu (mostrato solo se ce ne sono 2+).
export function getMondiDisponibili(): Promise<AxiosResponse<MondoOpt[]>> {
    return api.get<MondoOpt[]>('/mondo/disponibili');
}

export function getMasterMondo(mondoId: number): Promise<AxiosResponse<MasterMondo[]>> {
    return api.get<MasterMondo[]>(`/mondo/${mondoId}/master`);
}

export function addMasterMondo(mondoId: number, username: string): Promise<AxiosResponse<MasterMondo>> {
    return api.post<MasterMondo>(`/mondo/${mondoId}/master`, {username});
}

export function removeMasterMondo(mondoId: number, utenteId: number): Promise<AxiosResponse<void>> {
    return api.delete<void>(`/mondo/${mondoId}/master/${utenteId}`);
}

// Sistemi (admin): lista + creazione.
export function getSistemiAdmin(): Promise<AxiosResponse<MondoOpt[]>> {
    return api.get<MondoOpt[]>('/mondo/sistemi');
}

export function creaSistema(descrizione: string): Promise<AxiosResponse<MondoOpt>> {
    return api.post<MondoOpt>('/mondo/sistemi', {descrizione});
}

// Mondi (admin): creazione (sistemaId obbligatorio) + aggiornamento (rinomina/riassegna sistema).
export function creaMondo(descrizione: string, sistemaId: number): Promise<AxiosResponse<MondoOpt>> {
    return api.post<MondoOpt>('/mondo', {descrizione, sistemaId});
}

export function aggiornaMondo(
    mondoId: number, patch: { descrizione?: string; sistemaId?: number }
): Promise<AxiosResponse<MondoOpt>> {
    return api.put<MondoOpt>(`/mondo/${mondoId}`, patch);
}
