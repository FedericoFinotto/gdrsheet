<script setup lang="ts">
// Stessa struttura di QuestNode.vue (albero, ambito, note con visibilità propria), ma senza
// completamento né "in carico": il contenuto di un INFO è la sezione Note.
import {computed, ref} from 'vue'
import {useRouter} from 'vue-router'
import {Info} from '../../../../../../models/dto/Info'
import {getInfoDettaglio} from '../../../../../../service/InfoService'
import {coloreIncarico} from '../../../../../../function/coloreIncarico'

const props = withDefaults(defineProps<{
  info: Info
  idPersonaggio?: number
  idParty?: number
  depth?: number     // 0 = INFO radice; passato in incremento ad ogni livello di sotto-info
  ramoIndex?: number // posizione tra i fratelli nel figli del genitore; determina il colore del ramo
}>(), {depth: 0, ramoIndex: 0})
const emit = defineEmits<{ (e: 'changed'): void }>()

// Stessa palette di QuestNode.vue: colore diverso per ciascun sotto-info, assegnato per
// posizione (stabile anche se l'ordine dei fratelli non cambia).
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
const open = ref(false)

// Descrizione e note non arrivano con l'albero: si scaricano alla prima apertura del nodo e
// restano poi in memoria (dettaglioCaricato) per le aperture successive.
const caricandoDettaglio = ref(false)

async function caricaDettaglio() {
  if (props.info.dettaglioCaricato || caricandoDettaglio.value) return
  caricandoDettaglio.value = true
  try {
    const {data} = await getInfoDettaglio(props.info.id)
    props.info.descrizione = data.descrizione
    props.info.note = data.note ?? []
    props.info.dettaglioCaricato = true
  } catch (e) {
    console.error('Errore caricamento dettaglio info:', e)
  } finally {
    caricandoDettaglio.value = false
  }
}

function onHeadClick() {
  open.value = !open.value
  if (open.value) caricaDettaglio()
}

const isLeaf = computed(() => !props.info.figli.length)

// Chip ambito: solo per gli INFO radice (i sotto-info non hanno un proprio ambito).
const ambitoLabel = computed(() => {
  if (props.info.ambito === 'PARTY') return 'Party'
  if (props.info.ambito === 'MONDO') return 'Mondo'
  if (props.info.ambito === 'PERSONAGGIO') return `Personaggio: ${props.info.personaggioNome ?? '?'}`
  return null
})

function edit() {
  const params = new URLSearchParams()
  if (props.idPersonaggio) params.set('personaggio', String(props.idPersonaggio))
  else if (props.idParty) params.set('party', String(props.idParty))
  const q = params.toString() ? `?${params.toString()}` : ''
  router.push(`/itemeditor/${props.info.id}${q}`)
}
</script>

<template>
  <div class="info-node" :class="{'is-nested': depth > 0}" :style="ramoColore ? {'--ramo': ramoColore} : undefined">
    <div class="info-head" :class="{'root-head': depth === 0, 'senza-padding-basso': depth > 0 && open && !isLeaf}"
         @click="onHeadClick">
      <span class="nome" :class="{'nome-ramo': depth > 0 && open}">{{ info.nome }}</span>
      <span v-if="ambitoLabel" class="ambito-chip">{{ ambitoLabel }}</span>
      <button type="button" class="btn-edit" @click.stop="edit" title="Modifica">✎</button>
    </div>
    <div v-if="open" class="info-body" :class="{'con-linea': info.figli.length > 0}">
      <div v-if="caricandoDettaglio" class="caricamento">Caricamento…</div>
      <div v-if="info.descrizione" class="descrizione" v-safe-html="info.descrizione"></div>
      <div v-if="info.note.length" class="note">
        <strong>Note</strong>
        <div v-for="(n, i) in info.note" :key="i" class="nota-item">
          <div v-if="n.chip.length" class="nota-vis-row">
            <span v-for="c in n.chip" :key="c" class="nota-vis" :style="coloreIncarico(c)">{{ c }}</span>
          </div>
          <div class="nota-html" v-safe-html="n.testo"></div>
        </div>
      </div>
      <div v-if="info.figli.length" class="figli">
        <InfoNode v-for="(f, i) in info.figli" :key="f.id" :info="f" :depth="depth + 1" :ramo-index="i"
                  :id-personaggio="idPersonaggio" :id-party="idParty" @changed="emit('changed')"/>
      </div>
    </div>
  </div>
</template>

<style scoped>
.info-node {
  position: relative;
  background: var(--surface-0);
  color: var(--text-strong);
  border: 1px solid var(--hairline);
  border-radius: .7rem;
  margin-bottom: .45rem;
}

.info-head {
  display: flex;
  align-items: center;
  gap: .5rem;
  padding: .5rem .65rem;
  cursor: pointer;
  user-select: none;
  -webkit-user-select: none;
}
.info-head.root-head { padding: .65rem .8rem; }
.info-head.senza-padding-basso { padding-bottom: 0; }

.nome { flex: 1; min-width: 0; font-weight: 600; color: var(--text-strong); word-break: break-word; background: none; border-radius: 0; }
.root-head .nome { font-weight: 700; font-size: 1.02rem; }
.nome.nome-ramo { border-bottom: 2px solid color-mix(in srgb, var(--ramo) 30%, transparent); padding-bottom: 0; }

.ambito-chip {
  flex-shrink: 0;
  font-size: .62rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: .03em;
  border: 1px solid var(--hairline);
  color: var(--text-muted);
  background: transparent;
  border-radius: 999px;
  padding: .1rem .55rem;
}

.btn-edit {
  flex-shrink: 0;
  border: 1px solid var(--info-border);
  background: var(--info-bg);
  color: var(--info-text);
  border-radius: .4rem;
  padding: .25rem .55rem;
  cursor: pointer;
  font-size: .8rem;
}

.info-body { position: relative; padding: 0 .65rem .55rem; display: grid; gap: .45rem; }
/* Quando il nodo ha sotto-elementi, la riga colorata (stesso --ramo dell'underline del titolo,
   vedi .nome-ramo) attraversa TUTTO il corpo — descrizione e note comprese — non solo i figli,
   così risulta un'unica linea continua dal titolo fino all'ultimo sotto-elemento. */
.info-body.con-linea { padding-left: 1.15rem; }
.info-body.con-linea::before {
  content: '';
  position: absolute;
  left: .65rem;
  top: 0;
  bottom: 0;
  width: 2px;
  background: var(--ramo, #bfdbfe);
  opacity: .3;
}
.caricamento { font-size: .8rem; color: var(--text-muted); }
.descrizione { font-size: .88rem; color: var(--text-muted); white-space: pre-wrap; }
.note strong {
  font-size: .75rem;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: .05em;
}
.nota-item { margin-top: .3rem; }
.nota-vis-row { display: flex; flex-wrap: wrap; gap: .25rem; margin-bottom: .15rem; }
/* colore assegnato da coloreIncarico() in base al testo (nome party/utente): stesso testo,
   stesso colore, sempre. */
.nota-vis {
  display: inline-block;
  font-size: .68rem;
  font-weight: 700;
  letter-spacing: .01em;
  border-radius: 999px;
  padding: .1rem .5rem;
}
.nota-html { margin: .2rem 0; font-size: .88rem; color: var(--text-muted); }

.figli {
  margin-top: 0;
  padding-top: .3rem;
  display: grid;
  gap: .4rem;
}
</style>
