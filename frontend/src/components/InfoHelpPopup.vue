<script setup lang="ts">
import {VoceInfo} from "../function/labelCatalog";

defineProps<{
  titolo: string
  voci: VoceInfo[]
  nota?: string
}>()
</script>

<template>
  <div class="info-help">
    <h3 class="ih-title">{{ titolo }}</h3>
    <p v-if="nota" class="ih-nota">{{ nota }}</p>
    <div class="ih-list">
      <div v-for="v in voci" :key="v.nome" class="ih-row">
        <code class="ih-nome">{{ v.nome }}</code>
        <span class="ih-desc">{{ v.descrizione }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.info-help {
  max-width: 32rem;
  display: flex;
  flex-direction: column;
  gap: .6rem;
}
.ih-title { margin: 0; font-size: 1.05rem; }
.ih-nota { margin: 0; font-size: .85rem; color: #6b7280; }
.ih-list { display: flex; flex-direction: column; gap: .5rem; }
.ih-row {
  display: grid;
  grid-template-columns: minmax(6rem, auto) 1fr;
  gap: .6rem;
  align-items: start;
}
.ih-nome {
  font-size: .8rem;
  font-weight: 700;
  color: #3730a3;
  background: #eef2ff;
  padding: .1rem .4rem;
  border-radius: .35rem;
  white-space: nowrap;
}
.ih-desc { font-size: .85rem; line-height: 1.35; }
@media (max-width: 600px) {
  .ih-row { grid-template-columns: 1fr; gap: .15rem; }
  .ih-nome { justify-self: start; }
}
</style>
