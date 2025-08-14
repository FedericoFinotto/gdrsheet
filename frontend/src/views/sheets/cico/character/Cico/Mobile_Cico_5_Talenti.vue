<script setup lang="ts">
import {defineProps, markRaw, ref, watch} from 'vue';
import {storeToRefs} from "pinia";
import {useCharacterStore} from "../../../../../stores/personaggio";
import Mobile_DettaglioItem from "../Dettaglio/Mobile_DettaglioItem.vue";
import Tabella from "../../../../../components/Tabella.vue";

const characterStore = useCharacterStore()
const {cache} = storeToRefs(characterStore);


const props = defineProps({
  idPersonaggio: {
    type: Number,
    required: true
  }
});

const itemsAbilitaPassive = ref<any[]>([]);
watch(
    () => cache.value[props.idPersonaggio]?.items,
    (newChar) => {
      if (!newChar?.abilita) {
        itemsAbilitaPassive.value = [];
        return;
      }

      itemsAbilitaPassive.value = newChar.abilita
          .map(itm => {
            return {
              ...itm,
              expandedComponent: markRaw(Mobile_DettaglioItem),
              expandedProps: {data: {item: {...itm}, personaggio: cache[props.idPersonaggio]}}
            };
          })
          .sort((a, b) => a.nome.localeCompare(b.nome));

    },
    {immediate: true, deep: true}
);

const itemsTalenti = ref<any[]>([]);
watch(
    () => cache.value[props.idPersonaggio]?.items,
    (newChar) => {
      if (!newChar?.talenti) {
        itemsTalenti.value = [];
        return;
      }

      itemsTalenti.value = newChar.talenti
          .map(itm => {
            return {
              ...itm,
              expandedComponent: markRaw(Mobile_DettaglioItem),
              expandedProps: {data: {item: {...itm}, personaggio: cache[props.idPersonaggio]}}
            };
          })
          .sort((a, b) => a.nome.localeCompare(b.nome));

    },
    {immediate: true, deep: true}
);

const itemsMaledizioni = ref<any[]>([]);
watch(
    () => cache.value[props.idPersonaggio]?.items,
    (newChar) => {
      if (!newChar?.maledizioni) {
        itemsMaledizioni.value = [];
        return;
      }

      itemsMaledizioni.value = newChar.maledizioni
          .map(itm => {
            return {
              ...itm,
              expandedComponent: markRaw(Mobile_DettaglioItem),
              expandedProps: {data: {item: {...itm}, personaggio: cache[props.idPersonaggio]}}
            };
          })
          .sort((a, b) => a.nome.localeCompare(b.nome));

    },
    {immediate: true, deep: true}
);

const columnsAbilitaPassive = [
  {field: 'nome', label: 'Abilita Passive', disabled: (row) => row.disabled},
];

const columnsTalenti = [
  {field: 'nome', label: 'Talenti', disabled: (row) => row.disabled},
];

const columnsMaledizioni = [
  {field: 'nome', label: 'Maledizioni', disabled: (row) => row.disabled},
];

</script>

<template>
  <div>
    <Tabella v-if="itemsAbilitaPassive.length > 0"
             :columns="columnsAbilitaPassive"
             :expandable="true"
             :items="itemsAbilitaPassive"
    >
    </Tabella>
    <div class="spazietto"/>
    <Tabella v-if="itemsTalenti.length > 0"
             :columns="columnsTalenti"
             :expandable="true"
             :items="itemsTalenti"
    >
    </Tabella>
    <div class="spazietto"/>
    <Tabella v-if="itemsMaledizioni.length > 0"
             :columns="columnsMaledizioni"
             :expandable="true"
             :items="itemsMaledizioni"
    >
    </Tabella>
  </div>

</template>