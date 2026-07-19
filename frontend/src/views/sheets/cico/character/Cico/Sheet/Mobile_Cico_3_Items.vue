<script setup lang="ts">
import {computed, markRaw, ref, watch} from 'vue';
import Tabella from "../../../../../../components/Tabella.vue";
import {useCharacterStore} from "../../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import Mobile_DettaglioItem from "../../Dettaglio/Mobile_DettaglioItem.vue";
import BottoneAggiungiItem from "../../Shared/BottoneAggiungiItem.vue";
import {resetUtilizzi, setUtilizziUsati} from "../../../../../../service/PersonaggioService";
import {ItemSearchResult, searchPersonaggioItems} from "../../../../../../service/PartyService";
import usePopup from "../../../../../../function/usePopup";
import AggiungiDalCompendioPopup from "./AggiungiDalCompendioPopup.vue";
import {Effetto} from "../../../../../../models/dto/Items";

const characterStore = useCharacterStore()
const {cache} = storeToRefs(characterStore)
const {openPopup} = usePopup()

function apriCompendio() {
  openPopup(
    markRaw(AggiungiDalCompendioPopup),
    {idPersonaggio: props.idPersonaggio},
    {closable: true}
  )
}

const props = defineProps({
  idPersonaggio: {
    type: Number,
    required: true
  }
});

// Avvolge una lista di item col componente di dettaglio espandibile e ordina per nome.
function wrap(list: any[] | undefined) {
  return (list ?? [])
      .map(itm => ({
        ...itm,
        // utilizziTotale === 0 → formula esiste ma dà 0: mostra come disabilitato
        disabled: itm.disabled || itm.utilizziTotale === 0,
        expandedComponent: markRaw(Mobile_DettaglioItem),
        expandedProps: {data: {item: {...itm}, personaggio: cache.value[props.idPersonaggio]}}
      }))
      .sort((a, b) => a.nome.localeCompare(b.nome));
}

const resetting = ref(false)
async function handleReset() {
  resetting.value = true
  try {
    await resetUtilizzi(props.idPersonaggio)
    await characterStore.fetchCharacter(props.idPersonaggio, true)
  } finally {
    resetting.value = false
  }
}

// ── Ricerca profonda tra tutti gli item del personaggio (nome, label, note) ──
const ricerca = ref('')
const risultati = ref<ItemSearchResult[]>([])
const cercando = ref(false)
const inRicerca = computed(() => ricerca.value.trim().length >= 2)
let ricercaTimer: any = null
watch(ricerca, () => {
  if (ricercaTimer) clearTimeout(ricercaTimer)
  const q = ricerca.value.trim()
  if (q.length < 2) { risultati.value = []; return }
  ricercaTimer = setTimeout(async () => {
    cercando.value = true
    try {
      risultati.value = (await searchPersonaggioItems(props.idPersonaggio, q)).data
    } catch (e) {
      console.error('Errore ricerca item:', e)
      risultati.value = []
    } finally {
      cercando.value = false
    }
  }, 350)
})
const risultatiWrapped = computed(() => risultati.value.map(r => ({
  id: r.id,
  nome: r.nome,
  tipo: r.tipo,
  matchLabel: r.match,
  disabled: r.disabled,
  expandedComponent: markRaw(Mobile_DettaglioItem),
  expandedProps: {data: {item: {id: r.id, nome: r.nome, tipo: r.tipo, disabled: r.disabled}, personaggio: cache.value[props.idPersonaggio]}},
})))
const columnsRicerca = [
  {field: 'nome', label: 'Risultati', subfield: 'matchLabel', badge: (row: any) => row.tipo, disabled: (row: any) => row.disabled},
]

const items = computed(() => cache.value[props.idPersonaggio]?.items)

// Inventario
const itemsOggetti = computed(() => wrap(items.value?.oggetti));
const itemsArmi = computed(() => wrap(items.value?.armi));
const itemsEquipaggiamento = computed(() => wrap(items.value?.equipaggiamento));
const itemsConsumabili = computed(() => wrap(items.value?.consumabili));
const itemsMunizioni = computed(() => wrap(items.value?.munizioni));
const itemsContenitori = computed(() => wrap(items.value?.contenitori));
const itemsFrutti = computed(() => wrap(items.value?.frutti));
const itemsIdoli = computed(() => wrap(items.value?.idoli));
const itemsVeicoli = computed(() => wrap(items.value?.veicoli));

// Abilità attive = hanno utilizziTotale visibile (> 0); passive = tutto il resto
const itemsAbilita = computed(() =>
    wrap((items.value?.abilita ?? []).filter(i => (i.utilizziTotale ?? 0) > 0))
)
const itemsAbilitaPassive = computed(() =>
    wrap((items.value?.abilita ?? []).filter(i => !((i.utilizziTotale ?? 0) > 0)))
);
const itemsAltro = computed(() => wrap(items.value?.altro));
const itemsPatti = computed(() => wrap(items.value?.patti));
const itemsNotizie = computed(() => wrap(items.value?.notizie));
const itemsTalenti = computed(() => wrap(items.value?.talenti));
const itemsPrivilegi = computed(() => wrap(items.value?.privilegi));
const itemsMaledizioni = computed(() => wrap(items.value?.maledizioni));

// Effetti: collegamenti verso item EFFETTO aggregati su tutti gli item del personaggio.
// Riga = "Condizione: Nome oggetto che porta l'effetto"; espandendola si vede l'item EFFETTO
// vero e proprio (nome reale + descrizione), non l'oggetto che lo concede.
const itemsEffetti = computed(() => (items.value?.effetti ?? [])
    .map((e: Effetto) => ({
      id: e.targetId,
      nome: `${e.sourceNome} (${e.condizione ?? 'Sempre'}): ${e.targetNome}`,
      disabled: e.disabled,
      expandedComponent: markRaw(Mobile_DettaglioItem),
      expandedProps: {
        data: {item: {id: e.targetId, nome: e.targetNome, tipo: 'EFFETTO', disabled: e.disabled}, personaggio: cache.value[props.idPersonaggio]},
      },
    }))
    .sort((a, b) => a.nome.localeCompare(b.nome)));

const badgeQta = (row: any) => row.quantita != null && row.quantita !== 1 ? `x${row.quantita}` : null;

// Chip peso: "X.XX kg" — mostrato solo se l'item ha un PESO
function pesoChip(row: any): string | null {
  const p = row.peso
  if (!p) return null
  const tot = p * (row.quantita ?? 1)
  if (tot <= 0) return null
  const formatted = tot % 1 === 0 ? `${tot}` : tot.toFixed(2)
  return `${formatted} kg`
}

// Colonna utilizzi: visibile solo se l'item ha un totale definito
function utilizziCol() {
  return {
    field: 'utilizziUsati',
    label: '',
    type: 'counter' as const,
    counter: {
      hide: (row: any) => row.utilizziTotale == null || row.utilizziTotale === 0,
      chip: pesoChip,
      value: (row: any) => (row.utilizziTotale ?? 0) - (row.utilizziUsati ?? 0),
      max: (row: any) => row.utilizziTotale ?? null,
      onSub: (row: any) => {
        // consuma uno: incrementa il contatore degli usati (salvato), riduce il rimanente mostrato
        const nuovi = Math.min(row.utilizziTotale, (row.utilizziUsati ?? 0) + 1)
        setUtilizziUsati(row.id, props.idPersonaggio, nuovi)
            .then(() => characterStore.fetchCharacter(props.idPersonaggio, true))
      },
      onAdd: (row: any) => {
        // ripristina uno: decrementa il contatore degli usati (salvato), aumenta il rimanente mostrato
        const nuovi = Math.max(0, (row.utilizziUsati ?? 0) - 1)
        setUtilizziUsati(row.id, props.idPersonaggio, nuovi)
            .then(() => characterStore.fetchCharacter(props.idPersonaggio, true))
      },
    }
  }
}

// Colori dei chip prefisso: assegnati in modo deterministico in base al testo del prefisso
// (di norma un prefisso diverso = un oggetto genitore diverso), così prefissi diversi
// risultano visivamente distinguibili nell'elenco.
const PALETTE_PREFISSO = [
  {background: '#e0e7ff', color: '#3730a3'},
  {background: '#fce7f3', color: '#9d174d'},
  {background: '#dcfce7', color: '#166534'},
  {background: '#fef3c7', color: '#92400e'},
  {background: '#cffafe', color: '#155e75'},
  {background: '#fee2e2', color: '#991b1b'},
  {background: '#ede9fe', color: '#5b21b6'},
  {background: '#dbeafe', color: '#1d4ed8'},
]
function colorePrefisso(testo: string): { background: string; color: string } {
  let hash = 0
  for (let i = 0; i < testo.length; i++) hash = (hash * 31 + testo.charCodeAt(i)) | 0
  return PALETTE_PREFISSO[Math.abs(hash) % PALETTE_PREFISSO.length]
}

// chip descrittori prima del nome: Abilità (Str/Mag/Sop/Div) + Oggetto (Mag/Psi/Div/Leg/Uni),
// entrambi i gruppi possono comparire sullo stesso item.
function descrittoriChips(row: any): { text: string; class?: string; style?: Record<string, string> }[] {
  const chips: { text: string; class?: string; style?: Record<string, string> }[] = []
  if (row.prefissoOggetti) chips.push({text: row.prefissoOggetti, style: colorePrefisso(row.prefissoOggetti)})
  if (row.descrStraordinaria) chips.push({text: 'Str', class: 'chip-str'})
  if (row.descrMagica) chips.push({text: 'Mag', class: 'chip-mag'})
  if (row.descrSoprannaturale) chips.push({text: 'Sop', class: 'chip-sop'})
  if (row.descrNaturale) chips.push({text: 'Nat', class: 'chip-nat'})
  if (row.descrDivina) chips.push({text: 'Div', class: 'chip-div'})
  if (row.magico) chips.push({text: 'Magico', class: 'chip-o-magico'})
  if (row.psionico) chips.push({text: 'Psionico', class: 'chip-o-psionico'})
  if (row.divino) chips.push({text: 'Divino', class: 'chip-o-divino'})
  if (row.leggendario) chips.push({text: 'Leggendario', class: 'chip-o-leggendario'})
  if (row.unico) chips.push({text: 'Unico', class: 'chip-o-unico'})
  return chips
}

const col = (label: string, withBadge = false) => [
  {
    field: 'nome', label, disabled: (row: any) => row.disabled,
    ...(withBadge ? {badge: badgeQta} : {}),
    prefixChips: descrittoriChips,
  },
  utilizziCol(),
];

const columnsOggetti = col('Oggetti', true);
const columnsArmi = col('Armi', true);
const columnsEquipaggiamento = col('Equipaggiamento', true);
const columnsConsumabili = col('Consumabili', true);
const columnsMunizioni = col('Munizioni', true);
const columnsContenitori = col('Contenitori', true);
const columnsAltro = col('Altro', true);
const columnsVeicoli = col('Veicoli');
const columnsPatti = col('Patti');
const columnsNotizie = col('Notizie');
const columnsFrutti = col('Frutti');
const columnsIdoli = col('Idoli');
const columnsAbilita = col('Abilità');
const columnsAbilitaPassive = col('Abilità Passive');
const columnsTalenti = col('Talenti');
const columnsPrivilegi = col('Privilegi di Classe');
const columnsMaledizioni = col('Maledizioni');
const columnsEffetti = col('Effetti');
</script>

<template>
  <div>
    <div class="top-bar">
      <BottoneAggiungiItem :id-personaggio="props.idPersonaggio" label="Aggiungi oggetto"/>
      <button class="btn-compendio" type="button" @click="apriCompendio">
        &#128218; Compendio
      </button>
      <button class="btn-reset" :disabled="resetting" @click="handleReset">
        {{ resetting ? '…' : 'Azzera utilizzi' }}
      </button>
    </div>

    <!-- ricerca profonda (nome, label, note) su qualsiasi tipo di item del personaggio -->
    <div class="spazietto"/>
    <input class="ricerca-input" v-model="ricerca" type="text"
           placeholder="🔎 Cerca ovunque (privilegi, razza, abilità, label, note…)"/>

    <!-- risultati ricerca -->
    <template v-if="inRicerca">
      <div class="spazietto"/>
      <div v-if="cercando" class="stato-ricerca">Ricerca…</div>
      <Tabella v-else-if="risultatiWrapped.length" :columns="columnsRicerca" :expandable="true" :items="risultatiWrapped"/>
      <div v-else class="stato-ricerca">Nessun item trovato.</div>
    </template>

    <!-- inventario normale -->
    <template v-else>
    <div class="spazietto"/>
    <Tabella v-if="itemsOggetti.length > 0" :columns="columnsOggetti" :expandable="true" :items="itemsOggetti"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsArmi.length > 0" :columns="columnsArmi" :expandable="true" :items="itemsArmi"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsEquipaggiamento.length > 0" :columns="columnsEquipaggiamento" :expandable="true" :items="itemsEquipaggiamento"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsConsumabili.length > 0" :columns="columnsConsumabili" :expandable="true" :items="itemsConsumabili"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsMunizioni.length > 0" :columns="columnsMunizioni" :expandable="true" :items="itemsMunizioni"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsContenitori.length > 0" :columns="columnsContenitori" :expandable="true" :items="itemsContenitori"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsAltro.length > 0" :columns="columnsAltro" :expandable="true" :items="itemsAltro"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsVeicoli.length > 0" :columns="columnsVeicoli" :expandable="true" :items="itemsVeicoli"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsPatti.length > 0" :columns="columnsPatti" :expandable="true" :items="itemsPatti"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsNotizie.length > 0" :columns="columnsNotizie" :expandable="true" :items="itemsNotizie"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsFrutti.length > 0" :columns="columnsFrutti" :expandable="true" :items="itemsFrutti"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsIdoli.length > 0" :columns="columnsIdoli" :expandable="true" :items="itemsIdoli"/>

    <div class="spazietto"/>
    <BottoneAggiungiItem :id-personaggio="props.idPersonaggio" tipo="TALENTO" label="Aggiungi talento"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsAbilita.length > 0" :columns="columnsAbilita" :expandable="true" :items="itemsAbilita"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsAbilitaPassive.length > 0" :columns="columnsAbilitaPassive" :expandable="true" :items="itemsAbilitaPassive"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsTalenti.length > 0" :columns="columnsTalenti" :expandable="true" :items="itemsTalenti"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsPrivilegi.length > 0" :columns="columnsPrivilegi" :expandable="true" :items="itemsPrivilegi"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsMaledizioni.length > 0" :columns="columnsMaledizioni" :expandable="true" :items="itemsMaledizioni"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsEffetti.length > 0" :columns="columnsEffetti" :expandable="true" :items="itemsEffetti"/>
    </template>
  </div>
</template>

<style scoped>
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: .5rem;
  margin-bottom: .25rem;
}
.ricerca-input {
  width: 100%;
  box-sizing: border-box;
  padding: .5rem .7rem;
  border: 1px solid #bfdbfe;
  border-radius: .5rem;
  background: #f8fbff;
  font-size: .9rem;
}
.ricerca-input:focus { outline: none; border-color: #60a5fa; background: #fff; }
.stato-ricerca { padding: .6rem; color: #6b7280; font-size: .9rem; }
.btn-compendio {
  flex-shrink: 0;
  padding: .35rem .75rem;
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
  border-radius: .5rem;
  font-size: .8rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-compendio:hover { background: #dbeafe; }
.btn-reset {
  flex-shrink: 0;
  padding: .35rem .75rem;
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #991b1b;
  border-radius: .5rem;
  font-size: .8rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-reset:hover { background: #fee2e2; }
.btn-reset:disabled { opacity: .5; cursor: default; }
</style>
