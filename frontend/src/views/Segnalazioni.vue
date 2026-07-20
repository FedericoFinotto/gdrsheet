<script setup lang="ts">
import {defineAsyncComponent, onMounted, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import usePopup from '../function/usePopup'
import {
  aggiungiCommento,
  dettaglioSegnalazione,
  listaAllegati,
  listaCommenti,
  listaSegnalazioni,
  modificaSegnalazione,
  scaricaAllegato,
} from '../service/SegnalazioniService'
import {Allegato, Comento, Segnalazione} from '../models/dto/Segnalazione'
import {caricaViste, segnaVista, segnalazioneNonVista, ultimaVistaDi} from '../function/segnalazioniNotifiche'

const SegnalazioneCreatePopup = defineAsyncComponent(() => import('../components/SegnalazioneCreatePopup.vue'))
const ImmagineAllegatoPopup = defineAsyncComponent(() => import('../components/ImmagineAllegatoPopup.vue'))

const router = useRouter()
const {openPopup} = usePopup()

const segnalazioni = ref<Segnalazione[]>([])
const loading = ref(true)
const errorMsg = ref<string | null>(null)
const mostraTutte = ref(false)

// segnalazioni con variazioni non ancora viste dall'utente (per il puntino rosso in lista)
const nonVisti = reactive<Record<number, boolean>>({})

// ordine di visualizzazione: In corso, poi Nuove, poi Concluse, poi Deployate; il resto (es.
// eventuali stati sconosciuti) resta in fondo, nell'ordine restituito da Taiga
const ORDINE_STATO: Record<string, number> = {
  'in corso': 0,
  nuovo: 1, new: 1,
  concluso: 2,
  deployato: 3,
}
function prioritaStato(stato: string | null): number {
  const key = (stato ?? '').trim().toLowerCase()
  return ORDINE_STATO[key] ?? 99
}

async function carica() {
  loading.value = true
  errorMsg.value = null
  try {
    await caricaViste()
    segnalazioni.value = (await listaSegnalazioni(mostraTutte.value)).data
        .sort((a, b) => prioritaStato(a.stato) - prioritaStato(b.stato))
    for (const s of segnalazioni.value) nonVisti[s.id] = segnalazioneNonVista(s)
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
// al primo espandi, tiene descrizione+commenti dietro un unico placeholder invece di
// farli comparire uno alla volta (altrimenti la descrizione "salta dentro" in ritardo
// e sposta tutto il resto, dando l'effetto di flash/sfarfallio)
const primoCaricamento = ref<Set<number>>(new Set())
const allegati = reactive<Record<number, Allegato[]>>({})
// timestamp dell'ultima visita PRIMA di segnare come vista ora: serve a capire quali
// commenti sono arrivati dopo, per evidenziarli come "nuovo" invece di mostrarli tutti uguali
const sogliaCommentiNuovi = reactive<Record<number, string | null>>({})

function commentoNuovo(s: Segnalazione, c: Comento): boolean {
  const soglia = sogliaCommentiNuovi[s.id]
  if (!soglia || !c.data) return false
  return new Date(c.data).getTime() > new Date(soglia).getTime()
}

async function toggleAperto(s: Segnalazione) {
  if (aperti.value.has(s.id)) {
    aperti.value.delete(s.id)
    return
  }
  aperti.value.add(s.id)
  sogliaCommentiNuovi[s.id] = ultimaVistaDi(s.id)
  segnaVista(s.id)
  nonVisti[s.id] = false

  const èPrimaVolta = !descrizioneCaricata.value.has(s.id)
  if (èPrimaVolta) primoCaricamento.value.add(s.id)

  const promesse: Promise<void>[] = []
  if (èPrimaVolta) {
    descrizioneCaricata.value.add(s.id)
    promesse.push(
        dettaglioSegnalazione(s.id)
            .then(res => { s.descrizione = res.data.descrizione })
            .catch(e => console.error('Errore caricamento descrizione:', e))
    )
    promesse.push(
        listaAllegati(s.id)
            .then(res => { allegati[s.id] = res.data })
            .catch(e => console.error('Errore caricamento allegati:', e))
    )
  }

  const nuovoTestoInCorso = commenti[s.id]?.nuovo ?? ''
  commenti[s.id] = {lista: commenti[s.id]?.lista ?? [], loading: true, nuovo: nuovoTestoInCorso, inviando: false}
  promesse.push(
      listaCommenti(s.id)
          .then(res => { commenti[s.id].lista = res.data })
          .catch(e => console.error('Errore caricamento commenti:', e))
          .finally(() => { commenti[s.id].loading = false })
  )

  await Promise.all(promesse)
  primoCaricamento.value.delete(s.id)
}

// modifica inline titolo/descrizione, solo per le proprie segnalazioni (s.mia)
const inModifica = ref<Set<number>>(new Set())
interface BozzaModifica { titolo: string; descrizione: string; salvando: boolean }
const bozze = reactive<Record<number, BozzaModifica>>({})

function iniziaModifica(s: Segnalazione) {
  bozze[s.id] = {titolo: s.titolo, descrizione: s.descrizione ?? '', salvando: false}
  inModifica.value.add(s.id)
}

function annullaModifica(id: number) {
  inModifica.value.delete(id)
}

async function salvaModifica(s: Segnalazione) {
  const bozza = bozze[s.id]
  const titolo = bozza.titolo.trim()
  if (!titolo || bozza.salvando) return
  bozza.salvando = true
  try {
    const aggiornata = (await modificaSegnalazione(s.id, titolo, bozza.descrizione.trim())).data
    s.titolo = aggiornata.titolo
    s.descrizione = aggiornata.descrizione
    inModifica.value.delete(s.id)
  } catch (e) {
    console.error('Errore modifica segnalazione:', e)
  } finally {
    bozza.salvando = false
  }
}

async function apriImmagine(s: Segnalazione, a: Allegato) {
  try {
    const blob = (await scaricaAllegato(s.id, a.id)).data
    const url = URL.createObjectURL(blob)
    openPopup(ImmagineAllegatoPopup, {src: url}, {closable: true, autoClose: 0})
  } catch (e) {
    console.error('Errore download allegato:', e)
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
        <span v-if="nonVisti[s.id]" class="puntino-nonvisto" title="Ci sono variazioni non ancora viste"></span>
        <span class="seg-titolo">{{ s.titolo }}</span>
        <span v-if="mostraChipStato(s.stato)" class="seg-stato" :style="statoStyle(s.stato)">{{ s.stato }}</span>
        <button v-if="s.mia && aperti.has(s.id) && !inModifica.has(s.id)" class="btn ghost icona-modifica"
                title="Modifica" @click.stop="iniziaModifica(s)">✎</button>
      </div>

      <template v-if="aperti.has(s.id)">
        <div v-if="primoCaricamento.has(s.id)" class="state small">Caricamento…</div>
        <template v-else>

        <div v-if="inModifica.has(s.id)" class="modifica-form">
          <label class="campo">
            <span>Titolo</span>
            <input v-model="bozze[s.id].titolo" type="text"/>
          </label>
          <label class="campo">
            <span>Descrizione</span>
            <textarea v-model="bozze[s.id].descrizione" rows="4"/>
          </label>
          <div class="azioni">
            <button class="btn ghost" @click="annullaModifica(s.id)">Annulla</button>
            <button class="btn primary" :disabled="!bozze[s.id].titolo.trim() || bozze[s.id].salvando" @click="salvaModifica(s)">
              {{ bozze[s.id].salvando ? 'Salvataggio…' : 'Salva' }}
            </button>
          </div>
        </div>
        <p v-else-if="s.descrizione" class="seg-descrizione">{{ s.descrizione }}</p>

        <div v-if="allegati[s.id]?.length" class="allegati">
          <button v-for="a in allegati[s.id]" :key="a.id" class="allegato" @click="apriImmagine(s, a)">
            🖼️ {{ a.nome ?? 'immagine' }}
          </button>
        </div>

        <div class="commenti">
          <div v-if="commenti[s.id]?.loading" class="state small">Caricamento commenti…</div>
          <template v-else>
            <div v-for="(c, i) in commenti[s.id]?.lista ?? []" :key="i" class="commento" :class="{nuovo: commentoNuovo(s, c)}">
              <span class="commento-autore">{{ c.autore ?? '—' }}<span v-if="commentoNuovo(s, c)" class="badge-nuovo">nuovo</span></span>
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
.puntino-nonvisto { width: .5rem; height: .5rem; border-radius: 50%; background: #e02424; flex-shrink: 0; }
.seg-titolo { flex: 1; font-weight: 700; }
.seg-stato {
  font-size: .7rem; font-weight: 700;
  border-radius: 999px; padding: .1rem .5rem; flex-shrink: 0;
}
.seg-descrizione { margin: 0; font-size: .9rem; color: #374151; white-space: pre-wrap; }
.icona-modifica { padding: .2rem .45rem; flex-shrink: 0; }

.modifica-form { display: flex; flex-direction: column; gap: .6rem; }
.modifica-form .campo { display: flex; flex-direction: column; gap: .3rem; font-size: .85rem; color: #374151; }
.modifica-form input, .modifica-form textarea {
  padding: .5rem .6rem; border: 1px solid #d0d5dd; border-radius: .5rem; font: inherit; resize: vertical;
}
.modifica-form .azioni { display: flex; justify-content: flex-end; gap: .5rem; }

.allegati { display: flex; flex-wrap: wrap; gap: .5rem; }
.allegato {
  font: inherit; font-size: .8rem; color: #2563eb; background: #fff; cursor: pointer;
  border: 1px solid #d0d5dd; border-radius: .5rem; padding: .3rem .6rem;
}
.allegato:hover { text-decoration: underline; }

.commenti { display: flex; flex-direction: column; gap: .5rem; border-top: 1px solid #f1f3f5; padding-top: .5rem; }
.commento { display: flex; flex-direction: column; gap: .1rem; font-size: .85rem; padding: .3rem; border-radius: .4rem; }
.commento.nuovo { background: #eff6ff; border-left: 3px solid #2563eb; padding-left: .5rem; }
.commento-autore { font-weight: 700; color: #1f2937; display: flex; align-items: center; gap: .4rem; }
.commento-testo { white-space: pre-wrap; }
.badge-nuovo {
  font-size: .65rem; font-weight: 700; text-transform: uppercase; color: #fff;
  background: #2563eb; border-radius: 999px; padding: .05rem .4rem;
}

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
