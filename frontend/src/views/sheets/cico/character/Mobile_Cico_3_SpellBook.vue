<script setup lang="ts">
import {defineProps, markRaw, ref, watch} from 'vue';
import Tabella from "../../../../components/Tabella.vue";
import Mobile_DettaglioItem from "../../../../components/Mobile_DettaglioItem.vue";
import {useCharacterStore} from "../../../../stores/personaggio";
import {storeToRefs} from "pinia";

const props = defineProps({
  idPersonaggio: {
    type: Number,
    required: true
  }
});

const characterStore = useCharacterStore()
const {cache} = storeToRefs(characterStore)

const items = ref<any[]>([]);
const itemsLivello0 = ref<any[]>([]);
const itemsLivello1 = ref<any[]>([]);
const itemsLivello2 = ref<any[]>([]);
const itemsLivello3 = ref<any[]>([]);
const itemsLivello4 = ref<any[]>([]);
const itemsLivello5 = ref<any[]>([]);
const itemsLivello6 = ref<any[]>([]);
const itemsLivello7 = ref<any[]>([]);
const itemsLivello8 = ref<any[]>([]);
const itemsLivello9 = ref<any[]>([]);
// watch(
//     () => props.datiPersonaggio?.character,
//     (newChar) => {
//       if (!newChar?.items) {
//         items.value = [];
//         return;
//       }
//
//       items.value = getAllItems(newChar)
//           .filter(itm => [TIPO_ITEM.INCANTESIMO].includes(itm.tipo))
//           .map(itm => {
//             return {
//               ...itm,
//               expandedComponent: markRaw(Mobile_DettaglioItem),
//               expandedProps: {data: {item: {...itm}, personaggio: props.datiPersonaggio}},
//             };
//           })
//           .sort((a, b) => a.nome.localeCompare(b.nome));
//
//       console.log(items);
//
//       itemsLivello0.value = items.value.filter(itm => itm.livello === "0");
//       itemsLivello1.value = items.value.filter(itm => itm.livello === "1");
//       itemsLivello2.value = items.value.filter(itm => itm.livello === "2");
//       itemsLivello3.value = items.value.filter(itm => itm.livello === "3");
//       itemsLivello4.value = items.value.filter(itm => itm.livello === "4");
//       itemsLivello5.value = items.value.filter(itm => itm.livello === "5");
//       itemsLivello6.value = items.value.filter(itm => itm.livello === "6");
//       itemsLivello7.value = items.value.filter(itm => itm.livello === "7");
//       itemsLivello8.value = items.value.filter(itm => itm.livello === "8");
//       itemsLivello9.value = items.value.filter(itm => itm.livello === "9");
//     },
//     {immediate: true, deep: true}
// );


watch(
    () => cache.value[props.idPersonaggio]?.items,
    (newChar) => {
      if (!newChar) {
        items.value = [];
        return;
      }

      items.value = newChar.incantesimi
          .map(itm => {
            return {
              ...itm,
              expandedComponent: markRaw(Mobile_DettaglioItem),
              expandedProps: {data: {item: {...itm}, personaggio: cache[props.idPersonaggio]}}
            };
          })
          .sort((a, b) => a.nome.localeCompare(b.nome));

      console.log(items.value);

      itemsLivello0.value = items.value.filter(itm => itm.livello === 0);
      itemsLivello1.value = items.value.filter(itm => itm.livello === 1);
      itemsLivello2.value = items.value.filter(itm => itm.livello === 2);
      itemsLivello3.value = items.value.filter(itm => itm.livello === 3);
      itemsLivello4.value = items.value.filter(itm => itm.livello === 4);
      itemsLivello5.value = items.value.filter(itm => itm.livello === 5);
      itemsLivello6.value = items.value.filter(itm => itm.livello === 6);
      itemsLivello7.value = items.value.filter(itm => itm.livello === 7);
      itemsLivello8.value = items.value.filter(itm => itm.livello === 8);
      itemsLivello9.value = items.value.filter(itm => itm.livello === 9);

      console.log(itemsLivello0.value, itemsLivello1.value, itemsLivello2.value, itemsLivello3.value);


    },
    {immediate: true, deep: true}
);

const columnsLivello0 = [
  {field: 'nome', label: 'Cantrip'},
];
const columnsLivello1 = [
  {field: 'nome', label: 'Livello 1'},
];
const columnsLivello2 = [
  {field: 'nome', label: 'Livello 2'},
];
const columnsLivello3 = [
  {field: 'nome', label: 'Livello 3'},
];
const columnsLivello4 = [
  {field: 'nome', label: 'Livello 4'},
];
const columnsLivello5 = [
  {field: 'nome', label: 'Livello 5'},
];
const columnsLivello6 = [
  {field: 'nome', label: 'Livello 6'},
];
const columnsLivello7 = [
  {field: 'nome', label: 'Livello 7'},
];
const columnsLivello8 = [
  {field: 'nome', label: 'Livello 8'},
];
const columnsLivello9 = [
  {field: 'nome', label: 'Livello 9'},
];
</script>

<template>
  <Tabella v-if="itemsLivello0.length > 0"
           :columns="columnsLivello0"
           :expandable="true"
           :items="itemsLivello0"
  >
  </Tabella>
  <div class="spazietto"/>
  <Tabella v-if="itemsLivello1.length > 0"
           :columns="columnsLivello1"
           :expandable="true"
           :items="itemsLivello1"
  >
  </Tabella>
  <div class="spazietto"/>
  <Tabella v-if="itemsLivello2.length > 0"
           :columns="columnsLivello2"
           :expandable="true"
           :items="itemsLivello2"
  >
  </Tabella>
  <div class="spazietto"/>
  <Tabella v-if="itemsLivello3.length > 0"
           :columns="columnsLivello3"
           :expandable="true"
           :items="itemsLivello3"
  >
  </Tabella>
  <div class="spazietto"/>
  <Tabella v-if="itemsLivello4.length > 0"
           :columns="columnsLivello4"
           :expandable="true"
           :items="itemsLivello4"
  >
  </Tabella>
  <div class="spazietto"/>
  <Tabella v-if="itemsLivello5.length > 0"
           :columns="columnsLivello5"
           :expandable="true"
           :items="itemsLivello5"
  >
  </Tabella>
  <div class="spazietto"/>
  <Tabella v-if="itemsLivello6.length > 0"
           :columns="columnsLivello6"
           :expandable="true"
           :items="itemsLivello6"
  >
  </Tabella>
  <div class="spazietto"/>
  <Tabella v-if="itemsLivello7.length > 0"
           :columns="columnsLivello7"
           :expandable="true"
           :items="itemsLivello7"
  >
  </Tabella>
  <div class="spazietto"/>
  <Tabella v-if="itemsLivello8.length > 0"
           :columns="columnsLivello8"
           :expandable="true"
           :items="itemsLivello8"
  >
  </Tabella>
  <div class="spazietto"/>
  <Tabella v-if="itemsLivello9.length > 0"
           :columns="columnsLivello9"
           :expandable="true"
           :items="itemsLivello9"
  >
  </Tabella>
  <div class="spazietto"/>
</template>

<style scoped>
.spazietto {
  height: 20px;
}
</style>