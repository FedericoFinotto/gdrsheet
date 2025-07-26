<script setup lang="ts">
import {computed, defineProps, markRaw, ref, watch} from 'vue';
import Tabella from "../../../../components/Tabella.vue";
import {getDatiCaratteristica, getModificatoriFromPersonaggio, testoModificatore} from "../../../../function/Utils";
import DettaglioAbilita from "../../../../components/Mobile_DettaglioAbilita.vue";

const props = defineProps({
  datiPersonaggio: {
    type: Object,
    required: true
  }
});

const mods = computed(() => props.datiPersonaggio?.character ? getModificatoriFromPersonaggio(props.datiPersonaggio.character) : []);
const items = ref<any[]>([]);
watch(
    () => props.datiPersonaggio?.character,
    (newChar) => {
      if (!newChar?.stats) { items.value = []; return; }

      items.value = newChar.stats
          .filter(s => s.stat.tipo === 'AB')
          .map(stat => {
            const abilita = getDatiCaratteristica(props.datiPersonaggio, stat.stat.id, mods.value);
            return {
              ...abilita,
              id: abilita.statistica.id,
              nome: abilita.statistica.label,
              valore: testoModificatore(abilita.statistica.modificatore),
              caratteristica: abilita.base?.id ?? '',
              expandedComponent: markRaw(DettaglioAbilita),
              expandedProps: {data: {...abilita}}
            };
          })
          .filter(s => !s.rank.addestramento || (s.rank.valore > 0))
          .sort((a, b) => a.nome.localeCompare(b.nome)); // 👈 ordinamento alfabetico per nome
    },
    { immediate: true, deep: true }
);

const columns = [
  { field: 'nome', label: 'Abilita\'' },
  { field: 'valore', label: '' },
  { field: 'caratteristica', label: '' }
];
</script>

<template>
  <Tabella
      :columns="columns"
      :expandable="true"
      :items="items"
  >
  </Tabella>
</template>

<style scoped>
</style>