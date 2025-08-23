<script setup lang="ts">
import {computed, onMounted, reactive, ref, toRaw, watch} from 'vue'
import {
  getAbilitaClasseByPersonaggioLivelloClasse,
  getGradiClasseByPersonaggioLivelloClasse,
  getIdPersonaggioFromLivello,
  getItem,
  getListaAbilitaPerPersonaggio,
  getListaClassiPerPersonaggio,
  getListaMaledizioniPerPersonaggio,
  saveLivello,
} from '../../../../../../service/PersonaggioService'
import TabExpandable from '../../../../../../components/TabExpandable.vue'
import Mobile_DettaglioItemLivello from '../../Dettaglio/Mobile_DettaglioItemLivello.vue'
import {getItemLabel} from '../../../../../../function/Calcolo'

/* ========= Tipi ========= */
type Id = number

interface Modificatore {
  tipo: string;
  stat: { id: string };
  valore: number
}

interface ChildLink {
  itemTarget: { id: Id; tipo: string; nome?: string; labels?: any[] }
}

interface ItemDB {
  id: Id;
  nome?: string;
  modificatori?: Modificatore[];
  child?: ChildLink[];
  labels?: any[]
}

interface Classe {
  id: Id;
  nome: string
}

interface Maledizione {
  id: Id;
  nome: string
}
interface Abilita {
  id: Id                           // id numerico record abilità (db)
  nome?: string
  valore?: number                  // se presente (rank corrente)
  ranks?: number                   // fallback
  gradi?: number                   // fallback
  abilita?: { id?: string | number; nome?: string } // id string UNIVOCO qui
}

interface GradiDTO {
  formule: string[];
  toConsume: number;
  max: number
}
interface Caratteristiche {
  FOR?: number | null;
  DES?: number | null;
  COS?: number | null;
  INT?: number | null;
  SAG?: number | null;
  CAR?: number | null;
}
type TipoScelta = 'classe' | 'maledizione' | 'none'

interface LivelloDraft {
  livello: number | null
  caratteristiche: Caratteristiche
  tipoScelta: TipoScelta
  classeId: Id | null
  maledizioneId: Id | null
  // ❗️Qui teniamo i PUNTI spesi per abilità, indicizzati per id string univoco (abilita.abilita.id)
  ranghi: Record<string, number>
  livelliClasse: Record<number, boolean>
}

/* ========= Props/emit ========= */
const props = defineProps<{ item: ItemDB; readonly?: boolean }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void }>()

/* ========= Stato base ========= */
const personaggioId = ref<Id | null>(null)
const classeDetail = ref<any | null>(null)

const form = reactive<LivelloDraft>({
  livello: null,
  caratteristiche: {FOR: null, DES: null, COS: null, INT: null, SAG: null, CAR: null},
  tipoScelta: 'none',
  classeId: null,
  maledizioneId: null,
  ranghi: {},
  livelliClasse: {}
})

const busy = ref(false)
const disabledAll = computed(() => !!props.readonly || busy.value)
const canSave = computed(() => !busy.value && !props.readonly)

/* ========= Dati da servizi ========= */
const classi = ref<Classe[]>([])
const maledizioni = ref<Maledizione[]>([])
const abilita = ref<Abilita[]>([])                // elenco abilità
const classSkillIds = ref<Set<string>>(new Set()) // set con id string univoci (lowercase)

/* ========= Util ========= */
function unwrap<T>(res: any): T {
  return (res && 'data' in res) ? res.data : res
}

function abilitaNome(a: Abilita) {
  return (a.nome ?? a.abilita?.nome ?? '').toString().trim()
}

function abilitaAttuale(a: Abilita) {
  return Number(a.valore ?? a.ranks ?? a.gradi ?? 0)
}

function abilUid(a: Abilita) {
  // id string univoco; se manca, fallback su id numerico convertito a string
  const raw = (a.abilita?.id ?? a.id)
  return String(raw)
}
function readItemLevel(it: any): number | null {
  const raw = (getItemLabel(it, 'LVL') ?? '').toString().trim()
  if (!raw) return null
  const n = Number(raw)
  return Number.isFinite(n) ? n : null
}

/* ========= Righe abilità ========= */
type AbRow = { uid: string; name: string; current: number; isClass: boolean }
const abilitaRows = computed<AbRow[]>(() => {
  const set = classSkillIds.value
  const rows = (abilita.value || []).map((a) => {
    const uid = abilUid(a)
    const isClass = set.has(uid.toLowerCase())
    return {
      uid,
      name: abilitaNome(a) || `Abilità ${uid}`,
      current: abilitaAttuale(a),
      isClass
    }
  })
  return rows.sort((a, b) => a.name.localeCompare(b.name, 'it'))
})

/* ========= Gradi (budget & cap) ========= */
const gradi = ref<GradiDTO | null>(null)
const gradiLoading = ref(false)

/* punti spesi totali (somma diretta dei punti aggiunti) */
const consumedPoints = computed(() =>
    Object.values(form.ranghi).reduce((a, b) => a + Math.max(0, b ?? 0), 0)
)
const remainingPoints = computed(() =>
    Math.max(0, (gradi.value?.toConsume ?? 0) - consumedPoints.value)
)

/* effetto in ranghi dell'input punti su una riga */
function effectRanks(uid: string, row?: AbRow): number {
  const addPts = Math.max(0, form.ranghi[uid] ?? 0)
  const isClass = row ? row.isClass : (abilitaRows.value.find(r => r.uid === uid)?.isClass ?? true)
  // classe 1:1, fuori classe 1:2
  return isClass ? addPts : addPts / 2
}

/* clamp input per riga: budget e cap finale */
function setRangoInput(uid: string, raw: number) {
  const row = abilitaRows.value.find(r => r.uid === uid)
  if (!row) return

  const req = Math.max(0, Math.floor(Number(raw) || 0))

  // 1) clamp su budget totale (toConsume)
  const others = consumedPoints.value - Math.max(0, form.ranghi[uid] ?? 0)
  const budget = Math.max(0, (gradi.value?.toConsume ?? 0) - others)
  let n = Math.min(req, budget)

  // 2) clamp su cap finale in ranghi: current + effect <= max
  const cap = (gradi.value?.max ?? Infinity) - row.current
  const maxPtsForCap = row.isClass ? cap : cap * 2 // per arrivare a cap, fuori classe servono 2 punti a rango
  n = Math.min(n, Math.max(0, Math.floor(maxPtsForCap)))

  form.ranghi[uid] = n
}

function adjustRango(uid: string, delta: 1 | -1) {
  setRangoInput(uid, (form.ranghi[uid] ?? 0) + delta)
}

/* ========= LVL & Avanzamenti (classe) ========= */
type AvEntry = { livello: number; item: any }
function toLevelNumber(a: any): number | null {
  if (a?.livello != null) {
    const n = Number(a.livello);
    if (Number.isFinite(n)) return n
  }
  const viaSelf = readItemLevel(a);
  if (viaSelf != null) return viaSelf
  const viaTarget = readItemLevel(a?.itemTarget);
  if (viaTarget != null) return viaTarget
  return null
}
const avanzamentiClasse = computed<AvEntry[]>(() => {
  const det = classeDetail.value
  const arr: any[] = det?.avanzamento ?? []
  if (!Array.isArray(arr) || arr.length === 0) return []
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
watch(livelliDisponibili, (levels) => {
  const next: Record<number, boolean> = {}
  levels.forEach(lv => {
    next[lv] = form.livelliClasse[lv] ?? false
  })
  form.livelliClasse = next
})
const livelliSelezionati = computed<number[]>(() =>
    Object.entries(form.livelliClasse).filter(([, v]) => !!v).map(([k]) => Number(k)).sort((a, b) => a - b)
)
const livelliKey = computed(() => livelliSelezionati.value.join(','))

/* ========= Tipo scelta ========= */
function pickTipo(tipo: TipoScelta) {
  form.tipoScelta = tipo
  if (tipo === 'classe') form.maledizioneId = null
  if (tipo !== 'classe') {
    form.classeId = null
    classeDetail.value = null
    form.livelliClasse = {}
    classSkillIds.value = new Set()
    gradi.value = null
    form.ranghi = {}
  }
}

function isTipo(t: TipoScelta) {
  return form.tipoScelta === t
}

/* ========= Caricamenti ========= */
async function loadClasseDetail(id: Id | null) {
  if (!id) {
    classeDetail.value = null;
    return
  }
  try {
    const res = await getItem(id)
    classeDetail.value = unwrap<any>(res)
    // risincronizza livelli
    const next: Record<number, boolean> = {}
    for (const lv of livelliDisponibili.value) next[lv] = form.livelliClasse[lv] ?? false
    form.livelliClasse = next
  } catch (e) {
    console.error('Errore caricamento item:', e)
    classeDetail.value = null
    form.livelliClasse = {}
  }
}

/* abilità di classe: set di id string (lowercase) */
async function refreshAbilitaClasse() {
  const pid = personaggioId.value
  const lvl = form.livello
  const cls = form.classeId
  if (!pid || lvl == null || !cls) {
    classSkillIds.value = new Set();
    return
  }
  try {
    const resp = await getAbilitaClasseByPersonaggioLivelloClasse(pid, lvl, cls)
    const list = unwrap<any[]>(resp) ?? []
    const set = new Set<string>()
    for (const it of list) {
      const id = it?.id != null ? String(it.id).toLowerCase() : ''
      if (id) set.add(id)
    }
    classSkillIds.value = set
    // reclamp coi nuovi fattori
    for (const row of abilitaRows.value) setRangoInput(row.uid, form.ranghi[row.uid] ?? 0)
  } catch (e) {
    console.error('Errore abilità di classe:', e)
    classSkillIds.value = new Set()
  }
}

/* gradi (budget e cap) */
let gradiReq = 0

async function refreshGradi() {
  const pid = personaggioId.value
  const lvl = form.livello
  const cls = form.classeId
  const lvKey = livelliKey.value
  if (!pid || lvl == null || !cls || !lvKey) {
    gradi.value = null;
    return
  }

  const ticket = ++gradiReq
  gradiLoading.value = true
  try {
    const resp = await getGradiClasseByPersonaggioLivelloClasse(pid, lvl, cls, lvKey)
    if (ticket !== gradiReq) return
    gradi.value = unwrap<GradiDTO>(resp)
    // reclamp tutto con il nuovo budget/cap
    for (const row of abilitaRows.value) setRangoInput(row.uid, form.ranghi[row.uid] ?? 0)
  } catch (e) {
    console.error('Errore getGradi:', e)
    if (ticket === gradiReq) gradi.value = null
  } finally {
    if (ticket === gradiReq) gradiLoading.value = false
  }
}

/* ========= Lifecycle ========= */
onMounted(async () => {
  try {
    busy.value = true

    // LVL dall'item
    form.livello = readItemLevel(props.item)
    // Caratteristiche base (solo per LVL 0)
    for (const m of (props.item.modificatori ?? [])) {
      if (m.tipo === 'BASE') (form.caratteristiche as any)[m.stat.id] = m.valore
    }
    // figlio classe/maledizione
    for (const ch of (props.item.child ?? [])) {
      if (ch.itemTarget.tipo === 'CLASSE') {
        form.tipoScelta = 'classe';
        form.classeId = ch.itemTarget.id
      } else if (ch.itemTarget.tipo === 'MALEDIZIONE') {
        form.tipoScelta = 'maledizione';
        form.maledizioneId = ch.itemTarget.id
      }
    }

    // personaggio id
    personaggioId.value = unwrap<Id>(await getIdPersonaggioFromLivello(props.item.id))

    // liste
    const [cls, mal, abi] = await Promise.all([
      getListaClassiPerPersonaggio(personaggioId.value!),
      getListaMaledizioniPerPersonaggio(personaggioId.value!),
      getListaAbilitaPerPersonaggio(personaggioId.value!)
    ])
    classi.value = unwrap<Classe[]>(cls) ?? []
    maledizioni.value = unwrap<Maledizione[]>(mal) ?? []
    abilita.value = unwrap<Abilita[]>(abi) ?? []

    // Dettaglio classe
    if (form.tipoScelta === 'classe' && form.classeId) {
      await loadClasseDetail(form.classeId)
    }
  } finally {
    busy.value = false
  }
})

/* reazioni */
watch(() => form.classeId, async (id, prev) => {
  if (form.tipoScelta !== 'classe') return
  if (id === prev) return
  await loadClasseDetail(id)
})

watch(
    [() => personaggioId.value, () => form.livello, () => form.classeId],
    async () => {
      await refreshAbilitaClasse();
      await refreshGradi()
    },
    {immediate: true}
)
watch(() => livelliKey.value, async () => {
  await refreshGradi()
})

/* ========= Salvataggio ========= */
async function onSave() {
  if (!canSave.value || personaggioId.value == null) return
  try {
    busy.value = true

    // prepara array con id STRING univoco & punti spesi > 0
    const ranghiArray = Object.entries(form.ranghi)
        .filter(([, v]) => (v ?? 0) > 0)
        .map(([abilitaIdStr, valore]) => ({
          abilitaId: abilitaIdStr,           // << id STRING univoco
          punti: Number(valore)              // punti spesi (non ranghi)
        }))

    const payload = toRaw({
      livelloId: props.item.id,
      personaggioId: personaggioId.value,
      livello: form.livello,
      caratteristiche: Object.fromEntries(
          Object.entries(form.caratteristiche).filter(([, v]) => v !== null && v !== undefined)
      ),
      classeId: form.tipoScelta === 'classe' ? form.classeId : null,
      maledizioneId: form.tipoScelta === 'maledizione' ? form.maledizioneId : null,
      livelliClasse: livelliSelezionati.value,
      ranghi: ranghiArray,
      labelsPatch: {LVL: form.livello == null ? '' : String(form.livello)}
    })

    await saveLivello(payload)
    emit('saved')
  } catch (e) {
    console.error('Errore salvataggio livello:', e)
  } finally {
    busy.value = false
  }
}

/* ========= Titolo Tab abilità ========= */
const abilTabTitleExtra = computed(() => {
  const t = gradi.value?.toConsume ?? 0
  return t ? `  ${consumedPoints.value}/${t} (max ${gradi.value?.max ?? 0})` : ''
})
</script>

<template>
  <form class="lvl-editor" @submit.prevent="onSave">
    <header class="sp-head">
      <h2>Livello</h2>
      <span class="muted">ID #{{ props.item.id }}</span>
    </header>

    <!-- LVL -->
    <div class="row three">
      <label class="field">
        <span class="lbl">LVL</span>
        <input type="number" inputmode="numeric" min="0" step="1"
               v-model.number="form.livello" :disabled="disabledAll" placeholder="—"/>
      </label>
      <div class="field"></div>
      <div class="field"></div>
    </div>

    <!-- Caratteristiche iniziali (solo LVL 0) -->
    <TabExpandable title="Caratteristiche (facoltative)" :defaultOpen="false" v-if="form.livello === 0">
      <template #summary>
        {{
          Object.entries(form.caratteristiche)
              .filter(([_, v]) => v !== null && v !== undefined)
              .map(([k, v]) => `${k} ${v}`).join(', ') || '—'
        }}
      </template>
      <template #content>
        <div class="row three">
          <label v-for="k in ['FOR','DES','COS','INT','SAG','CAR']" :key="k" class="field">
            <span class="lbl">{{ k }}</span>
            <input type="number" inputmode="numeric" min="0" step="1"
                   :placeholder="'—'" :disabled="disabledAll"
                   v-model.number="(form.caratteristiche as any)[k]"/>
          </label>
        </div>
      </template>
    </TabExpandable>

    <!-- Classe / Maledizione -->
    <TabExpandable title="Classe / Maledizione" :defaultOpen="true">
      <template #summary>
        {{
          form.tipoScelta === 'classe'
              ? (classi.find(c => c.id === form.classeId)?.nome || 'Classe: —')
              : form.tipoScelta === 'maledizione'
                  ? (maledizioni.find(m => m.id === form.maledizioneId)?.nome || 'Maledizione: —')
                  : 'Nessuna'
        }}
      </template>
      <template #content>
        <div class="pick-row">
          <label class="toggle">
            <input type="radio" name="pick" value="classe"
                   :checked="form.tipoScelta==='classe'"
                   @change="pickTipo('classe')" :disabled="disabledAll">
            <span>Classe</span>
          </label>
          <label class="toggle">
            <input type="radio" name="pick" value="maledizione"
                   :checked="form.tipoScelta==='maledizione'"
                   @change="pickTipo('maledizione')" :disabled="disabledAll">
            <span>Maledizione</span>
          </label>
          <label class="toggle">
            <input type="radio" name="pick" value="none"
                   :checked="form.tipoScelta==='none'"
                   @change="pickTipo('none')" :disabled="disabledAll">
            <span>Nessuna</span>
          </label>
        </div>

        <div class="row three">
          <label class="field" v-if="form.tipoScelta==='classe'">
            <span class="lbl">Classe</span>
            <select v-model="form.classeId" :disabled="disabledAll || !isTipo('classe')">
              <option :value="null">—</option>
              <option v-for="c in classi" :key="'cls-'+c.id" :value="c.id">{{ c.nome }}</option>
            </select>
          </label>

          <label class="field" v-if="form.tipoScelta==='maledizione'">
            <span class="lbl">Maledizione</span>
            <select v-model="form.maledizioneId" :disabled="disabledAll || !isTipo('maledizione')">
              <option :value="null">—</option>
              <option v-for="m in maledizioni" :key="'mal-'+m.id" :value="m.id">{{ m.nome }}</option>
            </select>
          </label>

          <div class="field"></div>
        </div>

        <!-- Selezione livelli di classe -->
        <div v-if="form.tipoScelta==='classe' && classeDetail" class="levels-wrap">
          <div class="lbl">Seleziona livelli di classe</div>
          <div class="levels-grid">
            <label v-for="lv in livelliDisponibili" :key="'lv-'+lv" class="level-pill">
              <input type="checkbox" :disabled="disabledAll" v-model="form.livelliClasse[lv]"/>
              <span class="wrap-name">{{ lv }}</span>
            </label>
          </div>
        </div>

        <!-- Avanzamenti -->
        <div v-if="form.tipoScelta==='classe' && classeDetail">
          <Mobile_DettaglioItemLivello
              :key="['det', personaggioId ?? 'nop', form.classeId ?? 'noclass', ...livelliSelezionati].join('-')"
              :data="{ idPersonaggio: personaggioId, livello: livelliSelezionati, entity: classeDetail }"/>
        </div>
      </template>
    </TabExpandable>

    <!-- Abilità & Ranghi -->
    <TabExpandable
        v-if="(gradiLoading || (gradi && gradi.toConsume > 0)) && abilitaRows.length"
        :title="`Abilità & Ranghi${abilTabTitleExtra}`"
        :defaultOpen="true">
      <template #summary>
        <template v-if="gradiLoading">Calcolo…</template>
        <template v-else>{{ consumedPoints }} / {{ gradi?.toConsume ?? 0 }} (max {{ gradi?.max ?? '—' }})</template>
      </template>
      <template #content>
        <div v-if="gradiLoading" class="state">Calcolo gradi…</div>
        <template v-else>
          <div class="gradi-box">
            <div><strong>Ranghi disponibili:</strong> {{ remainingPoints + consumedPoints }} / {{
                gradi?.toConsume ?? 0
              }}
            </div>
            <div><strong>Max ranghi per abilità:</strong> {{ gradi?.max ?? '—' }}</div>
            <div v-if="gradi?.formule?.length"><strong>Formula(e):</strong> {{ gradi.formule.join(' · ') }}</div>
          </div>

          <ul class="skills-list">
            <li v-for="row in abilitaRows" :key="'ab-'+row.uid" class="skill-row">
              <div class="skill-main">
                <div class="skill-name">
                  <span class="badge" :class="row.isClass ? 'class' : 'cross'">{{
                      row.isClass ? 'Classe' : 'Fuori'
                    }}</span>
                  <span class="name wrap-name">{{ row.name }}</span>
                </div>

                <div class="skill-values">
                  <div class="val">
                    <span class="lab">Attuale</span>
                    <span class="num">{{ row.current }}</span>
                  </div>

                  <div class="val add">
                    <button type="button" class="btn mini" @click.stop="adjustRango(row.uid, -1)"
                            :disabled="disabledAll || (form.ranghi[row.uid] ?? 0) <= 0">−
                    </button>
                    <input class="mini-input" type="number" inputmode="numeric" min="0" step="1"
                           :value="form.ranghi[row.uid] ?? 0"
                           :disabled="disabledAll"
                           @input="setRangoInput(row.uid, Number(($event.target as HTMLInputElement).value))"/>
                    <button type="button" class="btn mini" @click.stop="adjustRango(row.uid, +1)"
                            :disabled="disabledAll">+
                    </button>
                    <div class="subline">
                      <span>Effetto: +{{ effectRanks(row.uid, row) }}</span>
                      <span class="hint">({{ row.isClass ? '1:1' : '1:2' }})</span>
                    </div>
                  </div>

                  <div class="val total">
                    <span class="lab">Totale</span>
                    <span class="num">{{ (row.current + effectRanks(row.uid, row)) }}</span>
                  </div>
                </div>
              </div>
            </li>
          </ul>
        </template>
      </template>
    </TabExpandable>
    <div class="spazietto"/>
    <div class="spazietto"/>

    <!-- Azioni -->
    <div class="actions">
      <button type="button" class="btn ghost" @click="onCancel" :disabled="busy">Annulla</button>
      <button type="submit" class="btn primary" :disabled="!canSave || personaggioId===null">Salva</button>
    </div>
  </form>


</template>

<style scoped>
/* layout base */
.lvl-editor {
  display: flex;
  flex-direction: column;
  gap: .75rem;
  min-height: 100%;
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

/* form fields */
.row {
  display: grid;
  gap: .5rem;
}

.row.three {
  grid-template-columns: 1fr 1fr 1fr;
}

@media (max-width: 900px) {
  .row.three {
    grid-template-columns: 1fr;
  }
}

.field {
  display: grid;
  gap: .35rem;
}

.lbl {
  font-size: .8rem;
  font-weight: 600;
  opacity: .85;
}

input[type="text"], input[type="number"], select {
  width: 100%;
  padding: .5rem .6rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  background: #fff;
  margin: 0;
}

/* toggles */
.pick-row {
  display: flex;
  gap: .75rem;
  align-items: center;
  flex-wrap: wrap;
}

.toggle {
  display: inline-flex;
  gap: .35rem;
  align-items: center;
}

/* livelli classe */
.levels-wrap {
  margin-top: .5rem;
}

.levels-grid {
  display: flex;
  flex-wrap: wrap;
  gap: .4rem .5rem;
}
.level-pill {
  display: inline-flex;
  align-items: center;
  gap: .35rem;
  padding: .2rem .45rem;
  border: 1px solid #e5e7eb;
  border-radius: .5rem;
  background: #fff;
}

.level-pill input {
  accent-color: #2563eb;
}

/* stato */
.state {
  padding: .5rem;
  border: 1px dashed #e5e7eb;
  border-radius: .5rem;
}

/* box info gradi */
.gradi-box {
  border: 1px solid #e5e7eb;
  border-radius: .5rem;
  padding: .5rem .6rem;
  background: #fafafa;
  display: grid;
  gap: .25rem;
  margin-bottom: .5rem;
}

/* lista abilità (mobile first) */
.skills-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: .5rem;
}

.skill-row {
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  background: #fff;
  padding: .5rem .6rem;
}

.skill-main {
  display: grid;
  gap: .35rem;
}

.skill-name {
  display: flex;
  align-items: center;
  gap: .4rem;
  min-width: 0;
}

.wrap-name {
  white-space: normal;
  word-break: break-word;
}

/* << va a capo se lungo */
.name {
  font-weight: 600;
}

.badge {
  font-size: .70rem;
  padding: .05rem .4rem;
  border-radius: .4rem;
  border: 1px solid transparent;
}

.badge.class {
  background: #eef2ff;
  color: #3730a3;
  border-color: #c7d2fe;
}

.badge.cross {
  background: #fef3c7;
  color: #92400e;
  border-color: #fde68a;
}

.skill-values {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: .5rem;
}

.val {
  display: grid;
  gap: .2rem;
  justify-items: center;
}

.val .lab {
  font-size: .72rem;
  opacity: .75;
}

.val .num {
  font-variant-numeric: tabular-nums;
}

.val.add {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: .35rem;
}

.btn.mini {
  padding: .2rem .45rem;
  border: 1px solid #d1d5db;
  background: #fff;
  border-radius: .4rem;
  cursor: pointer;
}

.btn.mini:disabled {
  opacity: .6;
  cursor: default;
}

.mini-input {
  width: 64px;
  text-align: center;
  padding: .25rem .4rem;
  border: 1px solid #d0d5dd;
  border-radius: .4rem;
}

.subline {
  grid-column: 1 / -1;
  font-size: .8rem;
  opacity: .85;
  text-align: center;
}

.hint {
  opacity: .7;
  margin-left: .25rem;
}

.val.total .num {
  font-weight: 600;
}

/* actions sticky */
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
