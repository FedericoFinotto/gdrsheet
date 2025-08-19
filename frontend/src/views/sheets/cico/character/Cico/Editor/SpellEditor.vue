<script setup lang="ts">
import {computed, onMounted, reactive, ref, toRaw} from 'vue'
import {saveSpell} from '../../../../../../service/PersonaggioService'

/* ========= tipi minimi ========= */
interface ItemLabel {
  label: string;
  valore?: string | null
}

interface ItemDB {
  id: number
  tipo: string
  nome?: string
  descrizione?: string
  labels?: ItemLabel[]
  componenti?: string[]
  tempo?: string
  ts?: string
  range?: string
  durata?: string
}

/* ========= props/emits ========= */
const props = defineProps<{ item: ItemDB; readonly?: boolean }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void }>()

/* ========= mapping EN -> IT ========= */
const MAP_SCHOOL: Record<string, string> = {
  Abjuration: 'Abiurazione', Conjuration: 'Evocazione', Divination: 'Divinazione',
  Enchantment: 'Ammaliamento', Evocation: 'Invocazione', Illusion: 'Illusione',
  Necromancy: 'Necromanzia', Transmutation: 'Trasmutazione', Universal: 'Universale'
}
const MAP_SUB: Record<string, string> = {
  Calling: 'Richiamo', Creation: 'Creazione', Summoning: 'Evocazione',
  Teleportation: 'Teletrasporto', Healing: 'Guarigione', Scrying: 'Scrutamento',
  Polymorph: 'Metamorfosi', Charm: 'Charme', Compulsion: 'Compulsione',
  Figment: 'Immagine', Glamer: 'Camuffamento', Pattern: 'Trama',
  Phantasm: 'Fantasma', Shadow: 'Ombra'
}
const MAP_DESC: Record<string, string> = {
  Acid: 'Acido', Air: 'Aria', Chaotic: 'Caotico', Cold: 'Freddo', Ice: 'Freddo',
  Darkness: 'Oscurità', Death: 'Morte', Earth: 'Terra', Electricity: 'Elettricità',
  Evil: 'Malvagio', Fear: 'Paura', Fire: 'Fuoco', Force: 'Forza', Good: 'Bene',
  'Language-Dependent': 'Dipendente dal linguaggio', Lawful: 'Legale',
  Light: 'Luce', 'Mind-Affecting': 'Influenza mentale', Sonic: 'Sonoro', Water: 'Acqua'
}
const DESC_ALIASES: Record<string, string> = {
  'mindaffecting': 'Mind-Affecting', 'mind affecting': 'Mind-Affecting',
  'language dependent': 'Language-Dependent', 'language-dependent': 'Language-Dependent'
}

/* set/lookup italiani per parsing IT */
const IT_SCHOOLS = Object.values(MAP_SCHOOL)
const IT_SUBS = Object.values(MAP_SUB)
const IT_DESCS = Object.values(MAP_DESC)
const LC_SCHOOL = new Map(IT_SCHOOLS.map(s => [s.toLowerCase(), s]))
const LC_SUB = new Map(IT_SUBS.map(s => [s.toLowerCase(), s]))
const LC_DESC = new Map(IT_DESCS.map(s => [s.toLowerCase(), s]))

/* set EN per validazione parser EN */
const SCHOOL_SET = new Set(Object.keys(MAP_SCHOOL))
const SUB_SET = new Set(Object.keys(MAP_SUB))
const DESC_SET = new Set(Object.keys(MAP_DESC))

/* ========= labels keys ========= */
const L = {
  SCUOLA: 'SCUOLA_SP',   // unico campo: può contenere EN (legacy) o IT (nuovo)
  TEMPO: 'TEMPO_SP',
  TS: 'TS_SP',
  RANGE: 'RANGE_SP',
  DURATA: 'DURATA_SP',
  COMP: 'COMP_SP'
} as const

/* ========= modello LOCALE ========= */
type ComponentKey =
    'V' | 'S' | 'M' | 'F' | 'DF' | 'XP' | 'X' | 'CORRUPT' | 'COLDFIRE' | 'FROSTFELL' | 'B'
type BoolMap = Record<string, boolean>
type SpellDraft = {
  nome: string
  descrizione: string
  scuole: BoolMap       // IT -> boolean
  subscuole: BoolMap    // IT -> boolean
  descrittori: BoolMap  // IT -> boolean
  tempo: string
  ts: string
  range: string
  durata: string
  comp: Record<ComponentKey, boolean>
}

function emptyBoolMap(list: string[]): BoolMap {
  return Object.fromEntries(list.map(s => [s, false])) as BoolMap
}

function emptyDraft(): SpellDraft {
  return {
    nome: '', descrizione: '',
    scuole: emptyBoolMap(IT_SCHOOLS),
    subscuole: emptyBoolMap(IT_SUBS),
    descrittori: emptyBoolMap(IT_DESCS),
    tempo: '', ts: 'Nessuno', range: '', durata: '',
    comp: {
      V: false,
      S: false,
      M: false,
      F: false,
      DF: false,
      XP: false,
      X: false,
      CORRUPT: false,
      COLDFIRE: false,
      FROSTFELL: false,
      B: false
    }
  }
}

const form = reactive<SpellDraft>(emptyDraft())

/* ========= helpers ========= */
function getLabel(labels: ItemLabel[] | undefined, key: string): string {
  const v = labels?.find(l => l.label === key)?.valore ?? ''
  return String(v ?? '').trim()
}

function splitTokens(s: string): string[] {
  return s.split(/[,/;]+|(?:\s+or\s+)|(?:\s+and\s+)/i).map(x => x.trim()).filter(Boolean)
}

function tcase(s: string) {
  return s.trim().replace(/\s+/g, ' ').replace(/(^|\s)\w/g, m => m.toUpperCase())
}

function normalizeDescToken(s: string): string {
  const raw = s.trim().toLowerCase().replace(/\s+/g, ' ')
  return DESC_ALIASES[raw] ?? tcase(raw)
}

/* ---- Parser SCUOLA_SP in Inglese -> IT arrays ---- */
function parseSchoolEN(line: string) {
  const raw = line.replace(/^"|"$/g, '').trim()
  if (!raw) return {schools: [] as string[], subs: [] as string[], desc: [] as string[]}

  const bracket = Array.from(raw.matchAll(/\[(.*?)\]/g)).map(m => m[1])
  const paren = Array.from(raw.matchAll(/\((.*?)\)/g)).map(m => m[1])

  const schoolPart = raw.replace(/\(.*?\)/g, '').replace(/\[.*?\]/g, '').trim()
  const schoolCandidates = schoolPart.split(/\s*\/\s*/).map(s => s.replace(/\d+$/, '').trim()).filter(Boolean)

  const schoolsIT: string[] = []
  for (const c of schoolCandidates) {
    const C = tcase(c)
    if (SCHOOL_SET.has(C)) schoolsIT.push(MAP_SCHOOL[C])
  }

  const subsIT: string[] = []
  const descIT: string[] = []

  for (const p of paren) {
    for (const tok of splitTokens(p)) {
      const T = tcase(tok)
      if (/^see text/i.test(T)) continue
      if (SUB_SET.has(T)) subsIT.push(MAP_SUB[T])
      else {
        const D = normalizeDescToken(T)
        if (DESC_SET.has(D)) descIT.push(MAP_DESC[D]) // es. "Evocation (Sonic)"
      }
    }
  }
  for (const b of bracket) {
    for (const tok of splitTokens(b)) {
      const D = normalizeDescToken(tok)
      if (/^see text/i.test(D)) continue
      if (DESC_SET.has(D)) descIT.push(MAP_DESC[D])
    }
  }

  return {
    schools: Array.from(new Set(schoolsIT)),
    subs: Array.from(new Set(subsIT)),
    desc: Array.from(new Set(descIT)),
  }
}

/* ---- Parser SCUOLA_SP già in Italiano -> IT arrays ---- */
function parseSchoolIT(line: string) {
  const raw = line.replace(/^"|"$/g, '').trim()
  if (!raw) return {schools: [] as string[], subs: [] as string[], desc: [] as string[]}

  const bracket = Array.from(raw.matchAll(/\[(.*?)\]/g)).map(m => m[1])
  const paren = Array.from(raw.matchAll(/\((.*?)\)/g)).map(m => m[1])

  const schoolPart = raw.replace(/\(.*?\)/g, '').replace(/\[.*?\]/g, '').trim()
  const schoolCandidates = schoolPart.split(/\s*\/\s*/).map(s => s.trim()).filter(Boolean)

  const schoolsIT: string[] = []
  for (const c of schoolCandidates) {
    const canon = LC_SCHOOL.get(c.toLowerCase())
    if (canon) schoolsIT.push(canon)
  }

  const subsIT: string[] = []
  const descIT: string[] = []

  const eat = (s: string) => splitTokens(s).forEach(tok => {
    const t = tok.toLowerCase()
    const sub = LC_SUB.get(t)
    const dsc = LC_DESC.get(t)
    if (sub) subsIT.push(sub)
    else if (dsc) descIT.push(dsc)
  })
  paren.forEach(eat)
  bracket.forEach(eat)

  return {
    schools: Array.from(new Set(schoolsIT)),
    subs: Array.from(new Set(subsIT)),
    desc: Array.from(new Set(descIT)),
  }
}

/* ---- Parser fallback: prova EN poi IT ---- */
function parseSchoolAny(line: string) {
  const en = parseSchoolEN(line)
  if (en.schools.length || en.subs.length || en.desc.length) return en
  return parseSchoolIT(line)
}

/* componenti da labels + fallback a item.componenti */
function parseComponenti(labels?: ItemLabel[], fromItem?: string[]) {
  const vals = (labels ?? []).filter(l => l.label === L.COMP).map(l => l.valore ?? '')
  const tokens = new Set<string>()
  for (const v of vals) splitTokens(v).forEach(t => tokens.add(t.toUpperCase()))
  if (fromItem?.length) fromItem.forEach(t => tokens.add(String(t).toUpperCase()))
  return tokens
}

/* ========= preload UNA VOLTA ========= */
onMounted(() => {
  Object.assign(form, emptyDraft())

  form.nome = props.item.nome ?? ''
  form.descrizione = props.item.descrizione ?? ''
  form.tempo = getLabel(props.item.labels, L.TEMPO) || (props.item.tempo ?? '')
  form.ts = getLabel(props.item.labels, L.TS) || (props.item.ts ?? 'Nessuno')
  form.range = getLabel(props.item.labels, L.RANGE) || (props.item.range ?? '')
  form.durata = getLabel(props.item.labels, L.DURATA) || (props.item.durata ?? '')

  // componenti
  const compTokens = parseComponenti(props.item.labels, props.item.componenti)
  ;(Object.keys(form.comp) as ComponentKey[]).forEach(k => {
    form.comp[k] = compTokens.has(k)
  })

  // scuole/sub/descrittori da SCUOLA_SP (EN o IT)
  const scuolaLine = getLabel(props.item.labels, L.SCUOLA)
  const parsed = parseSchoolAny(scuolaLine)
  // reset boolean map e set true per i riconosciuti
  for (const k of Object.keys(form.scuole)) form.scuole[k] = false
  for (const k of Object.keys(form.subscuole)) form.subscuole[k] = false
  for (const k of Object.keys(form.descrittori)) form.descrittori[k] = false
  parsed.schools.forEach(s => form.scuole[s] = true)
  parsed.subs.forEach(s => form.subscuole[s] = true)
  parsed.desc.forEach(d => form.descrittori[d] = true)
})

/* ========= UI state & azioni ========= */
const busy = ref(false)
const disabledAll = computed(() => !!props.readonly || busy.value)
const canSave = computed(() => form.nome.trim().length > 0 && !busy.value && !props.readonly)

function arrFromBoolMap(m: BoolMap) {
  return Object.keys(m).filter(k => m[k])
}

function buildScuolaStringIT(scuole: string[], sub: string[], desc: string[]) {
  const left = scuole.join('/')
  const mid = sub.length ? ` (${sub.join(', ')})` : ''
  const right = desc.length ? ` [${desc.join(', ')}]` : ''
  return `${left}${mid}${right}`.trim()
}

async function onSave() {
  if (!canSave.value) return
  try {
    busy.value = true

    const scuoleArr = arrFromBoolMap(form.scuole)
    const subsArr = arrFromBoolMap(form.subscuole)
    const descArr = arrFromBoolMap(form.descrittori)

    const payload = toRaw({
      nome: form.nome,
      descrizione: form.descrizione,
      tempo: form.tempo,
      ts: form.ts,
      range: form.range,
      durata: form.durata,
      componenti: (Object.entries(form.comp) as [ComponentKey, boolean][])
          .filter(([, v]) => v).map(([k]) => k),

      // campi strutturati italiani
      scuole: scuoleArr,
      subscuole: subsArr,
      descrittori: descArr,

      // opzionale: scrivi SCUOLA_SP in IT normalizzato
      labelsPatch: {[L.SCUOLA]: buildScuolaStringIT(scuoleArr, subsArr, descArr)}
    })

    await saveSpell(props.item.id, payload)
    emit('saved')
  } catch (e) {
    console.error('Errore salvataggio spell:', e)
  } finally {
    busy.value = false
  }
}

function onCancel() {
  emit('cancel')
}
</script>

<template>
  <form class="spell-editor" @submit.prevent="onSave">
    <header class="sp-head">
      <h2>Incantesimo</h2>
      <span class="muted">ID #{{ props.item.id }}</span>
    </header>

    <div class="row two">
      <label class="field">
        <span class="lbl">Nome</span>
        <input v-model.trim="form.nome" type="text" :disabled="disabledAll" required/>
      </label>
      <label class="field">
        <span class="lbl">TS</span>
        <select v-model="form.ts" :disabled="disabledAll">
          <option value="Nessuno">Nessuno</option>
          <option value="Tempra">Tempra</option>
          <option value="Riflessi">Riflessi</option>
          <option value="Volontà">Volontà</option>
        </select>
      </label>
    </div>

    <div class="row three">
      <label class="field">
        <span class="lbl">Tempo</span>
        <input v-model.trim="form.tempo" type="text" :disabled="disabledAll" placeholder="Azione standard / 1 round …"/>
      </label>
      <label class="field">
        <span class="lbl">Range</span>
        <input v-model.trim="form.range" type="text" :disabled="disabledAll"
               placeholder="Contatto / Vicino / Medio / Lungo …"/>
      </label>
      <label class="field">
        <span class="lbl">Durata</span>
        <input v-model.trim="form.durata" type="text" :disabled="disabledAll" placeholder="Istantanea / 1 round/liv …"/>
      </label>
    </div>

    <!-- Scuole -->
    <fieldset class="row components">
      <legend>Scuole</legend>
      <label v-for="s in IT_SCHOOLS" :key="s" class="chk">
        <input type="checkbox" v-model="form.scuole[s]" :disabled="disabledAll"/>
        <span class="chk-label">{{ s }}</span>
      </label>
    </fieldset>

    <!-- Sottoscuole -->
    <fieldset class="row components">
      <legend>Sottoscuole</legend>
      <label v-for="s in IT_SUBS" :key="s" class="chk">
        <input type="checkbox" v-model="form.subscuole[s]" :disabled="disabledAll"/>
        <span class="chk-label">{{ s }}</span>
      </label>
    </fieldset>

    <!-- Descrittori -->
    <fieldset class="row components">
      <legend>Descrittori</legend>
      <label v-for="d in IT_DESCS" :key="d" class="chk">
        <input type="checkbox" v-model="form.descrittori[d]" :disabled="disabledAll"/>
        <span class="chk-label">{{ d }}</span>
      </label>
    </fieldset>

    <!-- Componenti -->
    <fieldset class="row components">
      <legend>Componenti</legend>
      <label v-for="k in ['V','S','M','F','DF','XP','X','CORRUPT','COLDFIRE','FROSTFELL','B']" :key="k" class="chk">
        <input type="checkbox" v-model="form.comp[k as any]" :disabled="disabledAll"/>
        <span class="chk-label">{{ k }}</span>
      </label>
    </fieldset>

    <label class="field">
      <span class="lbl">Descrizione</span>
      <textarea v-model="form.descrizione" rows="16" :disabled="disabledAll" spellcheck="false"/>
    </label>

    <div class="actions">
      <button type="button" class="btn ghost" @click="onCancel" :disabled="busy">Annulla</button>
      <button type="submit" class="btn primary" :disabled="!canSave">Salva</button>
    </div>
  </form>
</template>

<style scoped>
.spell-editor {
  display: grid;
  gap: .75rem;
  margin: 0;
}

.sp-head {
  display: flex;
  align-items: baseline;
  gap: .5rem;
  margin: 0;
}

.sp-head h2 {
  margin: 0;
  font-size: 1rem;
}

.muted {
  opacity: .7;
  font-size: .85rem;
}

.row {
  display: grid;
  gap: .5rem;
}

.row.two {
  grid-template-columns: 1fr 12rem;
}

.row.three {
  grid-template-columns: 1fr 1fr 1fr;
}

@media (max-width: 720px) {
  .row.two, .row.three {
    grid-template-columns: 1fr;
  }
}

.field {
  display: grid;
  gap: .35rem;
  margin: 0;
}

.lbl {
  font-size: .8rem;
  font-weight: 600;
  opacity: .85;
  margin: 0;
}

input[type="text"], select, textarea {
  width: 100%;
  padding: .5rem .6rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  background: #fff;
  margin: 0;
}

textarea {
  resize: vertical;
  min-height: 16rem;
}

.components {
  margin: 0;
  border: 1px solid #e5e7eb;
  border-radius: .5rem;
  padding: .6rem;
}

.components legend {
  padding: 0 .25rem;
  font-weight: 600;
  margin: 0;
}

.chk {
  display: inline-flex;
  align-items: center;
  gap: .5rem;
  margin: 0 .75rem .5rem 0;
}

.chk input {
  accent-color: #2563eb;
}

.actions {
  position: sticky;
  bottom: 0;
  background: #fff;
  padding: .5rem 0 calc(.5rem + env(safe-area-inset-bottom, 0px));
  margin-top: .25rem;
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
  color: white;
}

.btn:disabled {
  opacity: .6;
  cursor: default;
}
</style>
