<script setup lang="ts">
import BaseItemEditor from '../BaseItemEditor.vue'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {CampoLabel} from '../../../../../../../models/dto/UpdateItemRequest'
import {TAGLIE_OPTIONS_NUMERICHE} from '../../../../../../../function/Utils'

const props = defineProps<{ item: ItemDB; readonly?: boolean; mode?: 'edit' | 'create' }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void; (e: 'savedResta', item: { id: number }): void }>()

const CAMPI: CampoLabel[] = [
  {key: 'PESO', label: 'Peso (kg)', placeholder: 'Es.: 1.5'},
  // NON è la taglia fisica dell'arma (quella è "Taglia oggetto", sotto "Info Oggetto"): questo
  // campo, se impostato, SOSTITUISCE la taglia di chi la impugna (es. arma magica di crescita).
  {key: 'TAGLIA', label: 'Modifica taglia personaggio', tipo: 'select', options: TAGLIE_OPTIONS_NUMERICHE},
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
      @saved-resta="emit('savedResta', $event)"
      @cancel="emit('cancel')"
  />
</template>
