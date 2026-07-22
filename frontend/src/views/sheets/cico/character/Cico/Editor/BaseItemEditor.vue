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
import {createItem, getItem, searchItems, updateItem} from '../../../../../../service/PersonaggioService'
import {Item} from '../../../../../../models/dto/Item'
import {getItemLabel, getItemLabels} from '../../../../../../models/entity/ItemLabel'
import useChildCreate from '../../../../../../function/useChildCreate'
import {useMondoSistema} from '../../../../../../function/useMondoSistema'
import HtmlEditor from '../../../../../../components/HtmlEditor.vue'
import SearchSelect from '../../../../../../components/SearchSelect.vue'
import LabelsEditor from './Sections/LabelsEditor.vue'
import MultiValueField from './Sections/MultiValueField.vue'
import ModificatoriEditor from './Sections/ModificatoriEditor.vue'
import AttacchiEditor from './Sections/AttacchiEditor.vue'
import ChildrenEditor from './Sections/ChildrenEditor.vue'
import EffettiEditor from './Sections/EffettiEditor.vue'
import SottoQuestEditor from './Sections/SottoQuestEditor.vue'
import NotesEditor, {NotaRow} from './Sections/NotesEditor.vue'
import {SPELL_LIST_CODES, spellListLabel} from '../../../../../../function/spellLists'

const props = withDefaults(defineProps<{
  item: ItemDB
  titolo?: string
  campiLabel?: CampoLabel[]      // campi specifici per tipo, mappati su ItemLabel
  campiLabelTitolo?: string      // titolo card per la sezione campiLabel (opzionale)
  suggestedKeys?: string[]       // chiavi suggerite nella sezione labels generiche
  readonly?: boolean
  mode?: 'edit' | 'create'
  idPersonaggio?: number      // solo create: aggancia l'item al FromCompendio del personaggio
  idParty?: number            // solo create QUEST di ambito PARTY: party a cui associare la quest (se non in un contesto personaggio)
  separateForme?: boolean     // separa i child FORMA in una sezione dedicata
  minimal?: boolean           // nasconde sezioni avanzate (EN name, manuale, utilizzi, compendio, folds)
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

// QUEST: albero di sotto-quest (child collegati di tipo QUEST). L'ambito (a chi è associata
// la quest RADICE: personaggio/party/mondo) si sceglie solo in creazione e solo per la radice
// (le sotto-quest, create dal flusso "crea e collega", non hanno un ambito proprio).
const isQuest = computed(() => props.item.tipo === 'QUEST')
const isVeicolo = computed(() => props.item.tipo === 'VEICOLO')
// Sezione "Incantesimi" su item generico (non classe): stessa lettura SPELL_<n>* del backend,
// ma slot/conosciuti sono un valore fisso (una riga), non una progressione a 20 livelli.
const TIPI_CON_INCANTESIMI = ['OGGETTO', 'EQUIPAGGIAMENTO', 'CONSUMABILE', 'ALTRO']
const canHaveSpells = computed(() => TIPI_CON_INCANTESIMI.includes(props.item.tipo))
const QUEST_SCOPE_OPTS = [
  {value: 'PERSONAGGIO', label: 'Personaggio'},
  {value: 'PARTY', label: 'Party'},
  {value: 'MONDO', label: 'Mondo (visibile a tutti i party)'},
]

const form = reactive<{
  nome: string
  enName: string
  manuale: string
  descrizione: string
  campi: Record<string, string>
  campiMulti: Record<string, string[]>
  descrOggetto: { magico: boolean; psionico: boolean; divino: boolean; leggendario: boolean; unico: boolean }
  infoOggetto: { taglia: string; costo: string; materiale: string; prefisso: string }
  infoVeicolo: { velocita: string }
  descrAbilita: { straordinaria: boolean; magica: boolean; soprannaturale: boolean; naturale: boolean; divina: boolean }
  labels: LabelRow[]
  modificatori: ModificatoreRow[]
  attacchi: AttaccoRow[]
  children: ChildRef[]
  forme: ChildRef[]
  effetti: ChildRef[]
  sezioniIncantesimi: Array<{
    liste: string[]; bonus: string; slot: string; conosciutiSeparati: boolean; conosciuti: string
    personalizzata: boolean; incantesimiCustom: Array<{ id: number; nome: string; livello: number }>
  }>
  note: NotaRow[]
  qta: number
  compendio: boolean
  visibilita: string
  idMondo: number
  idSistema: number
  questScope: string
  completata: boolean
}>({
  nome: '',
  enName: '',
  manuale: '',
  descrizione: '',
  campi: Object.fromEntries(props.campiLabel.filter(c => !c.multiValore).map(c => [c.key, ''])),
  campiMulti: Object.fromEntries(props.campiLabel.filter(c => c.multiValore).map(c => [c.key, [] as string[]])),
  descrOggetto: {magico: false, psionico: false, divino: false, leggendario: false, unico: false},
  infoOggetto: {taglia: '', costo: '', materiale: '', prefisso: ''},
  infoVeicolo: {velocita: ''},
  descrAbilita: {straordinaria: false, magica: false, soprannaturale: false, naturale: false, divina: false},
  labels: [],
  modificatori: [],
  attacchi: [],
  children: [],
  forme: [],
  effetti: [],
  sezioniIncantesimi: [],
  note: [],
  qta: 1,
  utilizzi: null as number | null,
  compendio: false,
  visibilita: '',
  idMondo: null as number | null,
  idSistema: null as number | null,
  questScope: '',
  completata: false,
})

const open = reactive({
  labels: false, modificatori: false, attacchi: false, children: false, forme: false, effetti: false,
  campiLabel: false, descrOggetto: false, infoOggetto: false, infoVeicolo: false, descrAbilita: false, note: false,
  incantesimi: false,
})

// Taglia fisica dell'oggetto (es. arma taglia Grande): puramente descrittiva, non modifica
// la taglia del personaggio (a differenza del campo TAGLIA usato altrove per quello scopo).
const TAGLIE_OGGETTO = [
  {value: '', label: '— nessuna —'},
  {value: 'Piccolissima', label: 'Piccolissima'},
  {value: 'Minuta', label: 'Minuta'},
  {value: 'Minuscola', label: 'Minuscola'},
  {value: 'Piccola', label: 'Piccola'},
  {value: 'Media', label: 'Media'},
  {value: 'Grande', label: 'Grande'},
  {value: 'Enorme', label: 'Enorme'},
  {value: 'Mastodontica', label: 'Mastodontica'},
  {value: 'Colossale', label: 'Colossale'},
]

async function preload() {
  form.nome = props.item.nome ?? ''
  form.descrizione = props.item.descrizione ?? ''

  const campoKeys = new Set(props.campiLabel.filter(c => !c.multiValore).map(c => c.key))
  const campoKeysMulti = new Set(props.campiLabel.filter(c => c.multiValore).map(c => c.key))
  form.campi = Object.fromEntries(props.campiLabel.filter(c => !c.multiValore).map(c => [c.key, '']))
  form.campiMulti = Object.fromEntries(props.campiLabel.filter(c => c.multiValore).map(c => [c.key, []]))

  // QTA: preferisce quantita già calcolata (da inventario personaggio), poi labels globali (compendio)
  form.qta = (showQta.value && props.item.quantita != null) ? props.item.quantita : 1
  form.utilizzi = null
  form.enName = ''
  form.manuale = ''
  form.idMondo = props.item.mondo?.id ?? null
  form.idSistema = props.item.sistema?.id ?? null
  // creando dalla pagina del compendio (?compendio=1) il flag è attivo di default
  form.compendio = props.mode === 'create' && route.query.compendio === '1'
  form.visibilita = ''
  form.labels = []
  form.descrOggetto = {magico: false, psionico: false, divino: false, leggendario: false, unico: false}
  form.infoOggetto = {taglia: '', costo: '', materiale: '', prefisso: ''}
  form.infoVeicolo = {velocita: ''}
  form.descrAbilita = {straordinaria: false, magica: false, soprannaturale: false, naturale: false, divina: false}
  form.note = []
  form.completata = false
  form.sezioniIncantesimi = []
  // righe SPELL_<n>* raccolte durante il giro delle label, riassemblate in sezioni a fine ciclo
  // (l'ordine delle label non è garantito, es. _SLOT potrebbe comparire prima di SPELL_<n> stesso)
  const spellRaw: Record<number, {
    liste?: string; bonus?: string; slot?: string; haConosciuti?: string; conosciuti?: string
    personalizzata?: string; incantesimi?: string
  }> = {}
  for (const l of (props.item.labels ?? [])) {
    const key = l.label ?? ''
    const val = l.valore ?? ''
    if (showQta.value && key === 'QTA') {
      // Labels globali: usate solo se quantita non è già stata impostata dall'inventario
      if (props.item.quantita == null) {
        const n = Number(val)
        form.qta = Number.isFinite(n) && n >= 0 ? Math.floor(n) : 1
      }
    } else if (key === 'UTILIZZI') {
      form.utilizzi = Number.isFinite(Number(val)) ? Number(val) : null
    } else if (key === 'COMPENDIO') {
      form.compendio = ['true', '1'].includes(String(val).toLowerCase())
    } else if (key === 'VISIBILITA') {
      form.visibilita = String(val).toUpperCase()
    } else if (key === 'EN_NAME') {
      form.enName = val
    } else if (key === 'MANUALE_SP') {
      form.manuale = val
    } else if (key === 'MAGICO') {
      form.descrOggetto.magico = val === '1'
    } else if (key === 'PSIONICO') {
      form.descrOggetto.psionico = val === '1'
    } else if (key === 'DIVINO') {
      form.descrOggetto.divino = val === '1'
    } else if (key === 'LEGGENDARIO') {
      form.descrOggetto.leggendario = val === '1'
    } else if (key === 'UNICO') {
      form.descrOggetto.unico = val === '1'
    } else if (key === 'COSTO') {
      form.infoOggetto.costo = val
    } else if (key === 'MATERIALE') {
      form.infoOggetto.materiale = val
    } else if (key === 'TAGLIA_OGGETTO') {
      form.infoOggetto.taglia = val
    } else if (key === 'PREFISSO_OGGETTI') {
      form.infoOggetto.prefisso = val
    } else if (key === 'VEICOLO_VELOCITA') {
      form.infoVeicolo.velocita = val
    } else if (key === 'DESCR_STR') {
      form.descrAbilita.straordinaria = val === '1'
    } else if (key === 'DESCR_MAG') {
      form.descrAbilita.magica = val === '1'
    } else if (key === 'DESCR_SOP') {
      form.descrAbilita.soprannaturale = val === '1'
    } else if (key === 'DESCR_NAT') {
      form.descrAbilita.naturale = val === '1'
    } else if (key === 'DESCR_DIV') {
      form.descrAbilita.divina = val === '1'
    } else if (key === 'QUEST_COMPLETATA') {
      form.completata = val === '1'
    } else if (key === 'NOTA') {
      try {
        const parsed = JSON.parse(val)
        form.note.push({testo: String(parsed.testo ?? ''), visibilita: String(parsed.visibilita ?? '')})
      } catch {
        // valore non JSON (dato legacy o corrotto): ignora la nota
      }
    } else if (campoKeysMulti.has(key)) {
      form.campiMulti[key].push(val)
    } else if (campoKeys.has(key) && !form.campi[key]) {
      form.campi[key] = val
    } else if (/^SPELL_\d+(_PROG|_BONUS|_SLOT|_HA_CONOSCIUTI|_CONOSCIUTI|_CUSTOM|_INCANTESIMI)?$/.test(key)) {
      const m = key.match(/^SPELL_(\d+)(_PROG|_BONUS|_SLOT|_HA_CONOSCIUTI|_CONOSCIUTI|_CUSTOM|_INCANTESIMI)?$/)!
      const n = Number(m[1])
      const suffix = m[2] ?? ''
      const row = (spellRaw[n] ??= {})
      if (suffix === '') row.liste = val
      else if (suffix === '_BONUS') row.bonus = val
      else if (suffix === '_SLOT') row.slot = val
      else if (suffix === '_HA_CONOSCIUTI') row.haConosciuti = val
      else if (suffix === '_CONOSCIUTI') row.conosciuti = val
      else if (suffix === '_CUSTOM') row.personalizzata = val
      else if (suffix === '_INCANTESIMI') row.incantesimi = val
      // _PROG: ignorata, gli item non hanno progressione (sempre a slot fisso)
    } else {
      form.labels.push({label: key, valore: val})
    }
  }
  form.sezioniIncantesimi = Object.keys(spellRaw).map(Number).sort((a, b) => a - b).map(n => {
    const r = spellRaw[n]
    const incantesimiCustom = (r.incantesimi ?? '').split(',').map(s => s.trim()).filter(Boolean).map(tok => {
      const [idStr, livStr] = tok.split(':')
      return {id: Number(idStr), nome: `#${idStr}`, livello: Number(livStr) || 0}
    })
    return {
      liste: (r.liste ?? '').split(',').map(s => s.trim()).filter(Boolean),
      bonus: r.bonus ?? '',
      slot: r.slot ?? '',
      conosciutiSeparati: r.haConosciuti === '1',
      conosciuti: r.conosciuti ?? '',
      personalizzata: r.personalizzata === '1',
      incantesimiCustom,
    }
  })
  // il label salva solo "id:livello": recupera i nomi per mostrarli nell'editor
  await Promise.all(form.sezioniIncantesimi.flatMap(s => s.incantesimiCustom.map(async c => {
    try { c.nome = (await getItem(c.id)).data?.nome ?? c.nome } catch { /* item non trovato: resta il placeholder #id */ }
  })))

  form.modificatori = (props.item.modificatori ?? []).map(m => ({
    id: m.id,
    statId: m.stat?.id ?? '',
    tipo: m.tipo,
    valore: String(m.valore ?? ''),
    nota: m.nota ?? '',
    sempreAttivo: !!m.sempreAttivo,
  }))

  // child: ATTACCO -> sezione attacchi, FORMA (se separateForme) -> forme, il resto -> item collegati
  const collegamentiNonAttacco = (props.item.child ?? []).filter(c => c.itemTarget?.tipo !== 'ATTACCO')
  const attacchi = (props.item.child ?? []).filter(c => c.itemTarget?.tipo === 'ATTACCO')
  form.attacchi = attacchi.map(c => {
    const dannoRows = getItemLabels(c.itemTarget, 'ATK_DANNO' as any) ?? []
    const danni = dannoRows.length
        ? dannoRows.map(v => {
          const [formula, tipo] = v.split('␞')
          return {formula: formula ?? '', tipo: tipo ?? ''}
        })
        // dati vecchi: un solo danno, ricavato da TPD/TDANNO
        : (getItemLabel(c.itemTarget, 'TPD') ? [{
          formula: getItemLabel(c.itemTarget, 'TPD') ?? '',
          tipo: getItemLabel(c.itemTarget, 'TDANNO' as any) ?? '',
        }] : [])
    return {
      id: c.itemTarget.id,
      nome: c.itemTarget.nome,
      tipoRisoluzione: getItemLabel(c.itemTarget, 'ATK_TIPO' as any) ?? (getItemLabel(c.itemTarget, 'TPC') ? 'TPC' : ''),
      tpc: getItemLabel(c.itemTarget, 'TPC') ?? '',
      tiroSalvezza: getItemLabel(c.itemTarget, 'TTS' as any) ?? '',
      tiroSalvezzaCd: getItemLabel(c.itemTarget, 'TTS_CD' as any) ?? '',
      danni,
    }
  })
  const isColNascosto = (c: any) => (c.labels ?? []).some((l: any) => l.label === 'HIDDEN' && l.valore === '1')
  const getColLabel = (c: any, key: string) => (c.labels ?? []).find((l: any) => l.label === key)?.valore ?? null
  const toChildRef = (c: any) => ({id: c.itemTarget.id, nome: c.itemTarget.nome, tipo: c.itemTarget.tipo, qty: c.qty ?? null, formulaQty: c.formulaQty ?? null, scelta: c.scelta ?? null, nascosto: isColNascosto(c), condizione: getColLabel(c, 'CONDIZIONE')})
  const collegamentiNonEffetto = collegamentiNonAttacco.filter(c => c.itemTarget.tipo !== 'EFFETTO')
  if (props.separateForme) {
    form.forme = collegamentiNonEffetto.filter(c => c.itemTarget.tipo === 'FORMA').map(toChildRef)
    form.children = collegamentiNonEffetto.filter(c => c.itemTarget.tipo !== 'FORMA').map(toChildRef)
  } else {
    form.forme = []
    form.children = collegamentiNonEffetto.map(toChildRef)
  }
  form.effetti = collegamentiNonAttacco.filter(c => c.itemTarget.tipo === 'EFFETTO').map(toChildRef)
}

// Sezioni incantesimi (item generico): stesso pattern liste/bonus di ClasseEditor.vue, ma slot/
// conosciuti sono una riga sola (nessuna progressione, l'item non ha livelli).
function addSezioneIncantesimi() {
  form.sezioniIncantesimi.push({
    liste: [], bonus: '', slot: '', conosciutiSeparati: false, conosciuti: '',
    personalizzata: false, incantesimiCustom: [],
  })
}
function removeSezioneIncantesimi(i: number) {
  form.sezioniIncantesimi.splice(i, 1)
}
function addListaIncantesimi(s: { liste: string[] }, code: string) {
  if (code && !s.liste.includes(code)) s.liste.push(code)
}
function removeListaIncantesimi(s: { liste: string[] }, code: string) {
  const i = s.liste.indexOf(code)
  if (i >= 0) s.liste.splice(i, 1)
}
function listeIncantesimiDisponibili(s: { liste: string[] }): string[] {
  return SPELL_LIST_CODES.filter(c => !s.liste.includes(c))
}
// codice libero (non nel catalogo SPELL_LIST_CODES), indicizzato per sezione
const customListaCode = reactive<string[]>([])
function confirmCustomLista(s: { liste: string[] }, i: number) {
  const code = (customListaCode[i] ?? '').trim().toUpperCase()
  if (!code) return
  addListaIncantesimi(s, code)
  customListaCode[i] = ''
}

// Sezione personalizzata: ricerca e aggiunta di incantesimi specifici uno a uno (invece di una
// lista standard). Stato indicizzato per sezione (i = indice in form.sezioniIncantesimi).
const queryIncCustom = reactive<Record<number, string>>({})
const livelloIncCustom = reactive<Record<number, number>>({})
const risultatiIncCustom = reactive<Record<number, Item[]>>({})
const searchingIncCustom = reactive<Record<number, boolean>>({})
let debounceIncTimer: ReturnType<typeof setTimeout> | null = null
let searchIncToken = 0

function onQueryIncCustom(i: number) {
  if (debounceIncTimer) clearTimeout(debounceIncTimer)
  debounceIncTimer = setTimeout(async () => {
    const q = (queryIncCustom[i] ?? '').trim()
    if (q.length < 2) {
      risultatiIncCustom[i] = []
      return
    }
    const token = ++searchIncToken
    searchingIncCustom[i] = true
    try {
      const res = await searchItems(q, 'INCANTESIMO')
      if (token !== searchIncToken) return
      risultatiIncCustom[i] = res.data ?? []
    } catch (e) {
      console.error('Errore ricerca incantesimi:', e)
    } finally {
      if (token === searchIncToken) searchingIncCustom[i] = false
    }
  }, 250)
}

function aggiungiIncantesimoCustom(s: { incantesimiCustom: Array<{ id: number; nome: string; livello: number }> }, i: number, itm: Item) {
  const l = Math.min(9, Math.max(0, Math.floor(Number(livelloIncCustom[i] ?? 0))))
  if (s.incantesimiCustom.some(c => c.id === itm.id)) return
  s.incantesimiCustom.push({id: itm.id, nome: itm.nome, livello: l})
  s.incantesimiCustom.sort((a, b) => a.livello - b.livello || a.nome.localeCompare(b.nome))
  risultatiIncCustom[i] = []
  queryIncCustom[i] = ''
}
function rimuoviIncantesimoCustom(s: { incantesimiCustom: any[] }, idx: number) {
  s.incantesimiCustom.splice(idx, 1)
}

const router = useRouter()
const route = useRoute()
const childCreate = useChildCreate()
const {mondoOptions, sistemaOptions, autoMondo, autoSistema} = useMondoSistema()

// In creazione, auto-seleziona se l'utente ha accesso a un solo mondo/sistema
watch([autoMondo, autoSistema], ([m, s]) => {
  if (props.mode !== 'create') return
  if (m !== null && form.idMondo === null) form.idMondo = m
  if (s !== null && form.idSistema === null) form.idSistema = s
}, {immediate: true})

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
  form.campiMulti = {...(snap.campiMulti ?? {})}
  form.descrOggetto = {...(snap.descrOggetto ?? {magico: false, psionico: false, divino: false, leggendario: false, unico: false})}
  form.infoOggetto = {...(snap.infoOggetto ?? {taglia: '', costo: '', materiale: '', prefisso: ''})}
  form.infoVeicolo = {...(snap.infoVeicolo ?? {velocita: ''})}
  form.descrAbilita = {...(snap.descrAbilita ?? {straordinaria: false, magica: false, soprannaturale: false, naturale: false, divina: false})}
  form.labels = snap.labels ?? []
  form.modificatori = snap.modificatori ?? []
  form.attacchi = snap.attacchi ?? []
  form.children = snap.children ?? []
  form.forme = snap.forme ?? []
  form.effetti = snap.effetti ?? []
  form.sezioniIncantesimi = snap.sezioniIncantesimi ?? []
  form.note = snap.note ?? []
  form.qta = snap.qta ?? 1
  form.utilizzi = snap.utilizzi ?? null
  form.compendio = !!snap.compendio
  form.visibilita = snap.visibilita ?? ''
  form.idMondo = snap.idMondo ?? null
  form.idSistema = snap.idSistema ?? null
  form.questScope = snap.questScope ?? ''
  form.completata = !!snap.completata
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
const sumSezioniIncantesimi = computed(() =>
    form.sezioniIncantesimi.length > 0 ? `${form.sezioniIncantesimi.length} sezioni` : 'nessuno')
const sumEffetti = computed(() =>
    form.effetti.map(c => `${c.condizione ?? 'Sempre'}: ${c.nome}`).join(', ') || '—')
const sumNote = computed(() =>
    form.note.length ? `${form.note.length} nota${form.note.length > 1 ? 'e' : ''}` : '—')
const sumDescrOggetto = computed(() => {
  const d = form.descrOggetto
  const flags = []
  if (d.magico) flags.push('Magico')
  if (d.psionico) flags.push('Psionico')
  if (d.divino) flags.push('Divino')
  if (d.leggendario) flags.push('Leggendario')
  if (d.unico) flags.push('Unico')
  return flags.join(', ') || '—'
})
const sumInfoOggetto = computed(() => {
  const i = form.infoOggetto
  const flags = []
  if (i.taglia.trim()) flags.push(`Taglia: ${i.taglia.trim()}`)
  if (i.costo.trim()) flags.push(`Costo: ${i.costo.trim()}`)
  if (i.materiale.trim()) flags.push(`Materiale: ${i.materiale.trim()}`)
  if (i.prefisso.trim()) flags.push(`Prefisso: ${i.prefisso.trim()}`)
  return flags.join(', ') || '—'
})
const sumInfoVeicolo = computed(() =>
    form.infoVeicolo.velocita.trim() ? `Velocità: ${form.infoVeicolo.velocita.trim()}` : '—')
const sumDescrAbilita = computed(() => {
  const d = form.descrAbilita
  const flags = []
  if (d.straordinaria) flags.push('Str')
  if (d.magica) flags.push('Mag')
  if (d.soprannaturale) flags.push('Sop')
  if (d.naturale) flags.push('Naturale')
  if (d.divina) flags.push('Div')
  return flags.join(', ') || '—'
})
const sumCampiLabel = computed(() => {
  const filled = []
  for (const c of props.campiLabel) {
    const has = c.multiValore
        ? (form.campiMulti[c.key] ?? []).some(v => v.trim())
        : !!(form.campi[c.key] ?? '').trim()
    if (has) filled.push(c.label)
  }
  return filled.join(', ') || '—'
})

function buildPayload(): UpdateItemRequest {
  const labels: LabelRow[] = []
  // quantità -> label QTA
  if (showQta.value) {
    labels.push({label: 'QTA', valore: String(Math.max(0, Math.floor(Number(form.qta) || 0)))})
  }
  // campi specifici -> labels
  for (const c of props.campiLabel) {
    if (c.multiValore) {
      for (const v of (form.campiMulti[c.key] ?? [])) {
        const trimmed = v.trim()
        if (trimmed) labels.push({label: c.key, valore: trimmed})
      }
    } else {
      const v = (form.campi[c.key] ?? '').trim()
      if (v) labels.push({label: c.key, valore: v})
    }
  }
  // labels generiche
  for (const l of form.labels) {
    if (l.label.trim()) labels.push({label: l.label.trim(), valore: l.valore})
  }
  // nome originale inglese e manuale di provenienza
  if (form.enName.trim()) labels.push({label: 'EN_NAME', valore: form.enName.trim()})
  if (form.manuale.trim()) labels.push({label: 'MANUALE_SP', valore: form.manuale.trim()})
  // Descrittori Oggetto
  if (form.descrOggetto.magico) labels.push({label: 'MAGICO', valore: '1'})
  if (form.descrOggetto.psionico) labels.push({label: 'PSIONICO', valore: '1'})
  if (form.descrOggetto.divino) labels.push({label: 'DIVINO', valore: '1'})
  if (form.descrOggetto.leggendario) labels.push({label: 'LEGGENDARIO', valore: '1'})
  if (form.descrOggetto.unico) labels.push({label: 'UNICO', valore: '1'})
  // Info Oggetto
  if (form.infoOggetto.taglia.trim()) labels.push({label: 'TAGLIA_OGGETTO', valore: form.infoOggetto.taglia.trim()})
  if (form.infoOggetto.costo.trim()) labels.push({label: 'COSTO', valore: form.infoOggetto.costo.trim()})
  if (form.infoOggetto.materiale.trim()) labels.push({label: 'MATERIALE', valore: form.infoOggetto.materiale.trim()})
  if (form.infoOggetto.prefisso.trim()) labels.push({label: 'PREFISSO_OGGETTI', valore: form.infoOggetto.prefisso.trim()})
  // Info Veicolo
  if (form.infoVeicolo.velocita.trim()) labels.push({label: 'VEICOLO_VELOCITA', valore: form.infoVeicolo.velocita.trim()})
  // Descrittori Abilità
  if (form.descrAbilita.straordinaria) labels.push({label: 'DESCR_STR', valore: '1'})
  if (form.descrAbilita.magica) labels.push({label: 'DESCR_MAG', valore: '1'})
  if (form.descrAbilita.soprannaturale) labels.push({label: 'DESCR_SOP', valore: '1'})
  if (form.descrAbilita.naturale) labels.push({label: 'DESCR_NAT', valore: '1'})
  if (form.descrAbilita.divina) labels.push({label: 'DESCR_DIV', valore: '1'})
  // Quest: stato di completamento (significativo solo per una quest senza sotto-quest)
  if (form.completata) labels.push({label: 'QUEST_COMPLETATA', valore: '1'})
  // Note generiche (qualunque item): ogni riga è un JSON {testo, visibilita}
  for (const n of form.note) {
    if (n.testo.trim()) labels.push({label: 'NOTA', valore: JSON.stringify({testo: n.testo, visibilita: n.visibilita})})
  }
  // utilizzi massimi (globale sull'item)
  if (form.utilizzi != null && Number.isInteger(form.utilizzi) && form.utilizzi > 0)
    labels.push({label: 'UTILIZZI', valore: String(form.utilizzi)})
  // flag compendio
  if (form.compendio) labels.push({label: 'COMPENDIO', valore: 'true'})
  // visibilità item (vuoto = visibile a tutti)
  if (form.visibilita) labels.push({label: 'VISIBILITA', valore: form.visibilita})
  // Incantesimi (item generico): stesse label SPELL_<n>* delle classi, slot/conosciuti a riga fissa.
  // Sezione personalizzata: niente liste, incantesimi agganciati direttamente uno a uno.
  if (canHaveSpells.value) {
    let n = 0
    for (const s of form.sezioniIncantesimi) {
      const liste = (s.liste ?? []).map(x => x.trim()).filter(Boolean)
      const custom = s.personalizzata ? (s.incantesimiCustom ?? []).filter(c => c.id) : []
      if (!s.personalizzata && liste.length === 0) continue
      if (s.personalizzata && custom.length === 0) continue
      if (liste.length > 0) labels.push({label: `SPELL_${n}`, valore: liste.join(',')})
      if (s.bonus.trim()) labels.push({label: `SPELL_${n}_BONUS`, valore: s.bonus.trim()})
      if (s.slot.trim()) labels.push({label: `SPELL_${n}_SLOT`, valore: s.slot.trim()})
      if (s.conosciutiSeparati) {
        labels.push({label: `SPELL_${n}_HA_CONOSCIUTI`, valore: '1'})
        if (s.conosciuti.trim()) labels.push({label: `SPELL_${n}_CONOSCIUTI`, valore: s.conosciuti.trim()})
      }
      if (s.personalizzata) {
        labels.push({label: `SPELL_${n}_CUSTOM`, valore: '1'})
        labels.push({label: `SPELL_${n}_INCANTESIMI`, valore: custom.map(c => `${c.id}:${c.livello}`).join(',')})
      }
      n++
    }
  }
  return toRaw({
    nome: form.nome.trim(),
    descrizione: form.descrizione,
    tipo: props.mode === 'create' ? props.item.tipo : undefined,
    idPersonaggio: props.mode === 'create' ? props.idPersonaggio : undefined,
    idMondo: form.idMondo ?? undefined,
    idSistema: form.idSistema ?? undefined,
    // creazione "al volo" di un figlio: tieni mondo/sistema ma non agganciare al FromCompendio
    skipFromCompendio: isLinkCreate.value ? true : undefined,
    // QUEST radice (non sotto-quest): ambito a cui associare la quest
    questScope: (props.mode === 'create' && isQuest.value && !isLinkCreate.value && form.questScope) ? form.questScope : undefined,
    idParty: (props.mode === 'create' && isQuest.value && !isLinkCreate.value) ? props.idParty : undefined,
    labels,
    modificatori: form.modificatori.filter(m => m.statId.trim()),
    attacchi: form.attacchi.filter(a => a.nome.trim()),
    children: [...form.children, ...form.forme, ...form.effetti].map(c => ({id: c.id, qty: c.qty ?? null, formulaQty: c.formulaQty ?? null, scelta: c.scelta ?? null, nascosto: c.nascosto ?? false, condizione: c.condizione ?? null})),
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
  form.campi = Object.fromEntries(props.campiLabel.filter(c => !c.multiValore).map(c => [c.key, '']))
  form.campiMulti = Object.fromEntries(props.campiLabel.filter(c => c.multiValore).map(c => [c.key, []]))
  form.descrOggetto = {magico: false, psionico: false, divino: false, leggendario: false, unico: false}
  form.infoOggetto = {taglia: '', costo: '', materiale: '', prefisso: ''}
  form.infoVeicolo = {velocita: ''}
  form.descrAbilita = {straordinaria: false, magica: false, soprannaturale: false, naturale: false, divina: false}
  form.labels = []
  form.modificatori = []
  form.attacchi = []
  form.children = []
  form.forme = []
  form.effetti = []
  form.note = []
  form.qta = 1
  form.completata = false
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
      <label v-if="!minimal" class="field utilizzi-field">
        <span class="lbl">Utilizzi max</span>
        <input v-model.number="form.utilizzi" type="number" min="1" step="1" inputmode="numeric"
               :disabled="disabledAll"/>
      </label>
    </div>

    <div v-if="!minimal" class="row two">
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

    <!-- descrizione anticipata in modalità minimal (non per QUEST: niente descrizione) -->
    <label v-if="minimal && !isQuest" class="field">
      <span class="lbl">Descrizione</span>
      <HtmlEditor v-model="form.descrizione" :rows="10" :disabled="disabledAll"/>
    </label>

    <!-- Descrittori Oggetto (validi per qualunque item) -->
    <section v-if="!minimal" class="fold">
      <button type="button" class="fold-head" @click="open.descrOggetto = !open.descrOggetto"
              :aria-expanded="open.descrOggetto ? 'true' : 'false'">
        <span class="fold-title">Descrittori Oggetto</span>
        <span class="fold-summary">{{ sumDescrOggetto }}</span>
        <span class="chev" :class="{ open: open.descrOggetto }">▸</span>
      </button>
      <div v-show="open.descrOggetto" class="fold-body">
        <div class="row two">
          <label class="field field-checkbox">
            <span class="lbl">Magico</span>
            <input type="checkbox" v-model="form.descrOggetto.magico" :disabled="disabledAll"/>
          </label>
          <label class="field field-checkbox">
            <span class="lbl">Psionico</span>
            <input type="checkbox" v-model="form.descrOggetto.psionico" :disabled="disabledAll"/>
          </label>
          <label class="field field-checkbox">
            <span class="lbl">Divino</span>
            <input type="checkbox" v-model="form.descrOggetto.divino" :disabled="disabledAll"/>
          </label>
          <label class="field field-checkbox">
            <span class="lbl">Leggendario</span>
            <input type="checkbox" v-model="form.descrOggetto.leggendario" :disabled="disabledAll"/>
          </label>
          <label class="field field-checkbox">
            <span class="lbl">Unico</span>
            <input type="checkbox" v-model="form.descrOggetto.unico" :disabled="disabledAll"/>
          </label>
        </div>
      </div>
    </section>

    <!-- Info Oggetto: taglia fisica dell'oggetto (diversa dalla taglia del personaggio), costo, materiale -->
    <section v-if="!minimal" class="fold">
      <button type="button" class="fold-head" @click="open.infoOggetto = !open.infoOggetto"
              :aria-expanded="open.infoOggetto ? 'true' : 'false'">
        <span class="fold-title">Info Oggetto</span>
        <span class="fold-summary">{{ sumInfoOggetto }}</span>
        <span class="chev" :class="{ open: open.infoOggetto }">▸</span>
      </button>
      <div v-show="open.infoOggetto" class="fold-body">
        <div class="row two">
          <label class="field">
            <span class="lbl">Taglia</span>
            <SearchSelect v-model="form.infoOggetto.taglia" :options="TAGLIE_OGGETTO" placeholder="— nessuna —" :disabled="disabledAll" :sort="false"/>
          </label>
          <label class="field">
            <span class="lbl">Costo</span>
            <input v-model.trim="form.infoOggetto.costo" type="text" :disabled="disabledAll" placeholder="es. 150 mo"/>
          </label>
          <label class="field">
            <span class="lbl">Materiale</span>
            <input v-model.trim="form.infoOggetto.materiale" type="text" :disabled="disabledAll" placeholder="es. Acciaio, mithral"/>
          </label>
          <label class="field">
            <span class="lbl">Prefisso oggetti</span>
            <input v-model.trim="form.infoOggetto.prefisso" type="text" :disabled="disabledAll" placeholder="es. Freccia"/>
          </label>
        </div>
      </div>
    </section>

    <!-- Info Veicolo (solo tipo VEICOLO) -->
    <section v-if="isVeicolo" class="fold">
      <button type="button" class="fold-head" @click="open.infoVeicolo = !open.infoVeicolo"
              :aria-expanded="open.infoVeicolo ? 'true' : 'false'">
        <span class="fold-title">Info Veicolo</span>
        <span class="fold-summary">{{ sumInfoVeicolo }}</span>
        <span class="chev" :class="{ open: open.infoVeicolo }">▸</span>
      </button>
      <div v-show="open.infoVeicolo" class="fold-body">
        <label class="field">
          <span class="lbl">Velocità</span>
          <input v-model.trim="form.infoVeicolo.velocita" type="text" :disabled="disabledAll" placeholder="es. 12 m"/>
        </label>
      </div>
    </section>

    <!-- Descrittori Abilità (validi per qualunque item, possono essere attivi insieme) -->
    <section v-if="!minimal" class="fold">
      <button type="button" class="fold-head" @click="open.descrAbilita = !open.descrAbilita"
              :aria-expanded="open.descrAbilita ? 'true' : 'false'">
        <span class="fold-title">Descrittori Abilità</span>
        <span class="fold-summary">{{ sumDescrAbilita }}</span>
        <span class="chev" :class="{ open: open.descrAbilita }">▸</span>
      </button>
      <div v-show="open.descrAbilita" class="fold-body">
        <div class="row two">
          <label class="field field-checkbox">
            <span class="lbl">Straordinaria (Str)</span>
            <input type="checkbox" v-model="form.descrAbilita.straordinaria" :disabled="disabledAll"/>
          </label>
          <label class="field field-checkbox">
            <span class="lbl">Magica (Mag)</span>
            <input type="checkbox" v-model="form.descrAbilita.magica" :disabled="disabledAll"/>
          </label>
          <label class="field field-checkbox">
            <span class="lbl">Soprannaturale (Sop)</span>
            <input type="checkbox" v-model="form.descrAbilita.soprannaturale" :disabled="disabledAll"/>
          </label>
          <label class="field field-checkbox">
            <span class="lbl">Naturale</span>
            <input type="checkbox" v-model="form.descrAbilita.naturale" :disabled="disabledAll"/>
          </label>
          <label class="field field-checkbox">
            <span class="lbl">Divina</span>
            <input type="checkbox" v-model="form.descrAbilita.divina" :disabled="disabledAll"/>
          </label>
        </div>
      </div>
    </section>

    <!-- campi specifici per tipo -->
    <template v-if="campiLabel.length">
      <section v-if="campiLabelTitolo" class="fold">
        <button type="button" class="fold-head" @click="open.campiLabel = !open.campiLabel"
                :aria-expanded="open.campiLabel ? 'true' : 'false'">
          <span class="fold-title">{{ campiLabelTitolo }}</span>
          <span class="fold-summary">{{ sumCampiLabel }}</span>
          <span class="chev" :class="{ open: open.campiLabel }">▸</span>
        </button>
        <div v-show="open.campiLabel" class="fold-body">
          <div class="row two">
            <label v-for="c in campiLabel" :key="c.key" class="field"
                   :class="{ full: c.textarea || c.multiValore, 'field-checkbox': c.tipo === 'checkbox' }">
              <template v-if="c.multiValore">
                <span class="lbl">{{ c.label }}</span>
                <MultiValueField v-model="form.campiMulti[c.key]" :textarea="c.textarea" :html="c.html"
                                  :disabled="disabledAll" :placeholder="c.placeholder"/>
              </template>
              <template v-else-if="c.tipo === 'checkbox'">
                <span class="lbl">{{ c.label }}</span>
                <input type="checkbox"
                       :checked="form.campi[c.key] === '1'"
                       :disabled="disabledAll"
                       @change="(e) => form.campi[c.key] = (e.target as HTMLInputElement).checked ? '1' : ''"/>
              </template>
              <template v-else>
                <span class="lbl">{{ c.label }}</span>
                <textarea v-if="c.textarea" v-model="form.campi[c.key]" rows="3"
                          :disabled="disabledAll" :placeholder="c.placeholder"/>
                <input v-else v-model="form.campi[c.key]"
                       :type="c.tipo === 'datetime-local' ? 'datetime-local' : 'text'"
                       :disabled="disabledAll" :placeholder="c.placeholder"/>
              </template>
            </label>
          </div>
        </div>
      </section>
      <div v-else class="row two">
        <label v-for="c in campiLabel" :key="c.key" class="field"
               :class="{ full: c.textarea || c.multiValore, 'field-checkbox': c.tipo === 'checkbox' }">
          <template v-if="c.multiValore">
            <span class="lbl">{{ c.label }}</span>
            <MultiValueField v-model="form.campiMulti[c.key]" :textarea="c.textarea"
                              :disabled="disabledAll" :placeholder="c.placeholder"/>
          </template>
          <template v-else-if="c.tipo === 'checkbox'">
            <span class="lbl">{{ c.label }}</span>
            <input type="checkbox"
                   :checked="form.campi[c.key] === '1'"
                   :disabled="disabledAll"
                   @change="(e) => form.campi[c.key] = (e.target as HTMLInputElement).checked ? '1' : ''"/>
          </template>
          <template v-else>
            <span class="lbl">{{ c.label }}</span>
            <textarea v-if="c.textarea" v-model="form.campi[c.key]" rows="3"
                      :disabled="disabledAll" :placeholder="c.placeholder"/>
            <input v-else v-model.trim="form.campi[c.key]" type="text"
                   :disabled="disabledAll" :placeholder="c.placeholder"/>
          </template>
        </label>
      </div>
    </template>

    <!-- slot per estensioni specifiche del tipo -->
    <slot name="specifico" :disabled="disabledAll"
          :quest-scope="form.questScope" :set-quest-scope="(v: string) => form.questScope = v"
          :completata="form.completata" :set-completata="(v: boolean) => form.completata = v"/>

    <!-- mondo / sistema + visibilità: in non-minimal restano qui, in minimal vanno in fondo -->
    <template v-if="!minimal">
      <div class="row two">
        <label class="field">
          <span class="lbl">Mondo</span>
          <SearchSelect v-model="form.idMondo" :options="mondoOptions" placeholder="— nessuno —" :disabled="disabledAll" :sort="false"/>
        </label>
        <label class="field">
          <span class="lbl">Sistema</span>
          <SearchSelect v-model="form.idSistema" :options="sistemaOptions" placeholder="— nessuno —" :disabled="disabledAll" :sort="false"/>
        </label>
      </div>
      <label class="compendio-flag">
        <input type="checkbox" v-model="form.compendio" :disabled="disabledAll"/>
        <span>Visibile nel compendio</span>
      </label>
      <label class="field">
        <span class="lbl">Visibilità</span>
        <SearchSelect v-model="form.visibilita" :disabled="disabledAll" :sort="false"
                      :options="[{value:'',label:'Tutti'},{value:'OWNER',label:'Proprietario'},{value:'MASTER',label:'Master'}]"/>
      </label>
    </template>

    <!-- Attacchi (item ATTACCO figli) -->
    <section v-if="!minimal" class="fold">
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

    <!-- Item collegati (child) — per una QUEST, solo altre QUEST (sotto-quest): resta visibile anche in minimal -->
    <section v-if="!minimal || isQuest" class="fold">
      <button type="button" class="fold-head" @click="open.children = !open.children"
              :aria-expanded="open.children ? 'true' : 'false'">
        <span class="fold-title">{{ isQuest ? 'Sotto-quest' : 'Item collegati' }}</span>
        <span class="fold-summary">{{ sumChildren }}</span>
        <span class="chev" :class="{ open: open.children }">▸</span>
      </button>
      <div v-show="open.children" class="fold-body">
        <SottoQuestEditor v-if="isQuest" v-model="form.children" :disabled="disabledAll"
                          :id-personaggio="props.idPersonaggio" :id-party="props.idParty"/>
        <ChildrenEditor v-else v-model="form.children" :disabled="disabledAll" :exclude-id="props.item.id"
                        :exclude-tipo="separateForme ? 'FORMA' : undefined"
                        @create-new="(t, n) => onCreateChild('children', t, n)"/>
      </div>
    </section>

    <!-- Effetti: sezione dedicata e slegata dagli item collegati generici -->
    <section v-if="!minimal" class="fold">
      <button type="button" class="fold-head" @click="open.effetti = !open.effetti"
              :aria-expanded="open.effetti ? 'true' : 'false'">
        <span class="fold-title">Effetti</span>
        <span class="fold-summary">{{ sumEffetti }}</span>
        <span class="chev" :class="{ open: open.effetti }">▸</span>
      </button>
      <div v-show="open.effetti" class="fold-body">
        <EffettiEditor v-model="form.effetti" :disabled="disabledAll" :id-personaggio="props.idPersonaggio"/>
      </div>
    </section>

    <!-- Note: generiche, disponibili su qualunque tipo di item, ognuna con una propria visibilità -->
    <section class="fold">
      <button type="button" class="fold-head" @click="open.note = !open.note"
              :aria-expanded="open.note ? 'true' : 'false'">
        <span class="fold-title">Note</span>
        <span class="fold-summary">{{ sumNote }}</span>
        <span class="chev" :class="{ open: open.note }">▸</span>
      </button>
      <div v-show="open.note" class="fold-body">
        <NotesEditor v-model="form.note" :disabled="disabledAll"/>
      </div>
    </section>

    <!-- Incantesimi (item generico): liste + slot/conosciuti a valore fisso, niente progressione -->
    <section v-if="!minimal && canHaveSpells" class="fold">
      <button type="button" class="fold-head" @click="open.incantesimi = !open.incantesimi"
              :aria-expanded="open.incantesimi ? 'true' : 'false'">
        <span class="fold-title">Incantesimi</span>
        <span class="fold-summary">{{ sumSezioniIncantesimi }}</span>
        <span class="chev" :class="{ open: open.incantesimi }">▸</span>
      </button>
      <div v-show="open.incantesimi" class="fold-body">
        <p class="muted">
          Slot/conosciuti sono un numero fisso (l'oggetto non ha livelli), sempre disponibile.
          Per tenere liste separate, crea più sezioni.
        </p>

        <div v-for="(s, i) in form.sezioniIncantesimi" :key="i" class="sez-card">
          <div class="sez-head">
            <span class="sez-title">Sezione {{ i + 1 }}</span>
            <button type="button" class="btn-del" :disabled="disabledAll" @click="removeSezioneIncantesimi(i)" title="Rimuovi">✕</button>
          </div>

          <label class="field checkbox-field">
            <input type="checkbox" v-model="s.personalizzata" :disabled="disabledAll"/>
            <span class="lbl">Sezione personalizzata (cerca e aggiungi incantesimi uno a uno, invece di una lista standard)</span>
          </label>

          <div v-if="!s.personalizzata" class="field">
            <span class="lbl">Liste incantesimi (unite in questa sezione)</span>
            <div v-if="s.liste.length" class="chips">
              <span v-for="code in s.liste" :key="code" class="chip">
                {{ spellListLabel(code) }}
                <button type="button" class="chip-x" :disabled="disabledAll" @click="removeListaIncantesimi(s, code)">✕</button>
              </span>
            </div>
            <SearchSelect :model-value="''" :disabled="disabledAll" placeholder="+ Aggiungi lista…"
                          :options="listeIncantesimiDisponibili(s).map(c => ({value: c, label: `${spellListLabel(c)} (${c})`}))"
                          @update:model-value="addListaIncantesimi(s, $event as string)"/>
            <div class="custom-lista-row">
              <input v-model.trim="customListaCode[i]" type="text" placeholder="Codice personalizzato, es. SP_ANELLO_CUSTOM"
                     :disabled="disabledAll" @keydown.enter.prevent="confirmCustomLista(s, i)"/>
              <button type="button" class="btn ghost" :disabled="disabledAll || !customListaCode[i]?.trim()"
                      @click="confirmCustomLista(s, i)">Aggiungi</button>
            </div>
          </div>

          <div v-else class="field">
            <span class="lbl">Incantesimi di questa sezione</span>
            <div v-if="s.incantesimiCustom.length" class="chips">
              <span v-for="(c, ci) in s.incantesimiCustom" :key="c.id" class="chip">
                {{ c.nome }} (liv. {{ c.livello }})
                <button type="button" class="chip-x" :disabled="disabledAll" @click="rimuoviIncantesimoCustom(s, ci)">✕</button>
              </span>
            </div>
            <div class="conc-add">
              <label class="field liv-input">
                <span class="lbl">Liv</span>
                <input v-model.number="livelloIncCustom[i]" type="number" min="0" max="9" :disabled="disabledAll"/>
              </label>
              <input class="grow" :value="queryIncCustom[i]" type="text" placeholder="Cerca incantesimo…"
                     :disabled="disabledAll" @input="queryIncCustom[i] = ($event.target as HTMLInputElement).value; onQueryIncCustom(i)"/>
            </div>
            <div v-if="searchingIncCustom[i]" class="muted">Ricerca…</div>
            <ul v-else-if="risultatiIncCustom[i]?.length" class="results">
              <li v-for="r in risultatiIncCustom[i]" :key="r.id">
                <button type="button" class="result" :disabled="disabledAll" @click="aggiungiIncantesimoCustom(s, i, r)">
                  <span class="nome">{{ r.nome }}</span>
                  <span class="plus">+</span>
                </button>
              </li>
            </ul>
          </div>

          <div class="rank-grid">
            <label class="field">
              <span class="lbl">Slot per livello — formato "2,1,-,-,-,-,-,-,-,-" dal liv. 0 al 9 ("-" = nessun accesso)</span>
              <input v-model.trim="s.slot" type="text" placeholder="2,1,-,-,-,-,-,-,-,-" :disabled="disabledAll"/>
            </label>
            <label class="field">
              <span class="lbl">Formula slot bonus</span>
              <input v-model.trim="s.bonus" type="text" placeholder="Es.: 1+(@SAG-#L)/4)" :disabled="disabledAll"/>
            </label>
          </div>

          <label class="field checkbox-field">
            <input type="checkbox" v-model="s.conosciutiSeparati" :disabled="disabledAll"/>
            <span class="lbl">Traccia incantesimi conosciuti separatamente dagli slot</span>
          </label>
          <div v-if="s.conosciutiSeparati" class="field">
            <span class="lbl">Incantesimi conosciuti — stesso formato degli slot</span>
            <input v-model.trim="s.conosciuti" type="text" placeholder="2,1,-,-,-,-,-,-,-,-" :disabled="disabledAll"/>
          </div>
        </div>

        <button type="button" class="btn outline" :disabled="disabledAll" @click="addSezioneIncantesimi">+ Aggiungi sezione</button>
      </div>
    </section>

    <!-- Labels generiche -->
    <section v-if="!minimal" class="fold">
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
    <section v-if="!minimal" class="fold">
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

    <label v-if="!minimal && !isQuest" class="field">
      <span class="lbl">Descrizione</span>
      <HtmlEditor v-model="form.descrizione" :rows="10" :disabled="disabledAll"/>
    </label>

    <!-- in minimal: mondo/sistema e visibilità in fondo -->
    <template v-if="minimal">
      <div class="row two">
        <label class="field">
          <span class="lbl">Mondo</span>
          <SearchSelect v-model="form.idMondo" :options="mondoOptions" placeholder="— nessuno —" :disabled="disabledAll" :sort="false"/>
        </label>
        <label class="field">
          <span class="lbl">Sistema</span>
          <SearchSelect v-model="form.idSistema" :options="sistemaOptions" placeholder="— nessuno —" :disabled="disabledAll" :sort="false"/>
        </label>
      </div>
      <label class="field">
        <span class="lbl">Visibilità</span>
        <SearchSelect v-model="form.visibilita" :disabled="disabledAll" :sort="false"
                      :options="[{value:'',label:'Tutti'},{value:'OWNER',label:'Proprietario'},{value:'MASTER',label:'Master'}]"/>
      </label>
    </template>

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

input[type="text"], input[type="number"], input[type="datetime-local"], textarea, select {
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
.btn-del {
  border: 1px solid #fecaca; background: #fef2f2; color: #991b1b;
  border-radius: .5rem; padding: .25rem .5rem; cursor: pointer;
}

.rank-grid { display: grid; grid-template-columns: 1fr 1fr; gap: .5rem; }
@media (max-width: 700px) { .rank-grid { grid-template-columns: 1fr; } }

.sez-card { border: 1px solid #e5e7eb; border-radius: .5rem; padding: .5rem; display: grid; gap: .5rem; margin-bottom: .4rem; background: #fafafa; }
.sez-head { display: flex; align-items: center; justify-content: space-between; }
.sez-title { font-weight: 700; font-size: .9rem; }
.chips { display: flex; flex-wrap: wrap; gap: .3rem; margin-bottom: .3rem; }
.chip { display: inline-flex; align-items: center; gap: .3rem; background: #eef2ff; color: #3730a3; border-radius: 1rem; padding: .1rem .5rem; font-size: .8rem; font-weight: 600; }
.chip-x { border: 0; background: transparent; color: #6366f1; cursor: pointer; font-size: .75rem; padding: 0; }
.custom-lista-row { display: flex; gap: .4rem; margin-top: .35rem; }
.custom-lista-row input { flex: 1; padding: .4rem .5rem; border: 1px solid #d0d5dd; border-radius: .4rem; }

.conc-add { display: grid; grid-template-columns: 5rem 1fr; gap: .4rem; align-items: end; }
.conc-add .grow { min-width: 0; padding: .5rem .6rem; border: 1px solid #d0d5dd; border-radius: .5rem; }
.liv-input { max-width: 6rem; }
.results {
  list-style: none; margin: 0; padding: 0;
  border: 1px solid #e5e7eb; border-radius: .5rem; overflow: hidden;
  max-height: 14rem; overflow-y: auto;
}
.results li + li { border-top: 1px solid #f3f4f6; }
.result {
  width: 100%; display: grid; grid-template-columns: 1fr auto; gap: .4rem; align-items: center;
  padding: .4rem .5rem; background: #fff; border: 0; cursor: pointer; text-align: left;
}
.result:hover { background: #f9fafb; }
.result .nome { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.plus { color: #2563eb; font-weight: 700; }
.checkbox-field { grid-auto-flow: column; justify-content: start; align-items: center; gap: .5rem; }
.checkbox-field input[type="checkbox"] { width: auto; }

/* nome + quantità */
.nome-qta { display: grid; grid-template-columns: 1fr auto; gap: .5rem; align-items: end; }
.field.grow { min-width: 0; }
.qta-field { width: 9rem; }
.utilizzi-field { width: 8rem; }
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
.field-checkbox { flex-direction: row; align-items: center; gap: .5rem; }
.field-checkbox input[type="checkbox"] { width: 1.1rem; height: 1.1rem; cursor: pointer; }
.section-card {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: .5rem;
  overflow: hidden;
}
.section-card-header {
  background: var(--color-surface-2, #f3f4f6);
  padding: .4rem .75rem;
  font-size: .78rem;
  font-weight: 600;
  color: var(--color-text-secondary, #6b7280);
  text-transform: uppercase;
  letter-spacing: .04em;
}
.section-card .row { padding: .75rem; }
</style>
