<script setup lang="ts">
import {onMounted, reactive, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {apriConto, getBanche, getParty, updateConto} from '../service/PartyService'
import {Banca, Conto, PartyDetail, Soldi, totaleInMo} from '../models/dto/Party'
import Transazione from '../components/Transazione.vue'

const route = useRoute()
const router = useRouter()

const partyId = Number(route.params.id)

const party = ref<PartyDetail | null>(null)
const banche = ref<Banca[]>([])
const loading = ref(true)
const errorMsg = ref<string | null>(null)

// bozze di modifica per conto (itemId -> soldi editabili)
const drafts = reactive<Record<number, Soldi>>({})
const busySave = ref<number | null>(null)

// pannello "apri conto" per banca (bancaId -> cc selezionato)
const apriContoSel = reactive<Record<number, string>>({})
const busyApri = ref<number | null>(null)

const MONETE: Array<{ key: keyof Soldi; sigla: string; cls: string }> = [
  {key: 'mp', sigla: 'MP', cls: 'mp'},
  {key: 'mo', sigla: 'MO', cls: 'mo'},
  {key: 'ma', sigla: 'MA', cls: 'ma'},
  {key: 'mr', sigla: 'MR', cls: 'mr'},
]

async function load() {
  loading.value = true
  errorMsg.value = null
  try {
    const [pRes, bRes] = await Promise.all([getParty(partyId), getBanche(partyId)])
    party.value = pRes.data
    banche.value = bRes.data
    for (const b of banche.value) {
      for (const c of b.conti) {
        drafts[c.itemId] = {...c.soldi}
      }
    }
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 403
        ? 'Non fai parte di questo party'
        : 'Errore nel caricamento'
    console.error('Errore caricamento banche:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!Number.isFinite(partyId)) {
    errorMsg.value = 'Id party non valido'
    loading.value = false
    return
  }
  load()
})

function isDirty(c: Conto): boolean {
  const d = drafts[c.itemId]
  if (!d) return false
  return MONETE.some(m => Number(d[m.key]) !== c.soldi[m.key])
}

function step(c: Conto, key: keyof Soldi, delta: number) {
  const d = drafts[c.itemId]
  if (!d) return
  d[key] = Math.max(0, (Number(d[key]) || 0) + delta)
}

function annulla(c: Conto) {
  drafts[c.itemId] = {...c.soldi}
}

function applicaTransazione(c: Conto, delta: Soldi) {
  const d = drafts[c.itemId]
  if (!d) return
  for (const k of ['mr', 'ma', 'mo', 'mp'] as Array<keyof Soldi>) {
    d[k] = Math.max(0, (Number(d[k]) || 0) + delta[k])
  }
}

function totaleDraft(c: Conto): string {
  const d = drafts[c.itemId] ?? c.soldi
  return totaleInMo({
    mr: Number(d.mr) || 0, ma: Number(d.ma) || 0, mo: Number(d.mo) || 0, mp: Number(d.mp) || 0,
  }).toLocaleString('it-IT', {maximumFractionDigits: 2})
}

async function salva(c: Conto) {
  const d = drafts[c.itemId]
  if (!d || busySave.value) return
  busySave.value = c.itemId
  try {
    const payload: Soldi = {
      mr: Math.max(0, Math.floor(Number(d.mr) || 0)),
      ma: Math.max(0, Math.floor(Number(d.ma) || 0)),
      mo: Math.max(0, Math.floor(Number(d.mo) || 0)),
      mp: Math.max(0, Math.floor(Number(d.mp) || 0)),
    }
    const res = await updateConto(c.itemId, payload)
    c.soldi = {...res.data}
    drafts[c.itemId] = {...res.data}
  } catch (e) {
    console.error('Errore salvataggio conto:', e)
    errorMsg.value = 'Errore nel salvataggio del conto'
  } finally {
    busySave.value = null
  }
}

// opzioni per aprire un nuovo conto: party + membri (non banche) senza conto in quella banca
function opzioniApriConto(banca: Banca): Array<{ cc: string; label: string }> {
  if (!party.value) return []
  const esistenti = new Set(banca.conti.map(c => c.cc))
  const out: Array<{ cc: string; label: string }> = []
  const ccParty = `P${party.value.id}`
  if (!esistenti.has(ccParty)) out.push({cc: ccParty, label: `Party — ${party.value.nome}`})
  for (const p of party.value.personaggi) {
    if (p.tipoPersonaggio === 'BANCA') continue
    const cc = `G${p.id}`
    if (!esistenti.has(cc)) out.push({cc, label: p.nome})
  }
  return out
}

async function onApriConto(banca: Banca) {
  const cc = apriContoSel[banca.personaggioId]
  if (!cc || busyApri.value) return
  busyApri.value = banca.personaggioId
  try {
    const res = await apriConto(banca.personaggioId, cc)
    banca.conti.push(res.data)
    drafts[res.data.itemId] = {...res.data.soldi}
    apriContoSel[banca.personaggioId] = ''
  } catch (e: any) {
    console.error('Errore apertura conto:', e)
    errorMsg.value = e?.response?.status === 409
        ? 'Conto già esistente in questa banca'
        : "Errore nell'apertura del conto"
  } finally {
    busyApri.value = null
  }
}
</script>

<template>
  <div class="banche-page">
    <header class="head">
      <button class="btn ghost" @click="router.back()">←</button>
      <div class="title">
        <h1>Banche</h1>
        <span v-if="party" class="muted">{{ party.nome }}</span>
      </div>
    </header>

    <div v-if="loading" class="state">Caricamento…</div>
    <div v-else-if="errorMsg" class="state error">{{ errorMsg }}</div>

    <template v-if="!loading">
      <section v-for="banca in banche" :key="banca.personaggioId" class="banca">
        <h2>🏦 {{ banca.nome }}</h2>

        <div v-if="!banca.conti.length" class="state">Nessun conto aperto.</div>

        <div v-for="c in banca.conti" :key="c.itemId" class="conto">
          <div class="conto-head">
            <span class="intestatario">{{ c.intestatarioNome }}</span>
            <span class="pill" :class="c.tipo === 'PARTY' ? 'party' : 'giocatore'">
              {{ c.tipo === 'PARTY' ? 'Party' : 'Giocatore' }}
            </span>
            <span class="tot">≈ {{ totaleDraft(c) }} MO</span>
          </div>

          <div class="monete">
            <div v-for="m in MONETE" :key="m.key" class="moneta" :class="m.cls">
              <span class="sigla">{{ m.sigla }}</span>
              <div class="stepper">
                <button type="button" class="step-btn" :disabled="busySave === c.itemId"
                        @click="step(c, m.key, -1)">−</button>
                <input
                    v-if="drafts[c.itemId]"
                    type="number" min="0" step="1" inputmode="numeric"
                    v-model.number="drafts[c.itemId][m.key]"
                    :disabled="busySave === c.itemId"
                />
                <button type="button" class="step-btn" :disabled="busySave === c.itemId"
                        @click="step(c, m.key, 1)">+</button>
              </div>
            </div>
          </div>

          <Transazione :disabled="busySave === c.itemId"
                       @applica="delta => applicaTransazione(c, delta)"/>

          <div v-if="isDirty(c)" class="conto-actions">
            <button type="button" class="btn ghost" :disabled="busySave === c.itemId" @click="annulla(c)">
              Annulla
            </button>
            <button type="button" class="btn primary" :disabled="busySave === c.itemId" @click="salva(c)">
              {{ busySave === c.itemId ? 'Salvataggio…' : 'Salva' }}
            </button>
          </div>
        </div>

        <!-- apertura nuovo conto -->
        <div v-if="opzioniApriConto(banca).length" class="apri-conto">
          <select v-model="apriContoSel[banca.personaggioId]">
            <option value="" disabled selected>Apri un conto per…</option>
            <option v-for="o in opzioniApriConto(banca)" :key="o.cc" :value="o.cc">{{ o.label }}</option>
          </select>
          <button
              type="button"
              class="btn primary"
              :disabled="!apriContoSel[banca.personaggioId] || busyApri === banca.personaggioId"
              @click="onApriConto(banca)"
          >
            {{ busyApri === banca.personaggioId ? 'Apertura…' : 'Apri conto' }}
          </button>
        </div>
      </section>

      <div v-if="!banche.length && !errorMsg" class="state">
        Nessuna banca nel party. Crea un personaggio con label TIPO_PERSONAGGIO = BANCA.
      </div>
    </template>
  </div>
</template>

<style scoped>
.banche-page {
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

.head { display: flex; align-items: center; gap: .75rem; }
.title { display: grid; }
.title h1 { margin: 0; font-size: 1.2rem; }
.muted { opacity: .65; font-size: .85rem; }

.banca {
  display: grid;
  gap: .5rem;
  border: 1px solid #e5e7eb;
  border-radius: .75rem;
  background: #fff;
  padding: .75rem;
}

.banca h2 { margin: 0; font-size: 1.05rem; }

.conto {
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  padding: .5rem .6rem;
  display: grid;
  gap: .5rem;
  background: #fafafa;
}

.conto-head {
  display: flex;
  align-items: center;
  gap: .5rem;
}

.intestatario { flex: 1; font-weight: 700; }
.tot { font-size: .85rem; font-weight: 600; opacity: .75; white-space: nowrap; }

.pill {
  font-size: .75rem;
  padding: .15rem .5rem;
  border-radius: .5rem;
}
.pill.party { background: #fef3c7; color: #92400e; }
.pill.giocatore { background: #dbeafe; color: #1e40af; }

.monete {
  display: grid;
  grid-template-columns: 1fr;
  gap: .4rem;
}

.moneta {
  display: grid;
  grid-template-columns: 2.2rem 1fr;
  align-items: center;
  gap: .5rem;
  padding: .3rem .4rem;
  border-radius: .5rem;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.moneta .sigla { font-size: .7rem; font-weight: 700; opacity: .8; }

.moneta.mp { background: #ecfeff; border-color: #a5f3fc; }
.moneta.mo { background: #fefce8; border-color: #fde68a; }
.moneta.ma { background: #f8fafc; border-color: #cbd5e1; }
.moneta.mr { background: #fff7ed; border-color: #fed7aa; }

.stepper {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: .2rem;
}

.stepper input {
  width: 100%;
  min-width: 0;
  text-align: center;
  padding: .3rem .15rem;
  border: 1px solid #d0d5dd;
  border-radius: .4rem;
  font-variant-numeric: tabular-nums;
}

.step-btn {
  width: 1.7rem;
  border: 1px solid #d0d5dd;
  border-radius: .4rem;
  background: #f9fafb;
  font-weight: 800;
  cursor: pointer;
}
.step-btn:disabled { opacity: .5; cursor: default; }

.conto-actions {
  display: flex;
  justify-content: flex-end;
  gap: .5rem;
}

.apri-conto {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: .4rem;
}

.apri-conto select {
  padding: .45rem .6rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  background: #fff;
}

.btn {
  padding: .45rem .8rem;
  border-radius: .5rem;
  border: 1px solid transparent;
  cursor: pointer;
}
.btn.ghost { border-color: #d0d5dd; background: #fff; }
.btn.primary { background: #2563eb; color: #fff; }
.btn:disabled { opacity: .6; cursor: default; }

.state {
  padding: .75rem;
  border: 1px dashed #e5e7eb;
  border-radius: .5rem;
}
.state.error { color: #991b1b; background: #fef2f2; border-color: #fecaca; }
</style>
