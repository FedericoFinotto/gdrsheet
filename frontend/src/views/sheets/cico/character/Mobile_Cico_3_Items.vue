<script setup lang="ts">
import {defineProps, markRaw, ref, watch} from 'vue';
import Tabella from "../../../../components/Tabella.vue";
import {getAllItems} from "../../../../function/Utils";
import {TIPO_ITEM} from "../../../../function/Constants";
import Mobile_DettaglioItem from "../../../../components/Mobile_DettaglioItem.vue";

const props = defineProps({
  datiPersonaggio: {
    type: Object,
    required: true
  }
});
const items = ref<any[]>([]);
const itemsOggetti = ref<any[]>([]);
const itemsArmi = ref<any[]>([]);
const itemsEquipaggiamento = ref<any[]>([]);
const itemsConsumabili = ref<any[]>([]);
const itemsMunizioni = ref<any[]>([]);
watch(
    () => props.datiPersonaggio?.character,
    (newChar) => {
      if (!newChar?.items) {
        itemsOggetti.value = [];
        return;
      }

      items.value = getAllItems(newChar)
          .filter(itm => [TIPO_ITEM.OGGETTO, TIPO_ITEM.ARMA, TIPO_ITEM.EQUIPAGGIAMENTO, TIPO_ITEM.CONSUMABILE, TIPO_ITEM.MUNIZIONE].includes(itm.tipo))
          .map(itm => {
            return {
              ...itm,
              expandedComponent: markRaw(Mobile_DettaglioItem),
              expandedProps: {data: {item: {...itm}, personaggio: props.datiPersonaggio}},
            };
          })
          .sort((a, b) => a.nome.localeCompare(b.nome));

      itemsOggetti.value = items.value.filter(itm => [TIPO_ITEM.OGGETTO].includes(itm.tipo));
      itemsArmi.value = items.value.filter(itm => [TIPO_ITEM.ARMA].includes(itm.tipo));
      itemsEquipaggiamento.value = items.value.filter(itm => [TIPO_ITEM.EQUIPAGGIAMENTO].includes(itm.tipo));
      itemsConsumabili.value = items.value.filter(itm => [TIPO_ITEM.CONSUMABILE].includes(itm.tipo));
      itemsMunizioni.value = items.value.filter(itm => [TIPO_ITEM.MUNIZIONE].includes(itm.tipo));
    },
    {immediate: true, deep: true}
);

const columnsOggetti = [
  {field: 'nome', label: 'Oggetti'},
];
const columnsArmi = [
  {field: 'nome', label: 'Armi'},
];
const columnsEquipaggiamento = [
  {field: 'nome', label: 'Equipaggiamento'},
];
const columnsConsumabili = [
  {field: 'nome', label: 'Consumabili'},
];
const columnsMunizioni = [
  {field: 'nome', label: 'Munizioni'},
];
</script>

<template>
  <Tabella v-if="itemsOggetti.length > 0"
           :columns="columnsOggetti"
           :expandable="true"
           :items="itemsOggetti"
  >
  </Tabella>
  <div class="spazietto"/>
  <Tabella v-if="itemsArmi.length > 0"
           :columns="columnsArmi"
           :expandable="true"
           :items="itemsArmi"
  >
  </Tabella>
  <div class="spazietto"/>
  <Tabella v-if="itemsEquipaggiamento.length > 0"
           :columns="columnsEquipaggiamento"
           :expandable="true"
           :items="itemsEquipaggiamento"
  >
  </Tabella>
  <div class="spazietto"/>
  <Tabella v-if="itemsConsumabili.length > 0"
           :columns="columnsConsumabili"
           :expandable="true"
           :items="itemsConsumabili"
  >
  </Tabella>
  <div class="spazietto"/>
  <Tabella v-if="itemsMunizioni.length > 0"
           :columns="columnsMunizioni"
           :expandable="true"
           :items="itemsMunizioni"
  >
  </Tabella>
</template>

<style scoped>
.spazietto {
  height: 20px;
}
</style>