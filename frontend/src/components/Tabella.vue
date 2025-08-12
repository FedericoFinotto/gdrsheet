<script setup lang="ts">
import {defineProps, ref} from 'vue';
import {PRIMARY_COLOR, SECONDARY_COLOR, TERTIARY_COLOR} from "../function/Constants";

type Fn<T = any> = (row: any) => T;

interface CounterDef {
  /** Valore corrente mostrato al centro */
  value: Fn<number>;
  /** Max opzionale; se assente mostro solo il valore */
  max?: Fn<number | null | undefined>;
  /** Click su '-' */
  onSub?: Fn<void>;
  /** Click su '+' */
  onAdd?: Fn<void>;
  /** Disabilitatori opzionali */
  disableSub?: Fn<boolean>;
  disableAdd?: Fn<boolean>;
  /** Se true, nasconde interamente il contatore per quella riga */
  hide?: Fn<boolean>;
}

interface ColumnDef {
  field: string;
  label: string;
  subfield?: string;
  disabled?: (row: any) => boolean;
  /** 'counter' abilita la cella interattiva - val [/max] + */
  type?: 'text' | 'counter';
  counter?: CounterDef;
}

const props = defineProps<{
  items: any[];
  columns: ColumnDef[];
  expandable?: boolean;
}>();

const ROW_COLOR_1 = SECONDARY_COLOR;
const ROW_COLOR_2 = TERTIARY_COLOR;
const EXPANDED_COLOR = PRIMARY_COLOR;
const DISABLED_COLOR = 'rgba(255, 0, 0, 0.12)';

const expandedId = ref<string | number | null>(null);

function toggleRow(id: string | number) {
  expandedId.value = expandedId.value === id ? null : id;
}
function isExpanded(id: string | number) {
  return expandedId.value === id;
}
function isRowDisabled(row: any): boolean {
  return props.columns.some(col => typeof col.disabled === 'function' && col.disabled(row));
}
function rowBg(row: any, rowIndex: number) {
  if (isRowDisabled(row)) return DISABLED_COLOR;
  if (isExpanded(row.id)) return EXPANDED_COLOR;
  return rowIndex % 2 === 0 ? ROW_COLOR_1 : ROW_COLOR_2;
}

/* ---------- helpers per la colonna counter ---------- */
function counterHidden(col: ColumnDef, row: any): boolean {
  return !!col.counter?.hide?.(row);
}

function counterVal(col: ColumnDef, row: any): number {
  const v = col.counter?.value?.(row);
  return Number.isFinite(v) ? Number(v) : 0;
}

function counterMax(col: ColumnDef, row: any): number | null | undefined {
  return col.counter?.max?.(row);
}

function canSub(col: ColumnDef, row: any): boolean {
  if (!col.counter?.onSub) return false;
  if (col.counter?.disableSub) return !col.counter.disableSub(row);
  return counterVal(col, row) > 0;
}

function canAdd(col: ColumnDef, row: any): boolean {
  if (!col.counter?.onAdd) return false;
  if (col.counter?.disableAdd) return !col.counter.disableAdd(row);
  const max = counterMax(col, row);
  return max == null || counterVal(col, row) < Number(max);
}

function onSub(col: ColumnDef, row: any) {
  col.counter?.onSub?.(row);
}

function onAdd(col: ColumnDef, row: any) {
  col.counter?.onAdd?.(row);
}
</script>

<template>
  <table class="custom-table">
    <thead>
    <tr>
      <th v-for="col in props.columns" :key="col.field" :class="{'counter-cell': col.type === 'counter'}">
        {{ col.label }}
      </th>
    </tr>
    </thead>

    <tbody>
    <template v-for="(row, rowIndex) in props.items" :key="row.id">
      <tr
          @click="toggleRow(row.id)"
          :style="{ backgroundColor: rowBg(row, rowIndex) }"
          :class="{ 'disabled-row': isRowDisabled(row) }"
      >
        <td
            v-for="col in props.columns"
            :key="col.field"
            :class="{ 'counter-cell': col.type === 'counter' }"
        >
          <!-- cella 'counter' -->
          <template v-if="col.type === 'counter' && col.counter">
            <div class="counter-cell-inner" @click.stop>
              <template v-if="!counterHidden(col, row)">
                <div class="counter-wrap">
                  <button class="counter-btn" @click.stop="onSub(col, row)" :disabled="!canSub(col, row)">−</button>

                  <div class="counter-value">
                    <span class="val">{{ counterVal(col, row) }}</span>
                    <template v-if="counterMax(col, row) != null">
                      <span class="slash">/</span>
                      <span class="max">{{ counterMax(col, row) }}</span>
                    </template>
                  </div>

                  <button class="counter-btn" @click.stop="onAdd(col, row)" :disabled="!canAdd(col, row)">+</button>
                </div>
              </template>
              <!-- se hidden, non mostro nulla (spazio libero a destra) -->
            </div>
          </template>

          <!-- cella testo default -->
          <template v-else>
            <div class="cell-content">
              <div class="primary">{{ row[col.field] }}</div>
              <div v-if="col.subfield" class="subtext">{{ row[col.subfield] }}</div>
            </div>
          </template>
        </td>
      </tr>

      <tr v-if="props.expandable && isExpanded(row.id)" :style="{ backgroundColor: EXPANDED_COLOR }">
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
.custom-table { width: 100%; border-collapse: collapse; }
.custom-table th, .custom-table td {
  padding: 0.5rem;
  border-bottom: 1px solid var(--border-color);
  text-align: left;
}
.custom-table tbody tr { cursor: pointer; }

.cell-content { display: flex; flex-direction: column; }
.primary { font-size: 1rem; }
.subtext { font-size: 0.75rem; color: #666; }

/* --- allineamento a destra per le celle counter --- */
.counter-cell {
  text-align: right;
}

.counter-cell-inner {
  display: flex;
  justify-content: flex-end; /* porta il blocco counter tutto a destra */
  width: 100%;
}

/* counter */
.counter-wrap {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.counter-btn {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  border: 1px solid rgba(0, 0, 0, .2);
  background: #fafafa;
  line-height: 22px;
  text-align: center;
  cursor: pointer;
}

.counter-btn:disabled {
  opacity: .4;
  cursor: default;
}

.counter-value {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  font-variant-numeric: tabular-nums;
}

.counter-value .val {
  min-width: 16px;
  text-align: right;
}

.counter-value .slash {
  opacity: .6;
}

.counter-value .max {
  opacity: .85;
}

/* righe "disabled" */
.disabled-row { /* bg inline, altri stili se servono */
}
</style>
