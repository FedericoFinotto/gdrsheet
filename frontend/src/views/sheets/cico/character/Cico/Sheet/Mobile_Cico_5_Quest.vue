<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import BottoneAggiungiItem from '../../Shared/BottoneAggiungiItem.vue'
import QuestNode from './QuestNode.vue'
import {getQuestPersonaggio} from '../../../../../../service/QuestService'
import {Quest} from '../../../../../../models/dto/Quest'

const props = defineProps<{ idPersonaggio: number }>()

const quests = ref<Quest[]>([])
const loading = ref(true)
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
  try {
    quests.value = (await getQuestPersonaggio(props.idPersonaggio, soloArchiviate.value)).data ?? []
  } finally {
    loading.value = false
  }
}

function toggleArchiviate() {
  soloArchiviate.value = !soloArchiviate.value
  load()
}

onMounted(load)
</script>

<template>
  <div class="quest-tab">
    <div class="top-bar">
      <BottoneAggiungiItem :id-personaggio="props.idPersonaggio" tipo="QUEST" label="Aggiungi quest"/>
      <button type="button" class="btn-icon" :class="{active: soloArchiviate}"
              :title="soloArchiviate ? 'Mostra le quest attive' : 'Mostra le quest archiviate'"
              @click="toggleArchiviate">
        {{ soloArchiviate ? '🗃️' : '🗄️' }}
      </button>
    </div>
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
    <div class="spazietto"/>
    <div v-if="loading" class="stato">Caricamento…</div>
    <div v-else-if="!quests.length" class="stato">{{ soloArchiviate ? 'Nessuna quest archiviata.' : 'Nessuna quest.' }}</div>
    <div v-else-if="!questFiltrate.length" class="stato">Nessuna quest per i filtri selezionati.</div>
    <QuestNode v-for="q in questFiltrate" :key="q.id" :quest="q" :id-personaggio="props.idPersonaggio" @changed="load"/>
  </div>
</template>

<style scoped>
.quest-tab { display: grid; gap: 0; }
.top-bar { display: flex; align-items: center; justify-content: space-between; gap: .5rem; }
.spazietto { height: .5rem; }
.stato { padding: .6rem; color: var(--text-muted); font-size: .9rem; }
.btn-icon {
  flex-shrink: 0; font-size: 1.05rem; line-height: 1;
  padding: .35rem .5rem; border-radius: .5rem; border: 1px solid var(--hairline);
  background: var(--surface-0); cursor: pointer;
}
.btn-icon.active { border-color: var(--info-border); background: var(--info-bg); }
.filtri-ambito { display: flex; flex-wrap: wrap; gap: .4rem; margin-top: .5rem; }
.chip-filtro {
  padding: .3rem .7rem; border-radius: 999px; border: 1px solid var(--hairline);
  background: var(--surface-0); color: var(--text-muted); font-size: .8rem; font-weight: 600; cursor: pointer;
}
.chip-filtro.active { border-color: var(--info-border); background: var(--info-bg); color: var(--info-text); }
</style>
