<script setup lang="ts">
import {computed, defineProps, markRaw, reactive, ref, watch} from 'vue';
import Tabella from '../../../../../../components/Tabella.vue';
import UtilizziBadge from '../../../../../../components/UtilizziBadge.vue';
import Mobile_DettaglioItem from '../../Dettaglio/Mobile_DettaglioItem.vue';
import SlotContatorePopup from '../../Dettaglio/SlotContatorePopup.vue';
import {useCharacterStore} from '../../../../../../stores/personaggio';
import {storeToRefs} from 'pinia';
import usePopup from '../../../../../../function/usePopup';
import Mobile_Cico_4_SpellBookPrepare from '../../Dettaglio/Mobile_Cico_4_SpellBookPrepare.vue';
import {updatePreparedSpells, updateSpellUsage, resetSlotUsati} from '../../../../../../service/PersonaggioService';
import {getValoreFormula} from '../../../../../../function/Calcolo';
import {iconForComponent} from "../../../../../../function/Utils";
import {SpellBook, SpellBookLivello} from "../../../../../../models/dto/SpellBook";

const props = defineProps({
  idPersonaggio: {type: Number, required: true}
});

const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);
const {openPopup} = usePopup();

/* ----------------- Normalizzazione (sincrona) ----------------- */
/** ATTENZIONE: qui rendiamo ogni riga REATTIVA con reactive(...) */
function normalizeLevels(livelli: any): Array<{
  livello: number; slot?: number; conosciuti?: number; slotConContatore?: boolean; slotUsati?: number;
  bonus?: any[]; incantesimi: any[];
}> {
  if (!livelli) return [];
  const arr = Array.isArray(livelli) ? livelli : Object.values(livelli);
  return arr
      .map((lv: any) => ({
        livello: Number(lv?.livello ?? 0),
        slot: Number(lv?.slot ?? 0),        // base; i bonus arrivano async
        conosciuti: lv?.conosciuti == null ? undefined : Number(lv.conosciuti),
        slotConContatore: Boolean(lv?.slotConContatore),
        slotUsati: lv?.slotUsati == null ? undefined : Number(lv.slotUsati),
        bonus: Array.isArray(lv?.bonus) ? lv.bonus : [],
        incantesimi: (lv?.incantesimi ?? []).map((itm: any) => {
          const nprepared = Number(itm?.nprepared ?? 0);
          const nused = Number(itm?.nused ?? 0);
          // riga REATTIVA
          const row = reactive({
            ...itm,
            nprepared,
            nused,
            get remaining() {
              return Number(this.nprepared) - Number(this.nused);
            }, // calcolato live
            expandedComponent: markRaw(Mobile_DettaglioItem),
            expandedProps: {
              data: {item: {} as any, personaggio: cache.value?.[props.idPersonaggio]}
            }
          });
          // evito copia pesante del dettaglio nell'expansion
          row.expandedProps.data.item = row;
          return row;
        })
      }))
      .sort((a, b) => a.livello - b.livello)
      .map(lv => ({
        ...lv,
        incantesimi: lv.incantesimi
            .slice()
            .sort((a: any, b: any) => (a?.nome ?? '').localeCompare(b?.nome ?? ''))
      }));
}

const groupedByClassLevel = computed(() => {
  const sbs = cache.value?.[props.idPersonaggio]?.items?.spellbooks ?? [];
  return sbs
      .map((sb: any) => ({
        classe: sb?.nomeClasse ?? 'Sconosciuta',
        idClasse: sb?.idClasse,
        sezioneIndice: sb?.sezioneIndice,
        fonteTipo: sb?.fonteTipo,
        spellList: sb?.spellList,
        casterLevel: sb?.casterLevel,
        caratteristica: sb?.caratteristica,
        cd: sb?.cd,
        levels: normalizeLevels(sb?.livelli),
        spurii: sb?.spurii ?? []
      }))
      .sort((a: any, b: any) => (a.classe ?? '').localeCompare(b.classe ?? ''));
});

function fonteTipoLabel(fonteTipo?: string): string {
  return fonteTipo === 'CLASSE' ? 'Classe' : 'Oggetto';
}

/* ----------------- Calcolo async BONUS slot ----------------- */
const slotBonusMap = ref<Record<string, number>>({});
const keySlot = (idClasse: number | string | undefined, livello: number) => `${idClasse ?? 'NA'}:${livello}`;
let lastSlotsRun = 0;

async function recomputeAllSlots() {
  const runId = ++lastSlotsRun;
  const personaggio = cache.value?.[props.idPersonaggio];
  if (!personaggio) return;

  const sbs: any[] = personaggio.items?.spellbooks ?? [];
  const jobs: Promise<void>[] = [];

  for (const sb of sbs) {
    const idClasse = sb?.idClasse;
    const levels = Array.isArray(sb?.livelli) ? sb.livelli : Object.values(sb?.livelli ?? {});
    for (const lv of levels) {
      jobs.push((async () => {
        try {
          const livello = Number(lv?.livello ?? 0);
          const bonusList: any[] = Array.isArray(lv?.bonus) ? lv.bonus : [];
          const modificatori = personaggio.modificatori;

          let bonusTot = 0;
          for (const b of bonusList) {
            const expr = String(b).replace(/#L/g, String(livello));
            const val = await getValoreFormula(modificatori, expr).catch(() => 0);
            // getValoreFormula potrebbe restituire {data:{risultato}}: prova a leggere entrambe
            const num = (val && typeof val === 'object' && 'data' in (val as any) && (val as any).data?.risultato != null)
                ? Number((val as any).data.risultato)
                : Number(val);
            bonusTot += Number.isFinite(num) ? num : 0;
          }

          if (runId !== lastSlotsRun) return;
          slotBonusMap.value[keySlot(idClasse, livello)] = bonusTot;
        } catch {
          // ignora errori singoli
        }
      })());
    }
  }

  await Promise.all(jobs).catch(() => {
  });
}

watch(
    () => cache.value?.[props.idPersonaggio]?.items?.spellbooks,
    () => {
      slotBonusMap.value = {};
      recomputeAllSlots();
    },
    {immediate: true, deep: true}
);

/* Computed helper: base + bonus */
const getSlotDisplay = computed<
    (idClasse: number | undefined, livello: number, base?: number) => number
>(() => (idClasse, livello, base = 0) => {
  const bonus = slotBonusMap.value[keySlot(idClasse, livello)] ?? 0;
  return Number(base ?? 0) + Number(bonus ?? 0);
});

// Testo "slot: X" sulla riga per una sezione con contatore: se non è stato usato nulla (attuale
// == totale) mostra solo il totale, altrimenti "attuale/totale" — stesso criterio di 2/2 vs 1/2
// già usato per gli utilizzi.
const testoSlotRiga = computed<
    (group: any, lv: SpellBookLivello) => string
>(() => (group, lv) => {
  const totale = getSlotDisplay.value(group.idClasse, lv.livello, lv.slot);
  const attuale = Math.max(0, totale - (lv.slotUsati ?? 0));
  return attuale === totale ? `${totale}` : `${attuale}/${totale}`;
});

// Somma di attuale/totale su tutti i livelli con contatore di una sezione, per il riepilogo nel
// titolo ("· Slot: attuale/totale") — null se la sezione non traccia nessun livello con contatore.
const riepilogoSlotGruppo = computed<
    (group: any) => { attuale: number; totale: number } | null
>(() => (group) => {
  const livelliConContatore = (group.levels ?? []).filter((lv: SpellBookLivello) => lv.slotConContatore);
  if (!livelliConContatore.length) return null;
  return livelliConContatore.reduce((acc: { attuale: number; totale: number }, lv: SpellBookLivello) => {
    const totale = getSlotDisplay.value(group.idClasse, lv.livello, lv.slot);
    const attuale = Math.max(0, totale - (lv.slotUsati ?? 0));
    return {attuale: acc.attuale + attuale, totale: acc.totale + totale};
  }, {attuale: 0, totale: 0});
});

// Testo "Slot: X" nel titolo: stesso criterio della riga per livello, solo il totale se non è
// stato usato nulla in tutta la sezione, altrimenti "attuale/totale". Null se non applicabile
// (nessun livello con contatore in questa sezione).
const testoSlotGruppo = computed<
    (group: any) => string | null
>(() => (group) => {
  const r = riepilogoSlotGruppo.value(group);
  if (!r) return null;
  return r.attuale === r.totale ? `${r.totale}` : `${r.attuale}/${r.totale}`;
});

// Totale copie preparate a un livello: somma di nprepared su tutti gli incantesimi del livello
// (se un incantesimo ha più copie preparate, contano tutte — non solo "presente/assente"). Un
// incantesimo "sempre preparato" (alwaysPrep) conta 1, anche se il suo nprepared non è tracciato
// numericamente (è sempre disponibile, non consuma una "copia" preparata come gli altri).
const totalePreparatiLivello = computed<
    (lv: SpellBookLivello) => number
>(() => (lv) => {
  return (lv.incantesimi ?? []).reduce((tot: number, itm: any) => {
    if (itm?.alwaysPrep) return tot + 1;
    const n = Number(itm?.nprepared ?? 0);
    return tot + (Number.isFinite(n) ? Math.max(0, n) : 0);
  }, 0);
});

// Sezioni di livello apribili/chiudibili come una card, chiuse di default (nessuna chiave nel Set
// all'avvio). Chiave = idClasse (o nome classe come fallback) + livello, stessa usata per :key.
const livelliAperti = ref<Set<string>>(new Set())

function levelKey(group: any, lv: SpellBookLivello): string {
  return `${group.idClasse ?? group.classe}-${lv.livello}`
}

function isLivelloAperto(group: any, lv: SpellBookLivello): boolean {
  return livelliAperti.value.has(levelKey(group, lv))
}

function toggleLivello(group: any, lv: SpellBookLivello) {
  const key = levelKey(group, lv)
  const next = new Set(livelliAperti.value)
  next.has(key) ? next.delete(key) : next.add(key)
  livelliAperti.value = next
}

// Un click/tap su "slot: X" (solo se la sezione traccia gli slot con contatore) apre il popup
// con la scomposizione slot/bonus/totale e i controlli per modificare gli slot usati.
function apriPopupSlot(group: any, lv: SpellBookLivello) {
  const bonus = getSlotDisplay.value(group.idClasse, lv.livello, 0)
  openPopup(
      SlotContatorePopup,
      {
        itemId: group.idClasse,
        personaggioId: props.idPersonaggio,
        sezioneIndice: group.sezioneIndice,
        livello: lv.livello,
        slot: lv.slot,
        bonus,
        usati: lv.slotUsati ?? 0,
      },
      {closable: true, autoClose: 0, title: `Slot · Livello ${lv.livello}`}
  )
}

// Azzera tutti i contatori "slot usati" (sezioni con SPELL_<n>_SLOT_CONTATORE) del personaggio.
const resettingSlot = ref(false)
async function handleResetSlot() {
  resettingSlot.value = true
  try {
    await resetSlotUsati(props.idPersonaggio)
    await characterStore.fetchCharacter(props.idPersonaggio, true)
  } finally {
    resettingSlot.value = false
  }
}

/* ----------------- Interazioni UI (optimistic + save) ----------------- */
const saving = ref<Set<number>>(new Set());

function setSaving(id: number, on: boolean) {
  const s = new Set(saving.value);
  on ? s.add(id) : s.delete(id);
  saving.value = s;
}

async function persistUsage(row: any, newUsage: number) {
  // adatta al tuo servizio BE; qui uso un esempio /consumo delta-based
  await updateSpellUsage({
    idPersonaggio: props.idPersonaggio,
    spellId: Number(row.id),
    newUsage
  });
}

async function consumeOne(row: any) {
  const prepared = Number(row.nprepared ?? 0);
  const used = Number(row.nused ?? 0);
  if (prepared - used <= 0) return;
  if (saving.value.has(row.id)) return;

  // optimistic
  const prev = used;
  row.nused = used + 1;
  setSaving(row.id, true);
  try {
    await persistUsage(row, row.nused);
  } catch (e) {
    // rollback
    row.nused = prev;
    console.error('Errore consumo:', e);
  } finally {
    setSaving(row.id, false);
  }
}

async function refundOne(row: any) {
  const used = Number(row.nused ?? 0);
  if (used <= 0) return;
  if (saving.value.has(row.id)) return;

  // optimistic
  const prev = used;
  row.nused = used - 1;
  setSaving(row.id, true);
  try {
    await persistUsage(row, row.nused);
  } catch (e) {
    // rollback
    row.nused = prev;
    console.error('Errore refund:', e);
  } finally {
    setSaving(row.id, false);
  }
}

function columnsForLevel(_lvl: number, spellList?: string) {
  const cols: any[] = [{field: 'nome', label: ''}];
  if (spellList === 'SP_DRUID') {
    cols.push({
      field: 'icons',
      label: '',
      type: 'icons',
      list: (row) => row.componenti.map(c => iconForComponent(c))
    })
    cols.push({
      field: 'remaining',
      label: '',
      type: 'counter',
      counter: {
        value: (row: any) => Number(row.remaining ?? 0),
        max: (row: any) => Number.isFinite(row.nprepared) ? Number(row.nprepared) : null,
        onSub: (row: any) => consumeOne(row),
        onAdd: (row: any) => refundOne(row),
        disableSub: (row: any) => saving.value.has(row.id) || Number(row.remaining ?? 0) <= 0,
        disableAdd: (row: any) => saving.value.has(row.id) || Number(row.nused ?? 0) <= 0,
        hide: (row: any) => row.alwaysPrep === true,
      },
      disabled: (row) => row.nprepared && row.nused === row.nprepared
    });
  }
  return cols;
}

type ShowPopupOpts = { idClasse?: number; classe: string; livello: number; spellList: string };

function showPopup(opts: ShowPopupOpts) {
  const idClasse = opts.idClasse!;
  const livello = opts.livello;
  const SENTINEL_ALWAYS = -54;

  const personaggio = cache.value?.[props.idPersonaggio];
  const sb: SpellBook = (personaggio?.items?.spellbooks ?? []).find((s: any) => s.idClasse === idClasse);
  const lvList: SpellBookLivello[] = Array.isArray(sb?.livelli) ? sb!.livelli : Object.values(sb?.livelli ?? {});
  const lv: SpellBookLivello = lvList.find((l: SpellBookLivello) => l?.livello === livello);
  const preparedInit: Record<number, number> = Object.fromEntries(
      (lv?.incantesimi ?? []).map((s: any) => {
        const raw = Number(s?.nprepared);
        const isAlways = Boolean(s?.alwaysPrep ?? s?.alwaysPrepared ?? false) || raw === SENTINEL_ALWAYS;
        const value = isAlways ? SENTINEL_ALWAYS : (Number.isFinite(raw) ? Math.max(0, Math.trunc(raw)) : 0);
        return [Number(s.id), value];
      })
  );

  openPopup(
      Mobile_Cico_4_SpellBookPrepare,
      {
        idClasse,
        classe: opts.classe,
        livello,
        spellList: opts.spellList,
        idPersonaggio: props.idPersonaggio,
        preparedInit,

        async onConfirm(payload: {
          idClasse: number;
          classe: string;
          livello: number;
          spellList: string;
          prepared: Record<number, number>;
        }) {
          await updatePreparedSpells({
            idPersonaggio: props.idPersonaggio,
            idClasse: payload.idClasse,
            spellList: payload.spellList,
            livello: payload.livello,
            prepared: payload.prepared
          });

          await characterStore.fetchCharacter(props.idPersonaggio, true);
          await recomputeAllSlots();
        },
        onClose() {
        }
      },
      {closable: true, autoClose: 0}
  );
}
</script>

<template>
  <div>
    <div class="reset-slot-row">
      <button class="btn-reset" type="button" :disabled="resettingSlot" @click="handleResetSlot">
        {{ resettingSlot ? '…' : 'Azzera slot incantesimo' }}
      </button>
    </div>
    <section v-for="group in groupedByClassLevel" :key="group.classe" class="mb-4">
      <h3 class="classe-title">
        {{ group.classe }}
        <span class="muted fonte-tipo">Da: {{ fonteTipoLabel(group.fonteTipo) }}</span>
        <span v-if="group.casterLevel != null" class="muted"> · CL: {{ group.casterLevel }}</span>
        <span v-if="group.cd != null" class="muted"> · CD: {{ group.cd }}</span>
        <span v-if="testoSlotGruppo(group)" class="muted"> · Slot: {{ testoSlotGruppo(group) }}</span>
      </h3>

      <div v-for="lv in group.levels" :key="`${group.idClasse ?? group.classe}-${lv.livello}`" class="level-block">
        <div class="level-header">
          <div class="level-title" @click="toggleLivello(group, lv)">
            <span class="chev" :class="{ open: isLivelloAperto(group, lv) }">▸</span>
            {{ lv.livello === 0 ? 'Cantrip' : `Livello ${lv.livello}` }}
            <span v-if="!lv.slotConContatore" class="muted"> · slot: {{ getSlotDisplay(group.idClasse, lv.livello, lv.slot) }}</span>
            <!-- sezione con contatore: un click apre il popup slot/bonus/totale + controlli -->
            <span v-else class="muted slot-hold" @click.stop="apriPopupSlot(group, lv)">
              · slot: {{ testoSlotRiga(group, lv) }}
            </span>
            <span v-if="lv.conosciuti != null" class="muted"> · conosciuti: {{ lv.conosciuti }}</span>
            <span class="muted"> · preparati: {{ totalePreparatiLivello(lv) }}</span>
          </div>
          <button
              class="prepare-btn"
              @click="showPopup({ idClasse: group.idClasse, classe: group.classe, livello: lv.livello, spellList: group.spellList })"
              title="Prepara incantesimi"
          >
            Prepara
          </button>
        </div>

        <Tabella
            v-if="isLivelloAperto(group, lv)"
            class="mb-3"
            :columns="columnsForLevel(lv.livello, group.spellList)"
            :expandable="true"
            :items="lv.incantesimi"
        />
      </div>

      <div v-if="group.spurii?.length" class="level-block">
        <div class="level-header">
          <div class="level-title">Incantesimi extra</div>
        </div>
        <div v-for="s in group.spurii" :key="s.id" class="spurio-row">
          <span class="spurio-nome">{{ s.nome }}</span>
          <UtilizziBadge
              v-if="s.utilizziTotale != null"
              :item-id="s.id"
              :personaggio-id="idPersonaggio"
              :usati="s.utilizziUsati ?? 0"
              :totale="s.utilizziTotale"
          />
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.slot-hold {
  cursor: pointer;
  user-select: none;
  -webkit-user-select: none;
  touch-action: manipulation;
  -webkit-touch-callout: none;
}
.level-title {
  cursor: pointer;
  user-select: none;
}
.chev {
  display: inline-block;
  transition: transform .15s ease;
  margin-right: .3rem;
}
.chev.open { transform: rotate(90deg); }
.reset-slot-row {
  display: flex;
  justify-content: flex-end;
  margin-bottom: .75rem;
}
.btn-reset {
  flex-shrink: 0;
  padding: .35rem .75rem;
  border: 1px solid var(--danger-border);
  background: var(--danger-bg);
  color: var(--danger-text);
  border-radius: .5rem;
  cursor: pointer;
}
.btn-reset:hover { background: var(--danger-border); }
.btn-reset:disabled { opacity: .5; cursor: default; }
</style>
