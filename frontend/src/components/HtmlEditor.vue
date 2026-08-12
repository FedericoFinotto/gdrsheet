<script setup lang="ts">
import {onBeforeUnmount, onMounted, ref, watch} from 'vue'

const props = withDefaults(defineProps<{
  modelValue?: string
  disabled?: boolean
  rows?: number
}>(), {modelValue: '', disabled: false, rows: 8})

const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const root = ref<HTMLElement | null>(null)
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

/* ------------------------------------------------------------------ selezione
 * Il colore si sceglie con un <input type="color">: cliccandolo il contenteditable
 * perde il focus e con esso la selezione, quindi la teniamo da parte e la
 * rimettiamo prima di applicare il comando.
 */
let ultimoRange: Range | null = null

function selezioneInterna(): Range | null {
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0) return null
  const r = sel.getRangeAt(0)
  return el.value?.contains(r.startContainer) ? r : null
}

function salvaRange() {
  const r = selezioneInterna()
  if (r) ultimoRange = r.cloneRange()
}

function ripristinaRange() {
  if (!ultimoRange || !el.value) return
  const sel = window.getSelection()
  sel?.removeAllRanges()
  sel?.addRange(ultimoRange)
}

/* ------------------------------------------------------------------ stili */
type StileInline = 'bold' | 'italic' | 'underline' | 'strikeThrough'

// true durante un exec() lanciato dalla toolbar: il focus() qui sotto è "nostro" e non deve
// far scattare il reset di onFocus (altrimenti annullerebbe il toggle appena richiesto).
let programmatic = false

function exec(cmd: string, value?: string) {
  if (props.disabled) return
  programmatic = true
  el.value?.focus()
  document.execCommand(cmd, false, value)
  onInput()
  salvaRange()
  programmatic = false
}

/**
 * Colore del testo. styleWithCSS viene acceso solo per questo comando: senza, i browser
 * generano <font color>, che è deprecato; con, esce <span style="color:…">. Viene rispento
 * subito dopo, altrimenti anche grassetto e corsivo diventerebbero <span style> al posto
 * di <b>/<i>, più pesanti da leggere e da riconoscere.
 */
function applicaColore(e: Event) {
  if (props.disabled) return
  const colore = (e.target as HTMLInputElement).value
  ripristinaRange()
  programmatic = true
  el.value?.focus()
  document.execCommand('styleWithCSS', false, 'true')
  document.execCommand('foreColor', false, colore)
  document.execCommand('styleWithCSS', false, 'false')
  onInput()
  salvaRange()
  programmatic = false
}

// Elementi che rappresentano davvero la formattazione, per ciascun comando.
const TAG_STILE: Record<StileInline, string> = {
  bold: 'b,strong',
  italic: 'i,em',
  underline: 'u,ins',
  strikeThrough: 's,strike,del',
}

function elementoAlCaret(): Element | null {
  const r = selezioneInterna()
  if (!r) return null
  let n: Node | null = r.startContainer
  if (n.nodeType === Node.TEXT_NODE) n = n.parentNode
  return n instanceof Element ? n : null
}

/**
 * Lo stile è "vero" se tra il caret e la radice dell'editor c'è un elemento di formattazione
 * (o uno style inline che lo impone).
 *
 * NON si usa getComputedStyle sull'elemento al caret: quello riporta il peso EREDITATO, quindi
 * dentro un <h3> — grassetto per default del browser — darebbe "grassetto" anche senza alcun
 * <b>. Era questa la causa dei grassetti comparsi a caso.
 */
function stileReale(cmd: StileInline): boolean {
  const node = elementoAlCaret()
  if (!node || !el.value) return false
  const tag = node.closest(TAG_STILE[cmd])
  if (tag && el.value.contains(tag)) return true
  // style inline, es. da HTML incollato o da execCommand in modalità CSS
  for (let n: Element | null = node; n && el.value.contains(n); n = n.parentElement) {
    const s = (n as HTMLElement).style
    if (cmd === 'bold' && s.fontWeight && (parseInt(s.fontWeight, 10) || 400) >= 600) return true
    if (cmd === 'italic' && (s.fontStyle === 'italic' || s.fontStyle === 'oblique')) return true
    if (cmd === 'underline' && s.textDecorationLine?.includes('underline')) return true
    if (cmd === 'strikeThrough' && s.textDecorationLine?.includes('line-through')) return true
  }
  return false
}

/**
 * Lo stile arriva dal blocco che contiene il caret (tipicamente un titolo, grassetto di suo).
 * In quel caso non è uno "stile di battitura" da annullare: va lasciato stare, altrimenti si
 * finisce per inserire markup spurio dentro l'intestazione.
 */
function stileDalBlocco(cmd: StileInline): boolean {
  const node = elementoAlCaret()
  const blocco = node?.closest('h1,h2,h3,h4,h5,h6,p,li,blockquote,div')
  if (!blocco || !el.value?.contains(blocco)) return false
  const cs = getComputedStyle(blocco)
  if (cmd === 'bold') return (parseInt(cs.fontWeight, 10) || 400) >= 600
  if (cmd === 'italic') return cs.fontStyle === 'italic' || cs.fontStyle === 'oblique'
  if (cmd === 'underline') return cs.textDecorationLine.includes('underline')
  return cs.textDecorationLine.includes('line-through')
}

const STILI: StileInline[] = ['bold', 'italic', 'underline', 'strikeThrough']

/**
 * Spegne gli stili attivi SOLO come stile di battitura pendente: quelli che il browser tiene a
 * livello di documento e che sopravvivono al cambio di focus, finendo per colorare di grassetto
 * un altro editor. La formattazione vera e quella ereditata dal blocco non vengono toccate.
 */
function resetStiliPendenti() {
  if (props.disabled || programmatic || !el.value) return
  // Su alcuni browser il focus arriva prima che la selezione sia posizionata: in quel caso non
  // facciamo nulla e ci pensa la seconda passata schedulata da onFocus.
  if (!selezioneInterna()) return
  for (const cmd of STILI) {
    if (!document.queryCommandState(cmd)) continue
    if (stileReale(cmd)) continue      // formattazione vera
    if (stileDalBlocco(cmd)) continue  // ereditata dal blocco (es. titolo)
    document.execCommand(cmd, false)   // qui è davvero pendente e spurio
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

/**
 * Uscendo dall'editor rilasciamo la selezione: è alla selezione che il browser aggancia lo stile
 * di battitura pendente, quindi così non se lo porta dietro nell'editor successivo. Se il focus
 * resta dentro al componente (toolbar, selettore colore) non si tocca nulla.
 */
function onBlur(e: FocusEvent) {
  const dest = e.relatedTarget as Node | null
  if (dest && root.value?.contains(dest)) return
  const sel = window.getSelection()
  if (selezioneInterna()) sel?.removeAllRanges()
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

onBeforeUnmount(() => {
  ultimoRange = null
})

// aggiorna il contenuto solo se cambia dall'esterno e l'editor non è in focus
watch(() => props.modelValue, (v) => {
  if (el.value && (v ?? '') !== el.value.innerHTML && document.activeElement !== el.value) {
    el.value.innerHTML = v ?? ''
  }
})
</script>

<template>
  <div class="html-editor" ref="root" :class="{ disabled }">
    <div class="he-toolbar">
      <!-- @mousedown.prevent: senza, il click sul pulsante toglie il focus al contenteditable e
           fa perdere la selezione/posizione del caret, con il risultato che lo stile viene
           applicato in un punto casuale (o resta "appeso" e colpisce l'editor successivo). -->
      <button type="button" :disabled="disabled || sourceMode" title="Grassetto" @mousedown.prevent @click="exec('bold')"><b>B</b></button>
      <button type="button" :disabled="disabled || sourceMode" title="Corsivo" @mousedown.prevent @click="exec('italic')"><i>I</i></button>
      <button type="button" :disabled="disabled || sourceMode" title="Sottolineato" @mousedown.prevent @click="exec('underline')"><u>U</u></button>
      <button type="button" :disabled="disabled || sourceMode" title="Barrato" @mousedown.prevent @click="exec('strikeThrough')"><s>S</s></button>
      <label class="he-color" :class="{ disabled: disabled || sourceMode }" title="Colore del testo">
        <span class="he-color-glifo">A</span>
        <input type="color" :disabled="disabled || sourceMode" @change="applicaColore"/>
      </label>
      <span class="he-sep"/>
      <button type="button" :disabled="disabled || sourceMode" title="Elenco puntato" @mousedown.prevent @click="exec('insertUnorderedList')">•</button>
      <button type="button" :disabled="disabled || sourceMode" title="Elenco numerato" @mousedown.prevent @click="exec('insertOrderedList')">1.</button>
      <span class="he-sep"/>
      <button type="button" :disabled="disabled || sourceMode" title="Titolo" @mousedown.prevent @click="exec('formatBlock', 'H3')">H</button>
      <button type="button" :disabled="disabled || sourceMode" title="Paragrafo" @mousedown.prevent @click="exec('formatBlock', 'P')">¶</button>
      <button type="button" :disabled="disabled || sourceMode" title="Rimuovi formattazione" @mousedown.prevent @click="exec('removeFormat')">⌫</button>
      <span class="he-sep"/>
      <!-- raggruppati in un flex proprio: se il toolbar va a capo per mancanza di spazio, i due
           restano comunque affiancati invece di finire uno sopra l'altro -->
      <div class="he-toolbar-tail">
        <slot name="toolbar-extra"/>
        <button type="button" :disabled="disabled" class="he-source-btn" :class="{active: sourceMode}"
                title="Vedi/modifica il codice HTML" @click="toggleSource">&lt;/&gt;</button>
      </div>
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
        @blur="onBlur"
        @keyup="salvaRange"
        @mouseup="salvaRange"
    />
  </div>
</template>

<style scoped>
.html-editor {
  border: 1px solid var(--hairline);
  border-radius: .5rem;
  background: var(--surface-0);
  overflow: hidden;
}

.html-editor.disabled { opacity: .6; }

.he-toolbar {
  display: flex;
  align-items: center;
  gap: .15rem;
  padding: .3rem .4rem;
  background: var(--btn-bg);
  border-bottom: 1px solid var(--hairline);
  flex-wrap: wrap;
}

.he-toolbar-tail {
  display: flex;
  align-items: center;
  gap: .15rem;
  margin-left: auto;
  flex-shrink: 0;
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
  color: var(--text-strong);
}

.he-toolbar button:hover { background: var(--btn-bg-hover); }
.he-toolbar button:disabled { opacity: .5; cursor: default; }
.he-source-btn { font-family: monospace; font-size: .8rem; }
.he-source-btn.active { background: var(--info-bg); border-color: var(--info-border); color: var(--info-text); }

/* selettore colore: l'input nativo è nascosto sotto la lettera, che fa da pulsante */
.he-color {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.9rem;
  height: 1.9rem;
  border-radius: .35rem;
  cursor: pointer;
}
.he-color:hover { background: var(--btn-bg-hover); }
.he-color.disabled { opacity: .5; cursor: default; }
.he-color-glifo {
  font-weight: 700;
  font-size: .9rem;
  line-height: 1;
  color: var(--text-strong);
  border-bottom: 3px solid currentColor;
  padding-bottom: 1px;
}
.he-color input[type="color"] {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  border: 0;
  padding: 0;
  cursor: pointer;
}
.he-color.disabled input { cursor: default; }

.he-sep {
  width: 1px;
  align-self: stretch;
  margin: .15rem .2rem;
  background: var(--hairline);
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
  color: var(--text-muted);
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
