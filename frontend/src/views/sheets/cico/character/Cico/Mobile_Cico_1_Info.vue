<script setup lang="ts">
import {defineProps, markRaw, ref, watch} from 'vue';
import {storeToRefs} from "pinia";
import {useCharacterStore} from "../../../../../stores/personaggio";
import Mobile_Stat from "../Shared/Mobile_Stat.vue";
import Mobile_HP from "../Shared/Mobile_HP.vue";
import Mobile_Contatore from "../Shared/Mobile_Contatore.vue";
import Tabella from "../../../../../components/Tabella.vue";
import Mobile_DettaglioItem from "../Dettaglio/Mobile_DettaglioItem.vue";

const characterStore = useCharacterStore()
const {cache} = storeToRefs(characterStore);

const props = defineProps({
  idPersonaggio: {
    type: Number,
    required: true
  }
});

const itemsTrasformazioni = ref<any[]>([]);
const itemsCompetenze = ref<any[]>([]);
const itemsLingue = ref<any[]>([]);
watch(
    () => cache.value[props.idPersonaggio]?.items,
    (newChar) => {
      if (!newChar?.trasformazioni) {
        itemsTrasformazioni.value = [];
        itemsCompetenze.value = [];
        itemsLingue.value = [];
        return;
      }

      itemsTrasformazioni.value = newChar.trasformazioni
          .map(itm => {
            return {
              ...itm,
              expandedComponent: markRaw(Mobile_DettaglioItem),
              expandedProps: {data: {item: {...itm}, personaggio: cache.value[props.idPersonaggio]}}
            };
          })
          .sort((a, b) => a.nome.localeCompare(b.nome));

      itemsCompetenze.value = newChar.competenze
          .sort((a, b) => a.nome.localeCompare(b.nome));

      itemsLingue.value = newChar.lingue
          .sort((a, b) => a.nome.localeCompare(b.nome));

    },
    {immediate: true, deep: true}
);

const columnsTrasformazioni = [
  {field: 'nome', label: 'Trasformazioni'},
];

const columnsCompetenze = [
  {field: 'nome', label: 'Competenze'},
];

const columnsLingue = [
  {field: 'nome', label: 'Lingue',},
];

</script>

<template>
  <div>
    <div class="nome">
      <h2>{{
          cache[idPersonaggio].modificatori?.nome ?? ""
        }}</h2>
    </div>
    <Mobile_HP v-if="cache[idPersonaggio].modificatori" :id-personaggio="idPersonaggio"/>
    <div v-if="cache[idPersonaggio].modificatori" class="stat-block">
      <Mobile_Stat id="FOR" :id-personaggio="idPersonaggio"></Mobile_Stat>
      <Mobile_Stat id="DES" :id-personaggio="idPersonaggio"></Mobile_Stat>
      <Mobile_Stat id="COS" :id-personaggio="idPersonaggio"></Mobile_Stat>
      <Mobile_Stat id="INT" :id-personaggio="idPersonaggio"></Mobile_Stat>
      <Mobile_Stat id="SAG" :id-personaggio="idPersonaggio"></Mobile_Stat>
      <Mobile_Stat id="CAR" :id-personaggio="idPersonaggio"></Mobile_Stat>
    </div>
    <div v-if="cache[idPersonaggio].modificatori" class="stat-block">
      <Mobile_Stat id="TMP" :id-personaggio="idPersonaggio" label="Tempra"></Mobile_Stat>
      <Mobile_Stat id="RFL" :id-personaggio="idPersonaggio" label="Riflessi"></Mobile_Stat>
      <Mobile_Stat id="VLT" :id-personaggio="idPersonaggio" label="Volonta"></Mobile_Stat>
    </div>
    <div v-if="cache[idPersonaggio].modificatori" class="stat-block">
      <Mobile_Stat id="CA" :id-personaggio="idPersonaggio" label="CA"></Mobile_Stat>
      <Mobile_Stat id="CAC" :id-personaggio="idPersonaggio" label="Contatto"></Mobile_Stat>
      <Mobile_Stat id="CAS" :id-personaggio="idPersonaggio" label="Sorpreso"></Mobile_Stat>
    </div>
    <div v-if="cache[idPersonaggio].modificatori" class="stat-block">
      <Mobile_Stat id="BAB" :id-personaggio="idPersonaggio" label="BAB"></Mobile_Stat>
      <Mobile_Stat id="LTT" :id-personaggio="idPersonaggio" label="Lotta"></Mobile_Stat>
      <Mobile_Stat id="MSC" :id-personaggio="idPersonaggio" label="Mischia"></Mobile_Stat>
      <Mobile_Stat id="GTT" :id-personaggio="idPersonaggio" label="Distanza"></Mobile_Stat>
    </div>
    <div class="stat-block">
      <template
          v-for="stat in cache[idPersonaggio].modificatori.attributi.filter(x => x.modificatori.length > 0)">
        <Mobile_Stat :id="stat.id" :id-personaggio="idPersonaggio" :label="stat.label"></Mobile_Stat>
      </template>
    </div>
    <div class="stat-block">
      <template
          v-for="stat in cache[idPersonaggio].modificatori.contatori.filter(x => x.id !== 'PF' && x.id != 'PFTEMP' && x.max > 0)">
        <Mobile_Contatore :id-stat="stat.id" :id-personaggio="idPersonaggio"></Mobile_Contatore>
      </template>
    </div>
    <div class="spazietto"></div>

    <Tabella v-if="itemsTrasformazioni.length > 0"
             :columns="columnsTrasformazioni"
             :expandable="true"
             :items="itemsTrasformazioni"
    >
    </Tabella>
    <div class="spazietto"></div>

    <Tabella v-if="itemsCompetenze.length > 0"
             :columns="columnsCompetenze"
             :expandable="false"
             :items="itemsCompetenze"
    >
    </Tabella>
    <div class="spazietto"></div>

    <Tabella v-if="itemsLingue.length > 0"
             :columns="columnsLingue"
             :expandable="false"
             :items="itemsLingue"
    >
    </Tabella>
  </div>
</template>

