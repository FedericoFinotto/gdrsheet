import {computed, onMounted, ref} from 'vue'
import api from '../service/api'
import {useMondoStore} from '../stores/mondo'

export interface MondoOpt { id: number; descrizione: string; sistemaId?: number | null; sistemaDescrizione?: string | null }
export interface SistemaOpt { id: number; descrizione: string }

const mondi = ref<MondoOpt[]>([])
const sistemi = ref<SistemaOpt[]>([])
const meiMondi = ref<MondoOpt[]>([])
let loaded = false

async function load() {
    if (loaded) return
    loaded = true
    const [rm, rs, rmei] = await Promise.all([
        api.get<MondoOpt[]>('/item/mondi'),
        api.get<SistemaOpt[]>('/item/sistemi'),
        api.get<MondoOpt[]>('/party/mondi'),
    ])
    mondi.value = rm.data
    sistemi.value = rs.data
    meiMondi.value = rmei.data
}

export function useMondoSistema() {
    onMounted(load)

    const mondoStore = useMondoStore()

    const mondoOptions = computed(() => [
        {value: null as number | null, label: '— nessuno —'},
        ...mondi.value.map(m => ({
            value: m.id,
            label: m.descrizione,
            hint: m.sistemaDescrizione ?? undefined,
        }))
    ])

    const sistemaOptions = computed(() => [
        {value: null as number | null, label: '— nessuno —'},
        ...sistemi.value.map(s => ({value: s.id, label: s.descrizione}))
    ])

    // Auto-seleziona mondo: prima il mondo correntemente selezionato nello switcher globale
    // (se presente tra le opzioni caricate), altrimenti dai miei mondi, altrimenti dalla lista
    // globale se c'è un solo mondo
    const autoMondo = computed<number | null>(() => {
        const corrente = mondoStore.corrente
        if (corrente != null && mondi.value.some(m => m.id === corrente)) return corrente
        if (meiMondi.value.length === 1) return meiMondi.value[0].id
        if (mondi.value.length === 1) return mondi.value[0].id
        return null
    })

    // Sistemi derivati dai miei mondi (deduplicati)
    const meiSistemiIds = computed(() => {
        const ids = new Set<number>()
        meiMondi.value.forEach(m => { if (m.sistemaId) ids.add(m.sistemaId) })
        return [...ids]
    })

    // Auto-seleziona sistema: prima il sistema del mondo correntemente selezionato, poi dai
    // sistemi dei miei mondi, poi dalla lista globale se c'è un solo sistema
    const autoSistema = computed<number | null>(() => {
        const corrente = mondoStore.corrente
        if (corrente != null) {
            const mondoCorrente = mondi.value.find(m => m.id === corrente)
            if (mondoCorrente?.sistemaId) return mondoCorrente.sistemaId
        }
        if (meiSistemiIds.value.length === 1) return meiSistemiIds.value[0]
        if (sistemi.value.length === 1) return sistemi.value[0].id
        return null
    })

    // Opzioni mondo/sistema per filtro compendio (solo i miei)
    const meiMondiOptions = computed(() => [
        {value: null as number | null, label: 'Tutti i mondi'},
        ...meiMondi.value.map(m => ({value: m.id, label: m.descrizione}))
    ])

    return {mondoOptions, sistemaOptions, autoMondo, autoSistema, meiMondi, meiMondiOptions}
}
