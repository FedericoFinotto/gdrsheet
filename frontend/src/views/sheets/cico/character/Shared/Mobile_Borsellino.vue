<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {getSoldi, updateSoldi} from '../../../../../service/PersonaggioService'
import {Soldi, totaleInMo} from '../../../../../models/dto/Party'

const props = defineProps<{ idPersonaggio: number }>()

const form = reactive<Soldi>({mr: 0, ma: 0, mo: 0, mp: 0})
const originale = ref<Soldi | null>(null)
const loading = ref(true)
const busy = ref(false)
const errorMsg = ref<string | null>(null)

const MONETE: Array<{ key: keyof Soldi; label: string; cls: string }> = [
  {key: 'mp', label: 'Platino (MP)', cls: 'mp'},
  {key: 'mo', label: 'Oro (MO)', cls: 'mo'},
  {key: 'ma', label: 'Argento (MA)', cls: 'ma'},
  {key: 'mr', label: 'Rame (MR)', cls: 'mr'},
]

const modificato = computed(() =>
    !!originale.value && MONETE.some(m => Number(form[m.key]) !== originale.value![m.key]))

const totMo = computed(() => {
  const n = totaleInMo({
    mr: Number(form.mr) || 0,
    ma: Number(form.ma) || 0,
    mo: Number(form.mo) || 0,
    mp: Number(form.mp) || 0,
  })
  return n.toLocaleString('it-IT', {maximumFractionDigits: 2})
})

async function load() {
  loading.value = true
  errorMsg.value = null
  try {
    const res = await getSoldi(props.idPersonaggio)
    Object.assign(form, res.data)
    originale.value = {...res.data}
  } catch (e) {
    errorMsg.value = 'Errore nel caricamento dei soldi'
    console.error('Errore caricamento soldi:', e)
  } finally {
    loading.value = false
  }
}

async function onSave() {
  if (busy.value) return
  busy.value = true
  errorMsg.value = null
  try {
    const payload: Soldi = {
      mr: Math.max(0, Math.floor(Number(form.mr) || 0)),
      ma: Math.max(0, Math.floor(Number(form.ma) || 0)),
      mo: Math.max(0, Math.floor(Number(form.mo) || 0)),
      mp: Math.max(0, Math.floor(Number(form.mp) || 0)),
    }
    const res = await updateSoldi(props.idPersonaggio, payload)
    Object.assign(form, res.data)
    originale.value = {...res.data}
  } catch (e) {
    errorMsg.value = 'Errore nel salvataggio'
    console.error('Errore salvataggio soldi:', e)
  } finally {
    busy.value = false
  }
}

function onCancel() {
  if (originale.value) Object.assign(form, originale.value)
}

onMounted(load)
</script>

<template>
  <section class="borsellino">
    <header class="bors-head">
      <span class="titolo">Borsellino</span>
      <span class="tot">≈ {{ totMo }} MO</span>
    </header>

    <div v-if="loading" class="state">Caricamento…</div>

    <template v-else>
      <div class="monete">
        <label v-for="m in MONETE" :key="m.key" class="moneta" :class="m.cls">
          <span class="lbl">{{ m.label }}</span>
          <input
              type="number"
              min="0"
              step="1"
              inputmode="numeric"
              v-model.number="form[m.key]"
              :disabled="busy"
          />
        </label>
      </div>

      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

      <div v-if="modificato" class="actions">
        <button type="button" class="btn ghost" :disabled="busy" @click="onCancel">Annulla</button>
        <button type="button" class="btn primary" :disabled="busy" @click="onSave">
          {{ busy ? 'Salvataggio…' : 'Salva' }}
        </button>
      </div>
    </template>
  </section>
</template>

<style scoped>
.borsellino {
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  background: #fff;
  padding: .6rem .75rem;
  display: grid;
  gap: .5rem;
}

.bors-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.titolo { font-weight: 700; }
.tot { font-size: .85rem; font-weight: 600; opacity: .75; }

.monete {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: .4rem;
}

@media (max-width: 480px) {
  .monete { grid-template-columns: repeat(2, 1fr); }
}

.moneta {
  display: grid;
  gap: .2rem;
  padding: .35rem .45rem;
  border-radius: .5rem;
  border: 1px solid #e5e7eb;
}

.moneta .lbl { font-size: .7rem; font-weight: 600; opacity: .8; }

.moneta input {
  width: 100%;
  padding: .35rem .45rem;
  border: 1px solid #d0d5dd;
  border-radius: .4rem;
  background: #fff;
  font-variant-numeric: tabular-nums;
}

.moneta.mp { background: #ecfeff; border-color: #a5f3fc; }
.moneta.mo { background: #fefce8; border-color: #fde68a; }
.moneta.ma { background: #f8fafc; border-color: #cbd5e1; }
.moneta.mr { background: #fff7ed; border-color: #fed7aa; }

.state { font-size: .85rem; opacity: .7; }

.error {
  margin: 0;
  padding: .4rem .6rem;
  border-radius: .5rem;
  color: #991b1b;
  background: #fef2f2;
  border: 1px solid #fecaca;
  font-size: .8rem;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: .5rem;
}

.btn {
  padding: .4rem .8rem;
  border-radius: .5rem;
  border: 1px solid transparent;
  cursor: pointer;
}
.btn.ghost { border-color: #d0d5dd; background: #fff; }
.btn.primary { background: #2563eb; color: #fff; }
.btn:disabled { opacity: .6; cursor: default; }
</style>
