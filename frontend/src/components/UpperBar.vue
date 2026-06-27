<script setup lang="ts">
import {computed, defineAsyncComponent, ref} from 'vue'
import {useRouter} from 'vue-router'
import Icona from "./Icona/Icona.vue";
import DiceD20Overlay from "./DiceD20Overlay.vue";
import usePopup from "../function/usePopup";
import useDiceRoll from "../function/useDiceRoll";
import {useAuthStore} from '../stores/auth'

const DiceRollerPopup = defineAsyncComponent(() => import('./DiceRollerPopup.vue'))
const DiceForcePopup = defineAsyncComponent(() => import('./DiceForcePopup.vue'))

const router = useRouter()
const auth = useAuthStore()
const {openPopup} = usePopup()
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
  const r = (auth.utente?.ruolo ?? '').toUpperCase()
  return r === 'MASTER' || r === 'ADMIN' || r === 'SUPERUSER'
})

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
    const url = new URL(window.location.href)
    url.searchParams.set('_', String(Date.now()))
    window.location.replace(url.toString())
  }
}

function onLogout() {
  auth.logout()
  router.replace('/login')
}

function naviga(path: string) {
  chiudiMenu()
  router.push(path)
}
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

    <!-- destra: hamburger -->
    <div class="bar-right">
      <Icona name="HAMBURGER" title="Menu" @click="apriMenu"/>
    </div>
  </header>

  <DiceD20Overlay/>

  <!-- menu slide-in -->
  <Teleport to="body">
    <div v-if="menuAperto" class="menu-overlay" @click.self="chiudiMenu">
      <div class="menu-panel">
        <div class="menu-header">
          <span class="menu-title">Menu</span>
          <button class="close-btn" @click="chiudiMenu">✕</button>
        </div>
        <nav class="menu-nav">
          <button class="menu-item" @click="naviga('/account')">
            <i class="fa-solid fa-user menu-icon"></i> Account
          </button>
          <button v-if="canManageUsers" class="menu-item" @click="naviga('/users')">
            <i class="fa-solid fa-users menu-icon"></i> Gestione Utenti
          </button>
          <button v-if="canManageUsers" class="menu-item" @click="naviga('/stats-admin')">
            <i class="fa-solid fa-chart-bar menu-icon"></i> Gestione Statistiche
          </button>
          <button class="menu-item" @click="naviga('/compendio')">
            <i class="fa-solid fa-book menu-icon"></i> Compendio
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
  flex-shrink: 0;
}

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
  background: #fff;
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
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.menu-title { font-weight: 700; font-size: 1rem; }

.close-btn {
  background: transparent;
  border: 0;
  cursor: pointer;
  font-size: 1rem;
  color: #6b7280;
  padding: .25rem .4rem;
  border-radius: .35rem;
}
.close-btn:hover { background: #f3f4f6; }

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
.menu-item:hover { background: #f3f4f6; }
.menu-item.danger { color: #dc2626; }
.menu-item.danger:hover { background: #fef2f2; }
.menu-item:disabled { opacity: .6; cursor: default; }

.menu-icon {
  width: 1.1rem;
  text-align: center;
  flex-shrink: 0;
  opacity: .75;
}

.menu-sep { border: 0; border-top: 1px solid #e5e7eb; margin: .35rem 0; }

.conferma-esci {
  margin-top: .4rem;
  padding: .75rem;
  border: 1px solid #fecaca;
  border-radius: .5rem;
  background: #fef2f2;
  display: flex;
  flex-direction: column;
  gap: .5rem;
}
.conferma-esci p { margin: 0; font-size: .9rem; color: #7f1d1d; }
.conferma-actions { display: flex; gap: .5rem; }

.btn {
  padding: .4rem .75rem;
  border-radius: .5rem;
  border: 1px solid transparent;
  cursor: pointer;
  font-size: .85rem;
}
.btn.ghost { border-color: #d0d5dd; background: #fff; color: #374151; }
.btn.danger { background: #dc2626; color: #fff; }
</style>
