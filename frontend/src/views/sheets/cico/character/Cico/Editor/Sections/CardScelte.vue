<script setup lang="ts">
import ChildrenEditor from './ChildrenEditor.vue'
import {ChildRef} from '../../../../../../../models/dto/UpdateItemRequest'

export interface SezioneScelta {
  titolo: string
  candidati: ChildRef[]
}

// N sezioni di scelta: ciascuna con un titolo libero e una lista di item candidati (X diversi per
// sezione). Quando un personaggio possiede questo item, sceglie UN candidato per sezione dal suo
// editor (Mobile_DettaglioItem.vue) — qui si definisce solo la struttura, non la scelta.
const props = defineProps<{
  sezioni: SezioneScelta[]  // reattivo del genitore, cresciuto/ridotto per riferimento (push/splice)
  excludeId?: number
  disabled?: boolean
}>()

function addSezione() {
  props.sezioni.push({titolo: '', candidati: []})
}
function removeSezione(i: number) {
  props.sezioni.splice(i, 1)
}
</script>

<template>
  <p class="muted">
    Ogni <strong>sezione</strong> è un gruppo di item tra cui scegliere: quando un personaggio
    possiede questo item, indicherà nel suo editor quale candidato scegliere per ciascuna sezione.
  </p>

  <div v-for="(s, i) in sezioni" :key="i" class="sez-card">
    <div class="sez-head">
      <span class="sez-title">Sezione {{ i + 1 }}</span>
      <button type="button" class="btn-del" :disabled="disabled" @click="removeSezione(i)" title="Rimuovi">✕</button>
    </div>

    <label class="field">
      <span class="lbl">Titolo (opzionale)</span>
      <input v-model.trim="s.titolo" type="text" :disabled="disabled" placeholder="es. Scegli un talento bonus"/>
    </label>

    <label class="field">
      <span class="lbl">Candidati</span>
      <ChildrenEditor v-model="s.candidati" :disabled="disabled" :exclude-id="excludeId" hide-create/>
    </label>
  </div>

  <button type="button" class="btn outline" :disabled="disabled" @click="addSezione">+ Aggiungi sezione</button>
</template>

<style scoped>
.muted { opacity: .7; font-size: .85rem; margin: 0; }
.sez-card { border: 1px solid var(--hairline); border-radius: .5rem; padding: .5rem; display: grid; gap: .5rem; margin-bottom: .4rem; background: var(--btn-bg); }
.sez-head { display: flex; align-items: center; justify-content: space-between; }
.sez-title { font-weight: 700; font-size: .9rem; }
.field { display: grid; gap: .3rem; }
.lbl { font-size: .8rem; font-weight: 600; opacity: .85; }
input[type="text"] { width: 100%; padding: .5rem .6rem; border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0); }
.btn { padding: .5rem .9rem; border-radius: .5rem; border: 1px solid transparent; cursor: pointer; }
.btn.outline { border-color: var(--info-border); background: var(--info-bg); color: var(--info-text); font-weight: 600; }
.btn:disabled { opacity: .6; cursor: default; }
.btn-del { border: 1px solid var(--danger-border); background: var(--danger-bg); color: var(--danger-text); border-radius: .5rem; padding: .25rem .5rem; cursor: pointer; }
</style>
