<script setup lang="ts">
import {defineAsyncComponent} from 'vue'
import {useRouter} from 'vue-router'
import Icona from "./Icona/Icona.vue";
import DiceD20Overlay from "./DiceD20Overlay.vue";
import usePopup from "../function/usePopup";
import useDiceRoll from "../function/useDiceRoll";

const DiceRollerPopup = defineAsyncComponent(() => import('./DiceRollerPopup.vue'))
const DiceForcePopup = defineAsyncComponent(() => import('./DiceForcePopup.vue'))

const router = useRouter()
const {openPopup} = usePopup()
const {risultato, mostraOverlay, lanciaD20, annulla} = useDiceRoll()
const goHome = () => router.push({path: '/'})
const goCompendio = () => router.push({path: '/compendio'})
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
  if (!longPress) lanciaD20() // tap breve = lancio del dado
}

function d20Cancel() {
  if (pressTimer) { clearTimeout(pressTimer); pressTimer = null }
}
</script>

<template>
  <header class="upperbar">
    <div class="title">
      <slot/>
    </div>

    <!-- icone a destra -->
    <div class="bar-icons">
      <!-- tiro globale d20: risultato scritto a fianco per 15s -->
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

      <Icona name="DADO" title="Lancia i dadi" @click="apriDadi"></Icona>
      <Icona name="COMPENDIO" @click="goCompendio"></Icona>
      <Icona name="HAMBURGER" @click="goHome"></Icona>
    </div>
  </header>

  <DiceD20Overlay/>
</template>

<style scoped>
.bar-icons {
  display: inline-flex;
  align-items: center;
  gap: 1rem;
}

.d20-trigger {
  display: inline-flex;
  align-items: center;
  gap: .4rem;
}

.d20-hit {
  display: inline-flex;
  cursor: pointer;
  touch-action: none;       /* evita scroll/menu contestuale sul long-press mobile */
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

.d20-chip:hover {
  background: #dc2626; /* rosso: suggerisce l'annullamento */
}

@keyframes d20-chip-in {
  from { opacity: 0; transform: translateX(-4px) scale(.8); }
  to   { opacity: 1; transform: none; }
}
</style>
