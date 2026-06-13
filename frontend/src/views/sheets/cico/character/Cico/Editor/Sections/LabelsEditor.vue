<script setup lang="ts">
import {LabelRow} from '../../../../../../../models/dto/UpdateItemRequest'

const props = defineProps<{
  modelValue: LabelRow[]
  suggestedKeys?: string[]
  disabled?: boolean
}>()
const emit = defineEmits<{ (e: 'update:modelValue', v: LabelRow[]): void }>()

const datalistId = `labels-keys-${Math.random().toString(36).slice(2, 8)}`

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
</script>

<template>
  <div class="labels-editor">
    <datalist :id="datalistId" v-if="suggestedKeys?.length">
      <option v-for="k in suggestedKeys" :key="k" :value="k"/>
    </datalist>

    <div v-if="!modelValue.length" class="empty">Nessuna label.</div>

    <div v-for="(row, i) in modelValue" :key="i" class="label-row">
      <input
          type="text"
          class="key"
          :value="row.label"
          :list="suggestedKeys?.length ? datalistId : undefined"
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
