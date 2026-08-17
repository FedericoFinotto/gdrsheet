<script setup lang="ts">
import {computed, ref} from 'vue'
import {Stat} from '../../../../../../../models/entity/Stat'

const props = defineProps<{
  rank1: string
  rank: string
  // array/Set passati per riferimento e mutati direttamente (stesso pattern del form reactive()
  // del genitore: push/splice/add/delete restano reattivi perché è lo stesso oggetto proxato)
  abilitaClasse: string[]
  abPersonaggio: Set<string>
  abEsclusaCap: Set<string>
  stats: Stat[]
  disabled?: boolean
}>()
const emit = defineEmits<{
  (e: 'update:rank1', v: string): void
  (e: 'update:rank', v: string): void
}>()

const filtroAbilita = ref('')

const FAMIGLIA_PLACEHOLDER: { id: string; label: string }[] = [
  {id: 'AB00', label: 'Tutte le Abilità'},
  {id: 'CO00', label: 'Tutte le Conoscenze'},
  {id: 'IN00', label: 'Tutti gli Intrattenere'},
  {id: 'AR00', label: 'Tutti gli Artigianati'},
]
const FAMIGLIA_PLACEHOLDER_IDS = new Set(FAMIGLIA_PLACEHOLDER.map(f => f.id))

const abilitaDisponibili = computed(() =>
    props.stats
        .filter(s => s.tipo === 'AB')
        .filter(s => !FAMIGLIA_PLACEHOLDER_IDS.has(s.id))
        .filter(s => !filtroAbilita.value.trim()
            || s.label.toLowerCase().includes(filtroAbilita.value.trim().toLowerCase()))
)

function isSelected(id: string): boolean {
  return props.abilitaClasse.includes(id)
}
function isPersonaggio(id: string): boolean {
  return props.abPersonaggio.has(id)
}
function isEsclusaCap(id: string): boolean {
  return props.abEsclusaCap.has(id)
}
function toggleAbilita(id: string) {
  const i = props.abilitaClasse.indexOf(id)
  if (i >= 0) {
    props.abilitaClasse.splice(i, 1)
    props.abPersonaggio.delete(id) // deselezionando, perde anche i flag PG ed esclusione cap
    props.abEsclusaCap.delete(id)
  } else {
    props.abilitaClasse.push(id)
  }
}
function togglePersonaggio(id: string) {
  if (!isSelected(id)) return
  if (props.abPersonaggio.has(id)) props.abPersonaggio.delete(id)
  else props.abPersonaggio.add(id)
}
function toggleEsclusaCap(id: string) {
  if (!isSelected(id)) return
  if (props.abEsclusaCap.has(id)) props.abEsclusaCap.delete(id)
  else props.abEsclusaCap.add(id)
}
function statLabel(id: string): string {
  return props.stats.find(s => s.id === id)?.label ?? id
}

function tutteAbIds(): string[] {
  return props.stats
      .filter(s => s.tipo === 'AB')
      .filter(s => !FAMIGLIA_PLACEHOLDER_IDS.has(s.id))
      .map(s => s.id)
}
function selezionaTutteAbilita() {
  props.abilitaClasse.splice(0, props.abilitaClasse.length, ...tutteAbIds())
}
function selezionaTuttePg() {
  const ids = tutteAbIds()
  props.abilitaClasse.splice(0, props.abilitaClasse.length, ...ids)
  props.abPersonaggio.clear()
  ids.forEach(id => props.abPersonaggio.add(id))
}
function deselezionaTutteAbilita() {
  props.abilitaClasse.splice(0, props.abilitaClasse.length)
  props.abPersonaggio.clear()
  props.abEsclusaCap.clear()
}

// long-press sul pulsante "Tutte": click = solo abilità, hold = anche PG
let lpTimer: any = null
let lpFired = false
function lpStart() {
  if (props.disabled) return
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
</script>

<template>
  <div class="rank-grid">
    <label class="field">
      <span class="lbl">Gradi al 1° livello del personaggio (RANK_1)</span>
      <input :value="rank1" type="text" placeholder="Es.: 4*(@INT+4)" :disabled="disabled"
             @input="emit('update:rank1', ($event.target as HTMLInputElement).value)"/>
    </label>
    <label class="field">
      <span class="lbl">Gradi agli altri livelli (RANK)</span>
      <input :value="rank" type="text" placeholder="Es.: (@INT+4)" :disabled="disabled"
             @input="emit('update:rank', ($event.target as HTMLInputElement).value)"/>
    </label>
  </div>

  <div class="ab-list ab-famiglie">
    <div v-for="f in FAMIGLIA_PLACEHOLDER" :key="f.id" class="ab-riga" :class="{ sel: isSelected(f.id) }">
      <button type="button" class="ab-toggle" :disabled="disabled" @click="toggleAbilita(f.id)">
        <span class="dot">{{ isSelected(f.id) ? '●' : '○' }}</span>
        <span class="ab-nome">{{ f.label }}</span>
      </button>
      <button v-if="isSelected(f.id)" type="button" class="ab-pg" :class="{ on: isPersonaggio(f.id) }"
              :disabled="disabled" @click="togglePersonaggio(f.id)"
              title="Abilità personaggio: vale anche nei livelli di altre classi">
        PG
      </button>
      <button v-if="isSelected(f.id)" type="button" class="ab-cap" :class="{ on: isEsclusaCap(f.id) }"
              :disabled="disabled" @click="toggleEsclusaCap(f.id)"
              title="Esclusa dal limite gradi: resta spendibile ma non alza il limite massimo (come cross-class), a meno che non sia di classe anche per un altro motivo">
        ?
      </button>
    </div>
  </div>

  <div class="ab-tools">
    <input v-model="filtroAbilita" type="text" placeholder="Filtra abilità…" :disabled="disabled" class="grow"/>
    <button type="button" class="btn outline sm" :disabled="disabled"
            title="Click: tutte. Tieni premuto: tutte + PG"
            @click="lpClick"
            @mousedown="lpStart" @mouseup="lpEnd" @mouseleave="lpEnd"
            @touchstart.passive="lpStart" @touchend="lpEnd" @touchcancel="lpEnd">Tutte</button>
    <button type="button" class="btn outline sm" :disabled="disabled" @click="deselezionaTutteAbilita">Nessuna</button>
  </div>
  <p class="muted hint-pg">
    <strong>PG</strong> = abilità personaggio: vale anche nei livelli che non usano questa classe.
    <strong>?</strong> = esclusa dal limite gradi: resta spendibile ma non alza il limite massimo
    (come cross-class), a meno che non sia di classe anche per un altro motivo.
  </p>
  <div class="ab-list">
    <div v-for="s in abilitaDisponibili" :key="s.id" class="ab-riga" :class="{ sel: isSelected(s.id) }">
      <button type="button" class="ab-toggle" :disabled="disabled" @click="toggleAbilita(s.id)">
        <span class="dot">{{ isSelected(s.id) ? '●' : '○' }}</span>
        <span class="ab-nome">{{ s.label }}</span>
      </button>
      <button v-if="isSelected(s.id)" type="button" class="ab-pg" :class="{ on: isPersonaggio(s.id) }"
              :disabled="disabled" @click="togglePersonaggio(s.id)"
              title="Abilità personaggio: vale anche nei livelli di altre classi">
        PG
      </button>
      <button v-if="isSelected(s.id)" type="button" class="ab-cap" :class="{ on: isEsclusaCap(s.id) }"
              :disabled="disabled" @click="toggleEsclusaCap(s.id)"
              title="Esclusa dal limite gradi: resta spendibile ma non alza il limite massimo (come cross-class), a meno che non sia di classe anche per un altro motivo">
        ?
      </button>
    </div>
  </div>
</template>

<style scoped>
.rank-grid { display: grid; grid-template-columns: 1fr 1fr; gap: .5rem; }
@media (max-width: 700px) { .rank-grid { grid-template-columns: 1fr; } }
.field { display: grid; gap: .3rem; }
.lbl { font-size: .8rem; font-weight: 600; opacity: .85; }
input[type="text"] { width: 100%; padding: .5rem .6rem; border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0); }

.hint-pg { margin: 0; }
.ab-list { display: grid; gap: .3rem; max-height: 18rem; overflow-y: auto; padding: .15rem; border: 1px solid var(--hairline); border-radius: .5rem; }
.ab-famiglie { max-height: none; overflow: visible; margin-bottom: .5rem; border-style: dashed; background: var(--primary-color); }
.ab-riga { display: flex; align-items: center; gap: .4rem; border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0); padding: .15rem .35rem; }
.ab-riga.sel { border-color: #c7d2fe; background: #eef2ff; }
.ab-toggle { flex: 1; display: flex; align-items: center; gap: .5rem; border: 0; background: transparent; cursor: pointer; text-align: left; padding: .35rem .25rem; font-size: .9rem; min-width: 0; }
.ab-toggle .dot { font-size: .9rem; color: #6366f1; width: 1rem; text-align: center; }
.ab-riga.sel .ab-toggle .dot { color: #4338ca; }
.ab-nome { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ab-pg { flex: 0 0 auto; border: 1px solid var(--info-border); background: var(--surface-0); color: var(--info-text); border-radius: 1rem; padding: .1rem .55rem; font-size: .75rem; font-weight: 700; cursor: pointer; }
.ab-pg.on { background: #4338ca; border-color: #4338ca; color: #fff; }
.ab-cap { flex: 0 0 auto; border: 1px solid var(--accent-pink-border); background: var(--surface-0); color: var(--accent-pink-text); border-radius: 1rem; padding: .1rem .55rem; font-size: .75rem; font-weight: 700; cursor: pointer; }
.ab-cap.on { background: #9d174d; border-color: #9d174d; color: #fff; }
.ab-toggle:disabled, .ab-pg:disabled, .ab-cap:disabled { opacity: .6; cursor: default; }
.ab-tools { display: flex; gap: .4rem; align-items: center; }
.ab-tools .grow { flex: 1; }
.btn.sm { padding: .3rem .6rem; font-size: .8rem; }
.btn { padding: .5rem .9rem; border-radius: .5rem; border: 1px solid transparent; cursor: pointer; }
.btn.outline { border-color: var(--info-border); background: var(--info-bg); color: var(--info-text); font-weight: 600; }
.btn:disabled { opacity: .6; cursor: default; }
.muted { opacity: .7; font-size: .85rem; }
</style>
