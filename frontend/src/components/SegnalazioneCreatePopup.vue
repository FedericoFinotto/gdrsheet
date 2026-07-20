<script setup lang="ts">
import {ref} from 'vue'
import usePopup from '../function/usePopup'
import {creaSegnalazione} from '../service/SegnalazioniService'
import {pendingScreenshot, scartaScreenshotPendente} from '../function/reportScreenshot'
import {segnaVista} from '../function/segnalazioniNotifiche'

const emit = defineEmits<{(e: 'created'): void}>()

const {closePopup} = usePopup()

const titolo = ref('')
const descrizione = ref('')
const salvando = ref(false)
const errore = ref<string | null>(null)

async function salva() {
  const t = titolo.value.trim()
  if (!t || salvando.value) return
  salvando.value = true
  errore.value = null
  try {
    const creata = (await creaSegnalazione(t, descrizione.value.trim(), pendingScreenshot.value)).data
    segnaVista(creata.id, creata.dataModifica)
    scartaScreenshotPendente()
    emit('created')
    closePopup()
  } catch (e) {
    console.error('Errore creazione segnalazione:', e)
    errore.value = 'Errore durante il salvataggio su Taiga'
  } finally {
    salvando.value = false
  }
}
</script>

<template>
  <div class="seg-create">
    <h3>Nuova segnalazione</h3>
    <label class="campo">
      <span>Titolo</span>
      <input v-model="titolo" type="text" placeholder="Titolo della segnalazione…" autofocus/>
    </label>
    <label class="campo">
      <span>Descrizione</span>
      <textarea v-model="descrizione" rows="5" placeholder="Descrivi il problema o la richiesta…"/>
    </label>
    <p v-if="pendingScreenshot" class="screenshot-hint">📎 Screenshot della pagina allegato automaticamente</p>
    <p v-if="errore" class="errore">{{ errore }}</p>
    <div class="azioni">
      <button class="btn ghost" @click="closePopup">Annulla</button>
      <button class="btn primary" :disabled="!titolo.trim() || salvando" @click="salva">
        {{ salvando ? 'Salvataggio…' : 'Salva' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.seg-create { display: flex; flex-direction: column; gap: .75rem; min-width: 18rem; }
.seg-create h3 { margin: 0; }
.campo { display: flex; flex-direction: column; gap: .3rem; font-size: .85rem; color: #374151; }
.campo input, .campo textarea {
  padding: .5rem .6rem; border: 1px solid #d0d5dd; border-radius: .5rem; font: inherit; resize: vertical;
}
.screenshot-hint { margin: 0; font-size: .8rem; color: #6b7280; }
.errore { margin: 0; font-size: .85rem; color: #991b1b; }
.azioni { display: flex; justify-content: flex-end; gap: .5rem; }
.btn { padding: .45rem .8rem; border-radius: .5rem; border: 1px solid #d0d5dd; background: #fff; cursor: pointer; }
.btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.btn.primary:disabled { opacity: .6; cursor: default; }
.btn.ghost { background: #fff; }
</style>
