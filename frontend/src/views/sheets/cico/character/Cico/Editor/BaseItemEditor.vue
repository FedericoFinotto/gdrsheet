<script setup lang="ts">
import {computed, onMounted, reactive, ref, toRaw, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
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
import useChildCreate from '../../../../../../function/useChildCreate'
import HtmlEditor from '../../../../../../components/HtmlEditor.vue'
import SearchSelect from '../../../../../../components/SearchSelect.vue'
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
  separateForme?: boolean     // separa i child FORMA in una sezione dedicata
}>(), {
  titolo: 'Item',
  campiLabel: () => [],
  suggestedKeys: () => [],
  mode: 'edit',
})

const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void; (e: 'savedStay'): void }>()

// tipi con gestione quantità (label QTA, moltiplica il peso)
const TIPI_CON_QTA = ['OGGETTO', 'CONSUMABILE', 'ARMA', 'MUNIZIONE', 'EQUIPAGGIAMENTO']
const showQta = computed(() => TIPI_CON_QTA.includes(props.item.tipo))

const form = reactive<{
  nome: string
  enName: string
  manuale: string
  descrizione: string
  campi: Record<string, string>
  labels: LabelRow[]
  modificatori: ModificatoreRow[]
  attacchi: AttaccoRow[]
  children: ChildRef[]
  forme: ChildRef[]
  qta: number
  compendio: boolean
  visibilita: string
  idMondo: number
  idSistema: number
}>({
  nome: '',
  enName: '',
  manuale: '',
  descrizione: '',
  campi: {},
  labels: [],
  modificatori: [],
  attacchi: [],
  children: [],
  forme: [],
  qta: 1,
  compendio: false,
  visibilita: '',
  idMondo: 1,
  idSistema: 1,
})

const open = reactive({labels: false, modificatori: false, attacchi: false, children: false, forme: false})

function preload() {
  form.nome = props.item.nome ?? ''
  form.descrizione = props.item.descrizione ?? ''

  const campoKeys = new Set(props.campiLabel.map(c => c.key))
  form.campi = Object.fromEntries(props.campiLabel.map(c => [c.key, '']))

  form.qta = 1
  form.enName = ''
  form.manuale = ''
  // creando dalla pagina del compendio (?compendio=1) il flag è attivo di default
  form.compendio = props.mode === 'create' && route.query.compendio === '1'
  form.visibilita = ''
  form.labels = []
  for (const l of (props.item.labels ?? [])) {
    const key = l.label ?? ''
    const val = l.valore ?? ''
    if (showQta.value && key === 'QTA') {
      const n = Number(val)
      form.qta = Number.isFinite(n) && n >= 0 ? Math.floor(n) : 1
    } else if (key === 'COMPENDIO') {
      form.compendio = ['true', '1'].includes(String(val).toLowerCase())
    } else if (key === 'VISIBILITA') {
      form.visibilita = String(val).toUpperCase()
    } else if (key === 'EN_NAME') {
      form.enName = val
    } else if (key === 'MANUALE_SP') {
      form.manuale = val
    } else if (campoKeys.has(key) && !form.campi[key]) {
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

  // child: ATTACCO -> sezione attacchi, FORMA (se separateForme) -> forme, il resto -> item collegati
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
  const nonAttacco = children.filter(t => t.tipo !== 'ATTACCO')
  if (props.separateForme) {
    form.forme = nonAttacco.filter(t => t.tipo === 'FORMA').map(t => ({id: t.id, nome: t.nome, tipo: t.tipo}))
    form.children = nonAttacco.filter(t => t.tipo !== 'FORMA').map(t => ({id: t.id, nome: t.nome, tipo: t.tipo}))
  } else {
    form.forme = []
    form.children = nonAttacco.map(t => ({id: t.id, nome: t.nome, tipo: t.tipo}))
  }
}

const router = useRouter()
const route = useRoute()
const childCreate = useChildCreate()

// editor figlio aperto dal flusso "crea e collega": ha ?link=1 nell'URL
const isLinkCreate = computed(() => props.mode === 'create' && !!route.query.link)

// Snapshot del form per preservarlo durante la creazione al volo di un figlio
function snapshotForm() {
  return JSON.parse(JSON.stringify(toRaw(form)))
}

function restoreSnapshot(snap: any) {
  form.nome = snap.nome ?? ''
  form.enName = snap.enName ?? ''
  form.manuale = snap.manuale ?? ''
  form.descrizione = snap.descrizione ?? ''
  form.campi = {...(snap.campi ?? {})}
  form.labels = snap.labels ?? []
  form.modificatori = snap.modificatori ?? []
  form.attacchi = snap.attacchi ?? []
  form.children = snap.children ?? []
  form.forme = snap.forme ?? []
  form.qta = snap.qta ?? 1
  form.compendio = !!snap.compendio
  form.visibilita = snap.visibilita ?? ''
  form.idMondo = snap.idMondo ?? 1
  form.idSistema = snap.idSistema ?? 1
}

// Al mount: se sto tornando da una creazione di figlio (draft pendente e NON sono io
// l'editor figlio), ripristino il form e aggancio il nuovo item creato.
function mountInit() {
  const slot = !route.query.link ? childCreate.peekDraft() : null
  if (slot && slot.tipo === props.item.tipo) {
    childCreate.takeDraft()
    restoreSnapshot(slot.snapshot)
    const created = childCreate.takeCreatedChild()
    if (created) {
      const list = slot.target === 'forme' ? form.forme : form.children
      if (!list.some(c => c.id === created.id)) list.push(created)
    }
    return
  }
  preload()
}

onMounted(mountInit)
watch(() => props.item?.id, preload)

// Crea un nuovo item collegato al volo: salva lo stato corrente e apre l'editor di creazione.
function onCreateChild(target: 'children' | 'forme', tipo?: string, nome?: string) {
  childCreate.stashDraft({target, tipo: props.item.tipo, snapshot: snapshotForm()})
  const tipoSeg = tipo ? `/${tipo}` : ''
  const params = new URLSearchParams({link: '1'})
  if (props.idPersonaggio) params.set('personaggio', String(props.idPersonaggio))
  if (nome && nome.trim()) params.set('nome', nome.trim())
  router.push(`/itemcreate${tipoSeg}?${params.toString()}`)
}

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
const sumForme = computed(() =>
    form.forme.map(c => c.nome).join(', ') || '—')

function buildPayload(): UpdateItemRequest {
  const labels: LabelRow[] = []
  // quantità -> label QTA
  if (showQta.value) {
    labels.push({label: 'QTA', valore: String(Math.max(0, Math.floor(Number(form.qta) || 0)))})
  }
  // campi specifici -> labels
  for (const c of props.campiLabel) {
    const v = (form.campi[c.key] ?? '').trim()
    if (v) labels.push({label: c.key, valore: v})
  }
  // labels generiche
  for (const l of form.labels) {
    if (l.label.trim()) labels.push({label: l.label.trim(), valore: l.valore})
  }
  // nome originale inglese e manuale di provenienza
  if (form.enName.trim()) labels.push({label: 'EN_NAME', valore: form.enName.trim()})
  if (form.manuale.trim()) labels.push({label: 'MANUALE_SP', valore: form.manuale.trim()})
  // flag compendio
  if (form.compendio) labels.push({label: 'COMPENDIO', valore: 'true'})
  // visibilità item (vuoto = visibile a tutti)
  if (form.visibilita) labels.push({label: 'VISIBILITA', valore: form.visibilita})
  return toRaw({
    nome: form.nome.trim(),
    descrizione: form.descrizione,
    tipo: props.mode === 'create' ? props.item.tipo : undefined,
    idPersonaggio: props.mode === 'create' ? props.idPersonaggio : undefined,
    idMondo: props.mode === 'create' ? (Number(form.idMondo) || undefined) : undefined,
    idSistema: props.mode === 'create' ? (Number(form.idSistema) || undefined) : undefined,
    // creazione "al volo" di un figlio: tieni mondo/sistema ma non agganciare al FromCompendio
    skipFromCompendio: isLinkCreate.value ? true : undefined,
    labels,
    modificatori: form.modificatori.filter(m => m.statId.trim()),
    attacchi: form.attacchi.filter(a => a.nome.trim()),
    childItemIds: [...form.children, ...form.forme].map(c => c.id),
  })
}

async function doSave(): Promise<ItemDB | null> {
  busy.value = true
  errorMsg.value = null
  try {
    const payload = buildPayload()
    const res = props.mode === 'create'
        ? await createItem(payload)
        : await updateItem(props.item.id, payload, props.idPersonaggio)
    return res.data ?? null
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 403
        ? 'Non hai i permessi per modificare questo personaggio'
        : (e?.message ?? 'Errore nel salvataggio')
    return null
  } finally {
    busy.value = false
  }
}

async function onSave() {
  if (!canSave.value) return
  const saved = await doSave()
  if (!saved) return
  // se sono l'editor figlio del flusso "crea e collega", registro l'item creato;
  // altrimenti (salvataggio normale) ripulisco eventuali draft pendenti
  if (isLinkCreate.value) {
    childCreate.setCreatedChild({id: saved.id, nome: saved.nome, tipo: saved.tipo})
  } else {
    childCreate.clearDraft()
  }
  emit('saved')
}

/* Salva e continua: salva, poi resta nell'editor con un nuovo item dello stesso tipo */
async function onSaveAndNew() {
  if (!canSave.value) return
  if (await doSave()) {
    emit('savedStay')
    resetForNew()
  }
}

function resetForNew() {
  form.nome = ''
  form.descrizione = ''
  form.campi = Object.fromEntries(props.campiLabel.map(c => [c.key, '']))
  form.labels = []
  form.modificatori = []
  form.attacchi = []
  form.children = []
  form.forme = []
  form.qta = 1
}

function onCancel() {
  // se annullo l'editor figlio del flusso "crea e collega" lascio il draft (il padre
  // lo ripristina senza agganciare nulla); se annullo un editor normale lo ripulisco
  if (!isLinkCreate.value) childCreate.clearDraft()
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

    <div class="row nome-qta">
      <label class="field grow">
        <span class="lbl">Nome</span>
        <input v-model.trim="form.nome" type="text" :disabled="disabledAll" required/>
      </label>
      <label v-if="showQta" class="field qta-field">
        <span class="lbl">Quantità</span>
        <div class="qta-stepper">
          <button type="button" class="qta-btn" :disabled="disabledAll || form.qta <= 0"
                  @click="form.qta = Math.max(0, (Number(form.qta) || 0) - 1)">−</button>
          <input v-model.number="form.qta" type="number" min="0" step="1" inputmode="numeric"
                 :disabled="disabledAll"/>
          <button type="button" class="qta-btn" :disabled="disabledAll"
                  @click="form.qta = (Number(form.qta) || 0) + 1">+</button>
        </div>
      </label>
    </div>

    <div class="row two">
      <label class="field">
        <span class="lbl">Nome originale (EN)</span>
        <input v-model.trim="form.enName" type="text" :disabled="disabledAll"
               placeholder="Nome originale in inglese"/>
      </label>
      <label class="field">
        <span class="lbl">Manuale</span>
        <input v-model.trim="form.manuale" type="text" :disabled="disabledAll"
               placeholder="Manuale di provenienza"/>
      </label>
    </div>

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

    <!-- mondo / sistema (solo in creazione) -->
    <div v-if="mode === 'create'" class="row two">
      <label class="field">
        <span class="lbl">Mondo (id)</span>
        <input v-model.number="form.idMondo" type="number" min="1" step="1" :disabled="disabledAll"/>
      </label>
      <label class="field">
        <span class="lbl">Sistema (id)</span>
        <input v-model.number="form.idSistema" type="number" min="1" step="1" :disabled="disabledAll"/>
      </label>
    </div>

    <!-- flag compendio -->
    <label class="compendio-flag">
      <input type="checkbox" v-model="form.compendio" :disabled="disabledAll"/>
      <span>Visibile nel compendio</span>
    </label>

    <!-- visibilità item nel personaggio -->
    <label class="field">
      <span class="lbl">Visibilità</span>
      <SearchSelect v-model="form.visibilita" :disabled="disabledAll" :sort="false"
                    :options="[{value:'',label:'Tutti'},{value:'OWNER',label:'Proprietario'},{value:'MASTER',label:'Master'}]"/>
    </label>

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

    <!-- Forme (child FORMA, solo se separateForme) -->
    <section v-if="separateForme" class="fold">
      <button type="button" class="fold-head" @click="open.forme = !open.forme"
              :aria-expanded="open.forme ? 'true' : 'false'">
        <span class="fold-title">Forme</span>
        <span class="fold-summary">{{ sumForme }}</span>
        <span class="chev" :class="{ open: open.forme }">▸</span>
      </button>
      <div v-show="open.forme" class="fold-body">
        <ChildrenEditor v-model="form.forme" :disabled="disabledAll" :exclude-id="props.item.id" only-tipo="FORMA"
                        @create-new="(t, n) => onCreateChild('forme', 'FORMA', n)"/>
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
        <ChildrenEditor v-model="form.children" :disabled="disabledAll" :exclude-id="props.item.id"
                        :exclude-tipo="separateForme ? 'FORMA' : undefined"
                        @create-new="(t, n) => onCreateChild('children', t, n)"/>
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
      <HtmlEditor v-model="form.descrizione" :rows="10" :disabled="disabledAll"/>
    </label>

    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

    <div class="actions">
      <button type="button" class="btn ghost" @click="onCancel" :disabled="busy">Annulla</button>
      <button v-if="mode === 'create' && !isLinkCreate" type="button" class="btn outline" :disabled="!canSave" @click="onSaveAndNew">
        Salva e continua
      </button>
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

input[type="text"], textarea, select {
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
.compendio-flag {
  display: inline-flex; align-items: center; gap: .5rem;
  font-size: .85rem; font-weight: 600; cursor: pointer;
}
.compendio-flag input { width: auto; }

.btn { padding: .5rem .9rem; border-radius: .5rem; border: 1px solid transparent; cursor: pointer; }
.btn.ghost { border-color: #d0d5dd; background: #fff; }
.btn.outline { border-color: #93c5fd; background: #eff6ff; color: #1d4ed8; font-weight: 600; }
.btn.primary { background: #2563eb; color: white; }
.btn:disabled { opacity: .6; cursor: default; }

/* nome + quantità */
.nome-qta { display: grid; grid-template-columns: 1fr auto; gap: .5rem; align-items: end; }
.field.grow { min-width: 0; }
.qta-field { width: 9rem; }
.qta-stepper { display: grid; grid-template-columns: auto 1fr auto; gap: .25rem; align-items: stretch; }
.qta-stepper input {
  width: 100%; text-align: center; padding: .5rem .25rem;
  border: 1px solid #d0d5dd; border-radius: .5rem; font-variant-numeric: tabular-nums;
}
.qta-btn {
  width: 2.1rem; border: 1px solid #d0d5dd; border-radius: .5rem; background: #f9fafb;
  font-weight: 800; font-size: 1rem; cursor: pointer;
}
.qta-btn:disabled { opacity: .5; cursor: default; }
</style>
