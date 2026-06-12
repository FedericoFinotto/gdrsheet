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
