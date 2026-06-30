<script setup lang="ts">
import {ref, watch} from 'vue'
import {getCompendio, linkItem} from '../../../../../../service/PersonaggioService'
import {useCharacterStore} from '../../../../../../stores/personaggio'
import {Item} from '../../../../../../models/dto/Item'
import {TIPO_ITEM} from '../../../../../../models/entity/ItemDB'
import usePopup from '../../../../../../function/usePopup'

const props = defineProps<{ idPersonaggio: number }>()

const {closePopup} = usePopup()
const store = useCharacterStore()

const query   = ref('')
const tipo    = ref<string>('')
const items   = ref<Item[]>([])
const total   = ref(0)
const page    = ref(0)
const loading = ref(false)
const linking = ref(false)

// Conferma
const pending = ref<Item | null>(null)

const PAGE_SIZE = 15

const TIPI = [
  {value: '', label: 'Tutti'},
  {value: TIPO_ITEM.OGGETTO,          label: 'Oggetto'},
  {value: TIPO_ITEM.ARMA,             label: 'Arma'},
  {value: TIPO_ITEM.CONSUMABILE,      label: 'Consumabile'},
  {value: TIPO_ITEM.EQUIPAGGIAMENTO,  label: 'Equipaggiamento'},
  {value: TIPO_ITEM.MUNIZIONI,        label: 'Munizioni'},
  {value: TIPO_ITEM.CONTENITORE,      label: 'Contenitore'},
  {value: TIPO_ITEM.FRUTTO,           label: 'Frutto'},
  {value: TIPO_ITEM.TALENTO,          label: 'Talento'},
]

let debounceTimer: ReturnType<typeof setTimeout> | null = null

function cerca(reset = true) {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(async () => {
    if (reset) { page.value = 0; items.value = [] }
    loading.value = true
    try {
      const res = await getCompendio({
        nome: query.value || undefined,
        tipo: tipo.value || undefined,
        page: page.value,
        size: PAGE_SIZE,
      })
      const data = res.data
      if (reset) { items.value = data.content }
      else        { items.value.push(...data.content) }
      total.value = data.totalElements
    } catch (_) {
      // ignora
    } finally {
      loading.value = false
    }
  }, 250)
}

watch([query, tipo], () => cerca(true), {immediate: true})

async function loadMore() {
  page.value++
  await cerca(false)
}

function seleziona(item: Item) {
  pending.value = item
}

function annulla() {
  pending.value = null
}

async function conferma() {
  if (!pending.value || linking.value) return
  linking.value = true
  try {
    await linkItem(pending.value.id, props.idPersonaggio)
    await store.fetchCharacter(props.idPersonaggio, true)
    closePopup()
  } catch (_) {
    linking.value = false
  }
}

const hasMore = () => items.value.length < total.value
</script>

<template>
  <div class="compendio-popup">
    <h3 class="popup-title">Aggiungi dal Compendio</h3>

    <!-- Filtri -->
    <div class="filters">
      <input
        v-model="query"
        class="search-input"
        placeholder="Cerca per nome…"
        type="search"
        autofocus
      />
      <select v-model="tipo" class="tipo-select">
        <option v-for="t in TIPI" :key="t.value" :value="t.value">{{ t.label }}</option>
      </select>
    </div>

    <!-- Lista risultati -->
    <div class="results-wrap">
      <div v-if="loading && items.length === 0" class="empty-state">Caricamento…</div>
      <div v-else-if="items.length === 0" class="empty-state">Nessun risultato</div>

      <div
        v-for="item in items"
        :key="item.id"
        class="result-row"
        :class="{'is-selected': pending?.id === item.id}"
        @click="seleziona(item)"
      >
        <span class="item-nome">{{ item.nome }}</span>
        <span class="item-tipo">{{ item.tipo }}</span>
        <span class="link-plus">+</span>
      </div>

      <button
        v-if="hasMore()"
        class="btn-load-more"
        :disabled="loading"
        @click="loadMore"
      >
        {{ loading ? 'Caricamento…' : `Carica altri (${total - items.length} rimasti)` }}
      </button>
    </div>

    <!-- Barra di conferma -->
    <transition name="slide-up">
      <div v-if="pending" class="confirm-bar">
        <span class="confirm-label">Aggiungere <strong>{{ pending.nome }}</strong>?</span>
        <div class="confirm-actions">
          <button class="btn-annulla" :disabled="linking" @click="annulla">Annulla</button>
          <button class="btn-conferma" :disabled="linking" @click="conferma">
            {{ linking ? '…' : 'Aggiungi' }}
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.compendio-popup {
  display: flex;
  flex-direction: column;
  gap: .6rem;
  /* niente width fissa: il popup-content wrapper gestisce la dimensione */
  width: 100%;
  min-width: 260px;
  max-height: 75dvh;       /* dvh tiene conto della tastiera mobile */
  box-sizing: border-box;
}
.popup-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
}
.filters {
  display: flex;
  gap: .4rem;
}
.search-input {
  flex: 1;
  min-width: 0;
  padding: .4rem .6rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: .4rem;
  font-size: .875rem;
  outline: none;
}
.search-input:focus { border-color: #2563eb; }
.tipo-select {
  flex-shrink: 0;
  padding: .4rem .3rem;
  border: 1px solid var(--color-border, #d1d5db);
  border-radius: .4rem;
  font-size: .8rem;
  background: #fff;
  cursor: pointer;
  max-width: 110px;
}
.results-wrap {
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-height: 80px;
  flex: 1;
}
.empty-state {
  padding: 1rem;
  text-align: center;
  color: var(--color-text-secondary, #6b7280);
  font-size: .875rem;
}
.result-row {
  display: flex;
  align-items: center;
  gap: .5rem;
  padding: .45rem .6rem;
  border-radius: .35rem;
  cursor: pointer;
  background: #fff;
  transition: background .1s;
  border: 1px solid transparent;
}
.result-row:hover     { background: #eff6ff; }
.result-row.is-selected {
  background: #eff6ff;
  border-color: #2563eb;
}
.item-nome {
  flex: 1;
  font-size: .875rem;
  font-weight: 500;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-tipo {
  font-size: .72rem;
  color: var(--color-text-secondary, #6b7280);
  background: var(--color-surface-2, #f3f4f6);
  padding: .1rem .35rem;
  border-radius: .3rem;
  flex-shrink: 0;
}
.link-plus {
  font-size: 1.1rem;
  font-weight: 800;
  color: #2563eb;
  flex-shrink: 0;
  width: 1.2rem;
  text-align: center;
}
.btn-load-more {
  margin-top: .3rem;
  padding: .4rem;
  border: 1px dashed #cbd5e1;
  border-radius: .4rem;
  background: #f8fafc;
  color: #475569;
  font-size: .8rem;
  cursor: pointer;
}
.btn-load-more:hover:not(:disabled) { background: #f1f5f9; }
.btn-load-more:disabled { opacity: .5; cursor: default; }

/* Barra conferma */
.confirm-bar {
  display: flex;
  align-items: center;
  gap: .5rem;
  padding: .55rem .6rem;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: .4rem;
  flex-shrink: 0;
}
.confirm-label {
  flex: 1;
  font-size: .82rem;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.confirm-actions {
  display: flex;
  gap: .35rem;
  flex-shrink: 0;
}
.btn-annulla {
  padding: .3rem .65rem;
  border: 1px solid #d1d5db;
  border-radius: .35rem;
  background: #fff;
  color: #374151;
  font-size: .8rem;
  cursor: pointer;
}
.btn-annulla:hover:not(:disabled) { background: #f9fafb; }
.btn-conferma {
  padding: .3rem .75rem;
  border: none;
  border-radius: .35rem;
  background: #2563eb;
  color: #fff;
  font-size: .8rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-conferma:hover:not(:disabled) { background: #1d4ed8; }
.btn-conferma:disabled, .btn-annulla:disabled { opacity: .5; cursor: default; }

/* Animazione barra conferma */
.slide-up-enter-active, .slide-up-leave-active { transition: all .18s ease; }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translateY(6px); }
</style>
