<script setup lang="ts">
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {Stat} from '../../../../../../../models/entity/Stat'
import api from '../../../../../../../service/api'
import {getStats} from '../../../../../../../service/PersonaggioService'
import HtmlEditor from '../../../../../../../components/HtmlEditor.vue'
import Icona from '../../../../../../../components/Icona/Icona.vue'
import SearchSelect from '../../../../../../../components/SearchSelect.vue'
import {useMondoSistema} from '../../../../../../../function/useMondoSistema'
import {getConfigMondo, getTipoItemConfig} from '../../../../../../../service/MondoAdminService'
import {LabelRow} from '../../../../../../../models/dto/UpdateItemRequest'
import CardInfoRazza from '../Classe/CardInfoRazza.vue'
import CardAbilitaClasse from '../Classe/CardAbilitaClasse.vue'
import CardIncantesimiClasse from '../Classe/CardIncantesimiClasse.vue'
import CardTabellaLivelli from '../Classe/CardTabellaLivelli.vue'
import CardPrivilegiClasse from '../Classe/CardPrivilegiClasse.vue'
import ChildrenEditor from '../Sections/ChildrenEditor.vue'
import CardScelte, {SezioneScelta} from '../Sections/CardScelte.vue'
import {ChildRef} from '../../../../../../../models/dto/UpdateItemRequest'

const props = defineProps<{ item: ItemDB; readonly?: boolean; mode?: 'edit' | 'create' }>()
const emit = defineEmits<{
  (e: 'saved'): void
  (e: 'cancel'): void
  (e: 'savedResta', item: { id: number }): void
}>()

const {mondoOptions, sistemaOptions, autoMondo, autoSistema} = useMondoSistema()

interface LivelloClasse {
  livello: number
  bab: string
  tmp: string
  rfl: string
  vlt: string
  spSlot: string
}

interface AbilitaConcessa {
  livello: number
  itemId: number | null   // null = oggetto "nuovo" da creare in fase di salvataggio
  nome: string
  tipo?: string
  nuovo?: boolean
  qty?: number | null
  // --- editing avanzato (caricati on-demand dall'item collegato quando si attiva la modalità avanzata) ---
  descrizione?: string
  altreLabels?: LabelRow[]   // label esistenti sull'item diverse dai 4 descrittori, da preservare al salvataggio
  straordinaria?: boolean
  magica?: boolean
  soprannaturale?: boolean
  naturale?: boolean
  gruppoPrivilegi?: string  // GRUPPO_PRIVILEGI: vedi PrivilegioEditor.vue
  caricato?: boolean
  salvandoRiga?: boolean
}

const form = reactive({
  nome: '',
  enName: '',
  manuale: '',
  idMondo: null as number | null,
  idSistema: null as number | null,
  descrizione: '',
  abilitaClasse: [] as string[],
  spellList: '',
  spellSlotBonus: '',
  // sezioni incantatore: ognuna 1..N liste (unite) + progressione + formula bonus + slot custom
  // + incantesimi conosciuti (opzionale/flaggabile, indipendente dalla progressione slot)
  sezioni: [] as Array<{
    liste: string[]; progressione: string; bonus: string; slot: string[]
    conosciutiSeparati: boolean; conosciuti: string[]; slotConContatore: boolean; caratteristica: string
    casterLevelSorgente: string; slotLivelloSorgente: string; modo: string
  }>,
  rank1: '',
  rank: '',
  // Info Razza (solo tipo RAZZA): campi puramente descrittivi.
  razzaTaglia: '',
  razzaVelocita: '',
  razzaCaratteristiche: '',
  razzaLap: '',
  razzaSpazio: '',
  razzaPortata: '',
  razzaEta: '',
  numLivelli: 20,
  dv: '',
  livelli: Array.from({length: 20}, (_, i) => ({
    livello: i + 1, bab: '', tmp: '', rfl: '', vlt: '', spSlot: '',
  })) as LivelloClasse[],
  abilitaConcesse: [] as AbilitaConcessa[],
  children: [] as ChildRef[],
  scelte: [] as SezioneScelta[],
})

watch([autoMondo, autoSistema], ([m, s]) => {
  if (props.mode !== 'create') return
  if (m !== null && form.idMondo === null) form.idMondo = m
  if (s !== null && form.idSistema === null) form.idSistema = s
}, {immediate: true})

// Liste/domini incantesimi abilitati per il mondo scelto (vedi MondoListaIncantesimiAbilitata
// lato backend): null finché non risolto o in caso di errore = nessuna restrizione, per non far
// sparire di colpo la tendina se la chiamata fallisce o il mondo non è ancora impostato.
const listeAbilitateMondo = ref<Set<string> | null>(null)
watch(() => form.idMondo, async (idMondo) => {
  if (!idMondo) { listeAbilitateMondo.value = null; return }
  try {
    const {data} = await getConfigMondo(idMondo)
    listeAbilitateMondo.value = new Set(data.listeIncantesimiAbilitate.map(l => l.codice))
  } catch (e) {
    console.error('Errore caricamento configurazione mondo:', e)
    listeAbilitateMondo.value = null
  }
}, {immediate: true})

// Card strutturali abilitate per (mondo, tipo item): vedi MondoTipoItemCardAbilitata lato backend.
const cards = ref<Set<string>>(new Set())
watch(() => form.idMondo, async (idMondo) => {
  if (!idMondo) { cards.value = new Set(); return }
  try {
    const {data} = await getTipoItemConfig(idMondo, props.item.tipo)
    cards.value = new Set(data.cardAbilitate)
  } catch (e) {
    console.error('Errore caricamento configurazione card:', e)
    cards.value = new Set()
  }
}, {immediate: true})

const isRazza = computed(() => props.item.tipo === 'RAZZA')

const loading = ref(props.mode !== 'create')
const busy = ref(false)
const errorMsg = ref<string | null>(null)
const disabledAll = computed(() => !!props.readonly || busy.value)
const canSave = computed(() => form.nome.trim().length > 0 && !busy.value && !props.readonly)

/* numero di livelli della classe -> righe mostrate nella tabella */
function ensureLivelli(n: number) {
  for (let i = form.livelli.length; i < n; i++) {
    form.livelli.push({livello: i + 1, bab: '', tmp: '', rfl: '', vlt: '', spSlot: ''})
  }
}
// righe compilate della tabella livelli, per il riepilogo del fold (vedi CardTabellaLivelli
// per la gestione interattiva della tabella stessa)
const livelliCompilati = computed(() => form.livelli.slice(0, form.numLivelli).filter(l => l.bab).length)

/* ---- caricamento ---- */
onMounted(async () => {
  loadStats()
  if (props.mode === 'create') return
  try {
    const res = await api.get(`/item/classe/${props.item.id}`)
    const d = res.data
    form.nome = d.nome ?? ''
    form.enName = d.enName ?? ''
    form.manuale = d.manuale ?? ''
    form.razzaTaglia = d.razzaTaglia ?? ''
    form.razzaVelocita = d.razzaVelocita ?? ''
    form.razzaCaratteristiche = d.razzaCaratteristiche ?? ''
    form.razzaLap = d.razzaLap ?? ''
    form.razzaSpazio = d.razzaSpazio ?? ''
    form.razzaPortata = d.razzaPortata ?? ''
    form.razzaEta = d.razzaEta ?? ''
    form.idMondo = d.idMondo ?? null
    form.idSistema = d.idSistema ?? null
    form.descrizione = d.descrizione ?? ''
    const tokensAb: string[] = d.abilitaClasse ?? []
    const spogliaToken = (t: string) => t.replace('!', '').replace('?', '').trim()
    form.abilitaClasse = tokensAb.map(spogliaToken).filter(Boolean)
    abPersonaggio.value = new Set(
        tokensAb.filter(t => t.includes('!')).map(spogliaToken).filter(Boolean)
    )
    abEsclusaCap.value = new Set(
        tokensAb.filter(t => t.includes('?')).map(spogliaToken).filter(Boolean)
    )
    form.spellList = d.spellList ?? ''
    form.spellSlotBonus = d.spellSlotBonus ?? ''
    form.sezioni = (d.sezioniIncantesimi ?? []).map((s: any) => ({
      liste: Array.isArray(s.liste) ? s.liste : (s.liste ? [s.liste] : []),
      progressione: s.progressione ?? 'CUSTOM',
      bonus: s.bonus ?? '',
      slot: Array.isArray(s.slot) ? s.slot.slice() : [],
      conosciutiSeparati: !!s.conosciutiSeparati,
      conosciuti: Array.isArray(s.conosciuti) ? s.conosciuti.slice() : [],
      slotConContatore: !!s.slotConContatore,
      caratteristica: s.caratteristica ?? '',
      casterLevelSorgente: s.casterLevelSorgente ?? 'NM',
      slotLivelloSorgente: s.slotLivelloSorgente ?? 'NM',
      modo: s.modo ?? 'SLOT',
    }))
    form.rank1 = d.rank1 ?? ''
    form.rank = d.rank ?? ''
    form.dv = d.dv ?? ''
    form.numLivelli = Math.max(1, Math.min(100, Number(d.numLivelli) || 20))
    ensureLivelli(form.numLivelli)
    for (const row of (d.livelli ?? [])) {
      const target = form.livelli[row.livello - 1]
      if (!target) continue
      target.bab = row.bab ?? ''
      target.tmp = row.tmp ?? ''
      target.rfl = row.rfl ?? ''
      target.vlt = row.vlt ?? ''
      target.spSlot = row.spSlot ?? ''
    }
    form.abilitaConcesse = (d.abilitaConcesse ?? []).map((a: any) => ({
      livello: a.livello, itemId: a.itemId, nome: a.nome, tipo: a.tipo, qty: a.qty ?? null,
    }))
    form.children = (d.children ?? []).map((c: any) => ({
      id: c.id, nome: c.nome, tipo: c.tipo, qty: c.qty ?? null, formulaQty: c.formulaQty ?? null,
      scelta: c.scelta ?? null, nascosto: c.nascosto ?? false, condizione: c.condizione ?? null,
    }))
    // card SCELTE: stesso schema di parsing di BaseItemEditor.vue (righe SCELTA_<n>_TITOLO/
    // _CANDIDATI grezze qui, i candidati sono già {id,nome,tipo} nel JSON salvato).
    const scelteRaw: Record<number, { titolo?: string; candidatiJson?: string }> = {}
    for (const l of (d.scelteLabels ?? [])) {
      const m = String(l.label ?? '').match(/^SCELTA_(\d+)_(TITOLO|CANDIDATI)$/)
      if (!m) continue
      const n = Number(m[1])
      const row = (scelteRaw[n] ??= {})
      if (m[2] === 'TITOLO') row.titolo = l.valore
      else row.candidatiJson = l.valore
    }
    form.scelte = Object.keys(scelteRaw).map(Number).sort((a, b) => a - b).map(n => {
      const r = scelteRaw[n]
      let candidati: ChildRef[] = []
      try { candidati = JSON.parse(r.candidatiJson ?? '[]') } catch { candidati = [] }
      return {titolo: r.titolo ?? '', candidati}
    })
  } catch (e) {
    errorMsg.value = 'Errore nel caricamento della classe'
    console.error('Errore caricamento classe:', e)
  } finally {
    loading.value = false
  }
})

/* ---- abilità di classe (multi-selezione dalle stat) ---- */
const stats = ref<Stat[]>([])
// id abilità marcate come "abilità personaggio" (serializzate con "!"):
// valgono anche nei livelli che non usano questa classe
const abPersonaggio = ref<Set<string>>(new Set())
// id abilità marcate come "esclusa dal limite gradi" (serializzate con "?", componibile con "!"):
// resta selezionabile/spendibile ma come se fosse cross-class ai fini del limite gradi massimi —
// a meno che la stessa abilità non sia comunque di classe per un altro motivo (altra classe/token
// senza "?"), nel qual caso vale comunque il limite pieno (l'esclusione non sopprime altre fonti)
const abEsclusaCap = ref<Set<string>>(new Set())

async function loadStats() {
  try {
    stats.value = await getStats()
  } catch (e) {
    console.error('Errore caricamento stats:', e)
  }
}

/* ---- salvataggio ---- */
function buildClassePayload() {
  return {
    id: props.mode === 'create' ? null : props.item.id,
    tipo: props.mode === 'create' ? props.item.tipo : undefined,
    nome: form.nome.trim(),
    enName: form.enName.trim() || null,
    manuale: form.manuale.trim() || null,
    razzaTaglia: form.razzaTaglia.trim() || null,
    razzaVelocita: form.razzaVelocita.trim() || null,
    razzaCaratteristiche: form.razzaCaratteristiche.trim() || null,
    razzaLap: form.razzaLap.trim() || null,
    razzaSpazio: form.razzaSpazio.trim() || null,
    razzaPortata: form.razzaPortata.trim() || null,
    razzaEta: form.razzaEta.trim() || null,
    idMondo: form.idMondo ?? null,
    idSistema: form.idSistema ?? null,
    descrizione: form.descrizione || null,
    abilitaClasse: form.abilitaClasse.map(id =>
        id + (abPersonaggio.value.has(id) ? '!' : '') + (abEsclusaCap.value.has(id) ? '?' : '')),
    spellList: null,
    spellSlotBonus: null,
    sezioniIncantesimi: form.sezioni
        .map(s => {
          const prog = (s.progressione || 'CUSTOM').trim()
          const modo = (s.modo || 'SLOT').trim()
          const modoLivello = modo === 'LIVELLO'
          // LIVELLO: s.slot ha un'unica riga (la soglia di sblocco), non va troncata a numLivelli
          // come le tabelle per-livello-di-classe di SLOT.
          const slot = modoLivello
              ? s.slot.slice(0, 1).map(x => (x ?? '').trim())
              : prog === 'CUSTOM'
                  ? s.slot.slice(0, form.numLivelli).map(x => (x ?? '').trim())
                  : []
          // LIVELLO: conosciuti è sempre attiva (ignora il checkbox conosciutiSeparati, non
          // mostrato in UI in questa modalità).
          const conosciuti = (s.conosciutiSeparati || modoLivello)
              ? s.conosciuti.slice(0, form.numLivelli).map(x => (x ?? '').trim())
              : []
          return {
            liste: (s.liste ?? []).map(x => x.trim()).filter(Boolean),
            progressione: prog,
            modo,
            bonus: s.bonus.trim() || null,
            slot: slot.some(x => x) ? slot : null,
            conosciutiSeparati: s.conosciutiSeparati || modoLivello,
            conosciuti: conosciuti.some(x => x) ? conosciuti : null,
            slotConContatore: s.slotConContatore,
            caratteristica: (s.caratteristica || '').trim() || null,
            casterLevelSorgente: s.casterLevelSorgente || null,
            slotLivelloSorgente: s.slotLivelloSorgente || null,
          }
        })
        .filter(s => s.liste.length > 0),
    rank1: form.rank1.trim() || null,
    rank: form.rank.trim() || null,
    dv: form.dv.trim() || null,
    numLivelli: form.numLivelli,
    livelli: form.livelli.slice(0, form.numLivelli),
    abilitaConcesse: form.abilitaConcesse.map(a => ({livello: a.livello, itemId: a.itemId, nome: a.nome, qty: a.qty ?? null})),
    children: form.children.map(c => ({
      id: c.id, qty: c.qty ?? null, formulaQty: c.formulaQty ?? null, scelta: c.scelta ?? null,
      nascosto: c.nascosto ?? false, condizione: c.condizione ?? null,
    })),
    // Card SCELTE: stesso schema di scrittura di BaseItemEditor.vue — una sezione senza candidati
    // non viene salvata, gli indici "n" restano sempre contigui e senza buchi.
    scelteLabels: (() => {
      const labels: { label: string; valore: string }[] = []
      let n = 0
      for (const s of form.scelte) {
        if (!s.candidati.length) continue
        if (s.titolo.trim()) labels.push({label: `SCELTA_${n}_TITOLO`, valore: s.titolo.trim()})
        labels.push({
          label: `SCELTA_${n}_CANDIDATI`,
          valore: JSON.stringify(s.candidati.map(c => ({id: c.id, nome: c.nome, tipo: c.tipo}))),
        })
        n++
      }
      return labels
    })(),
  }
}

/** Esegue il salvataggio; ritorna la classe salvata (con id, utile in creazione) o null se
 *  fallito. Separato dall'emit così serve sia al "Salva classe" che chiude sia al floppy. */
async function salva(): Promise<{ id: number } | null> {
  if (!canSave.value) return null
  busy.value = true
  errorMsg.value = null
  try {
    const {data} = await api.post('/item/classe', buildClassePayload())
    return data ?? {id: props.item.id}
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 403
        ? 'Non hai i permessi'
        : 'Errore nel salvataggio della classe'
    console.error('Errore salvataggio classe:', e)
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
  const salvataClasse = await salva()
  if (!salvataClasse) return
  emit('savedResta', salvataClasse)
  salvato.value = true
  if (timerSalvato) clearTimeout(timerSalvato)
  timerSalvato = setTimeout(() => salvato.value = false, 2000)
}

// Persistenza usata dalla card "Privilegi di Classe" per il bottone "Salva riga" in modalità
// avanzata: la card aggiorna prima l'item collegato, poi richiama questa per ripersistere la
// classe (senza emettere 'saved' — l'editor resta aperto, a differenza del bottone "Salva classe").
async function persistiClasseInline(): Promise<void> {
  errorMsg.value = null
  try {
    await api.post('/item/classe', buildClassePayload())
  } catch (e) {
    errorMsg.value = 'Errore nel salvataggio del privilegio'
    console.error('Errore salvataggio riga privilegio:', e)
    throw e
  }
}

const incantatore = computed(() => form.sezioni.some(s => s.liste.length > 0))

/* sezioni richiudibili */
const open = reactive({abilita: false, incantesimi: false, tabella: false, concesse: false, infoRazza: false, children: false, scelte: false})
const sumInfoRazza = computed(() => {
  const flags = []
  if (form.razzaTaglia.trim()) flags.push(`Taglia: ${form.razzaTaglia.trim()}`)
  if (form.razzaVelocita.trim()) flags.push(`Velocità: ${form.razzaVelocita.trim()}`)
  if (cards.value.has('CLASSE_ETA') && form.razzaEta.trim()) flags.push(`Età: ${form.razzaEta.trim()}`)
  if (form.razzaLap.trim()) flags.push(`LAP: ${form.razzaLap.trim()}`)
  return flags.join(', ') || '—'
})
</script>

<template>
  <form class="classe-editor" @submit.prevent="onSave">
    <header class="ce-head">
      <h2>{{ props.item.tipo === 'RAZZA' ? 'Razza' : 'Classe' }}</h2>
      <span class="muted">{{ props.mode === 'create' ? 'nuova' : `ID #${props.item.id}` }}</span>
    </header>

    <div v-if="loading" class="state">Caricamento…</div>

    <template v-else>
      <label class="field">
        <span class="lbl">Nome</span>
        <input v-model.trim="form.nome" type="text" :disabled="disabledAll" required/>
      </label>

      <div v-if="cards.has('NOME_EN') || cards.has('MANUALE')" class="rank-grid">
        <label v-if="cards.has('NOME_EN')" class="field">
          <span class="lbl">Nome originale (EN)</span>
          <input v-model.trim="form.enName" type="text" :disabled="disabledAll" placeholder="Nome originale in inglese"/>
        </label>
        <label v-if="cards.has('MANUALE')" class="field">
          <span class="lbl">Manuale</span>
          <input v-model.trim="form.manuale" type="text" :disabled="disabledAll" placeholder="Manuale di provenienza"/>
        </label>
      </div>

      <div class="rank-grid">
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
        <span class="lbl">Descrizione</span>
        <HtmlEditor v-model="form.descrizione" :rows="4" :disabled="disabledAll"/>
      </label>

      <!-- Info Razza (solo tipo RAZZA): campi puramente descrittivi -->
      <section v-if="isRazza && cards.has('CLASSE_INFO_RAZZA')" class="fold">
        <button type="button" class="fold-head" @click="open.infoRazza = !open.infoRazza">
          <span class="fold-title">Info Razza</span>
          <span class="fold-summary">{{ sumInfoRazza }}</span>
          <span class="chev" :class="{ open: open.infoRazza }">▸</span>
        </button>
        <div v-show="open.infoRazza" class="fold-body">
          <CardInfoRazza
              v-model:taglia="form.razzaTaglia" v-model:velocita="form.razzaVelocita"
              v-model:caratteristiche="form.razzaCaratteristiche" v-model:lap="form.razzaLap"
              v-model:spazio="form.razzaSpazio" v-model:portata="form.razzaPortata"
              v-model:eta="form.razzaEta" :show-eta="cards.has('CLASSE_ETA')"
              :disabled="disabledAll"/>
        </div>
      </section>

      <!-- Abilità di classe -->
      <section v-if="cards.has('CLASSE_ABILITA')" class="fold">
        <button type="button" class="fold-head" @click="open.abilita = !open.abilita">
          <span class="fold-title">Abilità di classe</span>
          <span class="fold-summary">{{ form.abilitaClasse.length }} selezionate</span>
          <span class="chev" :class="{ open: open.abilita }">▸</span>
        </button>
        <div v-show="open.abilita" class="fold-body">
          <CardAbilitaClasse
              v-model:rank1="form.rank1" v-model:rank="form.rank"
              :abilita-classe="form.abilitaClasse" :ab-personaggio="abPersonaggio" :ab-esclusa-cap="abEsclusaCap"
              :stats="stats" :disabled="disabledAll"/>
        </div>
      </section>

      <!-- Incantesimi: sezioni incantatore -->
      <section v-if="cards.has('CLASSE_INCANTESIMI')" class="fold">
        <button type="button" class="fold-head" @click="open.incantesimi = !open.incantesimi">
          <span class="fold-title">Incantesimi</span>
          <span class="fold-summary">{{ incantatore ? `${form.sezioni.length} sezioni` : 'non incantatore' }}</span>
          <span class="chev" :class="{ open: open.incantesimi }">▸</span>
        </button>
        <div v-show="open.incantesimi" class="fold-body">
          <CardIncantesimiClasse
              :sezioni="form.sezioni" :num-livelli="form.numLivelli" :stats="stats"
              :liste-abilitate-mondo="listeAbilitateMondo" :disabled="disabledAll"/>
        </div>
      </section>

      <!-- Generatore + tabella livelli -->
      <section v-if="cards.has('CLASSE_TABELLA_LIVELLI')" class="fold">
        <button type="button" class="fold-head" @click="open.tabella = !open.tabella">
          <span class="fold-title">Tabella livelli</span>
          <span class="fold-summary">{{ livelliCompilati }}/{{ form.numLivelli }} compilati</span>
          <span class="chev" :class="{ open: open.tabella }">▸</span>
        </button>
        <div v-show="open.tabella" class="fold-body">
          <CardTabellaLivelli
              :num-livelli="form.numLivelli" @update:num-livelli="form.numLivelli = $event"
              v-model:dv="form.dv" :livelli="form.livelli" :disabled="disabledAll"/>
        </div>
      </section>

      <!-- Privilegi di Classe -->
      <section v-if="cards.has('CLASSE_PRIVILEGI')" class="fold">
        <button type="button" class="fold-head" @click="open.concesse = !open.concesse">
          <span class="fold-title">Privilegi di Classe</span>
          <span class="fold-summary">{{ form.abilitaConcesse.length }}</span>
          <span class="chev" :class="{ open: open.concesse }">▸</span>
        </button>
        <div v-show="open.concesse" class="fold-body">
          <CardPrivilegiClasse
              :abilita-concesse="form.abilitaConcesse" :num-livelli="form.numLivelli"
              :disabled="disabledAll" :salva-classe="persistiClasseInline"/>
        </div>
      </section>

      <!-- Item collegati: card generica, disponibile anche per CLASSE/RAZZA come per qualunque altro tipo -->
      <section v-if="cards.has('ITEM_COLLEGATI')" class="fold">
        <button type="button" class="fold-head" @click="open.children = !open.children">
          <span class="fold-title">Item collegati</span>
          <span class="fold-summary">{{ form.children.length }}</span>
          <span class="chev" :class="{ open: open.children }">▸</span>
        </button>
        <div v-show="open.children" class="fold-body">
          <ChildrenEditor v-model="form.children" :disabled="disabledAll" :exclude-id="props.item.id"/>
        </div>
      </section>

      <!-- Scelte: card generica, disponibile anche per CLASSE/RAZZA come per qualunque altro tipo -->
      <section v-if="cards.has('SCELTE')" class="fold">
        <button type="button" class="fold-head" @click="open.scelte = !open.scelte">
          <span class="fold-title">Scelte</span>
          <span class="fold-summary">{{ form.scelte.length }}</span>
          <span class="chev" :class="{ open: open.scelte }">▸</span>
        </button>
        <div v-show="open.scelte" class="fold-body">
          <CardScelte :sezioni="form.scelte" :exclude-id="props.item.id" :disabled="disabledAll"/>
        </div>
      </section>

      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

      <div class="actions">
        <button type="button" class="btn ghost" @click="emit('cancel')" :disabled="busy">Annulla</button>
        <span v-if="salvato" class="salvato">Salvato</span>
        <button type="button" class="btn icona" :disabled="!canSave" title="Salva e resta qui"
                aria-label="Salva e resta qui" @click="onSalvaResta"><Icona name="SALVA"/></button>
        <button type="submit" class="btn primary" :disabled="!canSave">
          {{ busy ? 'Salvataggio…' : 'Salva classe' }}
        </button>
      </div>
    </template>
  </form>
</template>

<style scoped>
.classe-editor { display: grid; gap: .75rem; }

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
.slot-list { display: grid; gap: .25rem; max-height: 16rem; overflow-y: auto; }
.slot-row { display: grid; grid-template-columns: 2rem 1fr; gap: .4rem; align-items: center; }
.slot-liv { font-weight: 700; font-size: .8rem; color: var(--info-text); text-align: center; }
.ab-tools { display: flex; gap: .4rem; align-items: center; }
.ab-tools .grow { flex: 1; }
.btn.sm { padding: .3rem .6rem; font-size: .8rem; }

.ce-head { display: flex; align-items: baseline; gap: .5rem; }
.ce-head h2 { margin: 0; font-size: 1rem; }
.muted { opacity: .7; font-size: .85rem; }

.field { display: grid; gap: .3rem; }
.lbl { font-size: .8rem; font-weight: 600; opacity: .85; }
.checkbox-field { grid-auto-flow: column; justify-content: start; align-items: center; gap: .5rem; }
.checkbox-field input[type="checkbox"] { width: auto; }

input[type="text"], input[type="number"], input:not([type]), textarea, select {
  width: 100%; min-width: 0; padding: .5rem .6rem;
  border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0);
}
textarea { resize: vertical; }

.fold { border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0); }
.fold-head {
  width: 100%; display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: .5rem;
  padding: .5rem .75rem; background: var(--btn-bg); border: 0; border-bottom: 1px solid var(--hairline);
  cursor: pointer; text-align: left;
}
.fold-title { font-weight: 600; }
.fold-summary {
  color: var(--text-muted); opacity: .8; white-space: nowrap; overflow: hidden;
  text-overflow: ellipsis; text-align: right; font-size: .85rem;
}
.chev { transition: transform .15s ease; }
.chev.open { transform: rotate(90deg); }
.fold-body { padding: .6rem .75rem; display: grid; gap: .5rem; }

/* abilità di classe — lista a righe (stile trasformazioni) */
.hint-pg { margin: 0; }
.ab-list {
  display: grid; gap: .3rem; max-height: 18rem; overflow-y: auto;
  padding: .15rem; border: 1px solid var(--hairline); border-radius: .5rem;
}
.ab-famiglie {
  max-height: none; overflow: visible; margin-bottom: .5rem;
  border-style: dashed; background: var(--primary-color);
}
.ab-riga {
  display: flex; align-items: center; gap: .4rem;
  border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0); padding: .15rem .35rem;
}
.ab-riga.sel { border-color: #c7d2fe; background: #eef2ff; }
.ab-toggle {
  flex: 1; display: flex; align-items: center; gap: .5rem;
  border: 0; background: transparent; cursor: pointer; text-align: left;
  padding: .35rem .25rem; font-size: .9rem; min-width: 0;
}
.ab-toggle .dot { font-size: .9rem; color: #6366f1; width: 1rem; text-align: center; }
.ab-riga.sel .ab-toggle .dot { color: #4338ca; }
.ab-nome { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ab-pg {
  flex: 0 0 auto; border: 1px solid var(--info-border); background: var(--surface-0); color: var(--info-text);
  border-radius: 1rem; padding: .1rem .55rem; font-size: .75rem; font-weight: 700; cursor: pointer;
}
.ab-pg.on { background: #4338ca; border-color: #4338ca; color: #fff; }
.ab-cap {
  flex: 0 0 auto; border: 1px solid var(--accent-pink-border); background: var(--surface-0); color: var(--accent-pink-text);
  border-radius: 1rem; padding: .1rem .55rem; font-size: .75rem; font-weight: 700; cursor: pointer;
}
.ab-cap.on { background: #9d174d; border-color: #9d174d; color: #fff; }
.ab-toggle:disabled, .ab-pg:disabled, .ab-cap:disabled { opacity: .6; cursor: default; }

/* generatore */
.gen { display: grid; gap: .5rem; border: 1px dashed var(--hairline); border-radius: .5rem; padding: .5rem; background: var(--primary-color); }
.gen-grid { display: grid; grid-template-columns: 1fr 1fr; gap: .4rem; }

/* tabella livelli */
.liv-list { display: grid; gap: .4rem; }
.liv-card {
  display: grid; grid-template-columns: 2rem 1fr; gap: .5rem; align-items: start;
  border: 1px solid var(--hairline); border-radius: .5rem; padding: .4rem .5rem;
}
.liv-num {
  font-weight: 800; font-size: .95rem; color: var(--info-text);
  background: var(--info-bg); border-radius: .4rem; text-align: center; padding: .3rem 0;
}
.liv-fields { display: grid; grid-template-columns: repeat(3, 1fr); gap: .3rem; }
.liv-fields label { display: grid; gap: .1rem; min-width: 0; }
.liv-fields label.full { grid-column: 1 / -1; }
.liv-fields span { font-size: .65rem; font-weight: 700; opacity: .7; }
.liv-fields input { padding: .3rem .4rem; font-size: .85rem; }

/* abilità concesse */
.conc-row {
  display: grid; grid-template-columns: auto 1fr auto auto auto; gap: .4rem; align-items: center;
  border: 1px solid var(--hairline); border-radius: .5rem; padding: .35rem .5rem; background: var(--surface-0);
}
.conc-row .nome {
  white-space: normal !important; word-break: break-word; overflow: visible !important;
  text-overflow: unset !important; font-weight: 600;
}
.conc-row .qty-input {
  width: 2.4rem !important; min-width: 0; padding: .25rem .2rem !important;
  border: 1px solid var(--hairline); border-radius: .4rem; text-align: center; font-size: .8rem;
}
.btn-edit {
  border: 1px solid var(--info-border); background: var(--info-bg); color: var(--info-text);
  border-radius: .5rem; padding: .25rem .5rem; cursor: pointer;
}
.btn-edit:hover { background: #dbeafe; }
.btn-edit:disabled { opacity: .6; cursor: default; }
.conc-row .nome { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-weight: 600; }
.new-chip {
  display: inline-block; margin-right: .35rem;
  background: #16a34a; color: #fff; font-size: .65rem; font-weight: 800;
  border-radius: .35rem; padding: .05rem .35rem; vertical-align: middle;
}
.liv-pill {
  font-size: .72rem; padding: .1rem .45rem; border-radius: .5rem;
  background: var(--success-bg); color: var(--success-text); font-weight: 700; white-space: nowrap;
}
.conc-add { display: grid; grid-template-columns: 5rem 1fr; gap: .4rem; align-items: end; }
.conc-add .grow { min-width: 0; }

.adv-toggle { display: flex; align-items: center; gap: .5rem; font-size: .85rem; font-weight: 600; margin-bottom: .3rem; cursor: pointer; }
.adv-toggle-label { transition: color .15s; }
.switch { position: relative; display: inline-flex; flex-shrink: 0; width: 2.4rem; height: 1.3rem; }
.switch input { position: absolute; inset: 0; opacity: 0; margin: 0; cursor: pointer; z-index: 1; }
.switch-track {
  position: absolute; inset: 0; border-radius: 999px; background: var(--btn-bg);
  transition: background-color .15s;
}
.switch-thumb {
  position: absolute; top: .15rem; left: .15rem; width: 1rem; height: 1rem;
  border-radius: 50%; background: var(--surface-0); box-shadow: 0 1px 2px rgba(0,0,0,.3);
  transition: transform .15s;
}
.switch input:checked + .switch-track { background: #2563eb; }
.switch input:checked + .switch-track .switch-thumb { transform: translateX(1.1rem); }
.switch input:disabled + .switch-track { opacity: .6; cursor: default; }
.conc-item { display: flex; flex-direction: column; gap: .3rem; }
.conc-adv {
  border: 1px dashed var(--info-border); border-radius: .5rem; padding: .5rem; background: var(--primary-color);
  display: flex; flex-direction: column; gap: .5rem;
}
.conc-adv .liv-input { max-width: 6rem; }
.conc-adv-top { display: flex; gap: .6rem; }
.conc-adv-top .gruppo-input { max-width: 12rem; }
.conc-adv-body { display: flex; gap: .6rem; align-items: flex-start; }
.conc-adv-body .grow { flex: 1; min-width: 0; }
.conc-adv-flags {
  flex: 0 0 auto; display: flex; flex-direction: column; gap: .3rem;
  border: 1px solid var(--hairline); border-radius: .5rem; padding: .5rem; background: var(--surface-0); min-width: 9rem;
}
.chk-row { display: flex; align-items: center; gap: .4rem; font-size: .8rem; }
.conc-adv-actions { display: flex; justify-content: flex-end; }
@media (max-width: 640px) { .conc-adv-body { flex-direction: column; } }

.results {
  list-style: none; margin: 0; padding: 0;
  border: 1px solid var(--hairline); border-radius: .5rem; overflow: hidden;
  max-height: 14rem; overflow-y: auto;
}
.results li + li { border-top: 1px solid var(--hairline); }
.result {
  width: 100%; display: grid; grid-template-columns: 1fr auto auto; gap: .4rem; align-items: center;
  padding: .4rem .5rem; background: var(--surface-0); border: 0; cursor: pointer; text-align: left;
}
.result:hover { background: var(--surface-hover); }
.result .nome { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.plus { color: #2563eb; font-weight: 700; }

.btn-del {
  border: 1px solid var(--danger-border); background: var(--danger-bg); color: var(--danger-text);
  border-radius: .5rem; padding: .25rem .5rem; cursor: pointer;
}

.state { padding: .5rem; opacity: .7; }

.error {
  margin: 0; padding: .5rem .75rem; border-radius: .5rem;
  color: var(--danger-text); background: var(--danger-bg); border: 1px solid var(--danger-border); font-size: .85rem;
}

.actions {
  position: sticky; bottom: 0; background: var(--surface-0);
  padding: .5rem 0 calc(.5rem + env(safe-area-inset-bottom, 0px));
  border-top: 1px solid var(--hairline);
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
.btn.outline { border-color: var(--info-border); background: var(--info-bg); color: var(--info-text); font-weight: 600; justify-self: start; }
.btn.primary { background: #2563eb; color: white; }
.btn:disabled { opacity: .6; cursor: default; }
</style>
