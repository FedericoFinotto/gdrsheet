<script setup lang="ts">
defineProps<{
  comp: Record<string, boolean>  // reactive del genitore, mutata per riferimento (v-model sulle singole chiavi)
  keys: string[]
  disabled?: boolean
}>()
</script>

<template>
  <fieldset class="components">
    <legend class="sr-only">Componenti</legend>
    <label v-for="k in keys" :key="k" class="chk">
      <input type="checkbox" v-model="comp[k]" :disabled="disabled"/>
      <span class="chk-label">{{ k }}</span>
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
