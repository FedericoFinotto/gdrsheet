<script setup lang="ts">
import {onMounted, ref, watch} from 'vue'

const props = withDefaults(defineProps<{
  modelValue?: string
  disabled?: boolean
  rows?: number
}>(), {modelValue: '', disabled: false, rows: 8})

const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const el = ref<HTMLDivElement | null>(null)

function onInput() {
  if (el.value) emit('update:modelValue', el.value.innerHTML)
}

// true durante un exec() lanciato dalla toolbar: il focus() qui sotto è "nostro" e non deve
// far scattare il reset di onFocus (altrimenti annullerebbe il toggle appena richiesto dall'utente)
let programmatic = false

function exec(cmd: string, value?: string) {
  if (props.disabled) return
  programmatic = true
  el.value?.focus()
  document.execCommand(cmd, false, value)
  onInput()
  programmatic = false
}

// Alcuni browser (soprattutto mobile) mantengono attivo lo stato "grassetto/corsivo/sottolineato"
// da un'interazione precedente (anche di un altro campo): se l'editor è vuoto quando riceve il
// focus, lo spegniamo esplicitamente così il testo digitato riparte sempre in stile normale.
function onFocus() {
  if (props.disabled || programmatic) return
  if (!el.value || el.value.innerText.trim() !== '') return
  if (document.queryCommandState('bold')) document.execCommand('bold', false)
  if (document.queryCommandState('italic')) document.execCommand('italic', false)
  if (document.queryCommandState('underline')) document.execCommand('underline', false)
}

// incolla come testo semplice per evitare HTML "sporco" da altre fonti
function onPaste(e: ClipboardEvent) {
  e.preventDefault()
  const text = e.clipboardData?.getData('text/plain') ?? ''
  document.execCommand('insertText', false, text)
  onInput()
}

onMounted(() => {
  if (el.value) el.value.innerHTML = props.modelValue ?? ''
})

// aggiorna il contenuto solo se cambia dall'esterno e l'editor non è in focus
watch(() => props.modelValue, (v) => {
  if (el.value && (v ?? '') !== el.value.innerHTML && document.activeElement !== el.value) {
    el.value.innerHTML = v ?? ''
  }
})
</script>

<template>
  <div class="html-editor" :class="{ disabled }">
    <div class="he-toolbar">
      <button type="button" :disabled="disabled" title="Grassetto" @click="exec('bold')"><b>B</b></button>
      <button type="button" :disabled="disabled" title="Corsivo" @click="exec('italic')"><i>I</i></button>
      <button type="button" :disabled="disabled" title="Sottolineato" @click="exec('underline')"><u>U</u></button>
      <span class="he-sep"/>
      <button type="button" :disabled="disabled" title="Elenco puntato" @click="exec('insertUnorderedList')">•</button>
      <button type="button" :disabled="disabled" title="Elenco numerato" @click="exec('insertOrderedList')">1.</button>
      <span class="he-sep"/>
      <button type="button" :disabled="disabled" title="Titolo" @click="exec('formatBlock', 'H3')">H</button>
      <button type="button" :disabled="disabled" title="Paragrafo" @click="exec('formatBlock', 'P')">¶</button>
      <button type="button" :disabled="disabled" title="Rimuovi formattazione" @click="exec('removeFormat')">⌫</button>
    </div>
    <div
        ref="el"
        class="he-content"
        :contenteditable="!disabled"
        :style="{ minHeight: (rows * 1.4) + 'rem' }"
        @input="onInput"
        @paste="onPaste"
        @focus="onFocus"
    />
  </div>
</template>

<style scoped>
.html-editor {
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  background: #fff;
  overflow: hidden;
}

.html-editor.disabled { opacity: .6; }

.he-toolbar {
  display: flex;
  align-items: center;
  gap: .15rem;
  padding: .3rem .4rem;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  flex-wrap: wrap;
}

.he-toolbar button {
  min-width: 1.9rem;
  height: 1.9rem;
  padding: 0 .4rem;
  border: 1px solid transparent;
  border-radius: .35rem;
  background: transparent;
  cursor: pointer;
  font-size: .9rem;
  line-height: 1;
  color: #374151;
}

.he-toolbar button:hover { background: #e5e7eb; }
.he-toolbar button:disabled { opacity: .5; cursor: default; }

.he-sep {
  width: 1px;
  align-self: stretch;
  margin: .15rem .2rem;
  background: #e5e7eb;
}

.he-content {
  padding: .5rem .6rem;
  outline: none;
  font: inherit;
  overflow-y: auto;
  max-height: 30rem;
}

.he-content:empty::before {
  content: 'Descrizione…';
  color: #9ca3af;
}

.he-content :deep(ul),
.he-content :deep(ol) { margin: .3rem 0 .3rem 1.2rem; padding: 0; }

.he-content :deep(h3) { margin: .4rem 0 .2rem; font-size: 1rem; }

.he-content :deep(p) { margin: .3rem 0; }
</style>
