<script setup>
import {computed, defineProps, onMounted, ref, watch} from 'vue';
import {testoModificatore} from '../../../../../function/Utils';
// ⬇️ opzionale: collega un servizio reale; se non esiste, userà l'emit 'save-temp'
import {updateTemporaryModifier} from '../../../../../service/PersonaggioService';
import {useCharacterStore} from "../../../../../stores/personaggio"; // <-- crea questa API se non c'è

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
    await characterStore.fetchCharacter(props.idPersonaggio, true);
  }
}

</script>

<template>
  <div class="abilita-detail-card">
    <!-- Barretta temporaneo -->
    <div class="tempbar">
      <span class="tempbar__label">{{ TEMP_LABEL }}</span>
      <div class="tempbar__ctrl">
        <button class="btn minus" @click="dec()">−</button>
        <input
            class="tempbar__input"
            type="number"
            :value="tempValue"
            @input="onInput"
        />
        <button class="btn plus" @click="inc()">+</button>
      </div>
      <div class="tempbar__status">
        <span v-if="isSaving" class="saving">salvataggio…</span>
        <span v-else-if="tempValue !== lastSentValue" class="dirty">X</span>
        <span v-else class="ok">✓</span>
      </div>
    </div>

    <!-- Header principale -->
    <div class="abilita-detail-card__header">
      <strong>{{ props.stat.label }}</strong>:
      {{ testoModificatore(props.stat.modificatore) }}
    </div>

    <br><br>

    <!-- Sempre attivi -->
    <div v-if="modsSempre.length">
      <p v-for="(mod, index) in modsSempre" :key="'sempre-' + index">
        <strong>{{ mod.item || 'Sconosciuto' }}:</strong>
        {{ testoModificatore(mod.valore) }}
        <span v-if="mod.nota">{{ mod.nota }}</span>
      </p>
    </div>

    <!-- separatore se ci sono entrambi -->
    <div class="spazietto" v-if="modsSempre.length > 0 && modsSituaz.length > 0"></div>

    <!-- Situazionali (incluso 'Temporaneo' se presente/non zero) -->
    <div v-if="modsSituaz.length">
      <p v-for="(mod, index) in modsSituaz" :key="'sit-' + index">
        <strong>{{ mod.item || 'Sconosciuto' }}:</strong>
        {{ testoModificatore(mod.valore) }}
        <span v-if="mod.nota">{{ mod.nota }}</span>
      </p>
    </div>
  </div>
</template>

<style scoped>
.abilita-detail-card {
  margin: 0;
}

.abilita-detail-card__header {
  margin-top: 8px;
}

p {
  margin: 0;
}

ul {
  padding: 0;
}

li {
  margin-bottom: 6px;
  border-radius: 4px;
}

.spazietto {
  height: 8px;
}

/* --- Temp bar --- */
.tempbar {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  border: 1px solid rgba(0, 0, 0, .1);
  border-radius: 8px;
  background: #fafafa;
}

.tempbar__label {
  font-weight: 600;
  opacity: .85;
}

.tempbar__ctrl {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.tempbar__input {
  width: 80px;
  padding: 4px 6px;
  border: 1px solid rgba(0, 0, 0, .15);
  border-radius: 8px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.btn {
  border: 1px solid rgba(0, 0, 0, .2);
  background: #f7f7f7;
  border-radius: 8px;
  padding: 2px 10px;
  cursor: pointer;
  font-weight: 600;
}

.btn:hover {
  background: #eee;
}

.btn.minus, .btn.plus {
  width: 32px;
  height: 28px;
  padding: 0;
  display: grid;
  place-items: center;
}

.tempbar__status {
  font-size: .8rem;
}

.saving {
  color: #8a6d3b;
}

.dirty {
  color: #b35c00;
}

.ok {
  color: #166534;
}
</style>
