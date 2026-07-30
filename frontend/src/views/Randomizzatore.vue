<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import SearchSelect from '../components/SearchSelect.vue'
import EsitoRandomizzatore from '../components/EsitoRandomizzatore.vue'
import {
  Esito,
  estrai as estraiApi,
  getConfig,
  getStorico,
  RandomizzatoreConfig,
  StoricoVoce,
} from '../service/RandomizzatoreService'
import {useMondoSistema} from '../function/useMondoSistema'

const route = useRoute()
const router = useRouter()
const idRandomizzatore = Number(route.params.id)

const {autoMondo, autoSistema} = useMondoSistema()

const config = ref<RandomizzatoreConfig | null>(null)
const loading = ref(true)
const errorMsg = ref<string | null>(null)

// tag scelto per ogni categoria "scelta", indicizzato per id categoria
const scelte = ref<Record<number, number | null>>({})

const esito = ref<Esito | null>(null)
const estraendo = ref(false)
const storico = ref<StoricoVoce[]>([])
const mostraStorico = ref(false)

onMounted(async () => {
  try {
    const {data} = await getConfig(idRandomizzatore)
    config.value = data
    data.categorieScelte.forEach(c => scelte.value[c.id] = null)
  } catch (e: any) {
    errorMsg.value = e?.response?.data?.message
        ?? (e?.response?.status === 404 ? 'Randomizzatore non trovato' : 'Errore nel caricamento')
  } finally {
    loading.value = false
  }
})

// serve un tag per ognuna delle categorie da scegliere
const puoEstrarre = computed(() =>
    !!config.value
    && !estraendo.value
    && config.value.categorieScelte.every(c => scelte.value[c.id] != null))

async function estrai() {
  if (!puoEstrarre.value || !config.value) return
  estraendo.value = true
  errorMsg.value = null
  try {
    const tagScelti = config.value.categorieScelte.map(c => scelte.value[c.id] as number)
    const {data} = await estraiApi(idRandomizzatore, tagScelti, autoMondo.value, autoSistema.value)
    esito.value = data
    if (mostraStorico.value) await caricaStorico()
  } catch (e: any) {
    esito.value = null
    errorMsg.value = e?.response?.status === 404
        ? 'Nessun oggetto corrisponde ai criteri scelti'
        : (e?.response?.data?.message ?? 'Errore durante l\'estrazione')
  } finally {
    estraendo.value = false
  }
}

async function caricaStorico() {
  try {
    storico.value = (await getStorico(idRandomizzatore)).data
  } catch (e) {
    console.error('Errore caricamento storico:', e)
  }
}

async function toggleStorico() {
  mostraStorico.value = !mostraStorico.value
  if (mostraStorico.value && !storico.value.length) await caricaStorico()
}

function formatData(iso: string): string {
  try {
    return new Date(iso).toLocaleString('it-IT', {dateStyle: 'short', timeStyle: 'short'})
  } catch {
    return iso
  }
}
</script>

<template>
  <div class="rand-page">
    <header class="head">
      <button class="btn ghost" @click="router.back()">←</button>
      <div class="title">
        <h1>{{ config?.nome ?? 'Randomizzatore' }}</h1>
        <span v-if="config" class="muted">
          pesi combinati per {{ config.combina === 'SOMMA' ? 'somma' : 'prodotto' }}
        </span>
      </div>
    </header>

    <div v-if="loading" class="state">Caricamento…</div>
    <div v-else-if="!config" class="state error">{{ errorMsg }}</div>

    <template v-else>
      <!-- scelte -->
      <section class="block">
        <h2>Scegli</h2>
        <div v-if="!config.categorieScelte.length" class="state muted">
          Nessuna scelta richiesta: puoi estrarre direttamente.
        </div>
        <label v-for="c in config.categorieScelte" :key="c.id" class="field">
          <span class="lbl">{{ c.nome }}</span>
          <SearchSelect v-model="scelte[c.id]"
                        :options="c.tag.map(t => ({value: t.id, label: t.nome}))"
                        :placeholder="`Scegli ${c.nome.toLowerCase()}…`"/>
        </label>

        <div v-if="config.categoriePresenti.length" class="richieste">
          <span class="muted">Verranno estratti anche:</span>
          <span v-for="c in config.categoriePresenti" :key="c.id" class="chip">{{ c.nome }}</span>
        </div>

        <button class="btn primary big" :disabled="!puoEstrarre" @click="estrai">
          {{ estraendo ? 'Estrazione…' : '🎲 Estrai' }}
        </button>
        <p v-if="errorMsg" class="state error">{{ errorMsg }}</p>
      </section>

      <!-- esito (albero: l'oggetto estratto può innescare altri randomizzatori) -->
      <section v-if="esito" class="block">
        <EsitoRandomizzatore :esito="esito"/>
      </section>

      <!-- storico -->
      <section class="block">
        <button type="button" class="storico-toggle" @click="toggleStorico">
          <span class="chev" :class="{open: mostraStorico}">▸</span> Storico estrazioni
        </button>
        <div v-if="mostraStorico" class="storico">
          <div v-if="!storico.length" class="state muted">Nessuna estrazione registrata.</div>
          <div v-for="v in storico" :key="v.id" class="storico-riga">
            <div class="storico-top">
              <span class="muted">{{ formatData(v.eseguitoIl) }}</span>
              <span v-if="v.utente" class="muted utente">{{ v.utente }}</span>
            </div>
            <EsitoRandomizzatore v-if="v.esito" :esito="v.esito"/>
            <span v-else class="muted">—</span>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.rand-page {
  width: 100%; max-width: 44rem; margin: 0 auto; padding: 1rem;
  display: grid; gap: .75rem; align-content: start;
  height: 100%; min-height: 0; overflow-y: auto;
}

.head { display: flex; align-items: center; gap: .5rem; }
.title { flex: 1; display: grid; min-width: 0; }
.title h1 { margin: 0; font-size: 1.2rem; }
.muted { opacity: .65; font-size: .85rem; }

.block { display: grid; gap: .5rem; }
.block h2 { margin: 0; font-size: 1rem; }

.field { display: grid; gap: .25rem; }
.lbl { font-size: .8rem; font-weight: 700; opacity: .8; }

.richieste { display: flex; flex-wrap: wrap; align-items: center; gap: .35rem; }

.chip {
  font-size: .75rem; font-weight: 600; padding: .15rem .55rem;
  border-radius: 999px; background: #f3f4f6; color: #374151; white-space: nowrap;
}
.chip.scelto { background: #dbeafe; color: #1d4ed8; }
.chip.estratto { background: #dcfce7; color: #166534; }

.btn {
  border: 1px solid #d0d5dd; background: #fff; border-radius: .5rem;
  padding: .45rem .8rem; cursor: pointer; font-weight: 600;
}
.btn.ghost { background: #fff; }
.btn.primary { background: #2563eb; border-color: #2563eb; color: #fff; }
.btn.big { padding: .7rem 1rem; font-size: 1rem; margin-top: .25rem; }
.btn:disabled { opacity: .5; cursor: default; }

.esito-card {
  border: 1px solid #bbf7d0; background: #f0fdf4;
  border-radius: .6rem; padding: .75rem .9rem; display: grid; gap: .5rem;
}
.esito-head { display: flex; align-items: baseline; justify-content: space-between; gap: .5rem; flex-wrap: wrap; }
.esito-head h2 { margin: 0; font-size: 1.15rem; }
.candidati { white-space: nowrap; }
.tag-riga { display: flex; flex-wrap: wrap; gap: .35rem; }
.tag-riga.piccola .chip { font-size: .7rem; }
.descrizione { font-size: .9rem; white-space: pre-wrap; }
.descrizione :deep(p) { margin: .3rem 0; }

.storico-toggle {
  justify-self: start; border: 0; background: none; cursor: pointer;
  font-size: .85rem; font-weight: 600; color: #2563eb;
  display: inline-flex; align-items: center; gap: .3rem; padding: 0;
}
.storico-toggle .chev { transition: transform .15s ease; }
.storico-toggle .chev.open { transform: rotate(90deg); }

.storico { display: grid; gap: .4rem; }
.storico-riga {
  border: 1px solid #e5e7eb; border-radius: .5rem; padding: .45rem .6rem;
  background: #fff; display: grid; gap: .25rem;
}
.storico-top { display: flex; justify-content: space-between; gap: .5rem; align-items: baseline; }
.storico-nome { font-weight: 600; }
.utente { font-size: .75rem; }

.state { font-size: .9rem; padding: .25rem 0; }
.state.error { color: #991b1b; }
</style>
