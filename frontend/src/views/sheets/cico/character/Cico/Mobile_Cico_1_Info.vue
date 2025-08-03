<template>
  <h2 style="margin-bottom: 0; text-align: center">{{ cache[idPersonaggio].modificatori?.nome ?? "" }}</h2>
  <!--  <Mobile_HP v-if="cache[idPersonaggio].character"/>-->
  <div v-if="cache[idPersonaggio].modificatori" class="stat-block">
    <Mobile_Stat id="FOR" :id-personaggio="idPersonaggio"></Mobile_Stat>
    <Mobile_Stat id="DES" :id-personaggio="idPersonaggio"></Mobile_Stat>
    <Mobile_Stat id="COS" :id-personaggio="idPersonaggio"></Mobile_Stat>
    <Mobile_Stat id="INT" :id-personaggio="idPersonaggio"></Mobile_Stat>
    <Mobile_Stat id="SAG" :id-personaggio="idPersonaggio"></Mobile_Stat>
    <Mobile_Stat id="CAR" :id-personaggio="idPersonaggio"></Mobile_Stat>
  </div>
  <div v-if="cache[idPersonaggio].modificatori" class="stat-block">
    <Mobile_Stat id="TMP" :id-personaggio="idPersonaggio" label="Tempra"></Mobile_Stat>
    <Mobile_Stat id="RFL" :id-personaggio="idPersonaggio" label="Riflessi"></Mobile_Stat>
    <Mobile_Stat id="VLT" :id-personaggio="idPersonaggio" label="Volonta"></Mobile_Stat>
  </div>
  <div v-if="cache[idPersonaggio].modificatori" class="stat-block">
    <Mobile_Stat id="CA" :id-personaggio="idPersonaggio" label="CA"></Mobile_Stat>
    <Mobile_Stat id="CAC" :id-personaggio="idPersonaggio" label="Contatto"></Mobile_Stat>
    <Mobile_Stat id="CAS" :id-personaggio="idPersonaggio" label="Sorpreso"></Mobile_Stat>
  </div>
</template>

<script setup lang="ts">
import {defineProps} from 'vue';
import {storeToRefs} from "pinia";
import {useCharacterStore} from "../../../../../stores/personaggio";
import Mobile_Stat from "../Shared/Mobile_Stat.vue";

const characterStore = useCharacterStore()
const {cache} = storeToRefs(characterStore);

const props = defineProps({
  idPersonaggio: {
    type: Number,
    required: true
  }
});

</script>

<style>
.stat-block {
  width: 100%;
  padding: 8px;
  display: flex;
  flex-wrap: wrap; /* ✅ Permette di andare a capo */
  gap: 8px; /* (opzionale) spazio tra le stat */
  justify-content: center;
  background: transparent;
  box-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
}

.p-tabview-panels {
  padding: 0 !important; /* Usa !important per sovrascrivere gli stili di PrimeVue */
}

.p-tabview .p-tabview-panel {
  padding: 0 !important;
}

.spazietto {
  height: 20px;
}

</style>