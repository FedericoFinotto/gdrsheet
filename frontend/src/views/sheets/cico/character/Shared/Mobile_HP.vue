<script setup lang="ts">
import {computed, defineProps, onBeforeUnmount, ref, watch} from 'vue'
import {storeToRefs} from 'pinia'
import debounce from 'lodash/debounce'
import {updateHP} from "../../../../../service/PersonaggioService";
import {useCharacterStore} from "../../../../../stores/personaggio";

const props = defineProps<{ idPersonaggio: number }>()

// store slice
const {cache} = storeToRefs(useCharacterStore())

// --- stati di sessione (solo UI) ---
const delta = ref(0) // mostra l'ultima modifica cumulata (+/-)

// Helpers
function clamp(min: number, v: number, max: number) {
  return Math.min(max, Math.max(min, v))
}

// --- Stat rows dal cache ---
const pfStat = computed(() =>
    cache.value[props.idPersonaggio]?.modificatori.contatori.find(s => s.id === 'PF')
)
const pfTempStat = computed(() =>
    cache.value[props.idPersonaggio]?.modificatori.contatori.find(s => s.id === 'PFTEMP')
)

// --- Valori numerici di lavoro ---
// pfStat.valore = danni accumulati (negativo o 0)
const damageNeg = computed<number>(() => pfStat.value?.valore ?? 0) // <= 0
const hpMax = computed<number>(() => pfStat.value?.max ?? 0)
const pfTemp = computed<number>(() => pfTempStat.value?.valore ?? 0)

// PF correnti “reali” (non includono i temp)
const hp = computed<number>(() => Math.max(0, hpMax.value + damageNeg.value))

// --- Gradiente barra (HP reali + HP temp + vuoto) ---
const total = computed<number>(() => Math.max(0, hpMax.value + pfTemp.value))
const hpPercent = computed<number>(() => (total.value > 0 ? (hp.value / total.value) * 100 : 0))
const tempPercent = computed<number>(() =>
    total.value > 0 ? (pfTemp.value / total.value) * 100 : 0
)

const barraGradient = computed<string>(() =>
    `linear-gradient(to right,` +
    `#28a745 0%,#28a745 ${hpPercent.value}%,` +
    `#17a2b8 ${hpPercent.value}%,#17a2b8 ${hpPercent.value + tempPercent.value}%,` +
    `#ddd ${hpPercent.value + tempPercent.value}%,#ddd 100%)`
)

// --- Sync backend (invia: danni negativi + PF temp) ---
const debouncedUpdate = debounce(() => {
  updateHP(
      props.idPersonaggio,
      pfStat.value?.valore ?? 0,     // danni (≤ 0)
      pfTempStat.value?.valore ?? 0  // pf temp (≥ 0)
  ).then(() => {
    // reset feedback UI
    delta.value = 0
    // console.log('HP sincronizzati')
  })
}, 3000)

watch([damageNeg, pfTemp], () => debouncedUpdate(), {flush: 'post'})
onBeforeUnmount(() => debouncedUpdate.cancel())

/**
 * Modifica PF secondo il nuovo modello:
 * - amount < 0 => Danno: prima PFTEMP, poi danni (valore più negativo)
 * - amount > 0 => Cura: riduce i danni verso 0 (non tocca PFTEMP)
 */
function modifyHp(amount: number) {
  if (!pfStat.value) return
  delta.value += amount

  // stato attuale
  const curMax = hpMax.value
  const curDamage = pfStat.value.valore ?? 0 // <= 0
  const curTemp = pfTempStat.value?.valore ?? 0

  if (amount < 0) {
    // --- Danno ---
    let dmg = -amount

    // 1) Consuma PF TEMP
    if (pfTempStat.value && curTemp > 0) {
      const cut = Math.min(dmg, curTemp)
      pfTempStat.value.valore = Math.max(0, curTemp - cut)
      dmg -= cut
    }

    // 2) Il resto diventa danno (più negativo)
    if (dmg > 0) {
      // non superare -hpMax per evitare numeri senza senso
      const minDamage = -curMax
      pfStat.value.valore = clamp(minDamage, curDamage - dmg, 0)
    }
  } else if (amount > 0) {
    // --- Cura ---
    // Riduce i danni verso 0 (non ripristina PF TEMP)
    // Max curabile: -curDamage (es. curDamage = -15 => max 15)
    const curable = -curDamage
    if (curable > 0) {
      const heal = Math.min(amount, curable)
      pfStat.value.valore = curDamage + heal // meno negativo, verso 0
    }
  }
}
</script>

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
