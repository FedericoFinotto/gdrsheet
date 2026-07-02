<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ItemDB, TIPO_ITEM, TipoItem} from '../../../../../../models/entity/ItemDB'
import {CREATABLE_TYPES, editorForType, TIPO_ITEM_LABELS} from './editorRegistry'
import {useCharacterStore} from '../../../../../../stores/personaggio'
import {useAuthStore} from '../../../../../../stores/auth'
import SearchSelect from '../../../../../../components/SearchSelect.vue'

const route = useRoute()
const router = useRouter()
const characterStore = useCharacterStore()
const auth = useAuthStore()

const canCreateNotizia = computed(() => {
  const r = auth.effectiveRuolo.toUpperCase()
  return r === 'MASTER' || r === 'ADMIN' || r === 'SUPERUSER'
})

const creatableTypes = computed(() =>
    CREATABLE_TYPES.filter(t => t !== TIPO_ITEM.NOTIZIA || canCreateNotizia.value)
)

// se presente, il nuovo item viene agganciato al FromCompendio del personaggio
const idPersonaggio = computed<number | undefined>(() => {
  const n = Number(route.query.personaggio)
  return Number.isFinite(n) && n > 0 ? n : undefined
})

function parseTipo(v: unknown): TipoItem | null {
  const s = String(v ?? '').toUpperCase()
  return (Object.values(TIPO_ITEM) as string[]).includes(s) ? (s as TipoItem) : null
}

const tipo = ref<TipoItem | null>(parseTipo(route.params.tipo))
watch(() => route.params.tipo, v => { tipo.value = parseTipo(v) })

// nome eventualmente pre-compilato (creazione "al volo" dalla ricerca di un item da collegare)
const nomeIniziale = computed<string>(() => String(route.query.nome ?? ''))

// item vuoto per l'editor in modalità creazione
const blankItem = computed<ItemDB | null>(() => {
  if (!tipo.value) return null
  return {
    id: 0,
    nome: nomeIniziale.value,
    tipo: tipo.value,
    descrizione: '',
    child: [],
    modificatori: [],
    labels: [],
    avanzamento: [],
  }
})

const EditorComp = computed(() => editorForType(tipo.value))

function onTipoChange(v: string) {
  const params = new URLSearchParams()
  if (route.query.link) params.set('link', '1')   // mantieni il flag "crea e collega"
  if (route.query.compendio) params.set('compendio', '1') // mantieni il flag "mostra nel compendio"
  if (route.query.nome) params.set('nome', String(route.query.nome)) // mantieni il nome pre-compilato
  if (idPersonaggio.value) params.set('personaggio', String(idPersonaggio.value))
  const q = params.toString() ? `?${params.toString()}` : ''
  router.replace(v ? `/itemcreate/${v}${q}` : `/itemcreate${q}`)
}

function goBack() {
  router.back()
}

async function refreshPersonaggio() {
  if (!idPersonaggio.value) return
  try {
    await characterStore.fetchCharacter(idPersonaggio.value, true)
  } catch (e) {
    console.error('Errore refresh personaggio:', e)
  }
}

async function onSaved() {
  // ricarica la scheda così il nuovo item compare subito
  await refreshPersonaggio()
  goBack()
}

// "Salva e continua": l'editor si è già azzerato da solo, qui solo refresh
function onSavedStay() {
  refreshPersonaggio()
}
</script>

<template>
  <section class="item-create">
    <header class="head">
      <h1>Nuovo Item</h1>
      <div class="meta">
        <span v-if="tipo" class="pill">{{ TIPO_ITEM_LABELS[tipo] }}</span>
      </div>
    </header>

    <div class="editor-scroll">
      <label class="field">
        <span class="lbl">Tipo</span>
        <SearchSelect :model-value="tipo ?? ''" placeholder="Seleziona un tipo…"
                      :options="creatableTypes.map(t => ({value: t, label: TIPO_ITEM_LABELS[t]}))"
                      @update:model-value="onTipoChange($event as string)"/>
      </label>

      <component
          v-if="EditorComp && blankItem"
          :is="EditorComp"
          :key="tipo"
          :item="blankItem"
          mode="create"
          :id-personaggio="idPersonaggio"
          @cancel="goBack"
          @saved="onSaved"
          @saved-stay="onSavedStay"
      />

      <div v-else class="state empty">
        Seleziona il tipo di item da creare.
      </div>
    </div>
  </section>
</template>

<style scoped>
.item-create {
  height: 100dvh;
  max-height: 100dvh;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.head {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: .75rem;
  padding: .75rem 1rem;
  background: inherit;
  border-bottom: 1px solid #e5e7eb;
}

.head h1 {
  margin: 0;
  font-size: 1.1rem;
}

.meta { display: inline-flex; gap: .5rem; align-items: center; }

.pill {
  font-size: .8rem;
  padding: .1rem .5rem;
  border-radius: .5rem;
  background: #eef2ff;
  color: #3730a3;
}

.editor-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  padding: .75rem 1rem calc(2rem + env(safe-area-inset-bottom, 0px));
  display: grid;
  gap: .75rem;
  align-content: start;
}

.field { display: grid; gap: .35rem; }
.lbl { font-size: .8rem; font-weight: 600; opacity: .85; }
select {
  width: 100%; padding: .5rem .6rem; border: 1px solid #d0d5dd; border-radius: .5rem; background: #fff;
}

.state {
  padding: .75rem;
  border: 1px dashed #e5e7eb;
  border-radius: .5rem;
  margin: 0;
}
</style>
