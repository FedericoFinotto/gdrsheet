import api from './api'
import {AxiosResponse} from 'axios'
import {PartyDetail, PartyItem} from '../models/dto/Party'

export function getParty(id: number): Promise<AxiosResponse<PartyDetail>> {
    return api.get<PartyDetail>(`/party/${id}`)
}

export function getPartyItems(id: number): Promise<AxiosResponse<PartyItem[]>> {
    return api.get<PartyItem[]>(`/party/${id}/items`)
}

export function giveItem(itemId: number, fromPersonaggioId: number, toPersonaggioId: number): Promise<AxiosResponse<void>> {
    return api.post<void>('/party/give', {itemId, fromPersonaggioId, toPersonaggioId})
}
