<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import MultiSelectField from './MultiSelectField.vue'
import {getElencoRandomizzatori, Ref as RandRef} from '../../../../../../../service/RandomizzatoreService'

/**
 * Randomizzatori innescati quando questo oggetto viene estratto (label RAND_TRIGGER,
 * multi-valore). È così che si costruiscono le catene: es. "Barca di Tiranna" innesca
 * il randomizzatore "Tesori".
 */
const props = defineProps<{
  modelValue: string[]
  disabled?: boolean
  /** id dell'item corrente: si esclude da solo, per non innescare se stesso */
  idCorrente?: number
}>()
const emit = defineEmits<{ (e: 'update:modelValue', v: string[]): void }>()

const elenco = ref<RandRef[]>([])
const caricamento = ref(true)

onMounted(async () => {
  try {
    elenco.value = (await getElencoRandomizzatori()).data
  } catch (e) {
    console.error('Errore caricamento randomizzatori:', e)
  } finally {
    caricamento.value = false
  }
})

const opzioni = computed(() =>
    elenco.value
        .filter(r => r.id !== props.idCorrente)
        .map(r => ({value: String(r.id), label: r.nome})))
</script>

<template>
  <div class="trigger-editor">
    <div v-if="caricamento" class="stato">Caricamento randomizzatori…</div>
    <div v-else-if="!opzioni.length" class="stato">Nessun randomizzatore disponibile.</div>
    <template v-else>
      <MultiSelectField :model-value="modelValue" :options="opzioni" :disabled="disabled"
                        @update:model-value="v => emit('update:modelValue', v)"/>
      <p class="nota">
        Quando questo oggetto viene estratto, i randomizzatori selezionati vengono lanciati
        subito dopo. Le categorie già scelte (es. la difficoltà) vengono ereditate.
      </p>
    </template>
  </div>
</template>

<style scoped>
.trigger-editor { display: grid; gap: .4rem; }
.stato { font-size: .85rem; opacity: .7; }
.nota { margin: 0; font-size: .75rem; opacity: .6; }
</style>
