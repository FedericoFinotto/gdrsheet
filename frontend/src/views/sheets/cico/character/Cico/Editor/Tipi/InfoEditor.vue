<script setup lang="ts">
// Stesso schema di QuestEditor.vue, senza il checkbox "Completata" (un INFO non ha uno stato di
// completamento: il suo contenuto è la sezione Note, ciascuna con una propria visibilità).
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
// sotto-info creato dal flusso "+ Aggiungi sotto-info" (SottoQuestEditor): nessun ambito da
// scegliere, eredita la visibilità dal padre a cui viene collegato.
const isSubInfo = computed(() => props.mode === 'create' && !!route.query.link)

// Solo Party/Mondo: gli INFO si gestiscono esclusivamente dalla scheda party, non esiste un
// ambito Personaggio (a differenza delle QUEST, che hanno anche una pagina personale).
const AMBITO_OPTS = [
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
      titolo="Info"
      :minimal="true"
      @saved="emit('saved')"
      @saved-resta="emit('savedResta', $event)"
      @cancel="emit('cancel')"
      @saved-stay="emit('savedStay')"
  >
    <template #specifico="{disabled, questScope, setQuestScope, archiviata, setArchiviata}">
      <template v-if="props.mode === 'create' && !isSubInfo && !questScope">
        {{ setQuestScope('PARTY') }}
      </template>
      <section class="fold info-info">
        <div class="fold-head static">
          <span class="fold-title">Info</span>
        </div>
        <div class="fold-body">
          <label v-if="props.mode === 'create' && !isSubInfo" class="field">
            <span class="lbl">Ambito</span>
            <SearchSelect :model-value="questScope" :options="AMBITO_OPTS" :disabled="disabled" :sort="false"
                          @update:model-value="val => setQuestScope(String(val))"/>
          </label>
          <label class="field-checkbox">
            <input type="checkbox" :checked="archiviata" :disabled="disabled"
                   @change="e => setArchiviata((e.target as HTMLInputElement).checked)"/>
            <span>Archiviato (non caricato più in automatico entrando negli info)</span>
          </label>
        </div>
      </section>
    </template>
  </BaseItemEditor>
</template>

<style scoped>
.info-info { border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0); }
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
