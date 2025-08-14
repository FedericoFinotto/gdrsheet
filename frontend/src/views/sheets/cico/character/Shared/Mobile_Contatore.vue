<script setup lang="ts">
import {computed, defineProps} from 'vue'
import {useCharacterStore} from '../../../../../stores/personaggio'
import {storeToRefs} from 'pinia'
import {updateContatore} from '../../../../../service/PersonaggioService'

const characterStore = useCharacterStore()
const {cache} = storeToRefs(characterStore)

const props = defineProps<{
  idPersonaggio: number
  idStat: string
}>()

// stat.valore = valore SALVATO (offset negativo: 0..-max)
// mostriamo i rimanenti: mostrato = max + valore
const stat = computed(() =>
    cache.value[props.idPersonaggio].modificatori.contatori.find(s => s.id === props.idStat)
)

const max = computed(() => Number(stat.value?.max ?? 0))

// Rimanenti mostrati
const mostrato = computed(() => {
  const saved = Number(stat.value?.valore ?? 0) // offset (negativo/zero)
  const rem = max.value + saved
  // clamp visivo per sicurezza
  return Math.max(0, Math.min(max.value, rem))
})

function modifica(delta: number) {
  if (!stat.value) return
  const nuovoMostrato = Math.max(0, Math.min(max.value, mostrato.value + delta))
  const nuovoSalvato = nuovoMostrato - max.value // offset da salvare (<=0)
  stat.value.valore = nuovoSalvato
  updateContatore(props.idPersonaggio, props.idStat, String(nuovoSalvato)).then(resp => {
    if (resp) console.log(resp)
  })
}
</script>

<template>
  <div class="stat-box">
    <div class="stat-nome">{{ stat?.nome ?? props.idStat }}</div>

    <div class="stat-controls">
      <button class="button-contatore" @click="modifica(-1)">-</button>
      <div class="stat-bar">
        <span>{{ mostrato }} / {{ max }}</span>
      </div>
      <button class="button-contatore" click="modifica(1)">+</button>
    </div>
  </div>
</template>
