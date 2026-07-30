<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import BaseItemEditor from '../BaseItemEditor.vue'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {CampoLabel} from '../../../../../../../models/dto/UpdateItemRequest'
import {Categoria, getCategorie} from '../../../../../../../service/RandomizzatoreService'

const props = defineProps<{ item: ItemDB; readonly?: boolean; mode?: 'edit' | 'create' }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void }>()

// Le opzioni della tendina devono essere note al PRIMO render di BaseItemEditor: preload()
// legge campiLabel una volta sola, e una key non ancora dichiarata finirebbe tra le label
// generiche. Per questo l'editor si monta solo a categorie caricate.
const categorie = ref<Categoria[]>([])
const pronto = ref(false)

onMounted(async () => {
  try {
    categorie.value = (await getCategorie()).data
  } catch (e) {
    console.error('Errore caricamento categorie:', e)
  } finally {
    pronto.value = true
  }
})

const CAMPI = computed<CampoLabel[]>(() => [
  {
    key: 'CATEGORIA',
    label: 'Categoria di appartenenza',
    tipo: 'select',
    placeholder: 'Scegli la categoria…',
    options: categorie.value.map(c => ({value: String(c.id), label: c.nome})),
  },
])
</script>

<template>
  <div v-if="!pronto" class="caricamento">Caricamento categorie…</div>
  <BaseItemEditor
      v-else
      :item="props.item"
      :readonly="props.readonly"
      :mode="props.mode"
      titolo="Tag"
      :campi-label="CAMPI"
      campi-label-titolo="Dati Tag"
      @saved="emit('saved')"
      @cancel="emit('cancel')"
  />
</template>

<style scoped>
.caricamento { font-size: .9rem; opacity: .7; padding: .5rem 0; }
</style>
