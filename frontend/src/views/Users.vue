<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useAuthStore} from '../stores/auth'
import {createUser, impersonate, listUsers} from '../service/AuthService'
import {UtenteAdmin} from '../models/dto/Auth'
import SearchSelect from '../components/SearchSelect.vue'

const router = useRouter()
const auth = useAuthStore()

const isAdmin = computed(() => {
  const r = (auth.utente?.ruolo ?? '').toUpperCase()
  return r === 'ADMIN' || r === 'SUPERUSER'
})

const ruoliOpt = computed(() => {
  const o = [{value: 'GIOCATORE', label: 'Giocatore'}, {value: 'MASTER', label: 'Master'}]
  if (isAdmin.value) o.push({value: 'ADMIN', label: 'Admin'})
  return o
})

const utenti = ref<UtenteAdmin[]>([])
const loading = ref(true)
const errorMsg = ref<string | null>(null)

// form creazione
const nuovoUsername = ref('')
const nuovoNome = ref('')
const nuovoRuolo = ref('GIOCATORE')
const busyCrea = ref(false)

async function carica() {
  loading.value = true
  errorMsg.value = null
  try {
    utenti.value = (await listUsers()).data
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 403 ? 'Non autorizzato' : 'Errore nel caricamento'
  } finally {
    loading.value = false
  }
}

onMounted(carica)

async function onCrea() {
  if (!nuovoUsername.value.trim() || busyCrea.value) return
  busyCrea.value = true
  errorMsg.value = null
  try {
    const nome = nuovoNome.value.trim() || nuovoUsername.value.trim()
    await createUser(nuovoUsername.value.trim(), nome, nuovoRuolo.value)
    nuovoUsername.value = ''
    nuovoNome.value = ''
    nuovoRuolo.value = 'GIOCATORE'
    await carica()
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 409 ? 'Username già esistente' : 'Errore nella creazione'
  } finally {
    busyCrea.value = false
  }
}

async function onImpersona(u: UtenteAdmin) {
  if (!isAdmin.value) return
  if (!confirm(`Impersonare ${u.name} (@${u.username})?`)) return
  try {
    const res = await impersonate(u.id)
    auth.applySession(res.data)
    router.replace('/')
    // ricarica per riallineare stato/cache alla nuova sessione
    setTimeout(() => window.location.reload(), 50)
  } catch (e) {
    errorMsg.value = 'Errore nell\'impersonazione'
    console.error(e)
  }
}
</script>

<template>
  <div class="users">
    <header class="head">
      <h1>Gestione utenti</h1>
      <button class="btn ghost" @click="router.push('/')">Home</button>
    </header>

    <!-- creazione -->
    <section class="block">
      <h2>Nuovo utente</h2>
      <div class="crea-form">
        <input v-model="nuovoUsername" type="text" placeholder="Username"/>
        <input v-model="nuovoNome" type="text" placeholder="Nome (opzionale)"/>
        <SearchSelect v-model="nuovoRuolo" :options="ruoliOpt" :sort="false"/>
        <button class="btn primary" :disabled="busyCrea || !nuovoUsername.trim()"
                @click="onCrea">
          {{ busyCrea ? 'Creazione…' : 'Crea' }}
        </button>
      </div>
      <p class="muted">Il nuovo utente accede senza password e la imposta al primo login.</p>
    </section>

    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

    <!-- lista -->
    <section class="block">
      <h2>Utenti</h2>
      <div v-if="loading" class="state">Caricamento…</div>
      <ul v-else class="cards">
        <li v-for="u in utenti" :key="u.id" class="card">
          <div class="info">
            <span class="nome">{{ u.name }}</span>
            <span class="muted">@{{ u.username }}</span>
          </div>
          <span class="pill">{{ u.ruolo }}</span>
          <span v-if="u.mustSetPassword" class="pill warn">senza password</span>
          <button v-if="isAdmin && u.id !== auth.utente?.id" class="btn small"
                  @click="onImpersona(u)">Impersona</button>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.users {
  width: 100%;
  max-width: 40rem;
  margin: 0 auto;
  padding: 1rem;
  display: grid;
  gap: 1rem;
  align-content: start;
  height: 100%;
  min-height: 0;
  overflow-y: auto;
}

.head { display: flex; justify-content: space-between; align-items: center; }
.head h1 { margin: 0; font-size: 1.25rem; }

.block { display: grid; gap: .5rem; }
.block h2 { margin: 0; font-size: 1rem; }

.crea-form { display: grid; grid-template-columns: 1fr 1fr auto auto; gap: .4rem; }
@media (max-width: 640px) { .crea-form { grid-template-columns: 1fr 1fr; } }
.crea-form input, .crea-form select {
  padding: .45rem .6rem; border: 1px solid #d0d5dd; border-radius: .5rem;
}

.muted { opacity: .65; font-size: .85rem; margin: 0; }

.cards { list-style: none; margin: 0; padding: 0; display: grid; gap: .5rem; }
.card {
  display: flex; align-items: center; gap: .5rem;
  padding: .6rem .8rem; background: #fff; border: 1px solid #e5e7eb; border-radius: .6rem;
}
.info { flex: 1; display: grid; }
.info .nome { font-weight: 600; }

.pill { font-size: .72rem; padding: .15rem .5rem; border-radius: .5rem; background: #eef2ff; color: #3730a3; }
.pill.warn { background: #fff7ed; color: #9a3412; }

.state { padding: .6rem; border: 1px dashed #e5e7eb; border-radius: .5rem; }
.error {
  margin: 0; padding: .5rem .7rem; border-radius: .5rem;
  color: #991b1b; background: #fef2f2; border: 1px solid #fecaca; font-size: .85rem;
}

.btn {
  padding: .45rem .8rem; border-radius: .5rem; border: 1px solid #d0d5dd; background: #fff; cursor: pointer;
}
.btn.small { padding: .3rem .6rem; font-size: .85rem; }
.btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.btn:disabled { opacity: .6; cursor: default; }
</style>
