<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {useAuthStore} from '../stores/auth'
import SearchSelect from '../components/SearchSelect.vue'
import {TIPO_STAT} from '../models/entity/Stat'
import {
  createStat,
  createStatDefault,
  deleteStatDefault,
  getMondiAdmin,
  getStatDefaults,
  getStatsAll,
  MondoOpt,
  StatDefaultRow,
  updateStatDefault,
} from '../service/StatAdminService'

const router = useRouter()
const auth = useAuthStore()
const isMasterOrAdmin = computed(() => {
  const r = (auth.utente?.ruolo ?? '').toUpperCase()
  return r === 'MASTER' || r === 'ADMIN' || r === 'SUPERUSER'
})

const TIPO_OPTS = Object.entries(TIPO_STAT).map(([k, v]) => ({value: v, label: `${v} — ${k.toLowerCase()}`}))

const stats = ref<any[]>([])
const mondi = ref<MondoOpt[]>([])
const defaults = ref<StatDefaultRow[]>([])
const mondoSel = ref<number | null>(null)
const errorMsg = ref<string | null>(null)
const busy = ref(false)
const filtro = ref('')

const nuovaStat = ref<{ id: string; tipo: string; label: string }>({id: '', tipo: 'AB', label: ''})
// Per il tipo AB, distingue Abilità generica / Conoscenza / Intrattenere / Artigianato /
// Professione: sono tutte tipo='AB' a livello di DB, si distinguono solo per convenzione
// sull'id (vedi gruppoDi più sotto). Conoscenza, Intrattenere e Artigianato hanno "figli"
// (più specializzazioni selezionabili), Professione è invece completamente a parte.
const sottoTipoAB = ref<'AB' | 'AB_CON' | 'AB_INT' | 'AB_ART' | 'AB_PROF'>('AB')
const SOTTOTIPO_AB_OPTS = [
  {value: 'AB', label: 'Abilità generica'},
  {value: 'AB_CON', label: 'Conoscenza'},
  {value: 'AB_INT', label: 'Intrattenere'},
  {value: 'AB_ART', label: 'Artigianato'},
  {value: 'AB_PROF', label: 'Professione'},
]

// card apribili/chiudibili
const aperti = ref<Set<string>>(new Set())
function toggleCard(k: string) {
  const s = new Set(aperti.value)
  s.has(k) ? s.delete(k) : s.add(k)
  aperti.value = s
}
const isAperta = (k: string) => aperti.value.has(k)

const mondoOptions = computed(() => mondi.value.map(m => ({value: m.id, label: m.descrizione})))
const statOptions = computed(() =>
    stats.value.map(s => ({value: s.id, label: `${s.label} (${s.id})`, hint: s.tipo})))

// mappa statId -> stat_default del mondo selezionato
const defaultByStat = computed<Record<string, StatDefaultRow>>(() => {
  const m: Record<string, StatDefaultRow> = {}
  for (const d of defaults.value) m[d.statId] = d
  return m
})

// righe: tutte le stat, con il flag "abilitata in questo mondo" e l'eventuale default
const righe = computed(() => {
  const q = filtro.value.trim().toLowerCase()
  return stats.value
      .filter(s => !q || s.label.toLowerCase().includes(q) || s.id.toLowerCase().includes(q))
      .map(s => ({stat: s, def: defaultByStat.value[s.id] ?? null}))
})

// etichette e ordine dei "gruppi" (le abilità sono divise in Abilità/Conoscenze/Intrattenere/
// Artigianato/Professioni)
const TIPO_LABEL: Record<string, string> = {
  CAR: 'Caratteristiche', AB: 'Abilità', AB_CON: 'Conoscenze', AB_INT: 'Intrattenere', AB_ART: 'Artigianato', AB_PROF: 'Professioni',
  TS: 'Tiri Salvezza', PF: 'Punti Ferita', ATT: 'Attributi', CA: 'Classe Armatura',
  ATK: 'Attacco', COUNT: 'Contatori', VALUTA: 'Valuta',
}
const TIPO_ORDINE = Object.keys(TIPO_LABEL)

// gruppo grafico di una stat (separa conoscenze/intrattenere/artigianato/professioni dalle abilità)
function gruppoDi(stat: any): string {
  if (stat.tipo !== 'AB') return stat.tipo ?? 'ALTRO'
  const id = String(stat.id ?? '').toUpperCase()
  const label = String(stat.label ?? '').toLowerCase()
  if (id.startsWith('PR') || label.startsWith('professione')) return 'AB_PROF'
  if (id.startsWith('CO') || label.startsWith('conoscenz')) return 'AB_CON'
  if (id.startsWith('IN') || label.startsWith('intrattenere')) return 'AB_INT'
  if (id.startsWith('AR') || label.startsWith('artigianato')) return 'AB_ART'
  return 'AB'
}

// Convenzione di numerazione per le famiglie "numerate": prefisso id, cifre di padding e primo
// numero usato (es. AB1..AB36, CO00..CO11, PR00..PR06 — dati letti dal seed esistente; IN e AR
// seguono la stessa convenzione a 2 cifre di CO/PR essendo anch'esse famiglie "con figli").
const CONFIG_FAMIGLIA: Record<string, { prefisso: string; padding: number; primo: number }> = {
  AB: {prefisso: 'AB', padding: 0, primo: 1},
  AB_CON: {prefisso: 'CO', padding: 2, primo: 0},
  AB_INT: {prefisso: 'IN', padding: 2, primo: 0},
  AB_ART: {prefisso: 'AR', padding: 2, primo: 0},
  AB_PROF: {prefisso: 'PR', padding: 2, primo: 0},
}

// famiglia scelta nel form "nuova stat" (per AB usa il sotto-tipo scelto, altrimenti il tipo stesso)
const famigliaSelezionata = computed(() => nuovaStat.value.tipo === 'AB' ? sottoTipoAB.value : nuovaStat.value.tipo)

// tutte le stat esistenti della famiglia selezionata
const statDellaFamiglia = computed(() => {
  const fam = famigliaSelezionata.value
  const conf = CONFIG_FAMIGLIA[fam]
  return stats.value.filter(s => {
    if (gruppoDi(s) !== fam) return false
    // scarta eventuali id "anomali" che non seguono il pattern prefisso+numero
    return !conf || new RegExp(`^${conf.prefisso}\\d+$`).test(String(s.id).toUpperCase())
  })
})

// l'ultima statistica della famiglia: per le famiglie numerate quella col numero più alto,
// altrimenti l'ultima della lista (nessun ordine "di creazione" disponibile, solo un riferimento)
const ultimaStatFamiglia = computed(() => {
  const lista = statDellaFamiglia.value
  if (lista.length === 0) return null
  const conf = CONFIG_FAMIGLIA[famigliaSelezionata.value]
  if (!conf) return lista[lista.length - 1]
  return [...lista].sort((a, b) => {
    const na = parseInt(String(a.id).toUpperCase().replace(conf.prefisso, ''), 10) || 0
    const nb = parseInt(String(b.id).toUpperCase().replace(conf.prefisso, ''), 10) || 0
    return nb - na
  })[0]
})

// id suggerito per la nuova stat: incrementato per le famiglie numerate (AB/CO/IN/AR/PR),
// altrimenti l'id dell'ultima stat della stessa famiglia (solo come riferimento/formato da modificare a mano)
const idSuggerito = computed(() => {
  const conf = CONFIG_FAMIGLIA[famigliaSelezionata.value]
  const ultimo = ultimaStatFamiglia.value
  if (!conf) return ultimo?.id ?? ''
  const n = ultimo ? (parseInt(String(ultimo.id).toUpperCase().replace(conf.prefisso, ''), 10) || 0) + 1 : conf.primo
  return conf.prefisso + String(n).padStart(conf.padding, '0')
})

// prevalorizza l'id ogni volta che cambia tipo/sotto-tipo (l'admin può comunque modificarlo a mano)
watch([() => nuovaStat.value.tipo, sottoTipoAB], () => {
  nuovaStat.value.id = idSuggerito.value
})

// righe raggruppate (una card per gruppo)
const gruppi = computed(() => {
  const map: Record<string, any[]> = {}
  for (const r of righe.value) {
    const t = gruppoDi(r.stat)
    ;(map[t] ??= []).push(r)
  }
  return Object.entries(map).sort((a, b) => {
    const ia = TIPO_ORDINE.indexOf(a[0]);
    const ib = TIPO_ORDINE.indexOf(b[0])
    return (ia < 0 ? 999 : ia) - (ib < 0 ? 999 : ib)
  })
})

async function loadStats() {
  stats.value = (await getStatsAll()).data ?? []
}
async function loadMondi() {
  mondi.value = (await getMondiAdmin()).data ?? []
}
async function loadDefaults() {
  if (mondoSel.value == null) { defaults.value = []; return }
  defaults.value = (await getStatDefaults(mondoSel.value)).data ?? []
}

onMounted(async () => {
  if (!isMasterOrAdmin.value) { router.replace('/'); return }
  try {
    await Promise.all([loadStats(), loadMondi()])
    nuovaStat.value.id = idSuggerito.value
  } catch (e: any) {
    errorMsg.value = e?.message ?? 'Errore di caricamento'
  }
})

watch(mondoSel, loadDefaults)

// abilita/disabilita una stat nel mondo
async function toggleAbilitata(statId: string, def: StatDefaultRow | null) {
  if (mondoSel.value == null || busy.value) return
  busy.value = true
  errorMsg.value = null
  try {
    if (def && def.id != null) {
      await deleteStatDefault(def.id)
    } else {
      await createStatDefault({mondoId: mondoSel.value, statId})
    }
    await loadDefaults()
  } catch (e: any) {
    errorMsg.value = e?.response?.data?.message ?? e?.message ?? 'Errore'
  } finally {
    busy.value = false
  }
}

// salva le modifiche di una stat_default abilitata
async function salvaDef(def: StatDefaultRow) {
  if (def.id == null || busy.value) return
  busy.value = true
  errorMsg.value = null
  try {
    await updateStatDefault(def.id, {
      mondoId: def.mondoId,
      statId: def.statId,
      valoreDefault: (def.valoreDefault ?? '') || null,
      defaultModId: def.defaultModId || null,
      addestramento: !!def.addestramento,
    })
    await loadDefaults()
  } catch (e: any) {
    errorMsg.value = e?.response?.data?.message ?? e?.message ?? 'Errore nel salvataggio'
  } finally {
    busy.value = false
  }
}

// nuova stat: la crea globalmente e la assegna subito al mondo selezionato
async function salvaStat() {
  errorMsg.value = null
  if (!nuovaStat.value.id.trim() || !nuovaStat.value.label.trim()) {
    errorMsg.value = 'Id e label obbligatori'
    return
  }
  busy.value = true
  try {
    await createStat({
      id: nuovaStat.value.id.trim(),
      tipo: nuovaStat.value.tipo,
      label: nuovaStat.value.label.trim(),
    })
    if (mondoSel.value != null) {
      await createStatDefault({mondoId: mondoSel.value, statId: nuovaStat.value.id.trim()})
    }
    nuovaStat.value.label = ''
    await loadStats()
    await loadDefaults()
    // ricalcola il prossimo id suggerito per la stessa famiglia, per aggiungerne altre in fila
    nuovaStat.value.id = idSuggerito.value
  } catch (e: any) {
    errorMsg.value = e?.response?.data?.message ?? e?.message ?? 'Errore nel salvataggio della stat'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="stats-admin">
    <header class="head">
      <button class="btn ghost" @click="router.back()">←</button>
      <h1>Statistiche & Default Mondo</h1>
    </header>

    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

    <!-- Selettore mondo + filtro -->
    <div class="card">
      <h2>Statistiche per mondo</h2>
      <div class="grid">
        <label class="field grow">
          <span class="lbl">Mondo</span>
          <SearchSelect v-model="mondoSel" :options="mondoOptions" placeholder="Seleziona un mondo…"/>
        </label>
        <label v-if="mondoSel != null" class="field grow">
          <span class="lbl">Filtra statistiche</span>
          <input v-model="filtro" type="text" placeholder="Cerca per nome o id…"/>
        </label>
      </div>
      <div v-if="mondoSel == null" class="muted">Seleziona un mondo per gestire le statistiche.</div>
    </div>

    <!-- Una card (apribile) per tipo di stat -->
    <template v-if="mondoSel != null">
      <div v-for="[tipo, rows] in gruppi" :key="tipo" class="card">
        <button type="button" class="card-head" @click="toggleCard(tipo)" :aria-expanded="isAperta(tipo)">
          <span class="chev" :class="{ open: isAperta(tipo) }">▸</span>
          <span class="card-title">{{ TIPO_LABEL[tipo] ?? tipo }}</span>
          <span class="count">{{ rows.length }}</span>
        </button>

        <div v-show="isAperta(tipo)" class="stat-list">
          <div v-for="r in rows" :key="r.stat.id" class="stat-row" :class="{ on: !!r.def }">
            <label class="row-head">
              <input type="checkbox" :checked="!!r.def" :disabled="busy"
                     @change="toggleAbilitata(r.stat.id, r.def)"/>
              <span class="stat-name">{{ r.stat.label }}</span>
              <span class="stat-id">{{ r.stat.id }}</span>
            </label>

            <div v-if="r.def" class="row-edit">
              <label class="field sm">
                <span class="lbl">Valore default</span>
                <input v-model="r.def.valoreDefault" type="text" placeholder="Es.: 0"/>
              </label>
              <label class="field sm grow">
                <span class="lbl">Modificatore</span>
                <SearchSelect v-model="r.def.defaultModId"
                              :options="[{value:'',label:'— nessuno —'}, ...statOptions]"/>
              </label>
              <label class="field chk">
                <input type="checkbox" v-model="r.def.addestramento"/>
                <span>Addestramento</span>
              </label>
              <button class="btn primary sm" :disabled="busy" @click="salvaDef(r.def)">Salva</button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Nuova stat globale (assegnata subito al mondo selezionato) — apribile -->
    <div class="card">
      <button type="button" class="card-head" @click="toggleCard('__nuova')" :aria-expanded="isAperta('__nuova')">
        <span class="chev" :class="{ open: isAperta('__nuova') }">▸</span>
        <span class="card-title">Nuova statistica</span>
      </button>
      <div v-show="isAperta('__nuova')" class="card-body">
        <div class="grid">
          <label class="field">
            <span class="lbl">Tipo</span>
            <SearchSelect v-model="nuovaStat.tipo" :options="TIPO_OPTS" :sort="false"/>
          </label>
          <label v-if="nuovaStat.tipo === 'AB'" class="field">
            <span class="lbl">Sotto-tipo</span>
            <SearchSelect v-model="sottoTipoAB" :options="SOTTOTIPO_AB_OPTS" :sort="false"/>
          </label>
          <label class="field">
            <span class="lbl">Id (max 10)</span>
            <input v-model.trim="nuovaStat.id" type="text" maxlength="10" placeholder="Es.: NUOTARE"/>
          </label>
          <label class="field grow">
            <span class="lbl">Label</span>
            <input v-model.trim="nuovaStat.label" type="text" placeholder="Es.: Nuotare"/>
          </label>
        </div>
        <p class="muted ultima-info">
          <template v-if="ultimaStatFamiglia">
            Ultima statistica di questo tipo: <strong>{{ ultimaStatFamiglia.label }}</strong> ({{ ultimaStatFamiglia.id }})
          </template>
          <template v-else>Nessuna statistica di questo tipo ancora salvata.</template>
        </p>
        <button class="btn primary" :disabled="busy" @click="salvaStat">
          Crea statistica{{ mondoSel != null ? ' e abilita nel mondo' : '' }}
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.stats-admin { max-width: 64rem; margin: 0 auto; padding: 1rem; display: flex; flex-direction: column; gap: 1rem; }
.head { display: flex; align-items: center; gap: .6rem; }
.head h1 { margin: 0; font-size: 1.2rem; }
.card { border: 1px solid #e5e7eb; border-radius: .6rem; padding: 1rem; display: flex; flex-direction: column; gap: .6rem; background: #fff; }
.card h2 { margin: 0; font-size: 1rem; }
.card-head {
  display: flex; align-items: center; gap: .5rem; width: 100%;
  background: transparent; border: 0; padding: 0; cursor: pointer; text-align: left; font: inherit;
}
.card-title { font-weight: 700; font-size: 1rem; flex: 1; }
.chev { font-size: .8rem; opacity: .6; transition: transform .15s; flex-shrink: 0; }
.chev.open { transform: rotate(90deg); }
.card-body { display: flex; flex-direction: column; gap: .6rem; }
.grid { display: flex; flex-wrap: wrap; gap: .6rem; align-items: flex-end; }
.field { display: flex; flex-direction: column; gap: .3rem; min-width: 9rem; }
.field.grow { flex: 1; }
.field.sm { min-width: 7rem; }
.field.chk { flex-direction: row; align-items: center; gap: .4rem; min-width: auto; }
.lbl { font-size: .75rem; font-weight: 600; opacity: .8; }
input[type="text"] { width: 100%; box-sizing: border-box; padding: .45rem .55rem; border: 1px solid #d0d5dd; border-radius: .5rem; }

.stat-list { display: flex; flex-direction: column; gap: .3rem; }
.card h2 .count { font-size: .72rem; font-weight: 700; background: #e5e7eb; color: #374151; border-radius: 999px; padding: .05rem .45rem; margin-left: .35rem; }
.stat-row { border: 1px solid #e5e7eb; border-radius: .5rem; padding: .45rem .55rem; background: #fff; }
.stat-row.on { border-color: #c7d2fe; background: #f5f7ff; }
.row-head { display: flex; align-items: center; gap: .55rem; cursor: pointer; }
.stat-name { font-weight: 600; flex: 1; }
.stat-id { font-size: .72rem; background: #eef2ff; color: #3730a3; border-radius: .35rem; padding: .05rem .35rem; }
.stat-tipo { font-size: .72rem; opacity: .65; }
.row-edit { display: flex; flex-wrap: wrap; gap: .5rem; align-items: flex-end; margin-top: .5rem; padding-top: .5rem; border-top: 1px solid #eef2f7; }

.muted { font-size: .85rem; opacity: .6; }
.btn { padding: .5rem .9rem; border-radius: .5rem; border: 1px solid transparent; cursor: pointer; }
.btn.sm { padding: .4rem .7rem; font-size: .85rem; }
.btn.ghost { border-color: #d0d5dd; background: #fff; }
.btn.primary { background: #2563eb; color: #fff; align-self: flex-start; }
.btn:disabled { opacity: .6; cursor: default; }
.error { margin: 0; padding: .5rem .75rem; border-radius: .5rem; color: #991b1b; background: #fef2f2; border: 1px solid #fecaca; font-size: .85rem; }

/* mobile */
@media (max-width: 640px) {
  .stats-admin { padding: .6rem; }
  .card { padding: .7rem; }
  .grid { flex-direction: column; align-items: stretch; }
  .field, .field.sm, .field.grow { min-width: 0; width: 100%; }
  .row-edit { flex-direction: column; align-items: stretch; }
  .row-edit .btn.primary.sm { align-self: stretch; text-align: center; }
  .row-head { flex-wrap: wrap; }
  .stat-name { flex: 1 1 100%; }
}
</style>
