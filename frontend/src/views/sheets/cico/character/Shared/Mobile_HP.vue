<template>
  <div class="hp-container" :style="{ backgroundImage: barraGradient }">
    <button class="hp-btn" @click="modifyHp(-1)">-</button>
    <div class="hp-bar-wrapper">
      <div class="hp-bar">
        <div class="delta" v-if="delta < 0">
          ({{ delta }})
        </div>
        <div class="hp-center">
          <template v-if="pfTemp > 0">
            {{ hp + pfTemp }} ({{ hp }} + {{ pfTemp }}) / {{ hpMax }}
          </template>
          <template v-else>
            {{ hp }} / {{ hpMax }}
          </template>
        </div>
        <div class="delta" v-if="delta > 0">
          +{{ delta }}
        </div>
      </div>
    </div>
    <button class="hp-btn" @click="modifyHp(+1)">+</button>
  </div>
</template>

<script setup lang="ts">
import {computed, defineProps, onBeforeUnmount, ref, watch} from 'vue'
import {useCharacterStore} from '@/stores/personaggio'
import {storeToRefs} from 'pinia'
import {updateHP} from '@/service/PersonaggioService'
import debounce from 'lodash/debounce'

const props = defineProps<{ idPersonaggio: number }>()

// store slice
const {cache} = storeToRefs(useCharacterStore())

// --- stati di sessione ---
const delta = ref(0) // visualizza l’ultima modifica
const pfTempRemoved = ref(0) // quanti PF TEMP tolti finora
const hpAdded = ref(0) // quanti HP normali aggiunti finora

// computed su contatori
const pfStat = computed(() =>
    cache.value[props.idPersonaggio]?.modificatori.contatori.find(s => s.id === 'PF')
)
const pfTempStat = computed(() =>
    cache.value[props.idPersonaggio]?.modificatori.contatori.find(s => s.id === 'PFTEMP')
)

// valori numerici
const hp = computed(() => pfStat.value?.valore ?? 0)
const hpMax = computed(() => pfStat.value?.max ?? 0)
const pfTemp = computed(() => pfTempStat.value?.valore ?? 0)

// percentuali per il gradiente
const total = computed(() => hpMax.value + pfTemp.value)
const hpPercent = computed(() => total.value > 0 ? (hp.value / total.value) * 100 : 0)
const tempPercent = computed(() => total.value > 0 ? (pfTemp.value / total.value) * 100 : 0)

const barraGradient = computed(() =>
    `linear-gradient(to right,` +
    `#28a745 0%,#28a745 ${hpPercent.value}%,` +
    `#17a2b8 ${hpPercent.value}%,#17a2b8 ${hpPercent.value + tempPercent.value}%,` +
    `#ddd ${hpPercent.value + tempPercent.value}%,#ddd 100%` +
    `)`
)

// debounce dell’update remoto: 3s dopo l’ultima variazione
const debouncedUpdate = debounce(() => {
  updateHP(
      props.idPersonaggio,
      pfStat.value?.valore ?? 0,
      pfTempStat.value?.valore ?? 0
  ).then(() => {
    console.log('HP sincronizzati');
    // fine sessione: resetto gli stati temporanei
    delta.value = 0
    pfTempRemoved.value = 0
    hpAdded.value = 0
  })
}, 3000)

// ogni volta che cambiano hp o pfTemp, rilancio il debounce
watch([hp, pfTemp], () => {
  debouncedUpdate()
}, {flush: 'post'})

// cancello il debounce al destroy
onBeforeUnmount(() => {
  debouncedUpdate.cancel()
})

/**
 * Modifica i valori in cache e tiene traccia di hpAdded / pfTempRemoved.
 */
function modifyHp(amount: number) {
  delta.value += amount
  let rem = amount

  if (rem > 0) {
    // **Heal**: prima ripristino eventuali PF TEMP tolti
    if (pfTempRemoved.value > 0) {
      const restore = Math.min(rem, pfTempRemoved.value)
      pfTempStat.value!.valore += restore
      pfTempRemoved.value -= restore
      rem -= restore
    }
    // poi aggiungo HP “veri” e li conto in hpAdded
    if (rem > 0 && pfStat.value) {
      const added = Math.min(rem, pfStat.value.max - pfStat.value.valore)
      pfStat.value.valore += added
      hpAdded.value += added
    }

  } else if (rem < 0) {
    // **Undo di un heal**: prima tolgo gli HP appena aggiunti
    let damage = -rem
    if (hpAdded.value > 0) {
      const undo = Math.min(damage, hpAdded.value)
      pfStat.value!.valore -= undo
      hpAdded.value -= undo
      damage -= undo
    }
    // poi applico il danno usuale: PF TEMP -> HP
    if (damage > 0 && pfTempStat.value!.valore > 0) {
      const cut = Math.min(damage, pfTempStat.value!.valore)
      pfTempStat.value!.valore -= cut
      pfTempRemoved.value += cut
      damage -= cut
    }
    if (damage > 0 && pfStat.value) {
      pfStat.value.valore = Math.max(0, pfStat.value.valore - damage)
    }
  }
}
</script>

<style scoped>
.hp-container {
  display: flex;
  align-items: center;
  height: 2rem;
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.hp-btn {
  background: transparent;
  border: none;
  font-size: 1.2rem;
  padding: 0 0.6rem;
  cursor: pointer;
  flex-shrink: 0;
  height: 100%;
}

.hp-bar-wrapper {
  flex: 1;
  position: relative;
}

.hp-bar {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.hp-center {
  position: relative;
  z-index: 2;
  font-weight: bold;
  font-size: 0.9rem;
  white-space: nowrap;
}

.delta {
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

.delta:first-of-type {
  left: 0.5rem;
}

.delta:last-of-type {
  right: 0.5rem;
}
</style>
