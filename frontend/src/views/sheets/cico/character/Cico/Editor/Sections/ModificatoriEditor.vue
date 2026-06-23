<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {ModificatoreRow} from '../../../../../../../models/dto/UpdateItemRequest'
import {TIPO_MODIFICATORE} from '../../../../../../../models/entity/Modificatore'
import {Stat} from '../../../../../../../models/entity/Stat'
import {getStats} from '../../../../../../../service/PersonaggioService'
import {VARIABILI_FORMULA} from '../../../../../../../function/labelCatalog'
import InfoHelpPopup from '../../../../../../../components/InfoHelpPopup.vue'
import usePopup from '../../../../../../../function/usePopup'

const props = defineProps<{
  modelValue: ModificatoreRow[]
  disabled?: boolean
}>()
const emit = defineEmits<{ (e: 'update:modelValue', v: ModificatoreRow[]): void }>()

const TIPI = Object.values(TIPO_MODIFICATORE)

const {openPopup} = usePopup()

function apriInfoVariabili() {
  openPopup(InfoHelpPopup, {
    titolo: 'Variabili utilizzabili',
    nota: 'Usa le variabili nelle formule, es.: BAB+FOR oppure DIFETTO(LVL/2). Anteporre @ è facoltativo.',
    voci: VARIABILI_FORMULA,
  }, {closable: true, autoClose: 0})
}

const stats = ref<Stat[]>([])
onMounted(async () => {
  try {
    stats.value = await getStats()
  } catch (e) {
    console.error('Errore caricamento stats:', e)
  }
})

function statLabel(id: string): string {
  return stats.value.find(s => s.id === id)?.label ?? id
}

function update(idx: number, patch: Partial<ModificatoreRow>) {
  const next = props.modelValue.map((r, i) => i === idx ? {...r, ...patch} : r)
  emit('update:modelValue', next)
}

function add() {
  emit('update:modelValue', [...props.modelValue, {statId: '', tipo: 'MOD', valore: '', nota: '', sempreAttivo: false}])
}

function remove(idx: number) {
  emit('update:modelValue', props.modelValue.filter((_, i) => i !== idx))
}
</script>

<template>
  <div class="mods-editor">
    <div class="mods-toolbar">
      <button type="button" class="btn-info" title="Variabili utilizzabili nelle formule" @click="apriInfoVariabili">ⓘ Variabili</button>
    </div>

    <div v-if="!modelValue.length" class="empty">Nessun modificatore.</div>

    <div v-for="(row, i) in modelValue" :key="row.id ?? `new-${i}`" class="mod-row">
      <select
          class="stat"
          :value="row.statId"
          :disabled="disabled"
          @change="update(i, {statId: ($event.target as HTMLSelectElement).value})"
      >
        <option value="" disabled>— Stat —</option>
        <!-- mantiene visibile un eventuale id non presente nella lista -->
        <option v-if="row.statId && !stats.some(s => s.id === row.statId)" :value="row.statId">
          {{ row.statId }}
        </option>
        <option v-for="s in stats" :key="s.id" :value="s.id">{{ s.label }}</option>
      </select>
      <select
          :value="row.tipo"
          :disabled="disabled"
          @change="update(i, {tipo: ($event.target as HTMLSelectElement).value as any})"
      >
        <option v-for="t in TIPI" :key="t" :value="t">{{ t }}</option>
      </select>
      <input
          type="text"
          class="val"
          :value="row.valore"
          :disabled="disabled"
          placeholder="Valore / formula"
          @input="update(i, {valore: ($event.target as HTMLInputElement).value})"
      />
      <input
          type="text"
          class="nota"
          :value="row.nota"
          :disabled="disabled"
          placeholder="Nota"
          @input="update(i, {nota: ($event.target as HTMLInputElement).value})"
      />
      <label class="chk" title="Sempre attivo">
        <input
            type="checkbox"
            :checked="!!row.sempreAttivo"
            :disabled="disabled"
            @change="update(i, {sempreAttivo: ($event.target as HTMLInputElement).checked})"
        />
        <span>Sempre</span>
      </label>
      <button type="button" class="btn-del" :disabled="disabled" @click="remove(i)" title="Rimuovi">✕</button>
    </div>

    <button type="button" class="btn-add" :disabled="disabled" @click="add">+ Aggiungi modificatore</button>
  </div>
</template>

<style scoped>
input, select, textarea { min-width: 0; }
.mods-editor { display: grid; gap: .4rem; }
.mods-toolbar { display: flex; justify-content: flex-end; }
.btn-info {
  border: 1px solid #c7d2fe; background: #eef2ff; color: #3730a3;
  border-radius: .5rem; padding: .3rem .6rem; cursor: pointer; font-size: .8rem;
}
.btn-info:hover { background: #e0e7ff; }
.empty { font-size: .85rem; opacity: .6; }
.mod-row {
  display: grid; grid-template-columns: 11rem 8rem 1fr 1fr auto auto; gap: .4rem; align-items: center;
}
@media (max-width: 900px) {
  .mod-row { grid-template-columns: 1fr 1fr; }
}
input[type="text"], select {
  width: 100%; padding: .45rem .55rem; border: 1px solid #d0d5dd; border-radius: .5rem; background: #fff;
}
.chk { display: inline-flex; align-items: center; gap: .3rem; font-size: .8rem; }
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
