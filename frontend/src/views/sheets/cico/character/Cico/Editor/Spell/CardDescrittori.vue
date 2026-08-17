<script setup lang="ts">
type BoolMap = Record<string, boolean>

defineProps<{
  descrittori: BoolMap  // reactive del genitore, mutata per riferimento (v-model sulle singole chiavi)
  descs: string[]
  disabled?: boolean
}>()
</script>

<template>
  <fieldset class="components">
    <legend class="sr-only">Descrittori</legend>
    <label v-for="d in descs" :key="d" class="chk">
      <input type="checkbox" v-model="descrittori[d]" :disabled="disabled"/>
      <span class="chk-label">{{ d }}</span>
    </label>
  </fieldset>
</template>

<style scoped>
.components { margin: 0; border: 0; padding: 0; }
.components legend { display: none; }
.chk { display: inline-flex; align-items: center; gap: .5rem; margin: 0 .75rem .5rem 0; }
.chk input { accent-color: #2563eb; }
.sr-only {
  position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); border: 0;
}
</style>
