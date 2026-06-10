<script setup lang="ts">
import BaseItemEditor from '../BaseItemEditor.vue'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {CampoLabel} from '../../../../../../../models/dto/UpdateItemRequest'

const props = defineProps<{ item: ItemDB; readonly?: boolean; mode?: 'edit' | 'create' }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void }>()

const CAMPI: CampoLabel[] = [
  {key: 'TAGLIA', label: 'Taglia', placeholder: 'Es.: M'},
  {key: 'LINGUE', label: 'Lingue', placeholder: 'Lingue automatiche'},
]
</script>

<template>
  <BaseItemEditor
      :item="props.item"
      :readonly="props.readonly"
      :mode="props.mode"
      titolo="Razza"
      :campi-label="CAMPI"
      @saved="emit('saved')"
      @cancel="emit('cancel')"
  />
</template>
