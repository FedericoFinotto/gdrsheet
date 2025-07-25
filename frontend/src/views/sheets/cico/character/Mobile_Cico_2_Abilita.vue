<script setup lang="ts">
import {computed, defineProps, markRaw, ref, watch} from 'vue';
import Tabella from "../../../../components/Tabella.vue";
import {getModificatoriFromPersonaggio} from "../../../../function/Utils";
import DettaglioAbilita from "../../../../components/Mobile_DettaglioAbilita.vue";

const props = defineProps({
  datiPersonaggio: {
    type: Object,
    required: true
  }
});

const statMod = (mod: { id: string } | undefined) => {
  if (mod?.id) {
    const statObj = props.datiPersonaggio?.character.stats?.find(s => s.stat.id === mod.id);
    const base = parseInt(statObj?.valore) ?? 0;
    const bonus = mods.value
        .filter(m => m.stat.id === statObj?.stat.id)
        .reduce((sum, m) => sum + m.valore, 0);
    return Math.floor((base + bonus - 10) / 2);
  }
  return 0;
};

const mods = computed(() => props.datiPersonaggio?.character ? getModificatoriFromPersonaggio(props.datiPersonaggio.character) : []);
const items = ref<any[]>([]);
watch(
    () => props.datiPersonaggio?.character,
    (newChar) => {
      if (!newChar?.stats) { items.value = []; return; }
      items.value = newChar.stats
          .filter(s => s.stat.tipo === 'AB')
          .map(stat => {
            const thisMods = mods.value.filter(m => m.stat.id === stat.stat.id);
            const bonus = thisMods.reduce((sum, m) => sum + parseInt(m.valore), 0);
            const bonusCar = statMod(stat.mod);
            const valore = parseInt(stat.valore) + bonus + bonusCar;
            return {
              nome: stat.stat.label,
              id: stat.stat.id,
              caratteristica: stat.mod?.id,
              valore,
              modificatori: thisMods,
              base: stat.valore,
              bonusCaratteristica: bonusCar,
              expandedComponent: markRaw(DettaglioAbilita),
              expandedProps: { data: {
                  nome: stat.stat.label,
                  base: stat.valore,
                  bonusCaratteristica: bonusCar,
                  modificatori: thisMods,
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
    <template #col-valore="{ data }">
      <span :class="{'text-bold-green': data.valore > 10}">{{ data.valore }}</span>
    </template>
  </Tabella>
</template>

<style scoped>
.text-bold-green { font-weight: bold; color: green; }
</style>