<script setup lang="ts">
import {defineProps, ref} from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

const props = defineProps({
  items: {type: Array, required: true},
  columns: {type: Array, required: true},
  expandable: {type: Boolean, default: false}
})

// il tuo stato “manuale” delle righe espanse
const expandedRows = ref<any[]>([])

/**
 * Gestisce manualmente il toggle: apre/chiude solo la riga ricevuta
 */
function onRowToggle(event: { data: any; expandedRows: any[] }) {
  const row = event.data
  const idx = expandedRows.value.findIndex(r => r.id === row.id)

  if (idx >= 0) {
    // era aperta → chiudi
    expandedRows.value.splice(idx, 1)
  } else {
    // chiudi tutto e riapri solo questa
    expandedRows.value = [row]
  }
}

/**
 * Se vuoi che anche un click su qualunque cella apra/chiuda:
 */
function onRowClick(event: { data: any }) {
  onRowToggle({data: event.data, expandedRows: expandedRows.value})
}
</script>

<template>
  <DataTable
      :value="items"
      dataKey="id"
      tableStyle="width:100%"
      class="p-datatable-sm"
      :expandedRows="expandedRows"
      @rowToggle="onRowToggle"
      @rowClick="expandable ? onRowClick : undefined"
  >
    <!-- colonna expander -->
    <Column
        v-if="expandable"
        expander
        style="width:3rem"
    />

    <!-- colonne dinamiche invariati -->
    <Column
        v-for="col in columns"
        :key="col.field"
        :field="col.field"
        :header="col.label"
    >
      <template #body="{ data }">
        <slot
            v-if="$slots[`col-${col.field}`]"
            :name="`col-${col.field}`"
            :data="data"
        />
        <template v-else>
          {{ data[col.field] }}
        </template>
      </template>
    </Column>

    <!-- slot di espansione -->
    <template v-if="expandable" #expansion="{ data }">
      <component
          v-if="data.expandedComponent"
          :is="data.expandedComponent"
          v-bind="data.expandedProps"
      />
      <slot
          v-else
          name="expansion"
          :data="data"
      />
    </template>
  </DataTable>
</template>
