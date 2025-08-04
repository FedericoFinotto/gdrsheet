<script setup lang="ts">
import {defineProps, ref} from 'vue';
import {PRIMARY_COLOR, SECONDARY_COLOR, TERTIARY_COLOR} from "../function/Constants";

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

const ROW_COLOR_1 = SECONDARY_COLOR;
const ROW_COLOR_2 = TERTIARY_COLOR;
const EXPANDED_COLOR = PRIMARY_COLOR;

const expandedId = ref<string | number | null>(null);

function toggleRow(id: string | number) {
  expandedId.value = expandedId.value === id ? null : id;
}

function isExpanded(id: string | number) {
  return expandedId.value === id;
}
</script>

<template>
  <table class="custom-table">
    <thead>
    <tr>
      <th v-for="col in props.columns" :key="col.field">
        {{ col.label }}
      </th>
    </tr>
    </thead>
    <tbody>
    <template v-for="(row, rowIndex) in props.items" :key="row.id">
      <tr
          @click="toggleRow(row.id)"
          :style="{ backgroundColor: isExpanded(row.id) ? EXPANDED_COLOR : (rowIndex % 2 === 0 ? ROW_COLOR_1 : ROW_COLOR_2) }"
      >
        <td v-for="col in props.columns" :key="col.field">
          <div class="cell-content">
            <div class="primary">{{ row[col.field] }}</div>
            <div v-if="col.subfield" class="subtext">{{ row[col.subfield] }}</div>
          </div>
        </td>
      </tr>
      <tr
          v-if="props.expandable && isExpanded(row.id)"
          :style="{ backgroundColor: EXPANDED_COLOR }"
      >
        <td :colspan="props.columns.length">
          <component
              v-if="row.expandedComponent"
              :is="row.expandedComponent"
              v-bind="row.expandedProps"
          />
          <slot v-else name="expansion" :data="row"/>
        </td>
      </tr>
    </template>
    </tbody>
  </table>
</template>

<style scoped>
.custom-table {
  width: 100%;
  border-collapse: collapse;
}

.custom-table th,
.custom-table td {
  padding: 0.5rem;
  border-bottom: 1px solid var(--border-color);
  text-align: left;
}

.custom-table tbody tr {
  cursor: pointer;
}
.cell-content {
  display: flex;
  flex-direction: column;
}
.primary {
  font-size: 1rem;
}
.subtext {
  font-size: 0.75rem;
  color: #666;
}
</style>
