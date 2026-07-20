import {Segnalazione} from '../models/dto/Segnalazione'
import {listaViste, segnaVistaServer} from '../service/SegnalazioniService'

// "Ultima volta vista" per segnalazione: persistito lato server (una riga per utente in
// cache_entry, vedi SegnalazioneVisteService), non più in localStorage — così vale per
// l'utente a prescindere dal dispositivo/browser usato. Cache in memoria lato client per
// evitare una chiamata di rete a ogni controllo.
let mappa: Record<number, string> | null = null
let caricamento: Promise<Record<number, string>> | null = null

async function assicuraCaricata(): Promise<Record<number, string>> {
    if (mappa) return mappa
    if (!caricamento) {
        caricamento = listaViste()
            .then(res => {
                mappa = {}
                for (const [id, ts] of Object.entries(res.data ?? {})) mappa[Number(id)] = ts
                return mappa
            })
            .catch(e => {
                console.error('Errore caricamento viste segnalazioni:', e)
                mappa = {}
                return mappa
            })
    }
    return caricamento
}

/** Da chiamare all'avvio della pagina/prima di leggere lo stato di visto, per assicurarsi che la mappa sia pronta. */
export async function caricaViste(): Promise<void> {
    await assicuraCaricata()
}

/** Ultima volta vista di una segnalazione, o null se non è mai stata vista. Richiede che caricaViste() sia già stato chiamato. */
export function ultimaVistaDi(id: number): string | null {
    return mappa?.[id] ?? null
}

/**
 * Marca una segnalazione come vista ORA (orario del client, non dataModifica di Taiga: quel
 * valore è quello caricato in memoria all'apertura della pagina e non si aggiorna più, quindi
 * se qualcosa cambia dopo la soglia resterebbe bloccata sul vecchio valore per sempre nella
 * sessione corrente, e il contenuto risulterebbe "nuovo" a ogni apri/chiudi).
 */
export async function segnaVista(id: number): Promise<void> {
    await assicuraCaricata()
    mappa![id] = new Date().toISOString()
    segnaVistaServer(id).catch(e => console.error('Errore salvataggio vista segnalazione:', e))
}

function nonVista(s: Segnalazione, m: Record<number, string>): boolean {
    if (!s.dataModifica) return false
    const ultimaVista = m[s.id]
    if (!ultimaVista) return true
    return new Date(s.dataModifica).getTime() > new Date(ultimaVista).getTime()
}

/** True se la segnalazione ha variazioni non ancora viste dall'utente. Richiede che caricaViste() sia già stato chiamato. */
export function segnalazioneNonVista(s: Segnalazione): boolean {
    return nonVista(s, mappa ?? {})
}

/** Quante segnalazioni (tra quelle passate, tipicamente le "mie") hanno variazioni non ancora viste. */
export async function contaNonLette(segnalazioni: Segnalazione[]): Promise<number> {
    const m = await assicuraCaricata()
    return segnalazioni.filter(s => nonVista(s, m)).length
}
