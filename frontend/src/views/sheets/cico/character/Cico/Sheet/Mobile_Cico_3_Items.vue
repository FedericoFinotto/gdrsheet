<script setup lang="ts">
import {defineProps, markRaw, ref, watch} from 'vue';
import Tabella from "../../../../../../components/Tabella.vue";
import {useCharacterStore} from "../../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import Mobile_DettaglioItem from "../../Dettaglio/Mobile_DettaglioItem.vue";
import Mobile_Borsellino from "../../Shared/Mobile_Borsellino.vue";
import BottoneAggiungiItem from "../../Shared/BottoneAggiungiItem.vue";

const characterStore = useCharacterStore()
const {cache} = storeToRefs(characterStore)

const props = defineProps({
  idPersonaggio: {
    type: Number,
    required: true
  }
});
const itemsArmi = ref<any[]>([]);
const itemsOggetti = ref<any[]>([]);
const itemsEquipaggiamento = ref<any[]>([]);
const itemsConsumabili = ref<any[]>([]);
const itemsMunizioni = ref<any[]>([]);

watch(
    () => cache.value[props.idPersonaggio]?.items,
    (newChar) => {
      if (!newChar) {
        itemsOggetti.value = [];
        itemsEquipaggiamento.value = [];
        itemsConsumabili.value = [];
        itemsMunizioni.value = [];
        return;
      }

      itemsArmi.value = newChar.armi
          .map(itm => {
            return {
              ...itm,
              expandedComponent: markRaw(Mobile_DettaglioItem),
              expandedProps: {data: {item: {...itm}, personaggio: cache.value[props.idPersonaggio]}}
            };
          })
          .sort((a, b) => a.nome.localeCompare(b.nome));

      itemsOggetti.value = newChar.oggetti
          .map(itm => {
            return {
              ...itm,
              expandedComponent: markRaw(Mobile_DettaglioItem),
              expandedProps: {data: {item: {...itm}, personaggio: cache.value[props.idPersonaggio]}}
            };
          })
          .sort((a, b) => a.nome.localeCompare(b.nome));

      itemsEquipaggiamento.value = newChar.equipaggiamento
          .map(itm => {
            return {
              ...itm,
              expandedComponent: markRaw(Mobile_DettaglioItem),
              expandedProps: {data: {item: {...itm}, personaggio: cache.value[props.idPersonaggio]}}
            };
          })
          .sort((a, b) => a.nome.localeCompare(b.nome));

      itemsConsumabili.value = newChar.consumabili
          .map(itm => {
            return {
              ...itm,
              expandedComponent: markRaw(Mobile_DettaglioItem),
              expandedProps: {data: {item: {...itm}, personaggio: cache.value[props.idPersonaggio]}}
            };
          })
          .sort((a, b) => a.nome.localeCompare(b.nome));

      itemsMunizioni.value = newChar.munizioni
          .map(itm => {
            return {
              ...itm,
              expandedComponent: markRaw(Mobile_DettaglioItem),
              expandedProps: {data: {item: {...itm}, personaggio: cache.value[props.idPersonaggio]}}
            };
          })
          .sort((a, b) => a.nome.localeCompare(b.nome));
    },
    {immediate: true, deep: true}
);

const badgeQta = (row) => row.quantita != null && row.quantita !== 1 ? `x${row.quantita}` : null;

const columnsOggetti = [
  {field: 'nome', label: 'Oggetti', disabled: (row) => row.disabled, badge: badgeQta},
];
const columnsArmi = [
  {field: 'nome', label: 'Armi', disabled: (row) => row.disabled, badge: badgeQta},
];
const columnsEquipaggiamento = [
  {field: 'nome', label: 'Equipaggiamento', disabled: (row) => row.disabled, badge: badgeQta},
];
const columnsConsumabili = [
  {field: 'nome', label: 'Consumabili', disabled: (row) => row.disabled, badge: badgeQta},
];
const columnsMunizioni = [
  {field: 'nome', label: 'Munizioni', disabled: (row) => row.disabled, badge: badgeQta},
];
</script>

<template>
  <div>
    <Mobile_Borsellino :id-personaggio="props.idPersonaggio"/>
    <div class="spazietto"/>
    <BottoneAggiungiItem :id-personaggio="props.idPersonaggio" label="Aggiungi oggetto"/>
    <div class="spazietto"/>
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
  </div>
</template>