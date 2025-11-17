<script setup lang="ts">
import {computed, defineComponent, h, onMounted, reactive, ref, toRaw, watch} from 'vue'
import TabExpandable from '../../../../../../components/TabExpandable.vue'
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
import {ItemDB, TIPO_ITEM} from '../../../../../../models/entity/ItemDB'
import {Classe} from '../../../../../../models/dto/Classe'
import {Item} from '../../../../../../models/dto/Item'
import {UpdateLivelloRequest} from '../../../../../../models/dto/UpdateLivelloRequest'
import {Abilita} from '../../../../../../models/dto/Abilita'
import {Gradi} from '../../../../../../models/dto/Gradi'
import {Rank} from '../../../../../../models/dto/Rank'
import {AbilitaClasse} from '../../../../../../models/dto/AbilitaClasse'
import {getItemLabel, LABELS} from '../../../../../../models/entity/ItemLabel'

/* ========= Sottocomponente locale per dettaglio mobile (render function, niente template runtime) ========= */
const Mobile_DettaglioItemLivello = defineComponent({
  name: 'Mobile_DettaglioItemLivello',
  props: {
    data: {type: Object, required: true}
  },
  setup(rawProps: any) {
    const levels = computed(() => (rawProps.data?.livelli ?? []).slice().sort((a: number, b: number) => a - b))
    return () =>
        h('div', {class: 'mdet'},
            levels.value.map((lv: number) =>
                h('div', {class: 'mdet-block', key: 'md-lv-' + lv}, [
                  h('div', {class: 'mdet-hd'}, `Livello ${lv}`),
                  h('ul', {class: 'mdet-list'},
                      (rawProps.data?.grantsByLevel?.[lv] ?? []).map((r: any) =>
                          h('li', {key: `md-r-${lv}-${r.id}`}, [
                            r.tipo === 'ABILITA'
                                ? h('span', {class: 'pill blue'}, 'ABILITA')
                                : h('span', {class: 'pill red'}, r.tipo),
                            h('span', {class: 'mdet-name', style: 'margin-left:.35rem'}, r.name)
                          ])
                      )
                  )
                ])
            )
        )
  }
})

/* ========= Tipi ========= */
type Id = number

interface Caratteristiche {
  FOR?: number | null;
  DES?: number | null;
  COS?: number | null;
  INT?: number | null;
  SAG?: number | null;
  CAR?: number | null
}
type TipoScelta = 'classe' | 'maledizione' | 'none'
type GrantRow = { id: number; name: string; tipo: string; livello: number }

/* ========= Props/Emits ========= */
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

/* ========= Abilità di classe ========= */
const abilitaClasse = ref<AbilitaClasse[]>([])
const abilitaClasseSet = computed(() => new Set(abilitaClasse.value.filter(x => x.diClasse || x.all).map(x => String(x.id).toLowerCase())))
const isClassSkill = (uid: string) => abilitaClasseSet.value.has(uid.toLowerCase())
const abilitaAltraClasseSet = computed(() => new Set(abilitaClasse.value.filter(x => !x.diClasse && !x.all).map(x => String(x.id).toLowerCase())))
const isAltraClassSkill = (uid: string) => abilitaAltraClasseSet.value.has(uid.toLowerCase())

/* ========= Attuali senza il livello corrente ========= */
const currentByUid = reactive<Record<string, number>>({})
function extractThisLevelRanks(a: Abilita, livelloItemId: number) {
  const all: Rank[] = Array.isArray(a?.rank?.ranks) ? (a!.rank!.ranks as Rank[]) : []
  const thisLevel = all.filter(r => r?.itemId === livelloItemId)
  const other = all.filter(r => r?.itemId !== livelloItemId)
  const thisLVL = thisLevel.reduce((s, r) => s + (r?.valore || 0), 0)
  const otherLVL = other.reduce((sum, r) => sum + (r ? (r.diClasse ? r.valore : r.valore / 2) : 0), 0)
  return {otherLVL, thisLVL}
}
function preloadAttualiDaStats() {
  const lvlId = Number(props.item?.id)
  Object.keys(currentByUid).forEach(k => delete currentByUid[k])
  Object.keys(form.ranghi).forEach(k => delete form.ranghi[k])
  for (const a of (abilita.value ?? [])) {
    const uid = abilUid(a);
    if (!uid) continue
    const {otherLVL, thisLVL} = extractThisLevelRanks(a, lvlId)
    currentByUid[uid] = Math.max(0, otherLVL)
    form.ranghi[uid] = thisLVL
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
  return arr.filter(a => a?.itemTarget?.tipo === 'AVANZAMENTO')
      .map(a => ({livello: toLevelNumber(a), item: a.itemTarget}))
      .filter(e => e.livello != null) as AvEntry[]
})
const livelliDisponibili = computed<number[]>(() => {
  const s = new Set<number>();
  avanzamentiClasse.value.forEach(e => s.add(e.livello));
  return Array.from(s).sort((a, b) => a - b)
})

/* ========= Selezione / UI ========= */
function pickTipo(tipo: TipoScelta) {
  form.tipoScelta = tipo
  if (tipo === 'classe') form.maledizioneId = null
  if (tipo !== 'classe') {
    form.classeId = null;
    classeDetail.value = null;
    form.livelliClasse = {}
    abilitaClasse.value = [];
    gradiInfo.value = null
    Object.keys(grantsByLevel).forEach(k => delete (grantsByLevel as any)[k])
    selectedGrantIds.value.clear()
  }
}

function isTipo(t: TipoScelta) {
  return form.tipoScelta === t
}

/* ========= Gradi da consumare (debounced) ========= */
const gradiInfo = ref<Gradi | null>(null)
const livelliSelezionati = computed<number[]>(() =>
    Object.entries(form.livelliClasse).filter(([, v]) => !!v).map(([k]) => Number(k)).sort((a, b) => a - b)
)
const dettaglioKey = computed(() => [personaggioId.value ?? 'nop', form.classeId ?? 'noclass', ...livelliSelezionati.value].join('-'))
const totalPointsSpent = computed(() => Object.values(form.ranghi).reduce((a, b) => a + (Number(b) || 0), 0))
const showGradiTab = computed(() => form.tipoScelta === 'classe' && !!gradiInfo.value && (gradiInfo.value!.toConsume > 0) && abilita.value.length > 0)
const sumAbil = computed(() => {
  const total = gradiInfo.value?.toConsume ?? 0;
  const used = totalPointsSpent.value;
  const max = gradiInfo.value?.max ?? 0
  return `${used}/${total} (max ${max})`
})

function debounce<T extends (...args: any[]) => any>(fn: T, wait = 200) {
  let t: any;
  return (...args: Parameters<T>) => {
    if (t) clearTimeout(t);
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
    gradiInfo.value = null;
    return
  }
  const levelsStr = livelliSelezionati.value.join(',')
  const token = ++lastGradiReq
  try {
    const res = await getGradiClasseByPersonaggioLivelloClasse(personaggioId.value, form.livello, form.classeId, levelsStr)
    if (token === lastGradiReq) gradiInfo.value = unwrap<Gradi>(res)
  } catch (e) {
    if (token === lastGradiReq) gradiInfo.value = null;
    console.error('Errore getGradiClasseByPersonaggioLivelloClasse:', e)
  }
}

watch(gradiKey, (key) => {
  if (!key) return;
  debouncedRefresh()
})

/* ========= Righe abilità ========= */
type SkillRow = {
  uid: string; name: string; isClass: boolean; isOtherClass: boolean; spent: number; effect: number; current: number;
  total: number; max: number
}
const rows = computed<SkillRow[]>(() => {
  return (abilita.value ?? []).map(a => {
    const uid = abilUid(a);
    const name = abilName(a);
    const isClass = isClassSkill(uid);
    const isOtherClass = isAltraClassSkill(uid)
    const spent = Number(form.ranghi[uid] ?? 0)
    const effect = isClass ? spent : Math.floor(spent / 2)
    const current = Number(currentByUid[uid] ?? 0)
    const total = current + effect
    const max = isClass || isOtherClass ? gradiInfo.value?.max ?? Infinity : Math.floor((gradiInfo.value?.max ?? Infinity) / 2)
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
  const r = rows.value.find(x => x.uid === uid);
  if (!r || !canInc(r)) return;
  form.ranghi[uid] = (form.ranghi[uid] ?? 0) + 1
}

function dec(uid: string) {
  form.ranghi[uid] = Math.max(0, (form.ranghi[uid] ?? 0) - 1)
}
function onDirectChange(uid: string, val: string) {
  const n = Math.max(0, Math.floor(Number(val)));
  form.ranghi[uid] = n
  if (gradiInfo.value) {
    const overflow = totalPointsSpent.value - gradiInfo.value.toConsume;
    if (overflow > 0) form.ranghi[uid] = Math.max(0, n - overflow)
  }
  const a = abilita.value.find(x => abilUid(x) === uid)
  if (a && gradiInfo.value) {
    const isClass = isClassSkill(uid);
    let s = form.ranghi[uid];
    const current = currentByUid[uid] ?? 0
    while (s > 0) {
      const e = isClass ? s : Math.floor(s / 2);
      if (current + e <= gradiInfo.value.max) break;
      s--
    }
    form.ranghi[uid] = s
  }
}

/* ========= Grant selezionabili per livello ========= */
const grantsByLevel = reactive<Record<number, GrantRow[]>>({})
const selectedGrantIds = ref<Set<number>>(new Set())

function groupAvanzamentiByLevel(detail: any) {
  const lista: any[] = Array.isArray(detail?.avanzamento) ? detail.avanzamento : []
  return lista.reduce((map, a) => {
    if (a?.itemTarget?.tipo !== 'AVANZAMENTO') return map
    const lv = Number.isFinite(Number(a?.livello)) ? Number(a.livello) : null
    if (lv == null) return map
    const arr = map.get(lv)
    arr ? arr.push(a) : map.set(lv, [a])
    return map
  }, new Map<number, any[]>())
}

async function toGrantsByLevel(avanzPerLv: Map<number, any[]>) {
  const out: Record<number, GrantRow[]> = {}
  for (const [lv, avanzList] of avanzPerLv) {
    const allRows: GrantRow[] = []
    await Promise.all(avanzList.map(async (a: any) => {
      let children: any[] = Array.isArray(a.itemTarget?.child) ? a.itemTarget.child : []
      if (!children.length) {
        try {
          const res = await getItem(a.itemTarget.id)
          const adv = unwrap<any>(res)
          children = Array.isArray(adv?.child) ? adv.child : []
        } catch { /* ignore */
        }
      }
      const rows: GrantRow[] = (children ?? [])
          .map((ch: any) => ch?.itemTarget)
          .filter(Boolean)
          .map((it: any) => ({
            id: Number(it.id),
            name: String(it.nome ?? it.titolo ?? it.label ?? `#${it.id}`),
            tipo: String(it.tipo ?? 'ITEM'),
            livello: lv
          }))
      allRows.push(...rows)
    }))
    out[lv] = allRows
  }
  return out
}

async function buildGrantsByLevelAsync(detail: any) {
  // reset reattivo
  Object.keys(grantsByLevel).forEach(k => delete (grantsByLevel as any)[k])
  selectedGrantIds.value.clear()
  if (!detail) return

  const byLv = groupAvanzamentiByLevel(detail)
  const grouped = await toGrantsByLevel(byLv)

  for (const [k, list] of Object.entries(grouped)) {
    grantsByLevel[Number(k)] = list
  }
  // preselezione coerente con livelli già spuntati
  for (const lv of livelliSelezionati.value) {
    for (const r of (grantsByLevel[lv] ?? [])) selectedGrantIds.value.add(r.id)
  }
}

function toggleGrant(id: number, checked: boolean) {
  const set = selectedGrantIds.value;
  if (checked) set.add(id); else set.delete(id)
}

function selectAllLevel(lv: number) {
  (grantsByLevel[lv] ?? []).forEach(r => selectedGrantIds.value.add(r.id))
}

function deselectAllLevel(lv: number) {
  (grantsByLevel[lv] ?? []).forEach(r => selectedGrantIds.value.delete(r.id))
}

/* ========= Caricamento & Reazioni ========= */
onMounted(async () => {
  try {
    busy.value = true
    form.livello = readItemLevel(props.item)
    for (const m of (props.item.modificatori ?? [])) if (m.tipo === 'BASE') (form.caratteristiche as any)[m.stat.id] = m.valore

    for (const ch of (props.item.child ?? [])) {
      if (ch.itemTarget.tipo === TIPO_ITEM.CLASSE) {
        form.tipoScelta = 'classe';
        form.classeId = ch.itemTarget.id
        const livelliClasse: string | null = getItemLabel(props.item, LABELS.CLASSE_LIVELLO);
        if (typeof livelliClasse === 'string' && livelliClasse.trim()) {
          const lvls = livelliClasse.split(',').map(s => Number(s.trim())).filter(Number.isFinite)
          for (const lv of lvls) form.livelliClasse[lv] = true
        }
      } else if (ch.itemTarget.tipo === TIPO_ITEM.MALEDIZIONE) {
        form.tipoScelta = 'maledizione';
        form.maledizioneId = ch.itemTarget.id
      }
    }

    const pid = unwrap<Id>(await getIdPersonaggioFromLivello(props.item.id));
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

    if (form.tipoScelta === 'classe' && form.classeId) await loadClasseDetail(form.classeId)
  } catch (e) {
    console.error('Errore inizializzazione LivelloEditor:', e)
  } finally {
    busy.value = false
  }
})

async function loadClasseDetail(id: Id | null) {
  if (!id) {
    classeDetail.value = null
    Object.keys(grantsByLevel).forEach(k => delete (grantsByLevel as any)[k])
    selectedGrantIds.value.clear()
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

    await buildGrantsByLevelAsync(classeDetail.value)

    if (gradiKey.value) debouncedRefresh()
  } catch (e) {
    classeDetail.value = null
    form.livelliClasse = {}
    Object.keys(grantsByLevel).forEach(k => delete (grantsByLevel as any)[k])
    selectedGrantIds.value.clear()
    console.error('Errore caricando dettaglio classe:', e)
  }
}

/* Reazioni */
watch(() => form.classeId, async (id, prev) => {
  if (form.tipoScelta !== 'classe' || id === prev) return
  await loadClasseDetail(id)
})
watch([abilita, () => props.item.id], () => {
  preloadAttualiDaStats()
}, {deep: true})
watch(livelliSelezionati, async (now, prev) => {
  const prevSet = new Set(prev ?? []), nowSet = new Set(now ?? [])
  now.forEach(lv => {
    if (!prevSet.has(lv)) (grantsByLevel[lv] ?? []).forEach(r => selectedGrantIds.value.add(r.id))
  })
  prev.forEach(lv => {
    if (!nowSet.has(lv)) (grantsByLevel[lv] ?? []).forEach(r => selectedGrantIds.value.delete(r.id))
  })
  if (gradiKey.value) debouncedRefresh()
})

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
      ranghi: Object.entries(form.ranghi)
          .filter(([, v]) => (Number(v) || 0) > 0)
          .map(([abilitaId, punti]) => ({abilitaId, punti: Number(punti)})),
      labelsPatch: {LVL: form.livello == null ? '' : String(form.livello)},
      sottoItemIds: Array.from(selectedGrantIds.value)
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

/* ========= Summary ========= */
const sumCar = computed(() => {
  const c = cleanedCaratteristiche(form.caratteristiche);
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
        <input type="number" inputmode="numeric" min="0" step="1" v-model.number="form.livello" :disabled="disabledAll"
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
            <input type="number" inputmode="numeric" min="0" step="1" :placeholder="'—'" :disabled="disabledAll"
                   v-model.number="(form.caratteristiche as any)[k]"/>
          </label>
        </div>
      </template>
    </TabExpandable>

    <!-- Classe / Maledizione -->
    <TabExpandable title="Classe / Maledizione" :defaultOpen="true">
      <template #summary><span class="wrap">{{ sumScelta }}</span></template>
      <template #content>
        <div class="row">
          <div class="toggle-row">
            <label class="toggle">
              <input type="radio" name="pick" value="classe" :checked="form.tipoScelta==='classe'"
                     @change="pickTipo('classe')" :disabled="disabledAll">
              <span>Classe</span>
            </label>
            <label class="toggle">
              <input type="radio" name="pick" value="maledizione" :checked="form.tipoScelta==='maledizione'"
                     @change="pickTipo('maledizione')" :disabled="disabledAll">
              <span>Maledizione</span>
            </label>
            <label class="toggle">
              <input type="radio" name="pick" value="none" :checked="form.tipoScelta==='none'"
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

    <!-- Info livelli -->
    <TabExpandable v-if="classeDetail" title="Info Livelli Classe" :defaultOpen="false">
      <template #summary></template>
      <template #content>
        <Mobile_DettaglioItemLivello :key="dettaglioKey" :data="{ livelli: livelliSelezionati, grantsByLevel }"/>
      </template>
    </TabExpandable>

    <!-- Contenuti del livello -->
    <TabExpandable v-if="form.tipoScelta==='classe' && livelliSelezionati.length && Object.keys(grantsByLevel).length"
                   title="Contenuti del livello" :defaultOpen="true">
      <template #summary>{{ selectedGrantIds.size }} selezionati</template>
      <template #content>
        <div class="grants">
          <div class="grant-block" v-for="lv in livelliSelezionati" :key="'gbl-'+lv">
            <div class="grant-header">
              <span class="lbl">Livello {{ lv }}</span>
              <div class="grant-actions">
                <button type="button" class="btn" @click="selectAllLevel(lv)" :disabled="disabledAll">Seleziona tutto
                </button>
                <button type="button" class="btn ghost" @click="deselectAllLevel(lv)" :disabled="disabledAll">Nessuno
                </button>
              </div>
            </div>
            <div class="grant-list">
              <label class="grant-row" v-for="g in grantsByLevel[lv] ?? []" :key="'g-'+lv+'-'+g.id">
                <input type="checkbox" :checked="selectedGrantIds.has(g.id)"
                       @change="toggleGrant(g.id, ($event.target as HTMLInputElement).checked)"
                       :disabled="disabledAll"/>
                <span class="pill blue" v-if="g.tipo==='ABILITA'">{{ g.tipo }}</span>
                <span class="pill red" v-else>{{ g.tipo }}</span>
                <span class="grant-name">{{ g.name }}</span>
              </label>
              <div v-if="!(grantsByLevel[lv]?.length)" class="muted">Nessun contenuto per questo livello.</div>
            </div>
          </div>
        </div>
      </template>
    </TabExpandable>

    <!-- Abilità & Ranghi -->
    <TabExpandable v-if="showGradiTab" title="Abilità & Ranghi" :defaultOpen="true">
      <template #summary>{{ sumAbil }}</template>
      <template #content>
        <div class="skills">
          <div class="skill-row" v-for="r in rows" :key="r.uid"
               v-show="!($props as any).showOnlyModifiedAbilita || r.spent!==0">
            <div class="skill-main">
              <div class="skill-badges">
                <span v-if="r.isClass" class="pill blue">CLASSE</span>
                <span v-else class="pill red">CROSS</span>
              </div>
              <div class="skill-name">
                <ins v-if="r.isClass || r.isOtherClass">{{ r.name }}</ins>
                <span v-else>{{ r.name }}</span>
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
                  <input type="number" inputmode="numeric" min="0" step="1" :value="r.spent"
                         @input="onDirectChange(r.uid, ($event.target as HTMLInputElement).value)"
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
                <div class="stat-value">{{ r.total }}/{{ r.max }}</div>
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

/* Skills */
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
  flex-direction: row;
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
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: .1rem .45rem;
  border-radius: .5rem;
  font-size: .8rem;
}

.pill.blue {
  background: #dbeafe;
  color: #1e3a8a;
}

.pill.red {
  background: #fee2e2;
  color: #b91c1c;
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

/* Azioni */
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

/* Grants */
.grants {
  display: grid;
  gap: .6rem;
}

.grant-block {
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  padding: .5rem;
  background: #fff;
}

.grant-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: .35rem;
}

.grant-actions {
  display: inline-flex;
  gap: .35rem;
}

.grant-list {
  display: grid;
  gap: .35rem;
}

.grant-row {
  display: grid;
  grid-template-columns: auto auto 1fr;
  gap: .5rem;
  align-items: center;
}

.grant-name {
  word-break: break-word;
}

/* Mobile dettaglio */
.mdet {
  display: grid;
  gap: .5rem;
}

.mdet-block {
  border: 1px dashed #e5e7eb;
  border-radius: .6rem;
  padding: .5rem;
}

.mdet-hd {
  font-weight: 600;
  margin-bottom: .25rem;
}

.mdet-list {
  margin: 0;
  padding-left: 1rem;
}

.mdet-list li {
  list-style: disc;
}
</style>
