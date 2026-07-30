<script setup lang="ts">
/**
 * Selezione multipla a checkbox su un insieme chiuso di opzioni.
 * Il valore è la stessa lista di stringhe usata da MultiValueField, così la persistenza
 * (una riga label per valore) resta identica: cambia solo il modo di sceglierli.
 */
const props = defineProps<{
  modelValue: string[] | undefined
  options: Array<{ value: string | number | null; label: string }>
  disabled?: boolean
}>()
const emit = defineEmits<{ (e: 'update:modelValue', v: string[]): void }>()

function selezionato(value: string | number | null): boolean {
  return (props.modelValue ?? []).includes(String(value))
}

function toggle(value: string | number | null, on: boolean) {
  const v = String(value)
  const attuali = props.modelValue ?? []
  emit('update:modelValue', on ? [...attuali.filter(x => x !== v), v] : attuali.filter(x => x !== v))
}
</script>

<template>
  <div class="multi-select-field">
    <div v-if="!options.length" class="vuoto">Nessuna opzione disponibile.</div>
    <label v-for="o in options" :key="String(o.value)" class="opzione">
      <input type="checkbox" :checked="selezionato(o.value)" :disabled="disabled"
             @change="toggle(o.value, ($event.target as HTMLInputElement).checked)"/>
      <span>{{ o.label }}</span>
    </label>
  </div>
</template>

<style scoped>
.multi-select-field { display: flex; flex-wrap: wrap; gap: .35rem .75rem; }
.vuoto { font-size: .85rem; opacity: .6; }
.opzione {
  display: inline-flex; align-items: center; gap: .35rem;
  font-size: .85rem; cursor: pointer;
  border: 1px solid #e5e7eb; border-radius: .5rem; padding: .25rem .55rem; background: #fff;
}
.opzione input { width: auto; cursor: pointer; }
.opzione:has(input:checked) { background: #eef2ff; border-color: #c7d2fe; font-weight: 600; }
label:has(input:disabled) { opacity: .6; cursor: default; }
</style>
