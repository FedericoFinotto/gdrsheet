<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import TabExpandable from '../../../../../../../components/TabExpandable.vue'
import {ItemDB, TIPO_ITEM} from '../../../../../../../models/entity/ItemDB'
import {searchItems} from '../../../../../../../service/PersonaggioService'
import Icona from '../../../../../../../components/Icona/Icona.vue'

const props = defineProps<{
  disabled: boolean
  loading?: boolean
  items: ItemDB[]        // item extra correnti
}>()

const emit = defineEmits<{
  (e: 'update:items', v: ItemDB[]): void
}>()

const TIPI_DISPONIBILI = [
  {value: '', label: 'Tutti i tipi'},
  {value: TIPO_ITEM.ABILITA,      label: 'Abilità'},
  {value: TIPO_ITEM.TALENTO,      label: 'Talento'},
  {value: TIPO_ITEM.COMPETENZA,   label: 'Competenza'},
  {value: TIPO_ITEM.LINGUA,       label: 'Lingua'},
  {value: TIPO_ITEM.INCANTESIMO,  label: 'Incantesimo'},
  {value: TIPO_ITEM.FRUTTO,       label: 'Frutto'},
  {value: TIPO_ITEM.FORMA,        label: 'Forma'},
  {value: TIPO_ITEM.OGGETTO,      label: 'Oggetto'},
  {value: TIPO_ITEM.ARMA,         label: 'Arma'},
  {value: TIPO_ITEM.EQUIPAGGIAMENTO, label: 'Equipaggiamento'},
  {value: TIPO_ITEM.MALEDIZIONE,  label: 'Maledizione'},
  {value: TIPO_ITEM.ALTRO,        label: 'Altro'},
]

const TIPO_LABEL: Record<string, string> = Object.fromEntries(
    TIPI_DISPONIBILI.filter(t => t.value).map(t => [t.value, t.label])
)

const query = ref('')
const tipoFiltro = ref('')
const risultati = ref<ItemDB[]>([])
const searching = ref(false)
let searchTimer: any = null

watch([query, tipoFiltro], () => {
  clearTimeout(searchTimer)
  if (!query.value.trim() && !tipoFiltro.value) { risultati.value = []; return }
  searching.value = true
  searchTimer = setTimeout(async () => {
    try {
      const res = await searchItems(query.value.trim(), tipoFiltro.value || undefined)
      risultati.value = (res.data as any[]).filter(
          r => !props.items.find(i => i.id === r.id)
      )
    } catch { risultati.value = [] }
    finally { searching.value = false }
  }, 300)
})

function aggiungi(item: ItemDB) {
  if (props.items.find(i => i.id === item.id)) return
  emit('update:items', [...props.items, item])
  risultati.value = risultati.value.filter(r => r.id !== item.id)
}

function rimuovi(item: ItemDB) {
  emit('update:items', props.items.filter(i => i.id !== item.id))
}

const summaryText = computed(() =>
    props.items.length ? `${props.items.length} item aggiuntivi` : 'Nessuno'
)
</script>

<template>
  <TabExpandable title="Item aggiuntivi" :loading="loading">
    <template #summary>{{ summaryText }}</template>
    <template #content>
      <div class="extra-root">

        <!-- Item già aggiunti -->
        <div v-if="items.length" class="added-list">
          <div v-for="item in items" :key="item.id" class="added-row">
            <span class="added-tipo">{{ TIPO_LABEL[item.tipo] ?? item.tipo }}</span>
            <span class="added-nome">{{ item.nome }}</span>
            <button type="button" class="remove-btn" :disabled="disabled" @click="rimuovi(item)"
                title="Rimuovi">
              <Icona name="XMARK"/>
            </button>
          </div>
        </div>
        <div v-else class="muted">Nessun item aggiunto.</div>

        <div class="separator"/>

        <!-- Ricerca -->
        <div class="search-row">
          <select v-model="tipoFiltro" :disabled="disabled" class="tipo-select">
            <option v-for="t in TIPI_DISPONIBILI" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>
          <input
              v-model="query"
              type="text"
              placeholder="Cerca per nome…"
              :disabled="disabled"
              class="search-input"
          />
        </div>

        <!-- Risultati ricerca -->
        <div v-if="searching" class="muted">Ricerca…</div>
        <div v-else-if="risultati.length" class="results-list">
          <div v-for="r in risultati" :key="r.id" class="result-row" @click="!disabled && aggiungi(r)">
            <span class="added-tipo">{{ TIPO_LABEL[r.tipo] ?? r.tipo }}</span>
            <span class="result-nome">{{ r.nome }}</span>
            <Icona name="ADD" class="add-ico"/>
          </div>
        </div>
        <div v-else-if="query || tipoFiltro" class="muted">Nessun risultato.</div>

      </div>
    </template>
  </TabExpandable>
</template>

<style scoped>
.extra-root {
  display: flex;
  flex-direction: column;
  gap: .5rem;
}

.added-list {
  display: flex;
  flex-direction: column;
  gap: .25rem;
}

.added-row, .result-row {
  display: flex;
  align-items: center;
  gap: .45rem;
  padding: .3rem .5rem;
  border-radius: .4rem;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
}

.result-row {
  cursor: pointer;
  background: #fff;
}
.result-row:hover { background: #f0fdf4; border-color: #bbf7d0; }

.added-tipo {
  font-size: .65rem;
  font-weight: 700;
  padding: .1rem .35rem;
  border-radius: .3rem;
  background: #e0e7ff;
  color: #3730a3;
  white-space: nowrap;
  flex-shrink: 0;
}

.added-nome, .result-nome {
  flex: 1;
  font-size: .9rem;
}

.remove-btn {
  border: none;
  background: transparent;
  cursor: pointer;
  color: #ef4444;
  padding: .1rem .2rem;
  border-radius: .3rem;
  display: flex;
  align-items: center;
  font-size: .8rem;
}
.remove-btn:disabled { opacity: .4; cursor: default; }
.remove-btn:hover:not(:disabled) { background: #fee2e2; }

.add-ico {
  font-size: .8rem;
  color: #16a34a;
  margin-left: auto;
}

.separator {
  border-top: 1px solid #e5e7eb;
  margin: .1rem 0;
}

.search-row {
  display: flex;
  gap: .4rem;
}

.tipo-select {
  flex: 0 0 auto;
  max-width: 9rem;
  padding: .4rem .4rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  font-size: .85rem;
  background: #fff;
}

.search-input {
  flex: 1;
  padding: .4rem .6rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  font-size: .9rem;
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: .2rem;
  max-height: 12rem;
  overflow-y: auto;
}

.muted {
  font-size: .85rem;
  color: #9ca3af;
}
</style>
