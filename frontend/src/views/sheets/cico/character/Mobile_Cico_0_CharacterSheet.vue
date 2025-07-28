<script setup>
import {inject, ref} from 'vue';
import Mobile_Info from "./Mobile_Cico_1_Info.vue";
import Mobile_Abilita from "./Mobile_Cico_2_Abilita.vue";
import Mobile_Cico_3_Items from "./Mobile_Cico_3_Items.vue";
import Mobile_Cico_3_SpellBook from "./Mobile_Cico_3_SpellBook.vue";

const sharedData = inject('sharedData');
const activeIndex = ref(0); // pannello iniziale
</script>

<template>
  <div class="card">
    <TabView v-model:activeIndex="activeIndex">
      <TabPanel header="Info">
        <Mobile_Info
            :dati-personaggio="sharedData"/>
      </TabPanel>
      <TabPanel header="Abilita'">
        <Mobile_Abilita
            v-if="sharedData.character"
            :dati-personaggio="sharedData"/>
      </TabPanel>
      <TabPanel header="Inventario">
        <Mobile_Cico_3_Items
            v-if="sharedData.character"
            :dati-personaggio="sharedData"/>
      </TabPanel>
      <TabPanel header="Incantesimi">
        <Mobile_Cico_3_SpellBook
            v-if="sharedData.character"
            :dati-personaggio="sharedData"/>
      </TabPanel>
    </TabView>
  </div>
</template>

<style>
.p-tabview-panels {
  padding: 0 !important;
}
.p-tabview .p-tabview-panel {
  padding: 0 !important;
}
</style>
