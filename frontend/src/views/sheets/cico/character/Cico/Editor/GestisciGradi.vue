<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useCharacterStore} from '../../../../../../stores/personaggio'
import {
  getAbilitaClasseByPersonaggioLivelloClasse,
  getListaAbilitaPerPersonaggio,
  saveRanghiBulk,
} from '../../../../../../service/PersonaggioService'
import {Abilita} from '../../../../../../models/dto/Abilita'
import {AbilitaClasse} from '../../../../../../models/dto/AbilitaClasse'

const props = defineProps<{ idPersonaggio: number }>()
const router = useRouter()
const characterStore = useCharacterStore()

interface Colonna {
  id: number              // id dell'item LIVELLO
  livello: number         // numero del livello del personaggio
  classe: string
  budget: number          // gradi (punti) spendibili a questo livello (label GRADI_LIVELLO, 0 se assente)
  maxRank: number         // cap di gradi per singola abilità a questo livello (= livello + 3)
  classSet: Set<string>   // uid delle abilità DI CLASSE a questo livello (incl. trasversali)
}

interface Riga {
  uid: string
  nome: string
}

const loading = ref(true)
const saving = ref(false)
const errore = ref('')
const messaggio = ref('')
const avvisoBackend = ref(false)   // true se la risposta livelli non contiene i campi nuovi

const colonne = ref<Colonna[]>([])
const righe = ref<Riga[]>([])
// model[uid][livelloId] = punti rank spesi (valore grezzo salvato)
const model = reactive<Record<string, Record<number, number>>>({})
// stato iniziale (dopo il prefill) per salvare SOLO i livelli modificati
let snapshot: Record<string, Record<number, number>> = {}

function aggiornaSnapshot() {
  snapshot = {}
  for (const uid of Object.keys(model)) snapshot[uid] = {...model[uid]}
}

// un livello (colonna) è modificato se almeno un'abilità differisce dallo snapshot
function colonnaModificata(col: Colonna): boolean {
  return righe.value.some(r => (model[r.uid]?.[col.id] ?? 0) !== (snapshot[r.uid]?.[col.id] ?? 0))
}

const punti = (uid: string, livId: number): number => model[uid]?.[livId] ?? 0

function setPunti(uid: string, livId: number, v: number) {
  const n = Math.max(0, Math.floor(v) || 0)
  if (!model[uid]) model[uid] = {}
  model[uid][livId] = n
}

function inc(uid: string, col: Colonna) {
  if (!canInc(col, uid)) return
  setPunti(uid, col.id, punti(uid, col.id) + 1)
}
const dec = (uid: string, col: Colonna) => setPunti(uid, col.id, punti(uid, col.id) - 1)

// azzera tutte le celle
function azzeraTutto() {
  for (const r of righe.value) for (const c of colonne.value) setPunti(r.uid, c.id, 0)
}

// ripristina la situazione di quando si è aperta la pagina (snapshot iniziale)
function ripristina() {
  for (const r of righe.value) for (const c of colonne.value) {
    setPunti(r.uid, c.id, snapshot[r.uid]?.[c.id] ?? 0)
  }
}

const modificato = computed(() => colonne.value.some(colonnaModificata))

const isClasse = (col: Colonna, uid: string) => col.classSet.has(uid.toLowerCase())

// le professioni (stat id che inizia con "PR") sono a parte: non consumano il
// budget del livello e non rientrano nel conteggio.
const isProfessione = (uid: string) => uid.toUpperCase().startsWith('PR')
const righeNormali = computed(() => righe.value.filter(r => !isProfessione(r.uid)))
const righeProfessioni = computed(() => righe.value.filter(r => isProfessione(r.uid)))

// valore applicato: pieno se abilità di classe, metà se trasversale/cross
function applicato(col: Colonna, uid: string): number {
  const p = punti(uid, livId(col))
  return isClasse(col, uid) ? p : p / 2
}

const livId = (col: Colonna) => col.id
const fmt = (n: number) => Number.isInteger(n) ? String(n) : n.toFixed(1)

// totale punti spesi a quel livello (solo abilità non-professione, confronto col budget)
const totaleColonna = (col: Colonna) =>
    righeNormali.value.reduce((s, r) => s + punti(r.uid, col.id), 0)
const sforato = (col: Colonna) => totaleColonna(col) > col.budget
// somma dei valori APPLICATI per riga (effetto totale sull'abilità)
const totaleRiga = (uid: string) =>
    colonne.value.reduce((s, c) => s + applicato(c, uid), 0)

// si può incrementare se: c'è budget al livello E il cap per-abilità
// (gradi max = livello+3, cumulativo per livello) non viene superato.
function canInc(col: Colonna, uid: string): boolean {
  // le professioni non consumano il budget del livello
  if (!isProfessione(uid) && totaleColonna(col) >= col.budget) return false
  const delta = isClasse(col, uid) ? 1 : 0.5
  const idx = colonne.value.findIndex(c => c.id === col.id)
  let cum = 0
  for (let j = 0; j < colonne.value.length; j++) {
    cum += applicato(colonne.value[j], uid)
    if (j >= idx && cum + delta > colonne.value[j].maxRank) return false
  }
  return true
}

async function carica() {
  loading.value = true
  errore.value = ''
  try {
    await characterStore.fetchCharacter(props.idPersonaggio, true)
    const livelli = characterStore.cache[props.idPersonaggio]?.items?.livelli ?? []
    // i campi gradi/classeId esistono solo nella versione aggiornata del backend
    avvisoBackend.value = livelli.length > 0 && !('gradi' in (livelli[0] as any))
    const baseColonne = [...livelli]
        .map((l: any) => ({
          id: l.id,
          livello: Number(l.livello) || 0,
          classe: l.classe ?? '',
          classeId: (l.classeId ?? null) as number | null,
          gradi: (l.gradi ?? null) as number | null,
        }))
        .sort((a, b) => a.livello - b.livello)

    const res = await getListaAbilitaPerPersonaggio(props.idPersonaggio)
    const abilita: Abilita[] = res.data ?? []

    righe.value = abilita
        .map(a => ({uid: String(a.abilita?.id ?? ''), nome: a.abilita?.nome ?? ''}))
        .filter(r => r.uid)
        .sort((a, b) => a.nome.localeCompare(b.nome))

    // pre-valorizza la matrice dai rank esistenti (sommati per livello)
    for (const a of abilita) {
      const uid = String(a.abilita?.id ?? '')
      if (!uid) continue
      model[uid] = {}
      const ranks = a.rank?.ranks ?? []
      for (const c of baseColonne) {
        model[uid][c.id] = Math.max(0, ranks
            .filter(rk => rk.itemId === c.id)
            .reduce((s, rk) => s + (rk.valore || 0), 0))
      }
    }
    aggiornaSnapshot()

    // budget = valore congelato GRADI_LIVELLO (0 se non impostato);
    // cap per-abilità = livello + 3; abilità di classe dal backend (per il verde).
    colonne.value = await Promise.all(baseColonne.map(async (c): Promise<Colonna> => {
      const classSet = new Set<string>()
      if (c.classeId != null) {
        try {
          const abiRes = await getAbilitaClasseByPersonaggioLivelloClasse(props.idPersonaggio, c.livello, c.classeId)
          const ac = (abiRes.data ?? []) as AbilitaClasse[]
          // di classe (verde) = diClasse || all ; trasversali incluse, pure cross escluse
          ac.filter(x => x.diClasse || x.all).forEach(x => classSet.add(String(x.id).toLowerCase()))
        } catch (e) {
          console.error('Errore abilità di classe livello', c.livello, e)
        }
      }
      return {
        id: c.id,
        livello: c.livello,
        classe: c.classe,
        budget: c.gradi ?? 0,
        maxRank: c.livello + 3,
        classSet,
      }
    }))
  } catch (e) {
    console.error('Errore caricamento gradi:', e)
    errore.value = 'Errore nel caricamento dei dati.'
  } finally {
    loading.value = false
  }
}

async function salva() {
  if (saving.value) return
  saving.value = true
  errore.value = ''
  messaggio.value = ''
  try {
    const daSalvare = colonne.value.filter(colonnaModificata)
    if (daSalvare.length === 0) {
      messaggio.value = 'Nessuna modifica da salvare.'
      return
    }
    // un'unica chiamata: tutti i livelli modificati persistiti insieme (transazione)
    const livelli = daSalvare.map(c => ({
      livelloId: c.id,
      ranghi: righe.value
          .map(r => ({abilitaId: r.uid, punti: punti(r.uid, c.id)}))
          .filter(x => x.punti > 0),
    }))
    await saveRanghiBulk(props.idPersonaggio, livelli)
    aggiornaSnapshot()
    await characterStore.fetchCharacter(props.idPersonaggio, true)
    messaggio.value = `Salvati ${daSalvare.length} livell${daSalvare.length === 1 ? 'o' : 'i'}.`
  } catch (e) {
    console.error('Errore salvataggio gradi:', e)
    errore.value = 'Errore nel salvataggio.'
  } finally {
    saving.value = false
  }
}

function indietro() {
  router.push(`/scheda/${props.idPersonaggio}`)
}

const vuoto = computed(() => !loading.value && (colonne.value.length === 0 || righe.value.length === 0))

onMounted(carica)
</script>

<template>
  <div class="gestisci-gradi">
    <div class="gg-head">
      <button type="button" class="btn ghost" @click="indietro">‹ Indietro</button>
      <h2 class="gg-title">Gestisci gradi</h2>
      <button type="button" class="btn primary" :disabled="saving || loading || vuoto" @click="salva">
        {{ saving ? 'Salvataggio…' : 'Salva' }}
      </button>
    </div>

    <div v-if="!loading && !vuoto" class="gg-tools">
      <button type="button" class="btn small" :disabled="saving || !modificato" @click="ripristina">
        Ripristina
      </button>
      <button type="button" class="btn small danger" :disabled="saving" @click="azzeraTutto">
        Azzera tutto
      </button>
    </div>

    <p v-if="avvisoBackend" class="gg-msg warn">
      Il backend in esecuzione non restituisce ancora i gradi per livello: riavvialo per
      abilitare budget e abilità di classe. (Senza, il budget è 0 e non si possono assegnare gradi.)
    </p>
    <p v-if="messaggio" class="gg-msg ok">{{ messaggio }}</p>
    <p v-if="errore" class="gg-msg err">{{ errore }}</p>

    <p v-if="loading" class="gg-info">Caricamento…</p>
    <p v-else-if="vuoto" class="gg-info">Nessun livello o abilità disponibile.</p>

    <div v-else class="gg-table-wrap">
      <table class="gg-table">
        <thead>
          <tr>
            <th class="sticky-col abil-col">Abilità</th>
            <th v-for="c in colonne" :key="c.id" class="lvl-col" :title="c.classe">
              <div class="lvl-head">
                <span>Lv {{ c.livello }}</span>
                <span class="lvl-max">gradi {{ c.budget }}</span>
              </div>
            </th>
            <th class="tot-col">Tot</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in righeNormali" :key="r.uid">
            <td class="sticky-col abil-col" :title="r.nome">{{ r.nome }}</td>
            <td
                v-for="c in colonne" :key="c.id"
                class="cell" :class="{ classe: isClasse(c, r.uid) }"
            >
              <div class="stepper">
                <button type="button" class="step" @click="dec(r.uid, c)" :disabled="punti(r.uid, c.id) <= 0">−</button>
                <span class="val">{{ fmt(applicato(c, r.uid)) }}</span>
                <button type="button" class="step" @click="inc(r.uid, c)" :disabled="!canInc(c, r.uid)">+</button>
              </div>
            </td>
            <td class="tot-col">{{ fmt(totaleRiga(r.uid)) }}</td>
          </tr>
        </tbody>
        <tfoot>
          <tr>
            <td class="sticky-col abil-col">Punti spesi</td>
            <td v-for="c in colonne" :key="c.id" class="tot-col" :class="{ over: sforato(c) }">
              {{ totaleColonna(c) }}<span class="slash">/{{ c.budget }}</span>
            </td>
            <td class="tot-col"></td>
          </tr>
        </tfoot>
      </table>

      <!-- Professioni: a parte, non contano nel budget né nel conteggio -->
      <template v-if="righeProfessioni.length">
        <h3 class="prof-title">Professioni</h3>
        <table class="gg-table">
          <thead>
            <tr>
              <th class="sticky-col abil-col">Professione</th>
              <th v-for="c in colonne" :key="c.id" class="lvl-col">
                <div class="lvl-head">
                  <span>Lv {{ c.livello }}</span>
                  <span class="lvl-max">libere</span>
                </div>
              </th>
              <th class="tot-col">Tot</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in righeProfessioni" :key="r.uid">
              <td class="sticky-col abil-col" :title="r.nome">{{ r.nome }}</td>
              <td
                  v-for="c in colonne" :key="c.id"
                  class="cell" :class="{ classe: isClasse(c, r.uid) }"
              >
                <div class="stepper">
                  <button type="button" class="step" @click="dec(r.uid, c)" :disabled="punti(r.uid, c.id) <= 0">−</button>
                  <span class="val">{{ fmt(applicato(c, r.uid)) }}</span>
                  <button type="button" class="step" @click="inc(r.uid, c)" :disabled="!canInc(c, r.uid)">+</button>
                </div>
              </td>
              <td class="tot-col">{{ fmt(totaleRiga(r.uid)) }}</td>
            </tr>
          </tbody>
        </table>
      </template>
    </div>
  </div>
</template>

<style scoped>
.gestisci-gradi {
  padding: .6rem;
  display: flex;
  flex-direction: column;
  gap: .6rem;
}

.gg-head { display: flex; align-items: center; gap: .6rem; }
.gg-title { margin: 0; font-size: 1.1rem; flex: 1; text-align: center; }

.btn {
  padding: .45rem .8rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  background: #fff;
  font-weight: 700;
  font-size: .85rem;
  cursor: pointer;
  white-space: nowrap;
}
.btn.primary { border-color: #2563eb; background: #2563eb; color: #fff; }
.btn.primary:hover { background: #1d4ed8; }
.btn.ghost { color: #334155; }
.btn:disabled { opacity: .55; cursor: default; }

.btn.small { padding: .35rem .7rem; font-size: .8rem; }
.btn.danger { border-color: #ef4444; color: #b91c1c; }
.btn.danger:hover:not(:disabled) { background: #fef2f2; }

.gg-tools { display: flex; gap: .5rem; }

.gg-msg { margin: 0; font-size: .85rem; font-weight: 600; }
.gg-msg.ok { color: #15803d; }
.gg-msg.err { color: #b91c1c; }
.gg-msg.warn {
  color: #92400e;
  background: #fef3c7;
  border: 1px solid #fde68a;
  border-radius: .5rem;
  padding: .5rem .7rem;
  font-weight: 600;
}
.gg-info { margin: .4rem 0; opacity: .7; }

.gg-table-wrap {
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
}

.gg-table { border-collapse: collapse; width: 100%; font-size: .85rem; }

.gg-table th, .gg-table td {
  border: 1px solid #eef2f7;
  padding: .3rem .4rem;
  text-align: center;
}

.gg-table thead th {
  background: #f8fafc;
  font-weight: 700;
  position: sticky;
  top: 0;
  z-index: 1;
}

.lvl-head { display: flex; flex-direction: column; line-height: 1.1; }
.lvl-max { font-size: .68rem; font-weight: 600; color: #64748b; }

/* larghezza fissa così le due tabelle (abilità e professioni) restano allineate */
.abil-col {
  text-align: left;
  width: 9rem;
  min-width: 9rem;
  max-width: 9rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.prof-title {
  margin: .8rem 0 .3rem;
  font-size: .95rem;
  color: #475569;
}

.sticky-col { position: sticky; left: 0; background: #fff; z-index: 2; }
.gg-table thead .sticky-col { z-index: 3; background: #f8fafc; }

.lvl-col { min-width: 4.2rem; }
.tot-col { min-width: 2.8rem; font-weight: 700; background: #f9fafb; }

/* verde = abilità di classe (incl. trasversali), bianco = cross */
.cell { background: #fff; }
.cell.classe { background: #dcfce7; }

.stepper { display: inline-flex; align-items: center; gap: .2rem; }

.step {
  width: 1.3rem;
  height: 1.3rem;
  border: 1px solid #cbd5e1;
  border-radius: .3rem;
  background: #fff;
  font-weight: 800;
  line-height: 1;
  cursor: pointer;
}
.step:hover { background: #eff6ff; border-color: #2563eb; }
.step:disabled { opacity: .4; cursor: default; }

.val { min-width: 1.6rem; font-variant-numeric: tabular-nums; font-weight: 700; }

.slash { color: #94a3b8; font-weight: 600; }
.tot-col.over { color: #b91c1c; }
.tot-col.over .slash { color: #fca5a5; }
</style>
