<script setup lang="ts">
import {onMounted, ref} from 'vue'
import BottoneAggiungiItem from '../../Shared/BottoneAggiungiItem.vue'
import QuestNode from './QuestNode.vue'
import {getQuestPersonaggio} from '../../../../../../service/QuestService'
import {Quest} from '../../../../../../models/dto/Quest'

const props = defineProps<{ idPersonaggio: number }>()

const quests = ref<Quest[]>([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    quests.value = (await getQuestPersonaggio(props.idPersonaggio)).data ?? []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="quest-tab">
    <div class="top-bar">
      <BottoneAggiungiItem :id-personaggio="props.idPersonaggio" tipo="QUEST" label="Aggiungi quest"/>
    </div>
    <div class="spazietto"/>
    <div v-if="loading" class="stato">Caricamento…</div>
    <div v-else-if="!quests.length" class="stato">Nessuna quest.</div>
    <QuestNode v-for="q in quests" :key="q.id" :quest="q" :id-personaggio="props.idPersonaggio" @changed="load"/>
  </div>
</template>

<style scoped>
.quest-tab { display: grid; gap: 0; }
.top-bar { display: flex; align-items: center; }
.spazietto { height: .5rem; }
.stato { padding: .6rem; color: #6b7280; font-size: .9rem; }
</style>
