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
import {getItemLabel} from '../../../../../../function/Calcolo'
import {ItemDB} from "../../../../../../models/entity/ItemDB";
import {Classe} from "../../../../../../models/dto/Classe";
import {Item} from "../../../../../../models/dto/Item";
import {UpdateLivelloRequest} from "../../../../../../models/dto/UpdateLivelloRequest";
import {Abilita} from "../../../../../../models/dto/Abilita";
import {Gradi} from "../../../../../../models/dto/Gradi";

/* ========= Tipi ========= */
type Id = number

interface Caratteristiche {
  FOR?: number | null;
  DES?: number | null;
  COS?: number | null;
  INT?: number | null;
  SAG?: number | null;
  CAR?: number | null;
}

type TipoScelta = 'classe' | 'maledizione' | 'none'

const props = defineProps<{ item: ItemDB; readonly?: boolean }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void }>()

/* ========= Stato ========= */
const personaggioId = ref<Id | null>(null)
const classeDetail = ref<any | null>(null)

const form = reactive<UpdateLivelloRequest>({
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

/* ========= Liste ========= */
const classi = ref<Classe[]>([])
const maledizioni = ref<Item[]>([])
const abilita = ref<Abilita[]>([])

/* ========= Helpers ========= */
const abilUid = (a: Abilita) => String(a?.abilita?.id ?? '')
const abilName = (a: Abilita) => (a?.abilita?.nome ?? '').trim()

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

/* ========= Abilità di classe ========= */
const abilitaClasse = ref<{ id: string }[]>([])
const abilitaClasseSet = computed(() => new Set(abilitaClasse.value.map(x => String(x.id).toLowerCase())))
const isClassSkill = (uid: string) => abilitaClasseSet.value.has(uid.toLowerCase())

/* ========= “Attuale” senza il contributo del livello corrente ========= */
const currentByUid = reactive<Record<string, number>>({})

function extractThisLevelRanks(a: Abilita, livelloItemId: number) {
  const arr: any[] = Array.isArray(a?.rank?.ranks) ? (a!.rank!.ranks as any[]) : []
  const mine = arr.filter(r => r?.itemId === livelloItemId || r?.idItem === livelloItemId)
  const effect = mine.reduce((s, r) => s + (Number(r?.valore) || 0), 0)
  const spent = mine.reduce((s, r) => {
    const v = Number(r?.valore) || 0
    return s + (r?.diClasse ? v : v * 2)
  }, 0)
  return {effect, spent}
}

function preloadAttualiDaStats() {
  const lvlId = Number(props.item?.id)
  Object.keys(currentByUid).forEach(k => delete currentByUid[k])
  Object.keys(form.ranghi).forEach(k => delete form.ranghi[k])

  for (const a of (abilita.value ?? [])) {
    const uid = abilUid(a)
    if (!uid) continue
    const totalNow = Number(a?.rank?.modificatore ?? a?.rank?.valore ?? 0)
    const {effect, spent} = extractThisLevelRanks(a, lvlId)
    currentByUid[uid] = Math.max(0, totalNow - effect) // “Attuale” escluso questo livello
    form.ranghi[uid] = spent                           // pre-riempi i ± con i punti già spesi
  }
}

/* ========= Avanzamenti classe ========= */
function toLevelNumber(a: any): number | null {
  if (a?.livello != null && Number.isFinite(Number(a.livello))) return Number(a.livello)
  const viaSelf = readItemLevel(a);
  if (viaSelf != null) return viaSelf
  const viaTarget = readItemLevel(a?.itemTarget);
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
watch(livelliDisponibili, levels => {
  const next: Record<number, boolean> = {}
  levels.forEach(lv => {
    next[lv] = form.livelliClasse[lv] ?? false
  })
  form.livelliClasse = next
})

/* ========= Selezione / UI ========= */
function pickTipo(tipo: TipoScelta) {
  form.tipoScelta = tipo
  if (tipo === 'classe') form.maledizioneId = null
  if (tipo !== 'classe') {
    form.classeId = null
    classeDetail.value = null
    form.livelliClasse = {}
    abilitaClasse.value = []
    gradiInfo.value = null
  }
}

function isTipo(t: TipoScelta) {
  return form.tipoScelta === t
}

/* ========= Gradi da consumare ========= */
const gradiInfo = ref<Gradi | null>(null)
const livelliSelezionati = computed<number[]>(() =>
    Object.entries(form.livelliClasse).filter(([, v]) => !!v).map(([k]) => Number(k)).sort((a, b) => a - b)
)
const totalPointsSpent = computed(() =>
    Object.values(form.ranghi).reduce((a, b) => a + (Number(b) || 0), 0)
)
const showGradiTab = computed(() =>
    form.tipoScelta === 'classe' && !!gradiInfo.value && (gradiInfo.value!.toConsume > 0) && abilita.value.length > 0
)
const sumAbil = computed(() => {
  const total = gradiInfo.value?.toConsume ?? 0
  const used = totalPointsSpent.value
  const max = gradiInfo.value?.max ?? 0
  return `${used}/${total} (max ${max})`
})

let lastGradiReq = 0

async function refreshGradiInfo() {
  if (!personaggioId.value || !form.classeId || !form.livello) {
    gradiInfo.value = null;
    return
  }
  const levelsStr = livelliSelezionati.value.join(',')
  const token = ++lastGradiReq
  try {
    const res = await getGradiClasseByPersonaggioLivelloClasse(personaggioId.value, form.livello, form.classeId, levelsStr)
    if (token === lastGradiReq) gradiInfo.value = unwrap<GradiDTO>(res)
  } catch (e) {
    if (token === lastGradiReq) gradiInfo.value = null
    console.error('Errore getGradiClasseByPersonaggioLivelloClasse:', e)
  }
}

/* ========= Righe abilità per la UI ========= */
type SkillRow = {
  uid: string
  name: string
  isClass: boolean
  spent: number
  effect: number
  current: number
  total: number
}
const rows = computed<SkillRow[]>(() => {
  return (abilita.value ?? [])
      .map(a => {
        const uid = abilUid(a)
        const name = abilName(a)
        const isClass = isClassSkill(uid)
        const spent = Number(form.ranghi[uid] ?? 0)
        const effect = isClass ? spent : Math.floor(spent / 2)
        const current = Number(currentByUid[uid] ?? 0)
        const total = current + effect
        return {uid, name, isClass, spent, effect, current, total}
      })
      .sort((a, b) => a.name.localeCompare(b.name))
})

/* ========= Controlli ± con vincoli ========= */
function canInc(r: SkillRow): boolean {
  if (!gradiInfo.value) return true
  const nextSpent = r.spent + 1
  const nextEffect = r.isClass ? nextSpent : Math.floor(nextSpent / 2)
  const wouldExceedMax = (gradiInfo.value.max ?? Infinity) < (r.current + nextEffect)
  const wouldExceedBudget = (totalPointsSpent.value + 1) > gradiInfo.value.toConsume
  return !wouldExceedMax && !wouldExceedBudget
}

function inc(uid: string) {
  const r = rows.value.find(x => x.uid === uid)
  if (!r) return
  if (!canInc(r)) return
  form.ranghi[uid] = (form.ranghi[uid] ?? 0) + 1
}

function dec(uid: string) {
  form.ranghi[uid] = Math.max(0, (form.ranghi[uid] ?? 0) - 1)
}

// input manuale (punti spesi)
function onDirectChange(uid: string, val: string) {
  const n = Math.max(0, Math.floor(Number(val)))
  const before = form.ranghi[uid] ?? 0
  form.ranghi[uid] = n
  // clamp su budget
  if (gradiInfo.value) {
    const overflow = totalPointsSpent.value - gradiInfo.value.toConsume
    if (overflow > 0) form.ranghi[uid] = Math.max(0, n - overflow)
  }
  // clamp su max per abilità
  const a = abilita.value.find(x => abilUid(x) === uid)
  if (a && gradiInfo.value) {
    const isClass = isClassSkill(uid)
    const spent = form.ranghi[uid]
    const effect = isClass ? spent : Math.floor(spent / 2)
    const current = currentByUid[uid] ?? 0
    if (current + effect > gradiInfo.value.max) {
      // riduci finché rientra
      let s = spent
      while (s > 0) {
        const e = isClass ? s : Math.floor(s / 2)
        if (current + e <= gradiInfo.value.max) break
        s--
      }
      form.ranghi[uid] = s
    }
  }
}

/* ========= Prefill + Caricamento ========= */
onMounted(async () => {
  try {
    busy.value = true

    // LVL dall'item
    form.livello = readItemLevel(props.item)

    // Caratteristiche BASE
    for (const m of (props.item.modificatori ?? [])) {
      if (m.tipo === 'BASE') (form.caratteristiche as any)[m.stat.id] = m.valore
    }

    // Classe/Maledizione dal child
    for (const ch of (props.item.child ?? [])) {
      if (ch.itemTarget.tipo === 'CLASSE') {
        form.tipoScelta = 'classe';
        form.classeId = ch.itemTarget.id
      } else if (ch.itemTarget.tipo === 'MALEDIZIONE') {
        form.tipoScelta = 'maledizione';
        form.maledizioneId = ch.itemTarget.id
      }
    }

    // id personaggio
    const pid = unwrap<Id>(await getIdPersonaggioFromLivello(props.item.id))
    personaggioId.value = pid

    // liste
    const [cls, mal, abi] = await Promise.all([
      getListaClassiPerPersonaggio(pid),
      getListaMaledizioniPerPersonaggio(pid),
      getListaAbilitaPerPersonaggio(pid)
    ])
    classi.value = unwrap<Classe[]>(cls) ?? []
    maledizioni.value = unwrap<Item[]>(mal) ?? []
    abilita.value = unwrap<Abilita[]>(abi) ?? []

    // preload attuali/spesi
    preloadAttualiDaStats()

    // dettaglio classe + abilità di classe + gradi
    if (form.tipoScelta === 'classe' && form.classeId) {
      await loadClasseDetail(form.classeId)
      await refreshGradiInfo()
    }
  } catch (e) {
    console.error('Errore inizializzazione LivelloEditor:', e)
  } finally {
    busy.value = false
  }
})

async function loadClasseDetail(id: Id | null) {
  if (!id) {
    classeDetail.value = null;
    return
  }
  try {
    const res = await getItem(id)
    classeDetail.value = unwrap<any>(res)

    // abilità di classe (in base a pg/lvl/classe)
    if (personaggioId.value != null && form.livello != null) {
      const ac = await getAbilitaClasseByPersonaggioLivelloClasse(personaggioId.value, form.livello, id)
      abilitaClasse.value = unwrap<{ id: string }[]>(ac) ?? []
    }

    // setup livelli classe
    const levels = livelliDisponibili.value
    const next: Record<number, boolean> = {}
    levels.forEach(lv => {
      next[lv] = form.livelliClasse[lv] ?? false
    })
    form.livelliClasse = next
  } catch (e) {
    classeDetail.value = null
    form.livelliClasse = {}
    console.error('Errore caricando dettaglio classe:', e)
  }
}

/* Reazioni */
watch(() => form.classeId, async (id, prev) => {
  if (form.tipoScelta !== 'classe') return
  if (id === prev) return
  await loadClasseDetail(id)
  await refreshGradiInfo()
})
watch([livelliSelezionati, () => form.livello], () => {
  refreshGradiInfo()
})
watch([abilita, () => props.item.id], () => {
  preloadAttualiDaStats()
}, {deep: true})

/* ========= Salvataggio ========= */
async function onSave() {
  if (!canSave.value || personaggioId.value == null) return
  try {
    busy.value = true

    const payload = toRaw({
      livelloId: props.item.id,
      personaggioId: personaggioId.value,
      livello: form.livello,
      caratteristiche: cleanedCaratteristiche(form.caratteristiche),
      classeId: form.tipoScelta === 'classe' ? form.classeId : null,
      maledizioneId: form.tipoScelta === 'maledizione' ? form.maledizioneId : null,
      livelliClasse: livelliSelezionati.value,
      // invio id abilità STRING + punti spesi
      ranghi: Object.entries(form.ranghi)
          .filter(([, v]) => (Number(v) || 0) > 0)
          .map(([abilitaId, punti]) => ({abilitaId, punti: Number(punti)})),
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

/* ========= Summary Tabs ========= */
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
</script>

<template>
  <form class="spell-editor" @submit.prevent="onSave">
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

    <!-- Caratteristiche (solo livello 0) -->
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
      <template #summary>
        <span class="wrap">{{ sumScelta }}</span>
      </template>
      <template #content>
        <div class="row">
          <div class="toggle-row">
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
        </div>

        <div class="row three">
          <label class="field" v-if="form.tipoScelta==='classe'">
            <span class="lbl">Classe</span>
            <select v-model="form.classeId" :disabled="disabledAll || !isTipo('classe')">
              <option :value="null">—</option>
              <option v-for="c in classi" :key="'c-'+c.id" :value="c.id">{{ c.nome }}</option>
            </select>
          </label>

          <label class="field" v-if="form.tipoScelta==='maledizione'">
            <span class="lbl">Maledizione</span>
            <select v-model="form.maledizioneId" :disabled="disabledAll || !isTipo('maledizione')">
              <option :value="null">—</option>
              <option v-for="m in maledizioni" :key="'m-'+m.id" :value="m.id">{{ m.nome }}</option>
            </select>
          </label>

          <div class="field"></div>
        </div>

        <!-- Selezione livelli di classe -->
        <div v-if="form.tipoScelta==='classe' && classeDetail" class="levels-wrap">
          <div class="lbl" style="margin-bottom:.35rem;">Seleziona livelli di classe</div>
          <div class="levels-grid">
            <label v-for="lv in livelliDisponibili" :key="'lv-'+lv" class="level-pill">
              <input type="checkbox" :disabled="disabledAll" v-model="form.livelliClasse[lv]"/>
              <span>{{ lv }}</span>
            </label>
          </div>
        </div>
      </template>
    </TabExpandable>

    <!-- Abilità & Ranghi -->
    <TabExpandable v-if="showGradiTab" title="Abilità & Ranghi" :defaultOpen="true">
      <template #summary>{{ sumAbil }}</template>
      <template #content>
        <div class="box-info">
          <div><strong>Ranghi disponibili:</strong> {{ (gradiInfo!.toConsume - totalPointsSpent) }} /
            {{ gradiInfo!.toConsume }}
          </div>
          <div><strong>Max per abilità:</strong> {{ gradiInfo!.max }}</div>
          <div v-if="gradiInfo!.formule?.length"><strong>Formula(e):</strong> {{ gradiInfo!.formule.join(' + ') }}</div>
        </div>

        <div class="skills">
          <div class="skill-row" v-for="r in rows" :key="r.uid">
            <div class="skill-main">
              <div class="skill-name">{{ r.name }}</div>
              <div class="skill-badges">
                <span v-if="r.isClass" class="pill">di classe</span>
              </div>
            </div>

            <div class="skill-stats">
              <div class="stat">
                <div class="stat-label">Attuale</div>
                <div class="stat-value">{{ r.current }}</div>
              </div>

              <div class="stat">
                <div class="stat-label">Punti</div>
                <div class="counter">
                  <button type="button" class="btn" @click.stop="dec(r.uid)" :disabled="disabledAll || r.spent<=0">−
                  </button>
                  <input type="number" inputmode="numeric" min="0" step="1"
                         :value="r.spent" @input="onDirectChange(r.uid, ($event.target as HTMLInputElement).value)"
                         :disabled="disabledAll"/>
                  <button type="button" class="btn" @click.stop="inc(r.uid)" :disabled="disabledAll || !canInc(r)">+
                  </button>
                </div>
              </div>

              <div class="stat tight">
                <div class="stat-label">Effetto</div>
                <div class="stat-value">+{{ r.effect }}</div>
              </div>

              <div class="stat">
                <div class="stat-label">Totale</div>
                <div class="stat-value">{{ r.total }}</div>
              </div>
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
/* Layout base */
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
}

.wrap {
  white-space: normal;
  word-break: break-word;
}

/* Selettori */
.toggle-row {
  display: flex;
  gap: .75rem;
  flex-wrap: wrap;
}

.toggle {
  display: inline-flex;
  gap: .35rem;
  align-items: center;
}

/* Livelli classe */
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

/* Abilità & Ranghi */
.box-info {
  border: 1px dashed #e5e7eb;
  border-radius: .5rem;
  padding: .5rem .6rem;
  margin-bottom: .5rem;
  font-size: .92rem;
  background: #fafafa;
}

.skills {
  display: grid;
  gap: .45rem;
}

.skill-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: .5rem;
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  padding: .5rem .6rem;
  background: #fff;
}

@media (min-width: 700px) {
  .skill-row {
    grid-template-columns: 1fr auto;
    align-items: center;
  }
}

.skill-main {
  display: flex;
  flex-direction: column;
  gap: .25rem;
  min-width: 0;
}

.skill-name {
  font-weight: 600;
  line-height: 1.2;
  white-space: normal;
  word-break: break-word;
}

.skill-badges {
  display: flex;
  gap: .35rem;
  flex-wrap: wrap;
}

.pill {
  display: inline-block;
  padding: .1rem .45rem;
  border-radius: .5rem;
  background: #eef2ff;
  color: #3730a3;
  font-size: .8rem;
}

.skill-stats {
  display: grid;
  grid-auto-flow: column;
  gap: .6rem;
  align-items: center;
  justify-content: space-between;
}

@media (max-width: 699px) {
  .skill-stats {
    grid-auto-flow: column;
    justify-items: center;
  }
}

.stat {
  display: grid;
  gap: .2rem;
  justify-items: start;
}

.stat.tight .stat-value {
  min-width: 2.5rem;
  text-align: left;
}

.stat-label {
  font-size: .76rem;
  opacity: .75;
}

.stat-value {
  font-variant-numeric: tabular-nums;
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
  padding: .25rem .55rem;
  cursor: pointer;
  min-width: 2.1rem;
  min-height: 2.1rem;
}

.counter input[type="number"] {
  width: 3.2rem;
  text-align: center;
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
