<script setup lang="ts">
import {computed} from 'vue'
import {useRoute} from 'vue-router'
import BaseItemEditor from '../BaseItemEditor.vue'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import SearchSelect from '../../../../../../../components/SearchSelect.vue'

const props = defineProps<{
  item: ItemDB
  readonly?: boolean
  mode?: 'edit' | 'create'
  idPersonaggio?: number
  idParty?: number
}>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void; (e: 'savedStay'): void; (e: 'savedResta', item: { id: number }): void }>()

const route = useRoute()
// sotto-quest creata dal flusso "crea e collega": nessun ambito da scegliere, eredita
// la visibilità dalla quest padre a cui viene collegata.
const isSubQuest = computed(() => props.mode === 'create' && !!route.query.link)

const AMBITO_OPTS = [
  {value: 'PERSONAGGIO', label: 'Personaggio'},
  {value: 'PARTY', label: 'Party'},
  {value: 'MONDO', label: 'Mondo (visibile a tutti i party)'},
]
</script>

<template>
  <BaseItemEditor
      :item="props.item"
      :readonly="props.readonly"
      :mode="props.mode"
      :id-personaggio="props.idPersonaggio"
      :id-party="props.idParty"
      titolo="Quest"
      :minimal="true"
      @saved="emit('saved')"
      @saved-resta="emit('savedResta', $event)"
      @cancel="emit('cancel')"
      @saved-stay="emit('savedStay')"
  >
    <template #specifico="{disabled, questScope, setQuestScope, completata, setCompletata, archiviata, setArchiviata}">
      <template v-if="props.mode === 'create' && !isSubQuest && !questScope">
        {{ setQuestScope(props.idPersonaggio ? 'PERSONAGGIO' : 'PARTY') }}
      </template>
      <section class="fold quest-info">
        <div class="fold-head static">
          <span class="fold-title">Info Quest</span>
        </div>
        <div class="fold-body">
          <label v-if="props.mode === 'create' && !isSubQuest" class="field">
            <span class="lbl">Ambito della quest</span>
            <SearchSelect :model-value="questScope" :options="AMBITO_OPTS" :disabled="disabled" :sort="false"
                          @update:model-value="val => setQuestScope(String(val))"/>
          </label>
          <label class="field-checkbox">
            <input type="checkbox" :checked="completata" :disabled="disabled"
                   @change="e => setCompletata((e.target as HTMLInputElement).checked)"/>
            <span>Completata (solo se senza sotto-quest)</span>
          </label>
          <label class="field-checkbox">
            <input type="checkbox" :checked="archiviata" :disabled="disabled"
                   @change="e => setArchiviata((e.target as HTMLInputElement).checked)"/>
            <span>Archiviata (non caricata più in automatico entrando nelle quest)</span>
          </label>
        </div>
      </section>
    </template>
  </BaseItemEditor>
</template>

<style scoped>
.quest-info { border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0); }
.fold-head.static {
  padding: .5rem .75rem; background: var(--btn-bg); border-bottom: 1px solid var(--hairline);
}
.fold-title { font-weight: 600; }
.fold-body { padding: .6rem .75rem; display: grid; gap: .6rem; }
.field { display: grid; gap: .35rem; margin: 0; }
.lbl { font-size: .8rem; font-weight: 600; opacity: .85; }
.field-checkbox { display: inline-flex; align-items: center; gap: .5rem; font-size: .85rem; cursor: pointer; width: fit-content; }
.field-checkbox input { width: auto; }
</style>
