<script setup lang="ts">
import {defineProps, markRaw, ref, watch} from 'vue';
import Tabella from "../../../../../../components/Tabella.vue";
import {useCharacterStore} from "../../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import Mobile_DettaglioLivello from "../../Dettaglio/Mobile_DettaglioLivello.vue";

const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);

const props = defineProps({
  idPersonaggio: {
    type: Number,
    required: true
  }
});
const itemsLivelli = ref<Object[]>([]);

watch(
    () => cache.value[props.idPersonaggio]?.items,
    async (newChar) => {
      if (!newChar || !newChar.livelli) {
        itemsLivelli.value = [];
        return;
      }

      // Ordina prima
      itemsLivelli.value = newChar.livelli
          .map(itm => {
            return {
              ...itm,
              expandedComponent: markRaw(Mobile_DettaglioLivello),
              expandedProps: {data: {item: {...itm}, personaggio: cache.value[props.idPersonaggio]}}
            };
          })
          .sort((a, b) => a.nome.localeCompare(b.nome));
    },
    {immediate: true, deep: true}
);

const columnsAttacchi = [
  {field: 'nomeItem', subfield: 'nome', label: 'Livello'},
];
</script>

<template>
  <div>
    <Tabella
        v-if="itemsLivelli.length > 0"
        :columns="columnsAttacchi"
        :expandable="true"
        :items="itemsLivelli"
    />
  </div>
</template>
