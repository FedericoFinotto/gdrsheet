<script setup lang="ts">
import {useRouter} from 'vue-router'

const props = defineProps<{
  idPersonaggio: number
  tipo?: string      // tipo preselezionato (es. TALENTO); assente = scelta nella pagina di creazione
  label?: string
}>()

const router = useRouter()

function vai() {
  const base = props.tipo ? `/itemcreate/${props.tipo}` : '/itemcreate'
  router.push(`${base}?personaggio=${props.idPersonaggio}`)
}
</script>

<template>
  <button type="button" class="btn-add-item" @click="vai">
    <span class="plus">+</span>
    <span>{{ label ?? 'Aggiungi' }}</span>
  </button>
</template>

<style scoped>
.btn-add-item {
  display: inline-flex;
  align-items: center;
  gap: .35rem;
  padding: .4rem .8rem;
  border: 1px dashed var(--hairline);
  border-radius: .5rem;
  background: var(--surface-0);
  color: var(--text-muted);
  font-weight: 600;
  font-size: .85rem;
  cursor: pointer;
}

.btn-add-item:hover { background: var(--btn-bg-hover); }
.plus { font-weight: 800; color: #2563eb; }
</style>
