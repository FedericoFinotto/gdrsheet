<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ItemDB, TIPO_ITEM, TipoItem} from '../../../../../../models/entity/ItemDB'
import {CREATABLE_TYPES, editorForType, TIPO_ITEM_LABELS} from './editorRegistry'

const route = useRoute()
const router = useRouter()

function parseTipo(v: unknown): TipoItem | null {
  const s = String(v ?? '').toUpperCase()
  return (Object.values(TIPO_ITEM) as string[]).includes(s) ? (s as TipoItem) : null
}

const tipo = ref<TipoItem | null>(parseTipo(route.params.tipo))
watch(() => route.params.tipo, v => { tipo.value = parseTipo(v) })

// item vuoto per l'editor in modalità creazione
const blankItem = computed<ItemDB | null>(() => {
  if (!tipo.value) return null
  return {
    id: 0,
    nome: '',
    tipo: tipo.value,
    descrizione: '',
    child: [],
    modificatori: [],
    labels: [],
    avanzamento: [],
  }
})

const EditorComp = computed(() => editorForType(tipo.value))

function onTipoChange(e: Event) {
  const v = (e.target as HTMLSelectElement).value
  router.replace(v ? `/itemcreate/${v}` : '/itemcreate')
}

function goBack() {
  router.back()
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
        <select :value="tipo ?? ''" @change="onTipoChange">
          <option value="" disabled>Seleziona un tipo…</option>
          <option v-for="t in CREATABLE_TYPES" :key="t" :value="t">{{ TIPO_ITEM_LABELS[t] }}</option>
        </select>
      </label>

      <component
          v-if="EditorComp && blankItem"
          :is="EditorComp"
          :key="tipo"
          :item="blankItem"
          mode="create"
          @cancel="goBack"
          @saved="goBack"
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
