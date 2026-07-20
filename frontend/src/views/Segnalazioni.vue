<script setup lang="ts">
import {defineAsyncComponent, onMounted, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import usePopup from '../function/usePopup'
import {aggiungiCommento, dettaglioSegnalazione, listaCommenti, listaSegnalazioni} from '../service/SegnalazioniService'
import {Comento, Segnalazione} from '../models/dto/Segnalazione'
import {segnaVista} from '../function/segnalazioniNotifiche'

const SegnalazioneCreatePopup = defineAsyncComponent(() => import('../components/SegnalazioneCreatePopup.vue'))

const router = useRouter()
const {openPopup} = usePopup()

const segnalazioni = ref<Segnalazione[]>([])
const loading = ref(true)
const errorMsg = ref<string | null>(null)
const mostraTutte = ref(false)

async function carica() {
  loading.value = true
  errorMsg.value = null
  try {
    segnalazioni.value = (await listaSegnalazioni(mostraTutte.value)).data
  } catch (e) {
    console.error('Errore caricamento segnalazioni:', e)
    errorMsg.value = 'Errore nel caricamento delle segnalazioni'
  } finally {
    loading.value = false
  }
}

function toggleMostraTutte() {
  mostraTutte.value = !mostraTutte.value
  carica()
}

// colori semantici per gli stati noti, fallback hash-based per eventuali altri
const COLORI_STATO_NOTI: Record<string, { bg: string; fg: string }> = {
  nuovo: {bg: '#eef2ff', fg: '#3730a3'},
  new: {bg: '#eef2ff', fg: '#3730a3'},
  'in corso': {bg: '#fef3c7', fg: '#92400e'},
  concluso: {bg: '#dcfce7', fg: '#166534'},
  deployato: {bg: '#e0f2fe', fg: '#075985'},
  archiviato: {bg: '#f3f4f6', fg: '#6b7280'},
}
const PALETTE_FALLBACK = [
  {bg: '#fce7f3', fg: '#9d174d'},
  {bg: '#ede9fe', fg: '#5b21b6'},
  {bg: '#ffedd5', fg: '#9a3412'},
  {bg: '#cffafe', fg: '#155e75'},
]
function mostraChipStato(stato: string | null): boolean {
  const key = (stato ?? '').trim().toLowerCase()
  return key !== '' && key !== 'nuovo' && key !== 'new'
}
function statoStyle(stato: string | null): { background: string; color: string } {
  const key = (stato ?? '').trim().toLowerCase()
  const noto = COLORI_STATO_NOTI[key]
  if (noto) return {background: noto.bg, color: noto.fg}
  let hash = 0
  for (let i = 0; i < key.length; i++) hash = (hash * 31 + key.charCodeAt(i)) | 0
  const scelto = PALETTE_FALLBACK[Math.abs(hash) % PALETTE_FALLBACK.length]
  return {background: scelto.bg, color: scelto.fg}
}

onMounted(carica)

function apriCreazione() {
  openPopup(SegnalazioneCreatePopup, {onCreated: carica}, {closable: true, autoClose: 0})
}

// stato commenti per segnalazione aperta
const aperti = ref<Set<number>>(new Set())
interface CommentiState { lista: Comento[]; loading: boolean; nuovo: string; inviando: boolean }
const commenti = reactive<Record<number, CommentiState>>({})
// la descrizione non arriva dalla lista (Taiga la omette): caricata al primo espandi
const descrizioneCaricata = ref<Set<number>>(new Set())

async function toggleAperto(s: Segnalazione) {
  if (aperti.value.has(s.id)) {
    aperti.value.delete(s.id)
    return
  }
  aperti.value.add(s.id)
  segnaVista(s.id, s.dataModifica)

  if (!descrizioneCaricata.value.has(s.id)) {
    descrizioneCaricata.value.add(s.id)
    try {
      s.descrizione = (await dettaglioSegnalazione(s.id)).data.descrizione
    } catch (e) {
      console.error('Errore caricamento descrizione:', e)
    }
  }

  const nuovoTestoInCorso = commenti[s.id]?.nuovo ?? ''
  commenti[s.id] = {lista: commenti[s.id]?.lista ?? [], loading: true, nuovo: nuovoTestoInCorso, inviando: false}
  try {
    commenti[s.id].lista = (await listaCommenti(s.id)).data
  } catch (e) {
    console.error('Errore caricamento commenti:', e)
  } finally {
    commenti[s.id].loading = false
  }
}

async function invia(s: Segnalazione) {
  const st = commenti[s.id]
  const testo = st.nuovo.trim()
  if (!testo || st.inviando) return
  st.inviando = true
  try {
    const c = (await aggiungiCommento(s.id, testo)).data
    st.lista.push(c)
    st.nuovo = ''
    // il commento appena scritto aggiorna dataModifica su Taiga: segna comunque come vista
    // adesso, altrimenti il proprio commento risulterebbe come "notifica non letta"
    segnaVista(s.id)
  } catch (e) {
    console.error('Errore invio commento:', e)
  } finally {
    st.inviando = false
  }
}
</script>

<template>
  <div class="segnalazioni-page">
    <header class="head">
      <button class="btn ghost" @click="router.back()">←</button>
      <h1>Segnalazioni</h1>
    </header>

    <div class="azioni-header">
      <button class="btn primary aggiungi" @click="apriCreazione">+ Aggiungi segnalazione</button>
      <button class="btn ghost" @click="toggleMostraTutte">
        {{ mostraTutte ? 'Mostra solo le mie' : 'Mostra tutte' }}
      </button>
    </div>

    <div v-if="loading" class="state">Caricamento…</div>
    <div v-else-if="errorMsg" class="state error">{{ errorMsg }}</div>
    <div v-else-if="!segnalazioni.length" class="state">Nessuna segnalazione ancora.</div>

    <section v-for="s in segnalazioni" :key="s.id" class="seg-card">
      <div class="seg-head" @click="toggleAperto(s)">
        <span class="chev" :class="{open: aperti.has(s.id)}">▸</span>
        <span class="seg-titolo">{{ s.titolo }}</span>
        <span v-if="mostraChipStato(s.stato)" class="seg-stato" :style="statoStyle(s.stato)">{{ s.stato }}</span>
      </div>

      <template v-if="aperti.has(s.id)">
        <p v-if="s.descrizione" class="seg-descrizione">{{ s.descrizione }}</p>

        <div class="commenti">
          <div v-if="commenti[s.id]?.loading" class="state small">Caricamento commenti…</div>
          <template v-else>
            <div v-for="(c, i) in commenti[s.id]?.lista ?? []" :key="i" class="commento">
              <span class="commento-autore">{{ c.autore ?? '—' }}</span>
              <span class="commento-testo">{{ c.testo }}</span>
            </div>
            <div v-if="!(commenti[s.id]?.lista?.length)" class="state small">Nessun commento.</div>
          </template>

          <div class="nuovo-commento">
            <textarea
                v-model="commenti[s.id].nuovo"
                rows="2"
                placeholder="Scrivi un commento…"
                @keydown.enter.exact.prevent="invia(s)"
            />
            <button class="btn primary" :disabled="!commenti[s.id]?.nuovo?.trim() || commenti[s.id]?.inviando" @click="invia(s)">
              {{ commenti[s.id]?.inviando ? '…' : 'Invia' }}
            </button>
          </div>
        </div>
      </template>
    </section>
  </div>
</template>

<style scoped>
.segnalazioni-page {
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
}
.head { display: flex; align-items: center; gap: .5rem; }
.head h1 { margin: 0; font-size: 1.25rem; }

.azioni-header { display: flex; flex-wrap: wrap; gap: .5rem; }

.seg-card {
  display: grid;
  gap: .6rem;
  padding: .75rem;
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  background: #fff;
}
.seg-head { display: flex; gap: .5rem; align-items: center; cursor: pointer; }
.seg-head .chev { font-size: .8rem; opacity: .6; transition: transform .15s; flex-shrink: 0; }
.seg-head .chev.open { transform: rotate(90deg); }
.seg-titolo { flex: 1; font-weight: 700; }
.seg-stato {
  font-size: .7rem; font-weight: 700;
  border-radius: 999px; padding: .1rem .5rem; flex-shrink: 0;
}
.seg-descrizione { margin: 0; font-size: .9rem; color: #374151; white-space: pre-wrap; }

.commenti { display: flex; flex-direction: column; gap: .5rem; border-top: 1px solid #f1f3f5; padding-top: .5rem; }
.commento { display: flex; flex-direction: column; gap: .1rem; font-size: .85rem; }
.commento-autore { font-weight: 700; color: #1f2937; }
.commento-testo { white-space: pre-wrap; }

.nuovo-commento { display: flex; gap: .5rem; align-items: flex-end; }
.nuovo-commento textarea {
  flex: 1; padding: .5rem .6rem; border: 1px solid #d0d5dd; border-radius: .5rem; font: inherit; resize: vertical;
}

.btn { padding: .45rem .8rem; border-radius: .5rem; border: 1px solid #d0d5dd; background: #fff; cursor: pointer; }
.btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.btn.primary:disabled { opacity: .6; cursor: default; }
.btn.ghost { background: #fff; }

.state { padding: .75rem; border: 1px dashed #e5e7eb; border-radius: .5rem; }
.state.error { color: #991b1b; background: #fef2f2; border-color: #fecaca; }
.state.small { padding: .35rem; border: 0; font-size: .8rem; color: #6b7280; }
</style>
