<script setup lang="ts">
import BaseItemEditor from '../BaseItemEditor.vue'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {CampoLabel} from '../../../../../../../models/dto/UpdateItemRequest'

const props = defineProps<{ item: ItemDB; readonly?: boolean; mode?: 'edit' | 'create' }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void }>()

// TODO stub: gestire gli ATTACCO figli (collegamenti) con AttaccoEditor inline
const CAMPI: CampoLabel[] = [
  {key: 'TAGLIA', label: 'Taglia', placeholder: 'Es.: M'},
  {key: 'REQ_COMP', label: 'Competenza richiesta', placeholder: 'Es.: Armi da guerra'},
]
</script>

<template>
  <BaseItemEditor
      :item="props.item"
      :readonly="props.readonly"
      :mode="props.mode"
      titolo="Arma"
      :campi-label="CAMPI"
      @saved="emit('saved')"
      @cancel="emit('cancel')"
  />
</template>
