<script setup>
import { ref, defineProps } from 'vue';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';

// Props
const props = defineProps({
  items: {
    type: Array,
    required: true
  },
  columns: {
    type: Array,
    required: true
  },
  expandable: {
    type: Boolean,
    default: false
  }
});

const expandedRows = ref([]);

const toggleRow = (event) => {
  const row = event.data;
  const index = expandedRows.value.findIndex(r => r.id === row.id);
  if (index >= 0) {
    expandedRows.value.splice(index, 1);
  } else {
    expandedRows.value = [row];
  }
};
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
    <Column
        v-if="expandable"
        expander
        style="width: 3rem;"
    />

    <Column
        v-for="col in columns"
        :key="col.field"
        :field="col.field"
        :header="col.label"
    >
      <template #body="slotProps">
        <slot
            v-if="$slots[`col-${col.field}`]"
            :name="`col-${col.field}`"
            :data="slotProps.data"
        />
        <template v-else>
          {{ slotProps.data[col.field] }}
        </template>
      </template>
    </Column>

    <template v-if="expandable" #expansion="slotProps">
      <component
          v-if="slotProps.data.expandedComponent"
          :is="slotProps.data.expandedComponent"
          v-bind="slotProps.data.expandedProps"
      />
      <slot v-else name="expansion" :data="slotProps.data" />
    </template>
  </DataTable>
</template>

<style scoped>
</style>