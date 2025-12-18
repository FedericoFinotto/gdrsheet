<script setup lang="ts">
import {computed, onMounted, reactive, ref, toRaw, watch} from 'vue'

import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {Classe} from '../../../../../../../models/dto/Classe'
import {Item} from '../../../../../../../models/dto/Item'
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
  getListaMaledizioniPerPersonaggio,
  saveLivello,
} from '../../../../../../../service/PersonaggioService'

import TabLivelloBase from './TabLivelloBase.vue'
import TabClasseMaledizione from './TabClasseMaledizione.vue'
import TabContenutiLivello from './TabContenutiLivello.vue'
import TabAbilitaRanghi from './TabAbilitaRanghi.vue'
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

const personaggioId = ref<Id | null>(null)
const classeDetail = ref<any | null>(null)

const form = reactive<UpdateLivelloRequest>({
  maledizioneId: undefined, tipoScelta: undefined,
  livello: null,
  caratteristiche: {FOR: null, DES: null, COS: null, INT: null, SAG: null, CAR: null},
  classeId: null,
  maledizioneNome: null,
  ranghi: {},
  livelliClasse: {}
});

const busy = ref(false)
const disabledAll = computed(() => !!props.readonly || busy.value)
const canSave = computed(() => !busy.value && !props.readonly && !!form.classeId)

/* Liste base */
const classi = ref<Classe[]>([])
const maledizioni = ref<Item[]>([])
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
const livelliSelezionati = computed<number[]>(() =>
    Object.entries(form.livelliClasse)
        .filter(([, v]) => !!v)
        .map(([k]) => Number(k))
        .filter(n => Number.isFinite(n))
        .sort((a, b) => a - b)
)
const totalPointsSpent = computed(
    () => Object.values(form.ranghi).reduce((a, b) => a + (Number(b) || 0), 0)
)
const showGradiTab = computed(
    () => !!form.classeId && !!gradiInfo.value && (gradiInfo.value!.toConsume > 0) && abilita.value.length > 0
)
const sumAbil = computed(() => {
  const total = gradiInfo.value?.toConsume ?? 0
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
    if (token === lastGradiReq) gradiInfo.value = unwrap<Gradi>(res)
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
  const wouldExceedBudget = (totalPointsSpent.value + 1) > gradiInfo.value.toConsume
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
  if (gradiInfo.value) {
    const overflow = totalPointsSpent.value - gradiInfo.value.toConsume
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

/* onMounted & watchers */
onMounted(async () => {
  try {
    busy.value = true
    form.livello = readItemLevel(props.item)

    const classeLabel = getItemLabel(props.item, LABELS.CLASSE)
    const maledizioneLabel = getItemLabel(props.item, LABELS.MALEDIZIONE)

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
    const [cls, mal, abi] = await Promise.all([
      getListaClassiPerPersonaggio(pid),
      getListaMaledizioniPerPersonaggio(pid),
      getListaAbilitaPerPersonaggio(pid)
    ])
    classi.value = unwrap<Classe[]>(cls) ?? []
    maledizioni.value = unwrap<Item[]>(mal) ?? []
    abilita.value = unwrap<Abilita[]>(abi) ?? []

    preloadAttualiDaStats()

    if (form.classeId) await loadClasseDetail(form.classeId)
  } catch (e) {
    console.error('Errore inizializzazione LivelloEditor:', e)
  } finally {
    busy.value = false
  }
})

async function loadClasseDetail(id: Id | null) {
  if (!id) {
    classeDetail.value = null
    selectedGrantIds.value.clear()
    selectedGrants.value.clear()
    return
  }
  try {
    const res = await getItem(id)
    classeDetail.value = unwrap<any>(res)

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

watch(() => form.classeId, async (id, prev) => {
  if (id === prev) return
  await loadClasseDetail(id)
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
    const labelsPatch: Record<string, string> = {
      LVL: form.livello == null ? '' : String(form.livello)
    }
    if (form.classeId) labelsPatch[LABELS.CLASSE] = String(form.classeId)
    if (form.maledizioneNome?.trim()) labelsPatch['MLDZ'] = form.maledizioneNome.trim()

    const payload = toRaw({
      livelloId: props.item.id,
      personaggioId: personaggioId.value,
      livello: form.livello,
      caratteristiche: cleanedCaratteristiche(form.caratteristiche),
      classeId: form.classeId,
      maledizioneNome: form.maledizioneNome,
      livelliClasse: livelliSelezionati.value,
      ranghi: Object.entries(form.ranghi)
          .filter(([, v]) => (Number(v) || 0) > 0)
          .map(([abilitaId, punti]) => ({abilitaId, punti: Number(punti)})),
      labelsPatch,
      // selectedGrantIds contiene sia ITEM che MOD; qui decidi come interpretarle
      grantsSelezionati: Array.from(selectedGrants.value)
    })
    await saveLivello(payload)
    // emit('saved')
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
const maledizioneSelezionata = computed(() => {
  if (!form.maledizioneNome) return '—'
  const m = maledizioni.value.find(m => m.nome === form.maledizioneNome)
  return m ? m.nome : form.maledizioneNome
})
const sumClasseMaledizione = computed(() =>
    `Classe: ${classeSelezionata.value}${form.maledizioneNome ? ` | Maledizione: ${maledizioneSelezionata.value}` : ''}`
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
        :maledizioni="maledizioni"
        :classe-detail="classeDetail"
        :livelli-disponibili="livelliDisponibili"
        v-model:classe-id="form.classeId"
        v-model:maledizione-nome="form.maledizioneNome"
        v-model:livelli-classe="form.livelliClasse"
        :summary="sumClasseMaledizione"
    />

    <TabContenutiLivello
        :disabled="disabledAll"
        :classe-id="form.classeId"
        :classe="classeDetail"
        :livello="item"
        :livelli-selezionati="livelliSelezionati"
        :default-open="true"
        v-model:selected-grant-ids="selectedGrantIds"
        v-model:selected-grants="selectedGrants"
    />

    <TabAbilitaRanghi
        v-if="showGradiTab"
        :disabled="disabledAll"
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
