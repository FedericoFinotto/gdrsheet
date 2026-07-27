<script setup lang="ts">
import {onMounted, ref} from 'vue'
import BottoneAggiungiItem from '../../Shared/BottoneAggiungiItem.vue'
import QuestNode from './QuestNode.vue'
import {getQuestPersonaggio} from '../../../../../../service/QuestService'
import {Quest} from '../../../../../../models/dto/Quest'

const props = defineProps<{ idPersonaggio: number }>()

const quests = ref<Quest[]>([])
const loading = ref(true)
// false = solo le non archiviate (default all'ingresso nella sezione); true = SOLO le archiviate.
const soloArchiviate = ref(false)

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
    <div class="spazietto"/>
    <div v-if="loading" class="stato">Caricamento…</div>
    <div v-else-if="!quests.length" class="stato">{{ soloArchiviate ? 'Nessuna quest archiviata.' : 'Nessuna quest.' }}</div>
    <QuestNode v-for="q in quests" :key="q.id" :quest="q" :id-personaggio="props.idPersonaggio" @changed="load"/>
  </div>
</template>

<style scoped>
.quest-tab { display: grid; gap: 0; }
.top-bar { display: flex; align-items: center; justify-content: space-between; gap: .5rem; }
.spazietto { height: .5rem; }
.stato { padding: .6rem; color: #6b7280; font-size: .9rem; }
.btn-icon {
  flex-shrink: 0; font-size: 1.05rem; line-height: 1;
  padding: .35rem .5rem; border-radius: .5rem; border: 1px solid #d0d5dd;
  background: #fff; cursor: pointer;
}
.btn-icon.active { border-color: #93c5fd; background: #eff6ff; }
</style>
