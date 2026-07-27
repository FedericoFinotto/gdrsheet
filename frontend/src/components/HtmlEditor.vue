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

// Modalità sorgente: mostra/modifica l'HTML grezzo (per incollare HTML già pronto).
const sourceMode = ref(false)
const sourceText = ref('')

function toggleSource() {
  if (props.disabled) return
  if (sourceMode.value) {
    // torno alla vista normale: applico l'HTML digitato/incollato
    if (el.value) el.value.innerHTML = sourceText.value
    emit('update:modelValue', sourceText.value)
    sourceMode.value = false
  } else {
    sourceText.value = el.value?.innerHTML ?? props.modelValue ?? ''
    sourceMode.value = true
  }
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

type StileInline = 'bold' | 'italic' | 'underline'

// Stato "vero" secondo il DOM nel punto in cui si trova il caret. Serve perché
// queryCommandState() non distingue tra "il caret è davvero dentro un <b>" e "c'è uno stile di
// battitura pendente" — quest'ultimo è memorizzato a livello di documento dai browser e
// sopravvive al cambio di focus, anche verso un ALTRO editor.
function stileRealeAlCaret(cmd: StileInline): boolean {
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0) return false
  let node: Node | null = sel.getRangeAt(0).startContainer
  if (node.nodeType === Node.TEXT_NODE) node = node.parentNode
  if (!(node instanceof Element)) return false
  const cs = getComputedStyle(node)
  if (cmd === 'bold') {
    const w = cs.fontWeight
    return w === 'bold' || w === 'bolder' || (parseInt(w, 10) || 400) >= 600
  }
  if (cmd === 'italic') return cs.fontStyle === 'italic' || cs.fontStyle === 'oblique'
  return cs.textDecorationLine.includes('underline')
}

// Spegne gli stili che risultano attivi SOLO come stile di battitura pendente (ereditato da
// un'interazione precedente, tipicamente il pulsante B di un altro editor), lasciando intatta
// la formattazione reale: se il caret è dentro del testo davvero in grassetto, non lo tocchiamo.
function resetStiliPendenti() {
  if (props.disabled || programmatic || !el.value) return
  const sel = window.getSelection()
  // Su alcuni browser il focus arriva prima che la selezione sia posizionata: in quel caso non
  // facciamo nulla e ci pensa la seconda passata schedulata da onFocus.
  if (!sel || sel.rangeCount === 0 || !el.value.contains(sel.getRangeAt(0).startContainer)) return
  for (const cmd of ['bold', 'italic', 'underline'] as StileInline[]) {
    if (document.queryCommandState(cmd) && !stileRealeAlCaret(cmd)) {
      document.execCommand(cmd, false)
    }
  }
}

function onFocus() {
  // se il focus arriva dal focus() dentro exec() non dobbiamo schedulare nulla: annulleremmo lo
  // stile che l'utente ha appena chiesto con la toolbar
  if (props.disabled || programmatic) return
  // due passate (subito + al frame successivo) per coprire sia il focus da click, dove la
  // selezione è già posizionata, sia quello da tastiera dove arriva dopo
  resetStiliPendenti()
  requestAnimationFrame(resetStiliPendenti)
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
      <!-- @mousedown.prevent: senza, il click sul pulsante toglie il focus al contenteditable e
           fa perdere la selezione/posizione del caret, con il risultato che lo stile viene
           applicato in un punto casuale (o resta "appeso" e colpisce l'editor successivo). -->
      <button type="button" :disabled="disabled || sourceMode" title="Grassetto" @mousedown.prevent @click="exec('bold')"><b>B</b></button>
      <button type="button" :disabled="disabled || sourceMode" title="Corsivo" @mousedown.prevent @click="exec('italic')"><i>I</i></button>
      <button type="button" :disabled="disabled || sourceMode" title="Sottolineato" @mousedown.prevent @click="exec('underline')"><u>U</u></button>
      <span class="he-sep"/>
      <button type="button" :disabled="disabled || sourceMode" title="Elenco puntato" @mousedown.prevent @click="exec('insertUnorderedList')">•</button>
      <button type="button" :disabled="disabled || sourceMode" title="Elenco numerato" @mousedown.prevent @click="exec('insertOrderedList')">1.</button>
      <span class="he-sep"/>
      <button type="button" :disabled="disabled || sourceMode" title="Titolo" @mousedown.prevent @click="exec('formatBlock', 'H3')">H</button>
      <button type="button" :disabled="disabled || sourceMode" title="Paragrafo" @mousedown.prevent @click="exec('formatBlock', 'P')">¶</button>
      <button type="button" :disabled="disabled || sourceMode" title="Rimuovi formattazione" @mousedown.prevent @click="exec('removeFormat')">⌫</button>
      <span class="he-sep"/>
      <button type="button" :disabled="disabled" class="he-source-btn" :class="{active: sourceMode}"
              title="Vedi/modifica il codice HTML" @click="toggleSource">&lt;/&gt;</button>
    </div>
    <textarea
        v-if="sourceMode"
        v-model="sourceText"
        class="he-source"
        :disabled="disabled"
        :style="{ minHeight: (rows * 1.4) + 'rem' }"
        spellcheck="false"
    />
    <div
        v-else
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
.he-source-btn { font-family: monospace; font-size: .8rem; }
.he-source-btn.active { background: #dbeafe; border-color: #93c5fd; color: #1d4ed8; }

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

.he-source {
  display: block;
  width: 100%;
  padding: .5rem .6rem;
  border: 0;
  outline: none;
  font: 12px/1.5 ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  resize: vertical;
  overflow-y: auto;
  max-height: 30rem;
  box-sizing: border-box;
}
.he-source:disabled { opacity: .6; }
</style>
