<script setup lang="ts">
import {computed, markRaw, onMounted, reactive, ref, toRaw, watch} from 'vue'
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
  setScelta,
} from '../../../../../../../service/PersonaggioService'

import TabLivelloBase from './TabLivelloBase.vue'
import TabClasseMaledizione from './TabClasseMaledizione.vue'
import TabContenutiLivello from './TabContenutiLivello.vue'
import TabAbilitaRanghi from './TabAbilitaRanghi.vue'
import TabItemExtra from './TabItemExtra.vue'
import TabExpandable from '../../../../../../../components/TabExpandable.vue'
import Icona from '../../../../../../../components/Icona/Icona.vue'
import ModificatoriEditor from '../Sections/ModificatoriEditor.vue'
import {ModificatoreRow} from '../../../../../../../models/dto/UpdateItemRequest'
import {GrantRow} from "../../../../../../../models/dto/GrantRow";
import {getTipoItemConfig} from '../../../../../../../service/MondoAdminService'
import usePopup from '../../../../../../../function/usePopup'
import Mobile_DettaglioItem from '../../../Dettaglio/Mobile_DettaglioItem.vue'

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
const emit = defineEmits<{
  (e: 'saved'): void
  (e: 'cancel'): void
  (e: 'savedResta', item: ItemDB): void
}>()

const router = useRouter()
const route = useRoute()
const childCreate = useChildCreate()

// Card strutturali abilitate per (mondo, LIVELLO): vedi MondoTipoItemCardAbilitata lato backend.
const cards = ref<Set<string>>(new Set())
watch(() => props.item.mondo?.id, async (idMondo) => {
  if (!idMondo) { cards.value = new Set(); return }
  try {
    const {data} = await getTipoItemConfig(idMondo, 'LIVELLO')
    cards.value = new Set(data.cardAbilitate)
  } catch (e) {
    console.error('Errore caricamento configurazione card:', e)
    cards.value = new Set()
  }
}, {immediate: true})

// segnalato dal pulsante "Aggiungi Razza" (Mobile_Cico_7_Livelli.vue): apre subito la tab
// "Caratteristiche" invece di lasciarla chiusa come nel flusso normale di modifica di un livello
const apriCaratteristiche = computed(() => route.query.apriCaratteristiche === '1')

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
const canSave = computed(() =>
    !busy.value && !props.readonly && !!form.classeId && livelliSelezionati.value.length > 0)

/* Liste base */
const abilita = ref<Abilita[]>([])

// Ricerca remota classi/razze: il compendio ha ormai centinaia di classi (import bulk),
// caricarle tutte ad ogni apertura dell'editor sarebbe troppo lento — si cerca solo quando
// l'utente digita qualcosa (vedi SearchSelect "onSearch").
async function searchClassi(q: string) {
  if (personaggioId.value == null) return []
  const res = await getListaClassiPerPersonaggio(personaggioId.value, q)
  const list = unwrap<Classe[]>(res) ?? []
  return list.map((c: any) => ({
    value: c.id,
    label: c.nome,
    hint: c.tipo === 'RAZZA' ? 'Razza' : 'Classe',
    hintColor: c.tipo === 'RAZZA' ? '#15803d' : '#4338ca',
  }))
}

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
  // numero di livelli effettivi della classe (label LIVELLI_CLASSE)
  const raw = (classeDetail.value?.labels ?? []).find((l: any) => l.label === 'LIVELLI_CLASSE')?.valore
  const n = Number(raw)
  if (Number.isFinite(n) && n > 0) {
    return Array.from({length: n}, (_, i) => i + 1)
  }
  // fallback: ricava dai livelli degli avanzamenti presenti
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
  return (abilita.value ?? []).filter(a => a.abilita?.rankable !== false).map(a => {
    const uid = abilUid(a)
    const name = abilName(a)
    const isClass = isClassSkill(uid)
    const isOtherClass = isAltraClassSkill(uid)
    const spent = Number(form.ranghi[uid] ?? 0)
    // le professioni non hanno concetto di abilità di classe: 1 punto = 1 grado, sempre
    const effect = isProfessione(uid) ? spent : (isClass ? spent : Math.floor(spent / 2))
    const current = Number(currentByUid[uid] ?? 0)
    const total = current + effect
    const max = isProfessione(uid) || isClass || isOtherClass
        ? gradiInfo.value?.max ?? Infinity
        : Math.floor((gradiInfo.value?.max ?? Infinity) / 2)
    return {uid, name, isClass, isOtherClass, spent, effect, current, total, max}
  }).sort((a, b) => a.name.localeCompare(b.name))
})
function canInc(r: SkillRow): boolean {
  if (!gradiInfo.value) return true
  const nextSpent = r.spent + 1
  const nextEffect = isProfessione(r.uid) ? nextSpent : (r.isClass ? nextSpent : Math.floor(nextSpent / 2))
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
    const professione = isProfessione(uid)
    let s = form.ranghi[uid]
    const current = currentByUid[uid] ?? 0
    while (s > 0) {
      const e = professione ? s : (isClass ? s : Math.floor(s / 2))
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

/* Punti ferita: input dedicato a fianco del dado vita. Salvato come un modificatore VALORE
 * sulla stat PF — tecnicamente un "modificatore libero" come gli altri (stesso meccanismo/
 * tabella), ma tenuto FUORI dalla lista modificatoriLiberi/dal relativo editor generico qui
 * sotto per non duplicare l'informazione in due punti dell'UI. Identificato in modo univoco
 * dal campo "placeholder" (non da nota/sempreAttivo, ambigui se l'utente ne aggiunge altri
 * simili a mano dall'editor "Modificatori"). */
const PF_STAT_ID = 'PF'
const PLACEHOLDER_LIVELLO_PUNTI_FERITA = 'PH_LVL_PF'
const pfInput = ref<number | null>(null)
const pfModId = ref<number | null>(null)

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
    const abi = await getListaAbilitaPerPersonaggio(pid)
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
    // riga dedicata "Punti ferita": identificata univocamente dal placeholder (non da
    // nota/sempreAttivo, che potrebbero coincidere con un modificatore aggiunto a mano).
    // Va individuata ed esclusa PRIMA di popolare modificatoriLiberi, altrimenti comparirebbe
    // anche nell'editor generico "Modificatori" qui sotto.
    const pfMod = (props.item.modificatori ?? []).find((m: any) =>
        m.placeholder === PLACEHOLDER_LIVELLO_PUNTI_FERITA)
    if (pfMod) {
      pfModId.value = pfMod.id
      pfInput.value = Number(pfMod.valore)
    }

    modificatoriLiberi.value = (props.item.modificatori ?? [])
        .filter((m: any) => m.tipo !== 'BASE' && m.tipo !== 'RANK')
        .filter((m: any) => m.id !== pfModId.value)
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
      if (snap?.pfInput !== undefined) pfInput.value = snap.pfInput
      if (snap?.pfModId !== undefined) pfModId.value = snap.pfModId
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
    // idPersonaggio: stampa anche le eventuali SCELTA_<n>_FATTA di questo personaggio su questo
    // item Classe/Razza (vedi sezioniScelteClasse sotto) — senza, l'item tornerebbe con la sola
    // definizione globale delle Scelte, mai la scelta già fatta.
    const res = await getItem(id, personaggioId.value)
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
    // Un solo livello possibile (tipico delle RAZZA, che concedono tutto al livello 1): non ha
    // senso lasciarlo da selezionare a mano, non essendoci alternative tra cui scegliere.
    if (levels.length === 1) next[levels[0]] = true
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
    pfInput: pfInput.value,
    pfModId: pfModId.value,
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

/* Salvataggio — separato dall'emit così serve sia al Salva che chiude sia al floppy che resta */
async function salva(): Promise<boolean> {
  if (!canSave.value || personaggioId.value == null) return false
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
            .map(g => ({id: g.id, tipo: g.tipo, livello: g.livello, descrizione: g.descrizione, qty: g.qty ?? null})),
        ...extraItems.value
            .map(i => ({id: `item-${i.id}`, tipo: 'ITEM' as const, livello: 0, descrizione: i.nome})),
        // Scelte della Classe/Razza: il candidato scelto in una sezione va DAVVERO assegnato al
        // personaggio, non solo registrato come "scelta fatta" (SCELTA_<n>_FATTA, che resta solo
        // un marcatore su quale candidato è stato scelto) — stesso identico canale di
        // extraItems/selectedGrants sopra ("item-<id>", già gestito da ItemService.applyGrants),
        // niente da aggiungere lato backend.
        ...sezioniScelteClasse.value
            .filter(s => s.scelto != null)
            .map(s => s.candidati.find(c => c.id === s.scelto))
            .filter((c): c is { id: number; nome: string; tipo: string } => !!c)
            .map(c => ({id: `item-${c.id}`, tipo: 'ITEM' as const, livello: form.livello ?? 0, descrizione: c.nome})),
      ],
      modificatoriLiberi: [
        ...modificatoriLiberi.value
            .filter(m => m.statId && m.statId.trim())
            .map(m => ({
              id: m.id,
              statId: m.statId,
              tipo: m.tipo,
              valore: String(m.valore ?? ''),
              nota: m.nota ?? '',
              sempreAttivo: !!m.sempreAttivo,
            })),
        // Punti ferita: stesso meccanismo dei modificatori liberi, ma gestito qui a parte
        // (vedi pfInput sopra). Vuoto = nessuna riga inviata => se ne esisteva una, il backend
        // la elimina come qualunque modificatore libero non più presente nel payload.
        ...(pfInput.value != null && String(pfInput.value).trim() !== ''
            ? [{
              id: pfModId.value ?? undefined,
              statId: PF_STAT_ID,
              tipo: 'VALORE',
              valore: String(pfInput.value),
              nota: '',
              sempreAttivo: true,
              placeholder: PLACEHOLDER_LIVELLO_PUNTI_FERITA,
            }]
            : []),
      ]
    })
    await saveLivello(payload)
    return true
  } catch (e) {
    console.error('Errore salvataggio livello:', e)
    return false
  } finally {
    busy.value = false
  }
}

async function onSave() {
  if (await salva()) emit('saved')
}

// floppy: salva senza uscire dall'editor
const salvato = ref(false)
let timerSalvato: ReturnType<typeof setTimeout> | null = null

async function onSalvaResta() {
  if (!await salva()) return
  emit('savedResta', props.item)
  salvato.value = true
  if (timerSalvato) clearTimeout(timerSalvato)
  timerSalvato = setTimeout(() => salvato.value = false, 2000)
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
const classeSelezionata = computed(() => classeDetail.value?.nome || '—')
const sumClasseMaledizione = computed(() =>
    `Classe: ${classeSelezionata.value}${form.maledizioneNome ? ` | Maledizione: ${form.maledizioneNome}` : ''}`
)

// --- card "Scelte della Classe/Razza": stesso schema di Mobile_DettaglioItem.vue, qui applicato
// all'item Classe/Razza selezionato per questo livello invece che all'item aperto in inventario.
// Le sezioni (SCELTA_<n>_TITOLO/_CANDIDATI) sono globali sull'item, la scelta (SCELTA_<n>_FATTA)
// è scoped per questo personaggio — già "stampata" tra le label da loadClasseDetail sopra.
const sezioniScelteClasse = computed(() => {
  const labels = classeDetail.value?.labels ?? []
  const sezioni: { indice: number; titolo: string; candidati: { id: number; nome: string; tipo: string }[]; scelto: number | null }[] = []
  for (const l of labels) {
    const m = l.label?.match(/^SCELTA_(\d+)_CANDIDATI$/)
    if (!m) continue
    const n = Number(m[1])
    let candidati: { id: number; nome: string; tipo: string }[] = []
    try { candidati = JSON.parse(l.valore ?? '[]') } catch { candidati = [] }
    const titolo = labels.find((x: any) => x.label === `SCELTA_${n}_TITOLO`)?.valore ?? ''
    const fatta = labels.find((x: any) => x.label === `SCELTA_${n}_FATTA`)?.valore
    sezioni.push({indice: n, titolo, candidati, scelto: fatta ? Number(fatta) : null})
  }
  return sezioni.sort((a, b) => a.indice - b.indice)
})

// riga selezionabile (come le trasformazioni in Mobile_Cico_1_Info.vue): un tap sul già scelto lo
// deseleziona, su un altro candidato sposta la scelta lì (un solo scelto per sezione).
function toggleSceltaClasse(s: { indice: number; scelto: number | null }, candidatoId: number) {
  salvaSceltaClasse(s.indice, s.scelto === candidatoId ? null : candidatoId)
}

// popup "i" con il dettaglio del candidato — stesso componente/pattern di openInfoTrasf in
// Mobile_Cico_1_Info.vue, qui senza personaggio reale (il candidato è un item di compendio,
// non ancora posseduto: sola consultazione, come nel picker dell'albero grafico dei NODO).
const {openPopup} = usePopup()
function apriInfoCandidatoScelta(c: { id: number; nome: string; tipo: string }) {
  openPopup(
      markRaw(Mobile_DettaglioItem),
      {
        data: {
          item: {id: c.id, nome: c.nome, tipo: c.tipo},
          personaggio: {modificatori: {id: 0}, items: {trasformazioni: [], idoli: []}},
        },
        hideToggle: true,
      },
      {closable: true, autoClose: 0, title: c.nome},
  )
}

const savingScelteClasse = ref<Record<number, boolean>>({})

async function salvaSceltaClasse(sezioneIndice: number, sceltoId: number | null) {
  const idClasse = classeDetail.value?.id
  if (!idClasse || personaggioId.value == null || savingScelteClasse.value[sezioneIndice]) return
  savingScelteClasse.value = {...savingScelteClasse.value, [sezioneIndice]: true}
  try {
    await setScelta(idClasse, personaggioId.value, sezioneIndice, sceltoId)
    // aggiornamento ottimistico locale, stesso pattern di Mobile_DettaglioItem.vue
    const labels = classeDetail.value?.labels
    if (labels) {
      const key = `SCELTA_${sezioneIndice}_FATTA`
      const existing = labels.find((l: any) => l.label === key)
      if (sceltoId == null) {
        if (existing) classeDetail.value.labels = labels.filter((l: any) => l !== existing)
      } else if (existing) {
        existing.valore = String(sceltoId)
      } else {
        labels.push({id: -Date.now(), label: key, valore: String(sceltoId)})
      }
    }
  } catch (e) {
    console.error('Errore salvataggio scelta classe/razza:', e)
  } finally {
    savingScelteClasse.value = {...savingScelteClasse.value, [sezioneIndice]: false}
  }
}
</script>

<template>
  <form class="spell-editor" @submit.prevent="onSave">

    <TabLivelloBase
        :disabled="disabledAll"
        v-model:livello="form.livello"
        v-model:caratteristiche="form.caratteristiche"
        :summary="sumCar"
        :apri-caratteristiche="apriCaratteristiche"
    />

    <TabClasseMaledizione
        v-if="cards.has('LIVELLO_CLASSE_MALEDIZIONE')"
        :disabled="disabledAll"
        :search-classi="searchClassi"
        :classe-detail="classeDetail"
        :livelli-disponibili="livelliDisponibili"
        v-model:classe-id="form.classeId"
        v-model:maledizione-nome="form.maledizioneNome"
        v-model:livelli-classe="form.livelliClasse"
        :summary="sumClasseMaledizione"
        :show-maledizione="cards.has('LIVELLO_MALEDIZIONE')"
    />

    <div v-if="form.classeId && cards.has('LIVELLO_DV_PF_GRADI')" class="dv-row">
      <label class="dv-field">
        <span class="dv-lbl">Dadi vita</span>
        <input v-model.trim="form.dv" type="text" placeholder="Es.: 2d10" :disabled="disabledAll"/>
      </label>
      <label class="dv-field">
        <span class="dv-lbl">Punti ferita</span>
        <input v-model.number="pfInput" type="number" placeholder="—" :disabled="disabledAll"/>
      </label>
      <!-- Il valore continua a calcolarsi/congelarsi normalmente anche a campo nascosto
           (LIVELLO_GRADI gate solo la visibilità, non il calcolo — vedi gradiInput/budgetGradi). -->
      <label v-if="cards.has('LIVELLO_GRADI')" class="dv-field">
        <span class="dv-lbl">Gradi (punti abilità)</span>
        <input v-model.number="gradiInput" type="number" min="0" :disabled="disabledAll"/>
      </label>
    </div>

    <!-- Scelte definite sull'item Classe/Razza selezionato per questo livello: il personaggio le
         seleziona subito qui, invece di doverlo fare separatamente dal dettaglio dell'item. -->
    <TabExpandable v-if="cards.has('LIVELLO_SCELTE_CLASSE') && sezioniScelteClasse.length" title="Scelte della Classe/Razza" :defaultOpen="true">
      <template #summary>{{ sezioniScelteClasse.length }}</template>
      <template #content>
        <div v-for="s in sezioniScelteClasse" :key="s.indice" class="scelta-box">
          <span class="scelta-titolo">{{ s.titolo || `Scelta ${s.indice + 1}` }}</span>
          <div class="scelta-list">
            <div v-for="c in s.candidati" :key="c.id" class="scelta-riga" :class="{attiva: s.scelto === c.id}">
              <button type="button" class="scelta-toggle" :disabled="disabledAll || savingScelteClasse[s.indice]"
                      @click="toggleSceltaClasse(s, c.id)">
                <span class="dot">{{ s.scelto === c.id ? '●' : '○' }}</span>
                <span class="scelta-nome">{{ c.nome }}</span>
              </button>
              <button type="button" class="btn-info" :title="`Info: ${c.nome}`" @click.stop="apriInfoCandidatoScelta(c)">ⓘ</button>
            </div>
          </div>
        </div>
      </template>
    </TabExpandable>

    <TabContenutiLivello
        v-if="cards.has('LIVELLO_CONTENUTI')"
        :disabled="disabledAll"
        :loading="busy"
        :classe-id="form.classeId"
        :classe="classeDetail"
        :livello="item"
        :livelli-selezionati="livelliSelezionati"
        :default-open="true"
        :id-personaggio="personaggioId"
        v-model:selected-grant-ids="selectedGrantIds"
        v-model:selected-grants="selectedGrants"
    />

    <TabItemExtra
        v-if="cards.has('LIVELLO_ITEM_EXTRA')"
        :disabled="disabledAll"
        :loading="busy"
        :items="extraItems"
        :id-personaggio="personaggioId"
        @update:items="extraItems = $event"
        @create-new="onCreateExtra"
    />

    <TabExpandable v-if="cards.has('LIVELLO_MODIFICATORI')" title="Modificatori" :loading="busy">
      <template #summary>{{ modificatoriLiberi.filter(m => m.statId).length || '—' }}</template>
      <template #content>
        <ModificatoriEditor v-model="modificatoriLiberi" :disabled="disabledAll"/>
      </template>
    </TabExpandable>

    <TabAbilitaRanghi
        v-if="cards.has('LIVELLO_ABILITA_RANGHI')"
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
      <span v-if="salvato" class="salvato">Salvato</span>
      <button type="button" class="btn icona" :disabled="!canSave || personaggioId===null"
              title="Salva e resta qui" aria-label="Salva e resta qui"
              @click="onSalvaResta"><Icona name="SALVA"/></button>
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
.dv-row { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: .5rem; }
@media (max-width: 700px) { .dv-row { grid-template-columns: 1fr 1fr; } }
.dv-field { display: grid; gap: .3rem; }
.dv-lbl { font-size: .8rem; font-weight: 600; opacity: .85; }
.dv-field input {
  width: 100%; padding: .5rem .6rem; border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0);
  color: var(--text-strong);
}
.scelta-box { display: grid; gap: .35rem; margin-bottom: .75rem; }
.scelta-titolo { font-size: .85rem; font-weight: 600; }

/* Righe selezionabili, stesso stile delle trasformazioni in Mobile_Cico_1_Info.vue: pallino +
   nome cliccabili per scegliere, "i" a parte per il dettaglio — non un <select> generico. */
.scelta-list {
  border: 1px solid var(--hairline); border-radius: .5rem; overflow: hidden; background: var(--surface-0);
}
.scelta-riga { display: flex; align-items: center; border-bottom: 1px solid var(--hairline); }
.scelta-riga:last-child { border-bottom: 0; }
.scelta-riga.attiva { background: var(--info-bg); }

.scelta-toggle {
  flex: 1; display: flex; align-items: center; gap: .5rem;
  padding: .55rem .75rem; border: 0; background: transparent; cursor: pointer; text-align: left; min-width: 0;
}
.scelta-toggle:disabled { opacity: .55; cursor: default; }

.scelta-riga .dot { font-size: 1rem; color: var(--text-muted); flex-shrink: 0; width: 1rem; text-align: center; }
.scelta-riga.attiva .dot { color: var(--info-text); }

.scelta-nome { font-size: .9rem; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.scelta-riga.attiva .scelta-nome { font-weight: 700; color: var(--info-text); }

.scelta-riga .btn-info {
  flex-shrink: 0; padding: .55rem .75rem; border: 0; border-left: 1px solid var(--hairline);
  background: transparent; color: var(--text-muted); font-size: .9rem; cursor: pointer;
}
.scelta-riga .btn-info:hover { background: var(--info-bg); color: var(--info-text); }
.scelta-riga.attiva .btn-info { border-left-color: var(--info-border); }
.scelta-riga.attiva .btn-info:hover { background: var(--info-border); color: var(--info-text); }
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
  background: var(--surface-0);
  padding: .5rem 0 calc(.5rem + env(safe-area-inset-bottom, 0px));
  border-top: 1px solid var(--hairline);
  display: flex;
  justify-content: flex-end;
  gap: .5rem;
  align-items: center;
}
.btn {
  padding: .5rem .9rem;
  border-radius: .5rem;
  border: 1px solid transparent;
  cursor: pointer;
}
/* salva senza uscire: solo icona, per distinguerlo a colpo d'occhio dal Salva che chiude */
.btn.icona {
  border-color: var(--hairline); background: var(--surface-0); color: var(--text-strong);
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 2.4rem; padding: .5rem;
}
.btn.icona:hover:not(:disabled) { background: var(--btn-bg); }
.salvato {
  font-size: .8rem; font-weight: 600; color: var(--success-text);
  margin-right: auto; padding-left: .25rem;
}
.btn.ghost {
  border-color: var(--hairline);
  background: var(--surface-0);
  color: var(--text-strong);
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
