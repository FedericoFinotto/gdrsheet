<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {useAuthStore} from '../stores/auth'
import {useMondoStore} from '../stores/mondo'
import {getHome} from '../service/AuthService'
import {Home} from '../models/dto/Auth'
import {createParty} from '../service/PartyService'
import {getMieiPermessiMondo} from '../service/MondoAdminService'

const router = useRouter()
const auth = useAuthStore()
const mondoStore = useMondoStore()

const home = ref<Home | null>(null)
const loading = ref(true)
const errorMsg = ref<string | null>(null)

// creazione party: nel mondo selezionato nello switcher (UpperBar.vue) — niente scelta qui,
// coerente col resto dell'app che segue sempre mondoStore.corrente.
const showCreaParty = ref(false)
const nuovoPartyNome = ref('')
const busyParty = ref(false)

function apriCreaParty() {
  showCreaParty.value = !showCreaParty.value
}

const mondoCorrenteNome = computed(() =>
    mondoStore.disponibili.find(m => m.id === mondoStore.corrente)?.descrizione ?? '')

async function onCreaParty() {
  if (!nuovoPartyNome.value.trim() || mondoStore.corrente == null || busyParty.value) return
  busyParty.value = true
  try {
    const res = await createParty(nuovoPartyNome.value.trim(), mondoStore.corrente)
    router.push(`/party/${res.data}`)
  } catch (e) {
    console.error('Errore creazione party:', e)
    errorMsg.value = 'Errore nella creazione del party'
  } finally {
    busyParty.value = false
  }
}

// Un vero admin CON la modalità admin attiva, o chi è MASTER del mondo CORRENTE (permessi_mondo,
// GET /mondo/miei-permessi con mondoId — masterMondo, non "master" che è "un mondo qualsiasi") —
// la party verrà creata lì (vedi onCreaParty), quindi il permesso deve riguardare proprio quello,
// non un mondo qualsiasi di cui si è master (es. master di Costa che guarda Cico). Il vecchio
// ruolo globale "MASTER" sull'account non è più consultato dal backend per questo.
const mieiPermessi = ref({master: false, stats: false, pagine: false, masterMondo: false})
const isMasterMondoCorrente = computed(() => (auth.isRealAdmin && auth.adminMode) || mieiPermessi.value.masterMondo)
const canCreateParty = isMasterMondoCorrente

// Filtro per il mondo selezionato nello switcher (UpperBar.vue): un utente associato a party di
// mondi diversi vede in home solo quelli del mondo corrente — coerente con "master di un mondo
// implica master di tutti i suoi party" (vedi backend), qui applicato anche in sola lettura per
// i giocatori. mondoId assente/null (dato legacy o personaggio "libero" senza party) resta
// sempre visibile, non essendoci un mondo su cui filtrarlo. Nessun filtro finché il mondo store
// non ha ancora caricato "corrente" (mostra tutto invece di svuotare la home per un istante).
function delMondoCorrente(mondoId: number | null | undefined): boolean {
  return mondoStore.corrente == null || mondoId == null || mondoId === mondoStore.corrente
}
const partiesFiltrati = computed(() => (home.value?.parties ?? []).filter(p => delMondoCorrente(p.mondoId)))

// I preferiti (VISUALIZZATORE marcato con la stellina in scheda) compaiono assieme ai propri,
// non nella sezione "visualizzabili": è esattamente lo scopo della stellina.
const personaggiProprietario = computed(() =>
    (home.value?.personaggi ?? [])
        .filter(p => p.permesso === 'PROPRIETARIO' || p.preferito)
        .filter(p => delMondoCorrente(p.mondoId)))
const personaggiVisualizzatore = computed(() =>
    (home.value?.personaggi ?? [])
        .filter(p => p.permesso === 'VISUALIZZATORE' && !p.preferito)
        .filter(p => delMondoCorrente(p.mondoId)))

function caricaMieiPermessi() {
  getMieiPermessiMondo(mondoStore.corrente).then(r => mieiPermessi.value = r.data)
      .catch(e => console.error('Errore caricamento permessi mondo:', e))
}
// mondoStore.corrente si risolve in modo asincrono (mondoStore.carica() sotto): quando cambia
// (risoluzione iniziale, o lo switcher nel menu) va ricaricato il permesso scoped su QUEL mondo.
watch(() => mondoStore.corrente, caricaMieiPermessi)

onMounted(async () => {
  mondoStore.carica() // idempotente; non blocca il caricamento della home, il filtro si applica reattivamente
  caricaMieiPermessi()
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
  if (p.tipoPersonaggio === 'BANCA') {
    router.push(`/banca/${p.id}`)
    return
  }
  const tab = p.tipoPersonaggio === 'NAVE' ? '?tab=2' : ''
  router.push(`/scheda/${p.id}${tab}`)
}
</script>

<template>
  <div class="home">
    <div class="user">
      <h1>{{ mondoCorrenteNome || '—' }}</h1>
      <div class="permessi-riga">
        <!-- Master e Giocatore sono mutualmente esclusivi: essere master del mondo include già
             ogni potere di un giocatore su di esso, mostrarli insieme è ridondante — al più uno
             dei due. Stat/Config restano invece permessi indipendenti, sommabili a entrambi. -->
        <span v-if="isMasterMondoCorrente" class="chip-permesso">Master</span>
        <span v-else class="chip-permesso giocatore">Giocatore</span>
        <span v-if="(auth.isRealAdmin && auth.adminMode) || mieiPermessi.stats" class="chip-permesso">Stat</span>
        <span v-if="(auth.isRealAdmin && auth.adminMode) || mieiPermessi.pagine" class="chip-permesso">Config</span>
      </div>
    </div>

    <div v-if="loading" class="state">Caricamento…</div>
    <div v-else-if="errorMsg" class="state error">{{ errorMsg }}</div>

    <template v-else-if="home">
      <!-- Crea party -->
      <section v-if="canCreateParty" class="block">
        <button class="btn-add" @click="apriCreaParty">
          <span class="plus">+</span> Crea party
        </button>
        <div v-if="showCreaParty" class="crea-form">
          <input v-model="nuovoPartyNome" type="text" placeholder="Nome del party" @keyup.enter="onCreaParty"/>
          <p v-if="mondoCorrenteNome" class="muted">Verrà creato nel mondo «{{ mondoCorrenteNome }}» (vedi menu in alto).</p>
          <p v-else class="muted">Nessun mondo selezionato: apri il menu in alto e scegline uno.</p>
          <button class="btn primary" :disabled="busyParty || !nuovoPartyNome.trim() || mondoStore.corrente == null"
                  @click="onCreaParty">
            {{ busyParty ? 'Creazione…' : 'Crea' }}
          </button>
        </div>
      </section>

      <!-- Party -->
      <section v-if="partiesFiltrati.length" class="block">
        <h2>I tuoi party</h2>
        <ul class="cards">
          <li v-for="p in partiesFiltrati" :key="p.id">
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

      <div v-if="!partiesFiltrati.length && !personaggiProprietario.length && !personaggiVisualizzatore.length" class="state">
        Nessun party o personaggio associato al tuo utente{{ mondoStore.mostraSwitcher ? ' in questo mondo' : '' }}.
      </div>
    </template>
  </div>
</template>

<style scoped>
.home {
  width: 100%;
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

.user { display: grid; gap: .35rem; }
.user h1 { margin: 0; font-size: 1.25rem; }
.permessi-riga { display: flex; flex-wrap: wrap; gap: .35rem; }
.chip-permesso {
  font-size: .7rem; font-weight: 600; padding: .1rem .5rem; border-radius: 1rem;
  border: 1px solid #4338ca; background: #eef2ff; color: #4338ca; line-height: 1.4;
}
.chip-permesso.giocatore { border-color: var(--hairline); background: var(--btn-bg); color: var(--text-muted); }
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
  background: var(--surface-0);
  color: var(--text-strong);
  border: 1px solid var(--hairline);
  border-radius: .6rem;
  text-align: left;
}

.card .nome { flex: 1; font-weight: 600; }
.card.clickable { cursor: pointer; }
.card.clickable:hover { background: var(--btn-bg); }

.pill {
  font-size: .75rem;
  padding: .15rem .5rem;
  border-radius: .5rem;
}
.pill.master { background: var(--warning-bg); color: var(--warning-text); }
.pill.giocatore { background: var(--info-bg); color: var(--info-text); }
.pill.viewer { background: var(--btn-bg); color: var(--text-muted); }

.state {
  padding: .75rem;
  border: 1px dashed var(--hairline);
  border-radius: .5rem;
}
.state.error { color: var(--error-color); background: var(--surface-warn); border-color: var(--hairline); }

.btn {
  padding: .45rem .8rem;
  border-radius: .5rem;
  border: 1px solid var(--hairline);
  background: var(--surface-0);
  color: var(--text-strong);
  cursor: pointer;
}
.btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.btn:disabled { opacity: .6; cursor: default; }

.btn-add {
  justify-self: start;
  display: inline-flex;
  align-items: center;
  gap: .35rem;
  padding: .4rem .8rem;
  border: 1px dashed var(--text-muted);
  border-radius: .5rem;
  background: var(--surface-0);
  color: var(--text-strong);
  font-weight: 600;
  font-size: .85rem;
  cursor: pointer;
}
.btn-add .plus { font-weight: 800; color: #2563eb; }

.crea-form {
  display: grid;
  gap: .4rem;
  padding: .6rem;
  border: 1px solid var(--hairline);
  border-radius: .6rem;
  background: var(--surface-0);
}
.crea-form input {
  padding: .45rem .6rem;
  border: 1px solid var(--hairline);
  border-radius: .5rem;
  background: var(--surface-0);
  color: var(--text-strong);
}
</style>
