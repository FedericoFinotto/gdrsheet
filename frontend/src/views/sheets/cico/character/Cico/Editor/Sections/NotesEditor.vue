<script setup lang="ts">
import HtmlEditor from '../../../../../../../components/HtmlEditor.vue'
import SearchSelect from '../../../../../../../components/SearchSelect.vue'

export interface NotaRow {
  testo: string
  visibilita: string   // '' = tutti, 'OWNER' = solo proprietario del personaggio, 'MASTER' = solo master/admin
}

const props = withDefaults(defineProps<{
  modelValue: NotaRow[] | undefined
  disabled?: boolean
}>(), {
  modelValue: () => [],
})
const emit = defineEmits<{ (e: 'update:modelValue', v: NotaRow[]): void }>()

const VISIBILITA_OPTS = [
  {value: '', label: 'Tutti'},
  {value: 'OWNER', label: 'Proprietario del personaggio'},
  {value: 'MASTER', label: 'Master'},
]

function update(idx: number, patch: Partial<NotaRow>) {
  const next = [...(props.modelValue ?? [])]
  next[idx] = {...next[idx], ...patch}
  emit('update:modelValue', next)
}

function add() {
  emit('update:modelValue', [...(props.modelValue ?? []), {testo: '', visibilita: ''}])
}

function remove(idx: number) {
  emit('update:modelValue', (props.modelValue ?? []).filter((_, i) => i !== idx))
}
</script>

<template>
  <div class="notes-editor">
    <div v-if="!modelValue.length" class="empty">Nessuna nota.</div>
    <div v-for="(n, i) in modelValue" :key="i" class="nota-row">
      <HtmlEditor class="val" :model-value="n.testo" :rows="4" :disabled="disabled"
                  @update:model-value="val => update(i, {testo: val})"/>
      <div class="nota-footer">
        <label class="vis-field">
          <span class="lbl">Visibile a</span>
          <SearchSelect :model-value="n.visibilita" :options="VISIBILITA_OPTS" :disabled="disabled" :sort="false"
                        @update:model-value="val => update(i, {visibilita: String(val)})"/>
        </label>
        <button type="button" class="btn-del" :disabled="disabled" @click="remove(i)" title="Rimuovi nota">✕</button>
      </div>
    </div>
    <button type="button" class="btn-add" :disabled="disabled" @click="add">+ Aggiungi nota</button>
  </div>
</template>

<style scoped>
.notes-editor { display: grid; gap: .6rem; }
.empty { font-size: .85rem; opacity: .6; }
.nota-row {
  display: grid; gap: .4rem; padding: .5rem; border: 1px solid #e5e7eb; border-radius: .5rem; background: #fff;
}
.nota-footer { display: flex; align-items: flex-end; gap: .5rem; }
.vis-field { display: grid; gap: .3rem; flex: 1; min-width: 0; }
.vis-field .lbl { font-size: .75rem; font-weight: 600; opacity: .8; }
.btn-del {
  border: 1px solid #fecaca; background: #fef2f2; color: #991b1b;
  border-radius: .5rem; padding: .4rem .6rem; cursor: pointer;
}
.btn-add {
  justify-self: start; border: 1px dashed #d0d5dd; background: #fff;
  border-radius: .5rem; padding: .4rem .7rem; cursor: pointer;
}
button:disabled { opacity: .6; cursor: default; }
</style>
