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
  depth?: number   // 0 = quest radice; passato in incremento ad ogni livello di sotto-quest
}>(), {depth: 0})
const emit = defineEmits<{ (e: 'changed'): void }>()

const router = useRouter()
const {openPopup} = usePopup()
const open = ref(false)
const busy = ref(false)

function apriInCarico() {
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

const isLeaf = computed(() => !props.quest.figli.length)
const pct = computed(() => props.quest.totali > 0 ? Math.round(props.quest.completati * 100 / props.quest.totali) : 0)

// Bordo della pergamena radice: leggermente più "ink" quando completata. Le sotto-quest non
// hanno una card propria (niente sfondo/bordo da calcolare): stanno sulla stessa pergamena del
// genitore, si distinguono solo con indentazione/connettori/tipografia (vedi CSS).
const cardStyle = computed(() => {
  if (props.depth > 0) return {}
  return pct.value === 100 ? {boxShadow: 'inset 0 0 0 1px #7a8a53, 0 3px 10px rgba(59,45,24,.18)'} : {}
})

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
  busy.value = true
  try {
    await toggleQuestCompletata(props.quest.id)
    emit('changed')
  } finally {
    busy.value = false
  }
}

function edit() {
  const params = new URLSearchParams()
  if (props.idPersonaggio) params.set('personaggio', String(props.idPersonaggio))
  else if (props.idParty) params.set('party', String(props.idParty))
  const q = params.toString() ? `?${params.toString()}` : ''
  router.push(`/itemeditor/${props.quest.id}${q}`)
}
</script>

<template>
  <div class="quest-node" :class="{completa: pct === 100, 'is-nested': depth > 0}" :style="cardStyle">
    <!-- Quest radice: frontespizio della pergamena, titolo centrato, controlli in una fascia sopra. -->
    <div v-if="depth === 0" class="quest-head root-head" @click="open = !open">
      <div class="root-controls">
        <span class="chev" :class="{open}">▸</span>
        <span class="count">{{ quest.completati }} / {{ quest.totali }}</span>
        <button type="button" class="btn-edit" @click.stop="edit" title="Modifica">✎</button>
      </div>
      <h2 class="scroll-title">{{ quest.nome }}</h2>
      <div v-if="ambitoLabel || (quest.inCarico ?? []).length || pct === 100" class="chip-row root">
        <span v-if="ambitoLabel" class="ambito-chip">{{ ambitoLabel }}</span>
        <span v-for="(v, i) in (quest.inCarico ?? [])" :key="i" class="incarico-chip" :style="coloreIncarico(v)">{{ v }}</span>
        <span v-if="pct === 100" class="stamp-fatta">Fatta</span>
      </div>
    </div>
    <!-- Sotto-quest: riga compatta, stessa pergamena del genitore. -->
    <div v-else class="quest-head" @click="open = !open">
      <span class="chev" :class="{open}">▸</span>
      <div class="titolo-wrap">
        <span v-for="(v, i) in (quest.inCarico ?? [])" :key="i" class="incarico-chip" :style="coloreIncarico(v)">{{ v }}</span>
        <span class="nome">{{ quest.nome }}</span>
        <span v-if="pct === 100" class="stamp-fatta">Fatta</span>
      </div>
      <span class="count">{{ quest.completati }} / {{ quest.totali }}</span>
      <button type="button" class="btn-edit" @click.stop="edit" title="Modifica">✎</button>
    </div>
    <div v-if="open" class="quest-body">
      <div class="action-bar">
        <button type="button" class="btn-incarico" @click="apriInCarico">
          <span>+ In carico</span>
        </button>
        <button v-if="isLeaf" type="button" class="btn-completata" :class="{done: quest.completata}"
                :disabled="busy" @click="onToggle"
                :title="quest.completata ? 'Segna come non completata' : 'Segna come completata'">
          <span v-if="quest.completata">✓</span>
        </button>
      </div>
      <div v-if="quest.descrizione" class="descrizione" v-safe-html="quest.descrizione"></div>
      <div v-if="quest.note.length" class="note">
        <strong>Note</strong>
        <div v-for="(n, i) in quest.note" :key="i" class="nota-item">
          <span v-if="visLabel(n.visibilita)" class="nota-vis">{{ visLabel(n.visibilita) }}</span>
          <div class="nota-html" v-safe-html="n.testo"></div>
        </div>
      </div>
      <div v-if="quest.figli.length" class="figli">
        <QuestNode v-for="f in quest.figli" :key="f.id" :quest="f" :depth="depth + 1"
                   :id-personaggio="idPersonaggio" :id-party="idParty" @changed="emit('changed')"/>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Pergamena: tutto il testo dell'albero (radice e sotto-quest, che vivono nello stesso DOM
   annidato) eredita questo font indipendentemente dai confini di componente. */
.quest-node {
  font-family: Georgia, 'Iowan Old Style', 'Palatino Linotype', serif;
  margin-bottom: 1rem;
}

/* Quest radice: la pergamena vera e propria — bordo strappato, macchie di invecchiamento,
   ombra leggera. Il colore "completata" (JS, cardStyle) inasprisce solo il bordo interno. */
.quest-node:not(.is-nested) {
  background: #ecdfbd;
  background-image:
    radial-gradient(ellipse at 15% 20%, rgba(139, 105, 20, .10), transparent 40%),
    radial-gradient(ellipse at 85% 75%, rgba(139, 105, 20, .12), transparent 45%);
  box-shadow: inset 0 0 0 1px #c9b077, 0 3px 10px rgba(59, 45, 24, .18);
  border-radius: 6px;
  clip-path: polygon(
    0% 1.2%, 3% 0%, 9% .8%, 15% 0%, 22% .6%, 30% 0%, 38% .8%, 46% 0%, 54% .7%, 62% 0%,
    70% .8%, 78% 0%, 86% .7%, 93% 0%, 100% 1.2%,
    100% 98.8%, 96% 100%, 90% 99.2%, 84% 100%, 77% 99.3%, 70% 100%, 62% 99.2%, 54% 100%,
    46% 99.3%, 38% 100%, 30% 99.2%, 22% 100%, 14% 99.3%, 7% 100%, 0% 98.8%
  );
}

/* Sotto-quest (annidate in .figli, a qualunque profondità): niente pergamena propria — stanno
   su quella del genitore, si distinguono con indentazione + connettori tratteggiati (sotto) e
   testo via via più piccolo/tenue, non con un'altra card identica. */
.figli .nome { font-weight: 500; font-size: .95rem; }
.figli .figli .nome { font-weight: 400; font-size: .9rem; color: #6e5636; }

.root-head { display: flex; flex-direction: column; gap: .4rem; padding: 1.2rem 1.3rem .9rem; cursor: pointer; }
.root-controls { display: flex; align-items: center; justify-content: space-between; gap: .5rem; }
.scroll-title {
  margin: .1rem 0; text-align: center; text-wrap: balance;
  color: #3f2d18; font-size: 1.2rem; font-weight: 700; letter-spacing: .03em;
}
.chip-row.root { display: flex; flex-wrap: wrap; justify-content: center; gap: .35rem; }

.quest-head {
  display: flex; align-items: center; gap: .5rem;
  padding: .55rem .8rem; cursor: pointer;
}
/* Titolo e chip: stanno sulla stessa riga finché entrano; se lo spazio non basta vanno a
   capo, coi chip (primi nel DOM) sopra e il nome sotto — sempre dopo il chevron. */
.titolo-wrap {
  flex: 1; min-width: 0;
  display: flex; flex-wrap: wrap; align-items: baseline; gap: .3rem .5rem;
}
.ambito-chip {
  font-family: -apple-system, 'Segoe UI', sans-serif;
  font-size: .68rem; font-weight: 700; letter-spacing: .04em; text-transform: uppercase;
  border: 1px solid #8a6a3f; color: #6e5636; background: transparent;
  border-radius: 999px; padding: .1rem .55rem;
}
.incarico-chip {
  font-family: -apple-system, 'Segoe UI', sans-serif;
  font-size: .68rem; font-weight: 700; letter-spacing: .01em;
  border-radius: 999px; padding: .12rem .55rem;
  box-shadow: inset 0 0 0 1px rgba(59, 45, 24, .18);
}
.stamp-fatta {
  display: inline-flex; align-items: center;
  font-family: -apple-system, 'Segoe UI', sans-serif;
  font-size: .68rem; font-weight: 700; text-transform: uppercase; letter-spacing: .05em;
  color: #4a5c2e; border: 1.5px solid #4a5c2e; border-radius: 4px;
  padding: .05rem .4rem; transform: rotate(-3deg); flex-shrink: 0;
}
.chev { transition: transform .15s ease; flex-shrink: 0; color: #8a6a3f; }
.chev.open { transform: rotate(90deg); }
/* .nome è anche una classe globale non scoped (global.css, background: var(--primary-color)
   per altri usi come box titolo): qui va azzerata esplicitamente, altrimenti quel background
   filtra comunque perché lo scoping non impedisce alle regole di ALTRI file di applicarsi. */
.nome { font-weight: 600; min-width: 0; word-break: break-word; background: none; color: #3f2d18; }
.count {
  font-variant-numeric: tabular-nums;
  font-family: -apple-system, 'Segoe UI', sans-serif;
  font-size: .72rem; color: #8a6a3f; flex-shrink: 0;
  border: 1px solid #b99a5f; border-radius: 999px; padding: .1rem .55rem;
}
.btn-edit {
  flex-shrink: 0;
  font-family: -apple-system, 'Segoe UI', sans-serif;
  border: 1px solid #8a6a3f; background: transparent; color: #6e5636;
  border-radius: .4rem; padding: .2rem .5rem; cursor: pointer; font-size: .8rem;
}
.quest-body { padding: 0 .8rem .7rem .8rem; display: grid; gap: .6rem; }
.action-bar {
  display: flex; align-items: center; justify-content: space-between; gap: .5rem;
  padding-bottom: .5rem; border-bottom: 1px dashed #b99a5f;
}
.btn-incarico {
  font-family: -apple-system, 'Segoe UI', sans-serif;
  display: inline-flex; align-items: center; gap: .3rem; cursor: pointer;
  font-size: .85rem; font-weight: 600;
  border: 1px dashed #8a6a3f; background: transparent; color: #6e5636;
  border-radius: .5rem; padding: .35rem .7rem;
}
.btn-incarico:hover { background: rgba(139, 105, 20, .08); }
/* Completata: quadratino come .btn-edit, spunta visibile solo quando completata — un solo tap
   per invertire lo stato, niente checkbox/etichetta testuale. */
.btn-completata {
  flex-shrink: 0;
  width: 1.9rem; height: 1.9rem;
  display: inline-flex; align-items: center; justify-content: center;
  border-radius: .4rem; cursor: pointer;
  font-size: .85rem; font-weight: 700; line-height: 1;
  border: 1px solid #8a6a3f; background: transparent; color: transparent;
}
.btn-completata.done { border-color: #4a5c2e; background: #4a5c2e; color: #ecdfbd; }
.btn-completata:disabled { opacity: .6; cursor: default; }
.descrizione { font-size: .9rem; color: #4a3a22; white-space: pre-wrap; }
.note strong {
  font-family: -apple-system, 'Segoe UI', sans-serif;
  font-size: .78rem; color: #8a6a3f; text-transform: uppercase; letter-spacing: .06em;
}
.nota-item { margin-top: .3rem; }
.nota-vis {
  display: inline-block; margin-bottom: .15rem;
  font-family: -apple-system, 'Segoe UI', sans-serif;
  font-size: .7rem; font-weight: 700;
  color: #7a2e2e; background: transparent; border: 1px solid #7a2e2e; border-radius: .35rem; padding: .05rem .4rem;
}
.nota-html { margin: .2rem 0; font-size: .9rem; color: #4a3a22; }
/* Linee di connessione ad albero: tratteggiate color inchiostro-seppia, una verticale lungo
   tutto il gruppo di figli più una tacca orizzontale per ciascun figlio. */
.figli {
  position: relative;
  margin-top: .3rem;
  padding-left: 1.3rem;
  display: grid;
  gap: 2px;
}
.figli::before {
  content: '';
  position: absolute;
  left: .55rem;
  top: 0;
  bottom: 1.05rem;
  width: 1px;
  background: repeating-linear-gradient(to bottom, #8a6a3f 0 4px, transparent 4px 7px);
}
.figli > .quest-node { position: relative; }
.figli > .quest-node::before {
  content: '';
  position: absolute;
  left: -.75rem;
  top: 1.05rem;
  width: .65rem;
  height: 1px;
  background: #8a6a3f;
}
/* diramazioni più profonde: stesso schema ma più tenue, per non appiattire visivamente tutti
   i livelli sullo stesso colore */
.figli .figli::before {
  background: repeating-linear-gradient(to bottom, #b99a5f 0 4px, transparent 4px 7px);
}
.figli .figli > .quest-node::before { background: #b99a5f; }
</style>
