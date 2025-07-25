<template>
  <div class="card">
    <TabView value="0">
      <TabPanel header="Info" value="0">
        <Mobile_Info :dati-personaggio="sharedData"/>
      </TabPanel>
      <TabPanel header="Abilita'" value="1">
        <Mobile_Abilita v-if="sharedData.character" :dati-personaggio="sharedData"/>
      </TabPanel>
      <TabPanel header="Header III" value="2"/>
      <TabPanel header="Header IV" value="2"/>
      <TabPanel header="Header V" value="2"/>
      <TabPanel header="Header VI" value="2"/>
      <TabPanel header="Header VII" value="2"/>
      <TabPanel header="Header IIX" value="2"/>
    </TabView>
  </div>
</template>

<script setup>
import {inject, onMounted} from 'vue';
import Mobile_Abilita from "./Mobile_Cico_2_Abilita.vue";
import Mobile_Info from "./Mobile_Cico_1_Info.vue";


onMounted(() => {
  // L'onMounted è utile per il debug, ma in App.vue potrebbe non essere strettamente necessario se non logghi nulla di specifico qui.
});

const sharedData = inject('sharedData');
</script>

<style>
.p-tabview-panels {
  padding: 0 !important; /* Usa !important per sovrascrivere gli stili di PrimeVue */
}

.p-tabview .p-tabview-panel {
  padding: 0 !important;
}

</style>