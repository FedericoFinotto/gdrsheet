<script setup lang="ts">
import {computed, defineProps, markRaw, ref, watch} from 'vue';
import Tabella from "../../../../components/Tabella.vue";
import {getDatiCaratteristica, getModificatoriFromPersonaggio} from "../../../../function/Utils";
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
            console.log('LETTA', stat.stat.label, abilita);
            return {
              nome: stat.stat.label,
              id: stat.stat.id,
              caratteristica: stat.mod?.id,
              valore: abilita.modificatore,
              modificatori: abilita.modificatori,
              base: stat.valore,
              bonusCaratteristica: abilita.statisticaBase?.modificatore ?? 0,
              expandedComponent: markRaw(DettaglioAbilita),
              expandedProps: { data: {
                  nome: stat.stat.label,
                  base: stat.valore,
                  bonusCaratteristica: abilita.statisticaBase?.modificatore,
                  modificatori: abilita.modificatori,
                  caratteristica: stat.mod?.id
                }}
            };
          });
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