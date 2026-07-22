<script setup lang="ts">
import {computed} from 'vue'
import {Items} from '../../../../../../models/dto/Items'
import {DatiPersonaggio} from '../../../../../../models/dto/DatiPersonaggio'

const props = defineProps<{
  data: {
    modificatori: DatiPersonaggio
    items: Items
  }
}>()

const TAGLIE: Record<number, string> = {
  '-4': 'Piccolissima', '-3': 'Minuta', '-2': 'Minuscola', '-1': 'Piccola',
  0: 'Media', 1: 'Grande', 2: 'Enorme', 3: 'Mastodontica', 4: 'Colossale',
}
const nomeTag = (n: number | undefined) => n != null ? (TAGLIE[n] ?? `${n}`) : '—'

const mod   = computed(() => props.data.modificatori)
const items = computed(() => props.data.items)

// ---- Peso monete (arriva direttamente dal backend, già calcolato con calcolaSoldi) ----
const coinWeight = computed(() => mod.value?.pesoMonete ?? 0)

// ---- Peso personaggio (label PESO dell'anagrafica), già moltiplicato per taglia dal backend) ----
const pesoPersonaggioRaw = computed(() => {
  const v = mod.value?.info?.PESO
  if (!v) return 0
  return parseFloat(v.replace(',', '.')) || 0
})
const pesoPersonaggio = computed(() => {
  // pesoSenzaTaglia = peso corporeo prima del modificatore; se non c'è diff usiamo raw
  if (diffTaglia.value !== 0 && (mod.value?.pesoSenzaTaglia ?? 0) > 0) {
    const base = mod.value!.pesoSenzaTaglia!
    return diffTaglia.value > 0 ? base * multTaglia.value : base / multTaglia.value
  }
  return pesoPersonaggioRaw.value
})

// ---- Taglia ----
const tagliaBase    = computed(() => mod.value?.tagliaBase ?? 0)
const tagliaAttuale = computed(() => mod.value?.tagliaAttuale ?? tagliaBase.value)
const diffTaglia    = computed(() => tagliaAttuale.value - tagliaBase.value)
const multTaglia    = computed(() => diffTaglia.value !== 0 ? Math.pow(8, Math.abs(diffTaglia.value)) : 1)
const pesoSenzaTaglia = computed(() => mod.value?.pesoSenzaTaglia ?? mod.value?.pesoTotale ?? 0)
const pesoTotale    = computed(() => mod.value?.pesoTotale ?? 0)

// ---- Algoritmo greedy (identico al backend) ----
type ContItem = { id: number; nome: string; peso: number; capienza: number;
                  includiArmi: boolean; includiOggetti: boolean; includiConsumabili: boolean; includiTutti: boolean }
type FlatItem = { id: number; nome: string; peso: number; tipo: string; disabled: boolean }
type Slot = ContItem & { items: FlatItem[]; armiItems: FlatItem[]; oggettiItems: FlatItem[];
                          consumabiliItems: FlatItem[]; altroItems: FlatItem[]; filled: number }

const breakdown = computed(() => {
  const allItems = items.value
  if (!allItems) return null

  const contenitori: ContItem[] = (allItems.contenitori ?? [])
    .filter(c => (c.capienza ?? 0) > 0)
    .map(c => ({
      id: c.id,
      nome: c.nome,
      peso: c.peso ?? 0,
      capienza: c.capienza ?? 0,
      includiArmi: !!c.includiArmiAbilitate,
      includiOggetti: !!c.includiOggettiAbilitati,
      includiConsumabili: !!c.includiConsumabiliAbilitati,
      includiTutti: !!c.includiTuttiAbilitati,
    }))
    .sort((a, b) => b.capienza - a.capienza)

  const withPeso = (list: any[], tipo: string): FlatItem[] =>
    (list ?? []).filter(i => (i.peso ?? 0) > 0).map(i => ({
      id: i.id, nome: i.nome,
      peso: (i.peso ?? 0) * (i.quantita ?? 1),
      tipo, disabled: !!i.disabled,
    }))
  // Item dentro un CONTENITORE "separato" (es. la Stiva di una NAVE): per il peso contano come
  // qualunque altro item (stesso principio lato backend, vedi calcolaPeso), quindi eterogenei per
  // tipo reale invece che di un'unica lista tipizzata — a differenza di withPeso sopra.
  const withPesoMisto = (list: any[]): FlatItem[] =>
    (list ?? []).filter(i => (i.peso ?? 0) > 0).map(i => ({
      id: i.id, nome: i.nome,
      peso: (i.peso ?? 0) * (i.quantita ?? 1),
      tipo: i.tipo, disabled: !!i.disabled,
    }))
  const separatoItems = (allItems.inventariSeparati ?? []).flatMap(sep => sep.items ?? [])

  const all = [
    ...withPeso(allItems.oggetti, 'OGGETTO'),
    ...withPeso(allItems.consumabili, 'CONSUMABILE'),
    ...withPeso(allItems.munizioni, 'MUNIZIONI'),
    ...withPeso(allItems.armi, 'ARMA'),
    ...withPeso(allItems.equipaggiamento, 'EQUIPAGGIAMENTO'),
    ...withPeso(allItems.frutti, 'FRUTTO'),
    ...withPeso(allItems.idoli, 'IDOLO'),
    ...withPeso(allItems.patti, 'PATTO'),
    ...withPesoMisto(separatoItems),
  ]

  const disabled      = all.filter(i => i.disabled)
  const armeAb        = all.filter(i => !i.disabled && i.tipo === 'ARMA')
  const oggettiAb     = all.filter(i => !i.disabled && i.tipo === 'OGGETTO')
  const consumabiliAb = all.filter(i => !i.disabled && i.tipo === 'CONSUMABILE')
  const altriAbilitati = all.filter(i => !i.disabled && i.tipo !== 'ARMA' && i.tipo !== 'OGGETTO' && i.tipo !== 'CONSUMABILE')

  const slots: Slot[] = contenitori.map(c => ({...c, items: [], armiItems: [], oggettiItems: [], consumabiliItems: [], altroItems: [], filled: 0}))

  function distribuisci(pool: FlatItem[], accept: (s: Slot) => boolean, push: (s: Slot, i: FlatItem) => void) {
    const rem = [...pool]
    for (const slot of slots) {
      if (!accept(slot)) continue
      const toRemove: FlatItem[] = []
      for (const itm of rem) {
        if (slot.filled + itm.peso <= slot.capienza + 1e-9) {
          slot.filled += itm.peso
          push(slot, itm)
          toRemove.push(itm)
        }
      }
      toRemove.forEach(i => rem.splice(rem.indexOf(i), 1))
    }
    return rem // overflow
  }

  const overDisabled      = distribuisci(disabled,      () => true,            (s, i) => s.items.push(i))
  const overArme          = distribuisci(armeAb,        s => s.includiArmi || s.includiTutti,    (s, i) => s.armiItems.push(i))
  const overOggetti       = distribuisci(oggettiAb,     s => s.includiOggetti || s.includiTutti, (s, i) => s.oggettiItems.push(i))
  const overConsumabili   = distribuisci(consumabiliAb, s => s.includiConsumabili || s.includiTutti, (s, i) => s.consumabiliItems.push(i))
  const overAltri         = distribuisci(altriAbilitati, s => s.includiTutti, (s, i) => s.altroItems.push(i))

  // Distribuzione monete negli spazi rimasti
  let remCoins = coinWeight.value
  const slotCoins: number[] = slots.map(slot => {
    const space = slot.capienza - slot.filled
    const coinIn = Math.min(space, remCoins)
    slot.filled += coinIn
    remCoins -= coinIn
    return coinIn
  })

  // Overflow totale = tutte le categorie non assorbite + monete overflow
  const overflow: Array<{ nome: string; peso: number; label?: string }> = [
    ...overDisabled.map(i => ({nome: i.nome, peso: i.peso})),
    ...overArme.map(i => ({nome: i.nome, peso: i.peso, label: 'arma equipaggiata'})),
    ...overOggetti.map(i => ({nome: i.nome, peso: i.peso, label: 'oggetto equipaggiato'})),
    ...overConsumabili.map(i => ({nome: i.nome, peso: i.peso, label: 'consumabile equipaggiato'})),
    ...overAltri.map(i => ({nome: i.nome, peso: i.peso})),
  ]

  return {slots, slotCoins, overflow, remCoins, overflowTotal: overflow.reduce((s, i) => s + i.peso, 0)}
})

function fmt(n: number) {
  return n % 1 === 0 ? `${n}` : n.toFixed(2)
}
function pesoEffettivo(slot: Slot) {
  return slot.capienza > 0 ? slot.peso * Math.min(1, slot.filled / slot.capienza) : 0
}
</script>

<template>
  <div class="peso-popup">
    <h3 class="popup-title">Dettaglio Peso</h3>

    <!-- Peso personaggio (corpo) -->
    <div v-if="pesoPersonaggio > 0" class="row-main">
      <span>Peso personaggio</span>
      <span class="val">{{ fmt(pesoPersonaggio) }} kg</span>
    </div>

    <!-- Modificatore taglia -->
    <div v-if="diffTaglia !== 0" class="taglia-block">
      <div class="taglia-main">
        <span class="taglia-label">
          Modificatore taglia {{ diffTaglia > 0 ? '×' : '÷' }}{{ fmt(multTaglia) }}
        </span>
        <span class="taglia-val">
          {{ fmt(diffTaglia > 0 ? pesoSenzaTaglia * multTaglia : pesoSenzaTaglia / multTaglia) }} kg
        </span>
      </div>
      <div class="taglia-sub">
        {{ nomeTag(tagliaBase) }} → {{ nomeTag(tagliaAttuale) }}
        <span class="taglia-calc">{{ fmt(pesoSenzaTaglia) }} {{ diffTaglia > 0 ? '×' : '÷' }} {{ fmt(multTaglia) }}</span>
      </div>
    </div>

    <div class="divider"/>

    <!-- Contenitori -->
    <template v-if="breakdown">
      <div v-for="(slot, i) in breakdown.slots" :key="slot.id" class="container-block">
        <div class="row-container">
          <span class="container-nome">{{ slot.nome }}</span>
          <span class="container-peso">
            {{ fmt(pesoEffettivo(slot)) }} kg
            <span class="muted">({{ fmt(slot.filled) }} / {{ slot.capienza }} kg contenuti)</span>
          </span>
        </div>

        <!-- Item disabilitati dentro al contenitore -->
        <div v-for="itm in slot.items" :key="itm.id" class="row-child">
          <span>{{ itm.nome }}</span>
          <span class="val muted">{{ fmt(itm.peso) }} kg</span>
        </div>
        <div v-for="itm in slot.armiItems" :key="'a'+itm.id" class="row-child chip-arma">
          <span>{{ itm.nome }} <span class="tag">arma eq.</span></span>
          <span class="val muted">{{ fmt(itm.peso) }} kg</span>
        </div>
        <div v-for="itm in slot.oggettiItems" :key="'o'+itm.id" class="row-child chip-oggetto">
          <span>{{ itm.nome }} <span class="tag">oggetto eq.</span></span>
          <span class="val muted">{{ fmt(itm.peso) }} kg</span>
        </div>
        <div v-for="itm in slot.consumabiliItems" :key="'c'+itm.id" class="row-child chip-consumabile">
          <span>{{ itm.nome }} <span class="tag">consumabile eq.</span></span>
          <span class="val muted">{{ fmt(itm.peso) }} kg</span>
        </div>
        <div v-for="itm in slot.altroItems" :key="'x'+itm.id" class="row-child chip-altro">
          <span>{{ itm.nome }}</span>
          <span class="val muted">{{ fmt(itm.peso) }} kg</span>
        </div>
        <div v-if="breakdown.slotCoins[i] > 0.001" class="row-child chip-monete">
          <span>Monete</span>
          <span class="val muted">{{ fmt(breakdown.slotCoins[i]) }} kg</span>
        </div>
        <div v-if="slot.items.length === 0 && slot.armiItems.length === 0
                   && slot.oggettiItems.length === 0 && slot.consumabiliItems.length === 0
                   && slot.altroItems.length === 0 && breakdown.slotCoins[i] === 0" class="row-child muted">
          (vuoto)
        </div>
      </div>

      <!-- Item fuori da qualsiasi contenitore + monete residue (overflow) -->
      <template v-if="breakdown.overflow.length || breakdown.remCoins > 0.001">
        <div class="divider"/>
        <div v-for="(itm, i) in breakdown.overflow" :key="i" class="row-main">
          <span>{{ itm.nome }} <span v-if="itm.label" class="tag">{{ itm.label }}</span></span>
          <span class="val">{{ fmt(itm.peso) }} kg</span>
        </div>
        <!-- Monete che non sono entrate in nessun contenitore -->
        <div v-if="breakdown.remCoins > 0.001" class="row-main chip-monete-row">
          <span>Monete</span>
          <span class="val">{{ fmt(breakdown.remCoins) }} kg</span>
        </div>
      </template>

      <!-- Monete: nessun contenitore (tutto come addosso) -->
      <div v-if="breakdown.slots.length === 0 && coinWeight > 0" class="row-main chip-monete-row">
        <span>Monete</span>
        <span class="val">{{ fmt(coinWeight) }} kg</span>
      </div>
    </template>

    <div class="divider"/>

    <!-- Totale -->
    <div class="row-total">
      <span>Totale</span>
      <span class="val-total">{{ fmt(pesoTotale) }} kg</span>
    </div>
  </div>
</template>

<style scoped>
.peso-popup {
  padding: .75rem;
  min-width: 280px;
  max-width: 420px;
  display: flex;
  flex-direction: column;
  gap: .1rem;
}
.popup-title {
  margin: 0 0 .5rem;
  font-size: 1rem;
  font-weight: 700;
}
.row-main, .row-total {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: .5rem;
  padding: .2rem 0;
  font-size: .88rem;
}
.taglia-block {
  padding: .2rem 0;
}
.taglia-main {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: .5rem;
  font-size: .88rem;
  font-weight: 600;
  color: #d97706;
}
.taglia-label { flex: 1; }
.taglia-val { flex-shrink: 0; font-variant-numeric: tabular-nums; }
.taglia-sub {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: .5rem;
  font-size: .76rem;
  color: var(--color-text-secondary, #6b7280);
  padding-left: .1rem;
}
.taglia-calc { font-variant-numeric: tabular-nums; flex-shrink: 0; }
.row-container {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: .5rem;
  padding: .35rem 0 .1rem;
  font-size: .9rem;
  font-weight: 700;
  border-top: 1px solid var(--color-border, #e5e7eb);
  margin-top: .3rem;
}
.container-nome { flex: 1; }
.container-peso { text-align: right; font-variant-numeric: tabular-nums; }
.row-child {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: .1rem 0 .1rem 1rem;
  font-size: .82rem;
  gap: .5rem;
}
.row-total {
  padding-top: .4rem;
  font-weight: 700;
  font-size: .95rem;
  border-top: 2px solid var(--color-border, #e5e7eb);
  margin-top: .2rem;
}
.val { text-align: right; font-variant-numeric: tabular-nums; flex-shrink: 0; }
.val-total { text-align: right; font-variant-numeric: tabular-nums; flex-shrink: 0; font-size: 1rem; }
.divider { height: 1px; background: var(--color-border, #e5e7eb); margin: .35rem 0; }
.muted { color: var(--color-text-secondary, #6b7280); }
.tag {
  display: inline-block;
  font-size: .65rem;
  padding: .05rem .3rem;
  border-radius: 999px;
  background: var(--color-surface-2, #f3f4f6);
  color: var(--color-text-secondary, #6b7280);
  font-weight: 600;
  margin-left: .2rem;
  vertical-align: middle;
}
.chip-arma    { background: #fefce8; }
.chip-oggetto { background: #eff6ff; }
.chip-consumabile { background: #fdf4ff; }
.chip-altro   { background: #f3f4f6; }
.chip-monete  { background: #f0fdf4; }
.chip-monete-row { color: #15803d; font-weight: 500; }
</style>
