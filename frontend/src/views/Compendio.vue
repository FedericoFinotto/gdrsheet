<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {getCompendio, searchCompendioDeep} from '../service/PersonaggioService'
import {ItemSearchResult} from '../service/PartyService'
import SearchSelect from '../components/SearchSelect.vue'
import {Page} from '../models/dto/Party'
import {Item} from '../models/dto/Item'
import {TIPO_ITEM_LABELS} from './sheets/cico/character/Cico/Editor/editorRegistry'
import Mobile_DettaglioItem from './sheets/cico/character/Dettaglio/Mobile_DettaglioItem.vue'
import {useMondoSistema} from '../function/useMondoSistema'
import {useAuthStore} from '../stores/auth'
import {highlightMatch} from '../function/textHighlight'

const router = useRouter()
const {meiMondi, meiMondiOptions} = useMondoSistema()
const authStore = useAuthStore()

// Ricerca profonda (nome, descrizione, label, note, note modificatori): visibile solo ad
// admin/master, stesso ruolo effettivo già usato per il resto dell'app (isRealAdmin/effectiveRuolo).
const puoRicercaProfonda = computed(() => ['ADMIN', 'SUPERUSER', 'MASTER'].includes((authStore.effectiveRuolo || '').toUpperCase()))
const deepMode = ref(false)
const risultatiGlobali = ref<ItemSearchResult[]>([])
const cercandoGlobale = ref(false)
const globalExpandedId = ref<number | null>(null)
const inRicercaGlobale = computed(() => puoRicercaProfonda.value && deepMode.value && filtroNome.value.trim().length >= 2)
async function eseguiRicercaGlobale() {
  const q = filtroNome.value.trim()
  if (q.length < 2) { risultatiGlobali.value = []; return }
  cercandoGlobale.value = true
  try {
    risultatiGlobali.value = (await searchCompendioDeep(q)).data
  } catch (e) {
    console.error('Errore ricerca profonda compendio:', e)
    risultatiGlobali.value = []
  } finally {
    cercandoGlobale.value = false
  }
}
function toggleGlobalExpand(id: number) {
  globalExpandedId.value = globalExpandedId.value === id ? null : id
}

const pagina = ref<Page<Item> | null>(null)
const loading = ref(true)
const errorMsg = ref<string | null>(null)

const filtroNome = ref('')
const filtroTipo = ref('')
const filtroMondo = ref<number | null>(null)
const page = ref(0)
const PAGE_SIZE = 10

const haMultiMondi = computed(() => meiMondi.value.length > 1)

const TIPI_FILTRO = [
  {value: '', label: 'Tutti i tipi'},
  ...Object.entries(TIPO_ITEM_LABELS).map(([value, label]) => ({value, label})),
]

const expandedId = ref<number | null>(null)

async function load() {
  loading.value = true
  errorMsg.value = null
  try {
    const res = await getCompendio({
      nome: filtroNome.value.trim() || undefined,
      tipo: filtroTipo.value || undefined,
      idMondo: filtroMondo.value ?? undefined,
      page: page.value,
      size: PAGE_SIZE,
    })
    pagina.value = res.data
    page.value = res.data.page
  } catch (e) {
    errorMsg.value = 'Errore nel caricamento del compendio'
    console.error('Errore caricamento compendio:', e)
  } finally {
    loading.value = false
  }
}

let filtroTimer: any = null
let ricercaGlobaleTimer: any = null
watch([filtroNome, filtroTipo, filtroMondo, deepMode], () => {
  if (puoRicercaProfonda.value && deepMode.value) {
    if (ricercaGlobaleTimer) clearTimeout(ricercaGlobaleTimer)
    ricercaGlobaleTimer = setTimeout(eseguiRicercaGlobale, 350)
    return
  }
  if (filtroTimer) clearTimeout(filtroTimer)
  filtroTimer = setTimeout(() => {
    page.value = 0
    load()
  }, 300)
})

function vaiPagina(p: number) {
  if (!pagina.value) return
  const np = Math.min(Math.max(0, p), pagina.value.totalPages - 1)
  if (np === page.value) return
  page.value = np
  load()
}

function toggleExpand(id: number) {
  expandedId.value = expandedId.value === id ? null : id
}

// shim minimale per riusare il dettaglio item (nessun personaggio nel compendio)
const personaggioShim = {
  modificatori: {id: 0},
  items: {trasformazioni: [], idoli: []},
}

onMounted(load)
</script>

<template>
  <div class="compendio-page">
    <header class="head">
      <button class="btn ghost" @click="router.back()">←</button>
      <div class="title">
        <h1>📖 Compendio</h1>
        <span v-if="pagina" class="muted">{{ pagina.totalElements }} item</span>
      </div>
      <button class="btn primary" @click="router.push('/itemcreate?compendio=1')">+ Crea oggetto</button>
    </header>

    <!-- barra di ricerca unica: il tasto DEEP (solo admin/master) attiva la ricerca profonda -->
    <div class="global-search">
      <input
          type="text"
          v-model="filtroNome"
          :placeholder="deepMode ? '🔎 Cerca ovunque (nome, descrizione, label, note)…' : 'Cerca per nome…'"
      />
      <button v-if="puoRicercaProfonda" type="button" class="btn-deep" :class="{ active: deepMode }"
              title="Ricerca profonda: nome, descrizione, label, note" @click="deepMode = !deepMode">
        DEEP
      </button>
    </div>

    <!-- filtri (nascosti durante la ricerca profonda) -->
    <div v-if="!inRicercaGlobale" class="filters">
      <SearchSelect v-model="filtroTipo" class="filter-tipo" :options="TIPI_FILTRO" :sort="false"/>
    </div>
    <div v-if="!inRicercaGlobale && haMultiMondi" class="filters">
      <SearchSelect v-model="filtroMondo" :options="meiMondiOptions" :sort="false" class="filter-nome"/>
    </div>

    <!-- risultati ricerca profonda -->
    <template v-if="inRicercaGlobale">
      <div v-if="cercandoGlobale" class="state">Ricerca…</div>
      <ul v-else-if="risultatiGlobali.length" class="rows">
        <li v-for="r in risultatiGlobali" :key="r.id" class="row-wrap">
          <div class="row" :class="{ disabled: r.disabled }">
            <button class="row-main global" @click="toggleGlobalExpand(r.id)">
              <span class="pill tipo">{{ TIPO_ITEM_LABELS[r.tipo] ?? r.tipo }}</span>
              <span class="nome" v-html="highlightMatch(r.nome, filtroNome)"></span>
              <span v-if="r.matchTesto" class="match-snippet" v-html="highlightMatch(r.matchTesto, filtroNome)"></span>
            </button>
          </div>
          <div v-if="globalExpandedId === r.id" class="detail">
            <Mobile_DettaglioItem
                :key="`gdet-${r.id}`"
                :data="{item: {id: r.id, nome: r.nome, tipo: r.tipo, disabled: r.disabled}, personaggio: personaggioShim}"
            />
          </div>
        </li>
      </ul>
      <div v-else class="state">Nessun item trovato.</div>
    </template>

    <!-- paginator -->
    <div v-if="!inRicercaGlobale && pagina && pagina.totalPages > 1" class="paginator">
      <button class="btn" :disabled="page <= 0 || loading" @click="vaiPagina(page - 1)">‹</button>
      <span class="page-info">Pagina {{ page + 1 }} di {{ pagina.totalPages }}</span>
      <button class="btn" :disabled="page >= pagina.totalPages - 1 || loading" @click="vaiPagina(page + 1)">›</button>
    </div>

    <div v-if="!inRicercaGlobale && loading" class="state">Caricamento…</div>
    <div v-else-if="!inRicercaGlobale && errorMsg" class="state error">{{ errorMsg }}</div>

    <template v-else-if="!inRicercaGlobale && pagina">
      <ul class="rows">
        <li v-for="itm in pagina.content" :key="itm.id" class="row-wrap">
          <div class="row">
            <button class="row-main" @click="toggleExpand(itm.id)">
              <div class="row-main-content">
                <div class="row-top">
                  <span class="pill tipo">{{ TIPO_ITEM_LABELS[itm.tipo] ?? itm.tipo }}</span>
                  <span v-if="itm.manuale" class="manuale">{{ itm.manuale }}</span>
                </div>
                <span class="nome">{{ itm.nome }}</span>
              </div>
            </button>
            <button class="btn-edit" title="Modifica" @click="router.push(`/itemeditor/${itm.id}`)">
              ✎
            </button>
          </div>

          <!-- dettaglio espanso -->
          <div v-if="expandedId === itm.id" class="detail">
            <Mobile_DettaglioItem
                :key="`det-${itm.id}`"
                :data="{item: {id: itm.id, nome: itm.nome, tipo: itm.tipo, disabled: false}, personaggio: personaggioShim}"
            />
          </div>
        </li>
      </ul>
      <div v-if="!pagina.content.length" class="state">Nessun item trovato.</div>
    </template>
  </div>
</template>

<style scoped>
.compendio-page {
  width: 100%;
  max-width: 44rem;
  margin: 0 auto;
  padding: 1rem;
  display: grid;
  gap: .75rem;
  align-content: start;
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior-y: contain;
}

.head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: .5rem;
}

.title { flex: 1; display: grid; min-width: 0; }
.title h1 { margin: 0; font-size: 1.2rem; }
.muted { opacity: .65; font-size: .85rem; }

.global-search {
  display: flex;
  gap: .4rem;
}
.global-search input {
  flex: 1;
  min-width: 0;
  box-sizing: border-box;
  padding: .55rem .7rem;
  border: 1px solid #bfdbfe;
  border-radius: .6rem;
  background: #f8fbff;
  font-size: .95rem;
}
.global-search input:focus { outline: none; border-color: #60a5fa; background: #fff; }

.btn-deep {
  flex: none;
  padding: 0 .8rem;
  border: 1px solid #bfdbfe;
  border-radius: .6rem;
  background: #f8fbff;
  color: #3730a3;
  font-weight: 700;
  font-size: .8rem;
  letter-spacing: .03em;
  cursor: pointer;
}
.btn-deep.active { background: #2563eb; border-color: #2563eb; color: #fff; }

.row.disabled .nome { opacity: .5; text-decoration: line-through; }

.row-main.global {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: .4rem;
}
.row-main.global .nome { flex: 1 1 auto; }

.match-snippet {
  flex: 1 1 100%;
  font-size: .82rem;
  color: var(--color-text-secondary, #6b7280);
  overflow-wrap: anywhere;
}
.match-snippet :deep(mark.hl),
.nome :deep(mark.hl) {
  background: #fef08a;
  color: inherit;
  border-radius: .2rem;
  padding: 0 .1rem;
}

.filters {
  display: grid;
  grid-template-columns: auto;
  gap: .4rem;
}

.filter-nome, .filter-tipo {
  padding: .45rem .6rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  background: #fff;
}

.paginator {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: .5rem;
}

.page-info { font-size: .85rem; opacity: .75; }

.rows {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: .4rem;
}

.row {
  display: flex;
  align-items: stretch;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  overflow: hidden;
}

.row-main {
  flex: 1;
  display: flex;
  padding: .6rem .75rem;
  background: transparent;
  border: 0;
  cursor: pointer;
  text-align: left;
  min-width: 0;
}

.row-main:hover { background: #f9fafb; }

.row-main-content {
  display: grid;
  gap: .1rem;
  min-width: 0;
  width: 100%;
}

.row-top {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: baseline;
  gap: .1rem .5rem;
}

.manuale {
  font-size: .7rem;
  color: var(--color-text-secondary, #6b7280);
  overflow-wrap: break-word;
}

.nome {
  font-weight: 600;
  overflow-wrap: break-word;
}

.pill {
  font-size: .75rem;
  padding: .15rem .5rem;
  border-radius: .5rem;
}
.pill.tipo { background: #eef2ff; color: #3730a3; white-space: nowrap; }

.btn-edit {
  border: 0;
  border-left: 1px solid #e5e7eb;
  background: #f8fafc;
  padding: 0 .8rem;
  font-size: 1rem;
  cursor: pointer;
}
.btn-edit:hover { background: #eef2ff; }

.detail {
  margin-top: .25rem;
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  padding: .5rem;
  background: #fff;
}

.state {
  padding: .75rem;
  border: 1px dashed #e5e7eb;
  border-radius: .5rem;
}
.state.error { color: #991b1b; background: #fef2f2; border-color: #fecaca; }

.btn {
  padding: .45rem .8rem;
  border-radius: .5rem;
  border: 1px solid #d0d5dd;
  background: #fff;
  cursor: pointer;
}
.btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; font-weight: 600; }
.btn:disabled { opacity: .6; cursor: default; }
</style>
