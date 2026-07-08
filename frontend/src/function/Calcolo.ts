import {calcolaFormula} from "../service/PersonaggioService";

import {DatiPersonaggio} from "../models/dto/DatiPersonaggio";
import {Items} from "../models/dto/Items";
import {Attacco} from "../models/dto/Attacco";
import {applicaBonusDado, removePlus, risolviFormulaDanno, testoFormula, testoModificatore} from "./Utils";

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

export interface AttaccoCalcolatoRow {
    id: any
    nome: string
    nomeItem: string
    atk: string | null
    dmg: string | null
    attacco: string
    colpo: string
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
            let dannoVal: string | null = null

            if (itm.attacco) {
                const resp = await getValoreFormula(modificatori, itm.attacco)
                atkVal = testoModificatore(resp.data.risultato)
            }
            if (itm.colpo) {
                const resp = await getValoreFormula(modificatori, itm.colpo)
                dannoVal = risolviFormulaDanno(resp.data.formula, modificatori)
            }

            return {
                ...itm,
                atk: itm.tiroSalvezza ? `CD ${removePlus(atkVal)} ${itm.tiroSalvezza}` : atkVal,
                dmg: dannoVal,
                attacco: itm.attacco ? testoFormula(itm.attacco) : '',
                colpo: itm.colpo ? testoFormula(itm.colpo) : '',
            }
        })
    )
}







