<template>
  <div class="hp-container">
    <button @click="modificaHp(-1)">-</button>
    <div class="hp-bar-wrapper">
      <div class="hp-bar" :style="{ background: barraGradient }">
        <span class="hp-text">
          {{ hp }}
          <template v-if="pfTEMP > 0">
            + {{ pfTEMP }} ({{ hp + pfTEMP }})
          </template>
          / {{ hpMax }}

        </span>
      </div>
    </div>
    <button @click="modificaHp(1)">+</button>
  </div>
</template>

<script setup>
import {computed, defineProps} from 'vue'
import {useCharacterStore} from "../../../../../stores/personaggio";
import {storeToRefs} from "pinia";

const characterStore = useCharacterStore()
const {cache} = storeToRefs(characterStore)

const props = defineProps({
  idPersonaggio: {
    type: Number,
    required: true
  }
});

const pf = computed(() =>
    cache.value[props.idPersonaggio].modificatori.contatori.find(stat => stat.id === 'PF')
);

const pfTEMP = computed(() =>
    cache.value[props.idPersonaggio].modificatori.contatori.find(stat => stat.id === 'PFTEMP')?.valore ?? 0
);

const hp = computed(() => pf.value?.valore ?? 0);
const hpMax = computed(() => pf.value?.max ?? 0);
const total = computed(() => hpMax.value + pfTEMP.value);

const hpPercent = computed(() => total.value > 0 ? (hp.value / total.value) * 100 : 0);
const pfTempPercent = computed(() => total.value > 0 ? (pfTEMP.value / total.value) * 100 : 0);

// Crea il gradiente in base a hp + pfTEMP
const barraGradient = computed(() => {
  const hp = hpPercent.value;
  const temp = pfTempPercent.value;
  return `linear-gradient(to right,
    #28a745 0%,
    #28a745 ${hp}%,
    #17a2b8 ${hp}%,
    #17a2b8 ${hp + temp}%,
    #ddd ${hp + temp}%,
    #ddd 100%)`;
});

function modificaHp(delta) {
  const pf = cache.value[props.idPersonaggio].modificatori.contatori.find(stat => stat.id === 'PF');
  const pftemp = cache.value[props.idPersonaggio].modificatori.contatori.find(stat => stat.id === 'PFTEMP');

  if (!pf) return;

  if (delta < 0) {
    let danno = -delta;

    // Scala prima i PF TEMP
    if (pftemp && pftemp.valore > 0) {
      const toltiDaTemp = Math.min(danno, pftemp.valore);
      pftemp.valore -= toltiDaTemp;
      danno -= toltiDaTemp;
    }

    // Poi scala i PF normali
    if (danno > 0) {
      pf.valore = Math.max(0, pf.valore - danno);
    }

  } else if (delta > 0) {
    // Cura solo i PF normali
    pf.valore = Math.min(pf.max, pf.valore + delta);
  }

  // updateHp(props.idPersonaggio, pf.valore).catch(err => {
  //   console.error('Errore aggiornamento HP', err);
  // });
}

</script>

<style scoped>
.hp-container {
  display: flex;
  align-items: center;
  width: 100%;
}

button {
  padding: 0.3rem 0.6rem;
  font-size: 1.2rem;
  flex-shrink: 0;
  height: 2rem;
}

.hp-bar-wrapper {
  flex: 1;
  height: 2rem;
  position: relative;
  overflow: hidden;
  border: 1px solid #999;
}

.hp-bar {
  height: 100%;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.3s;
}

.hp-text {
  color: #000;
  font-weight: bold;
  z-index: 2;
  text-align: center;
  white-space: nowrap;
}
</style>
