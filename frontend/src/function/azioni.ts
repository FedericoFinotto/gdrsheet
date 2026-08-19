// Glifi del font icone Pathfinder2eActions (frontend/src/assets/fonts/Pathfinder2eActions.ttf,
// dichiarato in styles/global.css): il font rimappa i caratteri ASCII '1'..'5' su simboli
// grafici (1/2/3 Azioni, Azione Gratuita, Reazione). Mostrati solo quando il mondo dell'item ha
// MondoConfig.mostraSimboliAzioni attivo (vedi MondiAdmin.vue), altrimenti si mostra il testo.
import {Ref, ref, watch} from 'vue'
import {getConfigMondo} from '../service/MondoAdminService'

export const GLIFO_AZIONE_1 = '1'
export const GLIFO_AZIONI_2 = '2'
export const GLIFO_AZIONI_3 = '3'
export const GLIFO_AZIONE_GRATUITA = '4'
export const GLIFO_REAZIONE = '5'

// Pattern riconosciuti, in ordine: testo grezzo (case-insensitive) delle forme prodotte
// dall'editor/import (vedi normalizeTempo in SpellEditor.vue e classify_tempo nello script di
// import) -> glifo. Solo la PARTE RICONOSCIUTA a inizio stringa viene sostituita dal glifo; il
// resto del testo (es. una nota tra parentesi) resta come testo dopo l'icona.
const PATTERN: Array<{ re: RegExp; glifo: string }> = [
    {re: /^1\s*azione\b/i, glifo: GLIFO_AZIONE_1},
    {re: /^2\s*azioni\b/i, glifo: GLIFO_AZIONI_2},
    {re: /^3\s*azioni\b/i, glifo: GLIFO_AZIONI_3},
    {re: /^(1\s*)?azion[ei]\s*gratuit[ao]e?\b/i, glifo: GLIFO_AZIONE_GRATUITA},
    {re: /^lancio\s*gratuito\b/i, glifo: GLIFO_AZIONE_GRATUITA},
    {re: /^(1\s*)?reazion[ei]\b/i, glifo: GLIFO_REAZIONE},
]

export interface AzioneGlifo {
    glifo: string
    resto: string
}

/** null se il testo non corrisponde a nessun costo in azioni riconosciuto (mostra il testo com'è). */
export function parseAzioneGlifo(testo: string | null | undefined): AzioneGlifo | null {
    const s = (testo ?? '').trim()
    if (!s) return null
    for (const {re, glifo} of PATTERN) {
        const m = re.exec(s)
        if (m) return {glifo, resto: s.slice(m[0].length).trim()}
    }
    return null
}

// Composable: carica (e tiene aggiornato) il flag mostraSimboliAzioni del mondo indicato da
// idMondo (reattivo). false finché non risolto, in caso di errore, o se idMondo è null/undefined.
export function useMostraSimboliAzioni(idMondo: Ref<number | null | undefined>) {
    const mostraSimboliAzioni = ref(false)
    watch(idMondo, async mondo => {
        if (mondo == null) { mostraSimboliAzioni.value = false; return }
        try {
            const {data} = await getConfigMondo(mondo)
            mostraSimboliAzioni.value = data.mostraSimboliAzioni
        } catch (e) {
            console.error('Errore caricamento flag mostraSimboliAzioni:', e)
            mostraSimboliAzioni.value = false
        }
    }, {immediate: true})
    return mostraSimboliAzioni
}
