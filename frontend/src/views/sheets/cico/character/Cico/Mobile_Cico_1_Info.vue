<template>
  <div class="nome">
    <h2>{{
        cache[idPersonaggio].modificatori?.nome ?? ""
      }}</h2>
  </div>
  <Mobile_HP v-if="cache[idPersonaggio].modificatori" :id-personaggio="idPersonaggio"/>
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
  <div v-if="cache[idPersonaggio].modificatori" class="stat-block">
    <Mobile_Stat id="BAB" :id-personaggio="idPersonaggio" label="BAB"></Mobile_Stat>
    <Mobile_Stat id="LTT" :id-personaggio="idPersonaggio" label="Lotta"></Mobile_Stat>
    <Mobile_Stat id="MSC" :id-personaggio="idPersonaggio" label="Mischia"></Mobile_Stat>
    <Mobile_Stat id="GTT" :id-personaggio="idPersonaggio" label="Distanza"></Mobile_Stat>
  </div>
  <div class="stat-block">
    <template
        v-for="stat in cache[idPersonaggio].modificatori.contatori.filter(x => x.id !== 'PF' && x.id != 'PFTEMP')">
      <Mobile_Contatore :id-stat="stat.id" :id-personaggio="idPersonaggio"></Mobile_Contatore>
    </template>
  </div>
</template>

<script setup lang="ts">
import {defineProps} from 'vue';
import {storeToRefs} from "pinia";
import {useCharacterStore} from "../../../../../stores/personaggio";
import Mobile_Stat from "../Shared/Mobile_Stat.vue";
import Mobile_HP from "../Shared/Mobile_HP.vue";
import Mobile_Contatore from "../Shared/Mobile_Contatore.vue";

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
  display: flex;
  flex-wrap: wrap;
  padding: 0 !important;
  gap: 3px !important;
  margin-top: 3px;

  /* già presente: permette il wrap */
  /*gap: 8px;  spazio tra le caselle */
  /* elimina justify-content:center se vuoi che partano da sinistra
     o usa space-between per distribuire gli spazi in automatico */
  justify-content: flex-start;
}

.stat-block > * {
  flex: 1 1 0; /* grow = 1, shrink = 1, base = 0 */
  /* opzionale: imposti una min-width per evitare che diventino troppo piccole */
  min-width: 45px;
}

.stat-block > .stat-box:nth-child(odd) {
  background-color: var(--secondary-color);
}

.stat-block > .stat-box:nth-child(even) {
  background-color: var(--tertiary-color);
}

.nome {
  border: 1px solid var(--border-color);
  background-color: var(--primary-color);
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 40px;
  color: black;
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