<script setup lang="ts">
import {ref} from 'vue'
import usePopup from '../function/usePopup'
import {setQuestInCarico} from '../service/QuestService'

const props = defineProps<{
  questId: number
  valori: string[]
}>()
const emit = defineEmits<{ (e: 'saved', valori: string[]): void }>()

const {closePopup} = usePopup()
const lista = ref<string[]>([...props.valori])
const nuovo = ref('')
const busy = ref(false)

// Una volta che l'endpoint ha risposto, il salvataggio è garantito: si aggiorna subito la
// scheda con la lista appena inviata invece di aspettare un ricaricamento completo dell'albero.
async function persisti() {
  busy.value = true
  try {
    await setQuestInCarico(props.questId, lista.value)
    emit('saved', [...lista.value])
  } catch (e) {
    console.error('Errore salvataggio In carico:', e)
  } finally {
    busy.value = false
  }
}

async function aggiungi() {
  const v = nuovo.value.trim()
  if (!v || busy.value) return
  lista.value.push(v)
  nuovo.value = ''
  await persisti()
}

async function rimuovi(i: number) {
  if (busy.value) return
  lista.value.splice(i, 1)
  await persisti()
}
</script>

<template>
  <div class="incarico-popup">
    <h3 class="titolo">In carico</h3>
    <div v-if="!lista.length" class="stato">Nessuno.</div>
    <div v-for="(v, i) in lista" :key="i" class="riga">
      <span class="val">{{ v }}</span>
      <button type="button" class="btn-del" :disabled="busy" @click="rimuovi(i)" title="Rimuovi">✕</button>
    </div>
    <div class="nuovo-row">
      <input type="text" v-model="nuovo" class="nuovo-input" placeholder="Nome personaggio o party"
             :disabled="busy" @keydown.enter.prevent="aggiungi"/>
      <button type="button" class="btn-add" :disabled="busy || !nuovo.trim()" @click="aggiungi">+</button>
    </div>
  </div>
</template>

<style scoped>
.incarico-popup { display: flex; flex-direction: column; gap: .5rem; min-width: 14rem; }
.titolo { margin: 0; font-size: 1.05rem; }
.stato { font-size: .85rem; color: #6b7280; }
.riga {
  display: flex; align-items: center; justify-content: space-between; gap: .5rem;
  padding: .35rem .1rem; border-bottom: 1px solid #f1f3f5; font-size: .9rem;
}
.riga:last-of-type { border-bottom: 0; }
.btn-del {
  border: 1px solid #fecaca; background: #fef2f2; color: #991b1b;
  border-radius: .4rem; padding: .1rem .45rem; cursor: pointer; font-size: .8rem;
}
.btn-del:disabled { opacity: .6; cursor: default; }
.nuovo-row { display: flex; gap: .4rem; margin-top: .3rem; }
.nuovo-input {
  flex: 1; padding: .45rem .6rem; border: 1px solid #d0d5dd; border-radius: .5rem; font: inherit;
}
.btn-add {
  border: 1px solid #93c5fd; background: #eff6ff; color: #1d4ed8;
  border-radius: .5rem; padding: 0 .8rem; font-weight: 700; cursor: pointer;
}
.btn-add:disabled { opacity: .5; cursor: default; }
</style>
