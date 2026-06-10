<script setup lang="ts">
import BaseItemEditor from '../BaseItemEditor.vue'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {CampoLabel} from '../../../../../../../models/dto/UpdateItemRequest'

const props = defineProps<{ item: ItemDB; readonly?: boolean; mode?: 'edit' | 'create' }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void }>()

const CAMPI: CampoLabel[] = [
  {key: 'TPC', label: 'Tiro per colpire', placeholder: 'Formula, es.: BAB+FOR'},
  {key: 'TPD', label: 'Danni', placeholder: 'Formula, es.: 1d8+FOR'},
  {key: 'TDANNO', label: 'Tipo danni', placeholder: 'Es.: Perforante'},
  {key: 'TRANGE', label: 'Range', placeholder: 'Es.: 9 m'},
  {key: 'TTS', label: 'Tiro salvezza', placeholder: 'Es.: Tempra dimezza'},
]
</script>

<template>
  <BaseItemEditor
      :item="props.item"
      :readonly="props.readonly"
      :mode="props.mode"
      titolo="Attacco"
      :campi-label="CAMPI"
      @saved="emit('saved')"
      @cancel="emit('cancel')"
  />
</template>
