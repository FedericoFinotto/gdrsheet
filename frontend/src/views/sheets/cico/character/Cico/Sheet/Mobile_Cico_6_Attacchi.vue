<script setup lang="ts">
import {computed, defineAsyncComponent, defineProps} from 'vue';
import Tabella from "../../../../../../components/Tabella.vue";
import {useCharacterStore} from "../../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import {applicaBonusDado} from "../../../../../../function/Utils";
import {AttaccoCalcolatoRow} from "../../../../../../function/Calcolo";
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

// Le formule sono già precalcolate in background dallo store (subito dopo il caricamento di
// items), quindi qui basta leggere dalla cache: niente chiamate di rete al primo render.
const itemsAttacchi = computed(() => cache.value[props.idPersonaggio]?.attacchi ?? []);

// applica il bonus d20 al valore atk quando il dado è attivo
const itemsDisplay = computed(() => {
  if (risultato.value === null) return itemsAttacchi.value
  return itemsAttacchi.value.map(itm => ({
    ...itm,
    atk: itm.atk != null ? applicaBonusDado(itm.atk, risultato.value!) : itm.atk
  }))
})

function apriDanno(row: AttaccoCalcolatoRow) {
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
