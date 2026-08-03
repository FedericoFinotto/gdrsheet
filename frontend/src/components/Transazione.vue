<script setup lang="ts">
import {reactive, ref} from 'vue'
import {Soldi} from '../models/dto/Party'

const props = defineProps<{ disabled?: boolean }>()
const emit = defineEmits<{ (e: 'applica', delta: Soldi): void }>()

const open = ref(false)
const importo = reactive<Soldi>({mr: 0, ma: 0, mo: 0, mp: 0})

const MONETE: Array<{ key: keyof Soldi; sigla: string; cls: string }> = [
  {key: 'mp', sigla: 'MP', cls: 'mp'},
  {key: 'mo', sigla: 'MO', cls: 'mo'},
  {key: 'ma', sigla: 'MA', cls: 'ma'},
  {key: 'mr', sigla: 'MR', cls: 'mr'},
]

function norm(v: unknown): number {
  return Math.max(0, Math.floor(Number(v) || 0))
}

function vuota(): boolean {
  return MONETE.every(m => norm(importo[m.key]) === 0)
}

function applica(segno: 1 | -1) {
  if (vuota()) return
  emit('applica', {
    mr: segno * norm(importo.mr),
    ma: segno * norm(importo.ma),
    mo: segno * norm(importo.mo),
    mp: segno * norm(importo.mp),
  })
  importo.mr = 0
  importo.ma = 0
  importo.mo = 0
  importo.mp = 0
  open.value = false
}
</script>

<template>
  <div class="transazione">
    <button type="button" class="btn-trans" :disabled="disabled" @click.prevent="open = !open">
      ⇄ Transazione
    </button>

    <div v-if="open" class="trans-panel">
      <div class="trans-monete">
        <label v-for="m in MONETE" :key="m.key" class="trans-moneta" :class="m.cls">
          <span class="sigla">{{ m.sigla }}</span>
          <input
              type="number" min="0" step="1" inputmode="numeric"
              v-model.number="importo[m.key]"
              :disabled="disabled"
          />
        </label>
      </div>
      <div class="trans-actions">
        <button type="button" class="btn rimuovi" :disabled="disabled || vuota()" @click.prevent="applica(-1)">
          − Sottrai
        </button>
        <button type="button" class="btn aggiungi" :disabled="disabled || vuota()" @click.prevent="applica(1)">
          + Aggiungi
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.transazione { display: grid; gap: .4rem; }

.btn-trans {
  justify-self: start;
  padding: .3rem .7rem;
  border: 1px dashed var(--hairline);
  border-radius: .5rem;
  background: var(--surface-0);
  color: var(--text-strong);
  font-weight: 600;
  font-size: .8rem;
  cursor: pointer;
}
.btn-trans:hover { background: var(--btn-bg-hover); }
.btn-trans:disabled { opacity: .5; cursor: default; }

.trans-panel {
  border: 1px solid var(--hairline);
  border-radius: .5rem;
  background: var(--btn-bg);
  padding: .5rem;
  display: grid;
  gap: .4rem;
}

.trans-monete {
  display: grid;
  grid-template-columns: 1fr;
  gap: .35rem;
}

.trans-moneta {
  display: grid;
  grid-template-columns: 2.2rem 1fr;
  align-items: center;
  gap: .5rem;
  padding: .25rem .35rem;
  border-radius: .4rem;
  border: 1px solid var(--hairline);
  background: var(--surface-0);
}

.trans-moneta .sigla { font-size: .65rem; font-weight: 700; opacity: .8; }

.trans-moneta input {
  width: 100%;
  min-width: 0;
  text-align: center;
  padding: .25rem .15rem;
  border: 1px solid var(--hairline);
  border-radius: .35rem;
  font-variant-numeric: tabular-nums;
}

.trans-moneta.mp { background: var(--coin-mp-bg); border-color: var(--coin-mp-border); color: var(--coin-mp-text); }
.trans-moneta.mo { background: var(--coin-mo-bg); border-color: var(--coin-mo-border); color: var(--coin-mo-text); }
.trans-moneta.ma { background: var(--coin-ma-bg); border-color: var(--coin-ma-border); color: var(--coin-ma-text); }
.trans-moneta.mr { background: var(--coin-mr-bg); border-color: var(--coin-mr-border); color: var(--coin-mr-text); }

.trans-actions {
  display: flex;
  justify-content: flex-end;
  gap: .4rem;
}

.btn {
  padding: .35rem .7rem;
  border-radius: .5rem;
  border: 1px solid transparent;
  cursor: pointer;
  font-weight: 600;
  font-size: .8rem;
}
.btn.aggiungi { background: var(--success-bg); color: var(--success-text); border-color: var(--success-border); }
.btn.rimuovi { background: var(--danger-bg); color: var(--danger-text); border-color: var(--danger-border); }
.btn:disabled { opacity: .5; cursor: default; }
</style>
