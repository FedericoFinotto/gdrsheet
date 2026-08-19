<!--
  Mostra il valore di una label "costo in azioni" (oggi solo TEMPO_SP degli incantesimi): se
  mostraSimboli è attivo e il testo corrisponde a un pattern noto (vedi function/azioni.ts), lo
  sostituisce col glifo del font Pathfinder2eActions; altrimenti mostra il testo com'è.
-->
<script setup lang="ts">
import {computed} from 'vue'
import {parseAzioneGlifo} from '../function/azioni'

const props = defineProps<{ testo: string; mostraSimboli: boolean }>()

const parsed = computed(() => props.mostraSimboli ? parseAzioneGlifo(props.testo) : null)
</script>

<template>
  <template v-if="parsed">
    <span class="pf2e-icon" :title="testo">{{ parsed.glifo }}</span>
    <span v-if="parsed.resto">&nbsp;{{ parsed.resto }}</span>
  </template>
  <template v-else>{{ testo }}</template>
</template>

<!-- .pf2e-icon è definita globalmente in styles/global.css (condivisa con chi mostra il glifo
     fuori da questo componente, es. Mobile_DettaglioItem.vue accanto ai simboli componenti). -->
