<script setup lang="ts">
import {computed, markRaw, onMounted, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useCharacterStore} from '../../../../../../stores/personaggio'
import {
  getAbilitaClasseByPersonaggioLivelloClasse,
  getCompendio,
  getListaAbilitaPerPersonaggio,
  saveRanghiBulk,
} from '../../../../../../service/PersonaggioService'
import {Abilita} from '../../../../../../models/dto/Abilita'
import {AbilitaClasse} from '../../../../../../models/dto/AbilitaClasse'
import TabExpandable from '../../../../../../components/TabExpandable.vue'
import usePopup from '../../../../../../function/usePopup'
import Mobile_DettaglioItem from '../../Dettaglio/Mobile_DettaglioItem.vue'

const props = defineProps<{ idPersonaggio: number }>()
const router = useRouter()
const characterStore = useCharacterStore()
const {openPopup} = usePopup()

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
// larghezza della colonna nome, calcolata sul nome più lungo tra TUTTE le righe (abilità,
// conoscenze, intrattenere, artigianato, professioni) così ogni tabella ha la stessa larghezza
// e nessun nome viene troncato.
const abilColWidth = computed(() => {
  const maxLenAbilita = righe.value.reduce((m, r) => Math.max(m, r.nome.length), 0)
  // gli Skill Trick hanno anche il pulsante "info": qualche carattere in più di margine
  const maxLenTrick = righeSkillTrick.value.reduce((m, r) => Math.max(m, r.nome.length + 3), 0)
  const maxLen = Math.max(maxLenAbilita, maxLenTrick)
  return maxLen > 0 ? `${maxLen + 1}ch` : '9rem'
})
// model[uid][livelloId] = punti rank spesi (valore grezzo salvato)
const model = reactive<Record<string, Record<number, number>>>({})
// stato iniziale (dopo il prefill) per salvare SOLO i livelli modificati
let snapshot: Record<string, Record<number, number>> = {}

function aggiornaSnapshot() {
  snapshot = {}
  for (const uid of Object.keys(model)) snapshot[uid] = {...model[uid]}
}

// un livello (colonna) è modificato se almeno un'abilità (o uno Skill Trick) differisce dallo snapshot
function colonnaModificata(col: Colonna): boolean {
  return righe.value.some(r => (model[r.uid]?.[col.id] ?? 0) !== (snapshot[r.uid]?.[col.id] ?? 0))
      || stColonnaModificata(col)
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
  for (const r of righeSkillTrick.value) for (const c of colonne.value) setStPunti(r.itemId, c.id, 0)
}

// ripristina la situazione di quando si è aperta la pagina (snapshot iniziale)
function ripristina() {
  for (const r of righe.value) for (const c of colonne.value) {
    setPunti(r.uid, c.id, snapshot[r.uid]?.[c.id] ?? 0)
  }
  for (const r of righeSkillTrick.value) for (const c of colonne.value) {
    setStPunti(r.itemId, c.id, stSnapshot[r.itemId]?.[c.id] ?? 0)
  }
}

const modificato = computed(() => colonne.value.some(colonnaModificata))

const isClasse = (col: Colonna, uid: string) => col.classSet.has(uid.toLowerCase())

// le professioni (stat id che inizia con "PR") sono a parte: non consumano il
// budget del livello, non rientrano nel conteggio e non hanno concetto di "abilità
// di classe" — 1 punto investito vale sempre 1 grado, punto e basta.
const isProfessione = (uid: string) => uid.toUpperCase().startsWith('PR')
// La stat unica "TRICK" (Skill Trick) non va mostrata come riga normale: la sua ripartizione
// per singolo trucco vive nella sezione dedicata più sotto (vedi righeSkillTrick/stModel),
// che scrive più modificatori RANK sulla stessa stat distinti dal campo "nota" (id item). Una
// riga generica qui scriverebbe invece un unico modificatore senza nota, in conflitto.
const STAT_SKILL_TRICK = 'TRICK'
const righeNormali = computed(() => righe.value.filter(r => !isProfessione(r.uid)))
const righeProfessioni = computed(() => righe.value.filter(r => isProfessione(r.uid)))

// Famiglie che condividono il budget del livello (Abilità/Conoscenze/Intrattenere/Artigianato),
// distinte solo per raggruppare la visualizzazione — stesso pool di punti, stesso concetto di
// abilità di classe. Le Professioni (PR) restano a parte (vedi sopra).
type Famiglia = 'AB' | 'CO' | 'IN' | 'AR'
const FAMIGLIA_LABEL: Record<Famiglia, string> = {AB: 'Abilità', CO: 'Conoscenze', IN: 'Intrattenere', AR: 'Artigianato'}
const FAMIGLIE_ORDINATE: Famiglia[] = ['AB', 'CO', 'IN', 'AR']

function famigliaDi(uid: string): Famiglia {
  const id = uid.toUpperCase()
  if (id.startsWith('CO')) return 'CO'
  if (id.startsWith('IN')) return 'IN'
  if (id.startsWith('AR')) return 'AR'
  return 'AB'
}

// righe normali raggruppate per famiglia, solo per la visualizzazione (una tabella per gruppo)
const righePerFamiglia = computed(() =>
    FAMIGLIE_ORDINATE
        .map(f => ({famiglia: f, label: FAMIGLIA_LABEL[f], righe: righeNormali.value.filter(r => famigliaDi(r.uid) === f)}))
        .filter(g => g.righe.length > 0))

// valore applicato: le professioni sono sempre 1:1 (nessun concetto di classe/cross);
// per le altre, pieno se abilità di classe, metà se trasversale/cross
function applicato(col: Colonna, uid: string): number {
  const p = punti(uid, livId(col))
  if (isProfessione(uid)) return p
  return isClasse(col, uid) ? p : p / 2
}

const livId = (col: Colonna) => col.id
const fmt = (n: number) => Number.isInteger(n) ? String(n) : n.toFixed(1)

// totale punti spesi a quel livello (abilità non-professione + Skill Trick, confronto col budget:
// gli Skill Trick condividono lo stesso pool di punti abilità del livello)
const totaleColonna = (col: Colonna) =>
    righeNormali.value.reduce((s, r) => s + punti(r.uid, col.id), 0)
    + righeSkillTrick.value.reduce((s, r) => s + stPunti(r.itemId, col.id), 0)
const sforato = (col: Colonna) => totaleColonna(col) > col.budget
// somma dei valori APPLICATI per riga (effetto totale sull'abilità)
const totaleRiga = (uid: string) =>
    colonne.value.reduce((s, c) => s + applicato(c, uid), 0)

// si può incrementare se: c'è budget al livello E il cap per-abilità
// (gradi max = livello+3, cumulativo per livello) non viene superato.
function canInc(col: Colonna, uid: string): boolean {
  // le professioni non consumano il budget del livello
  if (!isProfessione(uid) && totaleColonna(col) >= col.budget) return false
  const delta = isProfessione(uid) ? 1 : (isClasse(col, uid) ? 1 : 0.5)
  const idx = colonne.value.findIndex(c => c.id === col.id)
  let cum = 0
  for (let j = 0; j < colonne.value.length; j++) {
    cum += applicato(colonne.value[j], uid)
    if (j >= idx && cum + delta > colonne.value[j].maxRank) return false
  }
  return true
}

// ---------- Skill Trick (Complete Scoundrel) ----------
// Un'unica stat reale "TRICK" in stats/stat_default (nessuna riga per singolo trucco): i punti di
// ogni trucco vengono salvati come modificatori RANK su quella stat, distinti dal campo "nota"
// (= id dell'item SKILL_TRICK nel compendio). Qui in UI si mostra 1 riga per trucco, cap 2 punti
// ciascuna; al raggiungimento di 2 il backend collega automaticamente l'item al livello.
interface RigaSkillTrick {
  itemId: number
  nome: string
}

const SKILL_TRICK_MAX = 2
const righeSkillTrick = ref<RigaSkillTrick[]>([])
// stModel[itemId][livelloId] = punti assegnati a quel trucco a quel livello
const stModel = reactive<Record<number, Record<number, number>>>({})
let stSnapshot: Record<number, Record<number, number>> = {}

function aggiornaStSnapshot() {
  stSnapshot = {}
  for (const itemId of Object.keys(stModel)) stSnapshot[Number(itemId)] = {...stModel[Number(itemId)]}
}

const stPunti = (itemId: number, livId: number): number => stModel[itemId]?.[livId] ?? 0

function setStPunti(itemId: number, livId: number, v: number) {
  const n = Math.max(0, Math.floor(v) || 0)
  if (!stModel[itemId]) stModel[itemId] = {}
  stModel[itemId][livId] = n
}

const stTotaleRiga = (itemId: number) =>
    colonne.value.reduce((s, c) => s + stPunti(itemId, c.id), 0)

function stCanInc(col: Colonna, itemId: number): boolean {
  if (totaleColonna(col) >= col.budget) return false
  return stTotaleRiga(itemId) < SKILL_TRICK_MAX
}

function stInc(itemId: number, col: Colonna) {
  if (!stCanInc(col, itemId)) return
  setStPunti(itemId, col.id, stPunti(itemId, col.id) + 1)
}
const stDec = (itemId: number, col: Colonna) => setStPunti(itemId, col.id, stPunti(itemId, col.id) - 1)

function stColonnaModificata(col: Colonna): boolean {
  return righeSkillTrick.value.some(r => (stModel[r.itemId]?.[col.id] ?? 0) !== (stSnapshot[r.itemId]?.[col.id] ?? 0))
}

// popup di sola lettura con la descrizione (prerequisito/beneficio) dello Skill Trick, stesso
// componente/pattern usato per il dettaglio degli item altrove (es. TabContenutiLivello.vue)
function mostraDescrizioneSkillTrick(r: RigaSkillTrick) {
  const personaggio = characterStore.cache[props.idPersonaggio]
  openPopup(
      markRaw(Mobile_DettaglioItem),
      {
        data: {
          item: {id: r.itemId, nome: r.nome, tipo: 'SKILL_TRICK'},
          personaggio,
        },
        readonly: true,
      },
      {closable: true, title: r.nome},
  )
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
        .filter(a => a.abilita?.rankable !== false)
        .filter(a => String(a.abilita?.id ?? '').toUpperCase() !== STAT_SKILL_TRICK)
        .map(a => ({uid: String(a.abilita?.id ?? ''), nome: a.abilita?.nome ?? ''}))
        .filter(r => r.uid)
        .sort((a, b) => a.nome.localeCompare(b.nome))

    // pre-valorizza la matrice dai rank esistenti (sommati per livello)
    for (const a of abilita) {
      const uid = String(a.abilita?.id ?? '')
      if (!uid || uid.toUpperCase() === STAT_SKILL_TRICK) continue
      model[uid] = {}
      const ranks = a.rank?.ranks ?? []
      for (const c of baseColonne) {
        model[uid][c.id] = Math.max(0, ranks
            .filter(rk => rk.itemId === c.id)
            .reduce((s, rk) => s + (rk.valore || 0), 0))
      }
    }
    aggiornaSnapshot()

    // Skill Trick: lista dal compendio (item di tipo SKILL_TRICK) + prefill dei punti già
    // spesi per livello (LivelloDTO.skillTrick, popolato dal backend dai modificatori con nota).
    try {
      const stRes = await getCompendio({tipo: 'SKILL_TRICK', size: 50})
      righeSkillTrick.value = (stRes.data?.content ?? [])
          .map((i: any) => ({itemId: i.id, nome: i.nome ?? ''}))
          .sort((a, b) => a.nome.localeCompare(b.nome))
    } catch (e) {
      console.error('Errore caricamento Skill Trick:', e)
      righeSkillTrick.value = []
    }
    for (const r of righeSkillTrick.value) stModel[r.itemId] = {}
    for (const l of livelli as any[]) {
      const punti = l.skillTrick ?? {}
      for (const [itemIdStr, val] of Object.entries(punti)) {
        const itemId = Number(itemIdStr)
        if (!stModel[itemId]) stModel[itemId] = {}
        stModel[itemId][l.id] = Number(val) || 0
      }
    }
    aggiornaStSnapshot()

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
      skillTrick: righeSkillTrick.value
          .map(r => ({itemId: r.itemId, punti: stPunti(r.itemId, c.id)}))
          .filter(x => x.punti > 0),
    }))
    await saveRanghiBulk(props.idPersonaggio, livelli)
    aggiornaSnapshot()
    aggiornaStSnapshot()
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

    <div v-else class="gg-table-wrap" :style="{ '--abil-col-w': abilColWidth }">
      <!-- Abilità/Conoscenze/Intrattenere/Artigianato: una tabella per famiglia, stesso budget condiviso -->
      <template v-for="grp in righePerFamiglia" :key="grp.famiglia">
        <h3 class="fam-title">{{ grp.label }}</h3>
        <table class="gg-table">
          <thead>
            <tr>
              <th class="sticky-col abil-col">{{ grp.label }}</th>
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
            <tr v-for="r in grp.righe" :key="r.uid">
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

      <!-- Skill Trick (Complete Scoundrel): condivide il budget punti del livello con le abilità
           sopra, ma ogni trucco ha un cap fisso di 2 punti invece del cap livello+3. Sezione
           apribile/chiudibile perché la lista è lunga; al raggiungimento di 2 punti il backend
           collega automaticamente l'item corrispondente al personaggio. -->
      <TabExpandable v-if="righeSkillTrick.length" title="Skill Trick" :default-open="false">
        <template #summary>
          {{ righeSkillTrick.filter(r => stTotaleRiga(r.itemId) >= SKILL_TRICK_MAX).length }}/{{ righeSkillTrick.length }} sbloccati
        </template>
        <template #content>
          <table class="gg-table">
            <thead>
              <tr>
                <th class="sticky-col abil-col">Skill Trick</th>
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
              <tr v-for="r in righeSkillTrick" :key="r.itemId">
                <td class="sticky-col abil-col" :title="r.nome">
                  <div class="st-name-row">
                    <span class="st-name">{{ r.nome }}</span>
                    <button type="button" class="btn-info" title="Vedi descrizione" @click="mostraDescrizioneSkillTrick(r)">ℹ</button>
                  </div>
                </td>
                <td v-for="c in colonne" :key="c.id" class="cell">
                  <div class="stepper">
                    <button type="button" class="step" @click="stDec(r.itemId, c)" :disabled="stPunti(r.itemId, c.id) <= 0">−</button>
                    <span class="val">{{ fmt(stPunti(r.itemId, c.id)) }}</span>
                    <button type="button" class="step" @click="stInc(r.itemId, c)" :disabled="!stCanInc(c, r.itemId)">+</button>
                  </div>
                </td>
                <td class="tot-col" :class="{ classe: stTotaleRiga(r.itemId) >= SKILL_TRICK_MAX }">{{ fmt(stTotaleRiga(r.itemId)) }}</td>
              </tr>
            </tbody>
          </table>
        </template>
      </TabExpandable>

      <!-- Riepilogo punti: budget condiviso da Abilità/Conoscenze/Intrattenere/Artigianato/Skill Trick -->
      <table v-if="righePerFamiglia.length" class="gg-table gg-summary">
        <tbody>
          <tr>
            <td class="sticky-col abil-col">Punti spesi</td>
            <td v-for="c in colonne" :key="c.id" class="lvl-col spesi-cell" :class="{ over: sforato(c) }">
              {{ totaleColonna(c) }}<span class="slash">/{{ c.budget }}</span>
            </td>
            <td class="tot-col"></td>
          </tr>
        </tbody>
      </table>

      <!-- Professioni: a parte, non contano nel budget e non hanno concetto di abilità di
           classe — 1 punto investito vale sempre 1 grado. -->
      <template v-if="righeProfessioni.length">
        <h3 class="fam-title">Professioni</h3>
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
              <td v-for="c in colonne" :key="c.id" class="cell">
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
  border: 1px solid var(--hairline);
  border-radius: .5rem;
  background: var(--surface-0);
  font-weight: 700;
  font-size: .85rem;
  cursor: pointer;
  white-space: nowrap;
}
.btn.primary { border-color: #2563eb; background: #2563eb; color: #fff; }
.btn.primary:hover { background: #1d4ed8; }
.btn.ghost { color: var(--text-muted); }
.btn:disabled { opacity: .55; cursor: default; }

.btn.small { padding: .35rem .7rem; font-size: .8rem; }
.btn.danger { border-color: var(--danger-border); color: var(--danger-text); }
.btn.danger:hover:not(:disabled) { background: var(--danger-bg); }

.gg-tools { display: flex; gap: .5rem; }

.gg-msg { margin: 0; font-size: .85rem; font-weight: 600; }
.gg-msg.ok { color: var(--success-text); }
.gg-msg.err { color: var(--danger-text); }
.gg-msg.warn {
  color: var(--warning-text);
  background: var(--warning-bg);
  border: 1px solid var(--warning-border);
  border-radius: .5rem;
  padding: .5rem .7rem;
  font-weight: 600;
}
.gg-info { margin: .4rem 0; opacity: .7; }

.gg-table-wrap {
  overflow-x: auto;
  border: 1px solid var(--hairline);
  border-radius: .6rem;
}

/* table-layout: fixed è necessario perché la tabella "Punti spesi" (riepilogo) e quella delle
   Professioni sono elementi <table> SEPARATI dalle tabelle per famiglia: con il layout automatico
   di default ognuna calcola le larghezze colonna in base al proprio contenuto (es. "3/5" nel
   riepilogo è diverso dagli stepper sopra), disallineando le colonne tra una tabella e l'altra.
   Con layout fisso le larghezze sono quelle dichiarate su th/td (vedi .abil-col/.lvl-col/.tot-col),
   identiche in ogni tabella perché condividono le stesse classi. */
.gg-table { border-collapse: collapse; width: 100%; font-size: .85rem; table-layout: fixed; }

.gg-table th, .gg-table td {
  border: 1px solid var(--table-border);
  padding: .3rem .4rem;
  text-align: center;
}

.gg-table thead th {
  background: var(--primary-color);
  font-weight: 700;
  position: sticky;
  top: 0;
  z-index: 1;
}

.lvl-head { display: flex; flex-direction: column; line-height: 1.1; }
.lvl-max { font-size: .68rem; font-weight: 600; color: var(--text-muted); }

/* larghezza calcolata sul nome più lungo tra tutte le tabelle (--abil-col-w, impostata
   dinamicamente), così ogni tabella resta allineata e nessun nome viene troncato */
.abil-col {
  text-align: left;
  width: var(--abil-col-w, 9rem);
  min-width: var(--abil-col-w, 9rem);
  max-width: var(--abil-col-w, 9rem);
  white-space: nowrap;
}

.fam-title {
  margin: .8rem 0 .3rem;
  font-size: .95rem;
  color: var(--text-muted);
}
.fam-title:first-child { margin-top: 0; }

.st-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: .3rem;
}
.st-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.btn-info {
  flex-shrink: 0;
  width: 1.4rem; height: 1.4rem; padding: 0;
  border: 1px solid var(--info-border); background: var(--info-bg); color: var(--info-text);
  border-radius: .4rem; cursor: pointer; font-size: .75rem; line-height: 1;
}
.btn-info:hover { background: var(--info-border); }

.gg-summary { margin-top: -1px; }
.gg-summary .abil-col { font-weight: 700; }

.sticky-col { position: sticky; left: 0; background: var(--surface-0); z-index: 2; }
.gg-table thead .sticky-col { z-index: 3; background: var(--primary-color); }

/* con table-layout: fixed questa è la larghezza REALE della colonna (non solo un minimo): deve
   contenere lo stepper (2 bottoni da 1.3rem + gap + valore) più il padding della cella, quindi
   più larga del min-width usato prima con il layout automatico. */
.lvl-col { width: 5.4rem; }
.tot-col { width: 3.2rem; font-weight: 700; background: var(--btn-bg); }
/* riga "Punti spesi": stessa LARGHEZZA delle colonne livello sopra (.lvl-col), ma stile visivo
   da colonna-riepilogo (grassetto + sfondo) come .tot-col — da qui due classi separate invece
   di riusare .tot-col anche per la larghezza, che la faceva più stretta e disallineata. */
.spesi-cell { font-weight: 700; background: var(--btn-bg); }

/* verde = abilità di classe (incl. trasversali), bianco = cross */
.cell { background: var(--surface-0); }
.cell.classe { background: var(--success-bg); }
/* Skill Trick sbloccato (2/2 punti): stesso verde usato per "abilità di classe" sopra */
.tot-col.classe { background: var(--success-bg); }

.stepper { display: inline-flex; align-items: center; gap: .2rem; }

.step {
  width: 1.3rem;
  height: 1.3rem;
  border: 1px solid var(--hairline);
  border-radius: .3rem;
  background: var(--surface-0);
  font-weight: 800;
  line-height: 1;
  cursor: pointer;
}
.step:hover { background: #eff6ff; border-color: #2563eb; }
.step:disabled { opacity: .4; cursor: default; }

.val { min-width: 1.6rem; font-variant-numeric: tabular-nums; font-weight: 700; }

.slash { color: var(--text-muted); font-weight: 600; }
.tot-col.over, .spesi-cell.over { color: var(--danger-text); }
.tot-col.over .slash, .spesi-cell.over .slash { color: var(--danger-border); }
</style>
