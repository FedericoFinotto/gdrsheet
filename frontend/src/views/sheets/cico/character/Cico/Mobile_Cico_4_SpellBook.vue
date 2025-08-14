<script setup lang="ts">
import {computed, defineProps, markRaw, reactive, ref, watch} from 'vue';
import Tabella from '../../../../../components/Tabella.vue';
import Mobile_DettaglioItem from '../Dettaglio/Mobile_DettaglioItem.vue';
import {useCharacterStore} from '../../../../../stores/personaggio';
import {storeToRefs} from 'pinia';
import usePopup from '../../../../../function/usePopup';
import Mobile_Cico_4_SpellBookPrepare from '../Dettaglio/Mobile_Cico_4_SpellBookPrepare.vue';
import {updatePreparedSpells, updateSpellUsage} from '../../../../../service/PersonaggioService';
import {getValoreFormula} from '../../../../../function/Calcolo';

const props = defineProps({
  idPersonaggio: {type: Number, required: true}
});

const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);
const {openPopup} = usePopup();

/* ----------------- Normalizzazione (sincrona) ----------------- */
/** ATTENZIONE: qui rendiamo ogni riga REATTIVA con reactive(...) */
function normalizeLevels(livelli: any): Array<{
  livello: number; slot?: number; bonus?: any[]; incantesimi: any[];
}> {
  if (!livelli) return [];
  const arr = Array.isArray(livelli) ? livelli : Object.values(livelli);
  return arr
      .map((lv: any) => ({
        livello: Number(lv?.livello ?? 0),
        slot: Number(lv?.slot ?? 0),        // base; i bonus arrivano async
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
        spellList: sb?.spellList,
        levels: normalizeLevels(sb?.livelli)
      }))
      .sort((a: any, b: any) => (a.classe ?? '').localeCompare(b.classe ?? ''));
});

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

          if (runId !== lastSlotsRun) return; // evita race
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
      }
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
  const sb = (personaggio?.items?.spellbooks ?? []).find((s: any) => s.idClasse === idClasse);
  const lvList = Array.isArray(sb?.livelli) ? sb!.livelli : Object.values(sb?.livelli ?? {});
  const lv = lvList.find((l: any) => Number(l?.livello) === livello);
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
    <section v-for="group in groupedByClassLevel" :key="group.classe" class="mb-4">
      <h3 class="classe-title">
        {{ group.classe }}
      </h3>

      <div v-for="lv in group.levels" :key="`${group.idClasse ?? group.classe}-${lv.livello}`" class="level-block">
        <div class="level-header">
          <div class="level-title">
            {{ lv.livello === 0 ? 'Cantrip' : `Livello ${lv.livello}` }}
            <span class="muted"> · slot: {{ getSlotDisplay(group.idClasse, lv.livello, lv.slot) }}</span>
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
            class="mb-3"
            :columns="columnsForLevel(lv.livello, group.spellList)"
            :expandable="true"
            :items="lv.incantesimi"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>

</style>
