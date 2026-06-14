<script setup lang="ts">
import {onBeforeUnmount, watch} from 'vue'
import useDiceRoll from '../function/useDiceRoll'

// Dado d20 3D (libreria dice-box-threejs) lanciato sopra tutta l'app.
// Il renderer usa sfondo trasparente e un piano che riceve solo le ombre,
// quindi si vede solo il dado (+ ombra) senza sfondo. Il contenitore non
// intercetta i click (pointer-events: none).
const {richiestaTiro, mostraOverlay, tiroCompletato} = useDiceRoll()

const containerId = 'd20-overlay-box'
let box: any = null
let initing: Promise<void> | null = null

// inizializza la box 3D solo al primo tiro (così three.js resta fuori dal caricamento iniziale)
async function assicuraBox() {
  if (box) return
  if (!initing) {
    initing = (async () => {
      // @ts-ignore - libreria senza tipi
      const {default: DiceBox} = await import('@3d-dice/dice-box-threejs')
      box = new DiceBox('#' + containerId, {
        sounds: false,
        shadows: true,
        theme_texture: '',
        theme_colorset: 'white',
        theme_material: 'plastic',
        gravity_multiplier: 400,
        baseScale: 140,
        strength: 0.45, // lancio più morbido: il dado resta vicino al centro
      })
      await box.initialize()
      // parte sempre dal centro dello schermo: forzo a (0,0) la posizione iniziale dei dadi
      const orig = box.getNotationVectors.bind(box)
      box.getNotationVectors = (...args: any[]) => {
        const res = orig(...args)
        res?.vectors?.forEach((v: any) => {
          if (v?.pos) { v.pos.x = 0; v.pos.y = 0 }
        })
        return res
      }
    })()
  }
  await initing
}

async function tira() {
  // Niente tiro predeterminato: lascio decidere alla fisica e poi uso ESATTAMENTE
  // il valore che la libreria legge dal dado fermo. Così il dado che vedi e il
  // valore usato sono per costruzione la stessa faccia (il predeterminato invece
  // saltava lo swap se il d20 si fermava inclinato, mostrando una faccia diversa).
  let valore = 1 + Math.floor(Math.random() * 20) // fallback, normalmente sovrascritto
  try {
    await assicuraBox()
    box?.clearDice?.()
    const esito: any = await box.roll('1d20')
    const letto = Number(esito?.sets?.[0]?.rolls?.[0]?.value ?? esito?.total)
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
  if (!v) { try { box?.clearDice?.() } catch { /* noop */ } }
})

onBeforeUnmount(() => { try { box?.clearDice?.() } catch { /* noop */ } })
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
