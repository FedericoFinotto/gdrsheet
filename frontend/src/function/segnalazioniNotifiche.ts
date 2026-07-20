import {Segnalazione} from '../models/dto/Segnalazione'

// Nessuna tabella lato backend per questo: tracciamo "ultima volta vista" per segnalazione
// in localStorage (per dispositivo) e confrontiamo con dataModifica di Taiga per contare
// quelle con variazioni non ancora viste dall'utente (nuovi commenti, cambio stato, ecc.).
const STORAGE_KEY = 'seg_ultima_vista'

function leggiMappa(): Record<number, string> {
    try {
        return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}')
    } catch {
        return {}
    }
}

function scriviMappa(mappa: Record<number, string>): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(mappa))
}

/** Marca una segnalazione come vista ora (o alla sua dataModifica, se nota). */
export function segnaVista(id: number, dataModifica?: string | null): void {
    const mappa = leggiMappa()
    mappa[id] = dataModifica ?? new Date().toISOString()
    scriviMappa(mappa)
}

function nonVista(s: Segnalazione, mappa: Record<number, string>): boolean {
    if (!s.dataModifica) return false
    const ultimaVista = mappa[s.id]
    if (!ultimaVista) return true
    return new Date(s.dataModifica).getTime() > new Date(ultimaVista).getTime()
}

/** Quante segnalazioni (tra quelle passate, tipicamente le "mie") hanno variazioni non ancora viste. */
export function contaNonLette(segnalazioni: Segnalazione[]): number {
    const mappa = leggiMappa()
    return segnalazioni.filter(s => nonVista(s, mappa)).length
}
