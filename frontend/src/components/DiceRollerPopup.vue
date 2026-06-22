<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'

interface Dado {
  key: string
  sides: number
}

const DADI_STANDARD: Dado[] = [
  {key: 'd4', sides: 4}, {key: 'd6', sides: 6}, {key: 'd8', sides: 8},
  {key: 'd10', sides: 10}, {key: 'd12', sides: 12}, {key: 'd20', sides: 20},
]

const DADI_SPECIALI: Dado[] = [
  {key: 'd3', sides: 3}, {key: 'd5', sides: 5}, {key: 'd7', sides: 7},
  {key: 'd9', sides: 9}, {key: 'd11', sides: 11}, {key: 'd15', sides: 15},
  {key: 'd24', sides: 24}, {key: 'd30', sides: 30}, {key: 'd50', sides: 50}, {key: 'd100', sides: 100},
]

interface VocePool {
  key: string
  sides: number
  count: number
}

interface GruppoRisultato {
  key: string
  sides: number
  tiri: number[]      // valori numerici grezzi (1..sides)
  testi: string[]     // rappresentazione mostrata (numero o Testa/Croce)
}

const props = defineProps<{ initialFormula?: string }>()

const notazioneVoce = (v: VocePool) => `${v.count}${v.key}`
const facciaTesto = (_key: string, val: number) => String(val)

// pool ordinato per preservare l'ordine di inserimento nella notazione
const pool = ref<VocePool[]>([])
const risultati = ref<GruppoRisultato[]>([])
const flatBonus = ref(0)  // bonus fisso da aggiungere al totale (es. +169 dalla formula danno)

const notazione = computed(() => {
  const diceStr = pool.value.map(notazioneVoce).join('+')
  if (!flatBonus.value) return diceStr
  return flatBonus.value > 0 ? `${diceStr}+${flatBonus.value}` : `${diceStr}${flatBonus.value}`
})
const poolVuoto = computed(() => pool.value.length === 0)

const totale = computed(() => {
  const diceSum = risultati.value.reduce((s, g) => s + g.tiri.reduce((a, b) => a + b, 0), 0)
  return diceSum + (haRisultati.value ? flatBonus.value : 0)
})
const haRisultati = computed(() => risultati.value.length > 0)

function parseFormula(formula: string) {
  pool.value = []
  flatBonus.value = 0
  risultati.value = []
  // Normalizza separatori: gestisce sia "16d8+14d6+171d10+169" che con spazi
  const parts = formula.replace(/\s+/g, '').replace(/-/g, '+-').split('+').filter(Boolean)
  for (const part of parts) {
    const m = part.match(/^(-?\d+)d(\d+)$/i)
    if (m) {
      const count = parseInt(m[1])
      const sides = parseInt(m[2])
      if (count > 0 && sides > 0) {
        const key = `d${sides}`
        const ex = pool.value.find(v => v.key === key)
        if (ex) ex.count += count
        else pool.value.push({key, sides, count})
      }
    } else {
      const num = parseInt(part)
      if (!isNaN(num)) flatBonus.value += num
    }
  }
}

onMounted(() => {
  if (props.initialFormula) parseFormula(props.initialFormula)
})

// quantità di dadi da aggiungere a ogni click sul dado
const quantita = ref(1)

function aggiungi(d: Dado) {
  const n = Math.max(1, Math.floor(Number(quantita.value) || 1))
  const esistente = pool.value.find(v => v.key === d.key)
  if (esistente) esistente.count += n
  else pool.value.push({key: d.key, sides: d.sides, count: n})
}

function rimuovi(v: VocePool) {
  if (v.count > 1) v.count--
  else pool.value = pool.value.filter(x => x.key !== v.key)
}

function pulisci() {
  pool.value = []
  risultati.value = []
  flatBonus.value = 0
}

function tira() {
  if (poolVuoto.value) return
  risultati.value = pool.value.map(v => {
    const tiri: number[] = []
    for (let i = 0; i < v.count; i++) tiri.push(1 + Math.floor(Math.random() * v.sides))
    return {
      key: v.key,
      sides: v.sides,
      tiri,
      testi: tiri.map(t => facciaTesto(v.key, t)),
    }
  })
}
</script>

<template>
  <div class="dice-roller">
    <h3 class="dr-title">Lancia i dadi</h3>

    <!-- pool composto -->
    <div class="pool" :class="{ vuoto: poolVuoto }">
      <template v-if="!poolVuoto">
        <button
            v-for="v in pool" :key="v.key" type="button"
            class="pool-chip" :title="`Rimuovi un ${v.key}`"
            @click="rimuovi(v)"
        >{{ notazioneVoce(v) }}</button>
      </template>
      <span v-else class="pool-hint">Premi i dadi qui sotto per comporre il tiro…</span>
    </div>

    <!-- risultato -->
    <div v-if="haRisultati" class="risultato">
      <div class="ris-totale">
        <span>Totale</span>
        <span class="ris-totale-val">{{ totale }}</span>
      </div>
      <div class="ris-righe">
        <div v-if="flatBonus" class="ris-riga">
          <span class="ris-dado">Bonus</span>
          <span class="ris-somma ris-somma--bonus">{{ flatBonus > 0 ? `+${flatBonus}` : flatBonus }}</span>
        </div>
        <div v-for="g in risultati" :key="g.key" class="ris-riga">
          <span class="ris-dado">{{ g.key }}</span>
          <span class="ris-somma">({{ g.tiri.reduce((a, b) => a + b, 0) }})</span>
          <span class="ris-tiri">{{ g.testi.join(', ') }}</span>
        </div>
      </div>
    </div>

    <!-- azioni -->
    <div class="azioni">
      <button type="button" class="btn primary" :disabled="poolVuoto" @click="tira">Tira</button>
      <button type="button" class="btn" :disabled="poolVuoto && !haRisultati" @click="pulisci">Pulisci</button>
    </div>

    <!-- riga opzioni: quantità per dado + bonus fisso -->
    <div class="opzioni-row">
      <label class="opzione-label">
        <span>Qtà per dado</span>
        <input v-model.number="quantita" type="number" min="1" step="1"/>
      </label>
      <label class="opzione-label bonus-label">
        <span>Bonus fisso</span>
        <input v-model.number="flatBonus" type="number" step="1" placeholder="0"/>
      </label>
    </div>

    <!-- dadi standard -->
    <div class="dadi-grid dadi-standard">
      <button
          v-for="d in DADI_STANDARD" :key="d.key" type="button"
          class="dado-btn dado-btn--std"
          @click="aggiungi(d)"
      >{{ d.key }}</button>
    </div>

    <!-- dadi speciali -->
    <details class="dadi-speciali-wrap">
      <summary class="dadi-speciali-toggle">Dadi speciali</summary>
      <div class="dadi-grid dadi-speciali">
        <button
            v-for="d in DADI_SPECIALI" :key="d.key" type="button"
            class="dado-btn"
            @click="aggiungi(d)"
        >{{ d.key }}</button>
      </div>
    </details>
  </div>
</template>

<style scoped>
.dice-roller {
  display: flex;
  flex-direction: column;
  gap: .6rem;
}

.dr-title {
  margin: 0;
  font-size: 1.05rem;
}

.pool {
  min-height: 2.6rem;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: .4rem;
  padding: .5rem;
  border: 1px dashed #d0d5dd;
  border-radius: .6rem;
  background: #f9fafb;
}

.pool.vuoto { justify-content: center; }

.pool-hint {
  color: #9ca3af;
  font-size: .9rem;
}

.pool-chip {
  padding: .3rem .6rem;
  border: 1px solid #2563eb;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 700;
  cursor: pointer;
}

.pool-chip:hover {
  background: #fee2e2;
  border-color: #ef4444;
  color: #b91c1c;
}

.risultato {
  display: flex;
  flex-direction: column;
  gap: .4rem;
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  background: #fff;
  overflow: hidden;
}

.ris-totale {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: .5rem .6rem;
  font-weight: 700;
  color: #374151;
  border-bottom: 1px solid #e5e7eb;
}

.ris-righe {
  display: flex;
  flex-direction: column;
  gap: .25rem;
  max-height: 9rem;
  overflow-y: auto;
  padding: .4rem .6rem;
}

.ris-riga {
  display: flex;
  gap: .6rem;
  align-items: baseline;
}

.ris-dado {
  min-width: 3.5rem;
  font-size: .85rem;
  font-weight: 600;
  color: #6b7280;
}

.ris-somma {
  min-width: 3rem;
  font-weight: 700;
  color: #374151;
  font-variant-numeric: tabular-nums;
}

.ris-somma--bonus {
  color: #7c3aed;
}

.ris-tiri {
  font-weight: 400;
  color: #6b7280;
  font-variant-numeric: tabular-nums;
  font-size: .85rem;
}


.ris-totale-val {
  font-size: 1.8rem;
  font-weight: 800;
  color: #1d4ed8;
  font-variant-numeric: tabular-nums;
}

.azioni {
  display: flex;
  gap: .5rem;
}

.btn {
  flex: 1;
  padding: .55rem .8rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  background: #fff;
  font-weight: 700;
  cursor: pointer;
}

.btn:hover { background: #f3f4f6; }
.btn.primary { border-color: #2563eb; background: #2563eb; color: #fff; }
.btn.primary:hover { background: #1d4ed8; }
.btn:disabled { opacity: .5; cursor: default; }

.opzioni-row {
  display: flex;
  gap: .6rem;
}

.opzione-label {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: .25rem;
  font-size: .85rem;
  font-weight: 600;
  color: #374151;
}

.opzione-label input {
  width: 100%;
  box-sizing: border-box;
  padding: .4rem .5rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  text-align: center;
  font-variant-numeric: tabular-nums;
  font-size: .95rem;
}

.bonus-label input {
  border-color: #a78bfa;
  background: #faf5ff;
  color: #6d28d9;
  font-weight: 700;
}

.bonus-label input:focus {
  outline: 2px solid #7c3aed;
  outline-offset: 1px;
}

.dadi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(4rem, 1fr));
  gap: .4rem;
}

.dadi-standard {
  grid-template-columns: repeat(6, 1fr);
}

.dado-btn {
  padding: .5rem .3rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  background: #fff;
  font-weight: 700;
  cursor: pointer;
  font-size: .9rem;
}

.dado-btn--std {
  padding: .7rem .3rem;
  font-size: 1rem;
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.dado-btn:hover { background: #eff6ff; border-color: #2563eb; color: #1d4ed8; }
.dado-btn--std:hover { background: #dbeafe; }
.dado-btn:active { transform: scale(.96); }

.dadi-speciali-wrap {
  font-size: .9rem;
}

.dadi-speciali-toggle {
  cursor: pointer;
  font-weight: 600;
  color: #6b7280;
  padding: .2rem 0;
  user-select: none;
}

.dadi-speciali {
  margin-top: .4rem;
}
</style>
