<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import type {AxiosResponse} from 'axios'
import {deleteItem, getItem, unlinkItem} from '../../../../../../service/PersonaggioService'
import {useCharacterStore} from '../../../../../../stores/personaggio'
import usePopup from '../../../../../../function/usePopup'

// Editors: registro tipo -> componente
import {editorForType} from './editorRegistry'
import {ItemDB} from "../../../../../../models/entity/ItemDB";

const route = useRoute();
const idItem = Number(route.params.id);
if (isNaN(idItem)) throw new Error('Parametro id non valido');

const router = useRouter()
const characterStore = useCharacterStore()
const {closePopup} = usePopup()
const loading = ref(false)
const errorMsg = ref<string | null>(null)
const item = ref<ItemDB | null>(null)

const EditorComp = computed(() => editorForType(item.value?.tipo))

// contesto personaggio (per scollegamento/refresh dopo eliminazione)
const idPersonaggio = computed<number | undefined>(() => {
  const n = Number(route.query.personaggio)
  return Number.isFinite(n) && n > 0 ? n : undefined
})

const deleting = ref(false)
const unlinking = ref(false)

// scollegabile: c'è il contesto personaggio e non è un item intestato (es. livelli)
const canUnlink = computed(() =>
    !!idPersonaggio.value && !!item.value && item.value.tipo !== 'LIVELLO')

async function onUnlink() {
  if (!item.value || !idPersonaggio.value || unlinking.value) return
  const ok = window.confirm("Sicuro? L'oggetto rimarrà nel compendio ma non farà più parte del tuo equipaggiamento")
  if (!ok) return
  unlinking.value = true
  try {
    await unlinkItem(item.value.id, idPersonaggio.value)
    closePopup()
    await characterStore.fetchCharacter(idPersonaggio.value, true)
    router.back()
  } catch (e: any) {
    errorMsg.value = e?.message ?? 'Errore nello scollegamento'
    console.error('Errore scollegamento item:', e)
  } finally {
    unlinking.value = false
  }
}

async function onDelete() {
  if (!item.value || deleting.value) return
  const ok = window.confirm(`Sei sicuro di voler eliminare "${item.value.nome}"?`)
  if (!ok) return
  deleting.value = true
  try {
    await deleteItem(item.value.id, idPersonaggio.value)
    // chiudi l'eventuale popup di dettaglio rimasto aperto sotto l'editor
    closePopup()
    if (idPersonaggio.value) {
      await characterStore.fetchCharacter(idPersonaggio.value, true)
    }
    router.back()
  } catch (e: any) {
    errorMsg.value = e?.message ?? 'Errore nell\'eliminazione'
    console.error('Errore eliminazione item:', e)
  } finally {
    deleting.value = false
  }
}

function parseId(v: unknown): number | null {
  const n = Number(v);
  return Number.isFinite(n) ? n : null
}

async function load(id: number) {
  loading.value = true;
  errorMsg.value = null;
  item.value = null
  try {
    const res: AxiosResponse<ItemDB> = await getItem(id)
    item.value = res.data
  } catch (e: any) {
    errorMsg.value = e?.message ?? 'Errore nel caricamento'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load(idItem)
})

watch(() => route.params.id, v => {
  const id = parseId(v);
  if (id != null) load(id)
})

function goBack() {
  router.back()
}

// dopo il salvataggio: chiudi il popup di dettaglio (mostra dati vecchi),
// ricarica la scheda e torna indietro
async function onSaved() {
  closePopup()
  if (idPersonaggio.value) {
    try {
      await characterStore.fetchCharacter(idPersonaggio.value, true)
    } catch (e) {
      console.error('Errore refresh personaggio:', e)
    }
  }
  router.back()
}
</script>

<template>
  <section class="item-editor">
    <header class="head">
      <h1>Editor Oggetto</h1>
      <div class="meta">
        <span v-if="item">#{{ item.id }}</span>
        <span v-if="item?.tipo" class="pill">{{ item.tipo }}</span>
        <button v-if="canUnlink" type="button" class="btn-unlink" :disabled="unlinking || deleting" @click="onUnlink">
          {{ unlinking ? 'Scollegamento…' : 'Scollega' }}
        </button>
        <button v-if="item" type="button" class="btn-delete" :disabled="deleting || unlinking" @click="onDelete">
          {{ deleting ? 'Eliminazione…' : 'Elimina' }}
        </button>
      </div>
    </header>

    <div class="editor-scroll">
      <div v-if="loading" class="state loading">Caricamento…</div>
      <div v-else-if="errorMsg" class="state error">{{ errorMsg }}</div>
      <div v-else-if="!item" class="state empty">Nessun dato.</div>

      <template v-else>
        <!-- editor dinamico per tipo -->
        <component
            v-if="EditorComp"
            :is="EditorComp"
            :item="item"
            :id-personaggio="idPersonaggio"
            @cancel="goBack"
            @saved="onSaved"
        />

        <div v-else class="state unsupported">
          <p><strong>Tipo non gestito:</strong> {{ item.tipo }}</p>
          <button class="btn" @click="goBack">Torna indietro</button>
        </div>
      </template>
    </div>
  </section>
</template>

<style scoped>
.item-editor {
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

.meta {
  display: inline-flex;
  gap: .5rem;
  align-items: center;
}

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
}

.state {
  padding: .75rem;
  border: 1px dashed #e5e7eb;
  border-radius: .5rem;
  margin: 0;
}

.state.error {
  color: #991b1b;
  background: #fef2f2;
  border-color: #fecaca;
}

.unsupported .btn {
  margin-top: .5rem;
}

.btn {
  padding: .5rem .9rem;
  border-radius: .5rem;
  border: 1px solid #d0d5dd;
  background: #fff;
  cursor: pointer;
}

.btn-delete {
  padding: .3rem .7rem;
  border-radius: .5rem;
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #991b1b;
  font-weight: 600;
  cursor: pointer;
}

.btn-delete:disabled { opacity: .6; cursor: default; }

.btn-unlink {
  padding: .3rem .7rem;
  border-radius: .5rem;
  border: 1px solid #fde68a;
  background: #fefce8;
  color: #92400e;
  font-weight: 600;
  cursor: pointer;
}

.btn-unlink:disabled { opacity: .6; cursor: default; }
</style>
