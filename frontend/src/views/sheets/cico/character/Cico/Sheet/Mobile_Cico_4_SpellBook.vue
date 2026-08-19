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
import {updatePreparedSpells, updateSpellUsage, resetSlotUsati, setManaUsati} from '../../../../../../service/PersonaggioService';
import {getValoreFormula} from '../../../../../../function/Calcolo';
import {iconForComponent} from "../../../../../../function/Utils";
import {parseAzioneGlifo} from "../../../../../../function/azioni";
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
              data: {item: {} as any, personaggio: cache.value?.[props.idPersonaggio], prepCounter: null as any}
            }
          });
          // evito copia pesante del dettaglio nell'expansion
          row.expandedProps.data.item = row;
          // stepper +/- del dettaglio esteso: stessa logica optimistic+persist già usata qui
          // (consumeOne/refundOne), solo spostata di posto nell'UI — vedi Mobile_DettaglioItem.vue
          row.expandedProps.data.prepCounter = {onSub: () => consumeOne(row), onAdd: () => refundOne(row)};
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
        mostraSimboliAzioni: sb?.mostraSimboliAzioni === true,
        mostraCasterLevel: sb?.mostraCasterLevel !== false,
        soloConosciuti: sb?.soloConosciuti === true,
        sistemaIncantesimi: sb?.sistemaIncantesimi ?? 'SLOT',
        manaUsati: Number(sb?.manaUsati ?? 0),
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
// Pool di mana totale di una sezione (sistemaIncantesimi === 'MANA'): stessa chiave usata per il
// gruppo (idClasse+sezioneIndice, non per livello — un solo pool condiviso per l'intera sezione).
const manaTotaleMap = ref<Record<string, number>>({});
const keyMana = (idClasse: number | string | undefined, sezioneIndice: number | undefined) => `${idClasse ?? 'NA'}:${sezioneIndice ?? 0}`;
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
    // Pool di mana (mondo a sistema MANA): formula unica per l'intera sezione, stessa logica di
    // calcolo delle formule bonus slot (getValoreFormula), non una per livello.
    if (sb?.sistemaIncantesimi === 'MANA' && sb?.formulaManaTotale) {
      jobs.push((async () => {
        try {
          const val = await getValoreFormula(personaggio.modificatori, String(sb.formulaManaTotale)).catch(() => 0);
          const num = (val && typeof val === 'object' && 'data' in (val as any) && (val as any).data?.risultato != null)
              ? Number((val as any).data.risultato)
              : Number(val);
          if (runId !== lastSlotsRun) return;
          manaTotaleMap.value[keyMana(idClasse, sb?.sezioneIndice)] = Number.isFinite(num) ? num : 0;
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

// Pool di mana (mondo a sistema MANA): totale dalla formula del mondo, usati dal contatore
// persistito lato personaggio — un solo pool condiviso per l'intera sezione (non per livello).
function manaTotaleGruppo(group: any): number {
  return manaTotaleMap.value[keyMana(group.idClasse, group.sezioneIndice)] ?? 0
}
const busyMana = reactive<Record<string, boolean>>({})
async function adeguaMana(group: any, delta: number) {
  const k = keyMana(group.idClasse, group.sezioneIndice)
  if (busyMana[k] || group.idClasse == null) return
  const nuovo = Math.max(0, (group.manaUsati ?? 0) + delta)
  if (nuovo === group.manaUsati) return
  busyMana[k] = true
  try {
    await setManaUsati(group.idClasse, props.idPersonaggio, group.sezioneIndice ?? 0, nuovo)
    await characterStore.fetchCharacter(props.idPersonaggio, true)
  } finally {
    busyMana[k] = false
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

// Icone di fondo riga: simbolo azione (se il mondo ha "Visualizza simboli azioni" attivo e il
// Tempo di Lancio è un pattern riconosciuto, vedi function/azioni.ts) seguito dai simboli
// componenti (V/S/M/...) — stesse icone mostrate nel dettaglio esteso dell'incantesimo.
function iconeRiga(row: any, mostraSimboliAzioni: boolean) {
  const icone: any[] = []
  if (mostraSimboliAzioni) {
    const parsed = parseAzioneGlifo(row.tempo)
    if (parsed && !parsed.resto) icone.push({glyph: parsed.glifo, title: row.tempo})
  }
  icone.push(...(row.componenti ?? []).map((c: string) => ({name: iconForComponent(c)})))
  return icone
}

// Testo compatto "rimasti/preparati" dopo le icone: solo per incantesimi con preparazione
// tracciata (non "sempre preparato" — per quelli l'icona basta, nessun numero da mostrare). Lo
// stepper +/- interattivo per consumare/recuperare un uso vive nel dettaglio esteso della riga
// (Mobile_DettaglioItem.vue, contatore "prepCounter" cablato sotto in normalizeLevels).
function testoContatore(row: any): string | null {
  if (row.alwaysPrep === true) return null
  return `${Number(row.remaining ?? 0)}/${Number(row.nprepared ?? 0)}`
}

function columnsForLevel(_lvl: number, spellList?: string, mostraSimboliAzioni?: boolean) {
  const cols: any[] = [{field: 'nome', label: ''}];
  // Icone (simbolo azione + componenti) + testo "rimasti/preparati": sempre, per ogni lista
  // incantesimi — ogni riga qui è già un incantesimo effettivamente preparato (vedi backend
  // generateSpellBookSezione, che popola i livelli SOLO dai figli di ITEM_INCANTESIMI_PREPARATI),
  // quindi nprepared/nused sono sempre significativi indipendentemente dalla classe.
  cols.push({
    field: 'icons',
    label: '',
    type: 'icons',
    list: (row) => iconeRiga(row, !!mostraSimboliAzioni),
    counterText: testoContatore,
  })
  return cols;
}

type ShowPopupOpts = { idClasse?: number; classe: string; livello: number; spellList: string; mostraSimboliAzioni?: boolean };

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
        mostraSimboliAzioni: !!opts.mostraSimboliAzioni,

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
        <span v-if="group.casterLevel != null && group.mostraCasterLevel" class="muted"> · CL: {{ group.casterLevel }}</span>
        <span v-if="group.cd != null" class="muted"> · CD: {{ group.cd }}</span>
        <span v-if="!group.soloConosciuti && group.sistemaIncantesimi !== 'MANA' && testoSlotGruppo(group)" class="muted"> · Slot: {{ testoSlotGruppo(group) }}</span>
        <!-- Mondo a sistema MANA: un pool condiviso per l'intera sezione (Mondo.formulaManaIncantesimi),
             non slot per livello — "−" spende un punto mana, "+" lo rimborsa (stessa convenzione
             dello stepper preparati/usati sopra). -->
        <span v-if="group.sistemaIncantesimi === 'MANA'" class="muted mana-hold">
          · 🔷 Mana:
          <button type="button" class="mana-btn" :disabled="busyMana[keyMana(group.idClasse, group.sezioneIndice)] || (manaTotaleGruppo(group) - group.manaUsati) <= 0"
                  @click.stop="adeguaMana(group, 1)">−</button>
          {{ Math.max(0, manaTotaleGruppo(group) - group.manaUsati) }}/{{ manaTotaleGruppo(group) }}
          <button type="button" class="mana-btn" :disabled="busyMana[keyMana(group.idClasse, group.sezioneIndice)] || group.manaUsati <= 0"
                  @click.stop="adeguaMana(group, -1)">+</button>
        </span>
      </h3>

      <div v-for="lv in group.levels" :key="`${group.idClasse ?? group.classe}-${lv.livello}`" class="level-block">
        <div class="level-header">
          <div class="level-title" @click="toggleLivello(group, lv)">
            <span class="chev" :class="{ open: isLivelloAperto(group, lv) }">▸</span>
            {{ lv.livello === 0 ? 'Cantrip' : `Livello ${lv.livello}` }}
            <!-- Sezione "classe di riferimento" (solo oggetti): niente pool di slot separato, il
                 numero disponibile è già quello dei conosciuti — niente "· slot: X", solo
                 l'icona libro con preparati/disponibili. -->
            <span v-if="group.soloConosciuti" class="muted">
              📖 {{ totalePreparatiLivello(lv) }}/{{ getSlotDisplay(group.idClasse, lv.livello, lv.slot) }}
            </span>
            <!-- Mondo a sistema MANA: niente slot/preparati per livello, il pool è unico per la
                 sezione (vedi il titolo della sezione) — qui solo il costo in mana di un incantesimo
                 di questo livello. -->
            <span v-else-if="group.sistemaIncantesimi === 'MANA'" class="muted">
              · 🔷 costo: {{ lv.livello }} mana
            </span>
            <template v-else>
              <span v-if="!lv.slotConContatore" class="muted"> · slot: {{ getSlotDisplay(group.idClasse, lv.livello, lv.slot) }}</span>
              <!-- sezione con contatore: un click apre il popup slot/bonus/totale + controlli -->
              <span v-else class="muted slot-hold" @click.stop="apriPopupSlot(group, lv)">
                · slot: {{ testoSlotRiga(group, lv) }}
              </span>
              <span v-if="lv.conosciuti != null" class="muted"> · conosciuti: {{ lv.conosciuti }}</span>
              <span class="muted"> · preparati: {{ totalePreparatiLivello(lv) }}</span>
            </template>
          </div>
          <button
              class="prepare-btn"
              @click="showPopup({ idClasse: group.idClasse, classe: group.classe, livello: lv.livello, spellList: group.spellList, mostraSimboliAzioni: group.mostraSimboliAzioni })"
              title="Prepara incantesimi"
          >
            Prepara
          </button>
        </div>

        <Tabella
            v-if="isLivelloAperto(group, lv)"
            class="mb-3"
            :columns="columnsForLevel(lv.livello, group.spellList, group.mostraSimboliAzioni)"
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
.mana-hold {
  display: inline-flex;
  align-items: center;
  gap: .2rem;
}
.mana-btn {
  border: 1px solid var(--hairline);
  background: var(--surface-0);
  border-radius: .3rem;
  width: 1.2rem;
  height: 1.2rem;
  line-height: 1;
  font-size: .8rem;
  cursor: pointer;
  padding: 0;
}
.mana-btn:disabled {
  opacity: .5;
  cursor: default;
}
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
