<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {getParty} from '../service/PartyService'
import {getQuestParty} from '../service/QuestService'
import {PartyDetail} from '../models/dto/Party'
import {Quest} from '../models/dto/Quest'
import QuestNode from './sheets/cico/character/Cico/Sheet/QuestNode.vue'

const route = useRoute()
const router = useRouter()
const partyId = Number(route.params.id)

const party = ref<PartyDetail | null>(null)
const quests = ref<Quest[]>([])
const loading = ref(true)
const errorMsg = ref<string | null>(null)
// false = solo le non archiviate (default all'ingresso nella sezione); true = SOLO le archiviate.
const soloArchiviate = ref(false)

// Filtri per ambito, tutti attivi di default: nascondono le quest radice di quel tipo (le
// sotto-quest seguono comunque la propria radice, non hanno un ambito proprio).
const mostraParty = ref(true)
const mostraMondo = ref(true)
const mostraGiocatori = ref(true)
const questFiltrate = computed(() => quests.value.filter(q => {
  if (q.ambito === 'PARTY') return mostraParty.value
  if (q.ambito === 'MONDO') return mostraMondo.value
  if (q.ambito === 'PERSONAGGIO') return mostraGiocatori.value
  return true
}))

async function load() {
  loading.value = true
  errorMsg.value = null
  try {
    const [pRes, qRes] = await Promise.all([getParty(partyId), getQuestParty(partyId, soloArchiviate.value)])
    party.value = pRes.data
    quests.value = qRes.data ?? []
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 403 ? 'Non fai parte di questo party' : 'Errore nel caricamento'
    console.error('Errore caricamento quest party:', e)
  } finally {
    loading.value = false
  }
}

function toggleArchiviate() {
  soloArchiviate.value = !soloArchiviate.value
  load()
}

function aggiungiQuest() {
  router.push(`/itemcreate/QUEST?party=${partyId}`)
}

onMounted(load)
</script>

<template>
  <section class="party-quests">
    <header class="head">
      <button class="btn ghost" @click="router.back()">←</button>
      <div class="titolo">
        <h1>Quest del party</h1>
        <span v-if="party" class="sub">{{ party.nome }}</span>
      </div>
      <button type="button" class="btn-icon" :class="{active: soloArchiviate}"
              :title="soloArchiviate ? 'Mostra le quest attive' : 'Mostra le quest archiviate'"
              @click="toggleArchiviate">
        {{ soloArchiviate ? '🗃️' : '🗄️' }}
      </button>
    </header>

    <div class="body">
      <button type="button" class="btn-add-item" @click="aggiungiQuest">
        <span class="plus">+</span>
        <span>Aggiungi quest</span>
      </button>

      <div class="filtri-ambito">
        <button type="button" class="chip-filtro" :class="{active: mostraParty}" @click="mostraParty = !mostraParty">
          Party
        </button>
        <button type="button" class="chip-filtro" :class="{active: mostraMondo}" @click="mostraMondo = !mostraMondo">
          Mondo
        </button>
        <button type="button" class="chip-filtro" :class="{active: mostraGiocatori}" @click="mostraGiocatori = !mostraGiocatori">
          Giocatori
        </button>
      </div>

      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
      <div v-else-if="loading" class="stato">Caricamento…</div>
      <div v-else-if="!quests.length" class="stato">{{ soloArchiviate ? 'Nessuna quest archiviata.' : 'Nessuna quest.' }}</div>
      <div v-else-if="!questFiltrate.length" class="stato">Nessuna quest per i filtri selezionati.</div>
      <QuestNode v-for="q in questFiltrate" :key="q.id" :quest="q" :id-party="partyId" @changed="load"/>
    </div>
  </section>
</template>

<style scoped>
.party-quests { display: flex; flex-direction: column; min-height: 100dvh; background: var(--surface-0); }
.head {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: center; gap: .75rem;
  padding: .75rem 1rem; border-bottom: 1px solid var(--hairline); background: inherit;
  box-sizing: border-box;
}
.titolo { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.titolo h1 { margin: 0; font-size: 1.1rem; }
.sub { font-size: .8rem; color: var(--text-muted); }
.btn { padding: .4rem .7rem; border-radius: .5rem; border: 1px solid transparent; cursor: pointer; }
.btn.ghost { border-color: var(--hairline); background: var(--surface-0); }
.btn-icon {
  flex-shrink: 0; font-size: 1.1rem; line-height: 1;
  padding: .4rem .55rem; border-radius: .5rem; border: 1px solid var(--hairline);
  background: var(--surface-0); cursor: pointer;
}
.btn-icon.active { border-color: var(--info-border); background: var(--info-bg); }
.body {
  box-sizing: border-box;
  padding: .75rem 1rem calc(1.5rem + env(safe-area-inset-bottom, 0px)); display: grid; gap: .5rem; align-content: start;
}
.btn-add-item {
  justify-self: start; display: inline-flex; align-items: center; gap: .35rem;
  padding: .4rem .8rem; border: 1px dashed var(--hairline); border-radius: .5rem;
  background: var(--surface-0); color: var(--text-strong); font-weight: 600; font-size: .85rem; cursor: pointer;
}
.btn-add-item:hover { background: var(--surface-hover); }
.plus { font-weight: 800; color: #2563eb; }
.filtri-ambito { display: flex; flex-wrap: wrap; gap: .4rem; }
.chip-filtro {
  padding: .3rem .7rem; border-radius: 999px; border: 1px solid var(--hairline);
  background: var(--surface-0); color: var(--text-muted); font-size: .8rem; font-weight: 600; cursor: pointer;
}
.chip-filtro.active { border-color: var(--info-border); background: var(--info-bg); color: var(--info-text); }
.stato { padding: .6rem; color: var(--text-muted); font-size: .9rem; }
.error {
  margin: 0; padding: .5rem .75rem; border-radius: .5rem;
  color: var(--danger-text); background: var(--danger-bg); border: 1px solid var(--danger-border); font-size: .85rem;
}
</style>
