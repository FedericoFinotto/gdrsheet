<script setup lang="ts">
import {reactive} from 'vue'
import {Stat} from '../../../../../../../models/entity/Stat'
import {SPELL_LIST_CODES, spellListLabel} from '../../../../../../../function/spellLists'
import SearchSelect from '../../../../../../../components/SearchSelect.vue'

interface Sezione {
  liste: string[]; progressione: string; bonus: string; slot: string[]
  conosciutiSeparati: boolean; conosciuti: string[]; slotConContatore: boolean; caratteristica: string
  casterLevelSorgente: string; slotLivelloSorgente: string
  // "SLOT" (default, comportamento storico) o "LIVELLO" (spontanei: slot[0] è un'unica riga con la
  // soglia di sblocco di ciascun livello di incantesimo, conosciuti è obbligatoria e dà il numero
  // di incantesimi disponibili una volta sbloccato — vedi Constants.SPELL_MODO_* nel backend).
  modo: string
}

const props = defineProps<{
  sezioni: Sezione[]  // reactive array del genitore, mutata per riferimento (push/splice)
  numLivelli: number
  stats: Stat[]
  // liste/domini abilitati per il mondo corrente (null = nessuna restrizione, vedi ClasseEditor)
  listeAbilitateMondo: Set<string> | null
  disabled?: boolean
}>()

const PROGRESSIONI = ['CUSTOM', 'MAGO', 'STREGONE', 'CHIERICO', 'DRUIDO', 'BARDO', 'RANGER', 'PALADINO']
const OPZIONI_MODO = [
  {value: 'SLOT', label: 'A Slot'},
  {value: 'LIVELLO', label: 'A Livello (spontaneo)'},
]
const OPZIONI_CASTER_LEVEL = [
  {value: 'NM', label: 'Caster Level Non Maledetto'},
  {value: 'TOT', label: 'Caster Level Totale'},
]
const OPZIONI_LIVELLO_SLOT = [
  {value: 'MNM', label: 'Livello Massimo Non Maledetto'},
  {value: 'NM', label: 'Livello Totale Non Maledetto'},
  {value: 'TOT', label: 'Livello Totale'},
]

function addSezione() {
  props.sezioni.push({
    liste: [], progressione: 'CUSTOM', bonus: '', slot: [], conosciutiSeparati: false, conosciuti: [],
    slotConContatore: false,
    caratteristica: '', casterLevelSorgente: 'NM', slotLivelloSorgente: 'NM', modo: 'SLOT',
  })
}
// riga unica (non per livello di classe) usata in modalità LIVELLO per la soglia di sblocco
function sbloccoDi(s: { slot: string[] }): string {
  return s.slot[0] ?? ''
}
function setSblocco(s: { slot: string[] }, val: string) {
  s.slot[0] = val
}
function removeSezione(i: number) {
  props.sezioni.splice(i, 1)
}
function addLista(s: { liste: string[] }, code: string) {
  if (code && !s.liste.includes(code)) s.liste.push(code)
}
function removeLista(s: { liste: string[] }, code: string) {
  const i = s.liste.indexOf(code)
  if (i >= 0) s.liste.splice(i, 1)
}
// liste non ancora selezionate (per la tendina "aggiungi"), filtrate su quelle abilitate per il
// mondo della classe (se la config non è ancora nota, nessuna restrizione)
function listeDisponibili(s: { liste: string[] }): string[] {
  return SPELL_LIST_CODES
      .filter(c => !s.liste.includes(c))
      .filter(c => !props.listeAbilitateMondo || props.listeAbilitateMondo.has(c))
}
// codice libero (non nel catalogo SPELL_LIST_CODES), indicizzato per sezione
const customListaCode = reactive<string[]>([])
function confirmCustomLista(s: { liste: string[] }, i: number) {
  const code = (customListaCode[i] ?? '').trim().toUpperCase()
  if (!code) return
  addLista(s, code)
  customListaCode[i] = ''
}
// assicura la riga slot per il livello di classe (1-based)
function slotDi(s: { slot: string[] }, livello: number): string {
  return s.slot[livello - 1] ?? ''
}
function setSlot(s: { slot: string[] }, livello: number, val: string) {
  while (s.slot.length < livello) s.slot.push('')
  s.slot[livello - 1] = val
}
function conosciutiDi(s: { conosciuti: string[] }, livello: number): string {
  return s.conosciuti[livello - 1] ?? ''
}
function setConosciuti(s: { conosciuti: string[] }, livello: number, val: string) {
  while (s.conosciuti.length < livello) s.conosciuti.push('')
  s.conosciuti[livello - 1] = val
}
</script>

<template>
  <p class="muted">
    Ogni <strong>sezione</strong> ha una o più liste (sempre unite) e una progressione di slot.
    Per tenere liste separate, crea più sezioni.
  </p>

  <div v-for="(s, i) in sezioni" :key="i" class="sez-card">
    <div class="sez-head">
      <span class="sez-title">Sezione {{ i + 1 }}</span>
      <button type="button" class="btn-del" :disabled="disabled" @click="removeSezione(i)" title="Rimuovi">✕</button>
    </div>

    <div class="field">
      <span class="lbl">Liste incantesimi (unite in questa sezione)</span>
      <div v-if="s.liste.length" class="chips">
        <span v-for="code in s.liste" :key="code" class="chip">
          {{ spellListLabel(code) }}
          <button type="button" class="chip-x" :disabled="disabled" @click="removeLista(s, code)">✕</button>
        </span>
      </div>
      <SearchSelect :model-value="''" :disabled="disabled" placeholder="+ Aggiungi lista…"
                    :options="listeDisponibili(s).map(c => ({value: c, label: `${spellListLabel(c)} (${c})`}))"
                    @update:model-value="addLista(s, $event as string)"/>
      <div class="custom-lista-row">
        <input v-model.trim="customListaCode[i]" type="text" placeholder="Codice personalizzato, es. SP_MIA_LISTA"
               :disabled="disabled" @keydown.enter.prevent="confirmCustomLista(s, i)"/>
        <button type="button" class="btn ghost" :disabled="disabled || !customListaCode[i]?.trim()"
                @click="confirmCustomLista(s, i)">Aggiungi</button>
      </div>
    </div>

    <div class="rank-grid">
      <label class="field">
        <span class="lbl">Modo</span>
        <SearchSelect v-model="s.modo" :disabled="disabled" :options="OPZIONI_MODO" :sort="false"/>
      </label>
      <label v-if="(s.modo || 'SLOT') === 'SLOT'" class="field">
        <span class="lbl">Progressione</span>
        <SearchSelect v-model="s.progressione" :disabled="disabled"
                      :options="PROGRESSIONI" :sort="false"/>
      </label>
      <label class="field">
        <span class="lbl">Formula slot bonus</span>
        <input v-model.trim="s.bonus" type="text" placeholder="Es.: 1+(@SAG-#L)/4)" :disabled="disabled"/>
      </label>
      <label class="field">
        <span class="lbl">Caratteristica (per la CD: 10 + CL + modificatore)</span>
        <SearchSelect v-model="s.caratteristica" :disabled="disabled"
                      :options="[{value: '', label: '— nessuna —'}, ...stats.filter(x => x.tipo === 'CAR').map(x => ({value: x.id, label: x.label}))]"
                      :sort="false"/>
      </label>
      <label class="field">
        <span class="lbl">Livello usato per il CL</span>
        <SearchSelect v-model="s.casterLevelSorgente" :disabled="disabled"
                      :options="OPZIONI_CASTER_LEVEL" :sort="false"/>
      </label>
      <label class="field">
        <span class="lbl">Livello usato per pescare gli slot</span>
        <SearchSelect v-model="s.slotLivelloSorgente" :disabled="disabled"
                      :options="OPZIONI_LIVELLO_SLOT" :sort="false"/>
      </label>
    </div>

    <div v-if="(s.modo || 'SLOT') === 'SLOT' && (s.progressione || 'CUSTOM') === 'CUSTOM'" class="field">
      <span class="lbl">
        Slot per livello (CUSTOM) — formato "4,2,1,-,…" dal liv. 0 al 9.
        Usa <strong>-</strong> (o lascia vuoto) per "nessun accesso" (—), e <strong>0</strong>
        per "accesso ma 0 slot base" (es. slot bonus da caratteristica alta).
      </span>
      <div class="slot-list">
        <div v-for="l in numLivelli" :key="l" class="slot-row">
          <span class="slot-liv">{{ l }}</span>
          <input type="text" :value="slotDi(s, l)" placeholder="4,2,1,0,0,0,0,0,0,0"
                 :disabled="disabled" @input="setSlot(s, l, ($event.target as HTMLInputElement).value)"/>
        </div>
      </div>
    </div>

    <!-- Modo LIVELLO: un'unica riga "a che livello di classe si sblocca ciascun livello di
         incantesimo", niente tabella per livello — quella sotto (conosciuti) è obbligatoria e fa
         le veci degli slot. -->
    <div v-if="(s.modo || 'SLOT') === 'LIVELLO'" class="field">
      <span class="lbl">
        Livello di sblocco per livello di incantesimo — formato "1,3,5,7,9,12,15,17,20,25" dal
        liv. 0 al 9: il valore è il livello di classe a cui si sblocca quel livello di incantesimo.
        Usa <strong>-</strong> per "mai sbloccato".
      </span>
      <input type="text" :value="sbloccoDi(s)" placeholder="1,3,5,7,9,12,15,17,20,25"
             :disabled="disabled" @input="setSblocco(s, ($event.target as HTMLInputElement).value)"/>
    </div>

    <label v-if="(s.modo || 'SLOT') === 'SLOT'" class="field checkbox-field">
      <input type="checkbox" v-model="s.conosciutiSeparati" :disabled="disabled"/>
      <span class="lbl">Traccia incantesimi conosciuti separatamente dagli slot</span>
    </label>
    <div v-if="(s.modo || 'SLOT') === 'LIVELLO' || s.conosciutiSeparati" class="field">
      <span class="lbl">
        Incantesimi conosciuti per livello — stesso formato degli slot ("-" = nessun accesso).
        <template v-if="(s.modo || 'SLOT') === 'LIVELLO'">
          In modo "A Livello" è questo il numero di incantesimi disponibili una volta sbloccato il
          livello (non c'è una tabella di slot separata).
        </template>
        <template v-else>Nessun bonus da formula: il bonus da caratteristica si applica solo agli slot.</template>
      </span>
      <div class="slot-list">
        <div v-for="l in numLivelli" :key="l" class="slot-row">
          <span class="slot-liv">{{ l }}</span>
          <input type="text" :value="conosciutiDi(s, l)" placeholder="4,2,1,-,-,-,-,-,-,-"
                 :disabled="disabled" @input="setConosciuti(s, l, ($event.target as HTMLInputElement).value)"/>
        </div>
      </div>
    </div>

    <label class="field checkbox-field">
      <input type="checkbox" v-model="s.slotConContatore" :disabled="disabled"/>
      <span class="lbl">Traccia gli slot per livello con Contatore</span>
    </label>
  </div>

  <button type="button" class="btn outline" :disabled="disabled" @click="addSezione">+ Aggiungi sezione</button>
</template>

<style scoped>
.muted { opacity: .7; font-size: .85rem; margin: 0; }
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
.rank-grid { display: grid; grid-template-columns: 1fr 1fr; gap: .5rem; }
@media (max-width: 700px) { .rank-grid { grid-template-columns: 1fr; } }
.field { display: grid; gap: .3rem; }
.lbl { font-size: .8rem; font-weight: 600; opacity: .85; }
.checkbox-field { grid-auto-flow: column; justify-content: start; align-items: center; gap: .5rem; }
.checkbox-field input[type="checkbox"] { width: auto; }
input[type="text"] { width: 100%; min-width: 0; padding: .5rem .6rem; border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0); }
.btn { padding: .5rem .9rem; border-radius: .5rem; border: 1px solid transparent; cursor: pointer; }
.btn.ghost { border-color: var(--hairline); background: var(--surface-0); }
.btn.outline { border-color: var(--info-border); background: var(--info-bg); color: var(--info-text); font-weight: 600; }
.btn:disabled { opacity: .6; cursor: default; }
.btn-del { border: 1px solid var(--danger-border); background: var(--danger-bg); color: var(--danger-text); border-radius: .5rem; padding: .25rem .5rem; cursor: pointer; }
</style>
