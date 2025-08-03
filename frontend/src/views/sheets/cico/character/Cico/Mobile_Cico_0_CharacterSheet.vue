<script setup lang="ts">
import {onMounted, ref} from 'vue';
import {useRoute} from 'vue-router';

import {storeToRefs} from 'pinia';
import {useCharacterStore} from "../../../../../stores/personaggio";
import Mobile_Cico_Info from './Mobile_Cico_1_Info.vue';
import Mobile_Cico_Abilita from './Mobile_Cico_2_Abilita.vue';
import Mobile_Cico_Items from './Mobile_Cico_3_Items.vue';
import Mobile_Cico_SpellBook from './Mobile_Cico_4_SpellBook.vue';
import Mobile_Cico_Talenti from "./Mobile_Cico_5_Talenti.vue";
import Mobile_Cico_Attacchi from "./Mobile_Cico_6_Attacchi.vue";

const route = useRoute();
// Recupera e converte in numero il parametro `id` della rotta
const idPersonaggio = Number(route.params.id);
if (isNaN(idPersonaggio)) {
  throw new Error('Parametro id non valido');
}

const activeIndex = ref(0);
const loaded = ref(false);

const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);

onMounted(() => {
  console.log('ON MOUNTED CHARACTER SHEET, id:', idPersonaggio);
  characterStore.fetchCharacter(idPersonaggio).then(() => {
    loaded.value = true;
  });
});
</script>

<template>
  <div class="card" v-if="loaded">
    <TabView v-model:activeIndex="activeIndex">
      <TabPanel header="Info">
        <Mobile_Cico_Info :id-personaggio="idPersonaggio"/>
      </TabPanel>
      <TabPanel header="Abilita'">
        <Mobile_Cico_Abilita :id-personaggio="idPersonaggio"/>
      </TabPanel>
      <TabPanel header="Inventario">
        <Mobile_Cico_Items :id-personaggio="idPersonaggio"/>
      </TabPanel>
      <TabPanel header="Incantesimi">
        <Mobile_Cico_SpellBook :id-personaggio="idPersonaggio"/>
      </TabPanel>
      <TabPanel header="Talenti">
        <Mobile_Cico_Talenti :id-personaggio="idPersonaggio"/>
      </TabPanel>
      <TabPanel header="Attacchi">
        <Mobile_Cico_Attacchi :id-personaggio="idPersonaggio"/>
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
