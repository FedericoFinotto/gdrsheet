<template>
  <div class="stat-box">
    <div class="label">{{ label }}</div>
    <div class="modifier">{{ modifier >= 0 ? '+' + modifier : modifier }}</div>
    <div class="base">{{ value }}</div>
  </div>
</template>

<script setup>
import {computed, inject} from 'vue'

const props = defineProps({
  label: String,
  id: String
})

const sharedData = inject('sharedData')

// Trova la statistica corrispondente
const stat = computed(() => sharedData.character.stats.find(s => s.label === props.id));

const mods = computed(() => {
});

// Valore base della stat (es. 14)
const value = computed(() => {
  console.log('aaa', stat.value.valore);
  return stat.value.valore || 10;
})

// Calcolo del modificatore (D&D style)
const modifier = computed(() => Math.floor((value.value - 10) / 2))
</script>

<style scoped>
.stat-box {
  width: 80px;
  height: 100px;
  border: 2px solid #888;
  border-radius: 8px;
  text-align: center;
  padding: 8px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background: #f3f3f3;
  box-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
}

.label {
  font-size: 0.8rem;
  font-weight: bold;
  text-transform: uppercase;
}

.modifier {
  font-size: 1.4rem;
  font-weight: bold;
  color: #444;
}

.base {
  font-size: 1rem;
  color: #666;
}

</style>
