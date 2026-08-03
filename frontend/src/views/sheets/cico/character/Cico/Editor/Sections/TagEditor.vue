<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import SearchSelect from '../../../../../../../components/SearchSelect.vue'
import {Categoria, getCategorie, ItemTag} from '../../../../../../../service/RandomizzatoreService'

const props = defineProps<{
  modelValue: ItemTag[]
  disabled?: boolean
}>()
const emit = defineEmits<{ (e: 'update:modelValue', v: ItemTag[]): void }>()

// Categoria e tag sono in due tendine separate solo per comodità di scelta: la categoria
// non viene salvata (il backend la ricava sempre dal tag), serve solo a filtrare i tag.
const categorie = ref<Categoria[]>([])
const caricamento = ref(true)
const errore = ref<string | null>(null)

// categoria "di lavoro" per ogni riga: inizializzata da quella che il backend ha restituito
const categoriaRiga = ref<Record<number, number | null>>({})

onMounted(async () => {
  try {
    categorie.value = (await getCategorie()).data
  } catch (e: any) {
    errore.value = 'Impossibile caricare le categorie'
  } finally {
    caricamento.value = false
  }
  props.modelValue.forEach((r, i) => categoriaRiga.value[i] = r.idCategoria ?? categoriaDelTag(r.idTag))
})

const opzioniCategorie = computed(() =>
    categorie.value.map(c => ({value: c.id, label: c.nome})))

function opzioniTag(idCategoria: number | null | undefined) {
  if (idCategoria == null) return []
  const cat = categorie.value.find(c => c.id === idCategoria)
  return (cat?.tag ?? []).map(t => ({value: t.id, label: t.nome}))
}

function categoriaDelTag(idTag: number | null | undefined): number | null {
  if (idTag == null) return null
  for (const c of categorie.value) {
    if (c.tag.some(t => t.id === idTag)) return c.id
  }
  return null
}

function aggiorna(idx: number, patch: Partial<ItemTag>) {
  emit('update:modelValue', props.modelValue.map((r, i) => i === idx ? {...r, ...patch} : r))
}

function cambiaCategoria(idx: number, idCategoria: number | null) {
  categoriaRiga.value[idx] = idCategoria
  // il tag scelto non appartiene più alla categoria selezionata: va azzerato
  aggiorna(idx, {idTag: null})
}

function aggiungi() {
  emit('update:modelValue', [...props.modelValue, {idTag: null, peso: 1}])
  categoriaRiga.value[props.modelValue.length] = null
}

function rimuovi(idx: number) {
  emit('update:modelValue', props.modelValue.filter((_, i) => i !== idx))
  // reindicizza le categorie di lavoro
  const next: Record<number, number | null> = {}
  props.modelValue.forEach((_, i) => {
    if (i < idx) next[i] = categoriaRiga.value[i]
    else if (i > idx) next[i - 1] = categoriaRiga.value[i]
  })
  categoriaRiga.value = next
}
</script>

<template>
  <div class="tag-editor">
    <div v-if="caricamento" class="stato">Caricamento categorie…</div>
    <div v-else-if="errore" class="stato errore">{{ errore }}</div>
    <template v-else>
      <div v-if="!categorie.length" class="stato">
        Nessuna categoria definita. Crea prima degli item di tipo Categoria e Tag.
      </div>

      <div v-for="(riga, i) in modelValue" :key="i" class="tag-row">
        <SearchSelect :model-value="categoriaRiga[i] ?? null" :options="opzioniCategorie"
                      placeholder="Categoria…" :disabled="disabled"
                      @update:model-value="(v) => cambiaCategoria(i, v as number | null)"/>
        <SearchSelect :model-value="riga.idTag" :options="opzioniTag(categoriaRiga[i])"
                      placeholder="Tag…" :disabled="disabled || categoriaRiga[i] == null"
                      @update:model-value="(v) => aggiorna(i, {idTag: v as number | null})"/>
        <input class="peso" type="number" min="0" step="0.1" inputmode="decimal"
               :value="riga.peso" :disabled="disabled" placeholder="peso"
               @input="aggiorna(i, {peso: Number(($event.target as HTMLInputElement).value)})"/>
        <button type="button" class="btn-del" :disabled="disabled" title="Rimuovi"
                @click="rimuovi(i)">✕</button>
      </div>

      <button type="button" class="btn-add" :disabled="disabled || !categorie.length" @click="aggiungi">
        + Aggiungi tag
      </button>
      <p class="nota">Peso 0 o vuoto equivale a non avere il tag: la riga viene scartata al salvataggio.</p>
    </template>
  </div>
</template>

<style scoped>
.tag-editor { display: grid; gap: .4rem; }
.stato { font-size: .85rem; opacity: .7; }
.stato.errore { color: var(--danger-text); }
.tag-row {
  display: grid;
  grid-template-columns: 1fr 1fr 5rem auto;
  gap: .4rem;
  align-items: center;
}
@media (max-width: 700px) {
  .tag-row { grid-template-columns: 1fr 1fr; }
  .tag-row .peso { grid-column: 1; }
}
.peso {
  width: 100%; padding: .45rem .55rem; border: 1px solid var(--hairline);
  border-radius: .5rem; background: var(--surface-0); color: var(--text-strong); text-align: center;
  font-variant-numeric: tabular-nums; min-width: 0;
}
.btn-del {
  border: 1px solid var(--danger-border); background: var(--danger-bg); color: var(--danger-text);
  border-radius: .5rem; padding: .35rem .6rem; cursor: pointer;
}
.btn-add {
  justify-self: start; border: 1px dashed var(--hairline); background: var(--surface-0);
  color: var(--text-strong);
  border-radius: .5rem; padding: .4rem .7rem; cursor: pointer;
}
.nota { margin: 0; font-size: .75rem; opacity: .6; }
button:disabled { opacity: .6; cursor: default; }
</style>
