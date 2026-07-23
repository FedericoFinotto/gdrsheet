<script setup lang="ts">
import {computed, defineAsyncComponent, ref} from 'vue'
import {useRouter} from 'vue-router'
import {Quest} from '../../../../../../models/dto/Quest'
import {toggleQuestCompletata} from '../../../../../../service/QuestService'
import usePopup from '../../../../../../function/usePopup'
import {coloreIncarico} from '../../../../../../function/coloreIncarico'

const InCaricoQuickPopup = defineAsyncComponent(() => import('../../../../../../components/InCaricoQuickPopup.vue'))

const props = withDefaults(defineProps<{
  quest: Quest
  idPersonaggio?: number
  idParty?: number
  depth?: number     // 0 = quest radice; passato in incremento ad ogni livello di sotto-quest
  ramoIndex?: number // posizione tra i fratelli nel figli del genitore; determina il colore del ramo
}>(), {depth: 0, ramoIndex: 0})
const emit = defineEmits<{ (e: 'changed'): void }>()

// Colore del "ramo" (linea che collega la sotto-quest al genitore e sottolinea il nome): un
// colore diverso per ciascuna sotto-quest, assegnato per posizione (non per contenuto, a
// differenza di coloreIncarico) così resta stabile anche se l'ordine dei fratelli non cambia.
// Palette curata di 30 colori (non generata via formula): con una rotazione di tonalità pura
// due colori vicini nell'elenco potevano finire troppo simili (es. entrambi sul rosso); una
// lista scelta a mano garantisce che restino ben distinguibili anche fianco a fianco.
const RAMI = [
  '#e11d48', '#0ea5e9', '#16a34a', '#f97316', '#7c3aed', '#ca8a04', '#0891b2', '#db2777',
  '#4d7c0f', '#2563eb', '#b91c1c', '#059669', '#9333ea', '#ea580c', '#0d9488', '#c026d3',
  '#65a30d', '#1d4ed8', '#be123c', '#0284c7', '#a16207', '#7e22ce', '#15803d', '#d946ef',
  '#f43f5e', '#0f766e', '#be185d', '#84cc16', '#6366f1', '#b45309',
]
function coloreRamo(indice: number): string {
  return RAMI[indice % RAMI.length]
}
const ramoColore = computed(() => props.depth > 0 ? coloreRamo(props.ramoIndex) : undefined)

const router = useRouter()
const {openPopup} = usePopup()
const open = ref(false)
const busy = ref(false)

function apriInCarico() {
  azioniVisibili.value = false
  openPopup(
      InCaricoQuickPopup,
      {
        questId: props.quest.id,
        valori: props.quest.inCarico ?? [],
        // L'endpoint ha già risposto quando arriva qui: il salvataggio è garantito, quindi si
        // aggiorna subito la riga con la lista appena salvata invece di aspettare un
        // ricaricamento completo dell'albero (che resta comunque utile per il resto della vista,
        // ma non deve bloccare il feedback immediato su questa quest).
        onSaved: (valori: string[]) => { props.quest.inCarico = valori },
      },
      {closable: true, autoClose: 0}
  )
}

// Tastini "+ In carico" e "Modifica": nascosti di default, appaiono/scompaiono tenendo premuto
// sulla riga (tocco prolungato o click prolungato da desktop). Un tap normale continua ad
// aprire/chiudere la quest come prima; solo se il timer arriva a scadenza (pressione tenuta) si
// considera "hold" e si salta il toggle dell'apertura.
const azioniVisibili = ref(false)
let pressTimer: number | undefined
let holdFired = false

function onPressStart() {
  holdFired = false
  pressTimer = window.setTimeout(() => {
    holdFired = true
    azioniVisibili.value = !azioniVisibili.value
  }, 450)
}
function onPressEnd() {
  if (pressTimer !== undefined) {
    clearTimeout(pressTimer)
    pressTimer = undefined
  }
}
function onHeadClick() {
  if (holdFired) {
    holdFired = false
    return
  }
  open.value = !open.value
}

const isLeaf = computed(() => !props.quest.figli.length)
const pct = computed(() => props.quest.totali > 0 ? Math.round(props.quest.completati * 100 / props.quest.totali) : 0)

// "In carico" propagati verso l'alto: una sotto-quest mostra i propri valori PIÙ quelli di tutte
// le sue sotto-quest (a qualunque profondità), deduplicati — così un genitore lascia intuire,
// anche chiusa, chi è coinvolto più in basso nell'albero senza doverlo aprire fino in fondo. La
// quest principale (radice) mostra invece solo i propri, non quelli aggregati dei discendenti.
// L'editor rapido ("+ In carico") continua a leggere/scrivere solo quest.inCarico proprio: qui è
// solo visualizzazione.
function inCaricoRicorsivo(q: Quest): string[] {
  const visti = new Set<string>(q.inCarico ?? [])
  for (const f of q.figli ?? []) {
    for (const v of inCaricoRicorsivo(f)) visti.add(v)
  }
  return [...visti]
}
const chipsVisibili = computed(() => props.depth === 0 ? (props.quest.inCarico ?? []) : inCaricoRicorsivo(props.quest))

// Chip ambito: solo per le quest radice (le sotto-quest non hanno un proprio ambito).
const ambitoLabel = computed(() => {
  if (props.quest.ambito === 'PARTY') return 'Party'
  if (props.quest.ambito === 'MONDO') return 'Mondo'
  if (props.quest.ambito === 'PERSONAGGIO') return `Personaggio: ${props.quest.personaggioNome ?? '?'}`
  return null
})

const VISIBILITA_LABELS: Record<string, string> = {
  OWNER: 'Visibile solo al proprietario del personaggio',
  MASTER: 'Visibile solo al Master',
}
function visLabel(visibilita: string): string | null {
  return VISIBILITA_LABELS[visibilita] ?? null
}

async function onToggle() {
  if (busy.value) return
  azioniVisibili.value = false
  busy.value = true
  try {
    await toggleQuestCompletata(props.quest.id)
    // Il servizio ha già risposto: il cambio è garantito, quindi si mostra subito il flag
    // invertito, aggiornando anche il conteggio di QUESTA riga (per una foglia totali è sempre 1
    // e completati rispecchia solo completata) così l'anello/il timbro "Fatta" si aggiornano
    // subito. Il ricaricamento completo dell'albero resta comunque utile per propagare il nuovo
    // conteggio ai genitori più in alto.
    props.quest.completata = !props.quest.completata
    props.quest.completati = props.quest.completata ? props.quest.totali : 0
    emit('changed')
  } finally {
    busy.value = false
  }
}

function edit() {
  azioniVisibili.value = false
  const params = new URLSearchParams()
  if (props.idPersonaggio) params.set('personaggio', String(props.idPersonaggio))
  else if (props.idParty) params.set('party', String(props.idParty))
  const q = params.toString() ? `?${params.toString()}` : ''
  router.push(`/itemeditor/${props.quest.id}${q}`)
}
</script>

<template>
  <div class="quest-node" :class="{completa: pct === 100, 'is-nested': depth > 0}"
       :style="ramoColore ? {'--ramo': ramoColore} : undefined">
    <div class="quest-head" :class="{'root-head': depth === 0, 'senza-padding-basso': depth > 0 && open && !isLeaf}"
         @click="onHeadClick"
         @mousedown="onPressStart" @mouseup="onPressEnd" @mouseleave="onPressEnd"
         @touchstart.passive="onPressStart" @touchend="onPressEnd" @touchcancel="onPressEnd">
      <div class="titolo-wrap">
        <div v-if="chipsVisibili.length" class="chip-row">
          <span v-for="(v, i) in chipsVisibili" :key="i" class="incarico-chip" :style="coloreIncarico(v)">{{ v }}</span>
        </div>
        <span class="nome" :class="{'nome-ramo': depth > 0 && open}">{{ quest.nome }}</span>
      </div>
      <span v-if="ambitoLabel" class="ambito-chip">{{ ambitoLabel }}</span>
      <span v-if="pct === 100" class="stamp-fatta">Fatta</span>
      <span v-else-if="!isLeaf" class="count-ring" :style="{ '--pct': pct }">
        <span class="count-ring-fill"></span>
        <span class="count-ring-text">{{ quest.completati }}/{{ quest.totali }}</span>
      </span>
    </div>
    <div v-if="azioniVisibili" class="azioni-flyout" @click.stop>
      <button type="button" class="btn-incarico" @click="apriInCarico">
        <span>+ In carico</span>
      </button>
      <button type="button" class="btn-edit" @click="edit" title="Modifica">✎</button>
      <button v-if="isLeaf" type="button" class="btn-completata" :class="{done: quest.completata}"
              :disabled="busy" @click="onToggle"
              :title="quest.completata ? 'Segna come non completata' : 'Segna come completata'">
        <span v-if="quest.completata">✓</span>
      </button>
    </div>
    <div v-if="open" class="quest-body">
      <div v-if="quest.descrizione" class="descrizione" v-safe-html="quest.descrizione"></div>
      <div v-if="quest.note.length" class="note">
        <strong>Note</strong>
        <div v-for="(n, i) in quest.note" :key="i" class="nota-item">
          <span v-if="visLabel(n.visibilita)" class="nota-vis">{{ visLabel(n.visibilita) }}</span>
          <div class="nota-html" v-safe-html="n.testo"></div>
        </div>
      </div>
      <div v-if="quest.figli.length" class="figli">
        <QuestNode v-for="(f, i) in quest.figli" :key="f.id" :quest="f" :depth="depth + 1" :ramo-index="i"
                   :id-personaggio="idPersonaggio" :id-party="idParty" @changed="emit('changed')"/>
      </div>
    </div>
  </div>
</template>

<style scoped>
.quest-node {
  position: relative;
  background: #fff;
  border: 1px solid #e5eaf0;
  border-radius: .7rem;
  margin-bottom: .45rem;
}
.quest-node.completa {
  background: #eafbf0;
  border-color: #bfe8cc;
}

.quest-head {
  display: flex;
  align-items: center;
  gap: .5rem;
  padding: .5rem .65rem;
  cursor: pointer;
  user-select: none;
  -webkit-user-select: none;
}
.quest-head.root-head { padding: .65rem .8rem; }
/* Quando aperta e con sotto-quest: azzera il padding inferiore così il sottolineato del nome
   si congiunge senza stacco con l'inizio della linea che scende verso i figli. */
.quest-head.senza-padding-basso { padding-bottom: 0; }

/* Tastini rapidi ("+ In carico"/Modifica): nascosti finché non si tiene premuto sulla riga,
   poi appaiono come un piccolo pannello flottante sopra il bordo destro della card. */
.azioni-flyout {
  position: absolute;
  top: -.3rem;
  right: .5rem;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: .35rem;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: .55rem;
  padding: .3rem .4rem;
  box-shadow: 0 4px 14px rgba(15, 23, 42, .14);
}

.titolo-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: .15rem;
}
.chip-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-start;
  gap: .3rem;
}
/* .nome è anche una classe globale non scoped (global.css, background: var(--primary-color) per
   altri usi come box titolo): qui va azzerata esplicitamente, altrimenti quel background filtra
   comunque perché lo scoping non impedisce alle regole di ALTRI file di applicarsi. */
.nome { font-weight: 600; color: #1e293b; word-break: break-word; background: none; border-radius: 0; }
.root-head .nome { font-weight: 700; font-size: 1.02rem; }
/* Il ramo che arriva dal genitore "sottolinea" il nome della sotto-quest, con lo stesso colore
   della sua linea di collegamento (vedi .figli::before). border-radius: 0 sopra è necessario:
   .nome è anche una classe globale (badge "pill", border-radius grande) e un border-bottom su un
   box con angoli molto arrotondati appare come un arco invece che una linea dritta. */
.nome.nome-ramo { border-bottom: 2px solid var(--ramo); padding-bottom: 0; }

.incarico-chip {
  font-size: .66rem;
  font-weight: 700;
  letter-spacing: .01em;
  border-radius: 999px;
  padding: .1rem .5rem;
  flex-shrink: 0;
}
.ambito-chip {
  flex-shrink: 0;
  font-size: .62rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: .03em;
  border: 1px solid #cbd5e1;
  color: #64748b;
  background: transparent;
  border-radius: 999px;
  padding: .1rem .55rem;
}
.stamp-fatta {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  font-size: .68rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: .05em;
  color: #16a34a;
  border: 1.5px solid #16a34a;
  border-radius: 4px;
  padding: .05rem .4rem;
  transform: rotate(-3deg);
}

/* Contatore ad anello: traccia grigia + arco verde proporzionale alla percentuale, numero al
   centro su un disco bianco (per creare l'effetto "ring" senza SVG). */
.count-ring {
  position: relative;
  flex-shrink: 0;
  width: 2.05rem;
  height: 2.05rem;
  border-radius: 50%;
  background: conic-gradient(#22c55e calc(var(--pct) * 1%), #e2e8f0 0);
}
.count-ring-fill {
  position: absolute;
  inset: 3px;
  border-radius: 50%;
  background: #fff;
}
.count-ring-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: .56rem;
  font-weight: 700;
  color: #334155;
  font-variant-numeric: tabular-nums;
}

.quest-body { padding: 0 .65rem .55rem; display: grid; gap: .45rem; }
.btn-incarico {
  display: inline-flex;
  align-items: center;
  gap: .3rem;
  cursor: pointer;
  font-size: .82rem;
  font-weight: 600;
  border: 1px dashed #94a3b8;
  background: transparent;
  color: #475569;
  border-radius: .5rem;
  padding: .35rem .7rem;
}
.btn-incarico:hover { background: #f1f5f9; }
.btn-edit {
  flex-shrink: 0;
  border: 1px solid #93c5fd;
  background: #eff6ff;
  color: #1d4ed8;
  border-radius: .4rem;
  padding: .25rem .55rem;
  cursor: pointer;
  font-size: .8rem;
}
/* Completata: quadratino, spunta visibile solo quando completata — un solo tap per invertire lo
   stato, niente checkbox/etichetta testuale. */
.btn-completata {
  flex-shrink: 0;
  width: 1.9rem;
  height: 1.9rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: .4rem;
  cursor: pointer;
  font-size: .85rem;
  font-weight: 700;
  line-height: 1;
  border: 1px solid #94a3b8;
  background: transparent;
  color: transparent;
}
.btn-completata.done { border-color: #16a34a; background: #16a34a; color: #fff; }
.btn-completata:disabled { opacity: .6; cursor: default; }
.descrizione { font-size: .88rem; color: #334155; white-space: pre-wrap; }
.note strong {
  font-size: .75rem;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: .05em;
}
.nota-item { margin-top: .3rem; }
.nota-vis {
  display: inline-block;
  margin-bottom: .15rem;
  font-size: .68rem;
  font-weight: 700;
  color: #b91c1c;
  background: transparent;
  border: 1px solid #b91c1c;
  border-radius: .35rem;
  padding: .05rem .4rem;
}
.nota-html { margin: .2rem 0; font-size: .88rem; color: #334155; }

/* Linea di connessione ad albero: una verticale continua per ciascuna quest, dal primo
   all'ultimo dei suoi figli — il colore (--ramo) è quello assegnato a QUESTA quest dal suo
   genitore (vedi ramoColore/ramoIndex), quindi ogni sotto-albero ha la propria linea colorata. */
.figli {
  position: relative;
  margin-top: 0;
  padding-top: .3rem;
  padding-left: .32rem;
  display: grid;
  gap: .4rem;
}
/* Allineata sulla stessa X del nome/sottolineato nel proprio quest-head (stesso padding
   sinistro), così la linea sembra proseguire dritta da lì verso il basso. La linea stessa parte
   comunque da top:0 (non dal padding-top qui sopra) per restare congiunta al sottolineato senza
   stacco, anche se il primo figlio ha un po' di respiro sopra di sé. */
.figli::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 2px;
  background: var(--ramo, #bfdbfe);
}
</style>
