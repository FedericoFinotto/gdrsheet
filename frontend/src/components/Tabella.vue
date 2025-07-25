<script setup>
import { ref, defineProps, onMounted } from 'vue';
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

onMounted(() => {
});

const expandedRows = ref([]);

const toggleRow = (event) => {
  const row = event.data;
  const index = expandedRows.value.findIndex(r => r.id === row.id); // Confronta per `id`

  if (index >= 0) {
    expandedRows.value.splice(index, 1);
  } else {
    expandedRows.value = [row]; // Questa linea assicura che solo una riga sia espansa
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
      <template v-if="$slots[`col-${col.field}`]" #body="slotProps">
        <slot :name="`col-${col.field}`" :data="slotProps.data" />
      </template>
      <template v-else #body="slotProps">
        {{ slotProps.data[col.field] }}
      </template>
    </Column>

    <template v-if="expandable" #expansion="slotProps">
      <slot name="expansion" :data="slotProps.data" />
    </template>
  </DataTable>
</template>

<style scoped>
</style>