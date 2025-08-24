<script setup lang="ts">
import {defineProps, ref, watch} from 'vue';
import Tabella from "../../../../../../components/Tabella.vue";
import {useCharacterStore} from "../../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import {getValoreFormula} from "../../../../../../function/Calcolo";
import {testoFormula, testoModificatore} from "../../../../../../function/Utils";
import {Attacco} from "../../../../../../models/dto/Attacco";

const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);

const props = defineProps({
  idPersonaggio: {
    type: Number,
    required: true
  }
});
const itemsAttacchi = ref<Object[]>([]);

watch(
    () => cache.value[props.idPersonaggio]?.items,
    async (newChar) => {
      if (!newChar || !newChar.attacchi) {
        itemsAttacchi.value = [];
        return;
      }

      // Ordina prima
      const sorted = [...newChar.attacchi].sort((a: Attacco, b: Attacco) =>
          a.nomeItem.localeCompare(b.nomeItem)
      );

      // Enrich asincrono: trasforma ogni itm in { ...itm, atk }
      itemsAttacchi.value = await Promise.all(
          sorted.map(async itm => {
            let atkVal: string | null = null;
            let dannoVal: string | null = null;

            if (itm.attacco) {
              const resp = await getValoreFormula(cache.value[props.idPersonaggio].modificatori, itm.attacco);
              atkVal = testoModificatore(resp.data.risultato);
            }

            if (itm.colpo) {
              const resp = await getValoreFormula(cache.value[props.idPersonaggio].modificatori, itm.colpo);
              dannoVal = testoModificatore(resp.data.risultato);
            }

            return {
              ...itm,
              atk: atkVal,
              dmg: dannoVal,
              attacco: itm.attacco ? testoFormula(itm.attacco) : '',
              colpo: itm.colpo ? testoFormula(itm.colpo) : '',
            };
          })
      );
    },
    {immediate: true, deep: true}
);

const columnsAttacchi = [
  {field: 'nomeItem', subfield: 'nome', label: 'Arma'},
  {field: 'atk', subfield: 'attacco', label: 'Colpire'},
  {field: 'dmg', subfield: 'colpo', label: 'Danno'}
];
</script>

<template>
  <div>
    <Tabella
        v-if="itemsAttacchi.length > 0"
        :columns="columnsAttacchi"
        :expandable="false"
        :items="itemsAttacchi"
    />
  </div>
</template>
