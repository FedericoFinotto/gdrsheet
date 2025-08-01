<script setup>
import {onMounted, ref} from 'vue';
import Mobile_Info from "./Mobile_Cico_1_Info.vue";
import Mobile_Abilita from "./Mobile_Cico_2_Abilita.vue";
import {storeToRefs} from 'pinia'
import {useCharacterStore} from "../../../../stores/personaggio";

const activeIndex = ref(0);
const characterStore = useCharacterStore()
const {cache} = storeToRefs(characterStore);

const loaded = ref(false);

onMounted(() => {
  console.log("ON MOUNTED CHARACTER SHEET");
  characterStore.fetchCharacter(1).then(() => {
    loaded.value = true;
  })
})
</script>

<template>
  <div class="card" v-if="loaded">
    <TabView v-model:activeIndex="activeIndex">
      <TabPanel header="Info">
        <Mobile_Info
            :id-personaggio="1"/>
      </TabPanel>
      <TabPanel header="Abilita'">
        <Mobile_Abilita
            :id-personaggio="1"/>
      </TabPanel>
      <!--      <TabPanel header="Inventario">-->
      <!--        <Mobile_Cico_3_Items-->
      <!--            v-if="sharedData.character"-->
      <!--            :dati-personaggio="sharedData"/>-->
      <!--      </TabPanel>-->
      <!--      <TabPanel header="Incantesimi">-->
      <!--        <Mobile_Cico_3_SpellBook-->
      <!--            v-if="sharedData.character"-->
      <!--            :dati-personaggio="sharedData"/>-->
      <!--      </TabPanel>-->
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


<!--<script setup lang="ts">-->
<!--import {useRoute} from 'vue-router'-->
<!--import {useCharacterStore} from '@/stores/characterStore'-->
<!--import {storeToRefs} from 'pinia'-->

<!--const route = useRoute()-->
<!--const id = Number(route.params.id)-->
<!--const characterStore = useCharacterStore()-->
<!--const {cache} = storeToRefs(characterStore)-->

<!--// on mount or watch id-->
<!---->

<!--// in template: cache[id].character, cache[id].modificatori, cache[id].items-->
<!--</script>-->

