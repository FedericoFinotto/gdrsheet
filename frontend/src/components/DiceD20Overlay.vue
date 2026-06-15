<script setup lang="ts">
import {onBeforeUnmount, watch} from 'vue'
import useDiceRoll from '../function/useDiceRoll'

// Dado d20 3D con @3d-dice/dice-box: usa modelli reali con numerazione standard
// (dado equilibrato, niente spindown). Renderizza su canvas trasparente sopra
// tutta l'app; il contenitore non intercetta i click (pointer-events: none).
const {richiestaTiro, mostraOverlay, tiroCompletato} = useDiceRoll()

const containerId = 'd20-overlay-box'
let box: any = null
let initing: Promise<void> | null = null

// inizializza la box 3D solo al primo tiro (Babylon + Ammo restano fuori dal load iniziale)
async function assicuraBox() {
  if (box) return
  if (!initing) {
    initing = (async () => {
      const {default: DiceBox} = await import('@3d-dice/dice-box')
      box = new DiceBox({
        container: '#' + containerId,
        assetPath: '/assets/dice-box/',
        theme: 'default',
        scale: 7,
        enableShadows: true,
        shadowTransparency: 0.5,
        lightIntensity: 1.1,
      })
      await box.init()
    })()
  }
  await initing
}

async function tira() {
  // la libreria tira il dado vero e restituisce la faccia uscita: numero mostrato
  // e valore usato coincidono per costruzione.
  let valore = 1 + Math.floor(Math.random() * 20) // fallback
  try {
    await assicuraBox()
    box?.clear?.()
    const esito: any = await box.roll('1d20')
    const letto = Number(Array.isArray(esito) ? esito[0]?.value : esito?.value)
    if (Number.isFinite(letto) && letto >= 1 && letto <= 20) valore = letto
  } catch (e) {
    console.error('Errore tiro d20 in sovraimpressione:', e)
  } finally {
    tiroCompletato(valore)
  }
}

watch(richiestaTiro, (n) => { if (n > 0) tira() })

// a fine vita del risultato, rimuovi il dado dalla scena
watch(mostraOverlay, (v) => {
  if (!v) { try { box?.clear?.() } catch { /* noop */ } }
})

onBeforeUnmount(() => { try { box?.clear?.() } catch { /* noop */ } })
</script>

<template>
  <Teleport to="body">
    <div :id="containerId" class="d20-overlay"/>
  </Teleport>
</template>

<style scoped>
/* a tutto schermo, sopra l'app, senza sfondo e senza intercettare i click */
.d20-overlay {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 9999;
}

.d20-overlay :deep(canvas) {
  width: 100% !important;
  height: 100% !important;
  display: block;
}
</style>
