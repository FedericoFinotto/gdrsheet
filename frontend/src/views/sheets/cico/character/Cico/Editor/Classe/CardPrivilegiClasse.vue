<script setup lang="ts">
import {computed, reactive, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {Item} from '../../../../../../../models/dto/Item'
import {getItem, searchItems, updateItem} from '../../../../../../../service/PersonaggioService'
import {LABELS} from '../../../../../../../models/entity/ItemLabel'
import {LabelRow} from '../../../../../../../models/dto/UpdateItemRequest'
import HtmlEditor from '../../../../../../../components/HtmlEditor.vue'
import {useMondoStore} from '../../../../../../../stores/mondo'

interface AbilitaConcessa {
  livello: number
  itemId: number | null
  nome: string
  tipo?: string
  nuovo?: boolean
  qty?: number | null
  descrizione?: string
  altreLabels?: LabelRow[]
  straordinaria?: boolean
  magica?: boolean
  soprannaturale?: boolean
  naturale?: boolean
  gruppoPrivilegi?: string
  caricato?: boolean
  salvandoRiga?: boolean
}

const props = defineProps<{
  abilitaConcesse: AbilitaConcessa[] // reactive array del genitore, mutata per riferimento
  numLivelli: number
  disabled?: boolean
  salvaClasse: () => Promise<unknown> // persiste l'intera classe (serve al "Salva riga" avanzato)
}>()

const router = useRouter()
const route = useRoute()
const mondoStore = useMondoStore()
mondoStore.carica() // idempotente

function editConcessa(itemId: number) {
  const idPg = route.query.personaggio
  router.push(`/itemeditor/${itemId}` + (idPg ? `?personaggio=${idPg}` : ''))
}

const modalitaAvanzata = ref(false)
const DESCR_LABELS: string[] = [
  LABELS.DESCR_STRAORDINARIA, LABELS.DESCR_MAGICA, LABELS.DESCR_SOPRANNATURALE, LABELS.DESCR_NATURALE,
  LABELS.GRUPPO_PRIVILEGI,
]

async function caricaDettagliAvanzati(a: AbilitaConcessa) {
  if (!a.itemId || a.caricato) return
  try {
    const it = (await getItem(a.itemId)).data
    a.descrizione = it.descrizione ?? ''
    const val = (label: string) => it.labels?.find(l => l.label === label)?.valore
    a.straordinaria = val(LABELS.DESCR_STRAORDINARIA) === '1'
    a.magica = val(LABELS.DESCR_MAGICA) === '1'
    a.soprannaturale = val(LABELS.DESCR_SOPRANNATURALE) === '1'
    a.naturale = val(LABELS.DESCR_NATURALE) === '1'
    a.gruppoPrivilegi = val(LABELS.GRUPPO_PRIVILEGI) ?? ''
    a.altreLabels = (it.labels ?? [])
        .filter(l => l.label && !DESCR_LABELS.includes(l.label))
        .map(l => ({label: l.label!, valore: l.valore ?? ''}))
    a.caricato = true
  } catch (e) {
    console.error('Errore caricamento dettagli privilegio:', e)
  }
}
watch(modalitaAvanzata, (attiva) => {
  if (!attiva) return
  for (const a of props.abilitaConcesse) caricaDettagliAvanzati(a)
})

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
      const res = await searchItems(q, undefined, mondoStore.corrente)
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
  const l = Math.min(props.numLivelli || 20, Math.max(1, Math.floor(Number(livelloConcessa.value) || 1)))
  if (props.abilitaConcesse.some(a => a.itemId === itm.id && a.livello === l)) return
  props.abilitaConcesse.push({livello: l, itemId: itm.id, nome: itm.nome, tipo: itm.tipo})
  props.abilitaConcesse.sort((a, b) => a.livello - b.livello || a.nome.localeCompare(b.nome))
  risultatiConcessa.value = []
  queryConcessa.value = ''
}
function aggiungiNuovo() {
  const nome = queryConcessa.value.trim()
  if (!nome) return
  const l = Math.min(props.numLivelli || 20, Math.max(1, Math.floor(Number(livelloConcessa.value) || 1)))
  props.abilitaConcesse.push({livello: l, itemId: null, nome, tipo: 'PRIVILEGIO', nuovo: true})
  props.abilitaConcesse.sort((a, b) => a.livello - b.livello || a.nome.localeCompare(b.nome))
  risultatiConcessa.value = []
  queryConcessa.value = ''
}
const mostraNuovo = computed(() => {
  const q = queryConcessa.value.trim().toLowerCase()
  if (q.length < 2) return false
  return !risultatiConcessa.value.some(r => (r.nome ?? '').toLowerCase() === q)
})
function rimuoviConcessa(i: number) {
  props.abilitaConcesse.splice(i, 1)
}

async function salvaRigaAvanzata(a: AbilitaConcessa) {
  if (!a.itemId || a.salvandoRiga) return
  a.salvandoRiga = true
  try {
    const labels: LabelRow[] = [...(a.altreLabels ?? [])]
    if (a.straordinaria) labels.push({label: LABELS.DESCR_STRAORDINARIA, valore: '1'})
    if (a.magica) labels.push({label: LABELS.DESCR_MAGICA, valore: '1'})
    if (a.soprannaturale) labels.push({label: LABELS.DESCR_SOPRANNATURALE, valore: '1'})
    if (a.naturale) labels.push({label: LABELS.DESCR_NATURALE, valore: '1'})
    if (a.gruppoPrivilegi?.trim()) labels.push({label: LABELS.GRUPPO_PRIVILEGI, valore: a.gruppoPrivilegi.trim()})
    await updateItem(a.itemId, {descrizione: a.descrizione ?? '', labels})
    await props.salvaClasse()
  } catch (e: any) {
    console.error('Errore salvataggio riga privilegio:', e)
  } finally {
    a.salvandoRiga = false
  }
}
</script>

<template>
  <label class="adv-toggle">
    <span class="switch">
      <input type="checkbox" v-model="modalitaAvanzata" :disabled="disabled"/>
      <span class="switch-track"><span class="switch-thumb"></span></span>
    </span>
    <span class="adv-toggle-label">{{ modalitaAvanzata ? 'Modalità avanzata' : 'Modalità semplice' }}</span>
  </label>

  <div v-for="(a, i) in abilitaConcesse" :key="`${a.itemId ?? 'new'}-${a.nome}-${a.livello}`" class="conc-item">
    <div class="conc-row">
      <span class="liv-pill">Liv {{ a.livello }}</span>
      <span class="nome"><span v-if="a.nuovo" class="new-chip">NEW</span>{{ a.nome }}</span>
      <input
          class="qty-input"
          type="number"
          min="1"
          step="1"
          :value="a.qty ?? ''"
          :disabled="disabled"
          placeholder="—"
          title="Utilizzi concessi"
          @change="a.qty = ($event.target as HTMLInputElement).value ? parseInt(($event.target as HTMLInputElement).value) || null : null"
      />
      <button type="button" class="btn-edit" :disabled="disabled || !a.itemId"
              @click="editConcessa(a.itemId!)" title="Modifica">✎</button>
      <button type="button" class="btn-del" :disabled="disabled" @click="rimuoviConcessa(i)">✕</button>
    </div>

    <div v-if="modalitaAvanzata" class="conc-adv">
      <p v-if="!a.itemId" class="muted">Salva prima la classe per poter modificare questo nuovo privilegio.</p>
      <template v-else>
        <div class="conc-adv-top">
          <label class="field liv-input">
            <span class="lbl">Livello</span>
            <input v-model.number="a.livello" type="number" min="1" :max="numLivelli" :disabled="disabled"/>
          </label>
          <label class="field gruppo-input"
                 title="Se il personaggio ha più privilegi con lo stesso gruppo (es. una versione potenziata sbloccata da una classe di prestigio), in scheda si vede solo quello del livello più alto.">
            <span class="lbl">Gruppo privilegi</span>
            <input v-model="a.gruppoPrivilegi" type="text" placeholder="es. FURIA" :disabled="disabled"/>
          </label>
        </div>
        <div class="conc-adv-body">
          <label class="field grow">
            <span class="lbl">Descrizione</span>
            <HtmlEditor v-model="a.descrizione" :rows="3" :disabled="disabled"/>
          </label>
          <div class="conc-adv-flags">
            <label class="chk-row">
              <input type="checkbox" v-model="a.straordinaria" :disabled="disabled"/>
              <span>Straordinaria</span>
            </label>
            <label class="chk-row">
              <input type="checkbox" v-model="a.magica" :disabled="disabled"/>
              <span>Magica</span>
            </label>
            <label class="chk-row">
              <input type="checkbox" v-model="a.soprannaturale" :disabled="disabled"/>
              <span>Soprannaturale</span>
            </label>
            <label class="chk-row">
              <input type="checkbox" v-model="a.naturale" :disabled="disabled"/>
              <span>Naturale</span>
            </label>
          </div>
        </div>
        <div class="conc-adv-actions">
          <button type="button" class="btn primary sm" :disabled="disabled || a.salvandoRiga"
                  @click="salvaRigaAvanzata(a)">
            {{ a.salvandoRiga ? 'Salvataggio…' : 'Salva riga' }}
          </button>
        </div>
      </template>
    </div>
  </div>

  <div class="conc-add">
    <label class="field liv-input">
      <span class="lbl">Liv</span>
      <input v-model.number="livelloConcessa" type="number" min="1" :max="numLivelli || 20" :disabled="disabled"/>
    </label>
    <input class="grow" v-model="queryConcessa" type="text"
           placeholder="Cerca o scrivi un nuovo privilegio…" :disabled="disabled" @input="onQueryConcessa"/>
  </div>
  <div v-if="searching" class="muted">Ricerca…</div>
  <ul v-else-if="risultatiConcessa.length || mostraNuovo" class="results">
    <li v-for="r in risultatiConcessa" :key="r.id">
      <button type="button" class="result" :disabled="disabled" @click="aggiungiConcessa(r)">
        <span class="nome">{{ r.nome }}</span>
        <span class="liv-pill">{{ r.tipo }}</span>
        <span class="plus">+</span>
      </button>
    </li>
    <li v-if="mostraNuovo">
      <button type="button" class="result" :disabled="disabled" @click="aggiungiNuovo">
        <span class="nome">{{ queryConcessa.trim() }}</span>
        <span class="new-chip">NEW</span>
        <span class="plus">+</span>
      </button>
    </li>
  </ul>
</template>

<style scoped>
.field { display: grid; gap: .3rem; }
.lbl { font-size: .8rem; font-weight: 600; opacity: .85; }
.muted { opacity: .7; font-size: .85rem; }
input[type="text"], input[type="number"] { width: 100%; min-width: 0; padding: .5rem .6rem; border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0); }

.conc-row { display: grid; grid-template-columns: auto 1fr auto auto auto; gap: .4rem; align-items: center; border: 1px solid var(--hairline); border-radius: .5rem; padding: .35rem .5rem; background: var(--surface-0); }
.conc-row .nome { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-weight: 600; }
.conc-row .qty-input { width: 2.4rem !important; min-width: 0; padding: .25rem .2rem !important; border: 1px solid var(--hairline); border-radius: .4rem; text-align: center; font-size: .8rem; }
.btn-edit { border: 1px solid var(--info-border); background: var(--info-bg); color: var(--info-text); border-radius: .5rem; padding: .25rem .5rem; cursor: pointer; }
.btn-edit:hover { background: #dbeafe; }
.btn-edit:disabled { opacity: .6; cursor: default; }
.new-chip { display: inline-block; margin-right: .35rem; background: #16a34a; color: #fff; font-size: .65rem; font-weight: 800; border-radius: .35rem; padding: .05rem .35rem; vertical-align: middle; }
.liv-pill { font-size: .72rem; padding: .1rem .45rem; border-radius: .5rem; background: var(--success-bg); color: var(--success-text); font-weight: 700; white-space: nowrap; }
.conc-add { display: grid; grid-template-columns: 5rem 1fr; gap: .4rem; align-items: end; }
.conc-add .grow { min-width: 0; }

.adv-toggle { display: flex; align-items: center; gap: .5rem; font-size: .85rem; font-weight: 600; margin-bottom: .3rem; cursor: pointer; }
.adv-toggle-label { transition: color .15s; }
.switch { position: relative; display: inline-flex; flex-shrink: 0; width: 2.4rem; height: 1.3rem; }
.switch input { position: absolute; inset: 0; opacity: 0; margin: 0; cursor: pointer; z-index: 1; }
.switch-track { position: absolute; inset: 0; border-radius: 999px; background: var(--btn-bg); transition: background-color .15s; }
.switch-thumb { position: absolute; top: .15rem; left: .15rem; width: 1rem; height: 1rem; border-radius: 50%; background: var(--surface-0); box-shadow: 0 1px 2px rgba(0,0,0,.3); transition: transform .15s; }
.switch input:checked + .switch-track { background: #2563eb; }
.switch input:checked + .switch-track .switch-thumb { transform: translateX(1.1rem); }
.switch input:disabled + .switch-track { opacity: .6; cursor: default; }
.conc-item { display: flex; flex-direction: column; gap: .3rem; }
.conc-adv { border: 1px dashed var(--info-border); border-radius: .5rem; padding: .5rem; background: var(--primary-color); display: flex; flex-direction: column; gap: .5rem; }
.conc-adv .liv-input { max-width: 6rem; }
.conc-adv-top { display: flex; gap: .6rem; }
.conc-adv-top .gruppo-input { max-width: 12rem; }
.conc-adv-body { display: flex; gap: .6rem; align-items: flex-start; }
.conc-adv-body .grow { flex: 1; min-width: 0; }
.conc-adv-flags { flex: 0 0 auto; display: flex; flex-direction: column; gap: .3rem; border: 1px solid var(--hairline); border-radius: .5rem; padding: .5rem; background: var(--surface-0); min-width: 9rem; }
.chk-row { display: flex; align-items: center; gap: .4rem; font-size: .8rem; }
.conc-adv-actions { display: flex; justify-content: flex-end; }
@media (max-width: 640px) { .conc-adv-body { flex-direction: column; } }

.results { list-style: none; margin: 0; padding: 0; border: 1px solid var(--hairline); border-radius: .5rem; overflow: hidden; max-height: 14rem; overflow-y: auto; }
.results li + li { border-top: 1px solid var(--hairline); }
.result { width: 100%; display: grid; grid-template-columns: 1fr auto auto; gap: .4rem; align-items: center; padding: .4rem .5rem; background: var(--surface-0); border: 0; cursor: pointer; text-align: left; }
.result:hover { background: var(--surface-hover); }
.result .nome { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.plus { color: #2563eb; font-weight: 700; }
.btn-del { border: 1px solid var(--danger-border); background: var(--danger-bg); color: var(--danger-text); border-radius: .5rem; padding: .25rem .5rem; cursor: pointer; }
.btn { padding: .5rem .9rem; border-radius: .5rem; border: 1px solid transparent; cursor: pointer; }
.btn.primary { background: #2563eb; color: white; }
.btn.sm { padding: .3rem .6rem; font-size: .8rem; }
.btn:disabled { opacity: .6; cursor: default; }
</style>
