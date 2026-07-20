import api from './api'
import {AxiosResponse} from 'axios'

export interface NotiziaDTO {
    id: number
    nome: string
    descrizione?: string
    dataInizio?: string
    dataFine?: string
    archiviata: boolean
}

export function getNotizie(): Promise<AxiosResponse<NotiziaDTO[]>> {
    return api.get<NotiziaDTO[]>('/item/notizie')
}

export function getViste(): Promise<AxiosResponse<Record<number, string>>> {
    return api.get<Record<number, string>>('/item/notizie/viste')
}

export function segnaViste(ids: number[]): Promise<AxiosResponse<void>> {
    return api.put<void>('/item/notizie/viste', ids)
}
