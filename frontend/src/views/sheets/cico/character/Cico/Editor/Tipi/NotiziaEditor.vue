<script setup lang="ts">
import BaseItemEditor from '../BaseItemEditor.vue'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {CampoLabel} from '../../../../../../../models/dto/UpdateItemRequest'

const props = defineProps<{ item: ItemDB; readonly?: boolean; mode?: 'edit' | 'create' }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void }>()

const CAMPI: CampoLabel[] = [
  {key: 'ABILITATA', label: 'Abilitata', tipo: 'checkbox'},
  {key: 'DATA_INIZIO', label: 'Data inizio', tipo: 'datetime-local'},
  {key: 'DATA_FINE', label: 'Data fine', tipo: 'datetime-local'},
]
</script>

<template>
  <BaseItemEditor
      :item="props.item"
      :readonly="props.readonly"
      :mode="props.mode"
      titolo="Notizia"
      :campi-label="CAMPI"
      campi-label-titolo="Visibilità"
      minimal
      @saved="emit('saved')"
      @cancel="emit('cancel')"
  />
</template>
