<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {getBancaDetail} from '../service/PartyService'
import {BancaDetail} from '../models/dto/Party'
import SoldiView from '../components/SoldiView.vue'

const route = useRoute()
const router = useRouter()

const banca = ref<BancaDetail | null>(null)
const loading = ref(true)
const errorMsg = ref<string | null>(null)

onMounted(async () => {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) {
    errorMsg.value = 'Id banca non valido'
    loading.value = false
    return
  }
  try {
    const res = await getBancaDetail(id)
    banca.value = res.data
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 404
        ? 'Banca non trovata'
        : 'Errore nel caricamento'
    console.error('Errore caricamento banca:', e)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="banca-page">
    <header class="head">
      <button class="btn ghost" @click="router.back()">←</button>
      <div class="title">
        <h1>🏦 {{ banca?.nome ?? 'Banca' }}</h1>
        <span class="muted">Conti correnti</span>
      </div>
    </header>

    <div v-if="loading" class="state">Caricamento…</div>
    <div v-else-if="errorMsg" class="state error">{{ errorMsg }}</div>

    <template v-else-if="banca">
      <!-- totale banca -->
      <div class="card highlight">
        <SoldiView :soldi="banca.totale"/>
      </div>

      <!-- gruppi per party -->
      <section v-for="g in banca.gruppi" :key="g.partyId ?? 'none'" class="block">
        <header class="gruppo-head">
          <h2>{{ g.partyNome }}</h2>
          <SoldiView :soldi="g.totale" compatto/>
        </header>
        <ul class="cards">
          <li v-for="c in g.conti" :key="c.itemId" class="card">
            <span class="nome">{{ c.intestatarioNome }}</span>
            <span class="pill" :class="c.tipo === 'PARTY' ? 'party' : 'giocatore'">
              {{ c.tipo === 'PARTY' ? 'Party' : 'Giocatore' }}
            </span>
            <SoldiView :soldi="c.soldi" compatto/>
          </li>
        </ul>
      </section>

      <div v-if="!banca.gruppi.length" class="state">Nessun conto aperto in questa banca.</div>
    </template>
  </div>
</template>

<style scoped>
.banca-page {
  width: 100%;
  max-width: 44rem;
  margin: 0 auto;
  padding: 1rem;
  display: grid;
  gap: 1rem;
  align-content: start;
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior-y: contain;
}

.head { display: flex; flex-wrap: wrap; align-items: center; gap: .5rem; }
.title { display: grid; }
.title h1 { margin: 0; font-size: 1.2rem; }
.muted { opacity: .65; font-size: .85rem; }

.block { display: grid; gap: .5rem; }

.gruppo-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: .5rem;
  flex-wrap: wrap;
}

.gruppo-head h2 { margin: 0; font-size: 1rem; }

.cards {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: .5rem;
}

.card {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: .5rem;
  padding: .7rem .9rem;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
}

.card .nome { flex: 1; font-weight: 600; min-width: 8rem; }

.card.highlight {
  background: #fefce8;
  border-color: #fde68a;
  justify-content: center;
  padding: 1rem;
}

.pill {
  font-size: .75rem;
  padding: .15rem .5rem;
  border-radius: .5rem;
}
.pill.party { background: #fef3c7; color: #92400e; }
.pill.giocatore { background: #dbeafe; color: #1e40af; }

.state {
  padding: .75rem;
  border: 1px dashed #e5e7eb;
  border-radius: .5rem;
}
.state.error { color: #991b1b; background: #fef2f2; border-color: #fecaca; }

.btn {
  padding: .45rem .8rem;
  border-radius: .5rem;
  border: 1px solid #d0d5dd;
  background: #fff;
  cursor: pointer;
}
</style>
