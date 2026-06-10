<script setup lang="ts">
import BaseItemEditor from '../BaseItemEditor.vue'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {CampoLabel} from '../../../../../../../models/dto/UpdateItemRequest'

const props = defineProps<{ item: ItemDB; readonly?: boolean; mode?: 'edit' | 'create' }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void }>()

// TODO stub: gestire avanzamenti per livello (BAB, TS, contenuti concessi)
const CAMPI: CampoLabel[] = [
  {key: 'DV', label: 'Dado vita', placeholder: 'Es.: 1d10'},
  {key: 'SP_SLOT', label: 'Spell slot', placeholder: 'Configurazione slot incantesimi'},
  {key: 'ABCLASSE', label: 'Abilità di classe', placeholder: 'Lista abilità di classe', textarea: true},
]
</script>

<template>
  <BaseItemEditor
      :item="props.item"
      :readonly="props.readonly"
      :mode="props.mode"
      titolo="Classe"
      :campi-label="CAMPI"
      @saved="emit('saved')"
      @cancel="emit('cancel')"
  />
</template>
