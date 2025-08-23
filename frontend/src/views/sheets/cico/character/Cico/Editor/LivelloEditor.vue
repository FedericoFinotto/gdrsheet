<script setup lang="ts">
import {computed, onMounted, reactive, ref, toRaw, watch} from 'vue'
import {
  getAbilitaClasseByPersonaggioLivelloClasse,
  getIdPersonaggioFromLivello,
  getItem,
  getListaAbilitaPerPersonaggio,
  getListaClassiPerPersonaggio,
  getListaMaledizioniPerPersonaggio,
  saveLivello,
} from '../../../../../../service/PersonaggioService'
import TabExpandable from "../../../../../../components/TabExpandable.vue";
import {getItemLabel} from "../../../../../../function/Calcolo";
import Mobile_DettaglioItemLivello from "../../Dettaglio/Mobile_DettaglioItemLivello.vue";
import {AbilitaClasse} from "../../../../../../models/ItemDB";

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
  id: Id
  nome?: string
  modificatori?: Modificatore[]
  child?: ChildLink[]
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
  id: Id;
  nome?: string;
  abilita?: { nome?: string }
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
  ranghi: Record<Id, number>
  livelliClasse: Record<number, boolean> // selezione multipla per livello di classe
}

const props = defineProps<{ item: ItemDB; readonly?: boolean }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void }>()

/* ========= Stato ========= */
const personaggioId = ref<Id | null>(null);
const classeDetail = ref<any | null>(null);
const abilitaClasse = ref<AbilitaClasse[]>([]);

const form = reactive<LivelloDraft>({
  livello: null,
  caratteristiche: {FOR: null, DES: null, COS: null, INT: null, SAG: null, CAR: null},
  tipoScelta: 'none',
  classeId: null,
  maledizioneId: null,
  ranghi: {},
  livelliClasse: {}
})

const busy = ref<boolean>(false)
const disabledAll = computed(() => !!props.readonly || busy.value)
const canSave = computed(() => !busy.value && !props.readonly)

/* ========= Liste ========= */
const classi = ref<Classe[]>([])
const maledizioni = ref<Maledizione[]>([])
const abilita = ref<Abilita[]>([])
const filtroAbilita = ref('')

/* ========= Helpers ========= */
const abilitaNome = (a: Abilita) => (a.nome ?? a.abilita?.nome ?? '').trim()

const isAbilitaClasse = (a: Abilita) => (abilitaClasse.value?.map(x => x.id).includes(a.abilita.id));

const formulaRanghiDaConsumare = computed(() => {
  livelliSelezionati
})
const valoreRanghiDaConsumare = ref<number>(undefined);

const abilitaFiltrate = computed(() => {
  const f = filtroAbilita.value.trim().toLowerCase()
  if (!f) return abilita.value
  return abilita.value.filter(a => abilitaNome(a).toLowerCase().includes(f))
})

function pickTipo(tipo: TipoScelta) {
  form.tipoScelta = tipo
  if (tipo === 'classe') form.maledizioneId = null
  if (tipo === 'maledizione') {
    form.classeId = null;
    classeDetail.value = null;
    form.livelliClasse = {}
  }
  if (tipo === 'none') {
    form.classeId = null;
    form.maledizioneId = null;
    classeDetail.value = null;
    form.livelliClasse = {}
  }
}

function isTipo(t: TipoScelta) {
  return form.tipoScelta === t
}

function setRango(abilitaId: Id, delta: number) {
  const cur = form.ranghi[abilitaId] ?? 0
  form.ranghi[abilitaId] = Math.max(0, cur + delta)
}

function setRangoDirect(abilitaId: Id, v: string | number) {
  const n = Number(v)
  form.ranghi[abilitaId] = Number.isFinite(n) && n >= 0 ? n : 0
}

function cleanedCaratteristiche(src: Caratteristiche): Partial<Caratteristiche> {
  const out: Partial<Caratteristiche> = {}
  ;(['FOR', 'DES', 'COS', 'INT', 'SAG', 'CAR'] as (keyof Caratteristiche)[]).forEach(k => {
    const v = src[k]
    if (v !== null && v !== undefined && (v as any) !== '') out[k] = Number(v)
  })
  return out
}

function unwrap<T>(res: any): T {
  return (res && 'data' in res) ? res.data : res
}

/* ========= LVL (label livello item) ========= */
function readItemLevel(it: any): number | null {
  const raw = (getItemLabel(it, 'LVL') ?? '').toString().trim()
  if (!raw) return null
  const n = Number(raw)
  return Number.isFinite(n) ? n : null
}

/* ========= Avanzamenti classe ========= */
// best-effort parse del livello da un record avanzamento
function toLevelNumber(a: any): number | null {
  // 1) campo "livello" se presente
  if (a?.livello != null) {
    const n = Number(a.livello)
    if (Number.isFinite(n)) return n
  }
  // 2) label LVL sul nodo dell'avanzamento o sul suo itemTarget
  const viaSelf = readItemLevel(a)
  if (viaSelf != null) return viaSelf
  const viaTarget = readItemLevel(a?.itemTarget)
  if (viaTarget != null) return viaTarget
  return null
}

type AvEntry = { livello: number; item: any }
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
  const set = new Set<number>()
  avanzamentiClasse.value.forEach(e => set.add(e.livello))
  return Array.from(set).sort((a, b) => a - b)
})

// mantieni in sync la mappa di checkbox quando cambia la classe
watch(livelliDisponibili, (levels) => {
  const next: Record<number, boolean> = {}
  levels.forEach(lv => {
    next[lv] = form.livelliClasse[lv] ?? false
  })
  form.livelliClasse = next
})

/* ========= Abilità di classe (label ABCLASSE) ========= */
const abilitaDiClasse = computed<string[]>(() => {
  const it = classeDetail.value
  const raw = (getItemLabel(it, 'ABCLASSE') ?? '').toString()
  return raw.split(',').map(s => s.trim()).filter(Boolean)
})
const abilitaDiClasseSet = computed(() => new Set(abilitaDiClasse.value.map(n => n.toLowerCase())))

/* ========= Prefill + Caricamento ========= */
onMounted(async () => {
  try {
    busy.value = true

    // Prefill LVL da label dell'item livello
    form.livello = readItemLevel(props.item)

    // Prefill caratteristiche dai modificatori BASE
    for (const m of (props.item.modificatori ?? [])) {
      if (m.tipo === 'BASE') (form.caratteristiche as any)[m.stat.id] = m.valore
    }

    // Prefill scelta classe/maledizione dai child
    for (const ch of (props.item.child ?? [])) {
      if (ch.itemTarget.tipo === 'CLASSE') {
        form.tipoScelta = 'classe';
        form.classeId = ch.itemTarget.id
      } else if (ch.itemTarget.tipo === 'MALEDIZIONE') {
        form.tipoScelta = 'maledizione';
        form.maledizioneId = ch.itemTarget.id
      }
    }

    // 1) id personaggio dal livello
    const pid = unwrap<Id>(await getIdPersonaggioFromLivello(props.item.id))

    personaggioId.value = pid

    // 2) liste per quel personaggio
    const [cls, mal, abi] = await Promise.all([
      getListaClassiPerPersonaggio(pid),
      getListaMaledizioniPerPersonaggio(pid),
      getListaAbilitaPerPersonaggio(pid)
    ])
    classi.value = unwrap<Classe[]>(cls) ?? []
    maledizioni.value = unwrap<Maledizione[]>(mal) ?? []
    abilita.value = unwrap<Abilita[]>(abi) ?? []

    // Dettaglio classe se già selezionata
    if (form.tipoScelta === 'classe' && form.classeId) {
      await loadClasseDetail(form.classeId)
    }
  } catch (e) {
    console.error('Errore inizializzazione LivelloEditor:', e)
  } finally {
    busy.value = false
  }
})

/* ========= Watch: cambio classe -> carica dettaglio ========= */
async function loadClasseDetail(id: Id | null) {
  if (!id) {
    classeDetail.value = null;
    return
  }
  try {
    if (personaggioId.value !== undefined && personaggioId.value !== null) {
      getAbilitaClasseByPersonaggioLivelloClasse(personaggioId.value, form.livello, id).then(resp => {
            abilitaClasse.value = resp.data;
          }
      )
    }
    const res = await getItem(id)
    classeDetail.value = unwrap<any>(res)
    // aggiorna mappa livelli in base alla nuova classe
    const levels = livelliDisponibili.value
    const next: Record<number, boolean> = {}
    levels.forEach(lv => {
      next[lv] = form.livelliClasse[lv] ?? false
    })
    form.livelliClasse = next
  } catch (e) {
    console.error('Errore caricando dettaglio classe:', e)
    classeDetail.value = null
    form.livelliClasse = {}
  }
}

watch(() => form.classeId, async (id, prev) => {
  if (form.tipoScelta !== 'classe') return
  if (id === prev) return
  await loadClasseDetail(id)
})

const livelliSelezionati = computed<number[]>(() =>
    Object.entries(form.livelliClasse)
        .filter(([, v]) => !!v)
        .map(([k]) => Number(k))
        .sort((a, b) => a - b)
)

// includo nel key anche personaggio/classe così si rimonta pure quando cambiano
const dettaglioKey = computed(() => [
  personaggioId.value ?? 'nop',
  form.classeId ?? 'noclass',
  ...livelliSelezionati.value
].join('-'))

/* ========= Salvataggio ========= */
async function onSave() {
  if (!canSave.value || personaggioId.value == null) return
  try {
    busy.value = true

    const payload = toRaw({
      livelloId: props.item.id,
      personaggioId: personaggioId.value,
      livello: form.livello,                 // valore numerico del LVL (comodità)
      caratteristiche: cleanedCaratteristiche(form.caratteristiche),
      classeId: form.tipoScelta === 'classe' ? form.classeId : null,
      maledizioneId: form.tipoScelta === 'maledizione' ? form.maledizioneId : null,
      livelliClasse: livelliSelezionati,     // selezione multipla livelli classe
      ranghi: Object.entries(form.ranghi)
          .filter(([, v]) => (v ?? 0) > 0)
          .map(([abilitaId, valore]) => ({abilitaId: Number(abilitaId), ranghi: Number(valore)})),
      // in caso lato server tu gestisca patch di label:
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

function onCancel() {
  emit('cancel')
}

/* ========= Summaries ========= */
const sumCar = computed(() => {
  const c = cleanedCaratteristiche(form.caratteristiche)
  const parts: string[] = []
  ;(['FOR', 'DES', 'COS', 'INT', 'SAG', 'CAR'] as const).forEach(k => {
    const v = (c as any)[k];
    if (v != null) parts.push(`${k} ${v}`)
  })
  return parts.join(', ') || '—'
})
const sumScelta = computed(() => {
  if (form.tipoScelta === 'classe') return classi.value.find(c => c.id === form.classeId)?.nome || 'Classe: —'
  if (form.tipoScelta === 'maledizione') return maledizioni.value.find(m => m.id === form.maledizioneId)?.nome || 'Maledizione: —'
  return 'Nessuna'
})
const sumAbil = computed(() => {
  const n = Object.values(form.ranghi).reduce((a, b) => a + (b ?? 0), 0)
  return n > 0 ? `${n} ranghi totali` : '—'
})
</script>

<template>
  <form class="spell-editor" @submit.prevent="onSave">
    <header class="sp-head">
      <h2>Livello</h2>
      <span class="muted">ID #{{ props.item.id }}</span>
    </header>

    <!-- LVL (sempre presente sull'item livello) -->
    <div class="row three">
      <label class="field">
        <span class="lbl">LVL</span>
        <input type="number" inputmode="numeric" min="0" step="1"
               v-model.number="form.livello"
               :disabled="disabledAll"
               placeholder="—"/>
      </label>
      <div class="field"></div>
      <div class="field"></div>
    </div>

    <!-- Caratteristiche -->
    <TabExpandable title="Caratteristiche (facoltative)" :defaultOpen="false" v-if="form.livello === 0">
      <template #summary>{{ sumCar }}</template>
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
      <template #summary>{{ sumScelta }}</template>
      <template #content>
        <div class="row">
          <div class="toggle-row" style="display:flex; gap:.75rem;">
            <label class="toggle" style="display:flex; gap:.35rem; align-items:center;">
              <input type="radio" name="pick" value="classe"
                     :checked="form.tipoScelta==='classe'"
                     @change="pickTipo('classe')" :disabled="disabledAll"> <span>Classe</span>
            </label>
            <label class="toggle" style="display:flex; gap:.35rem; align-items:center;">
              <input type="radio" name="pick" value="maledizione"
                     :checked="form.tipoScelta==='maledizione'"
                     @change="pickTipo('maledizione')" :disabled="disabledAll"> <span>Maledizione</span>
            </label>
            <label class="toggle" style="display:flex; gap:.35rem; align-items:center;">
              <input type="radio" name="pick" value="none"
                     :checked="form.tipoScelta==='none'"
                     @change="pickTipo('none')" :disabled="disabledAll"> <span>Nessuna</span>
            </label>
          </div>
        </div>

        <div class="row three">
          <label class="field" v-if="form.tipoScelta==='classe'">
            <span class="lbl">Classe</span>
            <select v-model="form.classeId" :disabled="disabledAll || !isTipo('classe')">
              <option :value="null">—</option>
              <option v-for="c in classi" :key="c.id" :value="c.id">{{ c.nome }}</option>
            </select>
          </label>

          <label class="field" v-if="form.tipoScelta==='maledizione'">
            <span class="lbl">Maledizione</span>
            <select v-model="form.maledizioneId" :disabled="disabledAll || !isTipo('maledizione')">
              <option :value="null">—</option>
              <option v-for="m in maledizioni" :key="m.id" :value="m.id">{{ m.nome }}</option>
            </select>
          </label>

          <div class="field"></div>
        </div>

        <!-- Selezione livelli di classe (checkbox multiple, wrap) -->
        <div v-if="form.tipoScelta==='classe' && classeDetail" class="levels-wrap">
          <div class="lbl" style="margin-bottom:.35rem;">Seleziona livelli di classe</div>
          <div class="levels-grid">
            <label v-for="lv in livelliDisponibili" :key="lv" class="level-pill">
              <input type="checkbox"
                     :disabled="disabledAll"
                     v-model="form.livelliClasse[lv]"/>
              <span>{{ lv }}</span>
            </label>
          </div>
        </div>

        <!-- Avanzamenti della classe (solo gli item AVANZAMENTO) -->
        <div>
          <Mobile_DettaglioItemLivello
              :key="dettaglioKey"
              :data="{idPersonaggio: personaggioId, livello: livelliSelezionati, entity: classeDetail}"/>
        </div>
      </template>
    </TabExpandable>

    <!-- Abilità & ranghi -->
    <TabExpandable title="Abilità & Ranghi" :defaultOpen="true">
      <template #summary>{{ sumAbil }}</template>
      <template #content>
        <div class="row">
          <label class="field">
            <span class="lbl">Filtro</span>
            <input type="text" v-model.trim="filtroAbilita" :disabled="disabledAll" placeholder="Filtra abilità…"/>
          </label>
        </div>

        <div class="abilities-list">
          <div v-for="a in abilitaFiltrate" :key="a.id" class="abil-row">
            <div class="abil-name"
                 :class="{ isClassSkill: abilitaDiClasseSet.has(abilitaNome(a).toLowerCase()) }">
              <strong v-if="isAbilitaClasse(a)">{{ abilitaNome(a) }}</strong>
              <span v-if="!isAbilitaClasse(a)">{{ abilitaNome(a) }}</span>
            </div>
            <div class="counter">
              <button type="button" class="btn"
                      @click.stop="setRango(a.id, -1)"
                      :disabled="disabledAll || (form.ranghi[a.id] ?? 0) <= 0">−
              </button>
              <input type="number" inputmode="numeric" min="0" step="1"
                     :disabled="disabledAll"
                     :value="form.ranghi[a.id] ?? 0"
                     @input="setRangoDirect(a.id, ($event.target as HTMLInputElement).value)"/>
              <button type="button" class="btn"
                      @click.stop="setRango(a.id, +1)"
                      :disabled="disabledAll">+
              </button>
            </div>
          </div>
        </div>
      </template>
    </TabExpandable>

    <!-- Azioni -->
    <div class="actions">
      <button type="button" class="btn ghost" @click="onCancel" :disabled="busy">Annulla</button>
      <button type="submit" class="btn primary" :disabled="!canSave || personaggioId===null">Salva</button>
    </div>
  </form>
</template>

<style scoped>
/* base layout */
.spell-editor {
  display: flex;
  flex-direction: column;
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
  margin: 0;
}

.lbl {
  font-size: .8rem;
  font-weight: 600;
  opacity: .85;
  margin: 0;
}

input[type="text"], input[type="number"], select {
  width: 100%;
  padding: .5rem .6rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  background: #fff;
  margin: 0;
}

/* abilità & counter */
.abilities-list {
  display: grid;
  gap: .35rem;
}

.abil-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: .5rem;
  align-items: center;
  border: 1px solid #e5e7eb;
  border-radius: .5rem;
  padding: .35rem .5rem;
}

.abil-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.isClassSkill {
  font-weight: 600;
}

.counter {
  display: inline-flex;
  align-items: center;
  gap: .35rem;
}

.counter .btn {
  border: 1px solid #d1d5db;
  background: #fff;
  border-radius: .4rem;
  padding: .25rem .5rem;
  cursor: pointer;
}

.counter .btn:disabled {
  opacity: .6;
  cursor: default;
}

.counter input[type="number"] {
  width: 80px;
  text-align: center;
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

/* tabella avanzamenti */
.state {
  padding: .5rem;
  border: 1px dashed #e5e7eb;
  border-radius: .5rem;
}

.avz-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: .35rem;
}

.avz-table th, .avz-table td {
  border: 1px solid #e5e7eb;
  padding: .35rem .5rem;
  text-align: left;
}

.lv-col {
  width: 90px;
  white-space: nowrap;
}

.pill {
  display: inline-block;
  margin: 0 .25rem .25rem 0;
  padding: .1rem .45rem;
  border-radius: .5rem;
  background: #eef2ff;
  color: #3730a3;
  font-size: .85rem;
}

/* actions sticky */
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
