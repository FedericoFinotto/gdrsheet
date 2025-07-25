<template>
  <div class="card">
    <TabView value="0">
      <TabPanel value="0" header="Info">
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
      </TabPanel>
      <TabPanel value="1" header="Abilita'">
        <TestTabella v-if="sharedData.character"/>
      </TabPanel>
      <TabPanel value="2" header="Header III"/>
      <TabPanel value="2" header="Header IV"/>
      <TabPanel value="2" header="Header V"/>
      <TabPanel value="2" header="Header VI"/>
      <TabPanel value="2" header="Header VII"/>
      <TabPanel value="2" header="Header IIX"/>


    </TabView>
  </div>




</template>

<script setup>
import { inject, onMounted } from 'vue';
import Mobile_Stat from "./Mobile_Stat.vue";
import Mobile_HP from "./Mobile_HP.vue";
import Tabella from "./Tabella.vue"; // Assicurati che Tabella.vue sia il tuo componente corretto
import TestTabella from "./TestTabella.vue"; // Questo è il tuo componente padre per Tabella.vue

// Rimuovi PorcoDio se non è un componente valido o lo stai solo testando
// import PorcoDio from "./PorcoDio.vue";

onMounted(() => {
  // L'onMounted è utile per il debug, ma in App.vue potrebbe non essere strettamente necessario se non logghi nulla di specifico qui.
});

const sharedData = inject('sharedData');
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