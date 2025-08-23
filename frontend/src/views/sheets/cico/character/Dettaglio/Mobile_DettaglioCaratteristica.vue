<script setup>
import {computed, defineProps, onMounted, ref, watch} from 'vue';
import {testoFormula, testoModificatore} from '../../../../../function/Utils';
// ⬇️ opzionale: collega un servizio reale; se non esiste, userà l'emit 'save-temp'
import {updateTemporaryModifier} from '../../../../../service/PersonaggioService';
import {useCharacterStore} from "../../../../../stores/personaggio";
import Icona from "../../../../../components/Icona/Icona.vue";
import Tabella from "../../../../../components/Tabella.vue"; // <-- crea questa API se non c'è

const props = defineProps({
  stat: {
    type: Object,
    required: true
  },
  idPersonaggio: {
    type: Number,
    required: true
  }
});

const characterStore = useCharacterStore();

// ---------- Lettura "Temporaneo" iniziale ----------
const TEMP_LABEL = 'Temporaneo';

function findTempMod(mods) {
  if (!Array.isArray(mods)) return null;
  return mods.find(m => String(m?.item ?? '').toLowerCase() === TEMP_LABEL.toLowerCase()) || null;
}

const tempValue = ref(0);
const lastSentValue = ref(0);
const isSaving = ref(false);
let saveTimer = null;
const DEBOUNCE_MS = 1500;

// allinea tempValue a props.stat.modificatori (senza sovrascrivere mentre stai salvando)
function syncFromProps() {
  const t = findTempMod(props.stat?.modificatori);
  const v = Number(t?.valore ?? 0);
  if (!isSaving.value) {
    tempValue.value = Number.isFinite(v) ? v : 0;
    lastSentValue.value = tempValue.value;
  }
}

watch(() => props.stat?.modificatori, syncFromProps, {immediate: true, deep: true});

onMounted(() => {
  console.log('Component mounted.', props.stat);
});

// ---------- Liste mostrate (iniettano il temporaneo aggiornato nell’UI) ----------
const displayedMods = computed(() => {
  const mods = Array.isArray(props.stat?.modificatori) ? [...props.stat.modificatori] : [];
  const idx = mods.findIndex(m => String(m?.item ?? '').toLowerCase() === TEMP_LABEL.toLowerCase());
  if (idx >= 0) {
    // sovrascrivo solo il valore, mantengo nota/sempreAttivo se presenti
    mods[idx] = {...mods[idx], valore: tempValue.value};
  } else if (tempValue.value !== 0) {
    // se non esiste ma l'utente imposta un valore, mostralo tra i non-sempre-attivi
    mods.push({item: TEMP_LABEL, valore: tempValue.value, nota: '', sempreAttivo: false});
  }
  return mods;
});

const modsSempre = computed(() => displayedMods.value.filter(x => x?.sempreAttivo));
const modsSituaz = computed(() => displayedMods.value.filter(x => !x?.sempreAttivo));

// ---------- UI interazione ----------
function scheduleSave() {
  if (saveTimer) clearTimeout(saveTimer);
  saveTimer = setTimeout(saveNow, DEBOUNCE_MS);
}

function inc(step = 1) {
  tempValue.value = Math.trunc((tempValue.value ?? 0) + step);
  scheduleSave();
}

function dec(step = 1) {
  tempValue.value = Math.trunc((tempValue.value ?? 0) - step);
  scheduleSave();
}

function onInput(e) {
  const n = Number(e?.target?.value);
  tempValue.value = Number.isFinite(n) ? Math.trunc(n) : 0;
  scheduleSave();
}

// ---------- Salvataggio BE (debounced & optimistic) ----------
async function saveNow() {
  if (tempValue.value === lastSentValue.value) return;
  isSaving.value = true;
  const toSend = tempValue.value;

  try {
    await updateTemporaryModifier({
      idPersonaggio: props.idPersonaggio,
      idStat: props.stat.id,
      valore: toSend
    });


    lastSentValue.value = toSend;

    if (saveTimer) {
      clearTimeout(saveTimer);
      saveTimer = null;
    }
    await characterStore.fetchCharacter(props.idPersonaggio, true);
  } catch (err) {
    console.error('Errore salvataggio temporaneo:', err);
    // opzionale: potresti ripristinare tempValue a lastSentValue qui
    // tempValue.value = lastSentValue.value;
  } finally {
    isSaving.value = false;
    // await characterStore.fetchCharacter(props.idPersonaggio, true);
  }
}

const modificatoreModificatore = (mod) => {
  if (mod.tipo === 'BASE') return testoModificatore((mod.valore - 10) / 2);
  else return testoModificatore((mod.valore) / 2);
}

const mappaRiga = (mod) => {
  const STAT_KEYS = new Set(['FOR', 'DES', 'COS', 'INT', 'SAG', 'CAR'])
  console.log(mod);
  const base = testoModificatore(mod.valore)
  const formula = mod.formula ? testoFormula(mod.formula) : null;
  const extra = STAT_KEYS.has(String(props.stat.id).toUpperCase())
      ? ` [${modificatoreModificatore(mod)}]`
      : ''
  const valoreFinale = base === '+0' && extra === '' && formula ? formula : base + extra;
  console.log({
    ...mod,
    origine: mod.item ?? 'Sconosciuto',
    valor: valoreFinale,
  })
  return {
    ...mod,
    origine: mod.item ?? 'Sconosciuto',
    valor: valoreFinale,
  }
}

</script>

<template>
  <div class="abilita-detail-card">
    <!-- Header principale -->
    <div class="abilita-detail-card__header">
      <strong>{{ props.stat.label }}</strong>:
      {{ testoModificatore(props.stat.modificatore) }}
    </div>
    <div class="spazietto" v-for="i in 2" :key="i"/>

    <!-- Sempre attivi -->
    <div v-if="modsSempre.length">
      <Tabella :items="modsSempre.map(x => mappaRiga(x))"
               :columns="[{field: 'origine', subfield: 'nota', label: ''}, {field: 'valor'}]"/>
    </div>

    <!-- separatore se ci sono entrambi -->
    <div class="spazietto" v-if="modsSempre.length > 0 && modsSituaz.length > 0"/>

    <div v-if="modsSituaz.length">
      <Tabella :items="modsSituaz.map(x => mappaRiga(x))"
               :columns="[{field: 'origine', subfield: 'nota', label: ''}, {field: 'valor'}]"/>
    </div>

    <!-- Barretta temporaneo -->
    <div class="spazietto" v-for="i in 4" :key="i"/>
    <div class="bar-container">
      <Icona name="SUB" class="bar-btn" @click.stop="dec()"/>
      <div class="bar-wrapper">

        <div class="bar">
          <div class="bar-center">TEMP: {{ tempValue }}</div>

        </div>

      </div>
      <Icona name="ADD" class="bar-btn" @click.stop="inc()"/>

      <div class="bar-btn">
        <Icona v-if="isSaving" name="SPINNER" :spin="true"/>
        <!--        <Icona v-else-if="tempValue !== lastSentValue" name="XMARK"/>-->
        <!--        <Icona v-else name="CHECK"/>-->
      </div>

    </div>
  </div>
</template>

