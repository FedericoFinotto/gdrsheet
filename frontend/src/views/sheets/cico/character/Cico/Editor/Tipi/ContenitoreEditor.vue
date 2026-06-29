<script setup lang="ts">
import BaseItemEditor from '../BaseItemEditor.vue'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {CampoLabel} from '../../../../../../../models/dto/UpdateItemRequest'

const props = defineProps<{ item: ItemDB; readonly?: boolean; mode?: 'edit' | 'create' }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void }>()

const CAMPI: CampoLabel[] = [
  {key: 'PESO',                         label: 'Peso a capienza piena (kg)',   placeholder: 'Es.: 10'},
  {key: 'CAPIENZA',                     label: 'Capienza massima (kg)',         placeholder: 'Es.: 700'},
  {key: 'INCLUDI_ARMI_ABILITATE',       label: 'Includi armi equipaggiate',    tipo: 'checkbox'},
  {key: 'INCLUDI_OGGETTI_ABILITATI',    label: 'Includi oggetti equipaggiati', tipo: 'checkbox'},
  {key: 'INCLUDI_CONSUMABILI_ABILITATI',label: 'Includi consumabili equipaggiati', tipo: 'checkbox'},
]
</script>

<template>
  <BaseItemEditor
      :item="props.item"
      :readonly="props.readonly"
      :mode="props.mode"
      titolo="Contenitore"
      campi-label-titolo="Contenitore"
      :campi-label="CAMPI"
      @saved="emit('saved')"
      @cancel="emit('cancel')"
  />
</template>
