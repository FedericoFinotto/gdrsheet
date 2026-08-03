<script setup lang="ts">
import BaseItemEditor from '../BaseItemEditor.vue'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {CampoLabel} from '../../../../../../../models/dto/UpdateItemRequest'

const props = defineProps<{ item: ItemDB; readonly?: boolean; mode?: 'edit' | 'create' }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void; (e: 'savedResta', item: { id: number }): void }>()

// Stessi campi di un Talento (vedi TalentoEditor.vue): gli Skill Trick di Complete Scoundrel
// hanno la stessa forma (categoria, prerequisito, beneficio) dei talenti.
const CAMPI: CampoLabel[] = [
  {key: 'PAGE', label: 'Pagina', placeholder: 'es. 47'},
  {key: 'LINK', label: 'Link di riferimento', placeholder: 'https://dndtools.org/...'},
  {key: 'CATEGORY', label: 'Categorie', multiValore: true, placeholder: 'es. Movement, Mental, Manipulation, Interaction'},
  {key: 'PREREQUISITE', label: 'Prerequisito', textarea: true},
  {key: 'BENEFIT', label: 'Beneficio', textarea: true},
]
</script>

<template>
  <BaseItemEditor
      :item="props.item"
      :readonly="props.readonly"
      :mode="props.mode"
      titolo="Skill Trick"
      :campi-label="CAMPI"
      campi-label-titolo="Dati Skill Trick"
      @saved="emit('saved')"
      @saved-resta="emit('savedResta', $event)"
      @cancel="emit('cancel')"
  />
</template>
