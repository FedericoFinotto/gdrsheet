<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useAuthStore} from '../stores/auth'
import {getHome} from '../service/AuthService'
import {Home} from '../models/dto/Auth'

const router = useRouter()
const auth = useAuthStore()

const home = ref<Home | null>(null)
const loading = ref(true)
const errorMsg = ref<string | null>(null)

const personaggiProprietario = computed(() =>
    home.value?.personaggi.filter(p => p.permesso === 'PROPRIETARIO') ?? [])
const personaggiVisualizzatore = computed(() =>
    home.value?.personaggi.filter(p => p.permesso === 'VISUALIZZATORE') ?? [])

onMounted(async () => {
  try {
    const res = await getHome()
    home.value = res.data
  } catch (e: any) {
    errorMsg.value = 'Errore nel caricamento'
    console.error('Errore caricamento home:', e)
  } finally {
    loading.value = false
  }
})

function apriScheda(p: {id: number; tipoPersonaggio?: string | null}) {
  // le banche hanno una vista dedicata (solo conti correnti)
  if (p.tipoPersonaggio === 'BANCA') {
    router.push(`/banca/${p.id}`)
    return
  }
  // le navi si aprono direttamente sull'inventario (tab 2)
  const tab = p.tipoPersonaggio === 'NAVE' ? '?tab=2' : ''
  router.push(`/scheda/${p.id}${tab}`)
}

function onLogout() {
  auth.logout()
  router.replace('/login')
}
</script>

<template>
  <div class="home">
    <header class="head">
      <div class="user">
        <h1>{{ home?.utente?.name ?? auth.utente?.name ?? '' }}</h1>
        <span class="muted">@{{ home?.utente?.username ?? auth.utente?.username ?? '' }}</span>
      </div>
      <button class="btn ghost" @click="onLogout">Esci</button>
    </header>

    <div v-if="loading" class="state">Caricamento…</div>
    <div v-else-if="errorMsg" class="state error">{{ errorMsg }}</div>

    <template v-else-if="home">
      <!-- Party -->
      <section v-if="home.parties.length" class="block">
        <h2>I tuoi party</h2>
        <ul class="cards">
          <li v-for="p in home.parties" :key="p.id">
            <button class="card clickable" @click="router.push(`/party/${p.id}`)">
              <span class="nome">{{ p.nome }}</span>
              <span class="pill" :class="p.ruolo === 'MASTER' ? 'master' : 'giocatore'">
                {{ p.ruolo === 'MASTER' ? 'Master' : 'Giocatore' }}
              </span>
            </button>
          </li>
        </ul>
      </section>

      <!-- Personaggi di cui si è proprietari -->
      <section v-if="personaggiProprietario.length" class="block">
        <h2>I tuoi personaggi</h2>
        <ul class="cards">
          <li v-for="p in personaggiProprietario" :key="p.id">
            <button class="card clickable" @click="apriScheda(p)">
              <span class="nome">{{ p.nome }}</span>
              <span v-if="p.partyNome" class="muted">{{ p.partyNome }}</span>
            </button>
          </li>
        </ul>
      </section>

      <!-- Personaggi visualizzabili -->
      <section v-if="personaggiVisualizzatore.length" class="block">
        <h2>Personaggi visualizzabili</h2>
        <ul class="cards">
          <li v-for="p in personaggiVisualizzatore" :key="p.id">
            <button class="card clickable" @click="apriScheda(p)">
              <span class="nome">{{ p.nome }}</span>
              <span v-if="p.partyNome" class="muted">{{ p.partyNome }}</span>
              <span class="pill viewer">Visualizzatore</span>
            </button>
          </li>
        </ul>
      </section>

      <div v-if="!home.parties.length && !home.personaggi.length" class="state">
        Nessun party o personaggio associato al tuo utente.
      </div>
    </template>
  </div>
</template>

<style scoped>
.home {
  width: 100%;
  max-width: 40rem;
  margin: 0 auto;
  padding: 1rem;
  display: grid;
  gap: 1rem;
  align-content: start;
  /* il layout globale ha overflow:hidden, lo scroll va gestito qui */
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior-y: contain;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: .75rem;
}

.user { display: grid; }
.user h1 { margin: 0; font-size: 1.25rem; }
.muted { opacity: .65; font-size: .85rem; }

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
  align-items: center;
  gap: .5rem;
  padding: .7rem .9rem;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  text-align: left;
}

.card .nome { flex: 1; font-weight: 600; }
.card.clickable { cursor: pointer; }
.card.clickable:hover { background: #f9fafb; }

.pill {
  font-size: .75rem;
  padding: .15rem .5rem;
  border-radius: .5rem;
}
.pill.master { background: #fef3c7; color: #92400e; }
.pill.giocatore { background: #dbeafe; color: #1e40af; }
.pill.viewer { background: #f3f4f6; color: #374151; }

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
