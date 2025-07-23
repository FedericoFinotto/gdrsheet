<template>
  <h2 style="margin-bottom: 0; text-align: center">{{ sharedData.character?.nome ?? "" }}</h2>
  <Mobile_HP v-if="sharedData.character"/>
  <div v-if="sharedData.character" class="stat-block">
    <Mobile_Stat id="FOR"></Mobile_Stat>
    <Mobile_Stat id="DES"></Mobile_Stat>
    <Mobile_Stat id="COS"></Mobile_Stat>
    <Mobile_Stat id="INT"></Mobile_Stat>
    <Mobile_Stat id="SAG"></Mobile_Stat>
    <Mobile_Stat id="CAR"></Mobile_Stat>
  </div>
  <div v-if="sharedData">
    <p>{{ sharedData.character }}</p>
  </div>
  <div v-else>Loading…</div>

</template>

<script setup>

import {inject, onMounted} from 'vue'
import Mobile_Stat from "./Mobile_Stat.vue";
import Mobile_HP from "./Mobile_HP.vue";

onMounted(() => {
})

const sharedData = inject('sharedData')
</script>

<style>
.stat-block {
  width: 100%;
  padding: 8px;
  display: flex;
  flex-wrap: wrap; /* ✅ Permette di andare a capo */
  gap: 8px; /* (opzionale) spazio tra le stat */
  justify-content: center;
  background: #f3f3f3;
  box-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
}
</style>
