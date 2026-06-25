<script setup lang="ts">
import {onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {getParty, getPartyItems, giveItem} from '../service/PartyService'
import {formatKg, formatPesoTotale, Page, PartyDetail, PartyItem} from '../models/dto/Party'
import Mobile_DettaglioItem from './sheets/cico/character/Dettaglio/Mobile_DettaglioItem.vue'
import SearchSelect from '../components/SearchSelect.vue'

const route = useRoute()
const router = useRouter()

const partyId = Number(route.params.id)

const party = ref<PartyDetail | null>(null)
const pagina = ref<Page<PartyItem> | null>(null)
const loading = ref(true)
const errorMsg = ref<string | null>(null)

// filtri + paginazione
const filtroNome = ref('')
const filtroTipo = ref('')
const page = ref(0)
const PAGE_SIZE = 10

const TIPI_FILTRO = [
  {value: '', label: 'Tutti i tipi'},
  {value: 'OGGETTO', label: 'Oggetto'},
  {value: 'CONSUMABILE', label: 'Consumabile'},
  {value: 'ARMA', label: 'Arma'},
  {value: 'MUNIZIONE', label: 'Munizione'},
  {value: 'EQUIPAGGIAMENTO', label: 'Equipaggiamento'},
]

const expandedId = ref<number | null>(null)   // riga espansa (dettaglio)
const givingId = ref<number | null>(null)     // riga con pannello "give" aperto
const busyGive = ref(false)

async function load() {
  loading.value = true
  errorMsg.value = null
  try {
    const [pRes, iRes] = await Promise.all([
      getParty(partyId),
      getPartyItems(partyId, {
        nome: filtroNome.value.trim() || undefined,
        tipo: filtroTipo.value || undefined,
        page: page.value,
        size: PAGE_SIZE,
      }),
    ])
    party.value = pRes.data
    pagina.value = iRes.data
    // il backend può aver riallineato la pagina (es. dopo un filtro)
    page.value = iRes.data.page
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 403
        ? 'Non fai parte di questo party'
        : 'Errore nel caricamento'
    console.error('Errore caricamento item party:', e)
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

onMounted(() => {
  if (!Number.isFinite(partyId)) {
    errorMsg.value = 'Id party non valido'
    loading.value = false
    return
  }
  load()
})

function toggleExpand(id: number) {
  expandedId.value = expandedId.value === id ? null : id
  givingId.value = null
}

function toggleGive(id: number) {
  givingId.value = givingId.value === id ? null : id
}

// destinatari possibili: gli altri membri del party
function destinatari(itm: PartyItem) {
  return (party.value?.personaggi ?? []).filter(p => p.id !== itm.personaggioId)
}

async function doGive(itm: PartyItem, toId: number) {
  if (busyGive.value) return
  busyGive.value = true
  try {
    await giveItem(itm.id, itm.personaggioId, toId)
    givingId.value = null
    expandedId.value = null
    await load()
  } catch (e) {
    console.error('Errore spostamento item:', e)
    errorMsg.value = 'Errore nello spostamento'
  } finally {
    busyGive.value = false
  }
}

// shim minimale per riusare il dettaglio item della scheda personaggio
function personaggioShim(itm: PartyItem) {
  return {
    modificatori: {id: itm.personaggioId},
    items: {trasformazioni: [], idoli: []},
  }
}
</script>

<template>
  <div class="party-items-page">
    <header class="head">
      <button class="btn ghost" @click="router.back()">←</button>
      <div class="title">
        <h1>Item del party</h1>
        <span v-if="party" class="muted">{{ party.nome }}</span>
      </div>
      <span v-if="party" class="tot-peso">{{ formatPesoTotale(party.pesoTotale) }}</span>
    </header>

    <!-- filtri -->
    <div class="filters">
      <input
          type="text"
          v-model="filtroNome"
          placeholder="Cerca per nome…"
          class="filter-nome"
      />
      <SearchSelect v-model="filtroTipo" class="filter-tipo" :options="TIPI_FILTRO" :sort="false"/>
    </div>

    <!-- paginator -->
    <div v-if="pagina && pagina.totalPages > 1" class="paginator">
      <button class="btn" :disabled="page <= 0 || loading" @click="vaiPagina(page - 1)">‹</button>
      <span class="page-info">Pagina {{ page + 1 }} di {{ pagina.totalPages }} ({{ pagina.totalElements }} item)</span>
      <button class="btn" :disabled="page >= pagina.totalPages - 1 || loading" @click="vaiPagina(page + 1)">›</button>
    </div>

    <div v-if="loading" class="state">Caricamento…</div>
    <div v-else-if="errorMsg" class="state error">{{ errorMsg }}</div>

    <template v-else-if="pagina">
      <ul class="rows">
        <li v-for="itm in pagina.content" :key="itm.id" class="row-wrap">
          <div class="row" :class="{ disabled: itm.disabled }">
            <button class="row-main" @click="toggleExpand(itm.id)">
              <span class="nome">
                {{ itm.nome }}
                <span v-if="itm.quantita !== 1" class="pill qta">x{{ itm.quantita }}</span>
              </span>
              <span class="peso">{{ itm.peso > 0 ? formatKg(itm.peso) : '—' }}</span>
              <span class="pill owner">{{ itm.personaggioNome }}</span>
            </button>
            <button class="btn-give" :title="`Dai a un altro personaggio`" @click="toggleGive(itm.id)">
              🖐
            </button>
          </div>

          <!-- pannello give -->
          <div v-if="givingId === itm.id" class="give-panel">
            <span class="give-label">Dai a:</span>
            <button
                v-for="dest in destinatari(itm)"
                :key="dest.id"
                class="btn dest"
                :disabled="busyGive"
                @click="doGive(itm, dest.id)"
            >
              {{ dest.nome }}
            </button>
            <span v-if="!destinatari(itm).length" class="muted">Nessun altro membro nel party.</span>
          </div>

          <!-- dettaglio espanso: stesso componente della scheda personaggio -->
          <div v-if="expandedId === itm.id" class="detail">
            <Mobile_DettaglioItem
                :key="`det-${itm.id}-${itm.personaggioId}`"
                :data="{item: {id: itm.id, nome: itm.nome, tipo: itm.tipo, disabled: itm.disabled}, personaggio: personaggioShim(itm)}"
            />
          </div>
        </li>
      </ul>
      <div v-if="!pagina.content.length" class="state">Nessun item trovato.</div>
    </template>
  </div>
</template>

<style scoped>
.party-items-page {
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

.title { flex: 1; display: grid; }
.title h1 { margin: 0; font-size: 1.2rem; }
.muted { opacity: .65; font-size: .85rem; }
.tot-peso { font-weight: 700; font-variant-numeric: tabular-nums; }

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

.row.disabled .nome { opacity: .5; text-decoration: line-through; }

.row-main {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr auto auto;
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

.peso {
  font-size: .8rem;
  font-variant-numeric: tabular-nums;
  opacity: .75;
  white-space: nowrap;
}

.pill {
  font-size: .75rem;
  padding: .15rem .5rem;
  border-radius: .5rem;
}
.pill.owner { background: #eef2ff; color: #3730a3; white-space: nowrap; }
.pill.qta { background: #f0fdf4; color: #166534; margin-left: .3rem; font-weight: 700; }

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

.btn-give {
  border: 0;
  border-left: 1px solid #e5e7eb;
  background: #fefce8;
  padding: 0 .8rem;
  font-size: 1.1rem;
  cursor: pointer;
}
.btn-give:hover { background: #fef9c3; }

.give-panel {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: .4rem;
  padding: .5rem .75rem;
  background: #fefce8;
  border: 1px solid #fde68a;
  border-radius: .6rem;
  margin-top: .25rem;
}

.give-label { font-weight: 600; font-size: .85rem; }

.btn {
  padding: .4rem .8rem;
  border-radius: .5rem;
  border: 1px solid #d0d5dd;
  background: #fff;
  cursor: pointer;
}
.btn.dest { font-weight: 600; }
.btn.dest:hover { background: #f0fdf4; border-color: #86efac; }
.btn:disabled { opacity: .6; cursor: default; }

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
</style>
