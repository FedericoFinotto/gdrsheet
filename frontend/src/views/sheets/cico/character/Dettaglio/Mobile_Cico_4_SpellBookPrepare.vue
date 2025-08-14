<script setup lang="ts">
import type {PropType} from 'vue';
import {computed, defineEmits, defineProps, markRaw, ref, watch} from 'vue';
import {useCharacterStore} from '../../../../../stores/personaggio';
import {storeToRefs} from 'pinia';
import {getAllIncantesimiByClasseAndLivello} from '../../../../../service/PersonaggioService';
import Mobile_DettaglioItem from '../Dettaglio/Mobile_DettaglioItem.vue';

const props = defineProps({
  idPersonaggio: {type: Number, required: true},
  livello: {type: Number, required: true},
  classe: {type: String, required: true},
  idClasse: {type: Number, required: true},
  spellList: {type: String, required: true},
  /** mappa spellId -> prepared iniziale (prelevato dalla pagina principale).
   *  Se un valore è -54, viene interpretato come "sempre preparato". */
  preparedInit: {type: Object as PropType<Record<number, number>>, default: () => ({})}
});

const emit = defineEmits<{
  (e: 'confirm', payload: {
    idClasse: number;
    classe: string;
    livello: number;
    prepared: Record<number, number>; // contiene numeri >=0 o -54 per "sempre"
    spellList: string;
  }): void
  (e: 'close'): void
}>();

const {cache} = storeToRefs(useCharacterStore());

const loading = ref(false);
const error = ref<string | null>(null);
const spellsRaw = ref<any[]>([]);
const ExpandedComp = markRaw(Mobile_DettaglioItem);

// fetch con protezione race
let lastCall = 0;

async function loadData() {
  const callId = ++lastCall;
  loading.value = true;
  error.value = null;
  try {
    const data: any = await getAllIncantesimiByClasseAndLivello(props.idClasse, props.livello);
    let arr: any[] = [];
    if (Array.isArray(data)) arr = data;
    else if (Array.isArray(data?.data)) arr = data.data;
    else if (Array.isArray(data?.content)) arr = data.content;
    else if (data && typeof data === 'object') arr = Object.values(data);
    if (callId !== lastCall) return;
    spellsRaw.value = arr;
  } catch (e: any) {
    if (callId !== lastCall) return;
    error.value = e?.message ?? 'Errore durante il caricamento degli incantesimi';
    spellsRaw.value = [];
  } finally {
    if (callId === lastCall) loading.value = false;
  }
}

watch(() => [props.idClasse, props.livello], () => loadData(), {immediate: true});

// normalizzazione rows (porta anche alwaysPrep, se presente nella risposta)
function normalizeSpell(raw: any) {
  const itm = raw?.item ?? raw?.i ?? raw?.spell ?? raw;
  const id = itm?.id ?? raw?.idItem ?? raw?.itemId;
  const nome = itm?.nome ?? itm?.name ?? itm?.titolo ?? '';
  const alwaysPrep = Boolean(itm?.alwaysPrep ?? itm?.alwaysPrepared ?? false);
  return {id, nome, alwaysPrep, ...itm};
}

const spells = computed(() =>
    (spellsRaw.value ?? [])
        .map(normalizeSpell)
        .filter(s => s?.id != null)
        .sort((a, b) => (a?.nome ?? '').localeCompare(b?.nome ?? ''))
);

// stato prepared e "sempre"
const prepared = ref<Record<number, number>>({});
const alwaysPrepared = ref<Record<number, boolean>>({});

let preparedInitialSnapshot: Record<number, number> = {};
let alwaysInitialSnapshot: Record<number, boolean> = {};

watch([spells, () => props.preparedInit], () => {
  const nextPrep: Record<number, number> = {};
  const nextAlways: Record<number, boolean> = {};

  for (const s of spells.value) {
    const initVal = props.preparedInit[s.id as number];
    const fromSentinel = initVal === -54;
    const fromItem = Boolean((s as any).alwaysPrep);
    const isAlways = fromSentinel || fromItem;

    nextAlways[s.id as number] = isAlways;
    // se "sempre", il numerico non serve in UI ma lo teniamo 0 come placeholder
    const numeric = Number.isFinite(initVal) ? Math.max(0, Math.trunc(initVal)) : 0;
    nextPrep[s.id as number] = isAlways ? 0 : numeric;
  }

  prepared.value = nextPrep;
  alwaysPrepared.value = nextAlways;

  preparedInitialSnapshot = JSON.parse(JSON.stringify(nextPrep));
  alwaysInitialSnapshot = JSON.parse(JSON.stringify(nextAlways));
}, {immediate: true});

// expand/collapse
const expanded = ref<Set<number>>(new Set());
const toggleExpand = (id: number) => {
  const set = new Set(expanded.value);
  set.has(id) ? set.delete(id) : set.add(id);
  expanded.value = set;
};
const isExpanded = (id: number) => expanded.value.has(id);

// counter (disabilita se "sempre")
const inc = (id: number, step = 1) => {
  if (alwaysPrepared.value[id]) return;
  prepared.value[id] = Math.max(0, (prepared.value[id] ?? 0) + step);
};
const dec = (id: number, step = 1) => {
  if (alwaysPrepared.value[id]) return;
  prepared.value[id] = Math.max(0, (prepared.value[id] ?? 0) - step);
};

// footer actions (considera anche il flag "sempre")
function isDirty() {
  const a = prepared.value, b = preparedInitialSnapshot;
  const aa = alwaysPrepared.value, bb = alwaysInitialSnapshot;
  const keysPrep = new Set([...Object.keys(a), ...Object.keys(b)]);
  for (const k of keysPrep) if ((a as any)[k] !== (b as any)[k]) return true;

  const keysAlways = new Set([...Object.keys(aa), ...Object.keys(bb)]);
  for (const k of keysAlways) if ((aa as any)[k] !== (bb as any)[k]) return true;

  return false;
}

const reset = () => {
  prepared.value = JSON.parse(JSON.stringify(preparedInitialSnapshot));
  alwaysPrepared.value = JSON.parse(JSON.stringify(alwaysInitialSnapshot));
};

// conferma: costruisce la mappa con numerico >=0 o -54 se "sempre"
const confirm = () => {
  const out: Record<number, number> = {};
  for (const s of spells.value) {
    const id = s.id as number;
    out[id] = alwaysPrepared.value[id] ? -54 : (prepared.value[id] ?? 0);
  }
  emit('confirm', {
    idClasse: props.idClasse,
    classe: props.classe,
    livello: props.livello,
    spellList: props.spellList,
    prepared: out
  });
  emit('close'); // chiude il popup
};
</script>

<template>
  <div class="popup-root">
    <header class="popup-header">
      <h3 class="title">{{ livello === 0 ? 'Cantrip' : `Livello ${livello}` }} · {{ classe }}</h3>
    </header>

    <div class="popup-body">
      <div v-if="loading" class="state">Caricamento…</div>
      <div v-else-if="error" class="state error">{{ error }}</div>

      <template v-else>
        <div v-if="!spells.length" class="state">Nessun incantesimo disponibile.</div>

        <div v-else class="spell-list">
          <div v-for="sp in spells" :key="sp.id" class="spell-row">
            <div class="row-top">
              <button class="spell-toggle" @click="toggleExpand(sp.id)" :aria-expanded="isExpanded(sp.id)">
                <span class="spell-name" :title="sp.nome">{{ sp.nome }}</span>
              </button>

              <div class="row-actions">
                <label class="always-toggle">
                  <input type="checkbox" v-model="alwaysPrepared[sp.id]"/>
                </label>

                <div v-if="!alwaysPrepared[sp.id]" class="spell-controls">
                  <button class="btn minus" @click.stop="dec(sp.id)">−</button>
                  <div class="count" aria-live="polite">{{ prepared[sp.id] ?? 0 }}</div>
                  <button class="btn plus" @click.stop="inc(sp.id)">+</button>
                </div>

                <div v-else class="always-badge" title="Incantesimo sempre preparato">sempre</div>
              </div>
            </div>

            <transition name="expand">
              <div v-if="isExpanded(sp.id)" class="row-expand">
                <component :is="ExpandedComp" :data="{ item: sp, personaggio: cache?.[idPersonaggio] }"/>
              </div>
            </transition>
          </div>
        </div>
      </template>
    </div>

    <footer class="popup-footer">
      <button class="btn subtle" @click="reset" :disabled="!isDirty()">Reset</button>
      <div class="footer-spacer"/>
      <button class="btn primary" @click="confirm">Conferma</button>
    </footer>
  </div>
</template>
