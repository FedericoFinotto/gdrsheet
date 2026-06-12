import api from './api'
import {AxiosResponse} from 'axios'
import {Banca, BancaDetail, Conto, Page, PartyDetail, PartyItem, Soldi} from '../models/dto/Party'

export function getParty(id: number): Promise<AxiosResponse<PartyDetail>> {
    return api.get<PartyDetail>(`/party/${id}`)
}

export function getPartyItems(
    id: number,
    params: { nome?: string; tipo?: string; page?: number; size?: number } = {}
): Promise<AxiosResponse<Page<PartyItem>>> {
    return api.get<Page<PartyItem>>(`/party/${id}/items`, {params})
}

export function giveItem(itemId: number, fromPersonaggioId: number, toPersonaggioId: number): Promise<AxiosResponse<void>> {
    return api.post<void>('/party/give', {itemId, fromPersonaggioId, toPersonaggioId})
}

export function getBanche(partyId: number): Promise<AxiosResponse<Banca[]>> {
    return api.get<Banca[]>(`/party/${partyId}/banche`)
}

export function apriConto(bancaId: number, cc: string): Promise<AxiosResponse<Conto>> {
    return api.post<Conto>(`/party/banca/${bancaId}/conto`, null, {params: {cc}})
}

export function updateConto(itemId: number, soldi: Soldi): Promise<AxiosResponse<Soldi>> {
    return api.post<Soldi>(`/party/banca/conto/${itemId}`, soldi)
}

export function getBancaDetail(bancaId: number): Promise<AxiosResponse<BancaDetail>> {
    return api.get<BancaDetail>(`/party/banca/${bancaId}/dettaglio`)
}

export interface Mondo {
    id: number;
    nome: string;
}

export function getMieiMondi(): Promise<AxiosResponse<Mondo[]>> {
    return api.get<Mondo[]>('/party/mondi')
}

export function createParty(nome: string, mondoId: number): Promise<AxiosResponse<number>> {
    return api.post<number>('/party', {nome, mondoId})
}

export const TIPI_PERSONAGGIO = [
    {value: 'PG', label: 'Personaggio (PG)'},
    {value: 'NPC', label: 'NPC'},
    {value: 'BARCA', label: 'Barca'},
    {value: 'BANCA', label: 'Banca'},
    {value: 'STELLA', label: 'Stella'},
] as const

export function createPersonaggio(
    partyId: number, nome: string, tipo: string, proprietarioUtenteId?: number
): Promise<AxiosResponse<number>> {
    return api.post<number>('/party/personaggio', {partyId, nome, tipo, proprietarioUtenteId})
}

export function deleteParty(partyId: number): Promise<AxiosResponse<void>> {
    return api.delete<void>(`/party/${partyId}`)
}

export interface MembroParty {
    utenteId: number;
    username: string;
    name: string;
    ruolo: 'MASTER' | 'GIOCATORE';
}

export function getMembri(partyId: number): Promise<AxiosResponse<MembroParty[]>> {
    return api.get<MembroParty[]>(`/party/${partyId}/membri`)
}

export function addMembro(partyId: number, username: string, ruolo: string): Promise<AxiosResponse<MembroParty>> {
    return api.post<MembroParty>(`/party/${partyId}/membro`, {username, ruolo})
}
