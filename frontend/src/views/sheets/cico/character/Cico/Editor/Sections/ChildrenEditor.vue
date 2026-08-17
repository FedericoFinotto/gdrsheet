<script setup lang="ts">
import {ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ChildRef} from '../../../../../../../models/dto/UpdateItemRequest'
import {searchItems} from '../../../../../../../service/PersonaggioService'
import {Item} from '../../../../../../../models/dto/Item'
import {useMondoStore} from '../../../../../../../stores/mondo'

const props = defineProps<{
  modelValue: ChildRef[]
  disabled?: boolean
  excludeId?: number   // id dell'item in editing: non collegabile a se stesso
  onlyTipo?: string    // mostra/cerca solo questo tipo
  excludeTipo?: string // mostra/cerca qualsiasi tipo tranne questo
  hideCreate?: boolean // nasconde "+ Crea nuovo" e il "+" accanto alla ricerca (es. picker NODO)
}>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: ChildRef[]): void
  (e: 'create-new', tipo: string | undefined, nome?: string): void
}>()

const router = useRouter()
const route = useRoute()
const mondoStore = useMondoStore()
mondoStore.carica() // idempotente: se già in corso/fatto altrove (es. UpperBar) non rifà la chiamata

function editChild(c: ChildRef) {
  const idPg = route.query.personaggio
  const path = `/itemeditor/${c.id}` + (idPg ? `?personaggio=${idPg}` : '')
  router.push(path)
}

const query = ref('')
const results = ref<Item[]>([])
const searching = ref(false)
let searchToken = 0

async function doSearch() {
  const q = query.value.trim()
  if (q.length < 2) {
    results.value = []
    return
  }
  const token = ++searchToken
  searching.value = true
  try {
    // onlyTipo passato al backend, non solo filtrato dopo: altrimenti con onlyTipo impostato (es.
    // "A"/"Da" di un NODO, ristretti a soli NODO) i primi 20 risultati per nome potevano essere
    // tutti di altri tipi, lasciando fuori dai risultati proprio i NODO che si stavano cercando.
    const res = await searchItems(q, props.onlyTipo, mondoStore.corrente)
    if (token !== searchToken) return
    results.value = (res.data ?? []).filter(r =>
        r.tipo !== 'ATTACCO' &&
        r.tipo !== 'EFFETTO' &&
        r.id !== props.excludeId &&
        !props.modelValue.some(c => c.id === r.id) &&
        (!props.excludeTipo || r.tipo !== props.excludeTipo)
    )
  } catch (e) {
    console.error('Errore ricerca item:', e)
    if (token === searchToken) results.value = []
  } finally {
    if (token === searchToken) searching.value = false
  }
}

let debounceTimer: any = null
function onQueryInput() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(doSearch, 250)
}

const FRUTTO_SCELTE = [
  {value: 'ALL',    label: 'Tutti'},
  {value: 'MOD',    label: 'Modificatori'},
  {value: 'FORMA_1', label: 'Forma 1'},
  {value: 'FORMA_2', label: 'Forma 2'},
  {value: 'FORMA_3', label: 'Forma 3'},
  {value: 'FORMA_4', label: 'Forma 4'},
]

// niente selezionato o ALL = prendi tutto
function isEffectivelyAll(c: ChildRef): boolean {
  const set = getSceltaSet(c)
  return set.size === 0 || set.has('ALL')
}

function add(item: Item) {
  emit('update:modelValue', [...props.modelValue, {id: item.id, nome: item.nome, tipo: item.tipo, qty: null, formulaQty: null, scelta: null, condizione: null}])
  results.value = results.value.filter(r => r.id !== item.id)
}

function setQty(idx: number, val: string) {
  const n = parseInt(val)
  const updated = props.modelValue.map((c, i) =>
      i === idx ? {...c, qty: Number.isFinite(n) && n > 0 ? n : null} : c
  )
  emit('update:modelValue', updated)
}

function setFormulaQty(idx: number, val: string) {
  const trimmed = val.trim() || null
  emit('update:modelValue', props.modelValue.map((c, i) => i === idx ? {...c, formulaQty: trimmed} : c))
}

function setNascosto(idx: number, val: boolean) {
  emit('update:modelValue', props.modelValue.map((c, i) => i === idx ? {...c, nascosto: val} : c))
}

function getSceltaSet(c: ChildRef): Set<string> {
  return new Set((c.scelta ?? '').split(',').filter(Boolean))
}

function toggleScelta(idx: number, val: string) {
  const c = props.modelValue[idx]
  const set = getSceltaSet(c)
  if (val === 'ALL') {
    // ALL toggling: se era già selezionato lo rimuove (= niente = tutti), altrimenti lo seleziona esclusivo
    if (set.has('ALL')) set.clear()
    else { set.clear(); set.add('ALL') }
  } else {
    // Selezionare una voce specifica rimuove ALL
    set.delete('ALL')
    if (set.has(val)) set.delete(val)
    else set.add(val)
  }
  const scelta = set.size ? [...set].join(',') : null
  emit('update:modelValue', props.modelValue.map((item, i) => i === idx ? {...item, scelta} : item))
}

function remove(idx: number) {
  emit('update:modelValue', props.modelValue.filter((_, i) => i !== idx))
}
</script>

<template>
  <div class="children-editor">
    <div v-if="!modelValue.length" class="empty">Nessun item collegato.</div>

    <div v-for="(c, i) in modelValue" :key="c.id" class="child-row" :class="{'child-row--frutto': c.tipo === 'FRUTTO', 'child-row--disabled': c.nascosto}">
      <span class="nome">{{ c.nome }}</span>
      <span class="pill">{{ c.tipo }}</span>
      <template v-if="c.tipo === 'FRUTTO'">
        <div class="scelta-checks">
          <label v-for="s in FRUTTO_SCELTE" :key="s.value" class="scelta-check">
            <input
                type="checkbox"
                :checked="s.value === 'ALL' ? isEffectivelyAll(c) : getSceltaSet(c).has(s.value)"
                :disabled="disabled"
                @change="toggleScelta(i, s.value)"
            />
            {{ s.label }}
          </label>
        </div>
      </template>
      <input
          v-else
          class="qty-input"
          type="number"
          min="1"
          step="1"
          :value="c.qty ?? ''"
          :disabled="disabled"
          placeholder="—"
          title="Utilizzi concessi"
          @change="setQty(i, ($event.target as HTMLInputElement).value)"
      />
      <button type="button" class="btn-edit" @click="editChild(c)" title="Modifica">✎</button>
      <button type="button" class="btn-del" :disabled="disabled" @click="remove(i)" title="Scollega">✕</button>
      <!-- Formula utilizzi: occupa tutta la larghezza sotto la riga principale -->
      <div class="formula-qty-row">
        <span class="formula-qty-label">Formula utilizzi</span>
        <input
            class="formula-qty-input"
            type="text"
            :value="c.formulaQty ?? ''"
            :disabled="disabled"
            placeholder="es. 2*@LVL (opzionale, sovrascrive qty)"
            title="Formula per il calcolo degli utilizzi"
            @input="setFormulaQty(i, ($event.target as HTMLInputElement).value)"
        />
      </div>
      <!-- Nascondi (es. FORMA): non visibile da fuori, implica disabilitare -->
      <label v-if="c.tipo === 'FORMA'" class="disable-row">
        <input
            type="checkbox"
            :checked="!!c.nascosto"
            :disabled="disabled"
            @change="setNascosto(i, ($event.target as HTMLInputElement).checked)"
        />
        <span>Nascondi (non visibile da fuori)</span>
      </label>
    </div>

    <div class="search-box">
      <div class="search-row">
        <input
            type="text"
            v-model="query"
            :disabled="disabled"
            placeholder="Cerca un item da collegare (min 2 caratteri)…"
            @input="onQueryInput"
        />
        <button
            v-if="query.trim().length && !hideCreate"
            type="button"
            class="btn-add-named"
            :disabled="disabled"
            :title="`Crea e collega «${query.trim()}»`"
            @click="emit('create-new', onlyTipo, query.trim())"
        >+</button>
      </div>
      <div v-if="searching" class="hint">Ricerca…</div>
      <ul v-else-if="results.length" class="results">
        <li v-for="r in results" :key="r.id">
          <button type="button" class="result" :disabled="disabled" @click="add(r)">
            <span class="nome">{{ r.nome }}</span>
            <span class="pill">{{ r.tipo }}</span>
            <span class="plus">+</span>
          </button>
        </li>
      </ul>
      <div v-else-if="query.trim().length >= 2" class="hint">Nessun risultato.</div>
    </div>

    <button v-if="!hideCreate" type="button" class="btn-create" :disabled="disabled" @click="emit('create-new', onlyTipo)">
      + Crea nuovo
    </button>
  </div>
</template>

<style scoped>
input, select, textarea { min-width: 0; }
.children-editor { display: grid; gap: .4rem; }
.empty { font-size: .85rem; opacity: .6; }

.child-row {
  display: grid; grid-template-columns: 1fr auto auto auto auto; gap: .4rem; align-items: center;
  padding: .35rem .5rem; border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0);
  color: var(--text-strong);
}
.qty-input {
  width: 3.5rem; padding: .25rem .35rem; border: 1px solid var(--hairline); border-radius: .4rem;
  text-align: center; font-size: .8rem; background: var(--surface-0); color: var(--text-strong);
}
.child-row--frutto {
  grid-template-columns: 1fr auto auto auto;
  flex-wrap: wrap;
}
.scelta-checks {
  grid-column: 1 / -1;
  display: flex; flex-wrap: wrap; gap: .3rem .7rem;
  padding: .2rem 0 .1rem;
}
.scelta-check {
  display: flex; align-items: center; gap: .25rem;
  font-size: .8rem; cursor: pointer; user-select: none;
}
.scelta-check input[type="checkbox"] { width: auto; margin: 0; }
.formula-qty-row {
  grid-column: 1 / -1;
  display: flex; align-items: center; gap: .4rem;
  padding: .15rem 0 .05rem;
}
.formula-qty-label {
  font-size: .75rem; color: var(--text-muted); white-space: nowrap; flex-shrink: 0;
}
.formula-qty-input {
  flex: 1; padding: .25rem .4rem; border: 1px solid var(--hairline); border-radius: .35rem;
  font-size: .78rem; background: var(--btn-bg); color: var(--text-strong);
}
.formula-qty-input:focus { border-color: var(--info-border); outline: none; background: var(--surface-0); }
.disable-row {
  grid-column: 1 / -1;
  display: flex; align-items: center; gap: .4rem;
  font-size: .78rem; color: var(--text-muted); cursor: pointer; user-select: none;
}
.disable-row input[type="checkbox"] { width: auto; margin: 0; }
.child-row--disabled { opacity: .6; background: var(--surface-hover); }
.nome { white-space: normal; word-break: break-word; }
.pill {
  font-size: .75rem; padding: .1rem .45rem; border-radius: .5rem;
  background: var(--info-bg); color: var(--info-text);
}

.search-box { display: grid; gap: .35rem; margin-top: .25rem; }
.search-row { display: flex; gap: .4rem; align-items: stretch; }
.search-row input { flex: 1; }
input {
  width: 100%; padding: .45rem .55rem; border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0);
  color: var(--text-strong);
}
.btn-add-named {
  flex: 0 0 auto;
  border: 1px solid var(--info-border); background: var(--info-bg); color: var(--info-text);
  border-radius: .5rem; padding: 0 .8rem; font-weight: 700; font-size: 1.1rem; cursor: pointer;
}
.btn-add-named:hover { background: var(--info-border); }
.hint { font-size: .8rem; opacity: .6; }

.results {
  list-style: none; margin: 0; padding: 0;
  border: 1px solid var(--hairline); border-radius: .5rem; overflow: hidden;
  max-height: 14rem; overflow-y: auto;
}
.results li + li { border-top: 1px solid var(--hairline); }
.result {
  width: 100%; display: grid; grid-template-columns: 1fr auto auto; gap: .4rem; align-items: center;
  padding: .4rem .5rem; background: var(--surface-0); color: var(--text-strong); border: 0; cursor: pointer; text-align: left;
}
.result:hover { background: var(--surface-hover); }
.plus { color: var(--info-text); font-weight: 700; }

.btn-edit {
  border: 1px solid var(--info-border); background: var(--info-bg); color: var(--info-text);
  border-radius: .5rem; padding: .3rem .55rem; cursor: pointer;
}
.btn-edit:hover { background: var(--info-border); }

.btn-del {
  border: 1px solid var(--danger-border); background: var(--danger-bg); color: var(--danger-text);
  border-radius: .5rem; padding: .3rem .55rem; cursor: pointer;
}

.btn-create {
  justify-self: start; margin-top: .15rem;
  border: 1px dashed var(--info-border); background: var(--info-bg); color: var(--info-text);
  border-radius: .5rem; padding: .4rem .7rem; font-weight: 600; cursor: pointer;
}
.btn-create:hover { background: var(--info-border); }

button:disabled { opacity: .6; cursor: default; }
</style>
