<script setup lang="ts">
import {onMounted, ref} from 'vue'
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

async function load() {
  loading.value = true
  errorMsg.value = null
  try {
    const [pRes, qRes] = await Promise.all([getParty(partyId), getQuestParty(partyId)])
    party.value = pRes.data
    quests.value = qRes.data ?? []
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 403 ? 'Non fai parte di questo party' : 'Errore nel caricamento'
    console.error('Errore caricamento quest party:', e)
  } finally {
    loading.value = false
  }
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
    </header>

    <div class="body">
      <button type="button" class="btn-add-item" @click="aggiungiQuest">
        <span class="plus">+</span>
        <span>Aggiungi quest</span>
      </button>

      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
      <div v-else-if="loading" class="stato">Caricamento…</div>
      <div v-else-if="!quests.length" class="stato">Nessuna quest.</div>
      <QuestNode v-for="q in quests" :key="q.id" :quest="q" :id-party="partyId" @changed="load"/>
    </div>
  </section>
</template>

<style scoped>
.party-quests { display: flex; flex-direction: column; min-height: 100dvh; background: #fff; }
.head {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: center; gap: .75rem;
  padding: .75rem 1rem; border-bottom: 1px solid #e5e7eb; background: inherit;
  box-sizing: border-box;
}
.titolo { display: flex; flex-direction: column; }
.titolo h1 { margin: 0; font-size: 1.1rem; }
.sub { font-size: .8rem; color: #6b7280; }
.btn { padding: .4rem .7rem; border-radius: .5rem; border: 1px solid transparent; cursor: pointer; }
.btn.ghost { border-color: #d0d5dd; background: #fff; }
.body {
  box-sizing: border-box;
  padding: .75rem 1rem calc(1.5rem + env(safe-area-inset-bottom, 0px)); display: grid; gap: .5rem; align-content: start;
}
.btn-add-item {
  justify-self: start; display: inline-flex; align-items: center; gap: .35rem;
  padding: .4rem .8rem; border: 1px dashed #94a3b8; border-radius: .5rem;
  background: #fff; color: #334155; font-weight: 600; font-size: .85rem; cursor: pointer;
}
.btn-add-item:hover { background: #f8fafc; }
.plus { font-weight: 800; color: #2563eb; }
.stato { padding: .6rem; color: #6b7280; font-size: .9rem; }
.error {
  margin: 0; padding: .5rem .75rem; border-radius: .5rem;
  color: #991b1b; background: #fef2f2; border: 1px solid #fecaca; font-size: .85rem;
}
</style>
