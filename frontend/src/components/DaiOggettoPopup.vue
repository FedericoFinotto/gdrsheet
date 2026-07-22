<script setup lang="ts">
import {onMounted, ref} from 'vue'
import usePopup from '../function/usePopup'
import {getDestinatariGive, giveItem, Destinatario} from '../service/PartyService'
import {useCharacterStore} from '../stores/personaggio'

const props = defineProps<{
  itemId: number
  personaggioId: number   // possessore attuale dell'item
  itemNome?: string
}>()

const {closePopup} = usePopup()
const characterStore = useCharacterStore()

const loading = ref(true)
const errore = ref<string | null>(null)
const destinatari = ref<Destinatario[]>([])
const busy = ref(false)

onMounted(async () => {
  try {
    destinatari.value = (await getDestinatariGive(props.personaggioId)).data
  } catch (e) {
    console.error('Errore caricamento destinatari:', e)
    errore.value = 'Errore nel caricamento dei possibili destinatari'
  } finally {
    loading.value = false
  }
})

async function dai(dest: Destinatario) {
  if (busy.value) return
  busy.value = true
  errore.value = null
  try {
    await giveItem(props.itemId, props.personaggioId, dest.personaggioId, dest.contenitoreId)
    await characterStore.fetchCharacter(props.personaggioId, true)
    closePopup()
  } catch (e) {
    console.error('Errore spostamento item:', e)
    errore.value = 'Errore nello spostamento'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="dai-popup">
    <h3 class="dai-titolo">Dai a…</h3>
    <p v-if="itemNome" class="dai-sotto">{{ itemNome }}</p>

    <div v-if="loading" class="dai-stato">Caricamento…</div>
    <div v-else-if="errore" class="dai-stato errore">{{ errore }}</div>
    <div v-else-if="!destinatari.length" class="dai-stato">Nessun altro membro nel party.</div>
    <div v-else class="dai-lista">
      <button
          v-for="dest in destinatari"
          :key="`${dest.personaggioId}-${dest.contenitoreId ?? 0}`"
          type="button"
          class="dai-dest"
          :disabled="busy"
          @click="dai(dest)"
      >
        {{ dest.label }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.dai-popup { display: flex; flex-direction: column; gap: .6rem; min-width: 14rem; }
.dai-titolo { margin: 0; font-size: 1.05rem; }
.dai-sotto { margin: -.3rem 0 0; font-size: .85rem; color: #6b7280; }
.dai-stato { padding: .5rem 0; font-size: .85rem; color: #6b7280; }
.dai-stato.errore { color: #991b1b; }
.dai-lista { display: flex; flex-direction: column; gap: .4rem; }
.dai-dest {
  padding: .5rem .75rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  background: #fff;
  font-weight: 600;
  font-size: .9rem;
  text-align: left;
  cursor: pointer;
}
.dai-dest:hover:not(:disabled) { background: #f0fdf4; border-color: #86efac; }
.dai-dest:disabled { opacity: .6; cursor: default; }
</style>
