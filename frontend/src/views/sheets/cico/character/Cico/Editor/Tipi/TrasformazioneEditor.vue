<script setup lang="ts">
import BaseItemEditor from '../BaseItemEditor.vue'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {CampoLabel} from '../../../../../../../models/dto/UpdateItemRequest'

const props = defineProps<{ item: ItemDB; readonly?: boolean; mode?: 'edit' | 'create' }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void }>()

const CAMPI: CampoLabel[] = [
  {key: 'GRP_TRASF', label: 'Gruppo trasformazione', placeholder: 'Gruppo di mutua esclusione'},
  {key: 'TAGLIA', label: 'Taglia', placeholder: 'Es.: G'},
  {key: 'DV', label: 'Dadi vita', placeholder: 'DV della forma'},
]
</script>

<template>
  <BaseItemEditor
      :item="props.item"
      :readonly="props.readonly"
      :mode="props.mode"
      titolo="Trasformazione"
      :campi-label="CAMPI"
      @saved="emit('saved')"
      @cancel="emit('cancel')"
  />
</template>
