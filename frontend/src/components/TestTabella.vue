<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue';
import Tabella from "./Tabella.vue";
import { getModificatoriFromPersonaggio } from "../function/Utils.ts";
import DettaglioAbilita from "./DettaglioAbilita.vue"; // Importa il componente da mostrare nell'espansione

// Assicurati che SharedData e Stat siano definiti correttamente, se usi TypeScript
interface Stat {
  stat: { id: string; label: string; tipo: string; };
  mod?: { id: string; };
  valore: number;
}

interface Character {
  stats?: Stat[];
  // Altre proprietà di character
}

interface SharedData {
  character: Character;
  // Altre proprietà di sharedData
}

const sharedData = inject<SharedData>('sharedData');

const statMod = (mod: { id: string } | undefined) => {
  if (mod?.id) {
    const stat = sharedData?.character.stats?.find(s => s.stat?.id === mod.id);
    const base = stat?.valore ?? 0;
    const bonus = mods.value
        .filter(m => m.stat.id === stat?.stat.id)
        .reduce((sum, m) => sum + m.valore, 0);
    const valore = base + bonus;
    return Math.floor((valore - 10) / 2);
  }
  return 0;
};

const mods = computed(() => {
  return sharedData?.character ? getModificatoriFromPersonaggio(sharedData.character) : [];
});

const items = ref([]); // `items` è ora un ref, aggiornato tramite watcher

// Watcher per costruire l'array `items` quando `sharedData.character` cambia
watch(
    () => sharedData?.character,
    (newCharacter) => {
      if (!newCharacter || !newCharacter.stats) {
        items.value = [];
        return;
      }

      items.value = newCharacter.stats
          .filter(stat => stat.stat.tipo === 'AB')
          .map(stat => {
            const thisMods = mods.value.filter(mod => mod.stat.id === stat.stat.id);
            const bonus = thisMods.reduce((sum, mod) => sum + mod.valore, 0);
            const valore = stat.valore + bonus + statMod(stat.mod);

            return {
              nome: stat.stat.label,
              id: stat.stat.id,
              caratteristica: stat.mod?.id,
              valore: valore,
              modificatori: thisMods,
              // Nota: non c'è più `expanded` qui, perché il contenuto è gestito dallo slot
              // expandedData: { ... } // Puoi comunque passare questi dati allo slot se vuoi
            };
          });
    },
    {
      immediate: true,
      deep: true
    }
);

const columns = [
  { field: 'nome', label: 'Abilita\'' },
  { field: 'valore', label: '' },
  { field: 'caratteristica', label: '' }
];
</script>

<template>
  <div>
    <Tabella :columns="columns" :items="items" :expandable="true">
      <template #expansion="slotProps">
        <div>
          <DettaglioAbilita :data="slotProps.data" />
        </div>
      </template>

      <template #col-valore="{ data }">
        <span :class="{'text-bold-green': data.valore > 10}">{{ data.valore }}</span>
      </template>
    </Tabella>
  </div>
</template>

<style scoped>
.expanded-row-content {
  background-color: #f8f8f8;
  border-left: 5px solid #6c757d;
  padding: 1rem;
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
  border-radius: 4px;
}
.text-bold-green {
  font-weight: bold;
  color: green;
}
</style>