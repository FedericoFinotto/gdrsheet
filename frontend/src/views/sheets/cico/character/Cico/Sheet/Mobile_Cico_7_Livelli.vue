<script setup lang="ts">
import {defineProps, markRaw, ref, watch} from 'vue';
import Tabella from "../../../../../../components/Tabella.vue";
import {useCharacterStore} from "../../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import Mobile_DettaglioLivello from "../../Dettaglio/Mobile_DettaglioLivello.vue";
import {Livello} from "../../../../../../models/dto/Livello";

const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);

const props = defineProps({
  idPersonaggio: {
    type: Number,
    required: true
  }
});
const itemsLivelli = ref<Livello[]>([]);

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
              testoLivello: `Livello ${itm.livello}`,
              testoClasse: `${itm.classe ?? ''}${itm.maledizione ?? ''} ${itm.livelliClasse.join(' ')}`,
              expandedComponent: markRaw(Mobile_DettaglioLivello),
              expandedProps: {data: {item: {...itm}, personaggio: cache.value[props.idPersonaggio]}}
            };
          })
          .sort((a, b) => Number(a.livello ?? 0) - Number(b.livello ?? 0));
    },
    {immediate: true, deep: true}
);

const columnsAttacchi = [
  {field: 'testoLivello', subfield: '', label: 'Livello'},
  {field: 'testoClasse', subfield: '', label: ''},
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
