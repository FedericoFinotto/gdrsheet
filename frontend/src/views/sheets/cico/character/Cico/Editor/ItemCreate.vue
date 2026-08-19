<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ItemDB, TIPO_ITEM, TipoItem} from '../../../../../../models/entity/ItemDB'
import {CREATABLE_TYPES, editorForType, TIPO_ITEM_LABELS} from './editorRegistry'
import {useCharacterStore} from '../../../../../../stores/personaggio'
import {useAuthStore} from '../../../../../../stores/auth'
import {useMondoStore} from '../../../../../../stores/mondo'
import {getConfigMondo} from '../../../../../../service/MondoAdminService'
import SearchSelect from '../../../../../../components/SearchSelect.vue'

const route = useRoute()
const router = useRouter()
const characterStore = useCharacterStore()
const auth = useAuthStore()
const mondoStore = useMondoStore()

const canCreateNotizia = computed(() => {
  const r = auth.effectiveRuolo.toUpperCase()
  return r === 'MASTER' || r === 'ADMIN' || r === 'SUPERUSER'
})

// Tipi abilitati per il mondo corrente (vedi MondoTipoItemAbilitato lato backend): null finché
// non risolto o in caso di errore = nessuna restrizione, per non rompere la creazione item se la
// chiamata fallisce o il mondo non è ancora noto.
const tipiAbilitatiMondo = ref<Set<string> | null>(null)

const creatableTypes = computed(() =>
    CREATABLE_TYPES
        .filter(t => t !== TIPO_ITEM.NOTIZIA || canCreateNotizia.value)
        .filter(t => !tipiAbilitatiMondo.value || tipiAbilitatiMondo.value.has(t))
)

// se presente, il nuovo item viene agganciato al FromCompendio del personaggio
const idPersonaggio = computed<number | undefined>(() => {
  const n = Number(route.query.personaggio)
  return Number.isFinite(n) && n > 0 ? n : undefined
})

// se presente (pagina Quest di un party), il nuovo item viene associato al party
const idParty = computed<number | undefined>(() => {
  const n = Number(route.query.party)
  return Number.isFinite(n) && n > 0 ? n : undefined
})

// se presenti (creazione dal Compendio: mondo/sistema attualmente selezionati nello switcher),
// pre-compilano i campi corrispondenti invece di lasciarli vuoti — BaseItemEditor legge i default
// da props.item.mondo?.id/sistema?.id, da qui i placeholder minimali (mai inviati al backend as-is:
// form.idMondo/form.idSistema li sostituiscono integralmente al salvataggio).
const idMondoQuery = computed<number | undefined>(() => {
  const n = Number(route.query.mondo)
  return Number.isFinite(n) && n > 0 ? n : undefined
})
const idSistemaQuery = computed<number | undefined>(() => {
  const n = Number(route.query.sistema)
  return Number.isFinite(n) && n > 0 ? n : undefined
})

// mondo effettivo per filtrare i tipi creabili: quello in query se presente, altrimenti il
// mondo corrente dello switcher globale (carica() è idempotente: se già caricato altrove, es.
// dallo UpperBar, non rifà la chiamata).
const idMondoEffettivo = computed<number | undefined>(() => idMondoQuery.value ?? mondoStore.corrente ?? undefined)

// sistema effettivo: quello in query se presente, altrimenti il sistema del mondo corrente dello
// switcher globale (se noto tra i mondi disponibili)
const idSistemaEffettivo = computed<number | undefined>(() => {
  if (idSistemaQuery.value) return idSistemaQuery.value
  const mondoCorrente = mondoStore.disponibili.find(m => m.id === mondoStore.corrente)
  return mondoCorrente?.sistemaId ?? undefined
})

mondoStore.carica()
watch(idMondoEffettivo, async (idMondo) => {
  if (!idMondo) { tipiAbilitatiMondo.value = null; return }
  try {
    const {data} = await getConfigMondo(idMondo)
    tipiAbilitatiMondo.value = new Set(data.tipiAbilitati)
  } catch (e) {
    console.error('Errore caricamento configurazione mondo:', e)
    tipiAbilitatiMondo.value = null
  }
}, {immediate: true})

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
    mondo: idMondoEffettivo.value ? ({id: idMondoEffettivo.value} as ItemDB['mondo']) : undefined,
    sistema: idSistemaEffettivo.value ? ({id: idSistemaEffettivo.value} as ItemDB['sistema']) : undefined,
  }
})

const EditorComp = computed(() => editorForType(tipo.value))

function onTipoChange(v: string) {
  const params = new URLSearchParams()
  if (route.query.link) params.set('link', '1')   // mantieni il flag "crea e collega"
  if (route.query.compendio) params.set('compendio', '1') // mantieni il flag "mostra nel compendio"
  if (route.query.nome) params.set('nome', String(route.query.nome)) // mantieni il nome pre-compilato
  if (idPersonaggio.value) params.set('personaggio', String(idPersonaggio.value))
  if (idParty.value) params.set('party', String(idParty.value))
  if (idMondoQuery.value) params.set('mondo', String(idMondoQuery.value)) // mantieni il mondo pre-compilato
  if (idSistemaQuery.value) params.set('sistema', String(idSistemaQuery.value))
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

/* Floppy: salvato restando nell'editor. Qui l'item è appena stato CREATO, quindi si passa
 * subito alla sua modifica: restare in "creazione" farebbe creare un doppione al click
 * successivo. replace e non push, così Annulla torna da dove si era arrivati. */
async function onSavedResta(item: { id: number }) {
  await refreshPersonaggio()
  const params = new URLSearchParams()
  if (idPersonaggio.value) params.set('personaggio', String(idPersonaggio.value))
  const q = params.toString() ? `?${params.toString()}` : ''
  router.replace(`/itemeditor/${item.id}${q}`)
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
          :id-party="idParty"
          @cancel="goBack"
          @saved="onSaved"
          @saved-stay="onSavedStay"
          @saved-resta="onSavedResta"
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
  background: var(--surface-0);
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
  border-bottom: 1px solid var(--hairline);
}

.head h1 {
  margin: 0;
  font-size: 1.1rem;
  flex-shrink: 0;
}

.meta { display: flex; flex-wrap: wrap; gap: .4rem; align-items: center; justify-content: flex-end; margin-left: auto; }
.meta > * { flex-shrink: 0; }

.pill {
  font-size: .8rem;
  padding: .1rem .5rem;
  border-radius: .5rem;
  background: var(--info-bg);
  color: var(--info-text);
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
  width: 100%; padding: .5rem .6rem; border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0);
}

.state {
  padding: .75rem;
  border: 1px dashed var(--hairline);
  border-radius: .5rem;
  margin: 0;
}
</style>
