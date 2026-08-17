<script setup lang="ts">
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {useAuthStore} from '../stores/auth'
import {
  addMasterMondo, aggiornaConfigMondo, aggiornaMondo, aggiornaTipoItemConfig, creaListaIncantesimi,
  creaMondo, creaSistema, getCatalogoListeIncantesimi, getConfigMondo, getMasterMondo, getMondiAdmin,
  getSistemiAdmin, getTipoItemConfig, ListaIncantesimiOpt, MasterMondo, removeMasterMondo,
} from '../service/MondoAdminService'
import {MondoOpt} from '../function/useMondoSistema'
import SearchSelect from '../components/SearchSelect.vue'
import {TIPO_ITEM, TIPO_ITEM_LABELS} from '../models/entity/ItemDB'
import {CARD_LABELS, cardsForTipo} from '../function/cardEditorItems'

const router = useRouter()
const auth = useAuthStore()

// Non basta il ruolo admin sull'account: serve la modalità admin attiva (coerente con
// AuthzService.isAdmin lato backend, che altrimenti risponderebbe 403 a ogni chiamata di questa pagina).
const isAdmin = computed(() => {
  const r = (auth.utente?.ruolo ?? '').toUpperCase()
  return (r === 'ADMIN' || r === 'SUPERUSER') && auth.adminMode
})

const mondi = ref<MondoOpt[]>([])
const mondoOptions = computed(() => mondi.value.map(m => ({value: m.id, label: m.descrizione})))
const mondoSelezionato = ref<number | null>(null)

const sistemi = ref<MondoOpt[]>([])
const sistemaOptions = computed(() => sistemi.value.map(s => ({value: s.id, label: s.descrizione})))

const master = ref<MasterMondo[]>([])
const loadingMondi = ref(true)
const loadingMaster = ref(false)
const errorMsg = ref<string | null>(null)

const nuovoUsername = ref('')
const busyAdd = ref(false)

async function caricaMondi() {
  loadingMondi.value = true
  errorMsg.value = null
  try {
    const [rMondi, rSistemi] = await Promise.all([getMondiAdmin(), getSistemiAdmin()])
    mondi.value = rMondi.data ?? []
    sistemi.value = rSistemi.data ?? []
    if (mondoSelezionato.value === null && mondi.value.length) mondoSelezionato.value = mondi.value[0].id
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 403 ? 'Non autorizzato' : 'Errore nel caricamento dei mondi'
  } finally {
    loadingMondi.value = false
  }
}

// mondo selezionato: sistema attuale, per la riassegnazione inline (sincronizzato ogni volta che
// cambia la selezione o la lista mondi si aggiorna, es. dopo il salvataggio)
const sistemaDelMondo = ref<number | null>(null)
watch([mondoSelezionato, mondi], () => {
  sistemaDelMondo.value = mondi.value.find(m => m.id === mondoSelezionato.value)?.sistemaId ?? null
})

const busySistema = ref(false)
async function onSalvaSistema() {
  if (mondoSelezionato.value === null || sistemaDelMondo.value === null || busySistema.value) return
  busySistema.value = true
  errorMsg.value = null
  try {
    await aggiornaMondo(mondoSelezionato.value, {sistemaId: sistemaDelMondo.value})
    await caricaMondi()
  } catch (e) {
    console.error('Errore riassegnazione sistema:', e)
    errorMsg.value = 'Errore nel salvataggio del sistema'
  } finally {
    busySistema.value = false
  }
}

// nuovo sistema
const nuovoSistemaDescrizione = ref('')
const busyCreaSistema = ref(false)
async function onCreaSistema() {
  if (!nuovoSistemaDescrizione.value.trim() || busyCreaSistema.value) return
  busyCreaSistema.value = true
  errorMsg.value = null
  try {
    const res = await creaSistema(nuovoSistemaDescrizione.value.trim())
    nuovoSistemaDescrizione.value = ''
    await caricaMondi()
    // pre-seleziona il sistema appena creato per il form "nuovo mondo" qui sotto
    nuovoMondoSistema.value = res.data.id
  } catch (e) {
    console.error('Errore creazione sistema:', e)
    errorMsg.value = 'Errore nella creazione del sistema'
  } finally {
    busyCreaSistema.value = false
  }
}

// nuovo mondo
const nuovoMondoDescrizione = ref('')
const nuovoMondoSistema = ref<number | null>(null)
const busyCreaMondo = ref(false)
async function onCreaMondo() {
  if (!nuovoMondoDescrizione.value.trim() || nuovoMondoSistema.value === null || busyCreaMondo.value) return
  busyCreaMondo.value = true
  errorMsg.value = null
  try {
    const res = await creaMondo(nuovoMondoDescrizione.value.trim(), nuovoMondoSistema.value)
    nuovoMondoDescrizione.value = ''
    await caricaMondi()
    mondoSelezionato.value = res.data.id
  } catch (e) {
    console.error('Errore creazione mondo:', e)
    errorMsg.value = 'Errore nella creazione del mondo'
  } finally {
    busyCreaMondo.value = false
  }
}

async function caricaMaster() {
  if (mondoSelezionato.value === null) { master.value = []; return }
  loadingMaster.value = true
  errorMsg.value = null
  try {
    master.value = (await getMasterMondo(mondoSelezionato.value)).data ?? []
  } catch (e) {
    console.error('Errore caricamento master mondo:', e)
    errorMsg.value = 'Errore nel caricamento dei master'
  } finally {
    loadingMaster.value = false
  }
}

watch(mondoSelezionato, caricaMaster)

async function onAggiungi() {
  if (!nuovoUsername.value.trim() || mondoSelezionato.value === null || busyAdd.value) return
  busyAdd.value = true
  errorMsg.value = null
  try {
    await addMasterMondo(mondoSelezionato.value, nuovoUsername.value.trim())
    nuovoUsername.value = ''
    await caricaMaster()
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 404 ? 'Utente non trovato'
        : e?.response?.status === 409 ? 'È già master di questo mondo'
        : 'Errore nell\'assegnazione'
  } finally {
    busyAdd.value = false
  }
}

async function onRimuovi(m: MasterMondo) {
  if (mondoSelezionato.value === null) return
  const ok = confirm(`Togliere a ${m.name} (@${m.username}) il permesso master su questo mondo?`)
  if (!ok) return
  try {
    await removeMasterMondo(mondoSelezionato.value, m.utenteId)
    await caricaMaster()
  } catch (e) {
    console.error('Errore rimozione master mondo:', e)
    errorMsg.value = 'Errore nella rimozione'
  }
}

// fold apri/chiudi per le sezioni "Tipi item abilitati" e "Liste incantesimi abilitate"
const open = reactive({tipi: false, liste: false})

/* ---- Tipi item abilitati per il mondo ---- */
const tuttiTipi = Object.values(TIPO_ITEM) as string[]
const tipiAbilitati = ref<Set<string>>(new Set())
const busyTipi = ref(false)

function toggleTipo(t: string) {
  const s = new Set(tipiAbilitati.value)
  if (s.has(t)) s.delete(t); else s.add(t)
  tipiAbilitati.value = s
}
async function onSalvaTipi() {
  if (mondoSelezionato.value === null || busyTipi.value) return
  busyTipi.value = true
  errorMsg.value = null
  try {
    await aggiornaConfigMondo(mondoSelezionato.value, {tipiAbilitati: Array.from(tipiAbilitati.value)})
  } catch (e) {
    console.error('Errore salvataggio tipi abilitati:', e)
    errorMsg.value = 'Errore nel salvataggio dei tipi abilitati'
  } finally {
    busyTipi.value = false
  }
}

/* ---- Liste/domini incantesimi abilitati per il mondo ---- */
const catalogoListe = ref<ListaIncantesimiOpt[]>([])
const listeAbilitate = ref<Set<string>>(new Set())
const busyListe = ref(false)

function toggleLista(codice: string) {
  const s = new Set(listeAbilitate.value)
  if (s.has(codice)) s.delete(codice); else s.add(codice)
  listeAbilitate.value = s
}

// nuovo codice nel catalogo globale (non abilitato per nessun mondo finché non lo si salva
// esplicitamente qui sotto — ma lo pre-selezioniamo per comodità, dato che di solito lo si
// aggiunge proprio perché serve in QUESTO mondo)
const nuovaListaCodice = ref('')
const nuovaListaEtichetta = ref('')
const busyCreaLista = ref(false)
async function onCreaLista() {
  if (!nuovaListaCodice.value.trim() || !nuovaListaEtichetta.value.trim() || busyCreaLista.value) return
  busyCreaLista.value = true
  errorMsg.value = null
  try {
    const res = await creaListaIncantesimi(nuovaListaCodice.value.trim(), nuovaListaEtichetta.value.trim())
    catalogoListe.value = [...catalogoListe.value, res.data].sort((a, b) => a.etichetta.localeCompare(b.etichetta))
    listeAbilitate.value = new Set([...listeAbilitate.value, res.data.codice])
    nuovaListaCodice.value = ''
    nuovaListaEtichetta.value = ''
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 409 ? 'Codice già esistente nel catalogo' : 'Errore nella creazione della lista'
    console.error('Errore creazione lista incantesimi:', e)
  } finally {
    busyCreaLista.value = false
  }
}
async function onSalvaListe() {
  if (mondoSelezionato.value === null || busyListe.value) return
  busyListe.value = true
  errorMsg.value = null
  try {
    await aggiornaConfigMondo(mondoSelezionato.value, {codiciListeIncantesimi: Array.from(listeAbilitate.value)})
  } catch (e) {
    console.error('Errore salvataggio liste abilitate:', e)
    errorMsg.value = 'Errore nel salvataggio delle liste abilitate'
  } finally {
    busyListe.value = false
  }
}

/* ---- Editor per tipo: card strutturali + campi liberi, una card apribile per tipo abilitato ---- */
interface OpzioneRow { value: string; label: string }
interface CampoLiberoRow {
  chiave: string; etichetta: string
  tipoCampo: '' | 'TESTO' | 'TEXTAREA' | 'CHECKBOX' | 'SELECT' | 'DATETIME'
  placeholder: string; textarea: boolean; multiValore: boolean; html: boolean
  opzioni: OpzioneRow[]
}
interface TipoConfigState {
  loaded: boolean; loading: boolean; busy: boolean
  cardAbilitate: Set<string>; campiTitolo: string; campiLiberi: CampoLiberoRow[]
}

// stato per tipo (una entry per ogni tipo abilitato, popolata da caricaConfigMondo) + apri/chiudi
const statoTipi = reactive<Record<string, TipoConfigState>>({})
const openTipi = reactive<Record<string, boolean>>({})

const tipiAbilitatiOrdinati = computed(() =>
    Array.from(tipiAbilitati.value)
        .map(t => ({value: t, label: TIPO_ITEM_LABELS[t as keyof typeof TIPO_ITEM_LABELS] ?? t}))
        .sort((a, b) => a.label.localeCompare(b.label)))

function nuovoStatoTipo(): TipoConfigState {
  return {loaded: false, loading: false, busy: false, cardAbilitate: new Set(), campiTitolo: '', campiLiberi: []}
}

async function caricaConfigMondo() {
  Object.keys(statoTipi).forEach(k => delete statoTipi[k])
  Object.keys(openTipi).forEach(k => delete openTipi[k])
  if (mondoSelezionato.value === null) {
    tipiAbilitati.value = new Set()
    listeAbilitate.value = new Set()
    return
  }
  try {
    const {data} = await getConfigMondo(mondoSelezionato.value)
    tipiAbilitati.value = new Set(data.tipiAbilitati)
    listeAbilitate.value = new Set(data.listeIncantesimiAbilitate.map(l => l.codice))
    for (const t of data.tipiAbilitati) {
      statoTipi[t] = nuovoStatoTipo()
      openTipi[t] = false
    }
  } catch (e) {
    console.error('Errore caricamento configurazione mondo:', e)
    errorMsg.value = 'Errore nel caricamento della configurazione del mondo'
  }
}
watch(mondoSelezionato, caricaConfigMondo)

async function caricaTipoItemConfig(t: string) {
  const s = statoTipi[t]
  if (!s || mondoSelezionato.value === null) return
  s.loading = true
  errorMsg.value = null
  try {
    const {data} = await getTipoItemConfig(mondoSelezionato.value, t)
    s.cardAbilitate = new Set(data.cardAbilitate)
    s.campiTitolo = data.campiTitolo ?? ''
    s.campiLiberi = (data.campiLiberi ?? []).map(c => ({
      chiave: c.chiave, etichetta: c.etichetta,
      tipoCampo: (c.tipoCampo ?? '') as CampoLiberoRow['tipoCampo'],
      placeholder: c.placeholder ?? '', textarea: c.textarea, multiValore: c.multiValore, html: c.html,
      opzioni: (c.opzioni ?? []).map(o => ({value: o.value, label: o.label})),
    }))
    s.loaded = true
  } catch (e) {
    console.error('Errore caricamento configurazione tipo item:', e)
    errorMsg.value = 'Errore nel caricamento della configurazione del tipo'
  } finally {
    s.loading = false
  }
}

// apre/chiude la card di un tipo; al primo apertura carica la configurazione (lazy)
function toggleApriTipo(t: string) {
  openTipi[t] = !openTipi[t]
  if (openTipi[t] && statoTipi[t] && !statoTipi[t].loaded) caricaTipoItemConfig(t)
}

function toggleCard(t: string, c: string) {
  const s = statoTipi[t]
  if (!s) return
  const set = new Set(s.cardAbilitate)
  if (set.has(c)) set.delete(c); else set.add(c)
  s.cardAbilitate = set
}

function addCampo(t: string) {
  statoTipi[t]?.campiLiberi.push({
    chiave: '', etichetta: '', tipoCampo: '', placeholder: '',
    textarea: false, multiValore: false, html: false, opzioni: [],
  })
}
function removeCampo(t: string, i: number) {
  statoTipi[t]?.campiLiberi.splice(i, 1)
}
function moveCampo(t: string, i: number, dir: -1 | 1) {
  const arr = statoTipi[t]?.campiLiberi
  if (!arr) return
  const j = i + dir
  if (j < 0 || j >= arr.length) return
  ;[arr[i], arr[j]] = [arr[j], arr[i]]
}
function addOpzione(c: CampoLiberoRow) {
  c.opzioni.push({value: '', label: ''})
}
function removeOpzione(c: CampoLiberoRow, i: number) {
  c.opzioni.splice(i, 1)
}

async function onSalvaTipoConfig(t: string) {
  const s = statoTipi[t]
  if (!s || mondoSelezionato.value === null || s.busy) return
  s.busy = true
  errorMsg.value = null
  try {
    await aggiornaTipoItemConfig(mondoSelezionato.value, t, {
      cardAbilitate: Array.from(s.cardAbilitate),
      campiTitolo: s.campiTitolo.trim() || null,
      campiLiberi: s.campiLiberi
          .filter(c => c.chiave.trim() && c.etichetta.trim())
          .map(c => ({
            chiave: c.chiave.trim(), etichetta: c.etichetta.trim(),
            tipoCampo: c.tipoCampo || null, placeholder: c.placeholder.trim() || null,
            textarea: c.textarea, multiValore: c.multiValore, html: c.html,
            opzioni: (c.tipoCampo === 'SELECT' || c.tipoCampo === 'CHECKBOX')
                ? c.opzioni.filter(o => o.value.trim()).map(o => ({value: o.value.trim(), label: o.label.trim() || o.value.trim()}))
                : [],
          })),
    })
  } catch (e) {
    console.error('Errore salvataggio configurazione tipo item:', e)
    errorMsg.value = 'Errore nel salvataggio della configurazione del tipo'
  } finally {
    s.busy = false
  }
}

onMounted(async () => {
  if (!isAdmin.value) { router.replace('/'); return }
  await caricaMondi()
  await caricaMaster()
  await caricaConfigMondo()
  try {
    catalogoListe.value = (await getCatalogoListeIncantesimi()).data ?? []
  } catch (e) {
    console.error('Errore caricamento catalogo liste incantesimi:', e)
  }
})
</script>

<template>
  <div class="mondi-admin">
    <header class="head">
      <h1>Permessi per mondo</h1>
      <button class="btn ghost" @click="router.push('/')">Home</button>
    </header>

    <p class="muted">
      Il master di un mondo gestisce il compendio (creare/modificare/eliminare item, ricerca profonda,
      stat di default) solo di QUEL mondo — non degli altri. Un admin resta master di ogni mondo, sempre.
    </p>

    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    <div v-if="loadingMondi" class="state">Caricamento…</div>

    <template v-else>
      <!-- Sistemi: creazione (un sistema esiste solo per essere assegnato a uno o più mondi) -->
      <section class="block">
        <h2>Nuovo sistema</h2>
        <div class="add-form">
          <input v-model="nuovoSistemaDescrizione" type="text" placeholder="Nome sistema (es. D&D 3.5)"
                 @keyup.enter="onCreaSistema"/>
          <button class="btn primary" :disabled="busyCreaSistema || !nuovoSistemaDescrizione.trim()" @click="onCreaSistema">
            {{ busyCreaSistema ? 'Creazione…' : 'Crea sistema' }}
          </button>
        </div>
      </section>

      <!-- Mondi: creazione, sempre con un sistema (obbligatorio a livello di schema) -->
      <section class="block">
        <h2>Nuovo mondo</h2>
        <div class="add-form add-form-mondo">
          <input v-model="nuovoMondoDescrizione" type="text" placeholder="Nome mondo"/>
          <SearchSelect v-model="nuovoMondoSistema" :options="sistemaOptions" placeholder="Sistema…" :sort="false"/>
          <button class="btn primary"
                  :disabled="busyCreaMondo || !nuovoMondoDescrizione.trim() || nuovoMondoSistema === null"
                  @click="onCreaMondo">
            {{ busyCreaMondo ? 'Creazione…' : 'Crea mondo' }}
          </button>
        </div>
      </section>

      <section class="block">
        <h2>Mondo</h2>
        <SearchSelect v-model="mondoSelezionato" :options="mondoOptions" :sort="false"/>
      </section>

      <section v-if="mondoSelezionato !== null" class="block">
        <h2>Sistema di questo mondo</h2>
        <div class="add-form">
          <SearchSelect v-model="sistemaDelMondo" :options="sistemaOptions" :sort="false"/>
          <button class="btn primary" :disabled="busySistema || sistemaDelMondo === null" @click="onSalvaSistema">
            {{ busySistema ? 'Salvataggio…' : 'Salva' }}
          </button>
        </div>
      </section>

      <section v-if="mondoSelezionato !== null" class="block">
        <h2>Aggiungi master</h2>
        <div class="add-form">
          <input v-model="nuovoUsername" type="text" placeholder="Username" @keyup.enter="onAggiungi"/>
          <button class="btn primary" :disabled="busyAdd || !nuovoUsername.trim()" @click="onAggiungi">
            {{ busyAdd ? 'Assegnazione…' : 'Rendi master' }}
          </button>
        </div>
      </section>

      <section v-if="mondoSelezionato !== null" class="block">
        <h2>Master di questo mondo</h2>
        <div v-if="loadingMaster" class="state">Caricamento…</div>
        <div v-else-if="!master.length" class="state">Nessun master assegnato (a parte gli admin).</div>
        <ul v-else class="cards">
          <li v-for="m in master" :key="m.utenteId" class="card">
            <div class="info">
              <span class="nome">{{ m.name }}</span>
              <span class="muted">@{{ m.username }}</span>
            </div>
            <button class="btn small danger" @click="onRimuovi(m)">Rimuovi</button>
          </li>
        </ul>
      </section>

      <!-- Tipi item abilitati: solo questi sono creabili/editabili in questo mondo (opt-in) -->
      <section v-if="mondoSelezionato !== null" class="fold">
        <button type="button" class="fold-head" @click="open.tipi = !open.tipi">
          <span class="fold-title">Tipi item abilitati</span>
          <span class="fold-summary">{{ tipiAbilitati.size }}/{{ tuttiTipi.length }}</span>
          <span class="chev" :class="{open: open.tipi}">▸</span>
        </button>
        <div v-show="open.tipi" class="fold-body">
          <p class="muted">Solo i tipi abilitati sono creabili/editabili in questo mondo.</p>
          <div class="chip-grid">
            <label v-for="t in tuttiTipi" :key="t" class="chip-toggle" :class="{on: tipiAbilitati.has(t)}">
              <input type="checkbox" :checked="tipiAbilitati.has(t)" @change="toggleTipo(t)"/>
              {{ TIPO_ITEM_LABELS[t as keyof typeof TIPO_ITEM_LABELS] ?? t }}
            </label>
          </div>
          <button class="btn primary" :disabled="busyTipi" @click="onSalvaTipi">
            {{ busyTipi ? 'Salvataggio…' : 'Salva tipi abilitati' }}
          </button>
        </div>
      </section>

      <!-- Liste/domìni incantesimi abilitati per questo mondo -->
      <section v-if="mondoSelezionato !== null" class="fold">
        <button type="button" class="fold-head" @click="open.liste = !open.liste">
          <span class="fold-title">Liste / domìni incantesimi abilitati</span>
          <span class="fold-summary">{{ listeAbilitate.size }}/{{ catalogoListe.length }}</span>
          <span class="chev" :class="{open: open.liste}">▸</span>
        </button>
        <div v-show="open.liste" class="fold-body">
          <div v-if="!catalogoListe.length" class="state">Nessuna lista/dominio nel catalogo.</div>
          <div v-else class="chip-grid">
            <label v-for="l in catalogoListe" :key="l.codice" class="chip-toggle" :class="{on: listeAbilitate.has(l.codice)}">
              <input type="checkbox" :checked="listeAbilitate.has(l.codice)" @change="toggleLista(l.codice)"/>
              {{ l.etichetta }} ({{ l.codice }})
            </label>
          </div>
          <button class="btn primary" :disabled="busyListe" @click="onSalvaListe">
            {{ busyListe ? 'Salvataggio…' : 'Salva liste abilitate' }}
          </button>

          <div class="add-form add-form-lista">
            <input v-model="nuovaListaCodice" type="text" placeholder="Codice (es. SP_MIA_LISTA)" @keyup.enter="onCreaLista"/>
            <input v-model="nuovaListaEtichetta" type="text" placeholder="Etichetta (es. Mia Lista)" @keyup.enter="onCreaLista"/>
            <button class="btn outline"
                    :disabled="busyCreaLista || !nuovaListaCodice.trim() || !nuovaListaEtichetta.trim()"
                    @click="onCreaLista">
              {{ busyCreaLista ? 'Creazione…' : '+ Aggiungi al catalogo' }}
            </button>
          </div>
        </div>
      </section>

      <!-- Editor per tipo: una card apribile per ciascun tipo abilitato, con le sue card
           strutturali e i suoi campi liberi -->
      <section v-if="mondoSelezionato !== null" class="block">
        <h2>Editor per tipo</h2>
        <p class="muted">Card strutturali e campi liberi mostrati nell'editor, per ciascun tipo abilitato.</p>

        <section v-for="t in tipiAbilitatiOrdinati" :key="t.value" class="fold">
          <button type="button" class="fold-head" @click="toggleApriTipo(t.value)">
            <span class="fold-title">{{ t.label }}</span>
            <span class="fold-summary">
              {{ statoTipi[t.value]?.loaded ? `${statoTipi[t.value].cardAbilitate.size} card, ${statoTipi[t.value].campiLiberi.length} campi` : '' }}
            </span>
            <span class="chev" :class="{open: openTipi[t.value]}">▸</span>
          </button>
          <div v-show="openTipi[t.value]" class="fold-body">
            <div v-if="!statoTipi[t.value] || statoTipi[t.value].loading" class="state">Caricamento…</div>
            <template v-else>
              <h3>Card strutturali</h3>
              <div class="chip-grid">
                <label v-for="c in cardsForTipo(t.value)" :key="c" class="chip-toggle" :class="{on: statoTipi[t.value].cardAbilitate.has(c)}">
                  <input type="checkbox" :checked="statoTipi[t.value].cardAbilitate.has(c)" @change="toggleCard(t.value, c)"/>
                  {{ CARD_LABELS[c] ?? c }}
                </label>
              </div>

              <h3>Campi liberi</h3>
              <label class="field">
                <span class="lbl">Titolo gruppo campi (opzionale)</span>
                <input v-model="statoTipi[t.value].campiTitolo" type="text" placeholder="Es.: Dettagli aggiuntivi"/>
              </label>

              <div v-for="(c, i) in statoTipi[t.value].campiLiberi" :key="i" class="campo-row">
                <div class="campo-row-top">
                  <input v-model.trim="c.chiave" type="text" placeholder="Chiave (es. NOTE_EXTRA)" class="campo-chiave"/>
                  <input v-model.trim="c.etichetta" type="text" placeholder="Etichetta" class="campo-etichetta"/>
                  <select v-model="c.tipoCampo" class="campo-tipo">
                    <option value="">Testo semplice</option>
                    <option value="TESTO">Testo</option>
                    <option value="TEXTAREA">Area di testo</option>
                    <option value="CHECKBOX">Checkbox</option>
                    <option value="SELECT">Select</option>
                    <option value="DATETIME">Data/ora</option>
                  </select>
                  <button type="button" class="btn small ghost" :disabled="i===0" title="Sposta su" @click="moveCampo(t.value, i,-1)">▲</button>
                  <button type="button" class="btn small ghost" :disabled="i===statoTipi[t.value].campiLiberi.length-1" title="Sposta giù" @click="moveCampo(t.value, i,1)">▼</button>
                  <button type="button" class="btn small danger" title="Rimuovi" @click="removeCampo(t.value, i)">✕</button>
                </div>
                <div class="campo-row-flags">
                  <input v-model.trim="c.placeholder" type="text" placeholder="Placeholder (opzionale)" class="campo-placeholder"/>
                  <label class="chk-row"><input type="checkbox" v-model="c.textarea"/> Multilinea</label>
                  <label class="chk-row"><input type="checkbox" v-model="c.multiValore"/> Multi-valore</label>
                  <label class="chk-row"><input type="checkbox" v-model="c.html"/> HTML</label>
                </div>
                <div v-if="c.tipoCampo==='SELECT' || c.tipoCampo==='CHECKBOX'" class="campo-opzioni">
                  <div v-for="(o,oi) in c.opzioni" :key="oi" class="opzione-row">
                    <input v-model.trim="o.value" type="text" placeholder="Valore"/>
                    <input v-model.trim="o.label" type="text" placeholder="Etichetta"/>
                    <button type="button" class="btn small danger" @click="removeOpzione(c, oi)">✕</button>
                  </div>
                  <button type="button" class="btn small outline" @click="addOpzione(c)">+ Opzione</button>
                </div>
              </div>
              <button type="button" class="btn outline" @click="addCampo(t.value)">+ Aggiungi campo</button>

              <button class="btn primary" :disabled="statoTipi[t.value].busy" @click="onSalvaTipoConfig(t.value)">
                {{ statoTipi[t.value].busy ? 'Salvataggio…' : 'Salva configurazione editor' }}
              </button>
            </template>
          </div>
        </section>
      </section>
    </template>
  </div>
</template>

<style scoped>
.mondi-admin {
  width: 100%;
  padding: 1rem;
  display: grid;
  gap: 1rem;
  align-content: start;
  height: 100%;
  min-height: 0;
  overflow-y: auto;
}

.head { display: flex; justify-content: space-between; align-items: center; }
.head h1 { margin: 0; font-size: 1.25rem; }

.block { display: grid; gap: .5rem; }
.block h2 { margin: 0; font-size: 1rem; }

.muted { opacity: .65; font-size: .85rem; margin: 0; }

.add-form { display: flex; gap: .4rem; }
.add-form input {
  flex: 1;
  padding: .45rem .6rem; border: 1px solid var(--hairline); border-radius: .5rem;
}
.add-form-mondo { flex-wrap: wrap; }
.add-form-mondo input { flex: 1 1 12rem; }
.add-form-lista { flex-wrap: wrap; padding-top: .3rem; border-top: 1px dashed var(--hairline); }
.add-form-lista input { flex: 1 1 12rem; }

.cards { list-style: none; margin: 0; padding: 0; display: grid; gap: .5rem; }
.card {
  display: flex; align-items: center; gap: .5rem;
  padding: .6rem .8rem; background: var(--surface-0); border: 1px solid var(--hairline); border-radius: .6rem;
}
.info { flex: 1; display: grid; }
.info .nome { font-weight: 600; }

.state { padding: .6rem; border: 1px dashed var(--hairline); border-radius: .5rem; }
.error {
  margin: 0; padding: .5rem .7rem; border-radius: .5rem;
  color: var(--danger-text); background: var(--danger-bg); border: 1px solid var(--danger-border); font-size: .85rem;
}

.btn {
  padding: .45rem .8rem; border-radius: .5rem; border: 1px solid var(--hairline); background: var(--surface-0); cursor: pointer;
}
.btn.small { padding: .3rem .6rem; font-size: .85rem; }
.btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.btn.danger { border-color: var(--danger-border); background: var(--danger-bg); color: var(--danger-text); }
.btn.outline { border-color: var(--info-border); background: var(--info-bg); color: var(--info-text); font-weight: 600; justify-self: start; }
.btn.ghost { border-color: var(--hairline); background: var(--surface-0); }
.btn:disabled { opacity: .6; cursor: default; }

.fold { border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0); }
.fold + .fold { margin-top: .5rem; }
.fold-head {
  width: 100%; display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: .5rem;
  padding: .5rem .75rem; background: var(--btn-bg); border: 0; border-bottom: 1px solid var(--hairline);
  cursor: pointer; text-align: left;
}
.fold-title { font-weight: 600; font-size: .95rem; }
.fold-summary {
  color: var(--text-muted); opacity: .8; white-space: nowrap; overflow: hidden;
  text-overflow: ellipsis; text-align: right; font-size: .85rem;
}
.chev { transition: transform .15s ease; }
.chev.open { transform: rotate(90deg); }
.fold-body { padding: .6rem .75rem; display: grid; gap: .5rem; }

.chip-grid { display: flex; flex-wrap: wrap; gap: .4rem; }
.chip-toggle {
  display: inline-flex; align-items: center; gap: .4rem; padding: .3rem .7rem;
  border: 1px solid var(--hairline); border-radius: 1rem; background: var(--surface-0);
  font-size: .85rem; cursor: pointer; user-select: none;
}
.chip-toggle input { margin: 0; }
.chip-toggle.on { border-color: #4338ca; background: #eef2ff; color: #4338ca; font-weight: 600; }

.field { display: grid; gap: .3rem; }
.field .lbl { font-size: .8rem; font-weight: 600; opacity: .85; }
.field input[type="text"] { padding: .45rem .6rem; border: 1px solid var(--hairline); border-radius: .5rem; }

h3 { margin: .3rem 0 0; font-size: .9rem; }

.campo-row {
  display: grid; gap: .4rem; border: 1px solid var(--hairline); border-radius: .5rem;
  padding: .5rem; background: var(--btn-bg);
}
.campo-row-top { display: flex; flex-wrap: wrap; gap: .4rem; align-items: center; }
.campo-row-top input, .campo-row-top select {
  padding: .4rem .5rem; border: 1px solid var(--hairline); border-radius: .4rem; background: var(--surface-0);
}
.campo-chiave { flex: 1 1 10rem; }
.campo-etichetta { flex: 1 1 10rem; }
.campo-tipo { flex: 0 0 auto; }
.campo-row-flags { display: flex; flex-wrap: wrap; gap: .6rem; align-items: center; }
.campo-placeholder { flex: 1 1 12rem; padding: .4rem .5rem; border: 1px solid var(--hairline); border-radius: .4rem; background: var(--surface-0); }
.chk-row { display: flex; align-items: center; gap: .35rem; font-size: .8rem; }

.campo-opzioni {
  display: grid; gap: .3rem; padding-top: .4rem; border-top: 1px dashed var(--hairline);
}
.opzione-row { display: flex; gap: .4rem; }
.opzione-row input {
  flex: 1; padding: .35rem .5rem; border: 1px solid var(--hairline); border-radius: .4rem; background: var(--surface-0);
}
</style>
