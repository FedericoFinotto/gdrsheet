<script setup lang="ts">
import {computed, defineAsyncComponent, onMounted, onUnmounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import Icona from "./Icona/Icona.vue";
import DiceD20Overlay from "./DiceD20Overlay.vue";
import usePopup from "../function/usePopup";
import useDiceRoll from "../function/useDiceRoll";
import {useAuthStore} from '../stores/auth'
import {useMondoStore} from '../stores/mondo'
import {useCharacterStore} from '../stores/personaggio'
import {getNotizie, getViste as getVisteNotizie, segnaViste as segnaVisteNotizie, type NotiziaDTO} from '../service/NotizieService'
import NotiziePopup from './NotiziePopup.vue'
import {catturaScreenshot} from '../function/reportScreenshot'
import {listaSegnalazioni} from '../service/SegnalazioniService'
import {contaNonLette} from '../function/segnalazioniNotifiche'
import {getTheme, toggleTheme} from '../function/useTheme'
import {resetCachePersonaggio} from '../service/PersonaggioService'

const DiceRollerPopup = defineAsyncComponent(() => import('./DiceRollerPopup.vue'))
const DiceForcePopup = defineAsyncComponent(() => import('./DiceForcePopup.vue'))
const SegnalazioneCreatePopup = defineAsyncComponent(() => import('./SegnalazioneCreatePopup.vue'))

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const characterStore = useCharacterStore()
const mondoStore = useMondoStore()
const {openPopup, isVisible: popupAperto} = usePopup()
const {risultato, mostraOverlay, lanciaD20, annulla} = useDiceRoll()

const apriDadi = () => openPopup(DiceRollerPopup, {}, {closable: true, autoClose: 0})

// d20: tap = tira il dado; pressione lunga = popup per forzare un valore (1..20)
let pressTimer: ReturnType<typeof setTimeout> | null = null
let longPress = false

function d20Down() {
  longPress = false
  pressTimer = setTimeout(() => {
    longPress = true
    pressTimer = null
    openPopup(DiceForcePopup, {}, {closable: true, autoClose: 0})
  }, 450)
}

function d20Up() {
  if (pressTimer) { clearTimeout(pressTimer); pressTimer = null }
  if (!longPress) lanciaD20()
}

function d20Cancel() {
  if (pressTimer) { clearTimeout(pressTimer); pressTimer = null }
}

// hamburger menu
const menuAperto = ref(false)
const confermaEsci = ref(false)
function apriMenu() { menuAperto.value = true }
function chiudiMenu() { menuAperto.value = false; confermaEsci.value = false }

const canManageUsers = computed(() => {
  const r = auth.effectiveRuolo.toUpperCase()
  return r === 'MASTER' || r === 'ADMIN' || r === 'SUPERUSER'
})

// Personaggio aperto in questo momento (solo sulla scheda, route "Scheda" — vedi router/index.js):
// serve per il tasto "Reset cache", che agisce su QUEL personaggio.
const idPersonaggioAperto = computed<number | null>(() => {
  if (route.name !== 'Scheda') return null
  const n = Number(route.params.id)
  return Number.isFinite(n) ? n : null
})

const resettingCache = ref(false)
async function handleResetCache() {
  const id = idPersonaggioAperto.value
  if (!id || resettingCache.value) return
  resettingCache.value = true
  try {
    await resetCachePersonaggio(id)
    // ricarica subito i dati del personaggio aperto, coerente col nuovo stato della cache
    await characterStore.fetchCharacter(id, true)
  } catch (e) {
    console.error('Errore reset cache personaggio:', e)
  } finally {
    resettingCache.value = false
    chiudiMenu()
  }
}

// il reload va rimandato al prossimo tick: chiamarlo subito nello stesso handler del click
// fa gareggiare la navigazione col teardown di Vue Router, generando un errore in console
// (innocuo: il reload comunque avviene) ma inutile da vedere ogni volta.
function toggleAdminMode() {
  auth.setAdminMode(!auth.adminMode)
  setTimeout(() => window.location.reload(), 0)
}

// Tema chiaro/scuro
const temaScuro = ref(getTheme() === 'dark')
function onToggleTema() {
  toggleTheme()
  temaScuro.value = getTheme() === 'dark'
}

// aggiorna app
const aggiornando = ref(false)
async function forzaAggiornamento() {
  if (aggiornando.value) return
  aggiornando.value = true
  chiudiMenu()
  try {
    if ('serviceWorker' in navigator) {
      const regs = await navigator.serviceWorker.getRegistrations()
      await Promise.all(regs.map(r => r.unregister()))
    }
    if ('caches' in window) {
      const keys = await caches.keys()
      await Promise.all(keys.map(k => caches.delete(k)))
    }
  } catch (e) {
    console.error('Errore aggiornamento app:', e)
  } finally {
    // Naviga alla root con un parametro timestamp: il browser è costretto a
    // ri-scaricare index.html dal server (cache-busting), e i nuovi bundle
    // Vite hanno hash diversi quindi vengono scaricati freschi.
    window.location.replace('/?_=' + Date.now())
  }
}

function onLogout() {
  auth.logout()
  mondoStore.reset()
  router.replace('/login')
}

function naviga(path: string) {
  chiudiMenu()
  router.push(path)
}

// Notizie
const notizie = ref<NotiziaDTO[]>([])
const notizieAperte = ref(false)
const notizieViste = ref<Set<number>>(new Set())

const notizieNonViste = computed(() =>
    notizie.value.filter(n => !n.archiviata && !notizieViste.value.has(n.id)).length
)

async function caricaNotizie() {
  if (!localStorage.getItem('auth_token')) return
  try {
    const [resNotizie, resViste] = await Promise.all([getNotizie(), getVisteNotizie()])
    notizie.value = resNotizie.data
    notizieViste.value = new Set(Object.keys(resViste.data ?? {}).map(Number))
  } catch {
    notizie.value = []
  }
}

onMounted(caricaNotizie)

// Mondi tra cui l'utente può switchare (master di più mondi, o admin con più mondi esistenti):
// caricati una volta all'avvio, come notizie/segnalazioni sopra.
onMounted(() => {
  if (!localStorage.getItem('auth_token')) return
  mondoStore.carica()
})

const mondoAperto = ref(false)
function selezionaMondo(mondoId: number) {
  mondoStore.seleziona(mondoId)
  mondoAperto.value = false
  chiudiMenu()
}

function apriNotizie() {
  notizieAperte.value = true
  const idsDaSegnare = notizie.value.map(n => n.id).filter(id => !notizieViste.value.has(id))
  if (idsDaSegnare.length === 0) return
  for (const id of idsDaSegnare) notizieViste.value.add(id)
  segnaVisteNotizie(idsDaSegnare).catch(e => console.error('Errore salvataggio viste notizie:', e))
}

// Segnalazioni: cattura uno screenshot della pagina corrente "sottobanco" prima di navigare via,
// e mostra un badge se ci sono variazioni non ancora viste sulle proprie segnalazioni
const segnalazioniNonLette = ref(0)

async function caricaSegnalazioniNonLette() {
  if (!localStorage.getItem('auth_token')) return
  try {
    const res = await listaSegnalazioni(false)
    segnalazioniNonLette.value = await contaNonLette(res.data)
  } catch {
    segnalazioniNonLette.value = 0
  }
}

onMounted(caricaSegnalazioniNonLette)

async function apriSegnalazioni() {
  await catturaScreenshot()
  router.push('/segnalazioni')
}

function apriNotizieDaMenu() {
  chiudiMenu()
  apriNotizie()
}

function apriSegnalazioniDaMenu() {
  chiudiMenu()
  apriSegnalazioni()
}

// Scorciatoia globale (Ctrl+Shift+S): apre direttamente il form di creazione segnalazione con
// uno screenshot del momento esatto in cui viene premuta, anche se in quel momento è già aperto
// un altro popup (che sostituisce, dato che il sistema di popup è un singleton) — serve a poter
// segnalare un problema anche quando un popup bloccherebbe l'accesso ai tastini della barra.
async function apriSegnalazioneRapida() {
  await catturaScreenshot()
  openPopup(SegnalazioneCreatePopup, {}, {closable: true, autoClose: 0})
}

function onKeydownGlobale(e: KeyboardEvent) {
  if (e.ctrlKey && e.shiftKey && e.key.toLowerCase() === 's') {
    e.preventDefault()
    apriSegnalazioneRapida()
  }
}

onMounted(() => window.addEventListener('keydown', onKeydownGlobale))
onUnmounted(() => window.removeEventListener('keydown', onKeydownGlobale))
</script>

<template>
  <header class="upperbar">
    <!-- sinistra: home + dadi -->
    <div class="bar-left">
      <Icona name="HOME" title="Home" @click="router.push('/')"/>

      <span class="d20-trigger">
        <span
            class="d20-hit"
            @pointerdown="d20Down"
            @pointerup="d20Up"
            @pointerleave="d20Cancel"
            @contextmenu.prevent
        >
          <Icona name="DADO_D20" title="Tira un d20 (tieni premuto per forzare un valore)"/>
        </span>
        <button
            v-if="risultato !== null && !mostraOverlay"
            type="button" class="d20-chip" title="Annulla il tiro"
            @click="annulla"
        >{{ risultato }}</button>
      </span>

      <Icona name="DADO" title="Lancia i dadi" @click="apriDadi"/>
    </div>

    <!-- centro: slot titolo (usato dalle pagine che vogliono un titolo nella navbar) -->
    <div class="bar-title">
      <slot/>
    </div>

    <!-- destra: segnalazioni + campanella notizie + hamburger -->
    <div class="bar-right">
      <button v-if="segnalazioniNonLette > 0" class="bell-btn" title="Segnalazioni" @click="apriSegnalazioni">
        <i class="fa-solid fa-bug"></i>
        <span class="bell-badge">{{ segnalazioniNonLette }}</span>
      </button>
      <button v-if="notizieNonViste > 0" class="bell-btn" title="Notizie" @click="apriNotizie">
        <i class="fa-solid fa-bell"></i>
        <span class="bell-badge">{{ notizieNonViste }}</span>
      </button>
      <Icona name="HAMBURGER" title="Menu" @click="apriMenu"/>
    </div>
  </header>

  <DiceD20Overlay/>

  <!-- tastino segnalazione: visibile solo mentre un popup è aperto (altrimenti coprirebbe
       il resto della barra senza motivo), sopra a qualunque popup/overlay -->
  <Teleport to="body">
    <button v-if="popupAperto" class="fab-segnalazione" data-screenshot-ignore="true"
            title="Segnala un problema (Ctrl+Shift+S)" @click="apriSegnalazioneRapida">
      <i class="fa-solid fa-bug"></i>
    </button>
  </Teleport>

  <!-- popup notizie -->
  <Teleport to="body">
    <NotiziePopup v-if="notizieAperte" :notizie="notizie" @close="notizieAperte = false"/>
  </Teleport>

  <!-- menu slide-in -->
  <Teleport to="body">
    <div v-if="menuAperto" class="menu-overlay" data-screenshot-ignore="true" @click.self="chiudiMenu">
      <div class="menu-panel">
        <div class="menu-header">
          <span class="menu-title">Menu</span>
          <button class="close-btn" @click="chiudiMenu">✕</button>
        </div>
        <nav class="menu-nav">
          <button class="menu-item" @click="naviga('/account')">
            <i class="fa-solid fa-user menu-icon"></i> Account
          </button>
          <button class="menu-item" @click="apriNotizieDaMenu">
            <i class="fa-solid fa-bell menu-icon"></i> Notizie
            <span v-if="notizieNonViste > 0" class="menu-badge">{{ notizieNonViste }}</span>
          </button>
          <button class="menu-item" @click="apriSegnalazioniDaMenu">
            <i class="fa-solid fa-bug menu-icon"></i> Segnalazioni
            <span v-if="segnalazioniNonLette > 0" class="menu-badge">{{ segnalazioniNonLette }}</span>
          </button>

          <!-- Toggle tema chiaro/scuro -->
          <div class="admin-toggle-row">
            <span class="admin-toggle-label">
              <i class="fa-solid fa-moon menu-icon"></i>
              Tema Scuro
            </span>
            <button
              class="toggle-btn"
              :class="{ active: temaScuro }"
              @click="onToggleTema"
              :title="temaScuro ? 'Passa al tema chiaro' : 'Passa al tema scuro'"
            >
              <span class="toggle-track">
                <span class="toggle-thumb"/>
              </span>
              <span class="toggle-status">{{ temaScuro ? 'ON' : 'OFF' }}</span>
            </button>
          </div>

          <!-- Toggle modalità admin (solo per utenti con ruolo ADMIN/SUPERUSER) -->
          <div v-if="auth.isRealAdmin" class="admin-toggle-row">
            <span class="admin-toggle-label">
              <i class="fa-solid fa-shield-halved menu-icon"></i>
              Modalità Admin
            </span>
            <button
              class="toggle-btn"
              :class="{ active: auth.adminMode }"
              @click="toggleAdminMode"
              :title="auth.adminMode ? 'Clicca per disattivare i privilegi admin' : 'Clicca per attivare i privilegi admin'"
            >
              <span class="toggle-track">
                <span class="toggle-thumb"/>
              </span>
              <span class="toggle-status">{{ auth.adminMode ? 'ON' : 'OFF' }}</span>
            </button>
          </div>

          <!-- Switcher mondo: solo se ce n'è più di uno tra cui scegliere (master di più mondi,
               o admin con più mondi esistenti). Le pagine che dipendono dal mondo (compendio,
               creazione item, ricerca profonda...) seguono questa selezione. -->
          <div v-if="mondoStore.mostraSwitcher" class="mondo-switch-row">
            <button type="button" class="mondo-switch-head" @click="mondoAperto = !mondoAperto">
              <i class="fa-solid fa-globe menu-icon"></i>
              <span class="mondo-switch-label">Mondo</span>
              <span class="mondo-corrente">
                {{ mondoStore.disponibili.find(m => m.id === mondoStore.corrente)?.descrizione ?? '—' }}
              </span>
              <i class="fa-solid fa-chevron-down mondo-switch-chevron" :class="{open: mondoAperto}"></i>
            </button>
            <div v-if="mondoAperto" class="mondo-switch-list">
              <button
                  v-for="m in mondoStore.disponibili" :key="m.id" type="button"
                  class="mondo-switch-item" :class="{active: m.id === mondoStore.corrente}"
                  @click="selezionaMondo(m.id)"
              >{{ m.descrizione }}</button>
            </div>
          </div>

          <button v-if="canManageUsers" class="menu-item" @click="naviga('/users')">
            <i class="fa-solid fa-users menu-icon"></i> Gestione Utenti
          </button>
          <button v-if="canManageUsers" class="menu-item" @click="naviga('/stats-admin')">
            <i class="fa-solid fa-chart-bar menu-icon"></i> Gestione Statistiche
          </button>
          <!-- Assegnare il permesso Master di un mondo è una decisione riservata agli admin, non
               delegabile: a differenza delle altre voci qui sopra non basta il ruolo MASTER, e non
               basta nemmeno essere admin — serve la modalità admin attiva (coerente con
               AuthzService.isAdmin lato backend, che sarebbe l'unico a bloccarla comunque). -->
          <button v-if="auth.isRealAdmin && auth.adminMode" class="menu-item" @click="naviga('/mondi-admin')">
            <i class="fa-solid fa-globe menu-icon"></i> Permessi per mondo
          </button>
          <!-- Solo sulla scheda di un personaggio (idPersonaggioAperto): invalida la cache
               (personaggioItems/personaggioModificatori) di QUEL personaggio — utile quando la
               scheda non riflette una modifica appena fatta a una classe/razza condivisa. -->
          <button v-if="auth.isRealAdmin && auth.adminMode && idPersonaggioAperto" class="menu-item"
                  :disabled="resettingCache" @click="handleResetCache">
            <i class="fa-solid fa-arrows-rotate menu-icon"></i> {{ resettingCache ? 'Reset cache…' : 'Reset cache' }}
          </button>
          <button class="menu-item" @click="naviga('/compendio')">
            <i class="fa-solid fa-book menu-icon"></i> Compendio
          </button>
          <!-- scorciatoia master: il compendio già filtrato sui randomizzatori -->
          <button v-if="canManageUsers" class="menu-item" @click="naviga('/compendio?tipo=RANDOMIZZATORE')">
            <i class="fa-solid fa-dice-d20 menu-icon"></i> Randomizzatori
          </button>
          <hr class="menu-sep"/>
          <button class="menu-item" :disabled="aggiornando" @click="forzaAggiornamento">
            <i class="fa-solid fa-rotate menu-icon"></i> {{ aggiornando ? 'Aggiornamento…' : 'Aggiorna App' }}
          </button>
          <button class="menu-item danger" @click="confermaEsci = true">
            <i class="fa-solid fa-right-from-bracket menu-icon"></i> Esci
          </button>

          <div v-if="confermaEsci" class="conferma-esci">
            <p>Sei sicuro di voler uscire?</p>
            <div class="conferma-actions">
              <button class="btn ghost" @click="confermaEsci = false">Annulla</button>
              <button class="btn danger" @click="onLogout">Esci</button>
            </div>
          </div>
        </nav>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.upperbar {
  display: flex;
  align-items: center;
  gap: .5rem;
  padding: 0 1rem;
}

.bar-left {
  display: inline-flex;
  align-items: center;
  gap: 1rem;
  flex-shrink: 0;
}

.bar-title {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bar-right {
  display: inline-flex;
  align-items: center;
  gap: .5rem;
  flex-shrink: 0;
}

.bell-btn {
  position: relative;
  background: transparent;
  border: 0;
  cursor: pointer;
  font-size: 1.2rem;
  color: var(--text-muted);
  padding: .25rem .35rem;
  border-radius: .4rem;
  line-height: 1;
}
.bell-btn:hover { background: var(--btn-bg); }

.bell-badge {
  position: absolute;
  top: -.1rem;
  right: -.1rem;
  min-width: 1.1rem;
  height: 1.1rem;
  background: #dc2626;
  color: #fff;
  font-size: .6rem;
  font-weight: 700;
  border-radius: 9999px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 .2rem;
  line-height: 1;
}

.fab-segnalazione {
  position: fixed;
  top: .4rem;
  left: .4rem;
  z-index: 10050;
  width: 1.9rem;
  height: 1.9rem;
  border-radius: 50%;
  border: 0;
  background: #dc2626;
  color: #fff;
  font-size: .9rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 10px rgba(0,0,0,.35);
}
.fab-segnalazione:hover { background: #b91c1c; }

.d20-trigger {
  display: inline-flex;
  align-items: center;
  gap: .4rem;
}

.d20-hit {
  display: inline-flex;
  cursor: pointer;
  touch-action: none;
  -webkit-user-select: none;
  user-select: none;
  -webkit-touch-callout: none;
}

.d20-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.7rem;
  height: 1.7rem;
  padding: 0 .4rem;
  border: 0;
  border-radius: .5rem;
  background: #2563eb;
  color: #fff;
  font-weight: 800;
  font-size: .95rem;
  font-variant-numeric: tabular-nums;
  cursor: pointer;
  animation: d20-chip-in .2s ease;
}

.d20-chip:hover { background: #dc2626; }

@keyframes d20-chip-in {
  from { opacity: 0; transform: translateX(-4px) scale(.8); }
  to   { opacity: 1; transform: none; }
}

/* overlay */
.menu-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,.35);
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
}

.menu-panel {
  width: min(18rem, 85vw);
  background: var(--surface-0);
  color: var(--text-strong);
  height: 100%;
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 16px rgba(0,0,0,.12);
  overflow-y: auto;
}

.menu-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1rem .75rem;
  border-bottom: 1px solid var(--hairline);
  flex-shrink: 0;
}

.menu-title { font-weight: 700; font-size: 1rem; }

.close-btn {
  background: transparent;
  border: 0;
  cursor: pointer;
  font-size: 1rem;
  color: var(--text-muted);
  padding: .25rem .4rem;
  border-radius: .35rem;
}
.close-btn:hover { background: var(--btn-bg); }

.menu-nav {
  display: flex;
  flex-direction: column;
  padding: .5rem;
  gap: .1rem;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: .65rem;
  padding: .75rem .9rem;
  border: 0;
  border-radius: .5rem;
  background: transparent;
  text-align: left;
  font-size: .95rem;
  cursor: pointer;
  width: 100%;
  color: inherit;
}
.menu-item:hover { background: var(--btn-bg); }
.menu-item.danger { color: var(--error-color); }
.menu-item.danger:hover { background: var(--btn-bg-hover); }
.menu-item:disabled { opacity: .6; cursor: default; }

.menu-icon {
  width: 1.1rem;
  text-align: center;
  flex-shrink: 0;
  opacity: .75;
}

.menu-badge {
  margin-left: auto;
  min-width: 1.3rem;
  height: 1.3rem;
  background: #dc2626;
  color: #fff;
  font-size: .7rem;
  font-weight: 700;
  border-radius: 9999px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 .3rem;
  line-height: 1;
}

.menu-sep { border: 0; border-top: 1px solid var(--hairline); margin: .35rem 0; }

/* Switcher mondo */
.mondo-switch-row { margin: .15rem 0; }
.mondo-switch-head {
  display: flex;
  align-items: center;
  gap: .65rem;
  width: 100%;
  padding: .75rem .9rem;
  border: 0;
  border-radius: .5rem;
  background: transparent;
  text-align: left;
  font-size: .95rem;
  cursor: pointer;
  color: inherit;
}
.mondo-switch-head:hover { background: var(--btn-bg); }
.mondo-switch-label { flex-shrink: 0; }
.mondo-corrente {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  opacity: .7;
  font-size: .85rem;
  text-align: right;
}
.mondo-switch-chevron { flex-shrink: 0; font-size: .8rem; opacity: .6; transition: transform .15s ease; }
.mondo-switch-chevron.open { transform: rotate(180deg); }
.mondo-switch-list {
  display: flex;
  flex-direction: column;
  gap: .1rem;
  padding: .2rem .3rem .2rem 2.2rem;
}
.mondo-switch-item {
  padding: .5rem .6rem;
  border: 0;
  border-radius: .4rem;
  background: transparent;
  text-align: left;
  font-size: .88rem;
  cursor: pointer;
  color: inherit;
}
.mondo-switch-item:hover { background: var(--btn-bg); }
.mondo-switch-item.active { background: var(--surface-hover); color: #1d4ed8; font-weight: 600; }

/* Admin toggle (e tema scuro) */
.admin-toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: .6rem .9rem;
  border-radius: .5rem;
  background: var(--surface-hover);
  border: 1px solid var(--border-color);
  margin: .15rem 0;
}
.admin-toggle-label {
  display: flex;
  align-items: center;
  gap: .65rem;
  font-size: .95rem;
  color: var(--text-strong);
  font-weight: 500;
}
.toggle-btn {
  display: flex;
  align-items: center;
  gap: .4rem;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: .2rem .3rem;
  border-radius: .35rem;
}
.toggle-btn:hover { background: var(--btn-bg-hover); }
.toggle-track {
  width: 2.2rem;
  height: 1.2rem;
  background: var(--tile-gap);
  border-radius: 999px;
  position: relative;
  transition: background .18s;
  display: block;
}
.toggle-btn.active .toggle-track { background: #2563eb; }
.toggle-thumb {
  position: absolute;
  top: .15rem;
  left: .15rem;
  width: .9rem;
  height: .9rem;
  background: #fff;
  border-radius: 50%;
  transition: transform .18s;
  display: block;
  box-shadow: 0 1px 3px rgba(0,0,0,.25);
}
.toggle-btn.active .toggle-thumb { transform: translateX(1rem); }
.toggle-status {
  font-size: .75rem;
  font-weight: 700;
  color: var(--text-muted);
  min-width: 1.5rem;
}
.toggle-btn.active .toggle-status { color: #2563eb; }

.conferma-esci {
  margin-top: .4rem;
  padding: .75rem;
  border: 1px solid var(--border-color);
  border-radius: .5rem;
  background: var(--surface-warn);
  display: flex;
  flex-direction: column;
  gap: .5rem;
}
.conferma-esci p { margin: 0; font-size: .9rem; color: var(--text-strong); }
.conferma-actions { display: flex; gap: .5rem; }

.btn {
  padding: .4rem .75rem;
  border-radius: .5rem;
  border: 1px solid transparent;
  cursor: pointer;
  font-size: .85rem;
}
.btn.ghost { border-color: var(--border-color); background: var(--surface-0); color: var(--text-strong); }
.btn.danger { background: var(--error-color); color: #fff; }
</style>
