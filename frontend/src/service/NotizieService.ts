import api from './api'
import {AxiosResponse} from 'axios'

export interface NotiziaDTO {
    id: number
    nome: string
    descrizione?: string
    dataInizio?: string
    dataFine?: string
}

export function getNotizieAttive(): Promise<AxiosResponse<NotiziaDTO[]>> {
    return api.get<NotiziaDTO[]>('/item/notizie')
}
