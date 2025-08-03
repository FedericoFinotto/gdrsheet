<script setup lang="ts">
import {defineProps, ref} from 'vue';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';

// Definizione delle props
interface ColumnDef {
  field: string;
  label: string;
  subfield?: string;
}

const props = defineProps<{
  items: any[];
  columns: ColumnDef[];
  expandable?: boolean;
}>();

// Stato delle righe espanse
const expandedRows = ref<any[]>([]);

function onRowToggle(event: { data: any; }) {
  const row = event.data;
  const idx = expandedRows.value.findIndex(r => r.id === row.id);
  if (idx >= 0) {
    expandedRows.value.splice(idx, 1);
  } else {
    expandedRows.value = [row];
  }
}

function onRowClick(event: { data: any; }) {
  onRowToggle(event);
}
</script>

<template>
  <DataTable
      :value="props.items"
      dataKey="id"
      tableStyle="width:100%"
      class="p-datatable-sm"
      :expandedRows="expandedRows"
      @rowToggle="onRowToggle"
      @rowClick="props.expandable ? onRowClick : undefined"
  >
    <!-- Colonna expander -->
    <Column
        v-if="props.expandable"
        expander
        style="width:3rem"
    />

    <!-- Colonne dinamiche -->
    <Column
        v-for="col in props.columns"
        :key="col.field"
        :field="col.field"
        :header="col.label"
    >
      <template #body="{ data }">
        <div class="cell-content">
          <!-- Valore principale -->
          <div class="primary">
            {{ data[col.field] }}
          </div>
          <!-- Subfield in piccolo se presente -->
          <div v-if="col.subfield" class="subtext">
            {{ data[col.subfield] }}
          </div>
        </div>
      </template>
    </Column>

    <!-- Slot di espansione -->
    <template v-if="props.expandable" #expansion="{ data }">
      <component
          v-if="data.expandedComponent"
          :is="data.expandedComponent"
          v-bind="data.expandedProps"
      />
      <slot v-else name="expansion" :data="data"/>
    </template>
  </DataTable>
</template>

<style scoped>
.cell-content {
  display: flex;
  flex-direction: column;
}

.primary {
  font-size: 1rem;
}

.subtext {
  font-size: 0.75rem;
}

</style>
