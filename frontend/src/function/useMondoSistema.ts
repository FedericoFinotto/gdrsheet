import {computed, onMounted, ref} from 'vue'
import api from '../service/api'

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

    // Auto-seleziona mondo: prima dai miei mondi, poi dalla lista globale se c'è un solo mondo
    const autoMondo = computed<number | null>(() => {
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

    // Auto-seleziona sistema: prima dai sistemi dei miei mondi, poi dalla lista globale se c'è un solo sistema
    const autoSistema = computed<number | null>(() => {
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
