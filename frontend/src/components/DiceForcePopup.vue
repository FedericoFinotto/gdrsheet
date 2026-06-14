<script setup lang="ts">
import useDiceRoll from '../function/useDiceRoll'
import usePopup from '../function/usePopup'

const {forzaValore} = useDiceRoll()
const {closePopup} = usePopup()

const valori = Array.from({length: 20}, (_, i) => i + 1)

function scegli(n: number) {
  forzaValore(n)
  closePopup()
}
</script>

<template>
  <div class="force-roller">
    <h3 class="fr-title">Forza un valore</h3>
    <div class="fr-grid">
      <button
          v-for="n in valori" :key="n" type="button"
          class="fr-btn" @click="scegli(n)"
      >{{ n }}</button>
    </div>
  </div>
</template>

<style scoped>
.force-roller {
  display: flex;
  flex-direction: column;
  gap: .6rem;
}

.fr-title {
  margin: 0;
  font-size: 1.05rem;
}

.fr-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: .4rem;
}

.fr-btn {
  padding: .6rem .3rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  background: #fff;
  font-weight: 800;
  font-size: 1rem;
  font-variant-numeric: tabular-nums;
  cursor: pointer;
}

.fr-btn:hover { background: #eff6ff; border-color: #2563eb; color: #1d4ed8; }
.fr-btn:active { transform: scale(.95); }
</style>
