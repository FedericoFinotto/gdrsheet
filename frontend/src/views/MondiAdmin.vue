<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {useAuthStore} from '../stores/auth'
import {
  addMasterMondo, aggiornaMondo, creaMondo, creaSistema, getMasterMondo, getMondiAdmin,
  getSistemiAdmin, MasterMondo, removeMasterMondo,
} from '../service/MondoAdminService'
import {MondoOpt} from '../function/useMondoSistema'
import SearchSelect from '../components/SearchSelect.vue'

const router = useRouter()
const auth = useAuthStore()

// Non basta il ruolo admin sull'account: serve la modalità admin attiva (coerente con
// AuthzService.isAdmin lato backend, che altrimenti risponderebbe 403 a ogni chiamata di questa pagina).
const isAdmin = computed(() => {
  const r = (auth.utente?.ruolo ?? '').toUpperCase()
  return (r === 'ADMIN' || r === 'SUPERUSER') && auth.adminMode
})

const mondi = ref<MondoOpt[]>([])
const mondoOptions = computed(() => mondi.value.map(m => ({value: m.id, label: m.descrizione})))
const mondoSelezionato = ref<number | null>(null)

const sistemi = ref<MondoOpt[]>([])
const sistemaOptions = computed(() => sistemi.value.map(s => ({value: s.id, label: s.descrizione})))

const master = ref<MasterMondo[]>([])
const loadingMondi = ref(true)
const loadingMaster = ref(false)
const errorMsg = ref<string | null>(null)

const nuovoUsername = ref('')
const busyAdd = ref(false)

async function caricaMondi() {
  loadingMondi.value = true
  errorMsg.value = null
  try {
    const [rMondi, rSistemi] = await Promise.all([getMondiAdmin(), getSistemiAdmin()])
    mondi.value = rMondi.data ?? []
    sistemi.value = rSistemi.data ?? []
    if (mondoSelezionato.value === null && mondi.value.length) mondoSelezionato.value = mondi.value[0].id
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 403 ? 'Non autorizzato' : 'Errore nel caricamento dei mondi'
  } finally {
    loadingMondi.value = false
  }
}

// mondo selezionato: sistema attuale, per la riassegnazione inline (sincronizzato ogni volta che
// cambia la selezione o la lista mondi si aggiorna, es. dopo il salvataggio)
const sistemaDelMondo = ref<number | null>(null)
watch([mondoSelezionato, mondi], () => {
  sistemaDelMondo.value = mondi.value.find(m => m.id === mondoSelezionato.value)?.sistemaId ?? null
})

const busySistema = ref(false)
async function onSalvaSistema() {
  if (mondoSelezionato.value === null || sistemaDelMondo.value === null || busySistema.value) return
  busySistema.value = true
  errorMsg.value = null
  try {
    await aggiornaMondo(mondoSelezionato.value, {sistemaId: sistemaDelMondo.value})
    await caricaMondi()
  } catch (e) {
    console.error('Errore riassegnazione sistema:', e)
    errorMsg.value = 'Errore nel salvataggio del sistema'
  } finally {
    busySistema.value = false
  }
}

// nuovo sistema
const nuovoSistemaDescrizione = ref('')
const busyCreaSistema = ref(false)
async function onCreaSistema() {
  if (!nuovoSistemaDescrizione.value.trim() || busyCreaSistema.value) return
  busyCreaSistema.value = true
  errorMsg.value = null
  try {
    const res = await creaSistema(nuovoSistemaDescrizione.value.trim())
    nuovoSistemaDescrizione.value = ''
    await caricaMondi()
    // pre-seleziona il sistema appena creato per il form "nuovo mondo" qui sotto
    nuovoMondoSistema.value = res.data.id
  } catch (e) {
    console.error('Errore creazione sistema:', e)
    errorMsg.value = 'Errore nella creazione del sistema'
  } finally {
    busyCreaSistema.value = false
  }
}

// nuovo mondo
const nuovoMondoDescrizione = ref('')
const nuovoMondoSistema = ref<number | null>(null)
const busyCreaMondo = ref(false)
async function onCreaMondo() {
  if (!nuovoMondoDescrizione.value.trim() || nuovoMondoSistema.value === null || busyCreaMondo.value) return
  busyCreaMondo.value = true
  errorMsg.value = null
  try {
    const res = await creaMondo(nuovoMondoDescrizione.value.trim(), nuovoMondoSistema.value)
    nuovoMondoDescrizione.value = ''
    await caricaMondi()
    mondoSelezionato.value = res.data.id
  } catch (e) {
    console.error('Errore creazione mondo:', e)
    errorMsg.value = 'Errore nella creazione del mondo'
  } finally {
    busyCreaMondo.value = false
  }
}

async function caricaMaster() {
  if (mondoSelezionato.value === null) { master.value = []; return }
  loadingMaster.value = true
  errorMsg.value = null
  try {
    master.value = (await getMasterMondo(mondoSelezionato.value)).data ?? []
  } catch (e) {
    console.error('Errore caricamento master mondo:', e)
    errorMsg.value = 'Errore nel caricamento dei master'
  } finally {
    loadingMaster.value = false
  }
}

watch(mondoSelezionato, caricaMaster)

async function onAggiungi() {
  if (!nuovoUsername.value.trim() || mondoSelezionato.value === null || busyAdd.value) return
  busyAdd.value = true
  errorMsg.value = null
  try {
    await addMasterMondo(mondoSelezionato.value, nuovoUsername.value.trim())
    nuovoUsername.value = ''
    await caricaMaster()
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 404 ? 'Utente non trovato'
        : e?.response?.status === 409 ? 'È già master di questo mondo'
        : 'Errore nell\'assegnazione'
  } finally {
    busyAdd.value = false
  }
}

async function onRimuovi(m: MasterMondo) {
  if (mondoSelezionato.value === null) return
  const ok = confirm(`Togliere a ${m.name} (@${m.username}) il permesso master su questo mondo?`)
  if (!ok) return
  try {
    await removeMasterMondo(mondoSelezionato.value, m.utenteId)
    await caricaMaster()
  } catch (e) {
    console.error('Errore rimozione master mondo:', e)
    errorMsg.value = 'Errore nella rimozione'
  }
}

onMounted(async () => {
  if (!isAdmin.value) { router.replace('/'); return }
  await caricaMondi()
  await caricaMaster()
})
</script>

<template>
  <div class="mondi-admin">
    <header class="head">
      <h1>Permessi per mondo</h1>
      <button class="btn ghost" @click="router.push('/')">Home</button>
    </header>

    <p class="muted">
      Il master di un mondo gestisce il compendio (creare/modificare/eliminare item, ricerca profonda,
      stat di default) solo di QUEL mondo — non degli altri. Un admin resta master di ogni mondo, sempre.
    </p>

    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
    <div v-if="loadingMondi" class="state">Caricamento…</div>

    <template v-else>
      <!-- Sistemi: creazione (un sistema esiste solo per essere assegnato a uno o più mondi) -->
      <section class="block">
        <h2>Nuovo sistema</h2>
        <div class="add-form">
          <input v-model="nuovoSistemaDescrizione" type="text" placeholder="Nome sistema (es. D&D 3.5)"
                 @keyup.enter="onCreaSistema"/>
          <button class="btn primary" :disabled="busyCreaSistema || !nuovoSistemaDescrizione.trim()" @click="onCreaSistema">
            {{ busyCreaSistema ? 'Creazione…' : 'Crea sistema' }}
          </button>
        </div>
      </section>

      <!-- Mondi: creazione, sempre con un sistema (obbligatorio a livello di schema) -->
      <section class="block">
        <h2>Nuovo mondo</h2>
        <div class="add-form add-form-mondo">
          <input v-model="nuovoMondoDescrizione" type="text" placeholder="Nome mondo"/>
          <SearchSelect v-model="nuovoMondoSistema" :options="sistemaOptions" placeholder="Sistema…" :sort="false"/>
          <button class="btn primary"
                  :disabled="busyCreaMondo || !nuovoMondoDescrizione.trim() || nuovoMondoSistema === null"
                  @click="onCreaMondo">
            {{ busyCreaMondo ? 'Creazione…' : 'Crea mondo' }}
          </button>
        </div>
      </section>

      <section class="block">
        <h2>Mondo</h2>
        <SearchSelect v-model="mondoSelezionato" :options="mondoOptions" :sort="false"/>
      </section>

      <section v-if="mondoSelezionato !== null" class="block">
        <h2>Sistema di questo mondo</h2>
        <div class="add-form">
          <SearchSelect v-model="sistemaDelMondo" :options="sistemaOptions" :sort="false"/>
          <button class="btn primary" :disabled="busySistema || sistemaDelMondo === null" @click="onSalvaSistema">
            {{ busySistema ? 'Salvataggio…' : 'Salva' }}
          </button>
        </div>
      </section>

      <section v-if="mondoSelezionato !== null" class="block">
        <h2>Aggiungi master</h2>
        <div class="add-form">
          <input v-model="nuovoUsername" type="text" placeholder="Username" @keyup.enter="onAggiungi"/>
          <button class="btn primary" :disabled="busyAdd || !nuovoUsername.trim()" @click="onAggiungi">
            {{ busyAdd ? 'Assegnazione…' : 'Rendi master' }}
          </button>
        </div>
      </section>

      <section v-if="mondoSelezionato !== null" class="block">
        <h2>Master di questo mondo</h2>
        <div v-if="loadingMaster" class="state">Caricamento…</div>
        <div v-else-if="!master.length" class="state">Nessun master assegnato (a parte gli admin).</div>
        <ul v-else class="cards">
          <li v-for="m in master" :key="m.utenteId" class="card">
            <div class="info">
              <span class="nome">{{ m.name }}</span>
              <span class="muted">@{{ m.username }}</span>
            </div>
            <button class="btn small danger" @click="onRimuovi(m)">Rimuovi</button>
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>

<style scoped>
.mondi-admin {
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

.muted { opacity: .65; font-size: .85rem; margin: 0; }

.add-form { display: flex; gap: .4rem; }
.add-form input {
  flex: 1;
  padding: .45rem .6rem; border: 1px solid #d0d5dd; border-radius: .5rem;
}
.add-form-mondo { flex-wrap: wrap; }
.add-form-mondo input { flex: 1 1 12rem; }

.cards { list-style: none; margin: 0; padding: 0; display: grid; gap: .5rem; }
.card {
  display: flex; align-items: center; gap: .5rem;
  padding: .6rem .8rem; background: #fff; border: 1px solid #e5e7eb; border-radius: .6rem;
}
.info { flex: 1; display: grid; }
.info .nome { font-weight: 600; }

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
.btn.danger { border-color: #fecaca; background: #fef2f2; color: #991b1b; }
.btn:disabled { opacity: .6; cursor: default; }
</style>
