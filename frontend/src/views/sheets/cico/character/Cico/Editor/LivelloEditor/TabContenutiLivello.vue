<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import TabExpandable from '../../../../../../../components/TabExpandable.vue'
import {ItemDB} from '../../../../../../../models/entity/ItemDB'
import {Modificatore} from '../../../../../../../models/entity/Modificatore'
import {GrantRow} from "../../../../../../../models/dto/GrantRow";


const props = defineProps<{
  disabled: boolean
  classeId: number | null
  classe: any | null
  livello: ItemDB | null
  livelliSelezionati: number[]
  selectedGrantIds: Set<string>
  selectedGrants: GrantRow[]
  defaultOpen: boolean
}>()

const emit = defineEmits<{
  (e: 'update:selectedGrants', v: GrantRow[]): void
}>()

// stato locale NON computed
const grantsByLevel = ref<Record<number, GrantRow[]>>({})
const selectedGrantIdsLocal = ref<Set<string>>(new Set(props.selectedGrantIds))

// sync verso il padre quando cambia il set locale
function flushSelectedToParent() {
  const rows = getSelectedGrantRows()
  emit('update:selectedGrants', rows)
}


// helper per sapere se un id è selezionato
function isSelected(id: string): boolean {
  return selectedGrantIdsLocal.value.has(id)
}

// ------ FUNZIONE PRINCIPALE DI BUILD ------
function rebuild() {
  const out: Record<number, GrantRow[]> = {}
  const selected = new Set<string>(selectedGrantIdsLocal.value)

  if (!props.classe || !props.livello) {
    grantsByLevel.value = {}
    selectedGrantIdsLocal.value = selected
    flushSelectedToParent()
    return
  }

  const avanzamenti: any[] = Array.isArray(props.classe.avanzamento)
      ? props.classe.avanzamento
      : []

  const itemTargetsLivello =
      props.livello.child?.flatMap((x: any) => x.itemTarget) ?? []

  // CLONE della lista dei modificatori presenti sull'item di livello
  let remainingMods: Modificatore[] =
      ((props.livello.modificatori as Modificatore[]) ?? [])
          .filter(m => m.stat.id !== 'GRADI')
          .map(m => ({...m})) // clone shallow; sufficiente per matching

  // mi assicuro che i livelli siano in ordine crescente
  const livelliOrdinati = [...props.livelliSelezionati].sort((a, b) => a - b)

  for (const lv of livelliOrdinati) {
    const rows: GrantRow[] = []

    // ------- ITEM -------
    const items = avanzamenti
        .filter(a =>
            a?.itemTarget?.tipo !== 'AVANZAMENTO' &&
            Number.isFinite(Number(a?.livello)) &&
            Number(a.livello) === lv
        )

    for (const item of items) {
      const id = `item-${item.itemTarget.id}`

      // se presente tra gli item del livello -> preseleziona sempre
      if (itemTargetsLivello.find((x: any) => x.id === item.itemTarget.id)) {
        selected.add(id)
      }

      rows.push({
        id,
        descrizione: String(item.itemTarget.nome),
        tipo: 'ITEM',
        livello: lv,
        raw: item.itemTarget as ItemDB
      })
    }

    // ------- MOD -------
    const mods = avanzamenti
        .filter(a =>
            a?.itemTarget?.tipo === 'AVANZAMENTO' &&
            Number.isFinite(Number(a?.livello)) &&
            Number(a.livello) === lv
        )
        .flatMap(a => a.itemTarget.modificatori as Modificatore[])
        .filter(a => a.stat.id !== 'GRADI')

    for (const modificatore of mods) {
      const id = `mod-${modificatore.id}`

      // cerco un match nella lista "rimanente"
      const idx = remainingMods.findIndex(
          m =>
              m.stat.id === modificatore.stat.id &&
              m.valore === modificatore.valore
      )

      if (idx !== -1) {
        // match trovato: seleziono qui e lo tolgo dalla lista
        selected.add(id)
        remainingMods.splice(idx, 1)
      }

      rows.push({
        id,
        descrizione: `${modificatore.valore} ${modificatore.stat.label}`,
        tipo: 'MOD',
        livello: lv,
        raw: modificatore
      })
    }

    out[lv] = rows
  }

  grantsByLevel.value = out
  selectedGrantIdsLocal.value = selected
  flushSelectedToParent()
}


onMounted(() => {
  rebuild()
})

watch(
    () => [props.classe, props.livelliSelezionati],
    () => {
      rebuild()
    },
    {deep: true}
)

const flatGrants = computed(() =>
    Object.values(grantsByLevel.value).flat()
)

const summaryText = computed(
    () => `${selectedGrantIdsLocal.value.size}/${flatGrants.value.length} selezionati`
)

function toggleGrant(id: string, checked: boolean) {
  const set = new Set(selectedGrantIdsLocal.value)
  if (checked) set.add(id)
  else set.delete(id)
  selectedGrantIdsLocal.value = set
  flushSelectedToParent()
}

function selectAllLevel(lv: number) {
  const set = new Set(selectedGrantIdsLocal.value)
  ;(grantsByLevel.value[lv] ?? []).forEach(r => set.add(r.id))
  selectedGrantIdsLocal.value = set
  flushSelectedToParent()
}

function deselectAllLevel(lv: number) {
  const set = new Set(selectedGrantIdsLocal.value)
  ;(grantsByLevel.value[lv] ?? []).forEach(r => set.delete(r.id))
  selectedGrantIdsLocal.value = set
  flushSelectedToParent()
}

function getSelectedGrantRows(): GrantRow[] {
  const ids = selectedGrantIdsLocal.value
  const all = Object.values(grantsByLevel.value).flat()
  return all.filter(r => ids.has(r.id))
}

</script>

<template>
  <TabExpandable
      v-if="classeId && livelliSelezionati.length"
      title="Contenuti del livello"
      :defaultOpen="defaultOpen"
  >
    <template #summary>{{ summaryText }}</template>
    <template #content>
      <div class="grants">
        <div class="grant-block" v-for="lv in livelliSelezionati" :key="'gbl-'+lv">
          <div class="grant-header">
            <span class="lbl">Livello {{ lv }}</span>
            <div class="grant-actions">
              <button
                  type="button"
                  class="btn"
                  @click="selectAllLevel(lv)"
                  :disabled="disabled"
              >
                Seleziona tutto
              </button>
              <button
                  type="button"
                  class="btn ghost"
                  @click="deselectAllLevel(lv)"
                  :disabled="disabled"
              >
                Nessuno
              </button>
            </div>
          </div>
          <div class="grant-list">
            <label class="grant-row" v-for="g in grantsByLevel[lv] ?? []" :key="g.id">
              <input
                  type="checkbox"
                  :checked="isSelected(g.id)"
                  @change="toggleGrant(g.id, ($event.target as HTMLInputElement).checked)"
                  :disabled="disabled"
              />
              <span class="pill blue" v-if="g.tipo === 'ITEM'">ITEM</span>
              <span class="pill red" v-else>MOD</span>
              <span class="grant-name">{{ g.descrizione }}</span>
            </label>
            <div v-if="!(grantsByLevel[lv]?.length)" class="muted">
              Nessun contenuto per questo livello.
            </div>
          </div>
        </div>
      </div>
    </template>
  </TabExpandable>
</template>

<style scoped>
.grants {
  display: grid;
  gap: .6rem;
}

.grant-block {
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  padding: .5rem;
  background: #fff;
}

.grant-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: .35rem;
}

.grant-actions {
  display: inline-flex;
  gap: .35rem;
}

.grant-list {
  display: grid;
  gap: .35rem;
}

.grant-row {
  display: grid;
  grid-template-columns: auto auto 1fr;
  gap: .5rem;
  align-items: center;
}

.grant-name {
  word-break: break-word;
}

.lbl {
  font-size: .8rem;
  font-weight: 600;
  opacity: .85;
}

.btn {
  padding: .25rem .6rem;
  border-radius: .5rem;
  border: 1px solid #d0d5dd;
  background: #fff;
  cursor: pointer;
}

.btn.ghost {
  background: #fff;
}

.btn:disabled {
  opacity: .6;
  cursor: default;
}

.pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: .1rem .45rem;
  border-radius: .5rem;
  font-size: .8rem;
}

.pill.blue {
  background: #dbeafe;
  color: #1e3a8a;
}

.pill.red {
  background: #fee2e2;
  color: #b91c1c;
}

.muted {
  opacity: .7;
  font-size: .85rem;
}
</style>
