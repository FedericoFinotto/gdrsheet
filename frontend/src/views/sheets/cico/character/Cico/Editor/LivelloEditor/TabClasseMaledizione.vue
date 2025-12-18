<script setup lang="ts">
import {computed} from 'vue'
import {Classe} from '../../../../../../models/dto/Classe'
import {Item} from '../../../../../../models/dto/Item'
import TabExpandable from "../../../../../../../components/TabExpandable.vue";

const props = defineProps<{
  disabled: boolean
  summary: string
  classi: Classe[]
  maledizioni: Item[]
  classeDetail: any | null
  livelliDisponibili: number[]
  classeId: number | null
  maledizioneNome: string | null
  livelliClasse: Record<number, boolean>
}>()

const emit = defineEmits<{
  (e: 'update:classeId', v: number | null): void
  (e: 'update:maledizioneNome', v: string | null): void
  (e: 'update:livelliClasse', v: Record<number, boolean>): void
}>()

const classeIdLocal = computed({
  get: () => props.classeId,
  set: v => emit('update:classeId', v)
})

const maledizioneNomeLocal = computed({
  get: () => props.maledizioneNome,
  set: v => emit('update:maledizioneNome', v)
})

const livelliClasseLocal = computed({
  get: () => props.livelliClasse,
  set: v => emit('update:livelliClasse', v)
})
</script>

<template>
  <TabExpandable title="Classe / Maledizione" :defaultOpen="true">
    <template #summary><span class="wrap">{{ summary }}</span></template>
    <template #content>
      <!-- Classe -->
      <div class="row">
        <label class="field full-width">
          <span class="lbl">Classe <span class="required">*</span></span>
          <select v-model="classeIdLocal" :disabled="disabled" required>
            <option :value="null" disabled>— Seleziona una classe —</option>
            <option v-for="c in classi" :key="'c-'+c.id" :value="c.id">{{ c.nome }}</option>
          </select>
        </label>
      </div>

      <!-- Maledizione -->
      <div class="row">
        <label class="field full-width">
          <span class="lbl">Maledizione (opzionale)</span>
          <select v-model="maledizioneNomeLocal" :disabled="disabled">
            <option :value="null">— Nessuna maledizione —</option>
            <option v-for="m in maledizioni" :key="'m-'+m.id" :value="m.nome">{{ m.nome }}</option>
          </select>
        </label>
      </div>

      <!-- Livelli di classe -->
      <div v-if="classeDetail" class="levels-wrap">
        <div class="lbl" style="margin-bottom:.35rem;">Seleziona livelli di classe</div>
        <div class="levels-grid">
          <label v-for="lv in livelliDisponibili" :key="'lv-'+lv" class="level-pill">
            <input type="checkbox"
                   :disabled="disabled"
                   v-model="livelliClasseLocal[lv]"/>
            <span>{{ lv }}</span>
          </label>
        </div>
      </div>
    </template>
  </TabExpandable>
</template>

<style scoped>
.row {
  display: grid;
  gap: .5rem;
}

.field {
  display: grid;
  gap: .35rem;
}

.full-width {
  grid-column: 1 / -1;
}

.lbl {
  font-size: .8rem;
  font-weight: 600;
  opacity: .85;
}

.required {
  color: #ef4444;
}

select {
  width: 100%;
  padding: .5rem .6rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  background: #fff;
}

.wrap {
  white-space: normal;
  word-break: break-word;
}

.levels-wrap {
  margin-top: .5rem;
}

.levels-grid {
  display: flex;
  flex-wrap: wrap;
  gap: .4rem .5rem;
}

.level-pill {
  display: inline-flex;
  align-items: center;
  gap: .35rem;
  padding: .2rem .45rem;
  border: 1px solid #e5e7eb;
  border-radius: .5rem;
  background: #fff;
}

.level-pill input {
  accent-color: #2563eb;
}
</style>
