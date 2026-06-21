<script setup lang="ts">
import {computed, defineProps} from 'vue';
import Tabella from "../../../../../../components/Tabella.vue";
import {useCharacterStore} from "../../../../../../stores/personaggio";
import {storeToRefs} from "pinia";

const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);

const props = defineProps({
  idPersonaggio: {type: Number, required: true}
});

const itemsCompetenze = computed(() =>
    [...(cache.value[props.idPersonaggio]?.items?.competenze ?? [])]
        .sort((a: any, b: any) => a.nome.localeCompare(b.nome))
);
const itemsLingue = computed(() =>
    [...(cache.value[props.idPersonaggio]?.items?.lingue ?? [])]
        .sort((a: any, b: any) => a.nome.localeCompare(b.nome))
);

const columnsCompetenze = [{field: 'nome', label: 'Competenze'}];
const columnsLingue = [{field: 'nome', label: 'Lingue'}];
</script>

<template>
  <div>
    <Tabella v-if="itemsCompetenze.length" :columns="columnsCompetenze" :expandable="false" :items="itemsCompetenze"/>
    <div class="spazietto"/>

    <Tabella v-if="itemsLingue.length" :columns="columnsLingue" :expandable="false" :items="itemsLingue"/>
  </div>
</template>
