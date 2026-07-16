<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {getItem} from '../../../../../../../service/PersonaggioService'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'

const props = defineProps<{ itemId: number; nome?: string }>()

const item = ref<ItemDB | null>(null)
const loading = ref(true)

onMounted(async () => {
  try {
    item.value = (await getItem(props.itemId)).data
  } catch (e) {
    console.error('Errore caricamento descrizione item:', e)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="descr-popup">
    <h3 class="descr-title">{{ item?.nome ?? nome }}</h3>
    <p v-if="loading" class="muted">Caricamento…</p>
    <div v-else-if="item?.descrizione" class="descr-html" v-safe-html="item.descrizione"></div>
    <p v-else class="muted">Nessuna descrizione.</p>
  </div>
</template>

<style scoped>
.descr-popup { padding: .25rem; }
.descr-title { margin: 0 0 .6rem; font-size: 1.05rem; }
.descr-html { white-space: pre-wrap; }
.descr-html :deep(ul),
.descr-html :deep(ol) { margin: .3rem 0 .3rem 1.2rem; padding: 0; }
.descr-html :deep(h3) { margin: .4rem 0 .2rem; font-size: 1rem; }
.descr-html :deep(p) { margin: .3rem 0; }
.muted { opacity: .7; font-size: .9rem; }
</style>
