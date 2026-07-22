import api from './api'
import {AxiosResponse} from 'axios'
import {Allegato, Comento, Segnalazione} from '../models/dto/Segnalazione'

export function listaSegnalazioni(all: boolean = false, archiviate: boolean = false): Promise<AxiosResponse<Segnalazione[]>> {
    return api.get<Segnalazione[]>('/segnalazioni', {params: {all, archiviate}})
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

export function modificaSegnalazione(id: number, titolo: string, descrizione: string): Promise<AxiosResponse<Segnalazione>> {
    return api.patch<Segnalazione>(`/segnalazioni/${id}`, {titolo, descrizione})
}

export function listaAllegati(id: number): Promise<AxiosResponse<Allegato[]>> {
    return api.get<Allegato[]>(`/segnalazioni/${id}/allegati`)
}

export function scaricaAllegato(id: number, allegatoId: number): Promise<AxiosResponse<Blob>> {
    return api.get<Blob>(`/segnalazioni/${id}/allegati/${allegatoId}/contenuto`, {responseType: 'blob'})
}

// Immagine incorporata nel testo di un commento (sintassi markdown ![](url) di Taiga): il token
// firmato nell'url incorporato nel testo scade/ruota nel tempo, quindi il backend lo risolve a un
// url fresco cercando tra gli allegati della segnalazione (da qui serve anche l'id).
export function scaricaImmagineCommento(idSegnalazione: number, url: string): Promise<AxiosResponse<Blob>> {
    return api.get<Blob>(`/segnalazioni/${idSegnalazione}/immagine-commento`, {params: {url}, responseType: 'blob'})
}

export function listaViste(): Promise<AxiosResponse<Record<number, string>>> {
    return api.get<Record<number, string>>('/segnalazioni/viste')
}

export function segnaVistaServer(id: number): Promise<AxiosResponse<void>> {
    return api.put<void>(`/segnalazioni/${id}/vista`)
}
