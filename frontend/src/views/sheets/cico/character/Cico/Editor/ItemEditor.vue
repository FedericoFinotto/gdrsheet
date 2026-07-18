<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import type {AxiosResponse} from 'axios'
import {deleteItem, getItem, getItemDisabled, getItemParents, switchItemState, unlinkItem} from '../../../../../../service/PersonaggioService'
import type {Item} from '../../../../../../models/dto/Item'
import {useCharacterStore} from '../../../../../../stores/personaggio'
import usePopup from '../../../../../../function/usePopup'
import {useAuthStore} from '../../../../../../stores/auth'

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
const parents = ref<Item[]>([])

const EditorComp = computed(() => editorForType(item.value?.tipo))

// contesto personaggio (per scollegamento/refresh dopo eliminazione)
const idPersonaggio = computed<number | undefined>(() => {
  const n = Number(route.query.personaggio)
  return Number.isFinite(n) && n > 0 ? n : undefined
})

// contesto party (es. QUEST aperta dalla pagina Quest di un party, senza un personaggio di mezzo)
const idParty = computed<number | undefined>(() => {
  const n = Number(route.query.party)
  return Number.isFinite(n) && n > 0 ? n : undefined
})

const deleting = ref(false)
const unlinking = ref(false)

// stato abilitato/disabilitato (toggle esplicito)
const disabled = ref(false)
const toggling = ref(false)
// ha senso solo nel contesto personaggio e non per i livelli
const canToggle = computed(() =>
    !!idPersonaggio.value && !!item.value && item.value.tipo !== 'LIVELLO')

async function loadStato() {
  if (!idPersonaggio.value || !item.value) return
  try {
    const res = await getItemDisabled(item.value.id, idPersonaggio.value)
    disabled.value = !!res.data
  } catch (e) {
    console.error('Errore stato item:', e)
  }
}

async function onToggleStato() {
  if (!item.value || !idPersonaggio.value || toggling.value) return
  toggling.value = true
  try {
    await switchItemState(item.value.id, idPersonaggio.value)
    disabled.value = !disabled.value
    await characterStore.fetchCharacter(idPersonaggio.value, true)
  } catch (e) {
    errorMsg.value = (e as any)?.message ?? 'Errore nel cambio stato'
    console.error('Errore switch stato item:', e)
  } finally {
    toggling.value = false
  }
}

// scollegabile: c'è il contesto personaggio e non è un item intestato (es. livelli)
const canUnlink = computed(() =>
    !!idPersonaggio.value && !!item.value && item.value.tipo !== 'LIVELLO')

// eliminazione: solo master e admin
const auth = useAuthStore()
const canDelete = computed(() => {
  const r = (auth.utente?.ruolo ?? '').toUpperCase()
  return r === 'MASTER' || r === 'ADMIN' || r === 'SUPERUSER'
})

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
  parents.value = []
  try {
    const res: AxiosResponse<ItemDB> = await getItem(id)
    item.value = res.data
    try {
      const pres = await getItemParents(id)
      parents.value = pres.data ?? []
    } catch (e) {
      console.error('Errore caricamento padri item:', e)
    }
    await loadStato()
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

function openParent(p: Item) {
  const params = new URLSearchParams()
  if (idPersonaggio.value) params.set('personaggio', String(idPersonaggio.value))
  else if (idParty.value) params.set('party', String(idParty.value))
  const q = params.toString() ? `?${params.toString()}` : ''
  router.push(`/itemeditor/${p.id}${q}`)
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
        <button v-if="canToggle" type="button" class="btn-toggle" :class="disabled ? 'off' : 'on'"
                :disabled="toggling || deleting || unlinking" @click="onToggleStato">
          {{ toggling ? '…' : (disabled ? 'Disabilitato' : 'Abilitato') }}
        </button>
        <button v-if="canUnlink" type="button" class="btn-unlink" :disabled="unlinking || deleting" @click="onUnlink">
          {{ unlinking ? 'Scollegamento…' : 'Scollega' }}
        </button>
        <button v-if="item && canDelete" type="button" class="btn-delete" :disabled="deleting || unlinking" @click="onDelete">
          {{ deleting ? 'Eliminazione…' : 'Elimina' }}
        </button>
      </div>
    </header>

    <div v-if="parents.length" class="parent-bar">
      <span class="parent-label">Collegato a:</span>
      <button
          v-for="p in parents" :key="p.id"
          type="button" class="parent-chip"
          :title="`Apri ${p.nome}`"
          @click="openParent(p)"
      >{{ p.nome }}</button>
    </div>

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
            :id-party="idParty"
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
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: .5rem .75rem;
  padding: .75rem 1rem;
  background: inherit;
  border-bottom: 1px solid #e5e7eb;
}

.head h1 {
  margin: 0;
  font-size: 1.1rem;
  min-width: 0;
  flex-shrink: 0;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: .4rem;
  align-items: center;
  justify-content: flex-end;
  margin-left: auto;
}

.meta > * { flex-shrink: 0; }

.pill {
  font-size: .8rem;
  padding: .1rem .5rem;
  border-radius: .5rem;
  background: #eef2ff;
  color: #3730a3;
}

.parent-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: .4rem;
  padding: .5rem 1rem;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.parent-label {
  font-size: .8rem;
  font-weight: 600;
  opacity: .7;
}

.parent-chip {
  font-size: .8rem;
  padding: .25rem .6rem;
  border-radius: .5rem;
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 600;
  cursor: pointer;
}

.parent-chip:hover { background: #dbeafe; }

.editor-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  padding: .75rem 1rem calc(2rem + env(safe-area-inset-bottom, 0px));
  display: grid;
  gap: .75rem;
  /* senza questo, con contenuto più corto dello schermo le righe del grid
     si stirano (align-content: stretch) creando spazi vuoti casuali */
  align-content: start;
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

.btn-toggle {
  padding: .3rem .7rem;
  border-radius: .5rem;
  border: 1px solid transparent;
  font-weight: 600;
  cursor: pointer;
}
.btn-toggle.on { border-color: #bbf7d0; background: #f0fdf4; color: #166534; }
.btn-toggle.off { border-color: #fed7aa; background: #fff7ed; color: #9a3412; }
.btn-toggle:disabled { opacity: .6; cursor: default; }

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
