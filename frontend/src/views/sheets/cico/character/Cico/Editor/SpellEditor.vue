<script setup lang="ts">
import {computed, onMounted, reactive, ref, toRaw, watch} from 'vue'
import {saveSpell} from '../../../../../../service/PersonaggioService'
import {ItemLabel} from "../../../../../../models/entity/ItemLabel";
import {ItemDB} from "../../../../../../models/entity/ItemDB";
import {UpdateSpellRequest} from "../../../../../../models/dto/UpdateSpellRequest";
import HtmlEditor from "../../../../../../components/HtmlEditor.vue";
import SearchSelect from "../../../../../../components/SearchSelect.vue";
import Icona from "../../../../../../components/Icona/Icona.vue";
import {useMondoSistema} from '../../../../../../function/useMondoSistema'
import {getConfigMondo, getTipoItemConfig} from '../../../../../../service/MondoAdminService'
import CardScuole from './Spell/CardScuole.vue'
import CardSottoscuole from './Spell/CardSottoscuole.vue'
import CardDescrittori from './Spell/CardDescrittori.vue'
import CardClassiDomini from './Spell/CardClassiDomini.vue'
import CardComponenti from './Spell/CardComponenti.vue'


const props = defineProps<{ item: ItemDB; readonly?: boolean }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void; (e: 'savedResta', item: ItemDB): void }>()
const {mondoOptions, sistemaOptions} = useMondoSistema()

/* ========= Labels chiavi ========= */
const L = {
  SCUOLA: 'SCUOLA_SP',
  TEMPO: 'TEMPO_SP',
  TS: 'TS_SP',
  RANGE: 'RANGE_SP',
  DURATA: 'DURATA_SP',
  COMP: 'COMP_SP',
  MANUALE: 'MANUALE_SP',
  EN_NAME: 'EN_NAME',
  DESCRIZIONE_EN: 'DESCRIZIONE_EN'
} as const

/* ========= Mapping EN -> IT (Scuole/Sottoscuole/Descrittori) ========= */
const MAP_SCHOOL: Record<string, string> = {
  Abjuration: 'Abiurazione', Conjuration: 'Evocazione', Divination: 'Divinazione',
  Enchantment: 'Ammaliamento', Evocation: 'Invocazione', Illusion: 'Illusione',
  Necromancy: 'Necromanzia', Transmutation: 'Trasmutazione', Universal: 'Universale'
}
const MAP_SUB: Record<string, string> = {
  Calling: 'Richiamo', Creation: 'Creazione', Summoning: 'Evocazione',
  Teleportation: 'Teletrasporto', Healing: 'Guarigione', Scrying: 'Scrutamento',
  Polymorph: 'Metamorfosi', Charm: 'Charme', Compulsion: 'Compulsione',
  Figment: 'Immagine', Glamer: 'Camuffamento', Pattern: 'Trama',
  Phantasm: 'Fantasma', Shadow: 'Ombra'
}
const MAP_DESC: Record<string, string> = {
  Acid: 'Acido', Air: 'Aria', Chaotic: 'Caotico', Cold: 'Freddo', Ice: 'Freddo',
  Darkness: 'Oscurità', Death: 'Morte', Earth: 'Terra', Electricity: 'Elettricità',
  Evil: 'Malvagio', Fear: 'Paura', Fire: 'Fuoco', Force: 'Forza', Good: 'Bene',
  'Language-Dependent': 'Dipendente dal linguaggio', Lawful: 'Legale',
  Light: 'Luce', 'Mind-Affecting': 'Influenza mentale', Sonic: 'Sonoro', Water: 'Acqua'
}
const DESC_ALIASES: Record<string, string> = {
  'mindaffecting': 'Mind-Affecting', 'mind affecting': 'Mind-Affecting',
  'language dependent': 'Language-Dependent', 'language-dependent': 'Language-Dependent'
}
const IT_SCHOOLS = Object.values(MAP_SCHOOL)
const IT_SUBS = Object.values(MAP_SUB)
const IT_DESCS = Object.values(MAP_DESC)
const LC_SCHOOL = new Map(IT_SCHOOLS.map(s => [s.toLowerCase(), s]))
const LC_SUB = new Map(IT_SUBS.map(s => [s.toLowerCase(), s]))
const LC_DESC = new Map(IT_DESCS.map(s => [s.toLowerCase(), s]))
const SCHOOL_SET = new Set(Object.keys(MAP_SCHOOL))
const SUB_SET = new Set(Object.keys(MAP_SUB))
const DESC_SET = new Set(Object.keys(MAP_DESC))

/* ========= Classi/Domini SP_* ========= */
const CLASS_LABELS: Record<string, string> = {
  SP_BLACKGUARD: 'Cavaliere Nero', SP_STRENGTH: 'Forza', SP_GREED: 'Avarizia',
  SP_CLERIC: 'Chierico', SP_CORRUPTION: 'Corruzione', SP_EVIL: 'Male',
  SP_DRAGON: 'Drago', SP_OCEAN: 'Oceano', SP_FORCE: 'Forza (Dominio)',
  SP_CELERITY: 'Celerità', SP_PLANT: 'Vegetale', SP_LIBERATION: 'Libertà',
  SP_MADNESS: 'Follia', SP_DEATH: 'Morte', SP_ADEPT: 'Adepto', SP_ANIMAL: 'Animale',
  SP_WINTER: 'Inverno', SP_THIRST: 'Sete', SP_RUNE: 'Rune', SP_SPITE: 'Rancore',
  SP_WATER: 'Acqua', SP_DRUID: 'Druido', SP_ASN: 'Assassino', SP_ASSASIN: 'Assassino',
  SP_TEMPTATION: 'Tentazione', SP_WEATHER: 'Meteo', SP_EARTH: 'Terra', SP_MIND: 'Mente',
  SP_DEATHBOUND: 'Vincolo della Morte', SP_KNOWLEDGE: 'Conoscenza',
  SP_SEAFOLK: 'Genti del Mare', SP_FIRE: 'Fuoco', SP_WAR: 'Guerra',
  SP_BEGUILER: 'Mistificatore', SP_PESTILENCE: 'Pestilenza', SP_HEXBLADE: 'Lama Maledetta',
  SP_HEALER: 'Guaritore', SP_CREATION: 'Creazione', SP_CORRUPT: 'Corrotto', SP_DREAM: 'Sogno',
  SP_BARD: 'Bardo', SP_SORCERER: 'Stregone', SP_SUMMER: 'Estate', SP_CHAOS: 'Caos',
  SP_HUNGER: 'Fame', SP_SAND: 'Sabbia', SP_FURY: 'Furia', SP_WARMAGE: 'Mago da Guerra',
  SP_SHUGENJA: 'Shugenja', SP_GOOD: 'Bene', SP_GLORY: 'Gloria', SP_TRAVEL: 'Viaggio',
  SP_PACT: 'Patto', SP_ONEIROMANCY: 'Oniromanzia', SP_MAGIC: 'Magia', SP_WIZARD: 'Mago',
  SP_OOZE: 'Melma', SP_REPOSE: 'Riposo', SP_CITY: 'Città', SP_SKY: 'Cielo', SP_DEMONIC: 'Demoniaco',
  SP_COLD: 'Freddo', SP_DESTRUCTION: 'Distruzione', SP_SLOT_BONUS: 'Slot Bonus', SP_AIR: 'Aria',
  SP_RANGER: 'Ranger', SP_LUCK: 'Fortuna', SP_SUN: 'Sole', SP_LAW: 'Legge', SP_DOMINATION: 'Dominazione',
  SP_PURIFICATION: 'Purificazione', SP_PALADIN: 'Paladino', SP_PROTECTION: 'Protezione',
  SP_TRICKERY: 'Inganno', SP_ENTROPY: 'Entropia', SP_DESTINY: 'Destino',
  SP_HEALING: 'Guarigione', SP_DUSKBLADE: 'Lama Crepuscolare', SP_BLACKWATER: 'Acquenere'
}
const CLASS_CODES = Object.keys(CLASS_LABELS).sort()

/* ========= Modello locale ========= */
type ComponentKey = 'V' | 'S' | 'M' | 'F' | 'DF' | 'XP' | 'X' | 'CORRUPT' | 'COLDFIRE' | 'FROSTFELL' | 'B'
type BoolMap = Record<string, boolean>
type SpellDraft = {
  nome: string
  enName: string
  manuale: string
  descrizione: string
  descrizioneEn: string
  idMondo: number | null
  idSistema: number | null
  scuole: BoolMap
  subscuole: BoolMap
  descrittori: BoolMap
  tempo: string
  range: string
  durata: string
  ts: string
  comp: Record<ComponentKey, boolean>
  classi: Record<string, string>
  classiCustom: Array<{ codice: string; livello: string }>  // liste/classi non nel catalogo preimpostato
}
function emptyBoolMap(list: string[]): BoolMap {
  return Object.fromEntries(list.map(s => [s, false])) as BoolMap
}

function emptyClassLevels(): Record<string, string> {
  return Object.fromEntries(CLASS_CODES.map(c => [c, ''])) as Record<string, string>
}

function emptyDraft(): SpellDraft {
  return {
    nome: '', enName: '', manuale: '', descrizione: '', descrizioneEn: '',
    idMondo: null, idSistema: null,
    scuole: emptyBoolMap(IT_SCHOOLS),
    subscuole: emptyBoolMap(IT_SUBS),
    descrittori: emptyBoolMap(IT_DESCS),
    tempo: '', range: '', durata: '',
    ts: 'Nessuno',
    comp: {
      V: false,
      S: false,
      M: false,
      F: false,
      DF: false,
      XP: false,
      X: false,
      CORRUPT: false,
      COLDFIRE: false,
      FROSTFELL: false,
      B: false
    },
    classi: emptyClassLevels(),
    classiCustom: []
  }
}
const form = reactive<SpellDraft>(emptyDraft())

// Card strutturali abilitate per (mondo, INCANTESIMO): vedi MondoTipoItemCardAbilitata lato backend.
const cards = ref<Set<string>>(new Set())
watch(() => form.idMondo, async (idMondo) => {
  if (!idMondo) { cards.value = new Set(); return }
  try {
    const {data} = await getTipoItemConfig(idMondo, 'INCANTESIMO')
    cards.value = new Set(data.cardAbilitate)
  } catch (e) {
    console.error('Errore caricamento configurazione card:', e)
    cards.value = new Set()
  }
}, {immediate: true})

// Liste/domini incantesimi abilitati per il mondo scelto (vedi MondoListaIncantesimiAbilitata
// lato backend): codice -> etichetta (l'etichetta serve per i codici abilitati dall'admin che NON
// sono nel catalogo statico CLASS_LABELS qui sotto, es. liste create al volo per un mondo).
// null finché non risolto o in caso di errore = nessuna restrizione, per non far sparire di colpo
// la griglia Classi/Domìni se la chiamata fallisce o il mondo non è impostato.
const listeAbilitateMondo = ref<Map<string, string> | null>(null)
watch(() => form.idMondo, async (idMondo) => {
  if (!idMondo) { listeAbilitateMondo.value = null; return }
  try {
    const {data} = await getConfigMondo(idMondo)
    const mappa = new Map(data.listeIncantesimiAbilitate.map(l => [l.codice, l.etichetta] as const))
    listeAbilitateMondo.value = mappa
    // i codici abilitati per il mondo ma assenti dal catalogo statico (es. liste create ad hoc per
    // questo mondo) devono avere comunque una entry in form.classi, altrimenti il relativo <select>
    // della griglia non avrebbe nulla a cui legarsi (v-model su una chiave mai inizializzata)
    for (const codice of mappa.keys()) {
      if (form.classi[codice] === undefined) form.classi[codice] = ''
    }
  } catch (e) {
    console.error('Errore caricamento configurazione mondo:', e)
    listeAbilitateMondo.value = null
  }
}, {immediate: true})

// Codici classi/domìni mostrati nella griglia "Classi / Domìni": quelli abilitati per il mondo
// (catalogo statico o dinamico) PIÙ qualunque codice già valorizzato sull'incantesimo anche se non
// (più) abilitato — altrimenti disabilitare una lista dopo il fatto ne nasconderebbe silenziosamente
// il livello già impostato. Nessuna restrizione nota (mondo non impostato/errore) = tutto il
// catalogo statico, comportamento precedente.
const classCodesVisibili = computed(() => {
  if (!listeAbilitateMondo.value) return CLASS_CODES
  const abilitati = listeAbilitateMondo.value
  const staticiValorizzatiNonAbilitati = CLASS_CODES.filter(c => !abilitati.has(c) && !!form.classi[c])
  return [...abilitati.keys(), ...staticiValorizzatiNonAbilitati]
})

/* ========= Stato di espansione ========= */
const open = reactive({scuole: false, sub: false, desc: false, classi: false, comp: false})

// Switch bandierina per editare la descrizione IT/EN nello stesso riquadro (stesso pattern di
// BaseItemEditor.vue).
const mostraDescrizioneEn = ref(false)

/* ========= Helpers ========= */
function getLabel(labels: ItemLabel[] | undefined, key: string): string {
  const v = labels?.find(l => l.label === key)?.valore ?? ''
  return String(v ?? '').trim()
}
function splitTokens(s: string): string[] {
  return s.split(/[,/;]+|(?:\s+or\s+)|(?:\s+and\s+)/i).map(x => x.trim()).filter(Boolean)
}
function tcase(s: string) {
  return s.trim().replace(/\s+/g, ' ').replace(/(^|\s)\w/g, m => m.toUpperCase())
}
function normalizeDescToken(s: string) {
  const raw = s.trim().toLowerCase().replace(/\s+/g, ' ')
  return DESC_ALIASES[raw] ?? tcase(raw)
}

/* ========= Parser SCUOLA_SP ========= */
function parseSchoolEN(line: string) {
  const raw = line.replace(/^"|"$/g, '').trim()
  if (!raw) return {schools: [] as string[], subs: [] as string[], desc: [] as string[]}
  const bracket = Array.from(raw.matchAll(/\[(.*?)\]/g)).map(m => m[1])
  const paren = Array.from(raw.matchAll(/\((.*?)\)/g)).map(m => m[1])
  const schoolPart = raw.replace(/\(.*?\)/g, '').replace(/\[.*?\]/g, '').trim()
  const schoolCandidates = schoolPart.split(/\s*\/\s*/).map(s => s.replace(/\d+$/, '').trim()).filter(Boolean)

  const schoolsIT: string[] = []
  for (const c of schoolCandidates) {
    const C = tcase(c)
    if (SCHOOL_SET.has(C)) schoolsIT.push(MAP_SCHOOL[C])
  }
  const subsIT: string[] = []
  const descIT: string[] = []

  for (const p of paren) {
    for (const tok of splitTokens(p)) {
      const T = tcase(tok)
      if (/^see text/i.test(T)) continue
      if (SUB_SET.has(T)) subsIT.push(MAP_SUB[T])
      else {
        const D = normalizeDescToken(T)
        if (DESC_SET.has(D)) descIT.push(MAP_DESC[D])
      }
    }
  }
  for (const b of bracket) {
    for (const tok of splitTokens(b)) {
      const D = normalizeDescToken(tok)
      if (/^see text/i.test(D)) continue
      if (DESC_SET.has(D)) descIT.push(MAP_DESC[D])
    }
  }
  return {
    schools: Array.from(new Set(schoolsIT)),
    subs: Array.from(new Set(subsIT)),
    desc: Array.from(new Set(descIT)),
  }
}
function parseSchoolIT(line: string) {
  const raw = line.replace(/^"|"$/g, '').trim()
  if (!raw) return {schools: [] as string[], subs: [] as string[], desc: [] as string[]}
  const bracket = Array.from(raw.matchAll(/\[(.*?)\]/g)).map(m => m[1])
  const paren = Array.from(raw.matchAll(/\((.*?)\)/g)).map(m => m[1])
  const schoolPart = raw.replace(/\(.*?\)/g, '').replace(/\[.*?\]/g, '').trim()
  const schoolCandidates = schoolPart.split(/\s*\/\s*/).map(s => s.trim()).filter(Boolean)

  const schoolsIT: string[] = []
  for (const c of schoolCandidates) {
    const canon = LC_SCHOOL.get(c.toLowerCase())
    if (canon) schoolsIT.push(canon)
  }
  const subsIT: string[] = []
  const descIT: string[] = []

  const eat = (s: string) => splitTokens(s).forEach(tok => {
    const t = tok.toLowerCase()
    const sub = LC_SUB.get(t)
    const dsc = LC_DESC.get(t)
    if (sub) subsIT.push(sub)
    else if (dsc) descIT.push(dsc)
  })
  paren.forEach(eat)
  bracket.forEach(eat)

  return {
    schools: Array.from(new Set(schoolsIT)),
    subs: Array.from(new Set(subsIT)),
    desc: Array.from(new Set(descIT)),
  }
}
function parseSchoolAny(line: string) {
  const en = parseSchoolEN(line)
  if (en.schools.length || en.subs.length || en.desc.length) return en
  return parseSchoolIT(line)
}

/* ========= TS Parser/Renderer ========= */
type TSTypeEN = 'Will' | 'Fortitude' | 'Reflex' | 'None'
type TSEffectEN = 'negates' | 'half' | 'partial' | 'disbelief' | 'special'
type TSOp = 'or' | 'and' | 'then'
interface TSClause { type: TSTypeEN; effect?: TSEffectEN; qualifiers: string[] }
interface TSParsed { clauses: TSClause[]; ops: TSOp[]; notes: string[]; raw: string }

const TYPE_RE = /\b(will|fortitude|reflex|none|no|yes)\b/i
const EFFECT_WORDS: Record<string, TSEffectEN> = {
  negates: 'negates', half: 'half', partial: 'partial', disbelief: 'disbelief', special: 'special'
}
const QUALI_MAP: Record<string, string> = {
  harmless: 'harmless', object: 'object', 'if interacted with': 'if interacted with',
  'everyone else': 'everyone else', plants: 'plants', 'other living creatures': 'other living creatures'
}
const NOTE_WORDS = ['see text', 'see below', 'below', 'varies']

function norm(s: string) { return s.trim().replace(/\s+/g, ' ').toLowerCase() }
function titleCase(s: string) { return s.replace(/(^|\s)\w/g, m => m.toUpperCase()) }

function splitTsClauses(line: string) {
  const L = ' ' + line.replace(/;+/g, ' ; ').replace(/,+/g, ' , ') + ' '
  const cleaned = L.replace(/\s*;\s*then\s*/ig, ' then ')
      .replace(/\s+then\s+/ig, ' then ')
      .replace(/\s+and\s+/ig, ' and ')
      .replace(/\s+or\s+/ig, ' or ')
  const tokens = cleaned.split(/\s+(?=or|and|then)\s+|(?<=\sor|\sand|\sthen)\s+/i)
  const clauses: string[] = []
  const ops: TSOp[] = []
  for (const t of tokens) {
    const w = t.trim()
    if (!w) continue
    if (/^(or|and|then)$/i.test(w)) ops.push(w.toLowerCase() as TSOp)
    else clauses.push(w)
  }
  return {clauses, ops}
}
function extractParens(s: string) {
  const quals: string[] = []
  const parens = [...s.matchAll(/\((.*?)\)/g)].map(m => m[1])
  for (const p of parens) {
    const items = p.split(/[,;]+/).map(x => norm(x)).map(x => x.replace(/\.$/, '')).filter(Boolean)
    for (const it of items) {
      if (QUALI_MAP[it]) quals.push(QUALI_MAP[it])
      else if (NOTE_WORDS.includes(it)) quals.push(it)
      else quals.push(it)
    }
  }
  const rest = s.replace(/\(.*?\)/g, '').trim()
  return {rest, qualifiers: quals}
}
function parseTsClauseEN(raw: string): { clause?: TSClause, notes: string[] } {
  const {rest, qualifiers} = extractParens(raw)
  const r = norm(rest)
  const notes: string[] = []
  for (const w of NOTE_WORDS) if (r.includes(w)) notes.push(w)
  const m = r.match(TYPE_RE)
  if (!m) {
    if (r.includes('disbelief')) return {clause: {type: 'Will', effect: 'disbelief', qualifiers}, notes}
    return {notes}
  }
  let typeWord = m[1].toLowerCase()
  if (typeWord === 'no') typeWord = 'none'
  if (typeWord === 'yes') { notes.push('yes'); return {notes} }
  const type = titleCase(typeWord) as TSTypeEN
  let eff: TSEffectEN | undefined
  for (const k of Object.keys(EFFECT_WORDS)) if (r.includes(k)) { eff = EFFECT_WORDS[k]; break }
  return {clause: {type, effect: eff, qualifiers}, notes}
}
function parseSavingThrow(line: string): TSParsed {
  const raw = (line ?? '').trim().replace(/^"|"$/g, '')
  if (!raw) return {clauses: [], ops: [], notes: [], raw}
  const {clauses: rawClauses, ops} = splitTsClauses(raw)
  const clauses: TSClause[] = []
  const notes: string[] = []
  for (const c of rawClauses) {
    const {clause, notes: n} = parseTsClauseEN(c)
    if (clause) clauses.push(clause)
    if (n.length) notes.push(...n)
  }
  if (!clauses.length) {
    const lc = norm(raw)
    if (/\bnone\b/.test(lc)) clauses.push({type: 'None', qualifiers: []})
    else if (/\bno\b/.test(lc)) notes.push('no')
    else if (/\byes\b/.test(lc)) notes.push('yes')
  }
  return {clauses, ops, notes: Array.from(new Set(notes)), raw}
}
function renderSavingThrowIt(parsed: TSParsed): string {
  if (!parsed.clauses.length && !parsed.notes.length) return '—'
  const TTYPE: Record<TSTypeEN, string> = {Will: 'Volontà', Fortitude: 'Tempra', Reflex: 'Riflessi', None: 'Nessuno'}
  const TEFF: Record<NonNullable<TSEffectEN>, string> = {
    negates: 'nega', half: 'mezzi danni', partial: 'parziale', disbelief: 'incredulità', special: 'speciale'
  }
  const QMAP: Record<string, string> = {
    harmless: 'innocuo', object: 'oggetto', 'if interacted with': 'se interagito', 'everyone else': 'altri',
    plants: 'piante', 'other living creatures': 'altre creature viventi', 'see text': 'vedi testo', varies: 'variabile'
  }
  const joinOp = (op: TSOp) => op === 'or' ? ' o ' : op === 'and' ? ' e ' : ' poi '
  const parts: string[] = []
  parsed.clauses.forEach((c, i) => {
    const base = c.type === 'None' ? 'Nessuno' : `${TTYPE[c.type]}${c.effect ? ' ' + TEFF[c.effect] : ''}`
    const quals = (c.qualifiers || []).map(q => QMAP[q] || q).filter(Boolean)
    parts.push(quals.length ? `${base} (${quals.join(', ')})` : base)
    if (parsed.ops[i]) parts.push(joinOp(parsed.ops[i]))
  })
  const out = parts.join('')
  if (parsed.notes.length) {
    const nt = parsed.notes.map(n => n === 'yes' ? 'sì' : (n === 'no' ? 'no' : (n === 'see text' ? 'vedi testo' : n))).join(', ')
    return nt ? `${out}${out ? '; ' : ''}${nt}` : out
  }
  return out
}

/* ========= Normalizzatori TEMPO / DURATA / RANGE ========= */
function pluralize(itSing: string, itPl: string, n: number) { return n === 1 ? itSing : itPl }
const WORDNUM: Record<string, number> = {
  one: 1, two: 2, three: 3, four: 4, five: 5, six: 6, seven: 7, eight: 8, nine: 9, ten: 10,
  twelve: 12, twenty: 20, thirty: 30, sixty: 60
}
function wordToNumber(s: string): string { const n = WORDNUM[s.toLowerCase()]; return n != null ? String(n) : s }

function normalizeTempo(raw: string): string {
  if (!raw) return ''
  let s = raw.trim().replace(/^"|"$/g, '')
  s = s.replace(/\bSee text\b/gi, 'vedi testo')
      .replace(/\bsee text\b/gi, 'vedi testo')
      .replace(/\b(One|Two|Three|Four|Five|Six|Seven|Eight|Nine|Ten|Twelve|Twenty|Thirty|Sixty)\b/gi, m => wordToNumber(m))
      .replace(/\b(\d+)\s*full[-\s]?round\s*action(s)?\b/gi, (_m, n) => `${n} ${pluralize('azione di round completo', 'azioni di round completo', Number(n))}`)
      .replace(/\b(\d+)\s*full\s*rounds?\b/gi, (_m, n) => `${n} ${pluralize('round completo', 'round completi', Number(n))}`)
      .replace(/\b1\s*full\s*round\b/gi, '1 round completo')
      .replace(/\b(\d+)\s*standard\s*actions?\b/gi, (_m, n) => `${n} ${pluralize('azione standard', 'azioni standard', Number(n))}`)
      .replace(/\b(\d+)\s*swift\s*actions?\b/gi, (_m, n) => `${n} ${pluralize('azione rapida', 'azioni rapide', Number(n))}`)
      .replace(/\b(\d+)\s*immediate\s*actions?\b/gi, (_m, n) => `${n} ${pluralize('azione immediata', 'azioni immediate', Number(n))}`)
      .replace(/\b(\d+)\s*free\s*actions?\b/gi, (_m, n) => `${n} ${pluralize('azione gratuita', 'azioni gratuite', Number(n))}`)
      .replace(/\b(\d+)\s*actions?\b/gi, (_m, n) => `${n} ${pluralize('azione', 'azioni', Number(n))}`)
      .replace(/\b(\d+)\s*rounds?\b/gi, (_m, n) => `${n} ${pluralize('round', 'round', Number(n))}`)
      .replace(/\b(\d+)\s*minutes?\b/gi, (_m, n) => `${n} ${pluralize('minuto', 'minuti', Number(n))}`)
      .replace(/\b(\d+)\s*hours?\b/gi, (_m, n) => `${n} ${pluralize('ora', 'ore', Number(n))}`)
      .replace(/\bAt least (\d+)\s*minutes?\b/gi, (_m, n) => `Almeno ${n} ${pluralize('minuto', 'minuti', Number(n))}`)
      .replace(/\bor\b/gi, 'o').replace(/\band\b/gi, 'e')
  return s.replace(/\s+/g, ' ').trim()
}

function normalizeDurata(raw: string): string {
  if (!raw) return ''
  let s = raw.trim().replace(/^"|"$/g, '')
  s = s.replace(/\bInstantaneous\b/gi, 'Istantanea')
      .replace(/\bConcentration\b/gi, 'Concentrazione')
      .replace(/\bSee text\b/gi, 'vedi testo')
      .replace(/\bSee below\b/gi, 'vedi sotto')
      .replace(/\bmax(?:imum)?\b/gi, 'massimo')
      .replace(/\b(One|Two|Three|Four|Five|Six|Seven|Eight|Nine|Ten)\b/gi, m => wordToNumber(m))
      .replace(/\bmin\.\s*\/\s*level\b/gi, 'min./livello')
      .replace(/\bmin\.\s*\/\s* level\b/gi, 'min./livello')
      .replace(/\bminute\s*\/\s*level\b/gi, 'minuto/livello')
      .replace(/\bminutes\s*\/\s*level\b/gi, 'minuti/livello')
      .replace(/\bhour\s*\/\s*level\b/gi, 'ora/livello')
      .replace(/\bhours\s*\/\s*level\b/gi, 'ore/livello')
      .replace(/\bround\s*\/\s*level\b/gi, 'round/livello')
      .replace(/\brounds\s*\/\s*level\b/gi, 'round/livello')
      .replace(/\b(\d+)\s*day\/level\b/gi, (_m, n) => `${n} giorno/livello`)
      .replace(/\b(\d+)\s*days\/level\b/gi, (_m, n) => `${n} giorni/livello`)
      .replace(/\bOne\s*day\/level\b/gi, '1 giorno/livello')
      .replace(/\b(\d+)\s*rounds?\b/gi, (_m, n) => `${n} ${pluralize('round', 'round', Number(n))}`)
      .replace(/\b(\d+)\s*minutes?\b/gi, (_m, n) => `${n} ${pluralize('minuto', 'minuti', Number(n))}`)
      .replace(/\b(\d+)\s*min\.\b/gi, (_m, n) => `${n} ${pluralize('minuto', 'minuti', Number(n))}`)
      .replace(/\b(\d+)\s*hours?\b/gi, (_m, n) => `${n} ${pluralize('ora', 'ore', Number(n))}`)
      .replace(/\b(\d+)\s*weeks?\b/gi, (_m, n) => `${n} ${pluralize('settimana', 'settimane', Number(n))}`)
      .replace(/\b(\d+)\s*months?\b/gi, (_m, n) => `${n} ${pluralize('mese', 'mesi', Number(n))}`)
      .replace(/\b(\d+)\s*years?\b/gi, (_m, n) => `${n} ${pluralize('anno', 'anni', Number(n))}`)
      .replace(/\buntil discharged\b/gi, 'fino a scaricarsi')
      .replace(/\buntil expended\b/gi, 'fino a esaurimento')
      .replace(/\buntil used\b/gi, 'fino a utilizzo')
      .replace(/\buntil triggered\b/gi, 'fino ad attivazione')
      .replace(/\buntil completed\b/gi, 'fino a completamento')
      .replace(/\buntil escaped\b/gi, 'fino a fuga')
      .replace(/\buntil landing\b/gi, 'fino all’atterraggio')
      .replace(/\bPermanent\b/gi, 'Permanente')
      .replace(/\bpermanent\b/gi, 'permanente')
      .replace(/\bor\b/gi, 'o').replace(/\band\b/gi, 'e').replace(/\bthen\b/gi, 'poi')
  return s.replace(/\s+/g, ' ').trim()
}

function ftToMetersStr(feet: number): string {
  const m = feet * 0.3
  const rounded = Math.round(m * 10) / 10
  const s = rounded.toFixed(rounded % 1 === 0 ? 0 : 1)
  return s.replace('.', ',') + ' m'
}
function convertFeetEverywhere(s: string): string {
  s = s.replace(/ft\.(\d+)/gi, 'ft.')
      .replace(/(100)\s*ft\.\s*\+\s*(10)\s*ft\.\s*level/gi, '$1 ft. + $2 ft./level')
      .replace(/(400)\s*ft\.\s*\+\s*(40)\s*ft\.\s*level/gi, '$1 ft. + $2 ft./level')
      .replace(/(25)\s*ft\.\s*\+\s*(5)\s*ft\.\s*(?:2\s*)?level[s]?/gi, '$1 ft. + $2 ft./2 levels')
      .replace(/\bft\b/gi, 'ft.')
      .replace(/(\d+(?:\.\d+)?)\s*ft\.\s*\/\s*(\d+)\s*levels?\b/gi, (_m, n, div) => `${ftToMetersStr(Number(n))}/${div} livelli`)
      .replace(/(\d+(?:\.\d+)?)\s*ft\.\s*\/\s*level\b/gi, (_m, n) => `${ftToMetersStr(Number(n))}/livello`)
      .replace(/(\d+(?:\.\d+)?)\s*ft\./gi, (_m, n) => ftToMetersStr(Number(n)))
  return s
}
function normalizeRange(raw: string): string {
  if (!raw) return ''
  let s = raw.trim().replace(/^"|"$/g, '')
  s = s.replace(/\bPersonal\b/gi, 'Personale')
      .replace(/\bTouch\b/gi, 'Contatto')
      .replace(/\bClose\b/gi, 'Vicino')
      .replace(/\bShort\b/gi, 'Vicino')
      .replace(/\bMedium\b/gi, 'Medio')
      .replace(/\bLong\b/gi, 'Lungo')
      .replace(/\bUnlimited\b/gi, 'Illimitato')
      .replace(/\bSpecial\b/gi, 'Speciale')
      .replace(/\bSee text\b/gi, 'vedi testo')
      .replace(/\bSee below\b/gi, 'vedi sotto')
      .replace(/\bTargets?\b/gi, m => m[0] === 'T' ? 'Bersagli' : 'bersagli')
      .replace(/\bEffect\b/gi, 'Effetto')
      .replace(/\bArea\b/gi, 'Area')
  s = convertFeetEverywhere(s)
  s = s.replace(/\/\s*(\d+)\s*levels?\b/gi, (_m, n) => `/${n} livelli`)
      .replace(/\/\s*level\b/gi, '/livello')
      .replace(/\blevels\b/gi, 'livelli')
      .replace(/\blevel\b/gi, 'livello')
      .replace(/\bor\b/gi, 'o').replace(/\band\b/gi, 'e')
  return s.replace(/\s+/g, ' ').trim()
}

/* ========= TS: fallback-first ========= */
const ITALIAN_TS_HINTS = /(tempra|riflessi|volontà|nessuno|nega|parziale|mezzi|incredulità|vedi testo|sì|si\b)/i
function normalizeOrRawTS(raw: string | null | undefined): string {
  const s = String(raw ?? '').trim()
  if (!s) return 'Nessuno'
  // Se già italiano (o plausibile), non toccare
  if (ITALIAN_TS_HINTS.test(s)) return s
  // Prova a parsare EN -> IT
  const rendered = renderSavingThrowIt(parseSavingThrow(s))
  return rendered && rendered !== '—' ? rendered : s
}

/* ========= Preload una volta ========= */
function parseComponenti(labels?: ItemLabel[], fromItem?: string[]) {
  const vals = (labels ?? []).filter(l => l.label === L.COMP).map(l => l.valore ?? '')
  const tokens = new Set<string>()
  for (const v of vals) splitTokens(v).forEach(t => tokens.add(t.toUpperCase()))
  if (fromItem?.length) fromItem.forEach(t => tokens.add(String(t).toUpperCase()))
  return tokens
}

onMounted(() => {
  Object.assign(form, emptyDraft())
  form.nome = props.item.nome ?? ''
  form.enName = getLabel(props.item.labels, L.EN_NAME) ?? ''
  form.manuale = getLabel(props.item.labels, L.MANUALE) ?? ''
  form.descrizione = props.item.descrizione ?? ''
  form.descrizioneEn = getLabel(props.item.labels, L.DESCRIZIONE_EN) ?? ''
  mostraDescrizioneEn.value = false
  form.idMondo = props.item.mondo?.id ?? null
  form.idSistema = props.item.sistema?.id ?? null

  // Tempo / Durata / Range: normalizza in IT (non distruttivo)
  const tempoRaw = getLabel(props.item.labels, L.TEMPO) ?? '';
  const durataRaw = getLabel(props.item.labels, L.DURATA) ?? '';
  const rangeRaw = getLabel(props.item.labels, L.RANGE) ?? '';
  form.tempo = normalizeTempo(tempoRaw)
  form.durata = normalizeDurata(durataRaw)
  form.range = normalizeRange(rangeRaw)

  // TS: fallback-first -> se italiano/ignoto, usa il salvato
  const tsRaw = getLabel(props.item.labels, L.TS) ?? '';
  form.ts = normalizeOrRawTS(tsRaw)

  // Componenti
  const compTokens = parseComponenti(props.item.labels, undefined)
  ;(Object.keys(form.comp) as ComponentKey[]).forEach(k => { form.comp[k] = compTokens.has(k) })

  // Scuole/Sub/Descrittori
  const scuolaLine = getLabel(props.item.labels, L.SCUOLA)
  const parsed = parseSchoolAny(scuolaLine)
  for (const k of Object.keys(form.scuole)) form.scuole[k] = false
  for (const k of Object.keys(form.subscuole)) form.subscuole[k] = false
  for (const k of Object.keys(form.descrittori)) form.descrittori[k] = false
  parsed.schools.forEach(s => form.scuole[s] = true)
  parsed.subs.forEach(s => form.subscuole[s] = true)
  parsed.desc.forEach(d => form.descrittori[d] = true)

  // Classi/Domini SP_*: catalogo preimpostato + codici personalizzati (liberi, non nel catalogo)
  form.classiCustom = []
  for (const l of (props.item.labels ?? [])) {
    if (!/^SP_[A-Z_]+$/.test(l.label)) continue
    const n = Number((l.valore ?? '').trim())
    const livello = Number.isFinite(n) && n >= 0 && n <= 9 ? String(n) : ''
    if (!livello) continue
    if (l.label in CLASS_LABELS) {
      form.classi[l.label] = livello
    } else {
      form.classiCustom.push({codice: l.label, livello})
    }
  }
})

/* ========= Summaries ========= */
function arrFromBoolMap(m: BoolMap) { return Object.keys(m).filter(k => m[k]) }
const sumScuole = computed(() => arrFromBoolMap(form.scuole).join(' / ') || '—')
const sumSub = computed(() => arrFromBoolMap(form.subscuole).join(', ') || '—')
const sumDesc = computed(() => arrFromBoolMap(form.descrittori).join(', ') || '—')
const sumComp = computed(() => {
  const picked: string[] = []
  ;(Object.keys(form.comp) as ComponentKey[]).forEach(k => { if (form.comp[k]) picked.push(k) })
  return picked.join(', ') || '—'
})
function friendlyNameFromCode(code: string): string {
  const dinamica = listeAbilitateMondo.value?.get(code)
  if (dinamica) return dinamica
  if (CLASS_LABELS[code]) return CLASS_LABELS[code]
  return code.replace(/^SP_/, '').toLowerCase().replace(/_/g, ' ').replace(/(^|\s)\w/g, m => m.toUpperCase())
}
const sumClassi = computed(() => {
  const items: string[] = []
  // Object.keys(form.classi), non CLASS_CODES: include anche i codici abilitati dal mondo ma
  // assenti dal catalogo statico (vedi listeAbilitateMondo/classCodesVisibili sopra).
  for (const code of Object.keys(form.classi)) {
    const v = form.classi[code]
    if (v !== '' && v != null) items.push(`${friendlyNameFromCode(code)} ${v}`)
  }
  for (const c of form.classiCustom) {
    if (c.codice.trim() && c.livello !== '') items.push(`${c.codice.trim()} ${c.livello}`)
  }
  return items.join(', ') || '—'
})

/* ========= Salvataggio ========= */
const busy = ref(false)
const disabledAll = computed(() => !!props.readonly || busy.value)
const canSave = computed(() => form.nome.trim().length > 0 && !busy.value && !props.readonly)
function buildScuolaStringIT(scuole: string[], sub: string[], desc: string[]) {
  const left = scuole.join('/')
  const mid = sub.length ? ` (${sub.join(', ')})` : ''
  const right = desc.length ? ` [${desc.join(', ')}]` : ''
  return `${left}${mid}${right}`.trim()
}
/** Esegue il salvataggio; ritorna l'item salvato, o null se non è andato a buon fine. Separato
 *  dall'emit così può servire sia al Salva che chiude sia al floppy che resta nell'editor. */
async function salva(): Promise<ItemDB | null> {
  if (!canSave.value) return null
  try {
    busy.value = true
    const scuoleArr = arrFromBoolMap(form.scuole)
    const subsArr = arrFromBoolMap(form.subscuole)
    const descArr = arrFromBoolMap(form.descrittori)

    const classi: Array<{ classe: string; livello: number }> = []
    // Object.keys(form.classi), non CLASS_CODES: altrimenti un codice abilitato dal mondo ma
    // assente dal catalogo statico verrebbe silenziosamente scartato al salvataggio.
    for (const code of Object.keys(form.classi)) {
      const v = form.classi[code]
      if (v === '' || v == null) continue
      const n = Number(v)
      if (Number.isFinite(n) && n >= 0 && n <= 9) classi.push({classe: code, livello: n})
    }
    // codici personalizzati (liste non nel catalogo preimpostato)
    for (const c of form.classiCustom) {
      const codice = c.codice.trim().toUpperCase()
      if (!codice || c.livello === '') continue
      const n = Number(c.livello)
      if (Number.isFinite(n) && n >= 0 && n <= 9) classi.push({classe: codice, livello: n})
    }

    const payload: UpdateSpellRequest = toRaw({
      nome: form.nome,
      descrizione: form.descrizione,
      idMondo: form.idMondo ?? null,
      idSistema: form.idSistema ?? null,
      tempo: form.tempo,
      range: form.range,
      durata: form.durata,
      ts: form.ts, // <-- salva esattamente il valore visibile (già italiano o raw)
      componenti: (Object.entries(form.comp) as [ComponentKey, boolean][])
          .filter(([, v]) => v).map(([k]) => k),
      scuole: scuoleArr,
      subscuole: subsArr,
      descrittori: descArr,
      classi,
      labelsPatch: {
        [L.SCUOLA]: buildScuolaStringIT(scuoleArr, subsArr, descArr),
        [L.EN_NAME]: form.enName?.trim() ?? '',
        [L.MANUALE]: form.manuale?.trim() ?? '',
        [L.DESCRIZIONE_EN]: form.descrizioneEn?.trim() ?? ''
      }
    })

    const {data} = await saveSpell(props.item.id, payload)
    return data ?? props.item
  } catch (e) {
    console.error('Errore salvataggio spell:', e)
    return null
  } finally {
    busy.value = false
  }
}

async function onSave() {
  if (await salva()) emit('saved')
}

// floppy: salva senza uscire dall'editor
const salvato = ref(false)
let timerSalvato: ReturnType<typeof setTimeout> | null = null

async function onSalvaResta() {
  const item = await salva()
  if (!item) return
  emit('savedResta', item)
  salvato.value = true
  if (timerSalvato) clearTimeout(timerSalvato)
  timerSalvato = setTimeout(() => salvato.value = false, 2000)
}

function onCancel() { emit('cancel') }
</script>

<template>
  <form class="spell-editor" @submit.prevent="onSave">
    <header class="sp-head">
      <h2>Incantesimo</h2>
      <span class="muted">ID #{{ props.item.id }}</span>
    </header>

    <div class="row three">
      <label class="field">
        <span class="lbl"><span class="fi fi-it lang-flag-inline"></span> Nome</span>
        <input v-model.trim="form.nome" type="text" :disabled="disabledAll" required/>
      </label>
      <label class="field">
        <span class="lbl">Tempo</span>
        <input v-model.trim="form.tempo" type="text" :disabled="disabledAll" placeholder="Azione standard / 1 round …"/>
      </label>
      <label class="field">
        <span class="lbl">Tiro Salvezza</span>
        <input v-model.trim="form.ts" type="text" :disabled="disabledAll" placeholder="Es.: Volontà nega (innocuo) …"/>
      </label>
    </div>

    <div class="row three">
      <label class="field">
        <span class="lbl"><span class="fi fi-gb lang-flag-inline"></span> Nome (EN)</span>
        <input v-model.trim="form.enName" type="text" :disabled="disabledAll" placeholder="Nome originale in inglese"/>
      </label>
      <label class="field">
        <span class="lbl">Manuale</span>
        <input v-model.trim="form.manuale" type="text" :disabled="disabledAll" placeholder="Manuale di provenienza"/>
      </label>
      <div class="field"></div>
    </div>

    <div class="row three">
      <label class="field">
        <span class="lbl">Mondo</span>
        <SearchSelect v-model="form.idMondo" :options="mondoOptions" placeholder="— nessuno —" :disabled="disabledAll" :sort="false"/>
      </label>
      <label class="field">
        <span class="lbl">Sistema</span>
        <SearchSelect v-model="form.idSistema" :options="sistemaOptions" placeholder="— nessuno —" :disabled="disabledAll" :sort="false"/>
      </label>
      <div class="field"></div>
    </div>

    <div class="row three">
      <label class="field">
        <span class="lbl">Range</span>
        <input v-model.trim="form.range" type="text" :disabled="disabledAll"
               placeholder="Vicino (7,5 m + 1,5 m/2 livelli) …"/>
      </label>
      <label class="field">
        <span class="lbl">Durata</span>
        <input v-model.trim="form.durata" type="text" :disabled="disabledAll"
               placeholder="Istantanea / 1 round/livello …"/>
      </label>
      <div class="field"></div>
    </div>

    <!-- Scuole -->
    <section v-if="cards.has('SPELL_SCUOLE')" class="fold">
      <button type="button" class="fold-head" @click="open.scuole = !open.scuole"
              :aria-expanded="open.scuole ? 'true' : 'false'">
        <span class="fold-title">Scuole</span>
        <span class="fold-summary">{{ sumScuole }}</span>
        <span class="chev" :class="{ open: open.scuole }">▸</span>
      </button>
      <div v-show="open.scuole" class="fold-body">
        <CardScuole :scuole="form.scuole" :schools="IT_SCHOOLS" :disabled="disabledAll"/>
      </div>
    </section>

    <!-- Sottoscuole -->
    <section v-if="cards.has('SPELL_SOTTOSCUOLE')" class="fold">
      <button type="button" class="fold-head" @click="open.sub = !open.sub"
              :aria-expanded="open.sub ? 'true' : 'false'">
        <span class="fold-title">Sottoscuole</span>
        <span class="fold-summary">{{ sumSub }}</span>
        <span class="chev" :class="{ open: open.sub }">▸</span>
      </button>
      <div v-show="open.sub" class="fold-body">
        <CardSottoscuole :subscuole="form.subscuole" :subs="IT_SUBS" :disabled="disabledAll"/>
      </div>
    </section>

    <!-- Descrittori -->
    <section v-if="cards.has('SPELL_DESCRITTORI')" class="fold">
      <button type="button" class="fold-head" @click="open.desc = !open.desc"
              :aria-expanded="open.desc ? 'true' : 'false'">
        <span class="fold-title">Descrittori</span>
        <span class="fold-summary">{{ sumDesc }}</span>
        <span class="chev" :class="{ open: open.desc }">▸</span>
      </button>
      <div v-show="open.desc" class="fold-body">
        <CardDescrittori :descrittori="form.descrittori" :descs="IT_DESCS" :disabled="disabledAll"/>
      </div>
    </section>

    <!-- Classi/Domini -->
    <section v-if="cards.has('SPELL_CLASSI_DOMINI')" class="fold">
      <button type="button" class="fold-head" @click="open.classi = !open.classi"
              :aria-expanded="open.classi ? 'true' : 'false'">
        <span class="fold-title">Classi / Domìni</span>
        <span class="fold-summary">{{ sumClassi }}</span>
        <span class="chev" :class="{ open: open.classi }">▸</span>
      </button>
      <div v-show="open.classi" class="fold-body">
        <CardClassiDomini
            :classi="form.classi" :classi-custom="form.classiCustom" :class-codes="classCodesVisibili"
            :friendly-name="friendlyNameFromCode" :disabled="disabledAll"/>
      </div>
    </section>

    <!-- Componenti -->
    <section v-if="cards.has('SPELL_COMPONENTI')" class="fold">
      <button type="button" class="fold-head" @click="open.comp = !open.comp"
              :aria-expanded="open.comp ? 'true' : 'false'">
        <span class="fold-title">Componenti</span>
        <span class="fold-summary">{{ sumComp }}</span>
        <span class="chev" :class="{ open: open.comp }">▸</span>
      </button>
      <div v-show="open.comp" class="fold-body">
        <CardComponenti
            :comp="form.comp" :keys="['V','S','M','F','DF','XP','X','CORRUPT','COLDFIRE','FROSTFELL','B']"
            :disabled="disabledAll"/>
      </div>
    </section>

    <label class="field">
      <span class="lbl">Descrizione{{ mostraDescrizioneEn ? ' (EN)' : '' }}</span>
      <HtmlEditor v-if="!mostraDescrizioneEn" v-model="form.descrizione" :rows="16" :disabled="disabledAll">
        <template #toolbar-extra>
          <button type="button" class="lang-flag" :disabled="disabledAll"
                  title="Show in English" @mousedown.prevent @click="mostraDescrizioneEn = true">
            <span class="fi fi-it"></span>
          </button>
        </template>
      </HtmlEditor>
      <HtmlEditor v-else v-model="form.descrizioneEn" :rows="16" :disabled="disabledAll">
        <template #toolbar-extra>
          <button type="button" class="lang-flag" :disabled="disabledAll"
                  title="Mostra in italiano" @mousedown.prevent @click="mostraDescrizioneEn = false">
            <span class="fi fi-gb"></span>
          </button>
        </template>
      </HtmlEditor>
    </label>

    <div class="actions">
      <button type="button" class="btn ghost" @click="onCancel" :disabled="busy">Annulla</button>
      <span v-if="salvato" class="salvato">Salvato</span>
      <button type="button" class="btn icona" :disabled="!canSave" title="Salva e resta qui"
              aria-label="Salva e resta qui" @click="onSalvaResta"><Icona name="SALVA"/></button>
      <button type="submit" class="btn primary" :disabled="!canSave">Salva</button>
    </div>
  </form>
</template>

<style scoped>
.spell-editor { display: grid; gap: .75rem; margin: 0; }
.sp-head { display: flex; align-items: baseline; gap: .5rem; margin: 0; }
.sp-head h2 { margin: 0; font-size: 1rem; }
.muted { opacity: .7; font-size: .85rem; }

.row { display: grid; gap: .5rem; }
.row.three { grid-template-columns: 1fr 1fr 1fr; }
@media (max-width: 900px) { .row.three { grid-template-columns: 1fr; } }

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

input[type="text"], select, textarea {
  width: 100%; padding: .5rem .6rem; border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0); margin: 0;
}
textarea { resize: vertical; min-height: 16rem; }

/* folds */
.fold { border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0); }
.fold + .fold { margin-top: .25rem; }
.fold-head {
  width: 100%; display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: .5rem;
  padding: .5rem .75rem; background: var(--btn-bg); border: 0; border-bottom: 1px solid var(--hairline); cursor: pointer; text-align: left;
}
.fold-title { font-weight: 600; }
.fold-summary { color: var(--text-muted); opacity: .8; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.chev { transition: transform .15s ease; transform: rotate(0deg); }
.chev.open { transform: rotate(90deg); }
.fold-body { padding: .6rem .75rem; }

.components { margin: 0; border: 0; padding: 0; }
.components legend { display: none; }
.chk { display: inline-flex; align-items: center; gap: .5rem; margin: 0 .75rem .5rem 0; }
.chk input { accent-color: #2563eb; }

/* classi/domìni grid */
.class-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: .5rem .75rem; }
.class-row {
  display: grid; grid-template-columns: 1fr 5rem; align-items: center; gap: .5rem;
  padding: .25rem .4rem; border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0);
}
.class-name { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.level-select { width: 100%; }

.custom-classi { margin-top: .75rem; padding-top: .6rem; border-top: 1px dashed var(--hairline); display: grid; gap: .5rem; }
.custom-row { grid-template-columns: 1fr 5rem auto; }
.custom-code { width: 100%; padding: .35rem .5rem; border: 1px solid var(--hairline); border-radius: .4rem; }
.btn-del {
  border: 1px solid var(--danger-border); background: var(--danger-bg); color: var(--danger-text);
  border-radius: .5rem; padding: .25rem .5rem; cursor: pointer;
}

/* actions sticky */
.actions {
  position: sticky; bottom: 0; background: var(--surface-0);
  padding: .5rem 0 calc(.5rem + env(safe-area-inset-bottom, 0px));
  margin-top: .25rem; border-top: 1px solid var(--hairline);
  display: flex; justify-content: flex-end; gap: .5rem; align-items: center;
}

.btn { padding: .5rem .9rem; border-radius: .5rem; border: 1px solid transparent; cursor: pointer; }
.btn.ghost { border-color: var(--hairline); background: var(--surface-0); }
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
.btn.outline { border-color: var(--info-border); background: var(--info-bg); color: var(--info-text); font-weight: 600; }
.btn.primary { background: #2563eb; color: white; }
.btn:disabled { opacity: .6; cursor: default; }

/* a11y */
.sr-only {
  position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); border: 0;
}
</style>
