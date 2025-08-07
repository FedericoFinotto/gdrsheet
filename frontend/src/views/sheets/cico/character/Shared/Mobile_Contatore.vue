<template>
  <div class="stat-box">
    <div class="stat-nome">{{ stat?.nome ?? props.idStat }}</div>

    <div class="stat-controls">
      <button @click="modifica(-1)">-</button>
      <div class="stat-bar">
        <span>{{ valore }} / {{ max }}</span>
      </div>
      <button @click="modifica(1)">+</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, defineProps} from 'vue'
import {useCharacterStore} from '../../../../../stores/personaggio'
import {storeToRefs} from 'pinia'
import {updateContatore} from "../../../../../service/PersonaggioService";

const characterStore = useCharacterStore()
const {cache} = storeToRefs(characterStore)

const props = defineProps<{
  idPersonaggio: number
  idStat: string
}>()

const stat = computed(() =>
    cache.value[props.idPersonaggio].modificatori.contatori.find(s => s.id === props.idStat)
)

const valore = computed(() => stat.value?.valore ?? 0)
const max = computed(() => stat.value?.max ?? 0)

function modifica(delta: number) {
  if (!stat.value) return
  const nuovoValore = Math.max(0, Math.min(stat.value.max, stat.value.valore + delta))
  stat.value.valore = nuovoValore
  updateContatore(props.idPersonaggio, props.idStat, nuovoValore).then((resp) => {
    if (resp) console.log(resp);
  });
}
</script>

<style scoped>
.stat-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100px;
  border: 1px solid var(--border-color, #ccc);
  padding: 0.3rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.stat-nome {
  font-weight: bold;
  font-size: 0.8rem;
  text-align: center;
  margin-bottom: 0.3rem;
  color: #333;
}

.stat-controls {
  display: flex;
  align-items: center;
  width: 100%;
  height: 2rem;
}

button {
  width: 2rem;
  height: 2rem;
  font-size: 1.2rem;
  border: none;
  background: transparent;
  cursor: pointer;
}

.stat-bar {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: bold;
  font-size: 0.9rem;
  height: 100%;
  border-radius: 4px;
}
</style>
