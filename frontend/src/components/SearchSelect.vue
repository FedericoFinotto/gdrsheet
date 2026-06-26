<script setup lang="ts">
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue'

type Opt = { value: string | number | null; label: string; disabled?: boolean; hint?: string }

const props = withDefaults(defineProps<{
  modelValue: string | number | null | undefined
  // options come {value,label} oppure stringhe semplici
  options: Array<Opt | string | number>
  placeholder?: string
  disabled?: boolean
  // ordina alfabeticamente per label (default true); metti false per ordini semantici (taglia, livelli…)
  sort?: boolean
}>(), {
  placeholder: 'Seleziona…',
  disabled: false,
  sort: true,
})

const emit = defineEmits<{ (e: 'update:modelValue', v: string | number | null): void }>()

function norm(o: Opt | string | number): Opt {
  if (o !== null && typeof o === 'object') return o as Opt
  return {value: o, label: String(o)}
}

const opzioni = computed<Opt[]>(() => {
  const arr = (props.options ?? []).map(norm)
  if (props.sort) {
    return [...arr].sort((a, b) => String(a.label).localeCompare(String(b.label)))
  }
  return arr
})

const selected = computed(() => opzioni.value.find(o => o.value === props.modelValue) ?? null)
const selectedLabel = computed(() => selected.value?.label ?? '')

const open = ref(false)
const query = ref('')
const root = ref<HTMLElement | null>(null)
const inputEl = ref<HTMLInputElement | null>(null)

const filtrate = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return opzioni.value
  return opzioni.value.filter(o => o.label.toLowerCase().includes(q))
})

function toggle() {
  if (props.disabled) return
  open.value = !open.value
  if (open.value) {
    query.value = ''
    requestAnimationFrame(() => inputEl.value?.focus())
  }
}

function scegli(o: Opt) {
  if (o.disabled) return
  emit('update:modelValue', o.value)
  open.value = false
}

function onDocClick(e: MouseEvent) {
  if (root.value && !root.value.contains(e.target as Node)) open.value = false
}

onMounted(() => document.addEventListener('mousedown', onDocClick))
onBeforeUnmount(() => document.removeEventListener('mousedown', onDocClick))

watch(() => props.disabled, d => { if (d) open.value = false })
</script>

<template>
  <div class="search-select" ref="root" :class="{ disabled }">
    <button type="button" class="ss-control" :disabled="disabled" @click="toggle">
      <span class="ss-value" :class="{ placeholder: !selectedLabel }">{{ selectedLabel || placeholder }}</span>
      <span v-if="selected?.hint" class="ss-hint">{{ selected.hint }}</span>
      <span class="ss-caret" :class="{ open }">▾</span>
    </button>

    <div v-if="open" class="ss-panel">
      <input
          ref="inputEl"
          v-model="query"
          type="text"
          class="ss-search"
          placeholder="Cerca…"
          @keydown.enter.prevent="filtrate.length && scegli(filtrate[0])"
          @keydown.esc.prevent="open = false"
      />
      <ul class="ss-list">
        <li v-if="!filtrate.length" class="ss-empty">Nessun risultato</li>
        <li
            v-for="o in filtrate"
            :key="String(o.value)"
            class="ss-item"
            :class="{ selected: o.value === modelValue, disabled: o.disabled }"
            @click="scegli(o)"
        ><span class="ss-item-label">{{ o.label }}</span><span v-if="o.hint" class="ss-hint">{{ o.hint }}</span></li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.search-select { position: relative; width: 100%; }
.ss-control {
  width: 100%; box-sizing: border-box;
  display: flex; align-items: center; justify-content: space-between; gap: .4rem;
  padding: .5rem .6rem; border: 1px solid #d0d5dd; border-radius: .5rem; background: #fff;
  cursor: pointer; text-align: left; font: inherit; color: inherit;
}
.ss-control:disabled { opacity: .6; cursor: default; }
.ss-value { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ss-value.placeholder { opacity: .55; }
.ss-caret { opacity: .6; transition: transform .15s; flex-shrink: 0; }
.ss-caret.open { transform: rotate(180deg); }

.ss-panel {
  position: absolute; z-index: 50; top: calc(100% + 2px); left: 0; right: 0;
  background: #fff; border: 1px solid #d0d5dd; border-radius: .5rem;
  box-shadow: 0 8px 24px rgba(0,0,0,.12); overflow: hidden;
}
.ss-search {
  width: 100%; box-sizing: border-box; padding: .45rem .55rem;
  border: 0; border-bottom: 1px solid #eef2f7; outline: none; font: inherit;
}
.ss-list { list-style: none; margin: 0; padding: 0; max-height: 16rem; overflow-y: auto; }
.ss-item { padding: .45rem .6rem; cursor: pointer; font-size: .9rem; display: flex; align-items: center; gap: .4rem; }
.ss-item-label { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ss-hint {
  flex: 0 0 auto; font-size: .65rem; font-weight: 700; text-transform: uppercase;
  background: #eef2ff; color: #3730a3; border-radius: .35rem; padding: .05rem .35rem;
}
.ss-item:hover { background: #f3f4f6; }
.ss-item.selected { background: #eef2ff; color: #3730a3; font-weight: 600; }
.ss-item.disabled { opacity: .5; cursor: default; }
.ss-empty { padding: .5rem .6rem; opacity: .6; font-size: .85rem; }
</style>
