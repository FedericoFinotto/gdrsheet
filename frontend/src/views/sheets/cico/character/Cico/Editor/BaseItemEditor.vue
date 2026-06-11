<script setup lang="ts">
import {computed, onMounted, reactive, ref, toRaw, watch} from 'vue'
import {ItemDB} from '../../../../../../models/entity/ItemDB'
import {
  AttaccoRow,
  CampoLabel,
  ChildRef,
  LabelRow,
  ModificatoreRow,
  UpdateItemRequest
} from '../../../../../../models/dto/UpdateItemRequest'
import {createItem, updateItem} from '../../../../../../service/PersonaggioService'
import {getItemLabel} from '../../../../../../models/entity/ItemLabel'
import LabelsEditor from './Sections/LabelsEditor.vue'
import ModificatoriEditor from './Sections/ModificatoriEditor.vue'
import AttacchiEditor from './Sections/AttacchiEditor.vue'
import ChildrenEditor from './Sections/ChildrenEditor.vue'

const props = withDefaults(defineProps<{
  item: ItemDB
  titolo?: string
  campiLabel?: CampoLabel[]   // campi specifici per tipo, mappati su ItemLabel
  suggestedKeys?: string[]    // chiavi suggerite nella sezione labels generiche
  readonly?: boolean
  mode?: 'edit' | 'create'
  idPersonaggio?: number      // solo create: aggancia l'item al FromCompendio del personaggio
}>(), {
  titolo: 'Item',
  campiLabel: () => [],
  suggestedKeys: () => [],
  mode: 'edit',
})

const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void }>()

const form = reactive<{
  nome: string
  descrizione: string
  campi: Record<string, string>
  labels: LabelRow[]
  modificatori: ModificatoreRow[]
  attacchi: AttaccoRow[]
  children: ChildRef[]
}>({
  nome: '',
  descrizione: '',
  campi: {},
  labels: [],
  modificatori: [],
  attacchi: [],
  children: [],
})

const open = reactive({labels: false, modificatori: false, attacchi: false, children: false})

function preload() {
  form.nome = props.item.nome ?? ''
  form.descrizione = props.item.descrizione ?? ''

  const campoKeys = new Set(props.campiLabel.map(c => c.key))
  form.campi = Object.fromEntries(props.campiLabel.map(c => [c.key, '']))

  form.labels = []
  for (const l of (props.item.labels ?? [])) {
    const key = l.label ?? ''
    const val = l.valore ?? ''
    if (campoKeys.has(key) && !form.campi[key]) {
      form.campi[key] = val
    } else {
      form.labels.push({label: key, valore: val})
    }
  }

  form.modificatori = (props.item.modificatori ?? []).map(m => ({
    id: m.id,
    statId: m.stat?.id ?? '',
    tipo: m.tipo,
    valore: String(m.valore ?? ''),
    nota: m.nota ?? '',
    sempreAttivo: !!m.sempreAttivo,
  }))

  // child: ATTACCO -> sezione attacchi, il resto -> item collegati
  const children = (props.item.child ?? []).map(c => c.itemTarget).filter(Boolean)
  form.attacchi = children
      .filter(t => t.tipo === 'ATTACCO')
      .map(t => ({
        id: t.id,
        nome: t.nome,
        tpc: getItemLabel(t, 'TPC') ?? '',
        tpd: getItemLabel(t, 'TPD') ?? '',
        tipoDanni: getItemLabel(t, 'TDANNO' as any) ?? '',
      }))
  form.children = children
      .filter(t => t.tipo !== 'ATTACCO')
      .map(t => ({id: t.id, nome: t.nome, tipo: t.tipo}))
}

onMounted(preload)
watch(() => props.item?.id, preload)

const busy = ref(false)
const errorMsg = ref<string | null>(null)
const disabledAll = computed(() => !!props.readonly || busy.value)
const canSave = computed(() => form.nome.trim().length > 0 && !busy.value && !props.readonly)

const sumLabels = computed(() =>
    form.labels.filter(l => l.label).map(l => l.label).join(', ') || '—')
const sumMods = computed(() =>
    form.modificatori.filter(m => m.statId).map(m => `${m.statId} ${m.valore}`).join(', ') || '—')
const sumAttacchi = computed(() =>
    form.attacchi.filter(a => a.nome).map(a => a.nome).join(', ') || '—')
const sumChildren = computed(() =>
    form.children.map(c => c.nome).join(', ') || '—')

function buildPayload(): UpdateItemRequest {
  const labels: LabelRow[] = []
  // campi specifici -> labels
  for (const c of props.campiLabel) {
    const v = (form.campi[c.key] ?? '').trim()
    if (v) labels.push({label: c.key, valore: v})
  }
  // labels generiche
  for (const l of form.labels) {
    if (l.label.trim()) labels.push({label: l.label.trim(), valore: l.valore})
  }
  return toRaw({
    nome: form.nome.trim(),
    descrizione: form.descrizione,
    tipo: props.mode === 'create' ? props.item.tipo : undefined,
    idPersonaggio: props.mode === 'create' ? props.idPersonaggio : undefined,
    labels,
    modificatori: form.modificatori.filter(m => m.statId.trim()),
    attacchi: form.attacchi.filter(a => a.nome.trim()),
    childItemIds: form.children.map(c => c.id),
  })
}

async function onSave() {
  if (!canSave.value) return
  busy.value = true
  errorMsg.value = null
  try {
    const payload = buildPayload()
    if (props.mode === 'create') await createItem(payload)
    else await updateItem(props.item.id, payload)
    emit('saved')
  } catch (e: any) {
    errorMsg.value = e?.message ?? 'Errore nel salvataggio'
  } finally {
    busy.value = false
  }
}

function onCancel() {
  emit('cancel')
}
</script>

<template>
  <form class="base-editor" @submit.prevent="onSave">
    <header class="be-head">
      <h2>{{ titolo }}</h2>
      <span class="muted" v-if="mode === 'edit'">ID #{{ props.item.id }}</span>
      <span class="muted" v-else>nuovo</span>
    </header>

    <label class="field">
      <span class="lbl">Nome</span>
      <input v-model.trim="form.nome" type="text" :disabled="disabledAll" required/>
    </label>

    <!-- campi specifici per tipo -->
    <div v-if="campiLabel.length" class="row two">
      <label v-for="c in campiLabel" :key="c.key" class="field" :class="{ full: c.textarea }">
        <span class="lbl">{{ c.label }}</span>
        <textarea v-if="c.textarea" v-model="form.campi[c.key]" rows="3"
                  :disabled="disabledAll" :placeholder="c.placeholder"/>
        <input v-else v-model.trim="form.campi[c.key]" type="text"
               :disabled="disabledAll" :placeholder="c.placeholder"/>
      </label>
    </div>

    <!-- slot per estensioni specifiche del tipo -->
    <slot name="specifico" :disabled="disabledAll"/>

    <!-- Attacchi (item ATTACCO figli) -->
    <section class="fold">
      <button type="button" class="fold-head" @click="open.attacchi = !open.attacchi"
              :aria-expanded="open.attacchi ? 'true' : 'false'">
        <span class="fold-title">Attacchi</span>
        <span class="fold-summary">{{ sumAttacchi }}</span>
        <span class="chev" :class="{ open: open.attacchi }">▸</span>
      </button>
      <div v-show="open.attacchi" class="fold-body">
        <AttacchiEditor v-model="form.attacchi" :disabled="disabledAll"/>
      </div>
    </section>

    <!-- Item collegati (child) -->
    <section class="fold">
      <button type="button" class="fold-head" @click="open.children = !open.children"
              :aria-expanded="open.children ? 'true' : 'false'">
        <span class="fold-title">Item collegati</span>
        <span class="fold-summary">{{ sumChildren }}</span>
        <span class="chev" :class="{ open: open.children }">▸</span>
      </button>
      <div v-show="open.children" class="fold-body">
        <ChildrenEditor v-model="form.children" :disabled="disabledAll" :exclude-id="props.item.id"/>
      </div>
    </section>

    <!-- Labels generiche -->
    <section class="fold">
      <button type="button" class="fold-head" @click="open.labels = !open.labels"
              :aria-expanded="open.labels ? 'true' : 'false'">
        <span class="fold-title">Labels</span>
        <span class="fold-summary">{{ sumLabels }}</span>
        <span class="chev" :class="{ open: open.labels }">▸</span>
      </button>
      <div v-show="open.labels" class="fold-body">
        <LabelsEditor v-model="form.labels" :suggested-keys="suggestedKeys" :disabled="disabledAll"/>
      </div>
    </section>

    <!-- Modificatori -->
    <section class="fold">
      <button type="button" class="fold-head" @click="open.modificatori = !open.modificatori"
              :aria-expanded="open.modificatori ? 'true' : 'false'">
        <span class="fold-title">Modificatori</span>
        <span class="fold-summary">{{ sumMods }}</span>
        <span class="chev" :class="{ open: open.modificatori }">▸</span>
      </button>
      <div v-show="open.modificatori" class="fold-body">
        <ModificatoriEditor v-model="form.modificatori" :disabled="disabledAll"/>
      </div>
    </section>

    <label class="field">
      <span class="lbl">Descrizione</span>
      <textarea v-model="form.descrizione" rows="10" :disabled="disabledAll" spellcheck="false"/>
    </label>

    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

    <div class="actions">
      <button type="button" class="btn ghost" @click="onCancel" :disabled="busy">Annulla</button>
      <button type="submit" class="btn primary" :disabled="!canSave">Salva</button>
    </div>
  </form>
</template>

<style scoped>
.base-editor { display: grid; gap: .75rem; margin: 0; }
.be-head { display: flex; align-items: baseline; gap: .5rem; margin: 0; }
.be-head h2 { margin: 0; font-size: 1rem; }
.muted { opacity: .7; font-size: .85rem; }

.row { display: grid; gap: .5rem; }
.row.two { grid-template-columns: 1fr 1fr; }
@media (max-width: 900px) { .row.two { grid-template-columns: 1fr; } }
.field.full { grid-column: 1 / -1; }

.field { display: grid; gap: .35rem; margin: 0; }
.lbl { font-size: .8rem; font-weight: 600; opacity: .85; margin: 0; }

input[type="text"], textarea {
  width: 100%; padding: .5rem .6rem; border: 1px solid #d0d5dd; border-radius: .5rem; background: #fff; margin: 0;
}
textarea { resize: vertical; }

.fold { border: 1px solid #e5e7eb; border-radius: .5rem; background: #fff; }
.fold-head {
  width: 100%; display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: .5rem;
  padding: .5rem .75rem; background: #f9fafb; border: 0; border-bottom: 1px solid #e5e7eb; cursor: pointer; text-align: left;
}
.fold-title { font-weight: 600; }
.fold-summary { color: #374151; opacity: .8; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.chev { transition: transform .15s ease; }
.chev.open { transform: rotate(90deg); }
.fold-body { padding: .6rem .75rem; }

.error {
  margin: 0; padding: .5rem .75rem; border-radius: .5rem;
  color: #991b1b; background: #fef2f2; border: 1px solid #fecaca; font-size: .85rem;
}

.actions {
  position: sticky; bottom: 0; background: #fff;
  padding: .5rem 0 calc(.5rem + env(safe-area-inset-bottom, 0px));
  margin-top: .25rem; border-top: 1px solid #e5e7eb;
  display: flex; justify-content: flex-end; gap: .5rem;
}
.btn { padding: .5rem .9rem; border-radius: .5rem; border: 1px solid transparent; cursor: pointer; }
.btn.ghost { border-color: #d0d5dd; background: #fff; }
.btn.primary { background: #2563eb; color: white; }
.btn:disabled { opacity: .6; cursor: default; }
</style>
