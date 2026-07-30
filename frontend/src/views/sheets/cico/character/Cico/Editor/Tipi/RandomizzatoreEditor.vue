<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import BaseItemEditor from '../BaseItemEditor.vue'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {CampoLabel} from '../../../../../../../models/dto/UpdateItemRequest'
import {Categoria, getCategorie} from '../../../../../../../service/RandomizzatoreService'

const props = defineProps<{ item: ItemDB; readonly?: boolean; mode?: 'edit' | 'create' }>()
const emit = defineEmits<{ (e: 'saved'): void; (e: 'cancel'): void }>()

const router = useRouter()

// Come per il TAG: le opzioni devono esserci al primo render, altrimenti preload() non
// riconosce le label già salvate e le degrada a label generiche.
const categorie = ref<Categoria[]>([])
const pronto = ref(false)

onMounted(async () => {
  try {
    categorie.value = (await getCategorie()).data
  } catch (e) {
    console.error('Errore caricamento categorie:', e)
  } finally {
    pronto.value = true
  }
})

const opzioniCategorie = computed(() =>
    categorie.value.map(c => ({value: String(c.id), label: c.nome})))

const CAMPI = computed<CampoLabel[]>(() => [
  {
    key: 'RAND_SCELTA',
    label: 'Categorie da scegliere all\'uso',
    multiValore: true,
    options: opzioniCategorie.value,
  },
  {
    key: 'RAND_PRESENTE',
    label: 'Categorie che l\'oggetto deve avere',
    multiValore: true,
    options: opzioniCategorie.value,
  },
  {
    key: 'RAND_COMBINA',
    label: 'Combinazione dei pesi',
    tipo: 'select',
    placeholder: 'Prodotto (predefinito)',
    options: [
      {value: 'PRODOTTO', label: 'Prodotto — premia chi è forte su tutte le categorie'},
      {value: 'SOMMA', label: 'Somma — una categoria debole si compensa con una forte'},
    ],
  },
])
</script>

<template>
  <div v-if="!pronto" class="caricamento">Caricamento categorie…</div>
  <div v-else>
    <div v-if="props.mode === 'edit'" class="prova">
      <button type="button" class="btn-prova" @click="router.push(`/randomizzatore/${props.item.id}`)">
        🎲 Apri il randomizzatore
      </button>
    </div>
    <BaseItemEditor
        :item="props.item"
        :readonly="props.readonly"
        :mode="props.mode"
        titolo="Randomizzatore"
        :campi-label="CAMPI"
        campi-label-titolo="Configurazione Randomizzatore"
        @saved="emit('saved')"
        @cancel="emit('cancel')"
    />
  </div>
</template>

<style scoped>
.caricamento { font-size: .9rem; opacity: .7; padding: .5rem 0; }
.prova { display: flex; justify-content: flex-end; margin-bottom: .5rem; }
.btn-prova {
  border: 1px solid #c7d2fe; background: #eef2ff; color: #3730a3;
  border-radius: .5rem; padding: .4rem .8rem; cursor: pointer; font-weight: 600; font-size: .85rem;
}
.btn-prova:hover { background: #e0e7ff; }
</style>
