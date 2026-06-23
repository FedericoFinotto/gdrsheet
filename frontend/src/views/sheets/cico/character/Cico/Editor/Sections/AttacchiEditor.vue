<script setup lang="ts">
import {AttaccoRow} from '../../../../../../../models/dto/UpdateItemRequest'
import {VARIABILI_FORMULA} from '../../../../../../../function/labelCatalog'
import InfoHelpPopup from '../../../../../../../components/InfoHelpPopup.vue'
import usePopup from '../../../../../../../function/usePopup'

const props = defineProps<{
  modelValue: AttaccoRow[]
  disabled?: boolean
}>()
const emit = defineEmits<{ (e: 'update:modelValue', v: AttaccoRow[]): void }>()

const {openPopup} = usePopup()

function apriInfoVariabili() {
  openPopup(InfoHelpPopup, {
    titolo: 'Variabili utilizzabili',
    nota: 'Usa le variabili nelle formule di TPC e TPD, es.: BAB+FOR oppure 1d8+FOR.',
    voci: VARIABILI_FORMULA,
  }, {closable: true, autoClose: 0})
}

function update(idx: number, patch: Partial<AttaccoRow>) {
  const next = props.modelValue.map((r, i) => i === idx ? {...r, ...patch} : r)
  emit('update:modelValue', next)
}

function add() {
  emit('update:modelValue', [...props.modelValue, {nome: '', tpc: '', tpd: '', tipoDanni: ''}])
}

function remove(idx: number) {
  emit('update:modelValue', props.modelValue.filter((_, i) => i !== idx))
}
</script>

<template>
  <div class="attacchi-editor">
    <div class="atk-toolbar">
      <button type="button" class="btn-info" title="Variabili utilizzabili nelle formule" @click="apriInfoVariabili">ⓘ Variabili</button>
    </div>

    <div v-if="!modelValue.length" class="empty">Nessun attacco.</div>

    <div v-for="(row, i) in modelValue" :key="row.id ?? `new-${i}`" class="atk-card">
      <div class="atk-head">
        <input
            type="text"
            class="nome"
            :value="row.nome"
            :disabled="disabled"
            placeholder="Nome attacco"
            @input="update(i, {nome: ($event.target as HTMLInputElement).value})"
        />
        <span v-if="row.id" class="pill">#{{ row.id }}</span>
        <button type="button" class="btn-del" :disabled="disabled" @click="remove(i)" title="Rimuovi">✕</button>
      </div>
      <div class="atk-fields">
        <label class="field">
          <span class="lbl">Tiro per colpire (TPC)</span>
          <input type="text" :value="row.tpc" :disabled="disabled"
                 placeholder="Formula, es.: BAB+FOR"
                 @input="update(i, {tpc: ($event.target as HTMLInputElement).value})"/>
        </label>
        <label class="field">
          <span class="lbl">Danni (TPD)</span>
          <input type="text" :value="row.tpd" :disabled="disabled"
                 placeholder="Formula, es.: 1d8+FOR"
                 @input="update(i, {tpd: ($event.target as HTMLInputElement).value})"/>
        </label>
        <label class="field">
          <span class="lbl">Tipo danni</span>
          <input type="text" :value="row.tipoDanni" :disabled="disabled"
                 placeholder="Es.: Perforante"
                 @input="update(i, {tipoDanni: ($event.target as HTMLInputElement).value})"/>
        </label>
      </div>
    </div>

    <button type="button" class="btn-add" :disabled="disabled" @click="add">+ Aggiungi attacco</button>
  </div>
</template>

<style scoped>
input, select, textarea { min-width: 0; }
.attacchi-editor { display: grid; gap: .5rem; }
.atk-toolbar { display: flex; justify-content: flex-end; }
.btn-info {
  border: 1px solid #c7d2fe; background: #eef2ff; color: #3730a3;
  border-radius: .5rem; padding: .3rem .6rem; cursor: pointer; font-size: .8rem;
}
.btn-info:hover { background: #e0e7ff; }
.empty { font-size: .85rem; opacity: .6; }

.atk-card { border: 1px solid #e5e7eb; border-radius: .5rem; padding: .5rem; display: grid; gap: .5rem; }
.atk-head { display: grid; grid-template-columns: 1fr auto auto; gap: .4rem; align-items: center; }
.atk-fields { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: .4rem; }
@media (max-width: 900px) { .atk-fields { grid-template-columns: 1fr; } }

.field { display: grid; gap: .25rem; }
.lbl { font-size: .75rem; font-weight: 600; opacity: .85; }
input {
  width: 100%; padding: .45rem .55rem; border: 1px solid #d0d5dd; border-radius: .5rem; background: #fff;
}
.nome { font-weight: 600; }
.pill {
  font-size: .75rem; padding: .1rem .45rem; border-radius: .5rem;
  background: #eef2ff; color: #3730a3;
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
