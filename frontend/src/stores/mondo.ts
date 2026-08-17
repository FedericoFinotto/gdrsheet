import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import {getMyLabel, setMyLabel} from '../service/AuthService'
import {getMondiDisponibili} from '../service/MondoAdminService'
import {MondoOpt} from '../function/useMondoSistema'

// UtenteLabel usata per ricordare l'ultimo mondo aperto (vedi Constants.UTENTE_LABEL_ULTIMO_MONDO
// lato backend): stessa chiave, stesso significato.
const LABEL_ULTIMO_MONDO = 'ULTIMO_MONDO'

/**
 * Mondo "corrente" selezionato dall'utente: quando ha permessi (master) su più mondi, o è admin
 * ed esistono più mondi, un menu nello UpperBar permette di switchare. Le pagine che dipendono
 * dal mondo (compendio, creazione item, ricerca profonda...) leggono questo store invece di
 * avere un proprio selettore locale, così la selezione è unica e coerente in tutta l'app.
 */
export const useMondoStore = defineStore('mondo', () => {
    const disponibili = ref<MondoOpt[]>([])
    const corrente = ref<number | null>(null)
    const caricato = ref(false)

    // ha senso mostrare lo switcher solo se c'è più di un mondo tra cui scegliere
    const mostraSwitcher = computed(() => disponibili.value.length > 1)

    // Più chiamanti (UpperBar all'avvio, Compendio.vue che vuole aspettarla) possono invocare
    // carica() quasi in contemporanea: senza condividere la stessa promise, chi arriva secondo
    // vedrebbe subito caricato.value=true (impostato in sincrono qui sotto) e proseguirebbe
    // credendo che corrente sia già risolto, mentre la prima chiamata è ancora in volo — il
    // sintomo era Compendio.vue che chiamava /item/compendio due volte (senza e poi con idMondo).
    let caricamentoPromise: Promise<void> | null = null

    // force=true: ri-scarica anche se già caricato (invece di riusare la promise memoizzata) —
    // usato dal toggle modalità admin (UpperBar.vue), dato che i mondi disponibili dipendono
    // dall'header X-Admin-Mode e vanno aggiornati senza un reload completo della pagina.
    function carica(force = false): Promise<void> {
        if (caricamentoPromise && !force) return caricamentoPromise
        caricato.value = true
        caricamentoPromise = (async () => {
            try {
                disponibili.value = (await getMondiDisponibili()).data ?? []
            } catch (e) {
                console.error('Errore caricamento mondi disponibili:', e)
                disponibili.value = []
            }
            if (!disponibili.value.length) {
                corrente.value = null
                return
            }
            let salvato: number | null = null
            try {
                const res = await getMyLabel(LABEL_ULTIMO_MONDO)
                salvato = res.data ? Number(res.data) : null
            } catch (e) {
                // nessuna preferenza salvata (prima volta) o errore di rete: non bloccante
            }
            corrente.value = disponibili.value.some(m => m.id === salvato) ? salvato : disponibili.value[0].id
        })()
        return caricamentoPromise
    }

    function seleziona(mondoId: number) {
        if (corrente.value === mondoId) return
        corrente.value = mondoId
        setMyLabel(LABEL_ULTIMO_MONDO, String(mondoId))
            .catch(e => console.error('Errore salvataggio ultimo mondo:', e))
    }

    // al logout: si ricarica da zero al prossimo login (utente diverso = permessi diversi)
    function reset() {
        disponibili.value = []
        corrente.value = null
        caricato.value = false
        caricamentoPromise = null
    }

    return {disponibili, corrente, mostraSwitcher, carica, seleziona, reset}
})
