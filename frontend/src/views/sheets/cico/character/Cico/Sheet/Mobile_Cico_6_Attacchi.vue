<script setup lang="ts">
import {computed, defineAsyncComponent, defineProps, ref, watch} from 'vue';
import Tabella from "../../../../../../components/Tabella.vue";
import {useCharacterStore} from "../../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import {getValoreFormula} from "../../../../../../function/Calcolo";
import {applicaBonusDado, removePlus, risolviFormulaDanno, testoFormula, testoModificatore} from "../../../../../../function/Utils";
import {Attacco} from "../../../../../../models/dto/Attacco";
import useDiceRoll from "../../../../../../function/useDiceRoll";
import usePopup from "../../../../../../function/usePopup";

const DiceRollerPopup = defineAsyncComponent(() => import('../../../../../../components/DiceRollerPopup.vue'))
const {openPopup} = usePopup()

const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);
const {risultato} = useDiceRoll()

const props = defineProps({
  idPersonaggio: {
    type: Number,
    required: true
  }
});

interface AttaccoRow {
  id: any
  nome: string
  nomeItem: string
  atk: string | null        // valore base calcolato
  dmg: string | null
  attacco: string
  colpo: string
  [key: string]: any
}

const itemsAttacchi = ref<AttaccoRow[]>([]);

watch(
    () => cache.value[props.idPersonaggio]?.items,
    async (newChar) => {
      if (!newChar || !newChar.attacchi) {
        itemsAttacchi.value = [];
        return;
      }

      const sorted = [...newChar.attacchi].sort((a: Attacco, b: Attacco) =>
          a.nomeItem.localeCompare(b.nomeItem)
      );

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
              dannoVal = risolviFormulaDanno(resp.data.formula, cache.value[props.idPersonaggio].modificatori);
            }

            return {
              ...itm,
              atk: itm.tiroSalvezza ? `CD ${removePlus(atkVal)} ${itm.tiroSalvezza}` : atkVal,
              dmg: dannoVal,
              attacco: itm.attacco ? testoFormula(itm.attacco) : '',
              colpo: itm.colpo ? testoFormula(itm.colpo) : '',
            };
          })
      );
    },
    {immediate: true, deep: true}
);

// applica il bonus d20 al valore atk quando il dado è attivo
const itemsDisplay = computed(() => {
  if (risultato.value === null) return itemsAttacchi.value
  return itemsAttacchi.value.map(itm => ({
    ...itm,
    atk: itm.atk != null ? applicaBonusDado(itm.atk, risultato.value!) : itm.atk
  }))
})

function apriDanno(row: AttaccoRow) {
  if (!row.dmg) return
  openPopup(DiceRollerPopup, {initialFormula: row.dmg}, {closable: true, autoClose: 0})
}

const columnsAttacchi = [
  {field: 'nome', subfield: 'nomeItem', label: 'Arma'},
  {field: 'atk', subfield: 'attacco', label: 'Colpire'},
  {field: 'dmg', subfield: 'colpo', label: 'Danno', onClick: (row: any) => apriDanno(row)}
];
</script>

<template>
  <div>
    <Tabella
        v-if="itemsDisplay.length > 0"
        :columns="columnsAttacchi"
        :expandable="false"
        :items="itemsDisplay"
    />
  </div>
</template>
