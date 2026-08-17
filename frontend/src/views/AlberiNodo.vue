<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useMondoStore} from '../stores/mondo'
import {getAlberiNodo} from '../service/PersonaggioService'

const router = useRouter()
const mondoStore = useMondoStore()

const alberi = ref<string[]>([])
const loading = ref(true)
const errorMsg = ref<string | null>(null)

onMounted(async () => {
  await mondoStore.carica()
  try {
    if (mondoStore.corrente != null) alberi.value = (await getAlberiNodo(mondoStore.corrente)).data ?? []
  } catch (e) {
    console.error('Errore caricamento alberi NODO:', e)
    errorMsg.value = 'Errore nel caricamento degli alberi'
  } finally {
    loading.value = false
  }
})

function apri(albero: string) {
  router.push(`/nodi/albero/${encodeURIComponent(albero)}`)
}
</script>

<template>
  <div class="alberi-page">
    <header class="head">
      <button class="btn ghost" @click="router.back()">←</button>
      <div class="title">
        <h1>🌳 Alberi</h1>
        <span v-if="alberi.length" class="muted">{{ alberi.length }} albero{{ alberi.length > 1 ? 'i' : '' }}</span>
      </div>
    </header>

    <div v-if="loading" class="state">Caricamento…</div>
    <div v-else-if="errorMsg" class="state error">{{ errorMsg }}</div>
    <div v-else-if="!alberi.length" class="state">Nessun albero trovato in questo mondo.</div>
    <ul v-else class="rows">
      <li v-for="a in alberi" :key="a">
        <button class="row" @click="apri(a)">
          <span class="nome">{{ a }}</span>
          <span class="arrow">›</span>
        </button>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.alberi-page {
  width: 100%;
  padding: 1rem;
  display: grid;
  gap: .75rem;
  align-content: start;
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior-y: contain;
}
.head { display: flex; align-items: center; gap: .5rem; }
.title { flex: 1; display: grid; min-width: 0; }
.title h1 { margin: 0; font-size: 1.2rem; }
.muted { opacity: .65; font-size: .85rem; }

.rows { list-style: none; margin: 0; padding: 0; display: grid; gap: .4rem; }
.row {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: .5rem;
  padding: .7rem .9rem;
  background: var(--surface-0);
  color: var(--text-strong);
  border: 1px solid var(--hairline);
  border-radius: .6rem;
  cursor: pointer;
  text-align: left;
}
.row:hover { background: var(--surface-hover); }
.row .nome { font-weight: 600; }
.row .arrow { opacity: .5; font-size: 1.1rem; }

.state { padding: .75rem; border: 1px dashed var(--hairline); border-radius: .5rem; }
.state.error { color: var(--danger-text); background: var(--danger-bg); border-color: var(--danger-border); }

.btn {
  padding: .45rem .8rem;
  border-radius: .5rem;
  border: 1px solid var(--hairline);
  background: var(--surface-0);
  cursor: pointer;
}
</style>
