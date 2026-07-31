import api from './api';
import {AxiosResponse} from 'axios';

/** Immagine di un item: il file sta sull'host esterno, qui viaggia solo il riferimento. */
export interface ItemImmagine {
    id: number;
    url: string;
    titolo?: string | null;
    ordine: number;
}

/** Stato dell'integrazione: se non è configurata lato server il caricamento va nascosto. */
export interface StatoHost {
    configurato: boolean;
    host: string;
    /** se false, rimuovendo l'immagine il file resterebbe sull'host */
    cancellazioneRemota: boolean;
    maxByte: number;
}

export function getStatoHost(): Promise<AxiosResponse<StatoHost>> {
    return api.get<StatoHost>('/immagini/stato');
}

export function getImmagini(idItem: number): Promise<AxiosResponse<ItemImmagine[]>> {
    return api.get<ItemImmagine[]>(`/immagini/item/${idItem}`);
}

export function caricaImmagine(idItem: number, file: File, titolo?: string): Promise<AxiosResponse<ItemImmagine>> {
    const form = new FormData();
    form.append('file', file);
    if (titolo) form.append('titolo', titolo);
    return api.post<ItemImmagine>(`/immagini/item/${idItem}`, form, {
        headers: {'Content-Type': 'multipart/form-data'},
    });
}

/** Rimuove l'immagine dall'item e cancella il file dall'host. */
export function eliminaImmagine(idImmagine: number): Promise<AxiosResponse<void>> {
    return api.delete<void>(`/immagini/${idImmagine}`);
}

export function riordinaImmagini(idItem: number, idInOrdine: number[]): Promise<AxiosResponse<ItemImmagine[]>> {
    return api.post<ItemImmagine[]>(`/immagini/item/${idItem}/ordine`, idInOrdine);
}
