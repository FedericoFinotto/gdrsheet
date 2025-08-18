<script setup lang="ts">
import {defineProps, ref} from 'vue'

type Fn<T = any> = (row: any) => T;

interface CounterDef {
  value: Fn<number>;
  max?: Fn<number | null | undefined>;
  onSub?: Fn<void>;
  onAdd?: Fn<void>;
  disableSub?: Fn<boolean>;
  disableAdd?: Fn<boolean>;
  hide?: Fn<boolean>;
}

interface ColumnDef {
  field: string;
  label: string;
  subfield?: string;
  disabled?: (row: any) => boolean;
  type?: 'text' | 'counter';
  counter?: CounterDef;
}

const props = defineProps<{
  items: any[];
  columns: ColumnDef[];
  expandable?: boolean;
}>()

const expandedId = ref<string | number | null>(null)

function toggleRow(id: string | number) {
  expandedId.value = expandedId.value === id ? null : id
}
function isExpanded(id: string | number) {
  return expandedId.value === id
}
function isRowDisabled(row: any): boolean {
  return props.columns.some(col => typeof col.disabled === 'function' && col.disabled(row))
}

/** Classi per la riga (nessuno style inline) */
function rowClass(row: any, rowIndex: number) {
  return [
    'tbl-row',
    rowIndex % 2 === 0 ? 'is-even' : 'is-odd',
    {'is-expanded': isExpanded(row.id), 'is-disabled': isRowDisabled(row)}
  ]
}

/* ---- helpers counter ---- */
function counterHidden(col: ColumnDef, row: any): boolean {
  return !!col.counter?.hide?.(row)
}
function counterVal(col: ColumnDef, row: any): number {
  const v = col.counter?.value?.(row)
  return Number.isFinite(v) ? Number(v) : 0
}

function counterMax(col: ColumnDef, row: any) {
  return col.counter?.max?.(row)
}
function canSub(col: ColumnDef, row: any): boolean {
  if (!col.counter?.onSub) return false
  if (col.counter?.disableSub) return !col.counter.disableSub(row)
  return counterVal(col, row) > 0
}
function canAdd(col: ColumnDef, row: any): boolean {
  if (!col.counter?.onAdd) return false
  if (col.counter?.disableAdd) return !col.counter.disableAdd(row)
  const max = counterMax(col, row)
  return max == null || counterVal(col, row) < Number(max)
}
function onSub(col: ColumnDef, row: any) {
  col.counter?.onSub?.(row)
}
function onAdd(col: ColumnDef, row: any) {
  col.counter?.onAdd?.(row)
}
</script>

<template>
  <table class="custom-table">
    <thead>
    <tr class="tbl-head-row">
      <th v-for="col in props.columns"
          :key="col.field"
          :class="['tbl-th', { 'counter-cell': col.type === 'counter' }]">
        {{ col.label }}
      </th>
    </tr>
    </thead>

    <tbody>
    <template v-for="(row, rowIndex) in props.items" :key="row.id">
      <tr
          :class="rowClass(row, rowIndex)"
          @click="toggleRow(row.id)"
          :aria-expanded="isExpanded(row.id) ? 'true' : 'false'"
          :aria-disabled="isRowDisabled(row) ? 'true' : 'false'"
      >
        <td v-for="col in props.columns"
            :key="col.field"
            :class="['tbl-td', { 'counter-cell': col.type === 'counter' }]">

          <!-- counter -->
          <template v-if="col.type === 'counter' && col.counter">
            <div class="counter-cell-inner" @click.stop>
              <template v-if="!counterHidden(col, row)">
                <div class="counter-wrap">
                  <button class="counter-btn"
                          @click.stop="onSub(col, row)"
                          :disabled="!canSub(col, row)">−
                  </button>

                  <div class="counter-value">
                    <span class="val">{{ counterVal(col, row) }}</span>
                    <template v-if="counterMax(col, row) != null">
                      <span class="slash">/</span>
                      <span class="max">{{ counterMax(col, row) }}</span>
                    </template>
                  </div>

                  <button class="counter-btn"
                          @click.stop="onAdd(col, row)"
                          :disabled="!canAdd(col, row)">+
                  </button>
                </div>
              </template>
            </div>
          </template>

          <!-- testo -->
          <template v-else>
            <div class="cell-content">
              <div class="primary">{{ row[col.field] }}</div>
              <div v-if="col.subfield" class="subtext">{{ row[col.subfield] }}</div>
            </div>
          </template>
        </td>
      </tr>

      <tr v-if="props.expandable && isExpanded(row.id)"
          class="tbl-row expansion-row is-expanded">
        <td :colspan="props.columns.length" class="tbl-td expansion-cell">
          <component v-if="row.expandedComponent"
                     :is="row.expandedComponent"
                     v-bind="row.expandedProps"/>
          <slot v-else name="expansion" :data="row"/>
        </td>
      </tr>
    </template>
    </tbody>
  </table>
</template>
