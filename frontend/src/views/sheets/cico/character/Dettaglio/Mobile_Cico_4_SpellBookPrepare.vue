<script setup lang="ts">
import type {PropType} from 'vue';
import {computed, defineEmits, defineProps, markRaw, ref, watch} from 'vue';
import {useCharacterStore} from '../../../../../stores/personaggio';
import {storeToRefs} from 'pinia';
import {getAllIncantesimiByClasseAndLivello} from '../../../../../service/PersonaggioService';
import Mobile_DettaglioItem from '../Dettaglio/Mobile_DettaglioItem.vue';
import Icona from '../../../../../components/Icona/Icona.vue';
import {iconForComponent} from '../../../../../function/Utils';
import {parseAzioneGlifo} from '../../../../../function/azioni';

const props = defineProps({
  idPersonaggio: {type: Number, required: true},
  livello: {type: Number, required: true},
  classe: {type: String, required: true},
  idClasse: {type: Number, required: true},
  spellList: {type: String, required: true},
  /** mappa spellId -> prepared iniziale (prelevato dalla pagina principale).
   *  Se un valore è -54, viene interpretato come "sempre preparato". */
  preparedInit: {type: Object as PropType<Record<number, number>>, default: () => ({})},
  // Mondo con "Visualizza simboli azioni" attivo (passato dalla pagina scheda, stesso flag già
  // risolto lì): stessa icona azione mostrata nella lista principale, vedi iconeRiga sotto.
  mostraSimboliAzioni: {type: Boolean, default: false}
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
    const data: any = await getAllIncantesimiByClasseAndLivello(props.idClasse, props.livello, props.spellList);
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

watch(() => [props.idClasse, props.livello, props.spellList], () => loadData(), {immediate: true});

// normalizzazione rows (porta anche alwaysPrep, se presente nella risposta)
function normalizeSpell(raw: any) {
  const itm = raw?.item ?? raw?.i ?? raw?.spell ?? raw;
  const id = itm?.id ?? raw?.idItem ?? raw?.itemId;
  const nome = itm?.nome ?? itm?.name ?? itm?.titolo ?? '';
  const alwaysPrep = Boolean(itm?.alwaysPrep ?? itm?.alwaysPrepared ?? false);
  return {id, nome, alwaysPrep, ...itm};
}

const spellsAll = computed(() =>
    (spellsRaw.value ?? [])
        .map(normalizeSpell)
        .filter(s => s?.id != null)
        .sort((a, b) => (a?.nome ?? '').localeCompare(b?.nome ?? ''))
);

// --- filtri ---
const fNome = ref('');
const fLista = ref('');
const fScuola = ref('');   // scuola / sottoscuola / descrittori
const fComp = ref('');     // componenti (V/S/M…)

// true se l'incantesimo è preparato in uno dei due modi (sempre, o un numero > 0) — usato sia
// per ordinare i preparati in cima sia per il badge visibile a riga chiusa (vedi template).
function isPrepared(s: any): boolean {
  return alwaysPrepared.value[s.id] === true || (prepared.value[s.id] ?? 0) > 0;
}

const spells = computed(() => {
  const nome = fNome.value.trim().toLowerCase();
  const lista = fLista.value.trim().toLowerCase();
  const scuola = fScuola.value.trim().toLowerCase();
  const comp = fComp.value.trim().toLowerCase();
  const filtered = spellsAll.value.filter((s: any) => {
    if (nome && !String(s.nome ?? '').toLowerCase().includes(nome)) return false;
    if (lista && !String(s.spellList ?? '').toLowerCase().includes(lista)) return false;
    if (scuola && !String(s.scuola ?? '').toLowerCase().includes(scuola)) return false;
    if (comp) {
      const compStr = Array.isArray(s.componenti) ? s.componenti.join(',').toLowerCase() : '';
      if (!compStr.includes(comp)) return false;
    }
    return true;
  });
  // preparati (in un modo o nell'altro) in cima: spellsAll è già alfabetico, un sort stabile
  // preserva quell'ordine dentro ciascuno dei due gruppi (preparati / non preparati).
  return filtered.slice().sort((a: any, b: any) => Number(isPrepared(b)) - Number(isPrepared(a)));
});

// stato prepared e "sempre"
const prepared = ref<Record<number, number>>({});
const alwaysPrepared = ref<Record<number, boolean>>({});

let preparedInitialSnapshot: Record<number, number> = {};
let alwaysInitialSnapshot: Record<number, boolean> = {};

watch([spellsAll, () => props.preparedInit], () => {
  const nextPrep: Record<number, number> = {};
  const nextAlways: Record<number, boolean> = {};

  for (const s of spellsAll.value) {
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

// Icone di fondo riga (azione + componenti): stessa logica/aspetto della lista principale
// (Mobile_Cico_4_SpellBook.vue, funzione iconeRiga) — qui replicata perché è un popup a parte,
// senza però lo stepper/testo "N/M" (quello che qui si prepara non ha ancora un uso da tracciare).
function iconeRiga(sp: any) {
  const icone: any[] = []
  if (props.mostraSimboliAzioni) {
    const parsed = parseAzioneGlifo(sp.tempo)
    if (parsed && !parsed.resto) icone.push({glyph: parsed.glifo, title: sp.tempo})
  }
  for (const c of (sp.componenti ?? [])) icone.push({name: iconForComponent(c)})
  return icone
}

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
  for (const s of spellsAll.value) {
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
      <h3 class="title">{{ livello === 0 ? 'Cantrip' : `Livello ${livello}` }} · {{ classe }} ({{ spells.length }})</h3>
    </header>

    <div class="popup-body">
      <div v-if="loading" class="state">Caricamento…</div>
      <div v-else-if="error" class="state error">{{ error }}</div>

      <template v-else>
        <div class="filtri">
          <input v-model="fNome" type="text" class="filtro" placeholder="Nome…"/>
          <input v-model="fLista" type="text" class="filtro" placeholder="Lista incantesimi…"/>
          <input v-model="fScuola" type="text" class="filtro" placeholder="Scuola / descrittori…"/>
          <input v-model="fComp" type="text" class="filtro" placeholder="Componenti (V/S/M…)"/>
        </div>

        <div v-if="!spells.length" class="state">Nessun incantesimo disponibile.</div>

        <div v-else class="spell-list">
          <div v-for="sp in spells" :key="sp.id" class="spell-row" :class="{'is-prepared': isPrepared(sp)}">
            <div class="row-top">
              <button class="spell-toggle" @click="toggleExpand(sp.id)" :aria-expanded="isExpanded(sp.id)">
                <span class="spell-name" :title="sp.nome">{{ sp.nome }}</span>
              </button>

              <!-- Icone (azione + componenti) + badge di stato preparazione, visibile anche a riga
                   chiusa: ✓ = sempre preparato, numero = quanti preparati. Editabili aprendo il
                   dettaglio (vedi row-expand sotto, stesso punto dove nella lista principale sta
                   il contatore dei consumati). -->
              <div class="row-icons" @click.stop>
                <span v-for="(ic, i) in iconeRiga(sp)" :key="i" class="row-icon-item">
                  <span v-if="ic.glyph" class="pf2e-icon" :title="ic.title">{{ ic.glyph }}</span>
                  <Icona v-else :name="ic.name" class="row-icon" :title="ic.title || ic.name"/>
                </span>
                <span v-if="alwaysPrepared[sp.id]" class="always-badge" title="Sempre preparato">✓</span>
                <span v-else-if="prepared[sp.id]" class="always-badge" title="Preparati">{{ prepared[sp.id] }}</span>
              </div>
            </div>

            <transition name="expand">
              <div v-if="isExpanded(sp.id)" class="row-expand">
                <component :is="ExpandedComp" :data="{
                  item: sp,
                  personaggio: cache?.[idPersonaggio],
                  prepareEditor: {
                    getValue: () => prepared[sp.id] ?? 0,
                    getAlways: () => alwaysPrepared[sp.id] === true,
                    onInc: () => inc(sp.id),
                    onDec: () => dec(sp.id),
                    onToggleAlways: () => { alwaysPrepared[sp.id] = !alwaysPrepared[sp.id] },
                  }
                }"/>
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

<style scoped>
/* Riga di un incantesimo preparato (in un modo o nell'altro): leggero sfondo per riconoscerla
   anche scorrendo velocemente la lista, oltre al badge ✓/N accanto alle icone. */
.spell-row.is-prepared {
  background: var(--always-badge-bg);
  border-radius: .4rem;
  margin: 0 -.4rem;
  padding-left: .4rem;
  padding-right: .4rem;
}

/* Icone azione + componenti, stesso aspetto della lista principale (Tabella.vue
   .row-icon/.pf2e-icon) — replicate qui perché questo popup non usa Tabella.vue. */
.row-icons {
  display: inline-flex;
  align-items: center;
  gap: .2rem;
}
.row-icon-item {
  display: inline-flex;
  align-items: center;
  font-size: .7rem;
}
.row-icon {
  vertical-align: middle;
}

.filtri {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: .4rem;
  margin-bottom: .6rem;
}
@media (max-width: 520px) {
  .filtri { grid-template-columns: 1fr; }
}
.filtro {
  width: 100%;
  box-sizing: border-box;
  padding: .4rem .55rem;
  border: 1px solid var(--hairline);
  border-radius: .5rem;
  font-size: .85rem;
  background: var(--surface-0);
}
</style>
