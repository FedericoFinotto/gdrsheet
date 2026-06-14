// Stato condiviso del "tiro globale" del d20. Il dado 3D viene lanciato in
// sovraimpressione (vedi DiceD20Overlay.vue); il risultato resta poi nella barra
// in alto in modo persistente e, finché è attivo, va sommato ai valori di
// caratteristiche/difese/attacchi (schermata info) e a tutte le abilità.
// Si annulla cliccando il valore nella barra (annulla()).
import {ref, type Ref} from 'vue'

const DURATA_RISULTATO = 300   // quanto resta il dado a schermo dopo essersi fermato (ms)

// Stato ancorato a un Symbol globale: così resta un singleton condiviso da TUTTI
// i componenti anche se il modulo viene rivalutato (es. HMR in dev), evitando che
// chip e somme finiscano a leggere due copie diverse di `risultato`.
interface DiceRollState {
  mostraOverlay: Ref<boolean>
  rolling: Ref<boolean>
  risultato: Ref<number | null>
  richiestaTiro: Ref<number>
  overlayId: ReturnType<typeof setTimeout> | null
}

const KEY = Symbol.for('dnd.diceRollState')
const g = globalThis as unknown as Record<symbol, DiceRollState>
const state: DiceRollState = g[KEY] ?? (g[KEY] = {
  mostraOverlay: ref(false),   // tiro in corso / dado a schermo
  rolling: ref(false),
  risultato: ref<number | null>(null),   // risultato attivo (anche bonus da sommare)
  richiestaTiro: ref(0),       // contatore osservato dall'overlay per far partire il tiro
  overlayId: null,
})

const {mostraOverlay, rolling, risultato, richiestaTiro} = state

function clearTimers() {
  if (state.overlayId) clearTimeout(state.overlayId)
  state.overlayId = null
}

// avvia un tiro: l'overlay (che possiede il dado 3D) reagisce a richiestaTiro
function lanciaD20() {
  if (mostraOverlay.value) return // un tiro alla volta
  clearTimers()
  risultato.value = null
  rolling.value = true
  mostraOverlay.value = true
  richiestaTiro.value++
}

// chiamata dall'overlay quando il dado 3D si è fermato
function tiroCompletato(valore: number) {
  risultato.value = valore
  rolling.value = false
  // il dado resta a schermo un attimo, poi sparisce; il risultato resta nella barra
  state.overlayId = setTimeout(() => {
    mostraOverlay.value = false // l'overlay pulisce il dado
  }, DURATA_RISULTATO)
}

// forza un valore senza lanciare il dado (pressione lunga -> scelta 1..20)
function forzaValore(n: number) {
  clearTimers()
  rolling.value = false
  mostraOverlay.value = false
  risultato.value = n
}

// annulla il risultato attivo e le somme (click sul valore nella barra)
function annulla() {
  clearTimers()
  risultato.value = null
  rolling.value = false
  mostraOverlay.value = false
}

export default function useDiceRoll() {
  return {mostraOverlay, rolling, risultato, richiestaTiro, lanciaD20, tiroCompletato, forzaValore, annulla}
}
