import api from './api'
import {AxiosResponse} from 'axios'
import {Page, PartyDetail, PartyItem} from '../models/dto/Party'

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
