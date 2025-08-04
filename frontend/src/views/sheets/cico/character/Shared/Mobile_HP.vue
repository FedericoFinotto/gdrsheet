<template>
  <div class="hp-container" :style="{ background: barraGradient }">
    <button @click="modificaHp(-1)">-</button>
    <div class="hp-bar-wrapper">
      <div class="hp-bar">
        <div class="delta-left" v-if="deltaInAttesa < 0">
          ({{ deltaInAttesa }})
        </div>

        <div class="hp-center">
          <template v-if="pfTEMP > 0">
            {{ hp + pfTEMP }} ({{ hp }} + {{ pfTEMP }}) / {{ hpMax }}
          </template>
          <template v-else>
            {{ hp }} / {{ hpMax }}
          </template>
        </div>

        <div class="delta-right" v-if="deltaInAttesa > 0">
          (+{{ deltaInAttesa }})
        </div>
      </div>
    </div>
    <button @click="modificaHp(1)">+</button>
  </div>

</template>

<script setup lang="ts">
import {computed, defineProps, ref} from 'vue'
import {useCharacterStore} from '../../../../../stores/personaggio'
import {storeToRefs} from 'pinia'

const characterStore = useCharacterStore()
const {cache} = storeToRefs(characterStore)

const props = defineProps({
  idPersonaggio: {
    type: Number,
    required: true
  }
})

const deltaInAttesa = ref(0)
const pfTempRimossiInAttesa = ref(0)
let timerId: ReturnType<typeof setTimeout> | null = null

const pf = computed(() =>
    cache.value[props.idPersonaggio].modificatori.contatori.find(stat => stat.id === 'PF')
)

const pfTEMP = computed(() =>
    cache.value[props.idPersonaggio].modificatori.contatori.find(stat => stat.id === 'PFTEMP')?.valore ?? 0
)

const hp = computed(() => pf.value?.valore ?? 0)
const hpMax = computed(() => pf.value?.max ?? 0)
const total = computed(() => hpMax.value + pfTEMP.value)

const hpPercent = computed(() => (total.value > 0 ? (hp.value / total.value) * 100 : 0))
const pfTempPercent = computed(() => (total.value > 0 ? (pfTEMP.value / total.value) * 100 : 0))

const barraGradient = computed(() => {
  const hp = hpPercent.value
  const temp = pfTempPercent.value
  return `linear-gradient(to right,
    #28a745 0%,
    #28a745 ${hp}%,
    #17a2b8 ${hp}%,
    #17a2b8 ${hp + temp}%,
    #ddd ${hp + temp}%,
    #ddd 100%)`
})

function modificaHp(delta: number) {
  const pf = cache.value[props.idPersonaggio].modificatori.contatori.find(stat => stat.id === 'PF')
  const pftemp = cache.value[props.idPersonaggio].modificatori.contatori.find(stat => stat.id === 'PFTEMP')

  if (!pf) return

  deltaInAttesa.value += delta

  if (delta < 0) {
    let danno = -delta
    if (pftemp && pftemp.valore > 0) {
      const toltiDaTemp = Math.min(danno, pftemp.valore)
      pftemp.valore -= toltiDaTemp
      pfTempRimossiInAttesa.value += toltiDaTemp
      danno -= toltiDaTemp
    }
    if (danno > 0) {
      pf.valore = Math.max(0, pf.valore - danno)
    }
  } else if (delta > 0) {
    // Prima ripristina PF TEMP se ne ho tolti in attesa
    if (pfTempRimossiInAttesa.value > 0) {
      const daRipristinare = Math.min(delta, pfTempRimossiInAttesa.value)
      if (pftemp) {
        pftemp.valore += daRipristinare
        pfTempRimossiInAttesa.value -= daRipristinare
        delta -= daRipristinare
      }
    }
    // Poi cura PF normali
    if (delta > 0) {
      pf.valore = Math.min(pf.max, pf.valore + delta)
    }
  }

  if (timerId) clearTimeout(timerId)
  timerId = setTimeout(() => {
    persistHp()
  }, 3000)
}

function persistHp() {
  const pf = cache.value[props.idPersonaggio].modificatori.contatori.find(stat => stat.id === 'PF')
  const pftemp = cache.value[props.idPersonaggio].modificatori.contatori.find(stat => stat.id === 'PFTEMP')

  if (!pf) return

  // TODO: chiamate asincrone verso backend
  // updateHp(props.idPersonaggio, pf.valore).catch(...)
  // updatePfTemp(props.idPersonaggio, pftemp?.valore).catch(...)

  deltaInAttesa.value = 0
  pfTempRimossiInAttesa.value = 0
  timerId = null
}
</script>

<style scoped>
.hp-container {
  display: flex;
  width: 100%;
  overflow: hidden;
  height: 2rem;
  align-items: center;
  border: 1px solid var(--border-color);
}

button {
  padding: 0.3rem 0.6rem;
  font-size: 1.2rem;
  flex-shrink: 0;
  height: 100%;
  background: transparent;
  border: none;
  z-index: 3;
  cursor: pointer;
}

.hp-bar-wrapper {
  flex: 1;
  height: 100%;
  position: relative;
}

.hp-bar {
  height: 100%;
  width: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hp-center {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  font-weight: bold;
  font-size: 0.9rem;
  color: #000;
  white-space: nowrap;
  z-index: 2;
}

.delta-left,
.delta-right {
  position: absolute;
  top: 0;
  height: 100%;
  display: flex;
  align-items: center;
  font-weight: bold;
  font-size: 0.9rem;
  z-index: 2;
  color: #000;
}

.delta-left {
  left: 0.5rem;
  justify-content: flex-start;
}

.delta-right {
  right: 0.5rem;
  justify-content: flex-end;
}

</style>
