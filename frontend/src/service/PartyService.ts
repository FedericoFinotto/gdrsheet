import api from './api'
import {AxiosResponse} from 'axios'
import {PartyDetail} from '../models/dto/Party'

export function getParty(id: number): Promise<AxiosResponse<PartyDetail>> {
    return api.get<PartyDetail>(`/party/${id}`)
}
