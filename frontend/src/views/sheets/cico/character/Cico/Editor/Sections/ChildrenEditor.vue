<script setup lang="ts">
import {ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ChildRef} from '../../../../../../../models/dto/UpdateItemRequest'
import {searchItems} from '../../../../../../../service/PersonaggioService'
import {Item} from '../../../../../../../models/dto/Item'

const props = defineProps<{
  modelValue: ChildRef[]
  disabled?: boolean
  excludeId?: number   // id dell'item in editing: non collegabile a se stesso
  onlyTipo?: string    // mostra/cerca solo questo tipo
  excludeTipo?: string // mostra/cerca qualsiasi tipo tranne questo
}>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: ChildRef[]): void
  (e: 'create-new', tipo: string | undefined, nome?: string): void
}>()

const router = useRouter()
const route = useRoute()

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
    const res = await searchItems(q)
    if (token !== searchToken) return
    results.value = (res.data ?? []).filter(r =>
        r.tipo !== 'ATTACCO' &&
        r.id !== props.excludeId &&
        !props.modelValue.some(c => c.id === r.id) &&
        (!props.onlyTipo || r.tipo === props.onlyTipo) &&
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

function add(item: Item) {
  emit('update:modelValue', [...props.modelValue, {id: item.id, nome: item.nome, tipo: item.tipo}])
  results.value = results.value.filter(r => r.id !== item.id)
}

function remove(idx: number) {
  emit('update:modelValue', props.modelValue.filter((_, i) => i !== idx))
}
</script>

<template>
  <div class="children-editor">
    <div v-if="!modelValue.length" class="empty">Nessun item collegato.</div>

    <div v-for="(c, i) in modelValue" :key="c.id" class="child-row">
      <span class="nome">{{ c.nome }}</span>
      <span class="pill">{{ c.tipo }}</span>
      <button type="button" class="btn-edit" @click="editChild(c)" title="Modifica">✎</button>
      <button type="button" class="btn-del" :disabled="disabled" @click="remove(i)" title="Scollega">✕</button>
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
            v-if="query.trim().length"
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

    <button type="button" class="btn-create" :disabled="disabled" @click="emit('create-new', onlyTipo)">
      + Crea nuovo
    </button>
  </div>
</template>

<style scoped>
input, select, textarea { min-width: 0; }
.children-editor { display: grid; gap: .4rem; }
.empty { font-size: .85rem; opacity: .6; }

.child-row {
  display: grid; grid-template-columns: 1fr auto auto auto; gap: .4rem; align-items: center;
  padding: .35rem .5rem; border: 1px solid #e5e7eb; border-radius: .5rem; background: #fff;
}
.nome { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.pill {
  font-size: .75rem; padding: .1rem .45rem; border-radius: .5rem;
  background: #eef2ff; color: #3730a3;
}

.search-box { display: grid; gap: .35rem; margin-top: .25rem; }
.search-row { display: flex; gap: .4rem; align-items: stretch; }
.search-row input { flex: 1; }
input {
  width: 100%; padding: .45rem .55rem; border: 1px solid #d0d5dd; border-radius: .5rem; background: #fff;
}
.btn-add-named {
  flex: 0 0 auto;
  border: 1px solid #93c5fd; background: #eff6ff; color: #1d4ed8;
  border-radius: .5rem; padding: 0 .8rem; font-weight: 700; font-size: 1.1rem; cursor: pointer;
}
.btn-add-named:hover { background: #dbeafe; }
.hint { font-size: .8rem; opacity: .6; }

.results {
  list-style: none; margin: 0; padding: 0;
  border: 1px solid #e5e7eb; border-radius: .5rem; overflow: hidden;
  max-height: 14rem; overflow-y: auto;
}
.results li + li { border-top: 1px solid #f3f4f6; }
.result {
  width: 100%; display: grid; grid-template-columns: 1fr auto auto; gap: .4rem; align-items: center;
  padding: .4rem .5rem; background: #fff; border: 0; cursor: pointer; text-align: left;
}
.result:hover { background: #f9fafb; }
.plus { color: #2563eb; font-weight: 700; }

.btn-edit {
  border: 1px solid #bfdbfe; background: #eff6ff; color: #1d4ed8;
  border-radius: .5rem; padding: .3rem .55rem; cursor: pointer;
}
.btn-edit:hover { background: #dbeafe; }

.btn-del {
  border: 1px solid #fecaca; background: #fef2f2; color: #991b1b;
  border-radius: .5rem; padding: .3rem .55rem; cursor: pointer;
}

.btn-create {
  justify-self: start; margin-top: .15rem;
  border: 1px dashed #93c5fd; background: #eff6ff; color: #1d4ed8;
  border-radius: .5rem; padding: .4rem .7rem; font-weight: 600; cursor: pointer;
}
.btn-create:hover { background: #dbeafe; }

button:disabled { opacity: .6; cursor: default; }
</style>
