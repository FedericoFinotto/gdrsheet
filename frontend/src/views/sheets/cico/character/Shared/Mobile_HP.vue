<template>
  <div class="hp-container">
    <div class="hp-label">Punti Ferita</div>
    <div class="hp-bar-wrapper">
      <div class="hp-bar" :style="{ width: hpPercent + '%' }"></div>
    </div>
    <div class="hp-values">
      <span>{{ hp }}/{{ hpMax }}</span>
      <button @click="modificaHp(-1)">-</button>
      <button @click="modificaHp(1)">+</button>
    </div>
  </div>
</template>

<script setup>
import {computed, inject} from 'vue'
import {useCharacterStore} from "../../../../../stores/personaggio.js";
import {storeToRefs} from "pinia";

const sharedData = inject('sharedData')
const characterStore = useCharacterStore()
const {cache} = storeToRefs(characterStore)

const hp = computed(() =>
    sharedData.character.stats?.find(stat => stat.stat.id === 'PF')?.valore ?? 0
);

const hpMax = computed(() =>
    sharedData.character.stats?.find(stat => stat.stat.id === 'PFMAX')?.valore ?? 0
);


const hpPercent = computed(() => (hp.value / hpMax.value) * 100)

function modificaHp(delta) {
  const nuovoHp = Math.max(0, Math.min(hpMax.value, hp.value + delta))
  sharedData.character.stats.find(stat => stat.stat.id === 'PF').valore = nuovoHp;
  // chiamata asincrona non bloccante
  // updateHp(sharedData.id, nuovoHp).catch(err => {
  //   console.error('Errore aggiornamento HP', err)
  // })
}
</script>

<style scoped>
.hp-container {
  padding: 1rem;
}

.hp-label {
  font-weight: bold;
  margin-bottom: 0.5rem;
}

.hp-bar-wrapper {
  width: 100%;
  background-color: #ddd;
  height: 20px;
  border-radius: 10px;
  overflow: hidden;
}

.hp-bar {
  background-color: #d9534f;
  height: 100%;
  transition: width 0.3s;
}

.hp-values {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.5rem;
}

button {
  padding: 0.3rem 0.6rem;
  font-size: 1rem;
}
</style>
