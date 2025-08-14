<script setup>
import {computed, defineProps} from 'vue'
import {useCharacterStore} from "../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import {testoModificatore} from "../../../../../function/Utils";
import usePopup from "../../../../../function/usePopup";
import Mobile_DettaglioCaratteristica from "../Dettaglio/Mobile_DettaglioCaratteristica.vue";

const {openPopup} = usePopup()

const characterStore = useCharacterStore()
const {cache} = storeToRefs(characterStore);

const props = defineProps({
  idPersonaggio: {
    type: Number,
    required: true
  },
  id: {
    type: String,
    required: true
  },
  label: {
    type: String,
    default: undefined
  }
});

function showPopup() {
  openPopup(
      Mobile_DettaglioCaratteristica,
      {stat, idPersonaggio: props.idPersonaggio},
      {closable: true, autoClose: 0}
  )
}

const stat = computed(() => {
  const statistiche = cache.value[props.idPersonaggio]?.modificatori;

  if (statistiche) {
    const caratteristica = statistiche.caratteristiche.find(x => x.id === props.id);
    const classeArmatura = statistiche.classeArmatura.find(x => x.id === props.id);
    const tiroSalvezza = statistiche.tiriSalvezza.find(x => x.id === props.id);
    const bonusAttacco = statistiche.bonusAttacco.find(x => x.id === props.id);
    const attributo = statistiche.attributi.find(x => x.id === props.id);

    if (caratteristica) {
      return caratteristica;
    }
    if (classeArmatura) {
      return classeArmatura;
    }
    if (tiroSalvezza) {
      return tiroSalvezza;
    }
    if (bonusAttacco) {
      const values = bonusAttacco.attacchiMultipli;
      bonusAttacco.modificatore = values
          .map(v => testoModificatore(v))
          .join(' / ');
      return bonusAttacco;
    }
    if (attributo) {
      const infinito = attributo.modificatori.filter(x => x.sempreAttivo && x.formula === '+INF');
      let mod = attributo.modificatore;
      if (infinito && infinito.length > 0) {
        mod = '+∞';
      }
      return {
        ...attributo,
        modificatore: mod
      };
    }
    return null;
  }
});
</script>

<template>
  <div class="stat-box">
    <div class="label">{{ label ?? id }}</div>
    <div class="modifier" @click="showPopup">{{ stat ? (testoModificatore(stat.modificatore) ?? '') : '' }}</div>
    <div class="base">{{ stat ? (stat.valore ?? '') : '' }}</div>
  </div>
</template>