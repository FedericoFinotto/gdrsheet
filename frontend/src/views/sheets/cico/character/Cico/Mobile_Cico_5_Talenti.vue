<template>
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

</template>

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

const columnsAbilitaPassive = [
  {field: 'nome', label: 'Abilita Passive', disabled: (row) => row.disabled},
];

const columnsTalenti = [
  {field: 'nome', label: 'Talenti', disabled: (row) => row.disabled},
];




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