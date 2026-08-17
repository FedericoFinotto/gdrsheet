<script setup lang="ts">
import SearchSelect from '../../../../../../../components/SearchSelect.vue'
import {TAGLIE_OPTIONS_NOME} from '../../../../../../../function/Utils'

defineProps<{
  taglia: string; velocita: string; caratteristiche: string; lap: string; spazio: string; portata: string
  disabled?: boolean
}>()
defineEmits<{
  (e: 'update:taglia', v: string): void
  (e: 'update:velocita', v: string): void
  (e: 'update:caratteristiche', v: string): void
  (e: 'update:lap', v: string): void
  (e: 'update:spazio', v: string): void
  (e: 'update:portata', v: string): void
}>()

const TAGLIE_RAZZA = [{value: '', label: '— nessuna —'}, ...TAGLIE_OPTIONS_NOME]
</script>

<template>
  <div class="rank-grid">
    <label class="field">
      <span class="lbl">Taglia</span>
      <SearchSelect :model-value="taglia" :options="TAGLIE_RAZZA" :disabled="disabled" :sort="false"
                    @update:model-value="$emit('update:taglia', String($event ?? ''))"/>
    </label>
    <label class="field">
      <span class="lbl">Velocità</span>
      <input :value="velocita" type="text" placeholder="Es.: 9 m" :disabled="disabled"
             @input="$emit('update:velocita', ($event.target as HTMLInputElement).value)"/>
    </label>
  </div>
  <label class="field">
    <span class="lbl">Caratteristiche</span>
    <input :value="caratteristiche" type="text" placeholder="Es.: +2 Destrezza, -2 Forza" :disabled="disabled"
           @input="$emit('update:caratteristiche', ($event.target as HTMLInputElement).value)"/>
  </label>
  <div class="rank-grid">
    <label class="field">
      <span class="lbl">LAP (Level Adjustment)</span>
      <input :value="lap" type="text" placeholder="Es.: +0" :disabled="disabled"
             @input="$emit('update:lap', ($event.target as HTMLInputElement).value)"/>
    </label>
    <label class="field">
      <span class="lbl">Spazio</span>
      <input :value="spazio" type="text" placeholder="Es.: 1,5 m" :disabled="disabled"
             @input="$emit('update:spazio', ($event.target as HTMLInputElement).value)"/>
    </label>
  </div>
  <label class="field">
    <span class="lbl">Portata</span>
    <input :value="portata" type="text" placeholder="Es.: 1,5 m" :disabled="disabled"
           @input="$emit('update:portata', ($event.target as HTMLInputElement).value)"/>
  </label>
</template>

<style scoped>
.rank-grid { display: grid; grid-template-columns: 1fr 1fr; gap: .5rem; }
@media (max-width: 700px) { .rank-grid { grid-template-columns: 1fr; } }
.field { display: grid; gap: .3rem; }
.lbl { font-size: .8rem; font-weight: 600; opacity: .85; }
input[type="text"] {
  width: 100%; padding: .5rem .6rem; border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0);
}
</style>
