<template>
  <h2 style="margin-bottom: 0; text-align: center">{{ datiPersonaggio.character?.nome ?? "" }}</h2>
  <Mobile_HP v-if="datiPersonaggio.character"/>
  <div v-if="datiPersonaggio.character" class="stat-block">
    <Mobile_Stat id="FOR" :dati-personaggio="datiPersonaggio"></Mobile_Stat>
    <Mobile_Stat id="DES" :dati-personaggio="datiPersonaggio"></Mobile_Stat>
    <Mobile_Stat id="COS" :dati-personaggio="datiPersonaggio"></Mobile_Stat>
    <Mobile_Stat id="INT" :dati-personaggio="datiPersonaggio"></Mobile_Stat>
    <Mobile_Stat id="SAG" :dati-personaggio="datiPersonaggio"></Mobile_Stat>
    <Mobile_Stat id="CAR" :dati-personaggio="datiPersonaggio"></Mobile_Stat>
  </div>
  <div v-if="datiPersonaggio.character" class="stat-block">
    <Mobile_Stat id="TMP" :dati-personaggio="datiPersonaggio" label="Tempra"></Mobile_Stat>
    <Mobile_Stat id="RFL" :dati-personaggio="datiPersonaggio" label="Riflessi"></Mobile_Stat>
    <Mobile_Stat id="VLT" :dati-personaggio="datiPersonaggio" label="Volonta"></Mobile_Stat>
  </div>
  <!--  <div v-if="datiPersonaggio.character" class="stat-block">-->
  <!--    <Mobile_Stat id="CA" :dati-personaggio="datiPersonaggio" label="CA"></Mobile_Stat>-->
  <!--    <Mobile_Stat id="CAC" :dati-personaggio="datiPersonaggio" label="Contatto"></Mobile_Stat>-->
  <!--    <Mobile_Stat id="CAS" :dati-personaggio="datiPersonaggio" label="Sorpreso"></Mobile_Stat>-->
  <!--  </div>-->
</template>

<script setup>
import {defineProps} from 'vue';
import Mobile_Stat from "../../../../components/Mobile_Stat.vue";
import Mobile_HP from "../../../../components/Mobile_HP.vue";


const props = defineProps({
  datiPersonaggio: {
    type: Object,
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

</style>