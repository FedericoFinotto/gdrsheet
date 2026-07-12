<script setup lang="ts">
const props = withDefaults(defineProps<{
  modelValue: string[] | undefined
  placeholder?: string
  disabled?: boolean
  textarea?: boolean
}>(), {
  modelValue: () => [],
})
const emit = defineEmits<{ (e: 'update:modelValue', v: string[]): void }>()

function update(idx: number, value: string) {
  const next = [...(props.modelValue ?? [])]
  next[idx] = value
  emit('update:modelValue', next)
}

function add() {
  emit('update:modelValue', [...(props.modelValue ?? []), ''])
}

function remove(idx: number) {
  emit('update:modelValue', (props.modelValue ?? []).filter((_, i) => i !== idx))
}
</script>

<template>
  <div class="multi-value-field">
    <div v-if="!modelValue.length" class="empty">Nessun valore.</div>
    <div v-for="(v, i) in modelValue" :key="i" class="mv-row">
      <textarea v-if="textarea" class="val" :value="v" :disabled="disabled" :placeholder="placeholder" rows="2"
                @input="update(i, ($event.target as HTMLTextAreaElement).value)"/>
      <input v-else type="text" class="val" :value="v" :disabled="disabled" :placeholder="placeholder"
             @input="update(i, ($event.target as HTMLInputElement).value)"/>
      <button type="button" class="btn-del" :disabled="disabled" @click="remove(i)" title="Rimuovi">✕</button>
    </div>
    <button type="button" class="btn-add" :disabled="disabled" @click="add">+ Aggiungi</button>
  </div>
</template>

<style scoped>
.multi-value-field { display: grid; gap: .4rem; }
.empty { font-size: .85rem; opacity: .6; }
.mv-row { display: grid; grid-template-columns: 1fr auto; gap: .4rem; align-items: start; }
.val {
  width: 100%; padding: .45rem .55rem; border: 1px solid #d0d5dd; border-radius: .5rem; background: #fff;
  min-width: 0;
}
textarea.val { resize: vertical; }
.btn-del {
  border: 1px solid #fecaca; background: #fef2f2; color: #991b1b;
  border-radius: .5rem; padding: .35rem .6rem; cursor: pointer; align-self: start;
}
.btn-add {
  justify-self: start; border: 1px dashed #d0d5dd; background: #fff;
  border-radius: .5rem; padding: .4rem .7rem; cursor: pointer;
}
button:disabled { opacity: .6; cursor: default; }
</style>
