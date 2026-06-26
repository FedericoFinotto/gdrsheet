<script setup lang="ts">
import {computed, ref} from 'vue'
import {Classe} from '../../../../../../models/dto/Classe'
import TabExpandable from "../../../../../../../components/TabExpandable.vue";
import SearchSelect from "../../../../../../../components/SearchSelect.vue";

const props = defineProps<{
  disabled: boolean
  summary: string
  classi: Classe[]
  classeDetail: any | null
  livelliDisponibili: number[]
  classeId: number | null
  maledizioneNome: string | null
  livelliClasse: Record<number, boolean>
}>()

const emit = defineEmits<{
  (e: 'update:classeId', v: number | null): void
  (e: 'update:maledizioneNome', v: string | null): void
  (e: 'update:livelliClasse', v: Record<number, boolean>): void
}>()

const classeIdLocal = computed({
  get: () => props.classeId,
  set: v => emit('update:classeId', v)
})

const maledizioneNomeLocal = computed({
  get: () => props.maledizioneNome ?? '',
  set: v => emit('update:maledizioneNome', v?.trim() ? v : null)
})

const livelliClasseLocal = computed({
  get: () => props.livelliClasse,
  set: v => emit('update:livelliClasse', v)
})

// filtro Classe / Razza / Tutti
const filtroTipo = ref<string>('')
const FILTRO_OPT = [
  {value: '', label: 'Tutti'},
  {value: 'CLASSE', label: 'Classe'},
  {value: 'RAZZA', label: 'Razza'},
]
const opzioniClasse = computed(() =>
    props.classi
        .filter((c: any) => !filtroTipo.value || c.tipo === filtroTipo.value)
        .map((c: any) => ({
          value: c.id,
          label: c.nome,
          hint: c.tipo === 'RAZZA' ? 'Razza' : 'Classe',
        }))
)
</script>

<template>
  <TabExpandable title="Classe / Maledizione" :defaultOpen="true">
    <template #summary><span class="wrap">{{ summary }}</span></template>
    <template #content>
      <!-- Classe -->
      <div class="row classe-row">
        <label class="field tipo-filter">
          <span class="lbl">Tipo</span>
          <SearchSelect v-model="filtroTipo" :disabled="disabled" :options="FILTRO_OPT" :sort="false"/>
        </label>
        <label class="field grow">
          <span class="lbl">Classe / Razza <span class="required">*</span></span>
          <SearchSelect v-model="classeIdLocal" :disabled="disabled" placeholder="— Seleziona —"
                        :options="opzioniClasse"/>
        </label>
      </div>

      <!-- Maledizione -->
      <div class="row">
        <label class="field full-width">
          <span class="lbl">Maledizione (opzionale)</span>
          <input type="text"
                 v-model="maledizioneNomeLocal"
                 :disabled="disabled"
                 placeholder="Nessuna maledizione"/>
        </label>
      </div>

      <!-- Livelli di classe -->
      <div v-if="classeDetail" class="levels-wrap">
        <div class="lbl" style="margin-bottom:.35rem;">Seleziona livelli di classe</div>
        <div class="levels-grid">
          <label v-for="lv in livelliDisponibili" :key="'lv-'+lv" class="level-pill">
            <input type="checkbox"
                   :disabled="disabled"
                   v-model="livelliClasseLocal[lv]"/>
            <span>{{ lv }}</span>
          </label>
        </div>
      </div>
    </template>
  </TabExpandable>
</template>

<style scoped>
.row {
  display: grid;
  gap: .5rem;
}

.field {
  display: grid;
  gap: .35rem;
}

.full-width {
  grid-column: 1 / -1;
}

.classe-row { display: flex; gap: .5rem; align-items: flex-end; }
.tipo-filter { flex: 0 0 8rem; }
.classe-row .grow { flex: 1; }

.lbl {
  font-size: .8rem;
  font-weight: 600;
  opacity: .85;
}

.required {
  color: #ef4444;
}

select, input[type="text"] {
  width: 100%;
  padding: .5rem .6rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  background: #fff;
}

.wrap {
  white-space: normal;
  word-break: break-word;
}

.levels-wrap {
  margin-top: .5rem;
}

.levels-grid {
  display: flex;
  flex-wrap: wrap;
  gap: .4rem .5rem;
}

.level-pill {
  display: inline-flex;
  align-items: center;
  gap: .35rem;
  padding: .2rem .45rem;
  border: 1px solid #e5e7eb;
  border-radius: .5rem;
  background: #fff;
}

.level-pill input {
  accent-color: #2563eb;
}
</style>
