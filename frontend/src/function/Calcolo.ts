import {calcolaFormula} from "../service/PersonaggioService";

import {DatiPersonaggio} from "../models/dto/DatiPersonaggio";
import {Items} from "../models/dto/Items";
import {Attacco} from "../models/dto/Attacco";
import {applicaBonusDado, risolviFormulaDanno, testoFormula, testoModificatore} from "./Utils";

// const REGEX_DADI = /^\d+d\d+([+-]\d+)?$/;
// const REGEX_NUMERO = /^[+-]?\d+$/;

export function getValoreFormula(personaggio: DatiPersonaggio, formula: string) {
    if (!formula) return null;
    // Per il calcolo di UNA formula (un singolo attacco/incantesimo) il BAB da usare è il primo
    // della sequenza di attacchi multipli. Clona bonusAttacco invece di mutare l'oggetto
    // condiviso (cache dello store): il vero "modificatore" serve intatto altrove (Lotta,
    // Mischia, Distanza), mutarlo in place lo sovrascriveva permanentemente col valore diminuito.
    const personaggioPerFormula: DatiPersonaggio = {
        ...personaggio,
        bonusAttacco: personaggio.bonusAttacco.map(x => ({...x, modificatore: x.attacchiMultipli[0]})),
    };
    return calcolaFormula(formula, personaggioPerFormula);
}

export interface DannoRisolto {
    valore: string
    tipo: string
    formula: string // formula originale "ripulita" (senza @/$), sotto-testo come per il TPC
}

export interface AttaccoCalcolatoRow {
    id: any
    nome: string
    nomeItem: string
    atk: string | null
    attacco: string        // sotto-testo di "Colpire": formula del TPC, o tipo di TS (es. "Riflessi")
    danniRisolti: DannoRisolto[] // un elemento per ogni danno: NON vanno accodati, una riga ciascuno
    [key: string]: any
}

// Calcola atk/dmg per tutti gli attacchi del personaggio (usato sia dal prefetch in background
// nello store che, se serve, da una chiamata diretta). Ogni riga richiede fino a 2 chiamate al
// backend (colpire + danno): per questo si prefetcha subito dopo il caricamento di items, invece
// di farlo al primo render della pagina Attacchi.
export async function calcolaAttacchi(items: Items | null | undefined, modificatori: DatiPersonaggio | null | undefined): Promise<AttaccoCalcolatoRow[]> {
    if (!items?.attacchi || !modificatori) return []

    const sorted = [...items.attacchi].sort((a: Attacco, b: Attacco) => a.nomeItem.localeCompare(b.nomeItem))

    return Promise.all(
        sorted.map(async (itm: any) => {
            let atkVal: string | null = null
            let cdVal: string | null = null

            const isTs = itm.tipoRisoluzione === 'TS'
            if (!isTs && itm.attacco) {
                const resp = await getValoreFormula(modificatori, itm.attacco)
                atkVal = testoModificatore(resp.data.risultato)
            }
            if (isTs && itm.tiroSalvezza && itm.tiroSalvezzaCd) {
                const resp = await getValoreFormula(modificatori, itm.tiroSalvezzaCd)
                cdVal = String(resp.data.risultato)
            }

            // ogni danno ha la propria formula e il proprio tipo, e resta una riga a sé — non
            // vanno accodati in un'unica stringa (un attacco può avere N danni indipendenti).
            const danni: {formula: string; tipo?: string}[] = itm.danni ?? []
            const dannoRisolti = await Promise.all(danni.map(async d => {
                if (!d.formula) return null
                const resp = await getValoreFormula(modificatori, d.formula)
                return {
                    valore: risolviFormulaDanno(resp.data.formula, modificatori),
                    tipo: d.tipo ?? '',
                    formula: testoFormula(d.formula),
                }
            }))
            const danniRisolti: DannoRisolto[] = dannoRisolti.filter((d): d is DannoRisolto => d !== null)

            return {
                ...itm,
                atk: isTs ? `CD ${cdVal}` : atkVal,
                attacco: isTs ? (itm.tiroSalvezza ?? '') : (itm.attacco ? testoFormula(itm.attacco) : ''),
                danniRisolti,
            }
        })
    )
}







