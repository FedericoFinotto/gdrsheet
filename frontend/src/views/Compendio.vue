<script setup lang="ts">
import {onMounted, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {getCompendio} from '../service/PersonaggioService'
import {Page} from '../models/dto/Party'
import {Item} from '../models/dto/Item'
import {TIPO_ITEM_LABELS} from './sheets/cico/character/Cico/Editor/editorRegistry'
import Mobile_DettaglioItem from './sheets/cico/character/Dettaglio/Mobile_DettaglioItem.vue'

const router = useRouter()

const pagina = ref<Page<Item> | null>(null)
const loading = ref(true)
const errorMsg = ref<string | null>(null)

const filtroNome = ref('')
const filtroTipo = ref('')
const page = ref(0)
const PAGE_SIZE = 10

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
watch([filtroNome, filtroTipo], () => {
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
      <button class="btn primary" @click="router.push('/itemcreate')">+ Crea oggetto</button>
    </header>

    <!-- filtri -->
    <div class="filters">
      <input
          type="text"
          v-model="filtroNome"
          placeholder="Cerca per nome…"
          class="filter-nome"
      />
      <select v-model="filtroTipo" class="filter-tipo">
        <option v-for="t in TIPI_FILTRO" :key="t.value" :value="t.value">{{ t.label }}</option>
      </select>
    </div>

    <!-- paginator -->
    <div v-if="pagina && pagina.totalPages > 1" class="paginator">
      <button class="btn" :disabled="page <= 0 || loading" @click="vaiPagina(page - 1)">‹</button>
      <span class="page-info">Pagina {{ page + 1 }} di {{ pagina.totalPages }}</span>
      <button class="btn" :disabled="page >= pagina.totalPages - 1 || loading" @click="vaiPagina(page + 1)">›</button>
    </div>

    <div v-if="loading" class="state">Caricamento…</div>
    <div v-else-if="errorMsg" class="state error">{{ errorMsg }}</div>

    <template v-else-if="pagina">
      <ul class="rows">
        <li v-for="itm in pagina.content" :key="itm.id" class="row-wrap">
          <div class="row">
            <button class="row-main" @click="toggleExpand(itm.id)">
              <span class="nome">{{ itm.nome }}</span>
              <span class="pill tipo">{{ TIPO_ITEM_LABELS[itm.tipo] ?? itm.tipo }}</span>
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

.filters {
  display: grid;
  grid-template-columns: 1fr auto;
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
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: .5rem;
  padding: .6rem .75rem;
  background: transparent;
  border: 0;
  cursor: pointer;
  text-align: left;
  min-width: 0;
}

.row-main:hover { background: #f9fafb; }

.nome {
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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
