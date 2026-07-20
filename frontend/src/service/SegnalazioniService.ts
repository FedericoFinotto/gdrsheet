import api from './api'
import {AxiosResponse} from 'axios'
import {Comento, Segnalazione} from '../models/dto/Segnalazione'

export function listaSegnalazioni(all: boolean = false): Promise<AxiosResponse<Segnalazione[]>> {
    return api.get<Segnalazione[]>('/segnalazioni', {params: {all}})
}

export function dettaglioSegnalazione(id: number): Promise<AxiosResponse<Segnalazione>> {
    return api.get<Segnalazione>(`/segnalazioni/${id}`)
}

export function creaSegnalazione(titolo: string, descrizione: string, screenshot: Blob | null): Promise<AxiosResponse<Segnalazione>> {
    const form = new FormData()
    form.append('titolo', titolo)
    form.append('descrizione', descrizione)
    if (screenshot) form.append('screenshot', screenshot, 'screenshot.png')
    return api.post<Segnalazione>('/segnalazioni', form, {headers: {'Content-Type': 'multipart/form-data'}})
}

export function listaCommenti(id: number): Promise<AxiosResponse<Comento[]>> {
    return api.get<Comento[]>(`/segnalazioni/${id}/commenti`)
}

export function aggiungiCommento(id: number, testo: string): Promise<AxiosResponse<Comento>> {
    return api.post<Comento>(`/segnalazioni/${id}/commenti`, {testo})
}
