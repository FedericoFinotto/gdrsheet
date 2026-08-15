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
import VisibilitaPicker from '../../../../../../components/VisibilitaPicker.vue'
import LabelsEditor from './Sections/LabelsEditor.vue'
import Icona from '../../../../../../components/Icona/Icona.vue'
import TagEditor from './Sections/TagEditor.vue'
import TriggerRandomizzatoriEditor from './Sections/TriggerRandomizzatoriEditor.vue'
import ImmaginiEditor from './Sections/ImmaginiEditor.vue'
import MultiSelectField from './Sections/MultiSelectField.vue'
import {getTagsItem, ItemTag, setTagsItem} from '../../../../../../service/RandomizzatoreService'
import MultiValueField from './Sections/MultiValueField.vue'
import ModificatoriEditor from './Sections/ModificatoriEditor.vue'
import AttacchiEditor from './Sections/AttacchiEditor.vue'
import ChildrenEditor from './Sections/ChildrenEditor.vue'
import EffettiEditor from './Sections/EffettiEditor.vue'
import SottoQuestEditor from './Sections/SottoQuestEditor.vue'
import NotesEditor, {NotaRow} from './Sections/NotesEditor.vue'
import {SPELL_LIST_CODES, spellListLabel} from '../../../../../../function/spellLists'
import {TAGLIE_OPTIONS_NOME} from '../../../../../../function/Utils'

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
  forceImmagini?: boolean     // mostra comunque il fold Immagini anche in minimal (es. ImmagineEditor.vue)
}>(), {
  titolo: 'Item',
  campiLabel: () => [],
  suggestedKeys: () => [],
  mode: 'edit',
})

const emit = defineEmits<{
  (e: 'saved'): void
  (e: 'cancel'): void
  (e: 'savedStay'): void
  /** salvato restando nell'editor; porta l'item salvato perché in creazione il contenitore
   *  deve passare alla modifica di quello appena creato (vedi onSalvaResta) */
  (e: 'savedResta', item: ItemDB): void
}>()

// tipi con gestione quantità (label QTA, moltiplica il peso)
const TIPI_CON_QTA = ['OGGETTO', 'CONSUMABILE', 'ARMA', 'MUNIZIONE', 'EQUIPAGGIAMENTO']
const showQta = computed(() => TIPI_CON_QTA.includes(props.item.tipo))

// QUEST: albero di sotto-quest (child collegati di tipo QUEST). L'ambito (a chi è associata
// la quest RADICE: personaggio/party/mondo) si sceglie solo in creazione e solo per la radice
// (le sotto-quest, create dal flusso "crea e collega", non hanno un ambito proprio).
const isQuest = computed(() => props.item.tipo === 'QUEST')
// INFO: stesso albero/ambito/archiviazione della QUEST (vedi sopra), ma senza completamento né
// "in carico" — il contenuto è la sezione Note qui sotto (già generica, N note con visibilità
// ciascuna diversa). isAmbito raggruppa le sezioni condivise da entrambi i tipi.
const isInfo = computed(() => props.item.tipo === 'INFO')
const isAmbito = computed(() => isQuest.value || isInfo.value)
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
  descrizioneEn: string
  campi: Record<string, string>
  campiMulti: Record<string, string[]>
  descrOggetto: { magico: boolean; psionico: boolean; divino: boolean; leggendario: boolean; unico: boolean }
  infoOggetto: { taglia: string; costo: string; materiale: string; prefisso: string }
  infoVeicolo: { velocita: string }
  descrAbilita: { straordinaria: boolean; magica: boolean; soprannaturale: boolean; naturale: boolean; divina: boolean }
  randTrigger: string[]
  labels: LabelRow[]
  modificatori: ModificatoreRow[]
  attacchi: AttaccoRow[]
  children: ChildRef[]
  forme: ChildRef[]
  effetti: ChildRef[]
  sezioniIncantesimi: Array<{
    liste: string[]; bonus: string; slot: string; conosciutiSeparati: boolean; conosciuti: string
    slotConContatore: boolean
    personalizzata: boolean; incantesimiCustom: Array<{ id: number; nome: string; livello: number }>
  }>
  aggiunteClasse: Array<{
    idClasse: number | null; nomeClasse: string; valore: string
    livello: boolean; items: boolean; bonus: boolean; spell: boolean
  }>
  note: NotaRow[]
  inCarico: string[]
  qta: number
  compendio: boolean
  visibilita: string
  idMondo: number
  idSistema: number
  questScope: string
  // INFO radice di ambito PARTY: id del party a cui è associata (label INFO_PARTY). Letto qui
  // (non tra le "labels generiche") perché in modifica serve a pre-selezionare l'Ambito e a
  // preservarlo se l'utente non lo tocca — vedi buildPayload().
  infoPartyId: number | null
  completata: boolean
  archiviata: boolean
}>({
  nome: '',
  enName: '',
  manuale: '',
  descrizione: '',
  descrizioneEn: '',
  campi: Object.fromEntries(props.campiLabel.filter(c => !c.multiValore).map(c => [c.key, ''])),
  campiMulti: Object.fromEntries(props.campiLabel.filter(c => c.multiValore).map(c => [c.key, [] as string[]])),
  descrOggetto: {magico: false, psionico: false, divino: false, leggendario: false, unico: false},
  infoOggetto: {taglia: '', costo: '', materiale: '', prefisso: ''},
  infoVeicolo: {velocita: ''},
  descrAbilita: {straordinaria: false, magica: false, soprannaturale: false, naturale: false, divina: false},
  randTrigger: [],
  labels: [],
  modificatori: [],
  attacchi: [],
  children: [],
  forme: [],
  effetti: [],
  sezioniIncantesimi: [],
  aggiunteClasse: [],
  note: [],
  inCarico: [],
  qta: 1,
  utilizzi: null as number | null,
  compendio: false,
  visibilita: '',
  idMondo: null as number | null,
  idSistema: null as number | null,
  questScope: '',
  infoPartyId: null as number | null,
  completata: false,
  archiviata: false,
})

const open = reactive({
  labels: false, modificatori: false, attacchi: false, children: false, forme: false, effetti: false,
  campiLabel: false, descrOggetto: false, infoOggetto: false, infoVeicolo: false, descrAbilita: false, note: false,
  incantesimi: false, inCarico: false, aggiunteClasse: false, tags: false, randTrigger: false, immagini: false,
})

/* Tag pesati per i randomizzatori: vivono su una tabella a parte (item_tag), non tra le
 * labels, quindi hanno un caricamento e un salvataggio propri.
 * Il salvataggio avviene solo se l'utente li ha davvero toccati: l'endpoint richiede i
 * permessi di master del mondo, e chi non usa questa sezione non deve inciamparci. */
const tags = ref<ItemTag[]>([])
const tagsModificati = ref(false)

// Switch bandierina per editare descrizione IT/EN nello stesso riquadro (vedi DESCRIZIONE_EN
// e il toggle analogo in Mobile_DettaglioItem.vue).
const mostraDescrizioneEn = ref(false)

function onTagsChange(v: ItemTag[]) {
  tags.value = v
  tagsModificati.value = true
}

async function caricaTags() {
  tags.value = []
  tagsModificati.value = false
  if (props.mode !== 'edit' || !props.item?.id) return
  try {
    tags.value = (await getTagsItem(props.item.id)).data
  } catch (e) {
    console.error('Errore caricamento tag item:', e)
  }
}

const sumTags = computed(() =>
    tags.value.filter(t => t.idTag != null).map(t => t.tag ?? `#${t.idTag}`).join(', ') || '—')

// Taglia fisica dell'oggetto (es. arma taglia Grande): puramente descrittiva, non modifica
// la taglia del personaggio (a differenza del campo TAGLIA usato altrove per quello scopo).
const TAGLIE_OGGETTO = [{value: '', label: '— nessuna —'}, ...TAGLIE_OPTIONS_NOME]

async function preload() {
  form.nome = props.item.nome ?? ''
  form.descrizione = props.item.descrizione ?? ''
  caricaTags()

  const campoKeys = new Set(props.campiLabel.filter(c => !c.multiValore).map(c => c.key))
  const campoKeysMulti = new Set(props.campiLabel.filter(c => c.multiValore).map(c => c.key))
  form.campi = Object.fromEntries(props.campiLabel.filter(c => !c.multiValore).map(c => [c.key, '']))
  form.campiMulti = Object.fromEntries(props.campiLabel.filter(c => c.multiValore).map(c => [c.key, []]))

  // QTA: preferisce quantita già calcolata (da inventario personaggio), poi labels globali (compendio)
  form.qta = (showQta.value && props.item.quantita != null) ? props.item.quantita : 1
  form.utilizzi = null
  form.enName = ''
  form.manuale = ''
  form.descrizioneEn = ''
  mostraDescrizioneEn.value = false
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
  form.randTrigger = []
  form.note = []
  form.inCarico = []
  form.infoPartyId = null
  form.completata = false
  form.archiviata = false
  form.sezioniIncantesimi = []
  form.aggiunteClasse = []
  // righe SPELL_<n>* raccolte durante il giro delle label, riassemblate in sezioni a fine ciclo
  // (l'ordine delle label non è garantito, es. _SLOT potrebbe comparire prima di SPELL_<n> stesso)
  const spellRaw: Record<number, {
    liste?: string; bonus?: string; slot?: string; haConosciuti?: string; conosciuti?: string
    personalizzata?: string; incantesimi?: string
  }> = {}
  // righe ADD_CLASSE_<n>* raccolte allo stesso modo (stesso motivo: ordine label non garantito)
  const addClasseRaw: Record<number, {
    idClasse?: string; valore?: string; livello?: string; items?: string; bonus?: string; spell?: string
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
    } else if (key === 'DESCRIZIONE_EN') {
      form.descrizioneEn = val
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
    } else if (key === 'RAND_TRIGGER') {
      form.randTrigger.push(val)
    } else if (key === 'DESCR_NAT') {
      form.descrAbilita.naturale = val === '1'
    } else if (key === 'DESCR_DIV') {
      form.descrAbilita.divina = val === '1'
    } else if (key === 'QUEST_COMPLETATA') {
      form.completata = val === '1'
    } else if (key === 'QUEST_ARCHIVIATA' || key === 'INFO_ARCHIVIATA') {
      form.archiviata = val === '1'
    } else if (key === 'NOTA') {
      try {
        const parsed = JSON.parse(val)
        form.note.push({testo: String(parsed.testo ?? ''), visibilita: String(parsed.visibilita ?? '')})
      } catch {
        // valore non JSON (dato legacy o corrotto): ignora la nota
      }
    } else if (key === 'IN_CARICO') {
      form.inCarico.push(val)
    } else if (key === 'INFO_PARTY') {
      form.infoPartyId = Number(val) || null
    } else if (campoKeysMulti.has(key)) {
      form.campiMulti[key].push(val)
    } else if (campoKeys.has(key) && !form.campi[key]) {
      form.campi[key] = val
    } else if (/^SPELL_\d+(_PROG|_BONUS|_SLOT|_HA_CONOSCIUTI|_CONOSCIUTI|_SLOT_CONTATORE|_CUSTOM|_INCANTESIMI)?$/.test(key)) {
      const m = key.match(/^SPELL_(\d+)(_PROG|_BONUS|_SLOT|_HA_CONOSCIUTI|_CONOSCIUTI|_SLOT_CONTATORE|_CUSTOM|_INCANTESIMI)?$/)!
      const n = Number(m[1])
      const suffix = m[2] ?? ''
      const row = (spellRaw[n] ??= {})
      if (suffix === '') row.liste = val
      else if (suffix === '_BONUS') row.bonus = val
      else if (suffix === '_SLOT') row.slot = val
      else if (suffix === '_HA_CONOSCIUTI') row.haConosciuti = val
      else if (suffix === '_CONOSCIUTI') row.conosciuti = val
      else if (suffix === '_SLOT_CONTATORE') row.slotConContatore = val
      else if (suffix === '_CUSTOM') row.personalizzata = val
      else if (suffix === '_INCANTESIMI') row.incantesimi = val
      // _PROG: ignorata, gli item non hanno progressione (sempre a slot fisso)
    } else if (/^ADD_CLASSE_\d+(_VALUE|_VALORE|_LIVELLO|_ITEMS|_BONUS|_SPELL)?$/.test(key)) {
      const m = key.match(/^ADD_CLASSE_(\d+)(_VALUE|_VALORE|_LIVELLO|_ITEMS|_BONUS|_SPELL)?$/)!
      const n = Number(m[1])
      const suffix = m[2] ?? ''
      const row = (addClasseRaw[n] ??= {})
      if (suffix === '') row.idClasse = val
      else if (suffix === '_VALUE' || suffix === '_VALORE') row.valore = val
      else if (suffix === '_LIVELLO') row.livello = val
      else if (suffix === '_ITEMS') row.items = val
      else if (suffix === '_BONUS') row.bonus = val
      else if (suffix === '_SPELL') row.spell = val
    } else {
      form.labels.push({label: key, valore: val})
    }
  }
  // INFO in modifica: pre-seleziona l'Ambito attuale (Party se ha INFO_PARTY, altrimenti Mondo) —
  // in creazione resta vuoto, lo decide InfoEditor.vue al primo render (default Party).
  if (isInfo.value && props.mode === 'edit') {
    form.questScope = form.infoPartyId != null ? 'PARTY' : 'MONDO'
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
      slotConContatore: r.slotConContatore === '1',
      personalizzata: r.personalizzata === '1',
      incantesimiCustom,
    }
  })
  // il label salva solo "id:livello": recupera i nomi per mostrarli nell'editor
  await Promise.all(form.sezioniIncantesimi.flatMap(s => s.incantesimiCustom.map(async c => {
    try { c.nome = (await getItem(c.id)).data?.nome ?? c.nome } catch { /* item non trovato: resta il placeholder #id */ }
  })))

  form.aggiunteClasse = Object.keys(addClasseRaw).map(Number).sort((a, b) => a - b)
      .map(n => {
        const r = addClasseRaw[n]
        return {
          idClasse: r.idClasse ? Number(r.idClasse) : null,
          nomeClasse: '',
          valore: r.valore ?? '',
          livello: r.livello === '1',
          items: r.items === '1',
          bonus: r.bonus === '1',
          spell: r.spell === '1',
        }
      })
  // il label salva solo l'id classe: recupera il nome per mostrarlo nella select
  await Promise.all(form.aggiunteClasse.map(async a => {
    if (a.idClasse == null) return
    try { a.nomeClasse = (await getItem(a.idClasse)).data?.nome ?? `#${a.idClasse}` } catch { a.nomeClasse = `#${a.idClasse}` }
  }))

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
    slotConContatore: false,
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

// Aggiunta Classe: N righe ADD_CLASSE_<n>* — livelli virtuali (anche da formula) concessi a una
// classe. Di default NON aggiunge nulla: ogni checkbox è un effetto indipendente e opt-in (vedi
// PersonaggioService.applyAddClasseLevels) — spuntarne una non implica le altre.
function addAggiuntaClasse() {
  form.aggiunteClasse.push({idClasse: null, nomeClasse: '', valore: '', livello: false, items: false, bonus: false, spell: false})
}
function removeAggiuntaClasse(i: number) {
  form.aggiunteClasse.splice(i, 1)
}
async function searchClasseAggiunta(q: string) {
  try {
    const res = await searchItems(q, 'CLASSE')
    return (res.data ?? []).map((c: any) => ({value: c.id, label: c.nome}))
  } catch (e) {
    console.error('Errore ricerca classi:', e)
    return []
  }
}
// SearchSelect (in modalità remota) emette solo l'id scelto, non l'etichetta: la recupero
// con una singola getItem, come già fatto altrove in questo file per gli incantesimi custom.
async function onPickClasseAggiunta(row: { idClasse: number | null; nomeClasse: string }, value: number | string | null) {
  row.idClasse = value == null ? null : Number(value)
  row.nomeClasse = ''
  if (row.idClasse == null) return
  try { row.nomeClasse = (await getItem(row.idClasse)).data?.nome ?? `#${row.idClasse}` } catch { row.nomeClasse = `#${row.idClasse}` }
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
  form.descrizioneEn = snap.descrizioneEn ?? ''
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
  form.aggiunteClasse = snap.aggiunteClasse ?? []
  form.note = snap.note ?? []
  form.inCarico = snap.inCarico ?? []
  form.qta = snap.qta ?? 1
  form.utilizzi = snap.utilizzi ?? null
  form.compendio = !!snap.compendio
  form.visibilita = snap.visibilita ?? ''
  form.idMondo = snap.idMondo ?? null
  form.idSistema = snap.idSistema ?? null
  form.questScope = snap.questScope ?? ''
  form.completata = !!snap.completata
  form.archiviata = !!snap.archiviata
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
const sumAggiunteClasse = computed(() =>
    form.aggiunteClasse.filter(a => a.idClasse).map(a => a.nomeClasse || `#${a.idClasse}`).join(', ') || '—')
const sumEffetti = computed(() =>
    form.effetti.map(c => `${c.condizione ?? 'Sempre'}: ${c.nome}`).join(', ') || '—')
const sumNote = computed(() =>
    form.note.length ? `${form.note.length} nota${form.note.length > 1 ? 'e' : ''}` : '—')
const sumInCarico = computed(() => form.inCarico.filter(v => v.trim()).join(', ') || '—')
function addInCarico() { form.inCarico.push('') }
function removeInCarico(i: number) { form.inCarico.splice(i, 1) }
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
  if (form.descrizioneEn.trim()) labels.push({label: 'DESCRIZIONE_EN', valore: form.descrizioneEn.trim()})
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
  // randomizzatori innescati da questo oggetto (catene)
  for (const idRand of form.randTrigger) {
    if (String(idRand).trim()) labels.push({label: 'RAND_TRIGGER', valore: String(idRand).trim()})
  }
  if (form.descrAbilita.divina) labels.push({label: 'DESCR_DIV', valore: '1'})
  // Quest: stato di completamento (significativo solo per una quest senza sotto-quest)
  if (form.completata) labels.push({label: 'QUEST_COMPLETATA', valore: '1'})
  // Quest/INFO: archiviata (esclusa dal caricamento automatico delle rispettive sezioni) — label
  // separate solo per chiarezza dei dati, il tipo item disambiguerebbe comunque le query.
  if (form.archiviata) labels.push({label: isInfo.value ? 'INFO_ARCHIVIATA' : 'QUEST_ARCHIVIATA', valore: '1'})
  // INFO in modifica: l'Ambito (Party/Mondo) qui è cambiabile anche dopo la creazione, a
  // differenza di come funziona per la radice in creazione (questScope/idParty sotto, letti SOLO
  // da createItem() — updateItem() non li guarda affatto). Party -> riscrive INFO_PARTY col party
  // corrente se non aveva già un proprio party; Mondo -> nessuna label, la rimuove il full-replace.
  if (isInfo.value && props.mode === 'edit') {
    if (form.questScope === 'PARTY') {
      const partyId = form.infoPartyId ?? props.idParty
      if (partyId != null) labels.push({label: 'INFO_PARTY', valore: String(partyId)})
    }
  }
  // Note generiche (qualunque item): ogni riga è un JSON {testo, visibilita}
  for (const n of form.note) {
    if (n.testo.trim()) labels.push({label: 'NOTA', valore: JSON.stringify({testo: n.testo, visibilita: n.visibilita})})
  }
  // Quest: "In carico" (righe di testo libero, es. nome personaggio/party)
  for (const v of form.inCarico) {
    if (v.trim()) labels.push({label: 'IN_CARICO', valore: v.trim()})
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
      if (s.slotConContatore) labels.push({label: `SPELL_${n}_SLOT_CONTATORE`, valore: '1'})
      if (s.personalizzata) {
        labels.push({label: `SPELL_${n}_CUSTOM`, valore: '1'})
        labels.push({label: `SPELL_${n}_INCANTESIMI`, valore: custom.map(c => `${c.id}:${c.livello}`).join(',')})
      }
      n++
    }
  }
  // Aggiunta Classe: livelli virtuali (anche da formula) concessi a una classe, disponibile su
  // qualunque item (nessuna restrizione di tipo, come nel backend).
  {
    let n = 0
    for (const a of form.aggiunteClasse) {
      if (!a.idClasse || !a.valore.trim()) continue
      labels.push({label: `ADD_CLASSE_${n}`, valore: String(a.idClasse)})
      labels.push({label: `ADD_CLASSE_${n}_VALUE`, valore: a.valore.trim()})
      if (a.livello) labels.push({label: `ADD_CLASSE_${n}_LIVELLO`, valore: '1'})
      if (a.items) labels.push({label: `ADD_CLASSE_${n}_ITEMS`, valore: '1'})
      if (a.bonus) labels.push({label: `ADD_CLASSE_${n}_BONUS`, valore: '1'})
      if (a.spell) labels.push({label: `ADD_CLASSE_${n}_SPELL`, valore: '1'})
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
    // QUEST/INFO radice (non sotto-quest/sotto-info): ambito a cui associare la radice
    questScope: (props.mode === 'create' && isAmbito.value && !isLinkCreate.value && form.questScope) ? form.questScope : undefined,
    idParty: (props.mode === 'create' && isAmbito.value && !isLinkCreate.value) ? props.idParty : undefined,
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
    const saved = res.data ?? null
    // i tag stanno su una tabella a parte: si salvano dopo, e solo se toccati (in creazione
    // servirebbe comunque l'id, disponibile appunto solo ora)
    if (saved?.id && tagsModificati.value) {
      await setTagsItem(saved.id, tags.value.filter(t => t.idTag != null && Number(t.peso) > 0))
      tagsModificati.value = false
    }
    return saved
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

/* Salva restando nell'editor (pulsante floppy).
 * In creazione il salvataggio CREA l'item: restare qui senza avvisare il contenitore
 * significherebbe che un secondo click ne crea un altro. Per questo si emette l'item
 * salvato, così chi ci contiene può passare alla modifica di quello appena creato. */
const salvato = ref(false)
let timerSalvato: ReturnType<typeof setTimeout> | null = null

async function onSalvaResta() {
  if (!canSave.value) return
  const item = await doSave()
  if (!item) return
  childCreate.clearDraft()
  emit('savedResta', item)
  salvato.value = true
  if (timerSalvato) clearTimeout(timerSalvato)
  timerSalvato = setTimeout(() => salvato.value = false, 2000)
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
  form.randTrigger = []
  form.labels = []
  form.modificatori = []
  form.attacchi = []
  form.children = []
  form.forme = []
  form.effetti = []
  form.note = []
  form.inCarico = []
  form.qta = 1
  form.completata = false
  form.archiviata = false
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
        <span class="lbl"><span v-if="!minimal" class="fi fi-it lang-flag-inline"></span> Nome</span>
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

    <label v-if="!minimal" class="field">
      <span class="lbl"><span class="fi fi-gb lang-flag-inline"></span> Nome (EN)</span>
      <input v-model.trim="form.enName" type="text" :disabled="disabledAll"
             placeholder="Nome originale in inglese"/>
    </label>

    <div v-if="!minimal" class="row two">
      <label class="field">
        <span class="lbl">Manuale</span>
        <input v-model.trim="form.manuale" type="text" :disabled="disabledAll"
               placeholder="Manuale di provenienza"/>
      </label>
    </div>

    <!-- descrizione anticipata in modalità minimal: comprende anche QUEST/sotto-quest, che sono
         sempre minimal e altrimenti non avrebbero NESSUN modo di modificare la descrizione dopo
         la creazione (l'altro blocco, più sotto, è per !minimal e quindi non le raggiunge mai) -->
    <label v-if="minimal" class="field">
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
            <span class="lbl">Taglia oggetto</span>
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
              <template v-if="c.multiValore && c.options">
                <span class="lbl">{{ c.label }}</span>
                <MultiSelectField v-model="form.campiMulti[c.key]" :options="c.options" :disabled="disabledAll"/>
              </template>
              <template v-else-if="c.multiValore">
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
              <template v-else-if="c.tipo === 'select'">
                <span class="lbl">{{ c.label }}</span>
                <SearchSelect :model-value="form.campi[c.key] || null"
                              :options="[{value: '', label: '—'}, ...(c.options ?? [])]"
                              :disabled="disabledAll" :placeholder="c.placeholder"
                              @update:model-value="(v) => form.campi[c.key] = v == null ? '' : String(v)"/>
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
          <template v-if="c.multiValore && c.options">
            <span class="lbl">{{ c.label }}</span>
            <MultiSelectField v-model="form.campiMulti[c.key]" :options="c.options" :disabled="disabledAll"/>
          </template>
          <template v-else-if="c.multiValore">
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
          <template v-else-if="c.tipo === 'select'">
            <span class="lbl">{{ c.label }}</span>
            <SearchSelect :model-value="form.campi[c.key] || null"
                          :options="[{value: '', label: '—'}, ...(c.options ?? [])]"
                          :disabled="disabledAll" :placeholder="c.placeholder"
                          @update:model-value="(v) => form.campi[c.key] = v == null ? '' : String(v)"/>
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
          :completata="form.completata" :set-completata="(v: boolean) => form.completata = v"
          :archiviata="form.archiviata" :set-archiviata="(v: boolean) => form.archiviata = v"/>

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
        <VisibilitaPicker v-model="form.visibilita" :disabled="disabledAll"/>
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

    <!-- Item collegati (child) — per QUEST/INFO, solo altri item dello stesso tipo (sotto-quest/
         sotto-info): resta visibile anche in minimal -->
    <section v-if="!minimal || isAmbito" class="fold">
      <button type="button" class="fold-head" @click="open.children = !open.children"
              :aria-expanded="open.children ? 'true' : 'false'">
        <span class="fold-title">{{ isQuest ? 'Sotto-quest' : isInfo ? 'Sotto-info' : 'Item collegati' }}</span>
        <span class="fold-summary">{{ sumChildren }}</span>
        <span class="chev" :class="{ open: open.children }">▸</span>
      </button>
      <div v-show="open.children" class="fold-body">
        <SottoQuestEditor v-if="isAmbito" v-model="form.children" :disabled="disabledAll"
                          :tipo="props.item.tipo" :id-personaggio="props.idPersonaggio" :id-party="props.idParty"/>
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

    <!-- In carico: solo QUEST, righe di testo libero (nome personaggio/party) mostrate come chip
         in cima all'accordion della quest/sotto-quest in scheda -->
    <section v-if="isQuest" class="fold">
      <button type="button" class="fold-head" @click="open.inCarico = !open.inCarico"
              :aria-expanded="open.inCarico ? 'true' : 'false'">
        <span class="fold-title">In carico</span>
        <span class="fold-summary">{{ sumInCarico }}</span>
        <span class="chev" :class="{ open: open.inCarico }">▸</span>
      </button>
      <div v-show="open.inCarico" class="fold-body">
        <div v-if="!form.inCarico.length" class="muted">Nessuno.</div>
        <div v-for="(v, i) in form.inCarico" :key="i" class="incarico-row">
          <input type="text" class="incarico-input" :value="v" :disabled="disabledAll"
                 placeholder="Nome personaggio o party"
                 @input="form.inCarico[i] = ($event.target as HTMLInputElement).value"/>
          <button type="button" class="btn-del" :disabled="disabledAll" @click="removeInCarico(i)" title="Rimuovi">✕</button>
        </div>
        <button type="button" class="btn-add" :disabled="disabledAll" @click="addInCarico">+ Aggiungi</button>
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

          <label class="field checkbox-field">
            <input type="checkbox" v-model="s.slotConContatore" :disabled="disabledAll"/>
            <span class="lbl">Traccia gli slot per livello con Contatore</span>
          </label>
        </div>

        <button type="button" class="btn outline" :disabled="disabledAll" @click="addSezioneIncantesimi">+ Aggiungi sezione</button>
      </div>
    </section>

    <!-- Aggiunta Classe: N righe ADD_CLASSE_<n>* — livelli virtuali (anche da formula) concessi a
         una classe del personaggio, con flag opzionali per cosa quei livelli concedono -->
    <section v-if="!minimal" class="fold">
      <button type="button" class="fold-head" @click="open.aggiunteClasse = !open.aggiunteClasse"
              :aria-expanded="open.aggiunteClasse ? 'true' : 'false'">
        <span class="fold-title">Aggiunta Classe</span>
        <span class="fold-summary">{{ sumAggiunteClasse }}</span>
        <span class="chev" :class="{ open: open.aggiunteClasse }">▸</span>
      </button>
      <div v-show="open.aggiunteClasse" class="fold-body">
        <p class="muted">
          Aggiunge livelli virtuali a una classe del personaggio (non persistiti, calcolati a runtime).
          Il valore può essere un numero fisso o una formula con @LIVELLO_NM_/_MNM_/_TOT_/_MAX_/
          _CASTER_NM_/_CASTER_&lt;idClasse&gt; (es. "@LIVELLO_CASTER_NM_2013/2") — sempre sui livelli
          REALI di ogni classe, mai su virtuali aggiunti da un ALTRO ADD_CLASSE nello stesso giro.
          <strong>Di base questa riga non aggiunge nulla</strong>: ogni checkbox è un effetto a sé,
          spuntane solo quelli che vuoi davvero concedere.
        </p>

        <div v-for="(a, i) in form.aggiunteClasse" :key="i" class="sez-card">
          <div class="sez-head">
            <span class="sez-title">Classe {{ i + 1 }}</span>
            <button type="button" class="btn-del" :disabled="disabledAll" @click="removeAggiuntaClasse(i)" title="Rimuovi">✕</button>
          </div>

          <label class="field">
            <span class="lbl">Classe</span>
            <SearchSelect :model-value="a.idClasse" :on-search="searchClasseAggiunta" :selected-label="a.nomeClasse"
                          :disabled="disabledAll" placeholder="Cerca classe…"
                          @update:model-value="onPickClasseAggiunta(a, $event)"/>
          </label>

          <label class="field">
            <span class="lbl">Livelli da aggiungere (numero o formula)</span>
            <input v-model.trim="a.valore" type="text" placeholder="Es.: 2  oppure  @LIVELLO_CASTER_NM_2013/2" :disabled="disabledAll"/>
          </label>

          <label class="field checkbox-field">
            <input type="checkbox" v-model="a.livello" :disabled="disabledAll"/>
            <span class="lbl">Conta come livello ufficiale della classe (lista livelli, variabili @LIVELLO_NM_/_MNM_/_TOT_/_MAX_)</span>
          </label>
          <label class="field checkbox-field">
            <input type="checkbox" v-model="a.items" :disabled="disabledAll"/>
            <span class="lbl">Concede anche i Privilegi di Classe dei livelli virtuali</span>
          </label>
          <label class="field checkbox-field">
            <input type="checkbox" v-model="a.bonus" :disabled="disabledAll"/>
            <span class="lbl">Concede anche BAB/Tempra/Riflessi/Volontà dei livelli virtuali</span>
          </label>
          <label class="field checkbox-field">
            <input type="checkbox" v-model="a.spell" :disabled="disabledAll"/>
            <span class="lbl">Conta per la progressione incantesimi (CL/slot) della classe</span>
          </label>
        </div>

        <button type="button" class="btn outline" :disabled="disabledAll" @click="addAggiuntaClasse">+ Aggiungi classe</button>
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

    <!-- Tag pesati (randomizzatori) -->
    <section v-if="!minimal" class="fold">
      <button type="button" class="fold-head" @click="open.tags = !open.tags"
              :aria-expanded="open.tags ? 'true' : 'false'">
        <span class="fold-title">Tag</span>
        <span class="fold-summary">{{ sumTags }}</span>
        <span class="chev" :class="{ open: open.tags }">▸</span>
      </button>
      <div v-show="open.tags" class="fold-body">
        <TagEditor :model-value="tags" :disabled="disabledAll" @update:model-value="onTagsChange"/>
      </div>
    </section>

    <!-- Immagini (caricate su un host esterno, non sul database) -->
    <section v-if="!minimal || forceImmagini" class="fold">
      <button type="button" class="fold-head" @click="open.immagini = !open.immagini"
              :aria-expanded="open.immagini ? 'true' : 'false'">
        <span class="fold-title">Immagini</span>
        <span class="fold-summary">&nbsp;</span>
        <span class="chev" :class="{ open: open.immagini }">▸</span>
      </button>
      <div v-show="open.immagini" class="fold-body">
        <!-- montato solo all'apertura: evita una chiamata di rete per ogni editor aperto -->
        <ImmaginiEditor v-if="open.immagini" :id-item="props.mode === 'edit' ? props.item?.id : null"
                        :disabled="disabledAll"/>
      </div>
    </section>

    <!-- Randomizzatori innescati da questo oggetto (catene) -->
    <section v-if="!minimal" class="fold">
      <button type="button" class="fold-head" @click="open.randTrigger = !open.randTrigger"
              :aria-expanded="open.randTrigger ? 'true' : 'false'">
        <span class="fold-title">Randomizzatori innescati</span>
        <span class="fold-summary">{{ form.randTrigger.length || '—' }}</span>
        <span class="chev" :class="{ open: open.randTrigger }">▸</span>
      </button>
      <div v-show="open.randTrigger" class="fold-body">
        <TriggerRandomizzatoriEditor v-model="form.randTrigger" :disabled="disabledAll"
                                     :id-corrente="props.item?.id"/>
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
      <span class="lbl">Descrizione{{ mostraDescrizioneEn ? ' (EN)' : '' }}</span>
      <HtmlEditor v-if="!mostraDescrizioneEn" v-model="form.descrizione" :rows="10" :disabled="disabledAll">
        <template #toolbar-extra>
          <button type="button" class="lang-flag" :disabled="disabledAll"
                  title="Show in English" @mousedown.prevent @click="mostraDescrizioneEn = true">
            <span class="fi fi-it"></span>
          </button>
        </template>
      </HtmlEditor>
      <HtmlEditor v-else v-model="form.descrizioneEn" :rows="10" :disabled="disabledAll">
        <template #toolbar-extra>
          <button type="button" class="lang-flag" :disabled="disabledAll"
                  title="Mostra in italiano" @mousedown.prevent @click="mostraDescrizioneEn = false">
            <span class="fi fi-gb"></span>
          </button>
        </template>
      </HtmlEditor>
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
        <VisibilitaPicker v-model="form.visibilita" :disabled="disabledAll"/>
      </label>
    </template>

    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

    <div class="actions">
      <button type="button" class="btn ghost" @click="onCancel" :disabled="busy">Annulla</button>
      <span v-if="salvato" class="salvato">Salvato</span>
      <!-- salva senza uscire dall'editor -->
      <button type="button" class="btn icona" :disabled="!canSave" title="Salva e resta qui"
              aria-label="Salva e resta qui" @click="onSalvaResta">
        <Icona name="SALVA"/>
      </button>
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
.lang-flag-inline { margin-right: .3rem; vertical-align: middle; }
.lang-flag-inline.fi { width: 1.1em; line-height: 1em; }
.lang-flag {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 1.9rem; height: 1.9rem; border: 1px solid transparent; border-radius: .35rem;
  background: transparent; cursor: pointer;
}
.lang-flag:hover { background: var(--btn-bg-hover); }
.lang-flag:disabled { opacity: .5; cursor: default; }
.lang-flag .fi { width: 1.2em; line-height: 1em; }

input[type="text"], input[type="number"], input[type="datetime-local"], textarea, select {
  width: 100%; padding: .5rem .6rem; border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0); margin: 0;
}
textarea { resize: vertical; }

.fold { border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0); }
.fold-head {
  width: 100%; display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: .5rem;
  padding: .5rem .75rem; background: var(--btn-bg); border: 0; border-bottom: 1px solid var(--hairline); cursor: pointer; text-align: left;
}
.fold-title { font-weight: 600; }
.fold-summary { color: var(--text-muted); opacity: .8; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.chev { transition: transform .15s ease; }
.chev.open { transform: rotate(90deg); }
.fold-body { padding: .6rem .75rem; }

.error {
  margin: 0; padding: .5rem .75rem; border-radius: .5rem;
  color: var(--danger-text); background: var(--danger-bg); border: 1px solid var(--danger-border); font-size: .85rem;
}

.actions {
  position: sticky; bottom: 0; background: var(--surface-0);
  padding: .5rem 0 calc(.5rem + env(safe-area-inset-bottom, 0px));
  margin-top: .25rem; border-top: 1px solid var(--hairline);
  display: flex; justify-content: flex-end; gap: .5rem; align-items: center;
}
/* salva senza uscire: solo icona, per distinguerlo a colpo d'occhio dal Salva che chiude */
.btn.icona {
  border-color: var(--hairline); background: var(--surface-0); color: var(--text-muted);
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 2.4rem; padding: .5rem;
}
.btn.icona:hover:not(:disabled) { background: var(--btn-bg); }
.salvato {
  font-size: .8rem; font-weight: 600; color: var(--success-text);
  margin-right: auto; padding-left: .25rem;
}
.compendio-flag {
  display: inline-flex; align-items: center; gap: .5rem;
  font-size: .85rem; font-weight: 600; cursor: pointer;
}
.compendio-flag input { width: auto; }

.btn { padding: .5rem .9rem; border-radius: .5rem; border: 1px solid transparent; cursor: pointer; }
.btn.ghost { border-color: var(--hairline); background: var(--surface-0); }
.btn.outline { border-color: var(--info-border); background: var(--info-bg); color: var(--info-text); font-weight: 600; }
.btn.primary { background: #2563eb; color: white; }
.btn:disabled { opacity: .6; cursor: default; }
.btn-del {
  border: 1px solid var(--danger-border); background: var(--danger-bg); color: var(--danger-text);
  border-radius: .5rem; padding: .25rem .5rem; cursor: pointer;
}
.btn-add {
  justify-self: start; border: 1px dashed var(--hairline); background: var(--surface-0);
  border-radius: .5rem; padding: .4rem .7rem; cursor: pointer; font-size: .85rem;
}
.incarico-row { display: flex; gap: .4rem; margin-bottom: .4rem; }
.incarico-input {
  flex: 1; padding: .45rem .6rem; border: 1px solid var(--hairline); border-radius: .5rem; font: inherit;
}

.rank-grid { display: grid; grid-template-columns: 1fr 1fr; gap: .5rem; }
@media (max-width: 700px) { .rank-grid { grid-template-columns: 1fr; } }

.sez-card { border: 1px solid var(--hairline); border-radius: .5rem; padding: .5rem; display: grid; gap: .5rem; margin-bottom: .4rem; background: var(--btn-bg); }
.sez-head { display: flex; align-items: center; justify-content: space-between; }
.sez-title { font-weight: 700; font-size: .9rem; }
.chips { display: flex; flex-wrap: wrap; gap: .3rem; margin-bottom: .3rem; }
.chip { display: inline-flex; align-items: center; gap: .3rem; background: var(--info-bg); color: var(--info-text); border-radius: 1rem; padding: .1rem .5rem; font-size: .8rem; font-weight: 600; }
.chip-x { border: 0; background: transparent; color: #6366f1; cursor: pointer; font-size: .75rem; padding: 0; }
.custom-lista-row { display: flex; gap: .4rem; margin-top: .35rem; }
.custom-lista-row input { flex: 1; padding: .4rem .5rem; border: 1px solid var(--hairline); border-radius: .4rem; }

.conc-add { display: grid; grid-template-columns: 5rem 1fr; gap: .4rem; align-items: end; }
.conc-add .grow { min-width: 0; padding: .5rem .6rem; border: 1px solid var(--hairline); border-radius: .5rem; }
.liv-input { max-width: 6rem; }
.results {
  list-style: none; margin: 0; padding: 0;
  border: 1px solid var(--hairline); border-radius: .5rem; overflow: hidden;
  max-height: 14rem; overflow-y: auto;
}
.results li + li { border-top: 1px solid var(--hairline); }
.result {
  width: 100%; display: grid; grid-template-columns: 1fr auto; gap: .4rem; align-items: center;
  padding: .4rem .5rem; background: var(--surface-0); border: 0; cursor: pointer; text-align: left;
}
.result:hover { background: var(--surface-hover); }
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
  border: 1px solid var(--hairline); border-radius: .5rem; font-variant-numeric: tabular-nums;
}
.qta-btn {
  width: 2.1rem; border: 1px solid var(--hairline); border-radius: .5rem; background: var(--btn-bg);
  font-weight: 800; font-size: 1rem; cursor: pointer;
}
.qta-btn:disabled { opacity: .5; cursor: default; }
.field-checkbox { flex-direction: row; align-items: center; gap: .5rem; }
.field-checkbox input[type="checkbox"] { width: 1.1rem; height: 1.1rem; cursor: pointer; }
.section-card {
  border: 1px solid var(--hairline);
  border-radius: .5rem;
  overflow: hidden;
}
.section-card-header {
  background: var(--btn-bg);
  padding: .4rem .75rem;
  font-size: .78rem;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: .04em;
}
.section-card .row { padding: .75rem; }
</style>
