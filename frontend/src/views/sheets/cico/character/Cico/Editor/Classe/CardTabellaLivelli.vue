<script setup lang="ts">
import {computed, reactive, watch} from 'vue'
import SearchSelect from '../../../../../../../components/SearchSelect.vue'
import {StatLivelloClasse} from '../../../../../../../service/StatAdminService'

interface LivelloClasse {
  livello: number
  valori: Record<string, string>  // statId -> valore, per le stat "livello classe" del mondo
  spSlot: string
}

const props = defineProps<{
  numLivelli: number
  dv: string
  livelli: LivelloClasse[]  // reactive array del genitore, cresciuta per riferimento (push)
  // Stat con colonna in questa tabella, configurate per il mondo della classe (vedi
  // StatController#getLivelloClasse): vuoto finché il mondo non ne ha configurate.
  statLivelloClasse: StatLivelloClasse[]
  disabled?: boolean
}>()
const emit = defineEmits<{
  (e: 'update:numLivelli', v: number): void
  (e: 'update:dv', v: string): void
}>()

function ensureLivelli(n: number) {
  for (let i = props.livelli.length; i < n; i++) {
    props.livelli.push({livello: i + 1, valori: {}, spSlot: ''})
  }
}
function onNumLivelliInput(v: string) {
  const n = Math.max(1, Math.min(100, Math.floor(Number(v) || 20)))
  ensureLivelli(n)
  emit('update:numLivelli', n)
}
watch(() => props.numLivelli, n => ensureLivelli(n), {immediate: true})

const livelliVisibili = computed<LivelloClasse[]>(() => props.livelli.slice(0, props.numLivelli))

function valore(row: LivelloClasse, statId: string): string {
  return row.valori?.[statId] ?? ''
}
function setValore(row: LivelloClasse, statId: string, v: string) {
  if (!row.valori) row.valori = {}
  row.valori[statId] = v
}

/* Generatore rapido BAB/TS: solo una comodità per il preset classico (mondo Cico), disponibile
 * solo se il mondo ha effettivamente configurato quelle stat come "livello classe". Altre stat
 * configurate da altri mondi restano solo a compilazione manuale. */
const idBab = computed(() => props.statLivelloClasse.find(s => s.statId === 'BAB')?.statId ?? null)
const idTmp = computed(() => props.statLivelloClasse.find(s => s.statId === 'TMP')?.statId ?? null)
const idRfl = computed(() => props.statLivelloClasse.find(s => s.statId === 'RFL')?.statId ?? null)
const idVlt = computed(() => props.statLivelloClasse.find(s => s.statId === 'VLT')?.statId ?? null)
const hasGeneratore = computed(() => !!(idBab.value || idTmp.value || idRfl.value || idVlt.value))

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
    if (idBab.value) setValore(row, idBab.value, `+${babPer(l)}`)
    if (idTmp.value) setValore(row, idTmp.value, `+${tsPer(l, gen.tmp)}`)
    if (idRfl.value) setValore(row, idRfl.value, `+${tsPer(l, gen.rfl)}`)
    if (idVlt.value) setValore(row, idVlt.value, `+${tsPer(l, gen.vlt)}`)
  }
}
</script>

<template>
  <div class="rank-grid">
    <label class="field">
      <span class="lbl">Livelli classe</span>
      <input :value="numLivelli" type="number" min="1" max="100" :disabled="disabled"
             @change="onNumLivelliInput(($event.target as HTMLInputElement).value)"/>
      <span class="muted">Quanti livelli ha la classe (default 20).</span>
    </label>
    <label class="field">
      <span class="lbl">Dadi vita</span>
      <input :value="dv" type="text" placeholder="Es.: 2d10 — vuoto = nessuno" :disabled="disabled"
             @input="emit('update:dv', ($event.target as HTMLInputElement).value)"/>
      <span class="muted">Impostato a ogni livello preso in questa classe. Vuoto = la classe non dà dadi vita.</span>
    </label>
  </div>

  <p v-if="!statLivelloClasse.length" class="muted">
    Nessuna stat "livello classe" configurata per questo mondo (vedi Statistiche & Default Mondo).
  </p>

  <div v-if="hasGeneratore" class="gen">
    <div class="gen-grid">
      <label v-if="idBab" class="field">
        <span class="lbl">BAB</span>
        <SearchSelect v-model="gen.bab" :disabled="disabled" :sort="false"
                      :options="[{value:'ALTO',label:'Alto (guerriero)'},{value:'MEDIO',label:'Medio (chierico)'},{value:'BASSO',label:'Basso (mago)'}]"/>
      </label>
      <label v-if="idTmp" class="field">
        <span class="lbl">Tempra</span>
        <SearchSelect v-model="gen.tmp" :disabled="disabled" :sort="false"
                      :options="[{value:'BUONO',label:'Buono'},{value:'SCARSO',label:'Scarso'}]"/>
      </label>
      <label v-if="idRfl" class="field">
        <span class="lbl">Riflessi</span>
        <SearchSelect v-model="gen.rfl" :disabled="disabled" :sort="false"
                      :options="[{value:'BUONO',label:'Buono'},{value:'SCARSO',label:'Scarso'}]"/>
      </label>
      <label v-if="idVlt" class="field">
        <span class="lbl">Volontà</span>
        <SearchSelect v-model="gen.vlt" :disabled="disabled" :sort="false"
                      :options="[{value:'BUONO',label:'Buono'},{value:'SCARSO',label:'Scarso'}]"/>
      </label>
    </div>
    <button type="button" class="btn outline" :disabled="disabled" @click="generaTabella">
      ⚙ Genera tabella livelli
    </button>
  </div>

  <div v-if="statLivelloClasse.length" class="liv-list">
    <div v-for="row in livelliVisibili" :key="row.livello" class="liv-card">
      <div class="liv-num">{{ row.livello }}</div>
      <div class="liv-fields">
        <label v-for="s in statLivelloClasse" :key="s.statId">
          <span>{{ s.statLabel }}</span>
          <input :value="valore(row, s.statId)" :disabled="disabled"
                 @input="setValore(row, s.statId, ($event.target as HTMLInputElement).value.trim())"/>
        </label>
      </div>
    </div>
  </div>
</template>

<style scoped>
.rank-grid { display: grid; grid-template-columns: 1fr 1fr; gap: .5rem; }
@media (max-width: 700px) { .rank-grid { grid-template-columns: 1fr; } }
.field { display: grid; gap: .3rem; }
.lbl { font-size: .8rem; font-weight: 600; opacity: .85; }
.muted { opacity: .7; font-size: .85rem; }
input[type="text"], input[type="number"] { width: 100%; padding: .5rem .6rem; border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0); }

.gen { display: grid; gap: .5rem; border: 1px dashed var(--hairline); border-radius: .5rem; padding: .5rem; background: var(--primary-color); }
.gen-grid { display: grid; grid-template-columns: 1fr 1fr; gap: .4rem; }
.btn { padding: .5rem .9rem; border-radius: .5rem; border: 1px solid transparent; cursor: pointer; }
.btn.outline { border-color: var(--info-border); background: var(--info-bg); color: var(--info-text); font-weight: 600; }
.btn:disabled { opacity: .6; cursor: default; }

.liv-list { display: grid; gap: .4rem; }
.liv-card { display: grid; grid-template-columns: 2rem 1fr; gap: .5rem; align-items: start; border: 1px solid var(--hairline); border-radius: .5rem; padding: .4rem .5rem; }
.liv-num { font-weight: 800; font-size: .95rem; color: var(--info-text); background: var(--info-bg); border-radius: .4rem; text-align: center; padding: .3rem 0; }
.liv-fields { display: grid; grid-template-columns: repeat(3, 1fr); gap: .3rem; }
.liv-fields label { display: grid; gap: .1rem; min-width: 0; }
.liv-fields span { font-size: .65rem; font-weight: 700; opacity: .7; }
.liv-fields input { padding: .3rem .4rem; font-size: .85rem; }
</style>
