<script setup lang="ts">
import {AttaccoRow, DannoRow} from '../../../../../../../models/dto/UpdateItemRequest'
import {VARIABILI_FORMULA} from '../../../../../../../function/labelCatalog'
import InfoHelpPopup from '../../../../../../../components/InfoHelpPopup.vue'
import usePopup from '../../../../../../../function/usePopup'

const props = defineProps<{
  modelValue: AttaccoRow[]
  disabled?: boolean
}>()
const emit = defineEmits<{ (e: 'update:modelValue', v: AttaccoRow[]): void }>()

const {openPopup} = usePopup()

function apriInfoVariabili() {
  openPopup(InfoHelpPopup, {
    titolo: 'Variabili utilizzabili',
    nota: 'Usa le variabili nelle formule di TPC, CD del tiro salvezza e danni, es.: BAB+FOR oppure 1d8+FOR.',
    voci: VARIABILI_FORMULA,
  }, {closable: true, autoClose: 0})
}

function update(idx: number, patch: Partial<AttaccoRow>) {
  const next = props.modelValue.map((r, i) => i === idx ? {...r, ...patch} : r)
  emit('update:modelValue', next)
}

function add() {
  emit('update:modelValue', [...props.modelValue, {
    nome: '', tipoRisoluzione: 'TPC', tpc: '', tiroSalvezza: '', tiroSalvezzaCd: '', danni: [],
  }])
}

function remove(idx: number) {
  emit('update:modelValue', props.modelValue.filter((_, i) => i !== idx))
}

function addDanno(idx: number) {
  const row = props.modelValue[idx]
  update(idx, {danni: [...(row.danni ?? []), {formula: '', tipo: ''}]})
}

function updateDanno(idx: number, dIdx: number, patch: Partial<DannoRow>) {
  const row = props.modelValue[idx]
  const danni = (row.danni ?? []).map((d, i) => i === dIdx ? {...d, ...patch} : d)
  update(idx, {danni})
}

function removeDanno(idx: number, dIdx: number) {
  const row = props.modelValue[idx]
  update(idx, {danni: (row.danni ?? []).filter((_, i) => i !== dIdx)})
}

const TIPI_RISOLUZIONE = [
  {value: 'TPC', label: 'Tiro Per Colpire'},
  {value: 'TS', label: 'Tiro Salvezza'},
]
const TIPI_TS = [
  {value: '', label: '— Seleziona —'},
  {value: 'Tempra', label: 'Tempra'},
  {value: 'Riflessi', label: 'Riflessi'},
  {value: 'Volontà', label: 'Volontà'},
]
</script>

<template>
  <div class="attacchi-editor">
    <div class="atk-toolbar">
      <button type="button" class="btn-info" title="Variabili utilizzabili nelle formule" @click="apriInfoVariabili">ⓘ Variabili</button>
    </div>

    <div v-if="!modelValue.length" class="empty">Nessun attacco.</div>

    <div v-for="(row, i) in modelValue" :key="row.id ?? `new-${i}`" class="atk-card">
      <div class="atk-head">
        <input
            type="text"
            class="nome"
            :value="row.nome"
            :disabled="disabled"
            placeholder="Nome attacco"
            @input="update(i, {nome: ($event.target as HTMLInputElement).value})"
        />
        <span v-if="row.id" class="pill">#{{ row.id }}</span>
        <button type="button" class="btn-del" :disabled="disabled" @click="remove(i)" title="Rimuovi attacco">✕</button>
      </div>

      <div class="atk-fields">
        <label class="field">
          <span class="lbl">Risoluzione</span>
          <select :value="row.tipoRisoluzione || 'TPC'" :disabled="disabled"
                  @change="update(i, {tipoRisoluzione: ($event.target as HTMLSelectElement).value})">
            <option v-for="opt in TIPI_RISOLUZIONE" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </label>

        <template v-if="(row.tipoRisoluzione || 'TPC') === 'TPC'">
          <label class="field">
            <span class="lbl">Tiro per colpire (TPC)</span>
            <input type="text" :value="row.tpc" :disabled="disabled"
                   placeholder="Formula, es.: BAB+FOR"
                   @input="update(i, {tpc: ($event.target as HTMLInputElement).value})"/>
          </label>
        </template>
        <template v-else>
          <label class="field">
            <span class="lbl">Tiro salvezza</span>
            <select :value="row.tiroSalvezza || ''" :disabled="disabled"
                    @change="update(i, {tiroSalvezza: ($event.target as HTMLSelectElement).value})">
              <option v-for="opt in TIPI_TS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
          </label>
          <label class="field">
            <span class="lbl">Formula CD</span>
            <input type="text" :value="row.tiroSalvezzaCd" :disabled="disabled"
                   placeholder="Formula, es.: 10+BAB/2+FOR"
                   @input="update(i, {tiroSalvezzaCd: ($event.target as HTMLInputElement).value})"/>
          </label>
        </template>
      </div>

      <!-- Danni: N per attacco, ognuno con formula e tipo propri -->
      <div class="danni-wrap">
        <div class="danni-head">
          <span class="lbl">Danni</span>
          <button type="button" class="btn-add-danno" :disabled="disabled" @click="addDanno(i)">+ Aggiungi danno</button>
        </div>
        <div v-if="!(row.danni ?? []).length" class="empty small">Nessun danno.</div>
        <div v-for="(d, di) in (row.danni ?? [])" :key="di" class="danno-row">
          <input type="text" :value="d.formula" :disabled="disabled"
                 placeholder="Formula danno, es.: 1d8+FOR"
                 @input="updateDanno(i, di, {formula: ($event.target as HTMLInputElement).value})"/>
          <input type="text" :value="d.tipo" :disabled="disabled"
                 placeholder="Tipo, es.: Tagliente"
                 @input="updateDanno(i, di, {tipo: ($event.target as HTMLInputElement).value})"/>
          <button type="button" class="btn-del-danno" :disabled="disabled" @click="removeDanno(i, di)" title="Rimuovi danno">✕</button>
        </div>
      </div>
    </div>

    <button type="button" class="btn-add" :disabled="disabled" @click="add">+ Aggiungi attacco</button>
  </div>
</template>

<style scoped>
input, select, textarea { min-width: 0; }
.attacchi-editor { display: grid; gap: .5rem; }
.atk-toolbar { display: flex; justify-content: flex-end; }
.btn-info {
  border: 1px solid #c7d2fe; background: #eef2ff; color: #3730a3;
  border-radius: .5rem; padding: .3rem .6rem; cursor: pointer; font-size: .8rem;
}
.btn-info:hover { background: #e0e7ff; }
.empty { font-size: .85rem; opacity: .6; }
.empty.small { font-size: .8rem; }

.atk-card { border: 1px solid #e5e7eb; border-radius: .5rem; padding: .5rem; display: grid; gap: .5rem; }
.atk-head { display: grid; grid-template-columns: 1fr auto auto; gap: .4rem; align-items: center; }
.atk-fields { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: .4rem; }
@media (max-width: 900px) { .atk-fields { grid-template-columns: 1fr; } }

.field { display: grid; gap: .25rem; }
.lbl { font-size: .75rem; font-weight: 600; opacity: .85; }
input, select {
  width: 100%; padding: .45rem .55rem; border: 1px solid #d0d5dd; border-radius: .5rem; background: #fff;
  font: inherit;
}
.nome { font-weight: 600; }
.pill {
  font-size: .75rem; padding: .1rem .45rem; border-radius: .5rem;
  background: #eef2ff; color: #3730a3;
}
.btn-del {
  border: 1px solid #fecaca; background: #fef2f2; color: #991b1b;
  border-radius: .5rem; padding: .35rem .6rem; cursor: pointer;
}
.btn-add {
  justify-self: start; border: 1px dashed #d0d5dd; background: #fff;
  border-radius: .5rem; padding: .4rem .7rem; cursor: pointer;
}

.danni-wrap {
  display: grid; gap: .35rem; padding: .5rem; border: 1px dashed #d0d5dd; border-radius: .5rem;
  background: #f9fafb;
}
.danni-head { display: flex; align-items: center; justify-content: space-between; }
.btn-add-danno {
  border: 1px solid #bbf7d0; background: #f0fdf4; color: #166534;
  border-radius: .5rem; padding: .25rem .55rem; cursor: pointer; font-size: .78rem;
}
.danno-row { display: grid; grid-template-columns: 2fr 1fr auto; gap: .4rem; align-items: center; }
@media (max-width: 900px) { .danno-row { grid-template-columns: 1fr; } }
.btn-del-danno {
  border: 1px solid #fecaca; background: #fef2f2; color: #991b1b;
  border-radius: .5rem; padding: .3rem .5rem; cursor: pointer; font-size: .8rem;
}
button:disabled { opacity: .6; cursor: default; }
</style>
