<script setup lang="ts">
import {computed, markRaw, ref, watch} from 'vue';
import {storeToRefs} from "pinia";
import {useCharacterStore} from "../../../../../../stores/personaggio";
import Mobile_Stat from "../../Shared/Mobile_Stat.vue";
import Mobile_HP from "../../Shared/Mobile_HP.vue";
import Mobile_Contatore from "../../Shared/Mobile_Contatore.vue";
import Tabella from "../../../../../../components/Tabella.vue";
import Mobile_DettaglioItem from "../../Dettaglio/Mobile_DettaglioItem.vue";
import {getItem, switchItemState, updatePersonaggioInfo} from "../../../../../../service/PersonaggioService";
import usePopup from "../../../../../../function/usePopup";
import useDiceRoll from "../../../../../../function/useDiceRoll";
import {testoTaglia} from "../../../../../../function/Utils";
import SearchSelect from "../../../../../../components/SearchSelect.vue";
import PesoDettaglioPopup from "./PesoDettaglioPopup.vue";

const characterStore = useCharacterStore()
const {cache} = storeToRefs(characterStore);

// durante il tiro globale del d20 il BAB sparisce: è già incluso in
// Lotta/Mischia/Distanza, che ricevono la somma sulle varie parti.
const {risultato} = useDiceRoll()

const props = defineProps({
  idPersonaggio: {type: Number, required: true}
});

// ── Frutti: carica i children TRASFORMAZIONE+FORMA via API (una volta sola per set di frutti) ──
// Mappa frutto.id → array di {id, tipo} dei children rilevanti
const fruttoChildMap = ref<Map<number, {id: number, tipo: string}[]>>(new Map())

const TIPI_FRUTTO_CHILD = new Set(['TRASFORMAZIONE', 'FORMA'])

const fruttiSignature = computed(() =>
    (cache.value[props.idPersonaggio]?.items?.frutti ?? [])
        .map((f: any) => f.id).sort().join(',')
)

watch(fruttiSignature, async (sig) => {
  if (!sig) { fruttoChildMap.value = new Map(); return }
  const frutti: any[] = cache.value[props.idPersonaggio]?.items?.frutti ?? []
  const map = new Map<number, {id: number, tipo: string}[]>()
  for (const frutto of frutti) {
    try {
      const res = await getItem(frutto.id)
      const figli = (res.data.child ?? [])
          .filter((c: any) => TIPI_FRUTTO_CHILD.has(c.itemTarget?.tipo))
          .map((c: any) => ({id: c.itemTarget.id as number, tipo: c.itemTarget.tipo as string}))
      map.set(frutto.id, figli)
    } catch { map.set(frutto.id, []) }
  }
  fruttoChildMap.value = map
}, {immediate: true})

// Set piatto di tutti gli ID child (per escluderli dalle liste indipendenti)
const fruttiChildIds = computed<Set<number>>(() => {
  const s = new Set<number>()
  fruttoChildMap.value.forEach(figli => figli.forEach(f => s.add(f.id)))
  return s
})

// Frutti con stato live, separando trasformazioni da forme
const fruttiConFigli = computed(() => {
  const items = cache.value[props.idPersonaggio]?.items
  const frutti: any[] = items?.frutti ?? []
  const trsfLive: any[] = items?.trasformazioni ?? []
  const formeLive: any[] = items?.forme ?? []
  return frutti.map(frutto => {
    const childDefs = fruttoChildMap.value.get(frutto.id) ?? []
    const trasf = childDefs
        .filter(c => c.tipo === 'TRASFORMAZIONE')
        .map(c => trsfLive.find(t => t.id === c.id))
        .filter(Boolean)
        .sort((a: any, b: any) => a.nome.localeCompare(b.nome))
    const forme = childDefs
        .filter(c => c.tipo === 'FORMA')
        .map(c => formeLive.find(f => f.id === c.id))
        .filter(Boolean)
        .sort((a: any, b: any) => a.nome.localeCompare(b.nome))
    return {frutto, trasf, forme}
  })
})

// ── Trasformazioni indipendenti (non figlie di nessun frutto), raggruppate per gruppo ──
const itemsTrasformazioni = computed(() => {
  const items = cache.value[props.idPersonaggio]?.items
  return (items?.trasformazioni ?? [])
      .filter((t: any) => !fruttiChildIds.value.has(t.id))
      .map((itm: any) => ({
        ...itm,
        expandedComponent: markRaw(Mobile_DettaglioItem),
        expandedProps: {data: {item: {...itm}, personaggio: cache.value[props.idPersonaggio]}}
      }))
      .sort((a: any, b: any) => a.nome.localeCompare(b.nome))
})

const gruppiTrasformazioni = computed(() => {
  const map: Record<string, any[]> = {}
  itemsTrasformazioni.value.forEach((itm: any) => {
    const key = itm.gruppo ?? 'Senza gruppo'
    if (!map[key]) map[key] = []
    map[key].push(itm)
  })
  return map
})


// ── Toggle trasformazione/forma ──
// siblings: lista entro cui cercare lo stesso gruppo per la mutua esclusione.
// Se non fornita (trasformazioni indipendenti) usa tutte trasf+forme del personaggio.
const toggling = ref(false)
async function toggleTrasf(trasf: any, siblings?: any[]) {
  if (toggling.value) return
  toggling.value = true
  try {
    const items = cache.value[props.idPersonaggio]?.items
    const scope: any[] = siblings ?? [
      ...(items?.trasformazioni ?? []),
      ...(items?.forme ?? []),
    ]
    const toSwitch: number[] = []
    if (trasf.disabled) {
      // attivare: prima disabilita le altre attive dello stesso gruppo nel scope
      scope
          .filter(t => !t.disabled && t.gruppo === trasf.gruppo)
          .forEach(t => toSwitch.push(t.id))
    }
    toSwitch.push(trasf.id)
    for (const id of toSwitch) {
      await switchItemState(id, props.idPersonaggio)
    }
    await characterStore.fetchCharacter(props.idPersonaggio, true)
  } finally {
    toggling.value = false
  }
}

// ── Popup info trasformazione ──
const {openPopup} = usePopup()

function openInfoTrasf(trasf: any) {
  const personaggio = cache.value[props.idPersonaggio]
  openPopup(
      Mobile_DettaglioItem,
      {
        data: {
          item: {id: trasf.id, nome: trasf.nome, tipo: trasf.tipo, disabled: trasf.disabled},
          personaggio: {
            modificatori: {id: props.idPersonaggio},
            items: {
              trasformazioni: personaggio?.items?.trasformazioni ?? [],
              idoli: personaggio?.items?.idoli ?? [],
            },
          },
        }
      },
      {closable: true, autoClose: 0}
  )
}

// Rimuove prefissi tipo "NCARATTERI: " dal nome (solo per il badge esterno)
function strippaPrefisso(nome: string): string {
  return nome.replace(/^\w+:\s*/, '')
}

// ── Apertura/chiusura card frutto ──
const openFrutti = ref<Set<number>>(new Set())
function toggleFruttoOpen(id: number) {
  const s = new Set(openFrutti.value)
  s.has(id) ? s.delete(id) : s.add(id)
  openFrutti.value = s
}

// ── Apertura/chiusura card gruppo trasformazioni ──
const openGruppi = ref<Set<string>>(new Set())
function toggleGruppoOpen(gruppo: string) {
  const s = new Set(openGruppi.value)
  s.has(gruppo) ? s.delete(gruppo) : s.add(gruppo)
  openGruppi.value = s
}

// ── Accordion Info Personaggio ──
const INFO_FIELDS: { key: string; label: string; type?: string }[] = [
  {key: 'LUOGO_NASCITA', label: 'Luogo di Nascita'},
  {key: 'DATA_NASCITA', label: 'Data di Nascita'},
  {key: 'RAZZA', label: 'Razza'},
  {key: 'SESSO', label: 'Sesso'},
  {key: 'PELLE', label: 'Pelle'},
  {key: 'ETA', label: 'Età', type: 'number'},
  {key: 'ALTEZZA', label: 'Altezza (cm)', type: 'number'},
  {key: 'PESO', label: 'Peso (kg)', type: 'number'},
  {key: 'CAPELLI', label: 'Capelli'},
  {key: 'OCCHI', label: 'Occhi'},
  {key: 'ALLINEAMENTO', label: 'Allineamento'},
  {key: 'TAGLIA', label: 'Taglia (base)', type: 'select'},
]

const TAGLIE: { value: string; label: string }[] = [
  {value: '-4', label: 'Piccolissima'},
  {value: '-3', label: 'Minuta'},
  {value: '-2', label: 'Minuscola'},
  {value: '-1', label: 'Piccola'},
  {value: '0', label: 'Media'},
  {value: '1', label: 'Grande'},
  {value: '2', label: 'Enorme'},
  {value: '3', label: 'Mastodontica'},
  {value: '4', label: 'Colossale'},
]

const infoOpen = ref(false)
const editNome = ref('')
const editInfo = ref<Record<string, string>>({})
const savingInfo = ref(false)

function syncInfoFromStore() {
  const mod = cache.value[props.idPersonaggio]?.modificatori
  editNome.value = mod?.nome ?? ''
  const src = mod?.info ?? {}
  const dst: Record<string, string> = {}
  for (const f of INFO_FIELDS) dst[f.key] = src[f.key] ?? ''
  editInfo.value = dst
}

// inizializza e tieni allineato quando cambiano i dati o si apre l'accordion
watch(
    () => [cache.value[props.idPersonaggio]?.modificatori?.nome, cache.value[props.idPersonaggio]?.modificatori?.info],
    () => { if (!savingInfo.value) syncInfoFromStore() },
    {immediate: true, deep: true}
)

function toggleInfoOpen() {
  if (!infoOpen.value) syncInfoFromStore()
  infoOpen.value = !infoOpen.value
}

const pesoTotale = computed(() => cache.value[props.idPersonaggio]?.modificatori?.pesoTotale)

function openPesoDettaglio() {
  const pg = cache.value[props.idPersonaggio]
  if (!pg?.modificatori || !pg?.items) return
  openPopup(
    markRaw(PesoDettaglioPopup),
    {data: {modificatori: pg.modificatori, items: pg.items}},
    {closable: true, autoClose: 0}
  )
}
const tagliaAttuale = computed(() => {
  const t = cache.value[props.idPersonaggio]?.modificatori?.tagliaAttuale
  return t != null ? testoTaglia(t) : null
})

async function salvaInfo() {
  if (savingInfo.value) return
  savingInfo.value = true
  try {
    await updatePersonaggioInfo(props.idPersonaggio, editNome.value.trim(), {...editInfo.value})
    await characterStore.fetchCharacter(props.idPersonaggio, true)
    infoOpen.value = false
  } catch (e) {
    console.error('Errore salvataggio info personaggio:', e)
  } finally {
    savingInfo.value = false
  }
}

</script>

<template>
  <div>
    <!-- Accordion Info Personaggio -->
    <div class="info-card">
      <button type="button" class="info-head" @click="toggleInfoOpen">
        <span class="chev" :class="{open: infoOpen}">▸</span>
        <h2 class="info-nome">{{ cache[idPersonaggio]?.modificatori?.nome ?? "" }}</h2>
        <span v-if="pesoTotale != null" class="info-peso-badge"
              @click.stop="openPesoDettaglio">{{ pesoTotale }} kg</span>
      </button>

      <div v-if="infoOpen" class="info-body">
        <label class="info-field info-field--full">
          <span class="info-label">Nome</span>
          <input v-model="editNome" type="text" class="info-input"/>
        </label>

        <label v-for="f in INFO_FIELDS" :key="f.key" class="info-field">
          <span class="info-label">{{ f.label }}</span>
          <SearchSelect v-if="f.type === 'select'" v-model="editInfo[f.key]"
                        :options="[{value:'',label:'—'}, ...TAGLIE]" :sort="false"/>
          <input v-else v-model="editInfo[f.key]" :type="f.type || 'text'" class="info-input"/>
        </label>

        <div v-if="tagliaAttuale" class="info-peso-row">
          <span class="info-label">Taglia effettiva</span>
          <span class="info-peso-val">{{ tagliaAttuale }}</span>
        </div>

        <div class="info-peso-row">
          <span class="info-label">Peso totale</span>
          <span class="info-peso-val">{{ pesoTotale != null ? pesoTotale + ' kg' : '—' }}</span>
        </div>

        <div class="info-actions">
          <button type="button" class="btn-salva" :disabled="savingInfo" @click="salvaInfo">
            {{ savingInfo ? 'Salvataggio…' : 'Salva' }}
          </button>
        </div>
      </div>
    </div>
    <Mobile_HP v-if="cache[idPersonaggio]?.modificatori" :id-personaggio="idPersonaggio"/>
    <div v-if="cache[idPersonaggio]?.modificatori" class="stat-block">
      <Mobile_Stat id="FOR" :id-personaggio="idPersonaggio"/>
      <Mobile_Stat id="DES" :id-personaggio="idPersonaggio"/>
      <Mobile_Stat id="COS" :id-personaggio="idPersonaggio"/>
      <Mobile_Stat id="INT" :id-personaggio="idPersonaggio"/>
      <Mobile_Stat id="SAG" :id-personaggio="idPersonaggio"/>
      <Mobile_Stat id="CAR" :id-personaggio="idPersonaggio"/>
    </div>
    <div v-if="cache[idPersonaggio]?.modificatori" class="stat-block">
      <Mobile_Stat id="TMP" :id-personaggio="idPersonaggio" label="Tempra"/>
      <Mobile_Stat id="RFL" :id-personaggio="idPersonaggio" label="Riflessi"/>
      <Mobile_Stat id="VLT" :id-personaggio="idPersonaggio" label="Volonta"/>
    </div>
    <div v-if="cache[idPersonaggio]?.modificatori" class="stat-block">
      <Mobile_Stat id="CA" :id-personaggio="idPersonaggio" label="CA"/>
      <Mobile_Stat id="CAC" :id-personaggio="idPersonaggio" label="Contatto"/>
      <Mobile_Stat id="CAS" :id-personaggio="idPersonaggio" label="Sorpreso"/>
    </div>
    <div v-if="cache[idPersonaggio]?.modificatori" class="stat-block">
      <Mobile_Stat v-if="risultato === null" id="BAB" :id-personaggio="idPersonaggio" label="BAB"/>
      <Mobile_Stat id="LTT" :id-personaggio="idPersonaggio" label="Lotta"/>
      <Mobile_Stat id="MSC" :id-personaggio="idPersonaggio" label="Mischia"/>
      <Mobile_Stat id="GTT" :id-personaggio="idPersonaggio" label="Distanza"/>
    </div>
    <div class="stat-block">
      <Mobile_Stat
          v-for="stat in (cache[idPersonaggio]?.modificatori?.attributi ?? []).filter(x => x.modificatori.length > 0)"
          :id="stat.id" :id-personaggio="idPersonaggio" :label="stat.label"
      />
    </div>
    <div class="stat-block">
      <Mobile_Contatore
          v-for="stat in (cache[idPersonaggio]?.modificatori?.contatori ?? []).filter(x => x.id !== 'PF' && x.id != 'PFTEMP' && x.max > 0)"
          :id-stat="stat.id" :id-personaggio="idPersonaggio"
      />
    </div>
    <div class="spazietto"/>

    <!-- Frutti: card apribile/chiudibile -->
    <template v-if="fruttiConFigli.length">
      <div class="frutti-list">
        <div v-for="{frutto, trasf, forme} in fruttiConFigli" :key="frutto.id" class="frutto-card">

          <!-- Header: sempre visibile, click apre/chiude -->
          <button type="button" class="frutto-head" @click="toggleFruttoOpen(frutto.id)">
            <span class="chev" :class="{open: openFrutti.has(frutto.id)}">▸</span>
            <span class="frutto-nome" :class="{dimmed: frutto.disabled}">{{ frutto.nome }}</span>
            <!-- chiuso: mostra gli elementi attivi (trasf + forme) come pillole compatte -->
            <template v-if="!openFrutti.has(frutto.id)">
              <span
                  v-for="t in [...trasf, ...forme].filter(t => !t.disabled)" :key="t.id"
                  class="pill-attiva"
              >{{ strippaPrefisso(t.nome) }}</span>
              <span v-if="[...trasf, ...forme].every(t => t.disabled)" class="pill-nessuna">—</span>
            </template>
          </button>

          <!-- Body: visibile solo da aperta -->
          <div v-if="openFrutti.has(frutto.id)" class="frutto-body">

            <!-- Forme -->
            <template v-if="forme.length">
              <div class="tipo-sep forma-sep">Forme</div>
              <div
                  v-for="f in forme" :key="f.id"
                  class="trasf-riga forma-riga"
                  :class="{attiva: !f.disabled}"
              >
                <button type="button" class="trasf-toggle" :disabled="toggling" @click="toggleTrasf(f, forme)">
                  <span class="dot forma-dot">{{ f.disabled ? '◇' : '◆' }}</span>
                  <span class="trasf-nome">{{ f.nome }}</span>
                </button>
                <button type="button" class="btn-info" :title="`Info: ${f.nome}`" @click.stop="openInfoTrasf(f)">ⓘ</button>
              </div>
            </template>

            <!-- Trasformazioni -->
            <template v-if="trasf.length">
              <div class="tipo-sep">Trasformazioni</div>
              <div
                  v-for="t in trasf" :key="t.id"
                  class="trasf-riga"
                  :class="{attiva: !t.disabled}"
              >
                <button type="button" class="trasf-toggle" :disabled="toggling" @click="toggleTrasf(t, trasf)">
                  <span class="dot">{{ t.disabled ? '○' : '●' }}</span>
                  <span class="trasf-nome">{{ t.nome }}</span>
                </button>
                <button type="button" class="btn-info" :title="`Info: ${t.nome}`" @click.stop="openInfoTrasf(t)">ⓘ</button>
              </div>
            </template>

          </div>

        </div>
      </div>
      <div class="spazietto"/>
    </template>

    <!-- Trasformazioni indipendenti (non figlie di alcun frutto), raggruppate per gruppo come i frutti -->
    <div v-if="Object.keys(gruppiTrasformazioni).length" class="frutti-list">
      <div v-for="(trasf, gruppo) in gruppiTrasformazioni" :key="gruppo" class="frutto-card">

        <!-- Header: chiuso mostra il gruppo e la trasformazione attiva -->
        <button type="button" class="frutto-head" @click="toggleGruppoOpen(String(gruppo))">
          <span class="chev" :class="{open: openGruppi.has(String(gruppo))}">▸</span>
          <span class="frutto-nome">Trasformazioni {{ gruppo }}</span>
          <template v-if="!openGruppi.has(String(gruppo))">
            <span
                v-for="t in trasf.filter(x => !x.disabled)" :key="t.id"
                class="pill-attiva"
            >{{ strippaPrefisso(t.nome) }}</span>
            <span v-if="trasf.every(x => x.disabled)" class="pill-nessuna">—</span>
          </template>
        </button>

        <!-- Body: una trasformazione per riga, con toggle e info -->
        <div v-if="openGruppi.has(String(gruppo))" class="frutto-body">
          <div
              v-for="t in trasf" :key="t.id"
              class="trasf-riga"
              :class="{attiva: !t.disabled}"
          >
            <button type="button" class="trasf-toggle" :disabled="toggling" @click="toggleTrasf(t, trasf)">
              <span class="dot">{{ t.disabled ? '○' : '●' }}</span>
              <span class="trasf-nome">{{ t.nome }}</span>
            </button>
            <button type="button" class="btn-info" :title="`Info: ${t.nome}`" @click.stop="openInfoTrasf(t)">ⓘ</button>
          </div>
        </div>

      </div>
    </div>
    <div class="spazietto"/>
  </div>
</template>

<style scoped>
/* ── Accordion Info Personaggio ── */
.info-card {
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  background: #fff;
  overflow: hidden;
  margin-bottom: .6rem;
}
.info-head {
  width: 100%;
  display: flex;
  align-items: center;
  gap: .5rem;
  padding: .5rem .75rem;
  background: #f9fafb;
  border: 0;
  cursor: pointer;
  text-align: left;
}
.info-head:hover { background: #f3f4f6; }
.info-nome { margin: 0; font-size: 1.2rem; flex: 1; min-width: 0; overflow-wrap: anywhere; }
.info-peso-badge {
  font-size: .75rem;
  font-weight: 600;
  padding: .15rem .5rem;
  border-radius: .4rem;
  background: #ecfccb;
  color: #3f6212;
  cursor: pointer;
}
.info-peso-badge:hover { background: #d9f99d; }
.info-body {
  border-top: 1px solid #e5e7eb;
  padding: .75rem;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: .6rem;
}
.info-field { display: flex; flex-direction: column; gap: .2rem; min-width: 0; }
.info-field--full { grid-column: 1 / -1; }
.info-label {
  font-size: .7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: .04em;
  color: #6b7280;
}
.info-input {
  width: 100%;
  box-sizing: border-box;
  min-width: 0;
  padding: .4rem .55rem;
  border: 1px solid #d0d5dd;
  border-radius: .45rem;
  font-size: .9rem;
}
.info-input:focus { outline: none; border-color: #6366f1; box-shadow: 0 0 0 2px #e0e7ff; }
.info-peso-row {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: .5rem .55rem;
  border-radius: .45rem;
  background: #f0fdf4;
}
.info-peso-val { font-weight: 700; color: #166534; }
.info-actions { grid-column: 1 / -1; display: flex; justify-content: flex-end; }
.btn-salva {
  padding: .45rem 1.2rem;
  border: 0;
  border-radius: .45rem;
  background: #4f46e5;
  color: #fff;
  font-weight: 600;
  font-size: .9rem;
  cursor: pointer;
}
.btn-salva:hover:not(:disabled) { background: #4338ca; }
.btn-salva:disabled { opacity: .5; cursor: default; }

/* ── Frutti ── */
.frutti-list { display: grid; gap: .4rem; }

.frutto-card {
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  background: #fff;
  overflow: hidden;
}

/* Header */
.frutto-head {
  width: 100%;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: .5rem;
  padding: .55rem .75rem;
  background: #f9fafb;
  border: 0;
  cursor: pointer;
  text-align: left;
  text-align: left;
}
.frutto-head:hover { background: #f3f4f6; }

.chev {
  font-size: .75rem;
  opacity: .6;
  transition: transform .15s;
  flex-shrink: 0;
}
.chev.open { transform: rotate(90deg); }

.frutto-nome {
  font-weight: 700;
  font-size: .9rem;
  flex-shrink: 0;
}
.frutto-nome.dimmed { opacity: .4; }

.pill-attiva {
  font-size: .75rem;
  padding: .15rem .5rem;
  border-radius: .4rem;
  background: #dbeafe;
  color: #1e40af;
  font-weight: 600;
  white-space: normal;
  overflow-wrap: anywhere;
}
.pill-nessuna { font-size: .8rem; opacity: .45; }

/* Body */
.frutto-body { border-top: 1px solid #e5e7eb; }

.trasf-riga {
  display: flex;
  align-items: center;
  border-bottom: 1px solid #f3f4f6;
}
.trasf-riga:last-child { border-bottom: 0; }
.trasf-riga.attiva { background: #eff6ff; }

.trasf-toggle {
  flex: 1;
  display: flex;
  align-items: center;
  gap: .5rem;
  padding: .55rem .75rem;
  border: 0;
  background: transparent;
  cursor: pointer;
  text-align: left;
  min-width: 0;
}
.trasf-toggle:disabled { opacity: .55; cursor: default; }

.dot {
  font-size: 1rem;
  color: #9ca3af;
  flex-shrink: 0;
  width: 1rem;
  text-align: center;
}
.trasf-riga.attiva .dot { color: #2563eb; }

.trasf-nome {
  font-size: .9rem;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.trasf-riga.attiva .trasf-nome { font-weight: 700; color: #1e40af; }

.btn-info {
  flex-shrink: 0;
  padding: .55rem .75rem;
  border: 0;
  border-left: 1px solid #e5e7eb;
  background: transparent;
  color: #6b7280;
  font-size: .9rem;
  cursor: pointer;
}
.btn-info:hover { background: #eef2ff; color: #3730a3; }
.trasf-riga.attiva .btn-info { border-left-color: #bfdbfe; }
.trasf-riga.attiva .btn-info:hover { background: #dbeafe; color: #1e40af; }

/* Separatore di tipo dentro la card */
.tipo-sep {
  padding: .25rem .75rem;
  font-size: .7rem;
  font-weight: 700;
  letter-spacing: .04em;
  text-transform: uppercase;
  color: #9ca3af;
  background: #f9fafb;
  border-top: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
}
.frutto-body > .tipo-sep:first-child { border-top: 0; }

/* Forme: diamanti e sfondo viola chiaro quando attive */
.forma-riga.attiva { background: #f5f3ff; }
.forma-riga .trasf-nome { }
.forma-riga.attiva .trasf-nome { color: #6d28d9; }
.forma-dot { color: #9ca3af; }
.forma-riga.attiva .forma-dot { color: #7c3aed; }
</style>
