<script setup lang="ts">
import {computed} from 'vue'
import TabExpandable from "../../../../../../../components/TabExpandable.vue";

interface Caratteristiche {
  FOR?: number | null;
  DES?: number | null;
  COS?: number | null;
  INT?: number | null;
  SAG?: number | null;
  CAR?: number | null
}

const props = defineProps<{
  disabled: boolean
  summary: string
  livello: number | null
  caratteristiche: Caratteristiche
}>()

const emit = defineEmits<{
  (e: 'update:livello', v: number | null): void
  (e: 'update:caratteristiche', v: Caratteristiche): void
}>()

const livelloLocal = computed({
  get: () => props.livello,
  set: v => emit('update:livello', v)
})

const caratteristicheLocal = computed({
  get: () => props.caratteristiche,
  set: v => emit('update:caratteristiche', v)
})
</script>

<template>
  <div>
    <!-- LVL -->
    <div class="row three">
      <label class="field">
        <span class="lbl">LVL</span>
        <input type="number"
               inputmode="numeric"
               min="0"
               step="1"
               v-model.number="livelloLocal"
               :disabled="disabled"
               placeholder="—"/>
      </label>
      <div class="field"></div>
      <div class="field"></div>
    </div>

    <!-- Caratteristiche -->
    <TabExpandable title="Caratteristiche (facoltative)" :defaultOpen="false" v-if="livelloLocal === 0">
      <template #summary>{{ summary }}</template>
      <template #content>
        <div class="row three">
          <label v-for="k in ['FOR','DES','COS','INT','SAG','CAR']" :key="k" class="field">
            <span class="lbl">{{ k }}</span>
            <input type="number"
                   inputmode="numeric"
                   min="0"
                   step="1"
                   :placeholder="'—'"
                   :disabled="disabled"
                   v-model.number="(caratteristicheLocal as any)[k]"/>
          </label>
        </div>
      </template>
    </TabExpandable>
  </div>
</template>

<style scoped>
.row {
  display: grid;
  gap: .5rem;
}

.row.three {
  grid-template-columns: 1fr 1fr 1fr;
}

@media (max-width: 900px) {
  .row.three {
    grid-template-columns: 1fr;
  }
}

.field {
  display: grid;
  gap: .35rem;
}

.lbl {
  font-size: .8rem;
  font-weight: 600;
  opacity: .85;
}

input[type="number"] {
  width: 100%;
  padding: .5rem .6rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  background: #fff;
}
</style>
