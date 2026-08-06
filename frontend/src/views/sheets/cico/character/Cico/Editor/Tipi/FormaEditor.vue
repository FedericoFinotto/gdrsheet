<script setup lang="ts">
import BaseItemEditor from '../BaseItemEditor.vue'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {CampoLabel} from '../../../../../../../models/dto/UpdateItemRequest'
import {TAGLIE_OPTIONS_NUMERICHE} from '../../../../../../../function/Utils'

const props = defineProps<{ item: ItemDB; readonly?: boolean; mode?: 'edit' | 'create' }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void; (e: 'savedResta', item: { id: number }): void }>()

const CAMPI: CampoLabel[] = [
  {key: 'GRP_TRASF', label: 'Gruppo', placeholder: 'Gruppo di mutua esclusione'},
  // Taglia che il personaggio ASSUME in questa forma (sostituisce la sua taglia base).
  {key: 'TAGLIA', label: 'Taglia assunta dal personaggio', tipo: 'select', options: TAGLIE_OPTIONS_NUMERICHE},
  {key: 'DV', label: 'Dadi vita', placeholder: 'DV della forma'},
]
</script>

<template>
  <BaseItemEditor
      :item="props.item"
      :readonly="props.readonly"
      :mode="props.mode"
      titolo="Forma"
      :campi-label="CAMPI"
      @saved="emit('saved')"
      @saved-resta="emit('savedResta', $event)"
      @cancel="emit('cancel')"
  />
</template>
