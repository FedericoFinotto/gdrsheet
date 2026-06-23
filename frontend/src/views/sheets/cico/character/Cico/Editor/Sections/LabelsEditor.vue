<script setup lang="ts">
import {computed} from 'vue'
import {LabelRow} from '../../../../../../../models/dto/UpdateItemRequest'
import {LABEL_CATALOG} from '../../../../../../../function/labelCatalog'
import InfoHelpPopup from '../../../../../../../components/InfoHelpPopup.vue'
import usePopup from '../../../../../../../function/usePopup'

const props = defineProps<{
  modelValue: LabelRow[]
  suggestedKeys?: string[]
  disabled?: boolean
}>()
const emit = defineEmits<{ (e: 'update:modelValue', v: LabelRow[]): void }>()

const {openPopup} = usePopup()

const datalistId = `labels-keys-${Math.random().toString(36).slice(2, 8)}`

// Suggerimenti per il datalist: se l'editor passa suggestedKeys usa quelle, altrimenti il catalogo.
const opzioni = computed<string[]>(() =>
    props.suggestedKeys?.length ? props.suggestedKeys : LABEL_CATALOG.map(l => l.nome)
)

function update(idx: number, patch: Partial<LabelRow>) {
  const next = props.modelValue.map((r, i) => i === idx ? {...r, ...patch} : r)
  emit('update:modelValue', next)
}

function add() {
  emit('update:modelValue', [...props.modelValue, {label: '', valore: ''}])
}

function remove(idx: number) {
  emit('update:modelValue', props.modelValue.filter((_, i) => i !== idx))
}

function apriInfo() {
  openPopup(InfoHelpPopup, {titolo: 'Label disponibili', voci: LABEL_CATALOG}, {closable: true, autoClose: 0})
}
</script>

<template>
  <div class="labels-editor">
    <div class="labels-toolbar">
      <button type="button" class="btn-info" title="Cosa fa ogni label" @click="apriInfo">ⓘ Info label</button>
    </div>

    <datalist :id="datalistId">
      <option v-for="k in opzioni" :key="k" :value="k"/>
    </datalist>

    <div v-if="!modelValue.length" class="empty">Nessuna label.</div>

    <div v-for="(row, i) in modelValue" :key="i" class="label-row">
      <input
          type="text"
          class="key"
          :value="row.label"
          :list="datalistId"
          :disabled="disabled"
          placeholder="Chiave"
          @input="update(i, {label: ($event.target as HTMLInputElement).value})"
      />
      <input
          type="text"
          class="val"
          :value="row.valore"
          :disabled="disabled"
          placeholder="Valore"
          @input="update(i, {valore: ($event.target as HTMLInputElement).value})"
      />
      <button type="button" class="btn-del" :disabled="disabled" @click="remove(i)" title="Rimuovi">✕</button>
    </div>

    <button type="button" class="btn-add" :disabled="disabled" @click="add">+ Aggiungi label</button>
  </div>
</template>

<style scoped>
input, select, textarea { min-width: 0; }
.labels-editor { display: grid; gap: .4rem; }
.labels-toolbar { display: flex; justify-content: flex-end; }
.btn-info {
  border: 1px solid #c7d2fe; background: #eef2ff; color: #3730a3;
  border-radius: .5rem; padding: .3rem .6rem; cursor: pointer; font-size: .8rem;
}
.btn-info:hover { background: #e0e7ff; }
.empty { font-size: .85rem; opacity: .6; }
.label-row { display: grid; grid-template-columns: 1fr 2fr auto; gap: .4rem; align-items: center; }
input {
  width: 100%; padding: .45rem .55rem; border: 1px solid #d0d5dd; border-radius: .5rem; background: #fff;
}
.btn-del {
  border: 1px solid #fecaca; background: #fef2f2; color: #991b1b;
  border-radius: .5rem; padding: .35rem .6rem; cursor: pointer;
}
.btn-add {
  justify-self: start; border: 1px dashed #d0d5dd; background: #fff;
  border-radius: .5rem; padding: .4rem .7rem; cursor: pointer;
}
button:disabled { opacity: .6; cursor: default; }
</style>
