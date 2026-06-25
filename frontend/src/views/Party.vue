<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useAuthStore} from '../stores/auth'
import {
  addMembro,
  createPersonaggio,
  deleteParty,
  getMembri,
  getParty,
  MembroParty,
  TIPI_PERSONAGGIO
} from '../service/PartyService'
import {formatKg, formatPesoTotale, PartyDetail, PersonaggioSoldi} from '../models/dto/Party'
import SoldiView from '../components/SoldiView.vue'
import SearchSelect from '../components/SearchSelect.vue'
import {listUsers} from '../service/AuthService'
import {UtenteAdmin} from '../models/dto/Auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const party = ref<PartyDetail | null>(null)
const loading = ref(true)
const errorMsg = ref<string | null>(null)

// membri del party (utenti) — usati sia per la gestione sia per il proprietario del PG
const membri = ref<MembroParty[]>([])

async function caricaMembri() {
  if (membri.value.length || !party.value) return
  try {
    membri.value = (await getMembri(party.value.id)).data
  } catch (e) {
    console.error('Errore caricamento membri:', e)
  }
}

// creazione personaggio
const showCreaPg = ref(false)
const nuovoNome = ref('')
const nuovoTipo = ref('PG')
const nuovoProprietario = ref<number | null>(null)
const busyPg = ref(false)
const tipiPg = TIPI_PERSONAGGIO

async function apriCreaPg() {
  showCreaPg.value = !showCreaPg.value
  if (showCreaPg.value) {
    nuovoProprietario.value = auth.utente?.id ?? null
    await caricaMembri()
  }
}

async function caricaParty() {
  const id = Number(route.params.id)
  const res = await getParty(id)
  party.value = res.data
}

async function onCreaPg() {
  if (!nuovoNome.value.trim() || busyPg.value || !party.value) return
  busyPg.value = true
  try {
    const proprietario = nuovoTipo.value === 'PG' ? (nuovoProprietario.value ?? undefined) : undefined
    await createPersonaggio(party.value.id, nuovoNome.value.trim(), nuovoTipo.value, proprietario)
    nuovoNome.value = ''
    nuovoTipo.value = 'PG'
    showCreaPg.value = false
    await caricaParty()
  } catch (e) {
    console.error('Errore creazione personaggio:', e)
    errorMsg.value = 'Errore nella creazione del personaggio'
  } finally {
    busyPg.value = false
  }
}

const isMaster = computed(() => party.value?.ruolo === 'MASTER')
const partyVuoto = computed(() => (party.value?.personaggi.length ?? 0) === 0)

// gestione membri
const showGestione = ref(false)
const nuovoUsername = ref('')
const nuovoRuolo = ref('GIOCATORE')
const busyMembro = ref(false)

// autocomplete utenti
const tuttiUtenti = ref<UtenteAdmin[]>([])
const ricercaUtente = ref('')
const showSuggerimenti = ref(false)

const suggerimenti = computed(() => {
  const q = ricercaUtente.value.trim().toLowerCase()
  if (!q) return []
  return tuttiUtenti.value.filter(u =>
    u.username.toLowerCase().includes(q) || u.name.toLowerCase().includes(q)
  ).slice(0, 8)
})

function selezionaUtente(u: UtenteAdmin) {
  nuovoUsername.value = u.username
  ricercaUtente.value = `${u.name} (@${u.username})`
  showSuggerimenti.value = false
}

function onRicercaInput() {
  nuovoUsername.value = ''
  showSuggerimenti.value = true
}

function onRicercaBlur() {
  setTimeout(() => { showSuggerimenti.value = false }, 150)
}

async function apriGestione() {
  showGestione.value = !showGestione.value
  if (showGestione.value) {
    await caricaMembri()
    if (!tuttiUtenti.value.length) {
      try {
        tuttiUtenti.value = (await listUsers()).data
      } catch (e) {
        console.error('Errore caricamento utenti:', e)
      }
    }
  }
}

async function onAddMembro() {
  if (!nuovoUsername.value.trim() || busyMembro.value || !party.value) return
  busyMembro.value = true
  try {
    const res = await addMembro(party.value.id, nuovoUsername.value.trim(), nuovoRuolo.value)
    membri.value.push(res.data)
    nuovoUsername.value = ''
    ricercaUtente.value = ''
  } catch (e: any) {
    console.error('Errore aggiunta membro:', e)
    errorMsg.value = e?.response?.status === 404
        ? 'Utente non trovato'
        : e?.response?.status === 409
            ? 'L\'utente fa già parte del party'
            : 'Errore nell\'associazione dell\'utente'
  } finally {
    busyMembro.value = false
  }
}

async function onEliminaParty() {
  if (!party.value || !partyVuoto.value) return
  if (!window.confirm(`Eliminare il party "${party.value.nome}"?`)) return
  try {
    await deleteParty(party.value.id)
    router.replace('/')
  } catch (e: any) {
    console.error('Errore eliminazione party:', e)
    errorMsg.value = e?.response?.status === 409
        ? 'Il party non è vuoto'
        : 'Errore nell\'eliminazione del party'
  }
}

onMounted(async () => {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) {
    errorMsg.value = 'Id party non valido'
    loading.value = false
    return
  }
  try {
    await caricaParty()
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 403
        ? 'Non fai parte di questo party'
        : 'Errore nel caricamento del party'
    console.error('Errore caricamento party:', e)
  } finally {
    loading.value = false
  }
})

function apriScheda(p: PersonaggioSoldi) {
  // le banche hanno una vista dedicata (solo conti correnti)
  if (p.tipoPersonaggio === 'BANCA') {
    router.push(`/banca/${p.id}`)
    return
  }
  // le navi si aprono direttamente sull'inventario (tab 2)
  const tab = p.tipoPersonaggio === 'NAVE' ? '?tab=2' : ''
  router.push(`/scheda/${p.id}${tab}`)
}

// gruppi: Party (senza TIPO_PERSONAGGIO, proprietari in cima), Barche (NAVE), Stella (STELLA)
const membriParty = computed<PersonaggioSoldi[]>(() =>
    (party.value?.personaggi ?? [])
        .filter(p => !p.tipoPersonaggio)
        .slice()
        .sort((a, b) => {
          if (a.proprietario !== b.proprietario) return a.proprietario ? -1 : 1
          return a.nome.localeCompare(b.nome)
        }))
const membriBarche = computed<PersonaggioSoldi[]>(() =>
    (party.value?.personaggi ?? []).filter(p => p.tipoPersonaggio === 'NAVE'))
const membriStella = computed<PersonaggioSoldi[]>(() =>
    (party.value?.personaggi ?? []).filter(p => p.tipoPersonaggio === 'STELLA'))
const membriBanche = computed<PersonaggioSoldi[]>(() =>
    (party.value?.personaggi ?? []).filter(p => p.tipoPersonaggio === 'BANCA'))
const membriAltri = computed<PersonaggioSoldi[]>(() =>
    (party.value?.personaggi ?? []).filter(p =>
        p.tipoPersonaggio && !['NAVE', 'STELLA', 'BANCA'].includes(p.tipoPersonaggio)))

const GRUPPI = computed(() => [
  {titolo: 'Party', membri: membriParty.value},
  {titolo: 'Barche', membri: membriBarche.value},
  {titolo: 'Stella', membri: membriStella.value},
  {titolo: 'Banche', membri: membriBanche.value},
  {titolo: 'Altro', membri: membriAltri.value},
].filter(g => g.membri.length > 0))
</script>

<template>
  <div class="party-page">
    <header class="head">
      <button class="btn ghost" @click="router.back()">←</button>
      <div class="title">
        <h1>{{ party?.nome ?? 'Party' }}</h1>
        <span v-if="party" class="pill" :class="party.ruolo === 'MASTER' ? 'master' : 'giocatore'">
          {{ party.ruolo === 'MASTER' ? 'Master' : 'Giocatore' }}
        </span>
      </div>
      <button v-if="party" class="btn" @click="router.push(`/party/${party.id}/items`)">Item</button>
      <button v-if="party" class="btn" @click="router.push(`/party/${party.id}/banche`)">Banche</button>
      <button v-if="isMaster" class="btn" @click="apriGestione">Gestione</button>
    </header>

    <div v-if="loading" class="state">Caricamento…</div>
    <div v-else-if="errorMsg" class="state error">{{ errorMsg }}</div>

    <template v-else-if="party">
      <!-- Gestione party (solo master) -->
      <section v-if="isMaster && showGestione" class="block gestione">
        <h2>Gestione party</h2>

        <div class="membri">
          <div v-for="m in membri" :key="m.utenteId" class="membro">
            <span class="nome">{{ m.name || m.username }}</span>
            <span class="muted">@{{ m.username }}</span>
            <span class="pill" :class="m.ruolo === 'MASTER' ? 'master' : 'giocatore'">
              {{ m.ruolo === 'MASTER' ? 'Master' : 'Giocatore' }}
            </span>
          </div>
        </div>

        <div class="crea-form">
          <div class="autocomplete">
            <input
              v-model="ricercaUtente"
              type="text"
              placeholder="Cerca utente per username o nome…"
              autocomplete="off"
              @input="onRicercaInput"
              @focus="showSuggerimenti = true"
              @blur="onRicercaBlur"
            />
            <ul v-if="showSuggerimenti && suggerimenti.length" class="suggerimenti">
              <li v-for="u in suggerimenti" :key="u.id" @mousedown.prevent="selezionaUtente(u)">
                <span class="sug-nome">{{ u.name }}</span>
                <span class="sug-username">@{{ u.username }}</span>
              </li>
            </ul>
          </div>
          <SearchSelect v-model="nuovoRuolo" :sort="false"
                        :options="[{value:'GIOCATORE',label:'Giocatore'},{value:'MASTER',label:'Master'}]"/>
          <button class="btn primary" :disabled="busyMembro || !nuovoUsername.trim()" @click="onAddMembro">
            {{ busyMembro ? 'Associazione…' : 'Associa utente' }}
          </button>
        </div>

        <button class="btn danger" :disabled="!partyVuoto" :title="partyVuoto ? '' : 'Il party non è vuoto'"
                @click="onEliminaParty">
          Elimina party
        </button>
        <p v-if="!partyVuoto" class="muted">Il party può essere eliminato solo se non ha personaggi.</p>
      </section>

      <!-- Somma di tutto il party -->
      <section class="block somma">
        <h2>Soldi del party</h2>
        <div class="card highlight">
          <SoldiView :soldi="party.somma"/>
          <div class="peso-monete">Peso monete: {{ formatKg(party.pesoMonete) }}</div>
        </div>
        <div class="card peso-row">
          <span class="peso-label">Peso totale</span>
          <span class="peso-val">{{ formatPesoTotale(party.pesoTotale) }}</span>
        </div>
      </section>

      <!-- Crea personaggio -->
      <section class="block">
        <button class="btn-add" @click="apriCreaPg">
          <span class="plus">+</span> Crea personaggio
        </button>
        <div v-if="showCreaPg" class="crea-form">
          <input v-model="nuovoNome" type="text" placeholder="Nome"/>
          <SearchSelect v-model="nuovoTipo" :options="tipiPg" :sort="false"/>
          <label v-if="nuovoTipo === 'PG'" class="field">
            <span class="muted">Proprietario</span>
            <SearchSelect v-model="nuovoProprietario" placeholder="Proprietario…"
                          :options="membri.map(m => ({value: m.utenteId, label: `${m.name || m.username} (@${m.username})`}))"/>
          </label>
          <button class="btn primary" :disabled="busyPg || !nuovoNome.trim() || (nuovoTipo === 'PG' && !nuovoProprietario)"
                  @click="onCreaPg">
            {{ busyPg ? 'Creazione…' : 'Crea' }}
          </button>
        </div>
      </section>

      <!-- Membri raggruppati -->
      <section v-for="g in GRUPPI" :key="g.titolo" class="block">
        <h2>{{ g.titolo }}</h2>
        <ul class="cards">
          <li v-for="p in g.membri" :key="p.id">
            <button class="card clickable" @click="apriScheda(p)">
              <span class="nome">
                {{ p.nome }}
                <span v-if="p.proprietario" class="pill mio">Tuo</span>
              </span>
              <span v-if="p.peso > 0" class="pill peso">{{ formatKg(p.peso) }}</span>
              <SoldiView :soldi="p.soldi" compatto/>
            </button>
          </li>
        </ul>
      </section>
      <div v-if="!party.personaggi.length" class="state">Nessun personaggio nel party.</div>
    </template>
  </div>
</template>

<style scoped>
.party-page {
  width: 100%;
  max-width: 44rem;
  margin: 0 auto;
  padding: 1rem;
  display: grid;
  gap: 1rem;
  align-content: start;
  /* il layout globale ha overflow:hidden, lo scroll va gestito qui */
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

.title {
  display: flex;
  align-items: center;
  gap: .5rem;
  min-width: 0;
  flex: 1;
}

.title h1 { margin: 0; font-size: 1.25rem; }

.block { display: grid; gap: .5rem; }
.block h2 { margin: 0; font-size: 1rem; }

.cards {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: .5rem;
}

.card {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: .5rem;
  padding: .7rem .9rem;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  text-align: left;
}

.card .nome { flex: 1; font-weight: 600; min-width: 8rem; }
.card.clickable { cursor: pointer; }
.card.clickable:hover { background: #f9fafb; }

.card.highlight {
  background: #fefce8;
  border-color: #fde68a;
  justify-content: center;
  padding: 1rem;
  flex-direction: column;
}

.peso-monete {
  font-size: .8rem;
  opacity: .7;
  font-weight: 600;
}

.pill {
  font-size: .75rem;
  padding: .15rem .5rem;
  border-radius: .5rem;
}
.pill.master { background: #fef3c7; color: #92400e; }
.pill.giocatore { background: #dbeafe; color: #1e40af; }
.pill.mio { background: #dcfce7; color: #166534; margin-left: .35rem; }
.pill.peso { background: #f3f4f6; color: #374151; }

.peso-row {
  justify-content: space-between;
}
.peso-label { font-weight: 600; }
.peso-val { font-weight: 700; font-variant-numeric: tabular-nums; }

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
.btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.btn:disabled { opacity: .6; cursor: default; }

.btn-add {
  justify-self: start;
  display: inline-flex;
  align-items: center;
  gap: .35rem;
  padding: .4rem .8rem;
  border: 1px dashed #94a3b8;
  border-radius: .5rem;
  background: #fff;
  color: #334155;
  font-weight: 600;
  font-size: .85rem;
  cursor: pointer;
}
.btn-add .plus { font-weight: 800; color: #2563eb; }

.crea-form {
  display: grid;
  gap: .4rem;
  padding: .6rem;
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  background: #fff;
  margin-top: .5rem;
}
.crea-form input, .crea-form select {
  padding: .45rem .6rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
}
.crea-form .field { display: grid; gap: .25rem; }
.crea-form .field .muted { font-size: .75rem; }

.gestione { gap: .6rem; }
.btn.danger { justify-self: start; background: #fef2f2; color: #991b1b; border-color: #fecaca; }

.autocomplete { position: relative; }
.autocomplete input { width: 100%; box-sizing: border-box; padding: .45rem .6rem; border: 1px solid #d0d5dd; border-radius: .5rem; }
.suggerimenti {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 100;
  list-style: none;
  margin: .15rem 0 0;
  padding: 0;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  box-shadow: 0 4px 12px rgba(0,0,0,.1);
  overflow: hidden;
}
.suggerimenti li {
  display: flex;
  align-items: center;
  gap: .5rem;
  padding: .45rem .7rem;
  cursor: pointer;
}
.suggerimenti li:hover { background: #f0f7ff; }
.sug-nome { font-weight: 600; flex: 1; }
.sug-username { font-size: .82rem; color: #64748b; }

.membri { display: grid; gap: .35rem; }
.membro {
  display: flex;
  align-items: center;
  gap: .5rem;
  padding: .4rem .6rem;
  border: 1px solid #e5e7eb;
  border-radius: .5rem;
  background: #fff;
}
.membro .nome { font-weight: 600; }
</style>
