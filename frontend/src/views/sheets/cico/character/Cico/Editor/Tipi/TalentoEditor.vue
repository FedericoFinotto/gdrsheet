<script setup lang="ts">
import BaseItemEditor from '../BaseItemEditor.vue'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {CampoLabel} from '../../../../../../../models/dto/UpdateItemRequest'

const props = defineProps<{ item: ItemDB; readonly?: boolean; mode?: 'edit' | 'create' }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void }>()

// Campi tipici di un talento stile dndtools.org (vedi scripts/dndtools-scraper).
// EN_NAME e MANUALE_SP sono già gestiti da BaseItemEditor, non vanno ripetuti qui.
const CAMPI: CampoLabel[] = [
  {key: 'PAGE', label: 'Pagina', placeholder: 'es. 47'},
  {key: 'LINK', label: 'Link di riferimento', placeholder: 'https://dndtools.org/feats/...'},
  {key: 'CATEGORY', label: 'Categorie', multiValore: true, placeholder: 'es. General'},
  {key: 'PREREQUISITE', label: 'Prerequisito', textarea: true},
  {key: 'REQUIRED_FOR', label: 'Richiesto per', textarea: true},
  {key: 'BENEFIT', label: 'Beneficio', textarea: true},
  {key: 'NORMAL', label: 'Normale', textarea: true},
  {key: 'SPECIAL', label: 'Speciale', textarea: true},
  {key: 'EXTRA', label: 'Altre sezioni', multiValore: true, textarea: true, placeholder: 'Titolo: testo'},
]
</script>

<template>
  <BaseItemEditor
      :item="props.item"
      :readonly="props.readonly"
      :mode="props.mode"
      titolo="Talento"
      :campi-label="CAMPI"
      campi-label-titolo="Dati Talento"
      @saved="emit('saved')"
      @cancel="emit('cancel')"
  />
</template>
