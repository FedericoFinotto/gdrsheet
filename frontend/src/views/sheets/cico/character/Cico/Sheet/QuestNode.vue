<script setup lang="ts">
import {computed, ref} from 'vue'
import {useRouter} from 'vue-router'
import {Quest} from '../../../../../../models/dto/Quest'
import {toggleQuestCompletata} from '../../../../../../service/QuestService'

const props = defineProps<{
  quest: Quest
  idPersonaggio?: number
}>()
const emit = defineEmits<{ (e: 'changed'): void }>()

const router = useRouter()
const open = ref(false)
const busy = ref(false)

const isLeaf = computed(() => !props.quest.figli.length)
const pct = computed(() => props.quest.totali > 0 ? Math.round(props.quest.completati * 100 / props.quest.totali) : 0)

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
  const q = props.idPersonaggio ? `?personaggio=${props.idPersonaggio}` : ''
  router.push(`/itemeditor/${props.quest.id}${q}`)
}
</script>

<template>
  <div class="quest-node" :class="{completa: pct === 100}">
    <div class="quest-head" @click="open = !open">
      <span v-if="ambitoLabel" class="ambito-chip" :class="'chip-' + quest.ambito?.toLowerCase()">{{ ambitoLabel }}</span>
      <div class="quest-head-main">
        <span class="chev" :class="{open}">▸</span>
        <span class="nome">{{ quest.nome }}</span>
        <span class="progress">
          <span class="bar"><span class="fill" :style="{width: pct + '%'}"/></span>
          <span class="count">{{ quest.completati }}/{{ quest.totali }} ({{ pct }}%)</span>
        </span>
        <button type="button" class="btn-edit" @click.stop="edit" title="Modifica">✎</button>
      </div>
    </div>
    <div v-if="open" class="quest-body">
      <div v-if="quest.descrizione" class="descrizione" v-safe-html="quest.descrizione"></div>
      <label v-if="isLeaf" class="completata-toggle">
        <input type="checkbox" :checked="quest.completata" :disabled="busy" @change="onToggle"/>
        <span>Completata</span>
      </label>
      <div v-if="quest.note.length" class="note">
        <strong>Note</strong>
        <div v-for="(n, i) in quest.note" :key="i" class="nota-item">
          <span v-if="visLabel(n.visibilita)" class="nota-vis">{{ visLabel(n.visibilita) }}</span>
          <div class="nota-html" v-safe-html="n.testo"></div>
        </div>
      </div>
      <div v-if="quest.figli.length" class="figli">
        <QuestNode v-for="f in quest.figli" :key="f.id" :quest="f" :id-personaggio="idPersonaggio" @changed="emit('changed')"/>
      </div>
    </div>
  </div>
</template>

<style scoped>
.quest-node {
  border: 1px solid #e5e7eb; border-radius: .5rem; background: #fff; margin-bottom: .4rem;
}
.quest-node.completa { border-color: #bbf7d0; background: #f0fdf4; }
.quest-head {
  display: flex; flex-direction: column; gap: .3rem;
  padding: .5rem .65rem; cursor: pointer;
}
.quest-head-main {
  display: grid; grid-template-columns: auto 1fr auto auto; align-items: center; gap: .5rem;
}
.ambito-chip {
  align-self: flex-start;
  font-size: .68rem; font-weight: 700; letter-spacing: .02em;
  border-radius: .35rem; padding: .1rem .45rem;
}
.ambito-chip.chip-party { background: #ede9fe; color: #5b21b6; }
.ambito-chip.chip-mondo { background: #dbeafe; color: #1d4ed8; }
.ambito-chip.chip-personaggio { background: #dcfce7; color: #166534; }
.chev { transition: transform .15s ease; flex-shrink: 0; }
.chev.open { transform: rotate(90deg); }
.nome { font-weight: 600; min-width: 0; word-break: break-word; }
.progress { display: flex; align-items: center; gap: .4rem; flex-shrink: 0; }
.bar { width: 4.5rem; height: .45rem; background: #eef2f7; border-radius: 999px; overflow: hidden; }
.fill { display: block; height: 100%; background: #22c55e; }
.count { font-size: .72rem; color: #6b7280; white-space: nowrap; font-variant-numeric: tabular-nums; }
.btn-edit {
  border: 1px solid #bfdbfe; background: #eff6ff; color: #1d4ed8;
  border-radius: .4rem; padding: .2rem .45rem; cursor: pointer; font-size: .8rem;
}
.quest-body { padding: 0 .65rem .65rem 1.9rem; display: grid; gap: .5rem; }
.descrizione { font-size: .85rem; white-space: pre-wrap; }
.completata-toggle { display: inline-flex; align-items: center; gap: .4rem; font-size: .85rem; cursor: pointer; width: fit-content; }
.completata-toggle input { width: auto; }
.note strong { font-size: .78rem; color: #6b7280; text-transform: uppercase; letter-spacing: .03em; }
.nota-item { margin-top: .3rem; }
.nota-vis {
  display: inline-block; margin-bottom: .15rem; font-size: .7rem; font-weight: 700;
  color: #9a3412; background: #fff7ed; border: 1px solid #fed7aa; border-radius: .35rem; padding: .05rem .4rem;
}
.nota-html { margin: .2rem 0; font-size: .85rem; }
.figli { display: grid; gap: 0; margin-top: .2rem; }
</style>
