<script setup lang="ts">
import {computed, ref} from 'vue'

interface Dado {
  key: string
  sides: number
}

const DADI: Dado[] = [
  {key: 'Moneta', sides: 2},
  {key: 'd3', sides: 3}, {key: 'd4', sides: 4}, {key: 'd5', sides: 5}, {key: 'd6', sides: 6},
  {key: 'd7', sides: 7}, {key: 'd8', sides: 8}, {key: 'd9', sides: 9}, {key: 'd10', sides: 10},
  {key: 'd11', sides: 11}, {key: 'd12', sides: 12}, {key: 'd15', sides: 15}, {key: 'd20', sides: 20},
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

const isMoneta = (key: string) => key === 'Moneta'
const notazioneVoce = (v: VocePool) => isMoneta(v.key) ? `${v.count}× Moneta` : `${v.count}${v.key}`
const facciaTesto = (key: string, val: number) => isMoneta(key) ? (val === 2 ? 'Testa' : 'Croce') : String(val)

// pool ordinato per preservare l'ordine di inserimento nella notazione
const pool = ref<VocePool[]>([])
const risultati = ref<GruppoRisultato[]>([])

const notazione = computed(() => pool.value.map(notazioneVoce).join(' + '))
const poolVuoto = computed(() => pool.value.length === 0)

// somma numerica complessiva (la moneta vale 1/2 come un d2)
const totale = computed(() =>
    risultati.value.reduce((s, g) => s + g.tiri.reduce((a, b) => a + b, 0), 0)
)
const haRisultati = computed(() => risultati.value.length > 0)

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
      <div class="ris-righe">
        <div v-for="g in risultati" :key="g.key" class="ris-riga">
          <span class="ris-dado">{{ g.key }}</span>
          <span class="ris-tiri">{{ g.testi.join(', ') }}</span>
        </div>
      </div>
      <div class="ris-totale">
        <span>Totale</span>
        <span class="ris-totale-val">{{ totale }}</span>
      </div>
    </div>

    <!-- azioni -->
    <div class="azioni">
      <button type="button" class="btn primary" :disabled="poolVuoto" @click="tira">Tira</button>
      <button type="button" class="btn" :disabled="poolVuoto && !haRisultati" @click="pulisci">Pulisci</button>
    </div>

    <!-- quantità da aggiungere a ogni click -->
    <label class="qta-row">
      <span>Quantità per dado</span>
      <input v-model.number="quantita" type="number" min="1" step="1"/>
    </label>

    <!-- dadi disponibili -->
    <div class="dadi-grid">
      <button
          v-for="d in DADI" :key="d.key" type="button"
          class="dado-btn"
          @click="aggiungi(d)"
      >{{ d.key }}</button>
    </div>
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
  padding: .6rem;
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  background: #fff;
}

.ris-righe {
  display: flex;
  flex-direction: column;
  gap: .25rem;
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

.ris-tiri {
  font-weight: 600;
  color: #111827;
  font-variant-numeric: tabular-nums;
}

.ris-totale {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  border-top: 1px solid #e5e7eb;
  padding-top: .4rem;
  font-weight: 700;
  color: #374151;
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

.qta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: .6rem;
  font-size: .9rem;
  font-weight: 600;
  color: #374151;
}

.qta-row input {
  width: 5rem;
  padding: .4rem .5rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.dadi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(4rem, 1fr));
  gap: .4rem;
}

.dado-btn {
  padding: .5rem .3rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  background: #fff;
  font-weight: 700;
  cursor: pointer;
}

.dado-btn:hover { background: #eff6ff; border-color: #2563eb; color: #1d4ed8; }
.dado-btn:active { transform: scale(.96); }
</style>
