<script setup lang="ts">
import {computed, onMounted, reactive, ref, toRaw, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import useChildCreate from '../../../../../../../function/useChildCreate'

import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {Classe} from '../../../../../../../models/dto/Classe'
import {UpdateLivelloRequest} from '../../../../../../../models/dto/UpdateLivelloRequest'
import {Abilita} from '../../../../../../../models/dto/Abilita'
import {Gradi} from '../../../../../../../models/dto/Gradi'
import {Rank} from '../../../../../../../models/dto/Rank'
import {AbilitaClasse} from '../../../../../../../models/dto/AbilitaClasse'
import {getItemLabel, LABELS} from '../../../../../../../models/entity/ItemLabel'
import {
  getAbilitaClasseByPersonaggioLivelloClasse,
  getGradiClasseByPersonaggioLivelloClasse,
  getIdPersonaggioFromLivello,
  getItem,
  getListaAbilitaPerPersonaggio,
  getListaClassiPerPersonaggio,
  saveLivello,
} from '../../../../../../../service/PersonaggioService'

import TabLivelloBase from './TabLivelloBase.vue'
import TabClasseMaledizione from './TabClasseMaledizione.vue'
import TabContenutiLivello from './TabContenutiLivello.vue'
import TabAbilitaRanghi from './TabAbilitaRanghi.vue'
import TabItemExtra from './TabItemExtra.vue'
import TabExpandable from '../../../../../../../components/TabExpandable.vue'
import ModificatoriEditor from '../Sections/ModificatoriEditor.vue'
import {ModificatoreRow} from '../../../../../../../models/dto/UpdateItemRequest'
import {GrantRow} from "../../../../../../../models/dto/GrantRow";

type Id = number
interface Caratteristiche {
  FOR?: number | null;
  DES?: number | null;
  COS?: number | null;
  INT?: number | null;
  SAG?: number | null;
  CAR?: number | null
}

type SkillRow = {
  uid: string; name: string; isClass: boolean; isOtherClass: boolean;
  spent: number; effect: number; current: number; total: number; max: number
}

const props = defineProps<{ item: ItemDB; readonly?: boolean }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void }>()

const router = useRouter()
const route = useRoute()
const childCreate = useChildCreate()

const personaggioId = ref<Id | null>(null)
const classeDetail = ref<any | null>(null)

const form = reactive<UpdateLivelloRequest>({
  maledizioneId: undefined, tipoScelta: undefined,
  livello: null,
  caratteristiche: {FOR: null, DES: null, COS: null, INT: null, SAG: null, CAR: null},
  classeId: null,
  maledizioneNome: null,
  dv: null,
  ranghi: {},
  livelliClasse: {}
});

const busy = ref(false)
const disabledAll = computed(() => !!props.readonly || busy.value)
const canSave = computed(() => !busy.value && !props.readonly && !!form.classeId)

/* Liste base */
const classi = ref<Classe[]>([])
const abilita = ref<Abilita[]>([])

/* Helpers */
const abilUid = (a: Abilita) => String((a as any)?.abilita?.id ?? '')
const abilName = (a: Abilita) => String((a as any)?.abilita?.nome ?? '').trim()
function cleanedCaratteristiche(src: Caratteristiche): Partial<Caratteristiche> {
  const out: Partial<Caratteristiche> = {}
  ;(['FOR', 'DES', 'COS', 'INT', 'SAG', 'CAR'] as (keyof Caratteristiche)[]).forEach(k => {
    const v = src[k];
    if (v !== null && v !== undefined && (v as any) !== '') out[k] = Number(v)
  })
  return out
}
function unwrap<T>(res: any): T {
  return (res && 'data' in res) ? res.data : res
}
function readItemLevel(it: any): number | null {
  const raw = (getItemLabel(it, 'LVL') ?? '').toString().trim()
  if (!raw) return null
  const n = Number(raw)
  return Number.isFinite(n) ? n : null
}
// gradi congelati sul livello (non retroattivi): se presenti vincono sul calcolo da formula
function readGradiLivello(): number | null {
  const raw = (getItemLabel(props.item, 'GRADI_LIVELLO') ?? '').toString().trim()
  if (!raw) return null
  const n = Number(raw)
  return Number.isFinite(n) ? n : null
}

/* Abilità di classe */
const abilitaClasse = ref<AbilitaClasse[]>([])
const abilitaClasseSet = computed(
    () => new Set(abilitaClasse.value.filter(x => x.diClasse || x.all).map(x => String(x.id).toLowerCase()))
)
const abilitaAltraClasseSet = computed(
    () => new Set(abilitaClasse.value.filter(x => !x.diClasse && !x.all).map(x => String(x.id).toLowerCase()))
)
const isClassSkill = (uid: string) => abilitaClasseSet.value.has(uid.toLowerCase())
const isAltraClassSkill = (uid: string) => abilitaAltraClasseSet.value.has(uid.toLowerCase())

/* Attuali senza il livello corrente */
const currentByUid = reactive<Record<string, number>>({})
function extractThisLevelRanks(a: Abilita, livelloItemId: number) {
  const ranks = (a as any)?.rank?.ranks
  const all: Rank[] = Array.isArray(ranks) ? (ranks as Rank[]) : []
  const thisLevel = all.filter(r => r?.itemId === livelloItemId)
  const other = all.filter(r => r?.itemId !== livelloItemId)
  const thisLVL = thisLevel.reduce((s, r) => s + (r?.valore || 0), 0)
  const otherLVL = other.reduce(
      (sum, r) => sum + (r ? (r.diClasse ? r.valore : r.valore / 2) : 0),
      0
  )
  return {otherLVL, thisLVL}
}
function preloadAttualiDaStats() {
  const lvlId = Number(props.item?.id)
  Object.keys(currentByUid).forEach(k => delete currentByUid[k])
  Object.keys(form.ranghi).forEach(k => delete form.ranghi[k])
  for (const a of (abilita.value ?? [])) {
    const uid = abilUid(a)
    if (!uid) continue
    const {otherLVL, thisLVL} = extractThisLevelRanks(a, lvlId)
    currentByUid[uid] = Math.max(0, otherLVL)
    form.ranghi[uid] = thisLVL
  }
}

/* Avanzamenti classe -> livelli disponibili */
function toLevelNumber(a: any): number | null {
  if (a?.livello != null && Number.isFinite(Number(a.livello))) return Number(a.livello)
  const viaSelf = readItemLevel(a)
  if (viaSelf != null) return viaSelf
  const viaTarget = readItemLevel(a?.itemTarget)
  if (viaTarget != null) return viaTarget
  return null
}
type AvEntry = { livello: number; item: any }
const avanzamentiClasse = computed<AvEntry[]>(() => {
  const arr: any[] = classeDetail.value?.avanzamento ?? []
  if (!Array.isArray(arr)) return []
  return arr
      .filter(a => a?.itemTarget?.tipo === 'AVANZAMENTO')
      .map(a => ({livello: toLevelNumber(a), item: a.itemTarget}))
      .filter(e => e.livello != null) as AvEntry[]
})
const livelliDisponibili = computed<number[]>(() => {
  const s = new Set<number>()
  avanzamentiClasse.value.forEach(e => s.add(e.livello))
  return Array.from(s).sort((a, b) => a - b)
})

/* Gradi */
const gradiInfo = ref<Gradi | null>(null)
// budget gradi del livello: input editabile, pre-valorizzato con la somma calcolata,
// congelato sul livello (label GRADI_LIVELLO) al salvataggio
const gradiInput = ref<number | null>(null)
const gradiFrozen = ref(false)
const budgetGradi = computed(() => Number(gradiInput.value ?? 0))
const livelliSelezionati = computed<number[]>(() =>
    Object.entries(form.livelliClasse)
        .filter(([, v]) => !!v)
        .map(([k]) => Number(k))
        .filter(n => Number.isFinite(n))
        .sort((a, b) => a - b)
)
const isProfessione = (uid: string) => uid.toUpperCase().startsWith('PR')
const totalPointsSpent = computed(
    () => Object.entries(form.ranghi)
        .filter(([uid]) => !isProfessione(uid))
        .reduce((a, [, b]) => a + (Number(b) || 0), 0)
)
const showGradiTab = computed(
    () => !!form.classeId && !!gradiInfo.value && (budgetGradi.value > 0) && abilita.value.length > 0
)
const sumAbil = computed(() => {
  const total = budgetGradi.value
  const used = totalPointsSpent.value
  const max = gradiInfo.value?.max ?? 0
  return `${used}/${total} (max ${max})`
})

function debounce<T extends (...args: any[]) => any>(fn: T, wait = 200) {
  let t: any
  return (...args: Parameters<T>) => {
    if (t) clearTimeout(t)
    t = setTimeout(() => fn(...args), wait)
  }
}
const gradiKey = computed(() => {
  if (!personaggioId.value || !form.classeId || !form.livello || !livelliSelezionati.value.length) return ''
  return `${personaggioId.value}|${form.classeId}|${form.livello}|${livelliSelezionati.value.join(',')}`
})
const debouncedRefresh = debounce(() => refreshGradiInfo(), 250)
let lastGradiReq = 0
async function refreshGradiInfo() {
  if (!personaggioId.value || !form.classeId || !form.livello || livelliSelezionati.value.length === 0) {
    gradiInfo.value = null
    return
  }
  const levelsStr = livelliSelezionati.value.join(',')
  const token = ++lastGradiReq
  try {
    const res = await getGradiClasseByPersonaggioLivelloClasse(
        personaggioId.value,
        form.livello,
        form.classeId,
        levelsStr
    )
    if (token === lastGradiReq) {
      const g = unwrap<Gradi>(res)
      gradiInfo.value = g
      // se non congelato, l'input segue la somma calcolata dai livelli selezionati
      if (!gradiFrozen.value) gradiInput.value = g.toConsume
    }
  } catch (e) {
    if (token === lastGradiReq) gradiInfo.value = null
    console.error('Errore getGradiClasseByPersonaggioLivelloClasse:', e)
  }
}

/* Righe abilità */
const rows = computed<SkillRow[]>(() => {
  return (abilita.value ?? []).map(a => {
    const uid = abilUid(a)
    const name = abilName(a)
    const isClass = isClassSkill(uid)
    const isOtherClass = isAltraClassSkill(uid)
    const spent = Number(form.ranghi[uid] ?? 0)
    const effect = isClass ? spent : Math.floor(spent / 2)
    const current = Number(currentByUid[uid] ?? 0)
    const total = current + effect
    const max = isClass || isOtherClass
        ? gradiInfo.value?.max ?? Infinity
        : Math.floor((gradiInfo.value?.max ?? Infinity) / 2)
    return {uid, name, isClass, isOtherClass, spent, effect, current, total, max}
  }).sort((a, b) => a.name.localeCompare(b.name))
})
function canInc(r: SkillRow): boolean {
  if (!gradiInfo.value) return true
  const nextSpent = r.spent + 1
  const nextEffect = r.isClass ? nextSpent : Math.floor(nextSpent / 2)
  const wouldExceedMax = r.max < (r.current + nextEffect)
  if (isProfessione(r.uid)) return !wouldExceedMax
  const wouldExceedBudget = (totalPointsSpent.value + 1) > budgetGradi.value
  return !wouldExceedMax && !wouldExceedBudget
}
function inc(uid: string) {
  const r = rows.value.find(x => x.uid === uid)
  if (!r || !canInc(r)) return
  form.ranghi[uid] = (form.ranghi[uid] ?? 0) + 1
}
function dec(uid: string) {
  form.ranghi[uid] = Math.max(0, (form.ranghi[uid] ?? 0) - 1)
}
function onDirectChange(uid: string, val: string) {
  const n = Math.max(0, Math.floor(Number(val)))
  form.ranghi[uid] = n
  if (gradiInfo.value && !isProfessione(uid)) {
    const overflow = totalPointsSpent.value - budgetGradi.value
    if (overflow > 0) form.ranghi[uid] = Math.max(0, n - overflow)
  }
  const a = abilita.value.find(x => abilUid(x) === uid)
  if (a && gradiInfo.value) {
    const isClass = isClassSkill(uid)
    let s = form.ranghi[uid]
    const current = currentByUid[uid] ?? 0
    while (s > 0) {
      const e = isClass ? s : Math.floor(s / 2)
      if (current + e <= gradiInfo.value.max) break
      s--
    }
    form.ranghi[uid] = s
  }
}

/* Grants: li calcola TabContenutiLivello, qui solo gli id selezionati */
const selectedGrantIds = ref<Set<string>>(new Set())
const selectedGrants = ref<GrantRow[]>([])

/* Item extra: item generici non derivati dalla classe */
const extraItems = ref<ItemDB[]>([])

/* Modificatori liberi del livello (aggiunti a mano, come su un item qualunque) */
const modificatoriLiberi = ref<ModificatoreRow[]>([])

// ID degli item presenti negli avanzamenti classe (non devono apparire come "extra")
const classeAvanzamentiItemIds = computed<Set<number>>(() => {
  const arr: any[] = classeDetail.value?.avanzamento ?? []
  const ids = new Set<number>()
  for (const a of arr) {
    if (a?.itemTarget?.tipo !== 'AVANZAMENTO' && a?.itemTarget?.id != null)
      ids.add(Number(a.itemTarget.id))
  }
  return ids
})

// Tipi esclusi dagli extra (gestiti altrove)
const TIPI_ESCLUSI_EXTRA = new Set(['CLASSE', 'RAZZA', 'MALEDIZIONE', 'AVANZAMENTO', 'LIVELLO', 'PERSONAGGIO', 'ATTACCO'])

/* onMounted & watchers */
onMounted(async () => {
  try {
    busy.value = true
    form.livello = readItemLevel(props.item)

    const classeLabel = getItemLabel(props.item, LABELS.CLASSE)
    const maledizioneLabel = getItemLabel(props.item, LABELS.MALEDIZIONE)

    const dvLabel = getItemLabel(props.item, 'DV')
    if (typeof dvLabel === 'string' && dvLabel.trim()) form.dv = dvLabel.trim()

    const gradiLabel = readGradiLivello()
    if (gradiLabel != null) {
      gradiInput.value = gradiLabel
      gradiFrozen.value = true   // valore già congelato: non ricalcolare
    }

    if (classeLabel) form.classeId = Number(classeLabel)
    if (typeof maledizioneLabel === 'string' && maledizioneLabel.trim()) {
      form.maledizioneNome = maledizioneLabel.trim()
    }

    const livelliClasseLabel: string | null = getItemLabel(props.item, LABELS.CLASSE_LIVELLO)
    if (typeof livelliClasseLabel === 'string' && livelliClasseLabel.trim()) {
      const lvls = livelliClasseLabel
          .split(',')
          .map(s => Number(s.trim()))
          .filter(lv => Number.isFinite(lv))
      form.livelliClasse = {}
      for (const lv of lvls) form.livelliClasse[lv] = true
    }

    for (const m of (props.item.modificatori ?? []))
      if (m.tipo === 'BASE') (form.caratteristiche as any)[m.stat.id] = m.valore

    const pid = unwrap<Id>(await getIdPersonaggioFromLivello(props.item.id))
    personaggioId.value = pid
    const [cls, abi] = await Promise.all([
      getListaClassiPerPersonaggio(pid),
      getListaAbilitaPerPersonaggio(pid)
    ])
    classi.value = unwrap<Classe[]>(cls) ?? []
    abilita.value = unwrap<Abilita[]>(abi) ?? []

    preloadAttualiDaStats()

    if (form.classeId) await loadClasseDetail(form.classeId)

    // Pre-popola item extra: figli del livello non gestiti dalla classe
    const classeIds = classeAvanzamentiItemIds.value
    extraItems.value = (props.item.child ?? [])
        .map((c: any) => c.itemTarget)
        .filter((t: any) => t && !TIPI_ESCLUSI_EXTRA.has(t.tipo) && !classeIds.has(Number(t.id)))

    // Pre-popola modificatori liberi: quelli propri del livello (no BASE/RANK, no copie da grant).
    // Oltre a id_sorgente (dati nuovi), escludo anche per match stat+valore con i modificatori
    // concessi dalla classe per i livelli selezionati (sana i dati vecchi senza id_sorgente).
    const grantedSig = new Map<string, number>() // "statId|valore" -> conteggio disponibile
    const lvSel = new Set(livelliSelezionati.value)
    for (const a of (classeDetail.value?.avanzamento ?? [])) {
      if (a?.itemTarget?.tipo !== 'AVANZAMENTO') continue
      if (!lvSel.has(Number(a?.livello))) continue
      for (const gm of (a.itemTarget.modificatori ?? [])) {
        if (gm?.stat?.id === 'GRADI') continue
        const k = `${gm?.stat?.id}|${gm?.valore}`
        grantedSig.set(k, (grantedSig.get(k) ?? 0) + 1)
      }
    }
    modificatoriLiberi.value = (props.item.modificatori ?? [])
        .filter((m: any) => m.tipo !== 'BASE' && m.tipo !== 'RANK')
        .filter((m: any) => {
          if (m.idSorgente != null) return false // copia da grant (dati nuovi)
          const k = `${m.stat?.id}|${m.valore}`
          const left = grantedSig.get(k) ?? 0
          if (left > 0) { grantedSig.set(k, left - 1); return false } // match con concesso dalla classe
          return true
        })
        .map((m: any) => ({
          id: m.id,
          statId: m.stat?.id ?? '',
          tipo: m.tipo,
          valore: String(m.valore ?? ''),
          nota: m.nota ?? '',
          sempreAttivo: !!m.sempreAttivo,
        }))

    // Ritorno dal flusso "crea e collega" di un item aggiuntivo: ripristina lo stato
    // salvato e aggancia il nuovo item creato.
    const draft = !route.query.link ? childCreate.peekDraft() : null
    if (draft && draft.target === 'extra' && draft.tipo === 'LIVELLO') {
      childCreate.takeDraft()
      const snap = draft.snapshot
      if (snap?.form) Object.assign(form, snap.form)
      if (Array.isArray(snap?.extraItems)) extraItems.value = snap.extraItems
      if (Array.isArray(snap?.modificatoriLiberi)) modificatoriLiberi.value = snap.modificatoriLiberi
      if (snap?.gradiInput !== undefined) gradiInput.value = snap.gradiInput
      const created = childCreate.takeCreatedChild()
      if (created && !extraItems.value.some((i: any) => i.id === created.id)) {
        extraItems.value = [...extraItems.value, created as any]
      }
    }
  } catch (e) {
    console.error('Errore inizializzazione LivelloEditor:', e)
  } finally {
    busy.value = false
  }
})

async function loadClasseDetail(id: Id | null, propagaDv = false) {
  if (!id) {
    classeDetail.value = null
    selectedGrantIds.value.clear()
    selectedGrants.value.clear()
    return
  }
  try {
    const res = await getItem(id)
    classeDetail.value = unwrap<any>(res)

    // dadi vita dalla classe (può anche non averne -> stringa vuota).
    // - al primo caricamento: pre-valorizza solo se il livello non ha già un DV
    // - al cambio classe (propagaDv): segue la classe, svuotando se la classe non ha DV
    const dvClasse = getItemLabel(classeDetail.value, 'DV')
    const dvClasseStr = (typeof dvClasse === 'string' ? dvClasse.trim() : '')
    if (propagaDv) {
      form.dv = dvClasseStr || null
    } else if (!form.dv && dvClasseStr) {
      form.dv = dvClasseStr
    }

    if (personaggioId.value != null && form.livello != null) {
      const ac = await getAbilitaClasseByPersonaggioLivelloClasse(personaggioId.value, form.livello, id)
      abilitaClasse.value = unwrap<AbilitaClasse[]>(ac) ?? []
    }

    const levels = livelliDisponibili.value
    const next: Record<number, boolean> = {}
    levels.forEach(lv => {
      next[lv] = form.livelliClasse[lv] ?? false
    })
    form.livelliClasse = next

    if (gradiKey.value) debouncedRefresh()
  } catch (e) {
    classeDetail.value = null
    form.livelliClasse = {}
    selectedGrantIds.value.clear()
    console.error('Errore caricando dettaglio classe:', e)
  }
}

/* Crea-e-collega un item aggiuntivo: salva lo stato corrente del livello e apre la
 * creazione di un nuovo item col nome pre-compilato. Al ritorno viene agganciato. */
function snapshotForm() {
  return JSON.parse(JSON.stringify({
    form,
    extraItems: extraItems.value,
    modificatoriLiberi: modificatoriLiberi.value,
    gradiInput: gradiInput.value,
  }))
}

function onCreateExtra(tipo: string | undefined, nome: string) {
  childCreate.stashDraft({target: 'extra', tipo: 'LIVELLO', snapshot: snapshotForm()})
  const tipoSeg = tipo ? `/${tipo}` : ''
  const params = new URLSearchParams({link: '1'})
  if (personaggioId.value != null) params.set('personaggio', String(personaggioId.value))
  if (nome && nome.trim()) params.set('nome', nome.trim())
  router.push(`/itemcreate${tipoSeg}?${params.toString()}`)
}

let classeWatchInit = true
watch(() => form.classeId, async (id, prev) => {
  if (id === prev) return
  // al primo fire (assegnazione iniziale da label) non propagare il DV: lo gestisce onMounted
  const propaga = !classeWatchInit
  classeWatchInit = false
  await loadClasseDetail(id, propaga)
})

watch([abilita, () => props.item.id], () => {
  preloadAttualiDaStats()
}, {deep: true})

watch(gradiKey, (key) => {
  if (!key) {
    gradiInfo.value = null
    return
  }
  debouncedRefresh()
})

/* Salvataggio */
async function onSave() {
  if (!canSave.value || personaggioId.value == null) return
  try {
    busy.value = true
    const payload = toRaw({
      livelloId: props.item.id,
      personaggioId: personaggioId.value,
      livello: form.livello,
      caratteristiche: cleanedCaratteristiche(form.caratteristiche) as Record<string, number>,
      classeId: form.classeId,
      maledizioneNome: form.maledizioneNome?.trim() || null,
      dv: form.dv?.trim() || null,
      gradi: gradiInput.value != null ? Number(gradiInput.value) : null,
      livelliClasse: livelliSelezionati.value,
      ranghi: Object.entries(form.ranghi)
          .filter(([, v]) => (Number(v) || 0) > 0)
          .map(([abilitaId, punti]) => ({abilitaId, punti: Number(punti)})),
      grantsSelezionati: [
        ...Array.from(selectedGrants.value)
            .map(g => ({id: g.id, tipo: g.tipo, livello: g.livello, descrizione: g.descrizione})),
        ...extraItems.value
            .map(i => ({id: `item-${i.id}`, tipo: 'ITEM' as const, livello: 0, descrizione: i.nome}))
      ],
      modificatoriLiberi: modificatoriLiberi.value
          .filter(m => m.statId && m.statId.trim())
          .map(m => ({
            id: m.id,
            statId: m.statId,
            tipo: m.tipo,
            valore: String(m.valore ?? ''),
            nota: m.nota ?? '',
            sempreAttivo: !!m.sempreAttivo,
          }))
    })
    await saveLivello(payload)
    emit('saved')
  } catch (e) {
    console.error('Errore salvataggio livello:', e)
  } finally {
    busy.value = false
  }
}
function onCancel() {
  emit('cancel')
}

/* Summary */
const sumCar = computed(() => {
  const c = cleanedCaratteristiche(form.caratteristiche)
  const parts: string[] = []
  ;(['FOR', 'DES', 'COS', 'INT', 'SAG', 'CAR'] as const).forEach(k => {
    const v = (c as any)[k]
    if (v != null) parts.push(`${k} ${v}`)
  })
  return parts.join(', ') || '—'
})
const classeSelezionata = computed(
    () => classi.value.find(c => c.id === form.classeId)?.nome || '—'
)
const sumClasseMaledizione = computed(() =>
    `Classe: ${classeSelezionata.value}${form.maledizioneNome ? ` | Maledizione: ${form.maledizioneNome}` : ''}`
)
</script>

<template>
  <form class="spell-editor" @submit.prevent="onSave">

    <TabLivelloBase
        :disabled="disabledAll"
        v-model:livello="form.livello"
        v-model:caratteristiche="form.caratteristiche"
        :summary="sumCar"
    />

    <TabClasseMaledizione
        :disabled="disabledAll"
        :classi="classi"
        :classe-detail="classeDetail"
        :livelli-disponibili="livelliDisponibili"
        v-model:classe-id="form.classeId"
        v-model:maledizione-nome="form.maledizioneNome"
        v-model:livelli-classe="form.livelliClasse"
        :summary="sumClasseMaledizione"
    />

    <div v-if="form.classeId" class="dv-row">
      <label class="dv-field">
        <span class="dv-lbl">Dadi vita</span>
        <input v-model.trim="form.dv" type="text" placeholder="Es.: 2d10" :disabled="disabledAll"/>
      </label>
      <label class="dv-field">
        <span class="dv-lbl">Gradi (punti abilità)</span>
        <input v-model.number="gradiInput" type="number" min="0" :disabled="disabledAll"/>
      </label>
    </div>

    <TabContenutiLivello
        :disabled="disabledAll"
        :loading="busy"
        :classe-id="form.classeId"
        :classe="classeDetail"
        :livello="item"
        :livelli-selezionati="livelliSelezionati"
        :default-open="true"
        v-model:selected-grant-ids="selectedGrantIds"
        v-model:selected-grants="selectedGrants"
    />

    <TabItemExtra
        :disabled="disabledAll"
        :loading="busy"
        :items="extraItems"
        @update:items="extraItems = $event"
        @create-new="onCreateExtra"
    />

    <TabExpandable title="Modificatori" :loading="busy">
      <template #summary>{{ modificatoriLiberi.filter(m => m.statId).length || '—' }}</template>
      <template #content>
        <ModificatoriEditor v-model="modificatoriLiberi" :disabled="disabledAll"/>
      </template>
    </TabExpandable>

    <TabAbilitaRanghi
        :disabled="disabledAll"
        :loading="busy"
        :rows="rows"
        :sum-abil="sumAbil"
        :can-inc="canInc"
        :default-open="false"
        @inc="inc"
        @dec="dec"
        @direct-change="onDirectChange"
    />

    <div class="actions">
      <button type="button" class="btn ghost" @click="onCancel" :disabled="busy">Annulla</button>
      <button type="submit" class="btn primary" :disabled="!canSave || personaggioId===null">Salva</button>
    </div>
  </form>
</template>

<style scoped>
.spell-editor {
  display: flex;
  flex-direction: column;
  gap: .75rem;
}
.dv-row { display: grid; grid-template-columns: 1fr 1fr; gap: .5rem; }
.dv-field { display: grid; gap: .3rem; }
.dv-lbl { font-size: .8rem; font-weight: 600; opacity: .85; }
.dv-field input {
  width: 100%; padding: .5rem .6rem; border: 1px solid #d0d5dd; border-radius: .5rem; background: #fff;
}
.sp-head {
  display: flex;
  align-items: baseline;
  gap: .5rem;
}
.sp-head h2 {
  margin: 0;
  font-size: 1rem;
}
.muted {
  opacity: .7;
  font-size: .85rem;
}
.actions {
  position: sticky;
  bottom: 0;
  background: #fff;
  padding: .5rem 0 calc(.5rem + env(safe-area-inset-bottom, 0px));
  border-top: 1px solid #e5e7eb;
  display: flex;
  justify-content: flex-end;
  gap: .5rem;
}
.btn {
  padding: .5rem .9rem;
  border-radius: .5rem;
  border: 1px solid transparent;
  cursor: pointer;
}
.btn.ghost {
  border-color: #d0d5dd;
  background: #fff;
}
.btn.primary {
  background: #2563eb;
  color: #fff;
}
.btn:disabled {
  opacity: .6;
  cursor: default;
}
</style>
