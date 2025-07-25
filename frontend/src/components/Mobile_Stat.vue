<template>
  <div class="stat-box">
    <div class="label">{{ label ?? id }}</div>
    <div class="modifier">{{ stat.modificatore ?? '' }}</div>
    <div class="base">{{ stat.valore }}</div>
  </div>
</template>

<script setup>
import {computed, defineProps} from 'vue'
import {getDatiCaratteristica} from "../function/Utils";

const props = defineProps({
  datiPersonaggio: {
    type: Object,
    required: true
  },
  id: {
    type: String,
    required: true
  },
  label: {
    type: String,
    default: undefined
  }
});

const stat = computed(() =>
    getDatiCaratteristica(props.datiPersonaggio, props.id)
);
</script>

<style scoped>
.stat-box {
  display: inline-flex;
  flex-direction: column;
  justify-content: space-between;

  /* larghezza automatica in base al contenuto */
  width: auto;
  min-width: 45px;

  padding: 4px;
  border: 2px solid #888;
  border-radius: 8px;
  background: #f3f3f3;
  box-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);

  text-align: center;
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
