<script setup lang="ts">
import ChildrenEditor from './ChildrenEditor.vue'
import {ChildRef} from '../../../../../../../models/dto/UpdateItemRequest'

// Struttura ad albero di un NODO (vedi CardEditorItem.NODO_STRUTTURA):
// - tipoLink: collegamento singolo al "contenuto" del nodo (un item di qualunque tipo);
// - albero: in quale albero va inserito questo nodo (testo libero);
// - a: nodi NODO successivi (verso cui si può andare da qui);
// - da: nodi NODO predecessori (da cui si arriva qui) — editabile qui per comodità, ma la
//   scrittura reale è sull'ALTRO nodo (vedi ItemService.applyNodoDa lato backend): dal punto di
//   vista di questa card è solo un'altra lista di nodi, identica ad "a" nella forma.
const props = defineProps<{
  tipoLink: ChildRef[]  // 0 o 1 elemento: riuso ChildrenEditor (lista) anche per il link singolo
  albero: string
  a: ChildRef[]
  da: ChildRef[]
  excludeId?: number
  disabled?: boolean
}>()
const emit = defineEmits<{
  (e: 'update:tipoLink', v: ChildRef[]): void
  (e: 'update:albero', v: string): void
  (e: 'update:a', v: ChildRef[]): void
  (e: 'update:da', v: ChildRef[]): void
}>()

// il link "Tipo" è a cardinalità 1: se ChildrenEditor emette più di un elemento (si aggiunge un
// secondo mentre il primo è già presente) si tiene solo l'ultimo aggiunto, sostituendo il vecchio.
function onUpdateTipoLink(v: ChildRef[]) {
  emit('update:tipoLink', v.length ? [v[v.length - 1]] : [])
}
</script>

<template>
  <label class="field">
    <span class="lbl">Tipo (collegamento al contenuto del nodo)</span>
    <ChildrenEditor :model-value="tipoLink" :disabled="disabled" :exclude-id="excludeId"
                     hide-create @update:model-value="onUpdateTipoLink"/>
  </label>

  <label class="field">
    <span class="lbl">Albero</span>
    <input :value="albero" type="text" :disabled="disabled" placeholder="es. Albero del Fuoco"
           @input="emit('update:albero', ($event.target as HTMLInputElement).value)"/>
    <span class="muted">In quale albero va inserito questo nodo: i nodi con lo stesso testo appartengono allo stesso albero.</span>
  </label>

  <label class="field">
    <span class="lbl">A — nodi successivi (verso cui si può andare da qui)</span>
    <ChildrenEditor :model-value="a" :disabled="disabled" :exclude-id="excludeId" only-tipo="NODO"
                     hide-create @update:model-value="v => emit('update:a', v)"/>
  </label>

  <label class="field">
    <span class="lbl">Da — nodi predecessori (da cui si arriva qui)</span>
    <ChildrenEditor :model-value="da" :disabled="disabled" :exclude-id="excludeId" only-tipo="NODO"
                     hide-create @update:model-value="v => emit('update:da', v)"/>
    <span class="muted">Aggiungere/togliere un nodo qui aggiorna quel nodo (viene aggiunto/rimosso dai suoi "A"), non un campo su questo nodo.</span>
  </label>
</template>

<style scoped>
.field { display: grid; gap: .3rem; }
.lbl { font-size: .8rem; font-weight: 600; opacity: .85; }
.muted { opacity: .7; font-size: .8rem; margin: 0; }
input[type="text"] {
  width: 100%; padding: .5rem .6rem; border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0);
}
</style>
