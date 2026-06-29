<script setup lang="ts">
import {computed, markRaw, ref} from 'vue';
import Tabella from "../../../../../../components/Tabella.vue";
import {useCharacterStore} from "../../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import Mobile_DettaglioItem from "../../Dettaglio/Mobile_DettaglioItem.vue";
import BottoneAggiungiItem from "../../Shared/BottoneAggiungiItem.vue";
import {resetUtilizzi, setUtilizziUsati} from "../../../../../../service/PersonaggioService";

const characterStore = useCharacterStore()
const {cache} = storeToRefs(characterStore)

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

// Capacità (ex pagina Talenti)
const itemsAbilitaPassive = computed(() => wrap(items.value?.abilita));
const itemsTalenti = computed(() => wrap(items.value?.talenti));
const itemsPrivilegi = computed(() => wrap(items.value?.privilegi));
const itemsMaledizioni = computed(() => wrap(items.value?.maledizioni));

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

const col = (label: string, withBadge = false) => [
  {field: 'nome', label, disabled: (row: any) => row.disabled, ...(withBadge ? {badge: badgeQta} : {})},
  utilizziCol(),
];

const columnsOggetti = col('Oggetti', true);
const columnsArmi = col('Armi', true);
const columnsEquipaggiamento = col('Equipaggiamento', true);
const columnsConsumabili = col('Consumabili', true);
const columnsMunizioni = col('Munizioni', true);
const columnsContenitori = col('Contenitori', true);
const columnsFrutti = col('Frutti');
const columnsIdoli = col('Idoli');
const columnsAbilitaPassive = col('Abilità Passive');
const columnsTalenti = col('Talenti');
const columnsPrivilegi = col('Privilegi di Classe');
const columnsMaledizioni = col('Maledizioni');
</script>

<template>
  <div>
    <div class="top-bar">
      <BottoneAggiungiItem :id-personaggio="props.idPersonaggio" label="Aggiungi oggetto"/>
      <button class="btn-reset" :disabled="resetting" @click="handleReset">
        {{ resetting ? '…' : 'Azzera utilizzi' }}
      </button>
    </div>
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
    <Tabella v-if="itemsFrutti.length > 0" :columns="columnsFrutti" :expandable="true" :items="itemsFrutti"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsIdoli.length > 0" :columns="columnsIdoli" :expandable="true" :items="itemsIdoli"/>

    <div class="spazietto"/>
    <BottoneAggiungiItem :id-personaggio="props.idPersonaggio" tipo="TALENTO" label="Aggiungi talento"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsAbilitaPassive.length > 0" :columns="columnsAbilitaPassive" :expandable="true" :items="itemsAbilitaPassive"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsTalenti.length > 0" :columns="columnsTalenti" :expandable="true" :items="itemsTalenti"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsPrivilegi.length > 0" :columns="columnsPrivilegi" :expandable="true" :items="itemsPrivilegi"/>
    <div class="spazietto"/>
    <Tabella v-if="itemsMaledizioni.length > 0" :columns="columnsMaledizioni" :expandable="true" :items="itemsMaledizioni"/>
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
