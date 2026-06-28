<script setup lang="ts">
import {ref} from 'vue'
import {setUtilizziUsati} from '../service/PersonaggioService'
import {useCharacterStore} from '../stores/personaggio'

const props = defineProps<{
  itemId: number
  personaggioId: number
  usati: number
  totale: number
}>()

const saving = ref(false)
const characterStore = useCharacterStore()

async function cambia(delta: number) {
  if (saving.value) return
  const nuovi = Math.max(0, Math.min(props.totale, props.usati + delta))
  if (nuovi === props.usati) return
  saving.value = true
  try {
    await setUtilizziUsati(props.itemId, props.personaggioId, nuovi)
    await characterStore.fetchCharacter(props.personaggioId, true)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="utilizzi" :class="{ esauriti: usati >= totale }">
    <button class="u-btn" :disabled="saving || usati <= 0" @click.stop="cambia(-1)">−</button>
    <span class="u-count" :title="`${usati} usati su ${totale}`">{{ usati }}/{{ totale }}</span>
    <button class="u-btn" :disabled="saving || usati >= totale" @click.stop="cambia(+1)">+</button>
  </div>
</template>

<style scoped>
.utilizzi {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  border-radius: .4rem;
  background: #e0f2fe;
  padding: 1px 3px;
  font-size: .72rem;
  font-weight: 700;
}
.utilizzi.esauriti {
  background: #fee2e2;
  color: #991b1b;
}
.u-btn {
  width: 1.25rem;
  height: 1.25rem;
  border: none;
  border-radius: .25rem;
  background: transparent;
  cursor: pointer;
  font-size: .85rem;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  color: inherit;
}
.u-btn:disabled { opacity: .35; cursor: default; }
.u-btn:not(:disabled):hover { background: rgba(0,0,0,.1); }
.u-count {
  min-width: 2.5rem;
  text-align: center;
}
</style>
