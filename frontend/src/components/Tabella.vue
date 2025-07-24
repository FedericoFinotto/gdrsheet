<script setup>
import {onMounted, ref, toRefs} from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

// Props
const props = defineProps({
  items: {
    type: Array,
    required: true
  },
  columns: {
    type: Array,
    required: true // [{ field: 'nome', label: 'Nome' }, ...]
  },
  expandable: {
    type: Boolean,
    default: false
  }
})

onMounted(() => {
  console.log(props);
})

// Espansione righe
const expandedRows = ref([])
const toggleRow = (event) => {
  const row = event.data
  const index = expandedRows.value.findIndex(r => r === row)
  if (index >= 0) {
    expandedRows.value.splice(index, 1)
  } else {
    expandedRows.value = [row]
  }
}
</script>

<template>
  <DataTable
      :value="items"
      dataKey="id"
      tableStyle="width: 100%"
      class="p-datatable-sm"
      v-model:expandedRows="expandedRows"
      selectionMode="single"
      @rowClick="expandable ? toggleRow : undefined"
  >
    <!-- Colonne dinamiche -->
    <Column
        v-for="col in columns"
        :key="col.field"
        :field="col.field"
        :header="col.label"
    >
      <template v-if="$slots[`col-${col.field}`]" #body="slotProps">
        <slot :name="`col-${col.field}`" :data="slotProps.data" />
      </template>
      <template v-else #body="slotProps">
        {{ slotProps.data[col.field] }}
      </template>
    </Column>

    <!-- Slot espansione -->
    <template v-if="expandable" #expansion="slotProps">
      <slot name="expansion" :data="slotProps.data" />
    </template>
  </DataTable>
</template>
