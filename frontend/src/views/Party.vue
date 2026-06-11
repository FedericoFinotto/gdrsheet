<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {getParty} from '../service/PartyService'
import {PartyDetail, PersonaggioSoldi} from '../models/dto/Party'
import SoldiView from '../components/SoldiView.vue'

const route = useRoute()
const router = useRouter()

const party = ref<PartyDetail | null>(null)
const loading = ref(true)
const errorMsg = ref<string | null>(null)

onMounted(async () => {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) {
    errorMsg.value = 'Id party non valido'
    loading.value = false
    return
  }
  try {
    const res = await getParty(id)
    party.value = res.data
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 403
        ? 'Non fai parte di questo party'
        : 'Errore nel caricamento del party'
    console.error('Errore caricamento party:', e)
  } finally {
    loading.value = false
  }
})

function apriScheda(id: number) {
  router.push(`/scheda/${id}`)
}

// gruppi: Party (senza TIPO_PERSONAGGIO, proprietari in cima), Barche (NAVE), Stella (STELLA)
const membriParty = computed<PersonaggioSoldi[]>(() =>
    (party.value?.personaggi ?? [])
        .filter(p => !p.tipoPersonaggio)
        .slice()
        .sort((a, b) => {
          if (a.proprietario !== b.proprietario) return a.proprietario ? -1 : 1
          return a.nome.localeCompare(b.nome)
        }))
const membriBarche = computed<PersonaggioSoldi[]>(() =>
    (party.value?.personaggi ?? []).filter(p => p.tipoPersonaggio === 'NAVE'))
const membriStella = computed<PersonaggioSoldi[]>(() =>
    (party.value?.personaggi ?? []).filter(p => p.tipoPersonaggio === 'STELLA'))
const membriAltri = computed<PersonaggioSoldi[]>(() =>
    (party.value?.personaggi ?? []).filter(p =>
        p.tipoPersonaggio && p.tipoPersonaggio !== 'NAVE' && p.tipoPersonaggio !== 'STELLA'))

const GRUPPI = computed(() => [
  {titolo: 'Party', membri: membriParty.value},
  {titolo: 'Barche', membri: membriBarche.value},
  {titolo: 'Stella', membri: membriStella.value},
  {titolo: 'Altro', membri: membriAltri.value},
].filter(g => g.membri.length > 0))
</script>

<template>
  <div class="party-page">
    <header class="head">
      <button class="btn ghost" @click="router.back()">←</button>
      <div class="title">
        <h1>{{ party?.nome ?? 'Party' }}</h1>
        <span v-if="party" class="pill" :class="party.ruolo === 'MASTER' ? 'master' : 'giocatore'">
          {{ party.ruolo === 'MASTER' ? 'Master' : 'Giocatore' }}
        </span>
      </div>
    </header>

    <div v-if="loading" class="state">Caricamento…</div>
    <div v-else-if="errorMsg" class="state error">{{ errorMsg }}</div>

    <template v-else-if="party">
      <!-- Somma di tutto il party -->
      <section class="block somma">
        <h2>Soldi del party</h2>
        <div class="card highlight">
          <SoldiView :soldi="party.somma"/>
        </div>
      </section>

      <!-- Membri raggruppati -->
      <section v-for="g in GRUPPI" :key="g.titolo" class="block">
        <h2>{{ g.titolo }}</h2>
        <ul class="cards">
          <li v-for="p in g.membri" :key="p.id">
            <button class="card clickable" @click="apriScheda(p.id)">
              <span class="nome">
                {{ p.nome }}
                <span v-if="p.proprietario" class="pill mio">Tuo</span>
              </span>
              <SoldiView :soldi="p.soldi" compatto/>
            </button>
          </li>
        </ul>
      </section>
      <div v-if="!party.personaggi.length" class="state">Nessun personaggio nel party.</div>
    </template>
  </div>
</template>

<style scoped>
.party-page {
  max-width: 44rem;
  margin: 0 auto;
  padding: 1rem;
  display: grid;
  gap: 1rem;
  align-content: start;
}

.head {
  display: flex;
  align-items: center;
  gap: .75rem;
}

.title {
  display: flex;
  align-items: center;
  gap: .5rem;
}

.title h1 { margin: 0; font-size: 1.25rem; }

.block { display: grid; gap: .5rem; }
.block h2 { margin: 0; font-size: 1rem; }

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
  text-align: left;
}

.card .nome { flex: 1; font-weight: 600; min-width: 8rem; }
.card.clickable { cursor: pointer; }
.card.clickable:hover { background: #f9fafb; }

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
.pill.master { background: #fef3c7; color: #92400e; }
.pill.giocatore { background: #dbeafe; color: #1e40af; }
.pill.mio { background: #dcfce7; color: #166534; margin-left: .35rem; }

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
