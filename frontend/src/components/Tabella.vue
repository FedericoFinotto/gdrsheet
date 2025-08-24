<script setup lang="ts">
import {ref} from 'vue'
import Icona from '@/components/icona/Icona.vue' // cambia il path se serve

type Fn<T = any> = (row: any) => T

/* --------- Tipi colonna --------- */

interface CounterDef {
  value: Fn<number>;
  max?: Fn<number | null | undefined>;
  onSub?: Fn<void>;
  onAdd?: Fn<void>;
  disableSub?: Fn<boolean>;
  disableAdd?: Fn<boolean>;
  hide?: Fn<boolean>;
}

interface RowIcon {
  name: string;            // nome per <Icona/>
  title?: string;
  class?: string;
  onClick?: Fn<void>;
  disabled?: Fn<boolean>;
  show?: Fn<boolean>;
}

type RowIconInput = string | RowIcon;

interface ColumnDef {
  field: string;
  label: string;
  subfield?: string;
  disabled?: (row: any) => boolean;

  type?: 'text' | 'counter' | 'icons';

  // counter
  counter?: CounterDef;

  // icons
  list?: Fn<RowIconInput[]>;
}

/* --------- Props --------- */
const props = defineProps<{
  items: any[];
  columns: ColumnDef[];
  expandable?: boolean;
}>()

/* --------- Espansione --------- */
const expandedId = ref<string | number | null>(null)
function toggleRow(id: string | number) {
  expandedId.value = expandedId.value === id ? null : id
}
function isExpanded(id: string | number) {
  return expandedId.value === id
}

/* --------- Stato riga --------- */
function isRowDisabled(row: any): boolean {
  return props.columns.some(col => typeof col.disabled === 'function' && col.disabled(row))
}
function rowClass(row: any, rowIndex: number) {
  return [
    'tbl-row',
    rowIndex % 2 === 0 ? 'is-even' : 'is-odd',
    {'is-expanded': isExpanded(row.id), 'is-disabled': isRowDisabled(row)}
  ]
}

/* --------- Helper counter --------- */
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

/* --------- Helper icone --------- */
function normIcon(ic: RowIconInput | undefined, row: any): RowIcon | null {
  if (!ic) return null
  if (typeof ic === 'string') return {name: ic}
  if (ic.show && !ic.show(row)) return null
  return ic
}

function iconsFor(col: ColumnDef, row: any): RowIcon[] {
  const arr = col.list ? col.list(row) : []
  return (arr || []).map(i => normIcon(i, row)).filter(Boolean) as RowIcon[]
}

function iconDisabled(ic: RowIcon, row: any) {
  return ic.disabled ? !!ic.disabled(row) : false
}

function clickIcon(ic: RowIcon, row: any) {
  if (!iconDisabled(ic, row)) ic.onClick?.(row)
}
</script>

<template>
  <table class="custom-table">
    <thead>
    <tr class="tbl-head-row">
      <th v-for="col in props.columns"
          :key="col.field"
          :class="['tbl-th', { 'counter-cell': col.type === 'counter', 'icons-cell': col.type === 'icons' }]">
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
            :class="['tbl-td', { 'counter-cell': col.type === 'counter', 'icons-cell': col.type === 'icons' }]">

          <!-- Colonna ICONS -->
          <template v-if="col.type === 'icons'">
            <div class="icons-wrap" @click.stop>
              <button
                  v-for="(ic, i) in iconsFor(col, row)"
                  :key="i"
                  class="row-icon-btn"
                  type="button"
                  :title="ic.title || ic.name"
                  :disabled="iconDisabled(ic, row)"
                  @click.stop="clickIcon(ic, row)"
              >
                <Icona :name="ic.name" :class="['row-icon', ic.class]" :title="ic.title || ic.name"/>
              </button>
            </div>
          </template>

          <!-- Colonna COUNTER -->
          <template v-else-if="col.type === 'counter' && col.counter">
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

          <!-- Colonna TESTO -->
          <template v-else>
            <div class="cell-content">
              <div class="primary">{{ row[col.field] }}</div>
              <div v-if="col.subfield" class="subtext">{{ row[col.subfield] }}</div>
            </div>
          </template>
        </td>
      </tr>

      <!-- Riga di espansione -->
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

<style scoped>
.slash {
  opacity: .6;
}

/* allinea a destra tutta la colonna icone */
.tbl-td.icons-cell,
.tbl-th.icons-cell {
  text-align: right;
}

/* contenitore icone: compatto e a destra */
.icons-wrap {
  display: inline-flex;
  justify-content: flex-end;
  gap: 0;
  align-items: center;
  width: 100%;
}

/* bottone “trasparente” che contiene l’icona */
.row-icon-btn {
  background: transparent;
  border: 0;
  padding: 0;
  margin: 0;
  cursor: pointer;
}

.row-icon-btn[disabled] {
  opacity: .5;
  cursor: default;
}

/* -> dimensione icone PIÙ PICCOLA e allineamento pulito */
.row-icon {
  font-size: 0.7rem;
  vertical-align: middle;
}

/* se l’icona è una <img> (SVG da /public), scala come il font */
img.row-icon {
  object-fit: contain;
  display: inline-block;
}

.subtext {
  font-size: 0.7rem;
  color: gray;
}

</style>
