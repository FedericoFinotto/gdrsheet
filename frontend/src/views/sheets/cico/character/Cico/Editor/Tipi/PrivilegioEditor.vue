<script setup lang="ts">
import BaseItemEditor from '../BaseItemEditor.vue'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {CampoLabel} from '../../../../../../../models/dto/UpdateItemRequest'

const props = defineProps<{ item: ItemDB; readonly?: boolean; mode?: 'edit' | 'create' }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void }>()

// GRUPPO_PRIVILEGI: se un personaggio ha 2+ privilegi con lo stesso valore di questo campo (es.
// una versione potenziata dello stesso privilegio sbloccata da una classe di prestigio), in
// scheda si vede solo quello legato al LIVELLO più alto (vedi PersonaggioService, backend).
const CAMPI: CampoLabel[] = [
  {key: 'GRUPPO_PRIVILEGI', label: 'Gruppo privilegi', placeholder: 'es. FURIA'},
]
</script>

<template>
  <BaseItemEditor
      :item="props.item"
      :readonly="props.readonly"
      :mode="props.mode"
      titolo="Privilegio di Classe"
      :campi-label="CAMPI"
      @saved="emit('saved')"
      @cancel="emit('cancel')"
  />
</template>
