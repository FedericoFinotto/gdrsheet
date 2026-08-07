<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {getCompendio, searchCompendioDeep} from '../service/PersonaggioService'
import {ItemSearchResult} from '../service/PartyService'
import SearchSelect from '../components/SearchSelect.vue'
import {Page} from '../models/dto/Party'
import {Item} from '../models/dto/Item'
import {TIPO_ITEM_LABELS} from './sheets/cico/character/Cico/Editor/editorRegistry'
import Mobile_DettaglioItem from './sheets/cico/character/Dettaglio/Mobile_DettaglioItem.vue'
import QuestNode from './sheets/cico/character/Cico/Sheet/QuestNode.vue'
import {useMondoStore} from '../stores/mondo'
import {highlightMatch} from '../function/textHighlight'
import {getQuestAlbero} from '../service/QuestService'
import {Quest} from '../models/dto/Quest'

const router = useRouter()
const route = useRoute()
const mondoStore = useMondoStore()
// Il compendio si riferisce sempre al mondo selezionato nello switcher del menu (vedi
// stores/mondo.ts): niente più selettore mondo locale a questa pagina.
const filtroMondo = computed(() => mondoStore.corrente)

// "+ Crea oggetto": pre-compila mondo E sistema del mondo corrente (mondoStore.disponibili porta
// già il sistemaId, niente da richiedere di nuovo al backend).
const urlCreaOggetto = computed(() => {
  if (mondoStore.corrente === null) return '/itemcreate?compendio=1'
  const sistemaId = mondoStore.disponibili.find(m => m.id === mondoStore.corrente)?.sistemaId
  const params = new URLSearchParams({compendio: '1', mondo: String(mondoStore.corrente)})
  if (sistemaId) params.set('sistema', String(sistemaId))
  return `/itemcreate?${params.toString()}`
})

// Ricerca profonda (nome, descrizione, label, note, note modificatori): visibile solo se si è
// master del mondo corrente (o admin) — mondoStore.corrente è per costruzione sempre uno dei
// mondi disponibili per l'utente (vedi stores/mondo.ts), quindi null qui significa "nessun mondo
// di cui sono master", cioè niente ricerca profonda.
const puoRicercaProfonda = computed(() => mondoStore.corrente !== null)
// I filtri vivono nella query string: così uscendo verso un editor o un randomizzatore e
// tornando indietro (router.back()) si ritrova la stessa vista, senza stato da conservare
// altrove. Inizializzati qui, cioè PRIMA che i watch siano registrati, per non farli scattare.
const deepMode = ref(route.query.deep === '1')
const risultatiGlobali = ref<ItemSearchResult[]>([])
const cercandoGlobale = ref(false)
const globalExpandedId = ref<number | null>(null)
const inRicercaGlobale = computed(() => puoRicercaProfonda.value && deepMode.value && filtroNome.value.trim().length >= 2)
async function eseguiRicercaGlobale() {
  const q = filtroNome.value.trim()
  if (q.length < 2) { risultatiGlobali.value = []; return }
  cercandoGlobale.value = true
  try {
    risultatiGlobali.value = (await searchCompendioDeep(q, filtroMondo.value)).data
  } catch (e) {
    console.error('Errore ricerca profonda compendio:', e)
    risultatiGlobali.value = []
  } finally {
    cercandoGlobale.value = false
  }
}
// Risultato QUEST: come nella ricerca del party, mostra l'albero radice+sotto-quest invece del
// dettaglio item generico (vedi PartyItems.vue per lo stesso pattern).
const alberiQuest = ref<Record<number, Quest>>({})
const caricandoAlbero = ref<Record<number, boolean>>({})

async function toggleGlobalExpand(r: ItemSearchResult) {
  globalExpandedId.value = globalExpandedId.value === r.id ? null : r.id
  if (globalExpandedId.value !== r.id || r.tipo !== 'QUEST' || alberiQuest.value[r.id] || caricandoAlbero.value[r.id]) return
  caricandoAlbero.value[r.id] = true
  try {
    alberiQuest.value[r.id] = (await getQuestAlbero(r.id)).data
  } catch (e) {
    console.error('Errore caricamento albero quest:', e)
  } finally {
    caricandoAlbero.value[r.id] = false
  }
}

const pagina = ref<Page<Item> | null>(null)
const loading = ref(true)
const errorMsg = ref<string | null>(null)

const filtroNome = ref(String(route.query.nome ?? ''))
const filtroTipo = ref(String(route.query.tipo ?? ''))
const page = ref(Math.max(0, Number(route.query.page) || 0))
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

// true solo dopo che mondoStore ha finito di risolvere il mondo corrente (carica() è async:
// prima di allora filtroMondo passa da null al valore vero, e senza questa guardia quel
// cambiamento farebbe scattare il watch sotto con una load() aggiuntiva — vedi onMounted.
const mondoPronto = ref(false)

let filtroTimer: any = null
let ricercaGlobaleTimer: any = null
watch([filtroNome, filtroTipo, filtroMondo, deepMode], () => {
  if (!mondoPronto.value) return
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

// Riflette i filtri nella URL. replace e non push: la cronologia non deve riempirsi di una voce
// per ogni tasto premuto, ma la voce corrente va aggiornata così che il back dall'editor torni
// alla vista giusta.
watch([filtroNome, filtroTipo, deepMode, page], () => {
  const q: Record<string, string> = {}
  if (filtroNome.value.trim()) q.nome = filtroNome.value.trim()
  if (filtroTipo.value) q.tipo = filtroTipo.value
  if (deepMode.value) q.deep = '1'
  if (page.value > 0) q.page = String(page.value)
  // niente replace se la query è già quella (confronto per chiave: l'ordine nella URL non conta)
  const uguale = (['nome', 'tipo', 'deep', 'page'] as const)
      .every(k => String(route.query[k] ?? '') === (q[k] ?? ''))
  if (!uguale) router.replace({query: q})
})

// …e viceversa: se la URL cambia mentre la pagina è già montata (es. la scorciatoia
// "Randomizzatori" del menu quando si è già nel compendio) i filtri devono adeguarsi.
// Il watch qui sopra non riscatta: dopo l'allineamento la query è identica.
watch(() => [route.query.nome, route.query.tipo, route.query.deep, route.query.page], () => {
  const nome = String(route.query.nome ?? '')
  const tipo = String(route.query.tipo ?? '')
  const deep = route.query.deep === '1'
  const pg = Math.max(0, Number(route.query.page) || 0)
  if (filtroNome.value !== nome) filtroNome.value = nome
  if (filtroTipo.value !== tipo) filtroTipo.value = tipo
  if (deepMode.value !== deep) deepMode.value = deep
  if (page.value !== pg) page.value = pg
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

// Al ritorno da un editor i filtri arrivano dalla URL: se erano in ricerca profonda va rilanciata
// quella, altrimenti il pannello resterebbe visibile ma vuoto.
// Aspetta che mondoStore abbia risolto il mondo corrente (no-op se un'altra pagina l'ha già
// caricato) PRIMA di caricare: una sola chiamata a /item/compendio, già col filtro giusto,
// invece di una senza idMondo seguita da una corretta appena la store finisce di idratarsi.
onMounted(async () => {
  await mondoStore.carica()
  mondoPronto.value = true
  if (inRicercaGlobale.value) eseguiRicercaGlobale()
  else load()
})
</script>

<template>
  <div class="compendio-page">
    <header class="head">
      <button class="btn ghost" @click="router.back()">←</button>
      <div class="title">
        <h1>📖 Compendio</h1>
        <span v-if="pagina" class="muted">{{ pagina.totalElements }} item</span>
      </div>
      <button class="btn primary" @click="router.push(urlCreaOggetto)">+ Crea oggetto</button>
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

    <!-- risultati ricerca profonda -->
    <template v-if="inRicercaGlobale">
      <div v-if="cercandoGlobale" class="state">Ricerca…</div>
      <ul v-else-if="risultatiGlobali.length" class="rows">
        <li v-for="r in risultatiGlobali" :key="r.id" class="row-wrap">
          <div class="row" :class="{ disabled: r.disabled }">
            <button class="row-main global" @click="toggleGlobalExpand(r)">
              <span class="pill tipo">{{ TIPO_ITEM_LABELS[r.tipo] ?? r.tipo }}</span>
              <span class="nome" v-html="highlightMatch(r.nome, filtroNome)"></span>
              <span v-if="r.matchTesto" class="match-snippet" v-html="highlightMatch(r.matchTesto, filtroNome)"></span>
            </button>
          </div>
          <div v-if="globalExpandedId === r.id" class="detail">
            <template v-if="r.tipo === 'QUEST'">
              <div v-if="caricandoAlbero[r.id]" class="state">Caricamento…</div>
              <QuestNode v-else-if="alberiQuest[r.id]" :key="`galbero-${r.id}`"
                         :quest="alberiQuest[r.id]" :evidenzia-id="r.id"/>
              <div v-else class="state">Quest non trovata.</div>
            </template>
            <Mobile_DettaglioItem
                v-else
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
            <button v-if="itm.tipo === 'RANDOMIZZATORE'" class="btn-edit btn-rand"
                    title="Apri il randomizzatore"
                    @click="router.push(`/randomizzatore/${itm.id}`)">
              🎲
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
  border: 1px solid var(--info-border);
  border-radius: .6rem;
  background: var(--info-bg);
  font-size: .95rem;
}
.global-search input:focus { outline: none; border-color: #60a5fa; background: var(--surface-0); }

.btn-deep {
  flex: none;
  padding: 0 .8rem;
  border: 1px solid var(--info-border);
  border-radius: .6rem;
  background: var(--info-bg);
  color: var(--info-text);
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
  color: var(--text-muted);
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

.filter-tipo {
  padding: .45rem .6rem;
  border: 1px solid var(--hairline);
  border-radius: .5rem;
  background: var(--surface-0);
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
  background: var(--surface-0);
  border: 1px solid var(--hairline);
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

.row-main:hover { background: var(--surface-hover); }

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
  color: var(--text-muted);
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
.pill.tipo { background: var(--info-bg); color: var(--info-text); white-space: nowrap; }

.btn-edit {
  border: 0;
  border-left: 1px solid var(--hairline);
  background: var(--btn-bg);
  padding: 0 .8rem;
  font-size: 1rem;
  cursor: pointer;
}
.btn-edit:hover { background: var(--info-bg); }
.btn-rand { border-right: 1px solid var(--hairline); }

.detail {
  margin-top: .25rem;
  border: 1px solid var(--hairline);
  border-radius: .6rem;
  padding: .5rem;
  background: var(--surface-0);
}

.state {
  padding: .75rem;
  border: 1px dashed var(--hairline);
  border-radius: .5rem;
}
.state.error { color: var(--danger-text); background: var(--danger-bg); border-color: var(--danger-border); }

.btn {
  padding: .45rem .8rem;
  border-radius: .5rem;
  border: 1px solid var(--hairline);
  background: var(--surface-0);
  cursor: pointer;
}
.btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; font-weight: 600; }
.btn:disabled { opacity: .6; cursor: default; }
</style>
