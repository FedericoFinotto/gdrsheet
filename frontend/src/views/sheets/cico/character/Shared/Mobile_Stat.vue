<template>
  <div class="stat-box">
    <div class="label">{{ label ?? id }}</div>
    <div class="modifier">{{ stat ? (testoModificatore(stat.modificatore) ?? '') : '' }}</div>
    <div class="base">{{ stat ? (stat.valore ?? '') : '' }}</div>
  </div>
</template>

<script setup>
import {computed, defineProps} from 'vue'
import {useCharacterStore} from "../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import {testoModificatore} from "../../../../../function/Utils";

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

const stat = computed(() => {
  const statistiche = cache.value[props.idPersonaggio]?.modificatori;

  if (statistiche) {
    const caratteristica = statistiche.caratteristiche.find(x => x.id === props.id);
    const classeArmatura = statistiche.classeArmatura.find(x => x.id === props.id);
    const tiroSalvezza = statistiche.tiriSalvezza.find(x => x.id === props.id);

    if (caratteristica) {
      return caratteristica;
    }
    if (classeArmatura) {
      return classeArmatura;
    }
    if (tiroSalvezza) {
      return tiroSalvezza;
    }
    return null;
  }
});
</script>

<style scoped>
.stat-box {
  display: inline-flex;
  flex-direction: column;
  justify-content: space-between;

  /* larghezza automatica in base al contenuto */
  width: auto;
  min-width: 45px;

  padding: 4px;
  border: 2px solid #888;
  border-radius: 8px;
  background: #f3f3f3;
  box-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);

  text-align: center;
}

.label {
  font-size: 0.8rem;
  font-weight: bold;
  text-transform: uppercase;
}

.modifier {
  font-size: 1.4rem;
  font-weight: bold;
  color: #444;
}

.base {
  font-size: 1rem;
  color: #666;
}
</style>
