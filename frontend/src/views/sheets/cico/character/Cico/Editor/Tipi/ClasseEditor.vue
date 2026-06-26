<script setup lang="ts">
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {Stat} from '../../../../../../../models/entity/Stat'
import {Item} from '../../../../../../../models/dto/Item'
import api from '../../../../../../../service/api'
import {getStats, searchItems} from '../../../../../../../service/PersonaggioService'
import HtmlEditor from '../../../../../../../components/HtmlEditor.vue'
import {SPELL_LIST_CODES, spellListLabel} from '../../../../../../../function/spellLists'
import SearchSelect from '../../../../../../../components/SearchSelect.vue'

const props = defineProps<{ item: ItemDB; readonly?: boolean; mode?: 'edit' | 'create' }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void }>()

const router = useRouter()
const route = useRoute()

function editConcessa(itemId: number) {
  const idPg = route.query.personaggio
  router.push(`/itemeditor/${itemId}` + (idPg ? `?personaggio=${idPg}` : ''))
}

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
}

const form = reactive({
  nome: '',
  enName: '',
  manuale: '',
  idMondo: 1,
  idSistema: 1,
  descrizione: '',
  abilitaClasse: [] as string[],
  spellList: '',
  spellSlotBonus: '',
  // sezioni incantatore: ognuna 1..N liste (unite) + progressione + formula bonus + slot custom
  sezioni: [] as Array<{ liste: string[]; progressione: string; bonus: string; slot: string[] }>,
  rank1: '',
  rank: '',
  numLivelli: 20,
  dv: '',
  livelli: Array.from({length: 20}, (_, i) => ({
    livello: i + 1, bab: '', tmp: '', rfl: '', vlt: '', spSlot: '',
  })) as LivelloClasse[],
  abilitaConcesse: [] as AbilitaConcessa[],
})

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
function clampNumLivelli() {
  const n = Math.max(1, Math.min(100, Math.floor(Number(form.numLivelli) || 20)))
  form.numLivelli = n
  ensureLivelli(n)
}
watch(() => form.numLivelli, clampNumLivelli)
const livelliVisibili = computed<LivelloClasse[]>(() => form.livelli.slice(0, form.numLivelli))

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
    form.descrizione = d.descrizione ?? ''
    const tokensAb: string[] = d.abilitaClasse ?? []
    form.abilitaClasse = tokensAb.map(t => t.replace('!', '').trim()).filter(Boolean)
    abPersonaggio.value = new Set(
        tokensAb.filter(t => t.includes('!')).map(t => t.replace('!', '').trim()).filter(Boolean)
    )
    form.spellList = d.spellList ?? ''
    form.spellSlotBonus = d.spellSlotBonus ?? ''
    form.sezioni = (d.sezioniIncantesimi ?? []).map((s: any) => ({
      liste: Array.isArray(s.liste) ? s.liste : (s.liste ? [s.liste] : []),
      progressione: s.progressione ?? 'CUSTOM',
      bonus: s.bonus ?? '',
      slot: Array.isArray(s.slot) ? s.slot.slice() : [],
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
      livello: a.livello, itemId: a.itemId, nome: a.nome, tipo: a.tipo,
    }))
  } catch (e) {
    errorMsg.value = 'Errore nel caricamento della classe'
    console.error('Errore caricamento classe:', e)
  } finally {
    loading.value = false
  }
})

/* ---- abilità di classe (multi-selezione dalle stat) ---- */
const stats = ref<Stat[]>([])
const filtroAbilita = ref('')
// id abilità marcate come "abilità personaggio" (serializzate con "!"):
// valgono anche nei livelli che non usano questa classe
const abPersonaggio = ref<Set<string>>(new Set())

async function loadStats() {
  try {
    stats.value = await getStats()
  } catch (e) {
    console.error('Errore caricamento stats:', e)
  }
}

const abilitaDisponibili = computed(() =>
    stats.value
        .filter(s => s.tipo === 'AB')
        .filter(s => !filtroAbilita.value.trim()
            || s.label.toLowerCase().includes(filtroAbilita.value.trim().toLowerCase()))
)

function isSelected(id: string): boolean {
  return form.abilitaClasse.includes(id)
}

function isPersonaggio(id: string): boolean {
  return abPersonaggio.value.has(id)
}

function toggleAbilita(id: string) {
  const i = form.abilitaClasse.indexOf(id)
  if (i >= 0) {
    form.abilitaClasse.splice(i, 1)
    abPersonaggio.value.delete(id) // deselezionando, perde anche il flag personaggio
  } else {
    form.abilitaClasse.push(id)
  }
}

function togglePersonaggio(id: string) {
  if (!isSelected(id)) return
  if (abPersonaggio.value.has(id)) abPersonaggio.value.delete(id)
  else abPersonaggio.value.add(id)
}

function statLabel(id: string): string {
  return stats.value.find(s => s.id === id)?.label ?? id
}

/* ---- generatori progressioni standard ---- */
const gen = reactive({
  bab: 'MEDIO' as 'ALTO' | 'MEDIO' | 'BASSO',
  tmp: 'BUONO' as 'BUONO' | 'SCARSO',
  rfl: 'SCARSO' as 'BUONO' | 'SCARSO',
  vlt: 'BUONO' as 'BUONO' | 'SCARSO',
})

function babPer(l: number): number {
  if (gen.bab === 'ALTO') return l
  if (gen.bab === 'MEDIO') return Math.floor(l * 3 / 4)
  return Math.floor(l / 2)
}

function tsPer(l: number, tipo: 'BUONO' | 'SCARSO'): number {
  return tipo === 'BUONO' ? 2 + Math.floor(l / 2) : Math.floor(l / 3)
}

function generaTabella() {
  for (const row of livelliVisibili.value) {
    const l = row.livello
    row.bab = `+${babPer(l)}`
    row.tmp = `+${tsPer(l, gen.tmp)}`
    row.rfl = `+${tsPer(l, gen.rfl)}`
    row.vlt = `+${tsPer(l, gen.vlt)}`
  }
}

/* ---- abilità concesse ---- */
const queryConcessa = ref('')
const livelloConcessa = ref(1)
const risultatiConcessa = ref<Item[]>([])
const searching = ref(false)
let searchToken = 0
let debounceTimer: any = null

function onQueryConcessa() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(async () => {
    const q = queryConcessa.value.trim()
    if (q.length < 2) {
      risultatiConcessa.value = []
      return
    }
    const token = ++searchToken
    searching.value = true
    try {
      const res = await searchItems(q)
      if (token !== searchToken) return
      risultatiConcessa.value = (res.data ?? []).filter(r =>
          !['ATTACCO', 'AVANZAMENTO', 'CLASSE', 'LIVELLO'].includes(r.tipo))
    } catch (e) {
      console.error('Errore ricerca:', e)
    } finally {
      if (token === searchToken) searching.value = false
    }
  }, 250)
}

function aggiungiConcessa(itm: Item) {
  const l = Math.min(20, Math.max(1, Math.floor(Number(livelloConcessa.value) || 1)))
  if (form.abilitaConcesse.some(a => a.itemId === itm.id && a.livello === l)) return
  form.abilitaConcesse.push({livello: l, itemId: itm.id, nome: itm.nome, tipo: itm.tipo})
  form.abilitaConcesse.sort((a, b) => a.livello - b.livello || a.nome.localeCompare(b.nome))
  risultatiConcessa.value = []
  queryConcessa.value = ''
}

// Privilegio "nuovo": creato come PRIVILEGIO al salvataggio della classe
function aggiungiNuovo() {
  const nome = queryConcessa.value.trim()
  if (!nome) return
  const l = Math.min(20, Math.max(1, Math.floor(Number(livelloConcessa.value) || 1)))
  form.abilitaConcesse.push({livello: l, itemId: null, nome, tipo: 'PRIVILEGIO', nuovo: true})
  form.abilitaConcesse.sort((a, b) => a.livello - b.livello || a.nome.localeCompare(b.nome))
  risultatiConcessa.value = []
  queryConcessa.value = ''
}

// mostra l'opzione "NEW" se c'è testo e nessun risultato ha esattamente quel nome
const mostraNuovo = computed(() => {
  const q = queryConcessa.value.trim().toLowerCase()
  if (q.length < 2) return false
  return !risultatiConcessa.value.some(r => (r.nome ?? '').toLowerCase() === q)
})

function rimuoviConcessa(i: number) {
  form.abilitaConcesse.splice(i, 1)
}

/* ---- salvataggio ---- */
async function onSave() {
  if (!canSave.value) return
  busy.value = true
  errorMsg.value = null
  try {
    const payload = {
      id: props.mode === 'create' ? null : props.item.id,
      nome: form.nome.trim(),
      enName: form.enName.trim() || null,
      manuale: form.manuale.trim() || null,
      idMondo: props.mode === 'create' ? (Number(form.idMondo) || null) : null,
      idSistema: props.mode === 'create' ? (Number(form.idSistema) || null) : null,
      descrizione: form.descrizione || null,
      abilitaClasse: form.abilitaClasse.map(id => abPersonaggio.value.has(id) ? `${id}!` : id),
      spellList: null,
      spellSlotBonus: null,
      sezioniIncantesimi: form.sezioni
          .map(s => {
            const prog = (s.progressione || 'CUSTOM').trim()
            const slot = prog === 'CUSTOM'
                ? s.slot.slice(0, form.numLivelli).map(x => (x ?? '').trim())
                : []
            return {
              liste: (s.liste ?? []).map(x => x.trim()).filter(Boolean),
              progressione: prog,
              bonus: s.bonus.trim() || null,
              slot: slot.some(x => x) ? slot : null,
            }
          })
          .filter(s => s.liste.length > 0),
      rank1: form.rank1.trim() || null,
      rank: form.rank.trim() || null,
      dv: form.dv.trim() || null,
      numLivelli: form.numLivelli,
      livelli: form.livelli.slice(0, form.numLivelli),
      abilitaConcesse: form.abilitaConcesse.map(a => ({livello: a.livello, itemId: a.itemId, nome: a.nome})),
    }
    await api.post('/item/classe', payload)
    emit('saved')
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 403
        ? 'Non hai i permessi'
        : 'Errore nel salvataggio della classe'
    console.error('Errore salvataggio classe:', e)
  } finally {
    busy.value = false
  }
}

const PROGRESSIONI = ['CUSTOM', 'MAGO', 'STREGONE', 'CHIERICO', 'DRUIDO', 'BARDO', 'RANGER', 'PALADINO']

function addSezione() {
  form.sezioni.push({liste: [], progressione: 'CUSTOM', bonus: '', slot: []})
}
function removeSezione(i: number) {
  form.sezioni.splice(i, 1)
}
function addLista(s: { liste: string[] }, code: string) {
  if (code && !s.liste.includes(code)) s.liste.push(code)
}
function removeLista(s: { liste: string[] }, code: string) {
  const i = s.liste.indexOf(code)
  if (i >= 0) s.liste.splice(i, 1)
}
// liste non ancora selezionate (per la tendina "aggiungi")
function listeDisponibili(s: { liste: string[] }): string[] {
  return SPELL_LIST_CODES.filter(c => !s.liste.includes(c))
}
// assicura la riga slot per il livello di classe (1-based)
function slotDi(s: { slot: string[] }, livello: number): string {
  return s.slot[livello - 1] ?? ''
}
function setSlot(s: { slot: string[] }, livello: number, val: string) {
  while (s.slot.length < livello) s.slot.push('')
  s.slot[livello - 1] = val
}

const incantatore = computed(() => form.sezioni.some(s => s.liste.length > 0))
// abilità di classe: tutte le stat di tipo AB
function tutteAbIds(): string[] {
  return stats.value.filter(s => s.tipo === 'AB').map(s => s.id)
}
function selezionaTutteAbilita() {
  form.abilitaClasse = tutteAbIds()
}
// seleziona tutte e marca anche PG su tutte (long-press su "Tutte")
function selezionaTuttePg() {
  const ids = tutteAbIds()
  form.abilitaClasse = ids
  abPersonaggio.value = new Set(ids)
}
function deselezionaTutteAbilita() {
  form.abilitaClasse = []
  abPersonaggio.value = new Set()
}

// long-press sul pulsante "Tutte": click = solo abilità, hold = anche PG
let lpTimer: any = null
let lpFired = false
function lpStart() {
  if (disabledAll.value) return
  lpFired = false
  lpTimer = setTimeout(() => { lpFired = true; selezionaTuttePg() }, 550)
}
function lpEnd() {
  if (lpTimer) { clearTimeout(lpTimer); lpTimer = null }
}
function lpClick() {
  if (lpFired) { lpFired = false; return } // il hold ha già agito
  selezionaTutteAbilita()
}

/* sezioni richiudibili */
const open = reactive({abilita: false, incantesimi: false, tabella: false, concesse: false})
</script>

<template>
  <form class="classe-editor" @submit.prevent="onSave">
    <header class="ce-head">
      <h2>Classe</h2>
      <span class="muted">{{ props.mode === 'create' ? 'nuova' : `ID #${props.item.id}` }}</span>
    </header>

    <div v-if="loading" class="state">Caricamento…</div>

    <template v-else>
      <label class="field">
        <span class="lbl">Nome</span>
        <input v-model.trim="form.nome" type="text" :disabled="disabledAll" required/>
      </label>

      <div class="rank-grid">
        <label class="field">
          <span class="lbl">Nome originale (EN)</span>
          <input v-model.trim="form.enName" type="text" :disabled="disabledAll" placeholder="Nome originale in inglese"/>
        </label>
        <label class="field">
          <span class="lbl">Manuale</span>
          <input v-model.trim="form.manuale" type="text" :disabled="disabledAll" placeholder="Manuale di provenienza"/>
        </label>
      </div>

      <div v-if="props.mode === 'create'" class="rank-grid">
        <label class="field">
          <span class="lbl">Mondo (id)</span>
          <input v-model.number="form.idMondo" type="number" min="1" step="1" :disabled="disabledAll"/>
        </label>
        <label class="field">
          <span class="lbl">Sistema (id)</span>
          <input v-model.number="form.idSistema" type="number" min="1" step="1" :disabled="disabledAll"/>
        </label>
      </div>

      <label class="field">
        <span class="lbl">Descrizione</span>
        <HtmlEditor v-model="form.descrizione" :rows="4" :disabled="disabledAll"/>
      </label>

      <!-- Abilità di classe -->
      <section class="fold">
        <button type="button" class="fold-head" @click="open.abilita = !open.abilita">
          <span class="fold-title">Abilità di classe</span>
          <span class="fold-summary">{{ form.abilitaClasse.length }} selezionate</span>
          <span class="chev" :class="{ open: open.abilita }">▸</span>
        </button>
        <div v-show="open.abilita" class="fold-body">
          <!-- Gradi (abilità): formule a livello di classe -->
          <div class="rank-grid">
            <label class="field">
              <span class="lbl">Gradi al 1° livello del personaggio (RANK_1)</span>
              <input v-model.trim="form.rank1" type="text" placeholder="Es.: 4*(@INT+4)" :disabled="disabledAll"/>
            </label>
            <label class="field">
              <span class="lbl">Gradi agli altri livelli (RANK)</span>
              <input v-model.trim="form.rank" type="text" placeholder="Es.: (@INT+4)" :disabled="disabledAll"/>
            </label>
          </div>

          <div class="ab-tools">
            <input v-model="filtroAbilita" type="text" placeholder="Filtra abilità…" :disabled="disabledAll" class="grow"/>
            <button type="button" class="btn outline sm" :disabled="disabledAll"
                    title="Click: tutte. Tieni premuto: tutte + PG"
                    @click="lpClick"
                    @mousedown="lpStart" @mouseup="lpEnd" @mouseleave="lpEnd"
                    @touchstart.passive="lpStart" @touchend="lpEnd" @touchcancel="lpEnd">Tutte</button>
            <button type="button" class="btn outline sm" :disabled="disabledAll" @click="deselezionaTutteAbilita">Nessuna</button>
          </div>
          <p class="muted hint-pg">
            <strong>PG</strong> = abilità personaggio: vale anche nei livelli che non usano questa classe.
          </p>
          <div class="ab-list">
            <div v-for="s in abilitaDisponibili" :key="s.id" class="ab-riga" :class="{ sel: isSelected(s.id) }">
              <button type="button" class="ab-toggle" :disabled="disabledAll" @click="toggleAbilita(s.id)">
                <span class="dot">{{ isSelected(s.id) ? '●' : '○' }}</span>
                <span class="ab-nome">{{ s.label }}</span>
              </button>
              <button v-if="isSelected(s.id)" type="button" class="ab-pg" :class="{ on: isPersonaggio(s.id) }"
                      :disabled="disabledAll" @click="togglePersonaggio(s.id)"
                      title="Abilità personaggio: vale anche nei livelli di altre classi">
                PG
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- Incantesimi: sezioni incantatore -->
      <section class="fold">
        <button type="button" class="fold-head" @click="open.incantesimi = !open.incantesimi">
          <span class="fold-title">Incantesimi</span>
          <span class="fold-summary">{{ incantatore ? `${form.sezioni.length} sezioni` : 'non incantatore' }}</span>
          <span class="chev" :class="{ open: open.incantesimi }">▸</span>
        </button>
        <div v-show="open.incantesimi" class="fold-body">
          <p class="muted">
            Ogni <strong>sezione</strong> ha una o più liste (sempre unite) e una progressione di slot.
            Per tenere liste separate, crea più sezioni.
          </p>

          <div v-for="(s, i) in form.sezioni" :key="i" class="sez-card">
            <div class="sez-head">
              <span class="sez-title">Sezione {{ i + 1 }}</span>
              <button type="button" class="btn-del" :disabled="disabledAll" @click="removeSezione(i)" title="Rimuovi">✕</button>
            </div>

            <div class="field">
              <span class="lbl">Liste incantesimi (unite in questa sezione)</span>
              <div v-if="s.liste.length" class="chips">
                <span v-for="code in s.liste" :key="code" class="chip">
                  {{ spellListLabel(code) }}
                  <button type="button" class="chip-x" :disabled="disabledAll" @click="removeLista(s, code)">✕</button>
                </span>
              </div>
              <SearchSelect :model-value="''" :disabled="disabledAll" placeholder="+ Aggiungi lista…"
                            :options="listeDisponibili(s).map(c => ({value: c, label: `${spellListLabel(c)} (${c})`}))"
                            @update:model-value="addLista(s, $event as string)"/>
            </div>

            <div class="rank-grid">
              <label class="field">
                <span class="lbl">Progressione</span>
                <SearchSelect v-model="s.progressione" :disabled="disabledAll"
                              :options="PROGRESSIONI" :sort="false"/>
              </label>
              <label class="field">
                <span class="lbl">Formula slot bonus</span>
                <input v-model.trim="s.bonus" type="text" placeholder="Es.: 1+(@SAG-#L)/4)" :disabled="disabledAll"/>
              </label>
            </div>

            <div v-if="(s.progressione || 'CUSTOM') === 'CUSTOM'" class="field">
              <span class="lbl">Slot per livello (CUSTOM) — formato "4,2,1,…" dal liv. 0 al 9</span>
              <div class="slot-list">
                <div v-for="l in form.numLivelli" :key="l" class="slot-row">
                  <span class="slot-liv">{{ l }}</span>
                  <input type="text" :value="slotDi(s, l)" placeholder="4,2,1,0,0,0,0,0,0,0"
                         :disabled="disabledAll" @input="setSlot(s, l, ($event.target as HTMLInputElement).value)"/>
                </div>
              </div>
            </div>
          </div>

          <button type="button" class="btn outline" :disabled="disabledAll" @click="addSezione">+ Aggiungi sezione</button>
        </div>
      </section>

      <!-- Generatore + tabella livelli -->
      <section class="fold">
        <button type="button" class="fold-head" @click="open.tabella = !open.tabella">
          <span class="fold-title">Tabella livelli</span>
          <span class="fold-summary">{{ livelliVisibili.filter(l => l.bab).length }}/{{ form.numLivelli }} compilati</span>
          <span class="chev" :class="{ open: open.tabella }">▸</span>
        </button>
        <div v-show="open.tabella" class="fold-body">
          <div class="rank-grid">
            <label class="field">
              <span class="lbl">Livelli classe</span>
              <input v-model.number="form.numLivelli" type="number" min="1" max="100" :disabled="disabledAll"/>
              <span class="muted">Quanti livelli ha la classe (default 20).</span>
            </label>
            <label class="field">
              <span class="lbl">Dadi vita</span>
              <input v-model.trim="form.dv" type="text" placeholder="Es.: 2d10 — vuoto = nessuno" :disabled="disabledAll"/>
              <span class="muted">Impostato a ogni livello preso in questa classe. Vuoto = la classe non dà dadi vita.</span>
            </label>
          </div>

          <!-- generatore -->
          <div class="gen">
            <div class="gen-grid">
              <label class="field">
                <span class="lbl">BAB</span>
                <SearchSelect v-model="gen.bab" :disabled="disabledAll" :sort="false"
                              :options="[{value:'ALTO',label:'Alto (guerriero)'},{value:'MEDIO',label:'Medio (chierico)'},{value:'BASSO',label:'Basso (mago)'}]"/>
              </label>
              <label class="field">
                <span class="lbl">Tempra</span>
                <SearchSelect v-model="gen.tmp" :disabled="disabledAll" :sort="false"
                              :options="[{value:'BUONO',label:'Buono'},{value:'SCARSO',label:'Scarso'}]"/>
              </label>
              <label class="field">
                <span class="lbl">Riflessi</span>
                <SearchSelect v-model="gen.rfl" :disabled="disabledAll" :sort="false"
                              :options="[{value:'BUONO',label:'Buono'},{value:'SCARSO',label:'Scarso'}]"/>
              </label>
              <label class="field">
                <span class="lbl">Volontà</span>
                <SearchSelect v-model="gen.vlt" :disabled="disabledAll" :sort="false"
                              :options="[{value:'BUONO',label:'Buono'},{value:'SCARSO',label:'Scarso'}]"/>
              </label>
            </div>
            <button type="button" class="btn outline" :disabled="disabledAll" @click="generaTabella">
              ⚙ Genera tabella livelli
            </button>
          </div>

          <!-- tabella -->
          <div class="liv-list">
            <div v-for="row in livelliVisibili" :key="row.livello" class="liv-card">
              <div class="liv-num">{{ row.livello }}</div>
              <div class="liv-fields">
                <label><span>BAB</span><input v-model.trim="row.bab" :disabled="disabledAll"/></label>
                <label><span>TMP</span><input v-model.trim="row.tmp" :disabled="disabledAll"/></label>
                <label><span>RFL</span><input v-model.trim="row.rfl" :disabled="disabledAll"/></label>
                <label><span>VLT</span><input v-model.trim="row.vlt" :disabled="disabledAll"/></label>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Privilegi di Classe -->
      <section class="fold">
        <button type="button" class="fold-head" @click="open.concesse = !open.concesse">
          <span class="fold-title">Privilegi di Classe</span>
          <span class="fold-summary">{{ form.abilitaConcesse.length }}</span>
          <span class="chev" :class="{ open: open.concesse }">▸</span>
        </button>
        <div v-show="open.concesse" class="fold-body">
          <div v-for="(a, i) in form.abilitaConcesse" :key="`${a.itemId ?? 'new'}-${a.nome}-${a.livello}`" class="conc-row">
            <span class="liv-pill">Liv {{ a.livello }}</span>
            <span class="nome"><span v-if="a.nuovo" class="new-chip">NEW</span>{{ a.nome }}</span>
            <button type="button" class="btn-edit" :disabled="disabledAll || !a.itemId"
                    @click="editConcessa(a.itemId!)" title="Modifica">✎</button>
            <button type="button" class="btn-del" :disabled="disabledAll" @click="rimuoviConcessa(i)">✕</button>
          </div>

          <div class="conc-add">
            <label class="field liv-input">
              <span class="lbl">Liv</span>
              <input v-model.number="livelloConcessa" type="number" min="1" max="20" :disabled="disabledAll"/>
            </label>
            <input class="grow" v-model="queryConcessa" type="text"
                   placeholder="Cerca o scrivi un nuovo privilegio…" :disabled="disabledAll" @input="onQueryConcessa"/>
          </div>
          <div v-if="searching" class="muted">Ricerca…</div>
          <ul v-else-if="risultatiConcessa.length || mostraNuovo" class="results">
            <li v-for="r in risultatiConcessa" :key="r.id">
              <button type="button" class="result" :disabled="disabledAll" @click="aggiungiConcessa(r)">
                <span class="nome">{{ r.nome }}</span>
                <span class="liv-pill">{{ r.tipo }}</span>
                <span class="plus">+</span>
              </button>
            </li>
            <li v-if="mostraNuovo">
              <button type="button" class="result" :disabled="disabledAll" @click="aggiungiNuovo">
                <span class="nome">{{ queryConcessa.trim() }}</span>
                <span class="new-chip">NEW</span>
                <span class="plus">+</span>
              </button>
            </li>
          </ul>
        </div>
      </section>

      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

      <div class="actions">
        <button type="button" class="btn ghost" @click="emit('cancel')" :disabled="busy">Annulla</button>
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

.sez-card { border: 1px solid #e5e7eb; border-radius: .5rem; padding: .5rem; display: grid; gap: .5rem; margin-bottom: .4rem; background: #fafafa; }
.sez-head { display: flex; align-items: center; justify-content: space-between; }
.sez-title { font-weight: 700; font-size: .9rem; }
.chips { display: flex; flex-wrap: wrap; gap: .3rem; margin-bottom: .3rem; }
.chip { display: inline-flex; align-items: center; gap: .3rem; background: #eef2ff; color: #3730a3; border-radius: 1rem; padding: .1rem .5rem; font-size: .8rem; font-weight: 600; }
.chip-x { border: 0; background: transparent; color: #6366f1; cursor: pointer; font-size: .75rem; padding: 0; }
.slot-list { display: grid; gap: .25rem; max-height: 16rem; overflow-y: auto; }
.slot-row { display: grid; grid-template-columns: 2rem 1fr; gap: .4rem; align-items: center; }
.slot-liv { font-weight: 700; font-size: .8rem; color: #3730a3; text-align: center; }
.ab-tools { display: flex; gap: .4rem; align-items: center; }
.ab-tools .grow { flex: 1; }
.btn.sm { padding: .3rem .6rem; font-size: .8rem; }

.ce-head { display: flex; align-items: baseline; gap: .5rem; }
.ce-head h2 { margin: 0; font-size: 1rem; }
.muted { opacity: .7; font-size: .85rem; }

.field { display: grid; gap: .3rem; }
.lbl { font-size: .8rem; font-weight: 600; opacity: .85; }

input[type="text"], input[type="number"], input:not([type]), textarea, select {
  width: 100%; min-width: 0; padding: .5rem .6rem;
  border: 1px solid #d0d5dd; border-radius: .5rem; background: #fff;
}
textarea { resize: vertical; }

.fold { border: 1px solid #e5e7eb; border-radius: .5rem; background: #fff; }
.fold-head {
  width: 100%; display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: .5rem;
  padding: .5rem .75rem; background: #f9fafb; border: 0; border-bottom: 1px solid #e5e7eb;
  cursor: pointer; text-align: left;
}
.fold-title { font-weight: 600; }
.fold-summary {
  color: #374151; opacity: .8; white-space: nowrap; overflow: hidden;
  text-overflow: ellipsis; text-align: right; font-size: .85rem;
}
.chev { transition: transform .15s ease; }
.chev.open { transform: rotate(90deg); }
.fold-body { padding: .6rem .75rem; display: grid; gap: .5rem; }

/* abilità di classe — lista a righe (stile trasformazioni) */
.hint-pg { margin: 0; }
.ab-list {
  display: grid; gap: .3rem; max-height: 18rem; overflow-y: auto;
  padding: .15rem; border: 1px solid #eef2f7; border-radius: .5rem;
}
.ab-riga {
  display: flex; align-items: center; gap: .4rem;
  border: 1px solid #e5e7eb; border-radius: .5rem; background: #fff; padding: .15rem .35rem;
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
  flex: 0 0 auto; border: 1px solid #c7d2fe; background: #fff; color: #4338ca;
  border-radius: 1rem; padding: .1rem .55rem; font-size: .75rem; font-weight: 700; cursor: pointer;
}
.ab-pg.on { background: #4338ca; border-color: #4338ca; color: #fff; }
.ab-toggle:disabled, .ab-pg:disabled { opacity: .6; cursor: default; }

/* generatore */
.gen { display: grid; gap: .5rem; border: 1px dashed #cbd5e1; border-radius: .5rem; padding: .5rem; background: #f8fafc; }
.gen-grid { display: grid; grid-template-columns: 1fr 1fr; gap: .4rem; }

/* tabella livelli */
.liv-list { display: grid; gap: .4rem; }
.liv-card {
  display: grid; grid-template-columns: 2rem 1fr; gap: .5rem; align-items: start;
  border: 1px solid #e5e7eb; border-radius: .5rem; padding: .4rem .5rem;
}
.liv-num {
  font-weight: 800; font-size: .95rem; color: #3730a3;
  background: #eef2ff; border-radius: .4rem; text-align: center; padding: .3rem 0;
}
.liv-fields { display: grid; grid-template-columns: repeat(3, 1fr); gap: .3rem; }
.liv-fields label { display: grid; gap: .1rem; min-width: 0; }
.liv-fields label.full { grid-column: 1 / -1; }
.liv-fields span { font-size: .65rem; font-weight: 700; opacity: .7; }
.liv-fields input { padding: .3rem .4rem; font-size: .85rem; }

/* abilità concesse */
.conc-row {
  display: grid; grid-template-columns: auto 1fr auto auto; gap: .4rem; align-items: center;
  border: 1px solid #e5e7eb; border-radius: .5rem; padding: .35rem .5rem; background: #fff;
}
.btn-edit {
  border: 1px solid #bfdbfe; background: #eff6ff; color: #1d4ed8;
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
  background: #f0fdf4; color: #166534; font-weight: 700; white-space: nowrap;
}
.conc-add { display: grid; grid-template-columns: 5rem 1fr; gap: .4rem; align-items: end; }
.conc-add .grow { min-width: 0; }

.results {
  list-style: none; margin: 0; padding: 0;
  border: 1px solid #e5e7eb; border-radius: .5rem; overflow: hidden;
  max-height: 14rem; overflow-y: auto;
}
.results li + li { border-top: 1px solid #f3f4f6; }
.result {
  width: 100%; display: grid; grid-template-columns: 1fr auto auto; gap: .4rem; align-items: center;
  padding: .4rem .5rem; background: #fff; border: 0; cursor: pointer; text-align: left;
}
.result:hover { background: #f9fafb; }
.result .nome { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.plus { color: #2563eb; font-weight: 700; }

.btn-del {
  border: 1px solid #fecaca; background: #fef2f2; color: #991b1b;
  border-radius: .5rem; padding: .25rem .5rem; cursor: pointer;
}

.state { padding: .5rem; opacity: .7; }

.error {
  margin: 0; padding: .5rem .75rem; border-radius: .5rem;
  color: #991b1b; background: #fef2f2; border: 1px solid #fecaca; font-size: .85rem;
}

.actions {
  position: sticky; bottom: 0; background: #fff;
  padding: .5rem 0 calc(.5rem + env(safe-area-inset-bottom, 0px));
  border-top: 1px solid #e5e7eb;
  display: flex; justify-content: flex-end; gap: .5rem;
}
.btn { padding: .5rem .9rem; border-radius: .5rem; border: 1px solid transparent; cursor: pointer; }
.btn.ghost { border-color: #d0d5dd; background: #fff; }
.btn.outline { border-color: #93c5fd; background: #eff6ff; color: #1d4ed8; font-weight: 600; justify-self: start; }
.btn.primary { background: #2563eb; color: white; }
.btn:disabled { opacity: .6; cursor: default; }
</style>
