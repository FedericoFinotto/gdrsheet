<script setup>
import Tag from 'primevue/tag'
import Tabella from "./Tabella.vue";
import {computed, inject} from "vue";
import {getModificatoriFromPersonaggio} from "../function/Utils.ts";

// Iniezione dei dati condivisi
const sharedData = inject('sharedData');

// Calcolo dei modificatori
const mods = computed(() => {
  return getModificatoriFromPersonaggio(sharedData.character);
});

// Calcolo degli items
const items = computed(() => {
  console.log("MODS", mods.value);
  return sharedData.character.stats?.filter(stat => stat.stat.tipo === 'AB').map(stat => {
    const thisMods = mods.value.filter(mod => mod.stat.id === stat.stat.id);
    return {
      nome: stat.stat.label,
      caratteristica: stat.mod?.id,
      valore: 10,
      modificatori: thisMods
    };
  }) ?? [];
});

// // Calcolo del valore di un particolare stat con modificatori
// const value = computed(() => {
//   const base = stat?.value?.valore ?? 10;  // Default a 10 se non definito
//   const bonus = mods.value
//       .filter(mod => mod.stat.id === props.id)
//       .reduce((sum, mod) => sum + mod.valore, 0);  // Somma i valori dei modificatori
//   return base + bonus;
// });

// Definizione delle colonne per la tabella
const columns = [
  {field: 'nome', label: 'Abilita\''},
  {field: 'valore', label: ''},
  {field: 'caratteristica', label: ''}
];
</script>

<template>
  <!-- Rendering della Tabella con i dati -->
  <Tabella :columns="columns" :items="items"/>
</template>
