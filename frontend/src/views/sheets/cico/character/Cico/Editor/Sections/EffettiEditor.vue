<script setup lang="ts">
import {reactive, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ChildRef} from '../../../../../../../models/dto/UpdateItemRequest'
import {createItem} from '../../../../../../../service/PersonaggioService'
import HtmlEditor from '../../../../../../../components/HtmlEditor.vue'

// Sezione dedicata agli Effetti: slegata dal meccanismo generico di "item collegati"
// (niente ricerca/collegamento di item esistenti). Aggiungere un effetto significa
// scegliere direttamente condizione, nome e descrizione: l'item EFFETTO sottostante
// viene creato al volo.
const props = defineProps<{
  modelValue: ChildRef[]
  disabled?: boolean
  idPersonaggio?: number
}>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: ChildRef[]): void
}>()

const router = useRouter()
const route = useRoute()

// in edit mode idPersonaggio non è passato come prop: recupero dal query param
// della rotta (presente quando l'editor è aperto dal contesto di un personaggio).
function resolveIdPersonaggio(): number | undefined {
  if (props.idPersonaggio != null) return props.idPersonaggio
  const q = route.query.personaggio
  const n = Number(Array.isArray(q) ? q[0] : q)
  return Number.isFinite(n) && n > 0 ? n : undefined
}

function editEffetto(c: ChildRef) {
  const idPg = route.query.personaggio
  const path = `/itemeditor/${c.id}` + (idPg ? `?personaggio=${idPg}` : '')
  router.push(path)
}

function setCondizione(idx: number, val: string) {
  const trimmed = val.trim() || null
  emit('update:modelValue', props.modelValue.map((c, i) => i === idx ? {...c, condizione: trimmed} : c))
}

function remove(idx: number) {
  emit('update:modelValue', props.modelValue.filter((_, i) => i !== idx))
}

const showForm = ref(false)
const creating = ref(false)
const errorMsg = ref<string | null>(null)
const nuovo = reactive({condizione: '', nome: '', descrizione: ''})

function openForm() {
  nuovo.condizione = ''
  nuovo.nome = ''
  nuovo.descrizione = ''
  errorMsg.value = null
  showForm.value = true
}

function closeForm() {
  showForm.value = false
}

async function salvaNuovo() {
  const nome = nuovo.nome.trim()
  if (!nome || creating.value) return
  creating.value = true
  errorMsg.value = null
  try {
    const res = await createItem({
      nome,
      descrizione: nuovo.descrizione,
      tipo: 'EFFETTO' as any,
      idPersonaggio: resolveIdPersonaggio(),
      skipFromCompendio: true,
    })
    const created = res.data
    if (created) {
      emit('update:modelValue', [...props.modelValue, {
        id: created.id, nome: created.nome, tipo: created.tipo as any,
        qty: null, formulaQty: null, scelta: null,
        condizione: nuovo.condizione.trim() || null,
      }])
      showForm.value = false
    }
  } catch (e: any) {
    errorMsg.value = e?.message ?? 'Errore nella creazione dell\'effetto'
  } finally {
    creating.value = false
  }
}
</script>

<template>
  <div class="effetti-editor">
    <div v-if="!modelValue.length && !showForm" class="empty">Nessun effetto.</div>

    <div v-for="(c, i) in modelValue" :key="c.id" class="effetto-row">
      <input
          class="condizione-input"
          type="text"
          :value="c.condizione ?? ''"
          :disabled="disabled"
          placeholder="Sempre"
          title="Condizione sotto cui l'effetto si applica (es. Indossato)"
          @input="setCondizione(i, ($event.target as HTMLInputElement).value)"
      />
      <span class="colon">:</span>
      <span class="nome">{{ c.nome }}</span>
      <button type="button" class="btn-edit" @click="editEffetto(c)" title="Modifica nome/descrizione">✎</button>
      <button type="button" class="btn-del" :disabled="disabled" @click="remove(i)" title="Rimuovi">✕</button>
    </div>

    <div v-if="showForm" class="new-form">
      <label class="field">
        <span class="lbl">Condizione</span>
        <input type="text" v-model.trim="nuovo.condizione" :disabled="creating" placeholder="es. Indossato (vuoto = sempre attivo)"/>
      </label>
      <label class="field">
        <span class="lbl">Nome</span>
        <input type="text" v-model.trim="nuovo.nome" :disabled="creating" placeholder="Nome dell'effetto" required/>
      </label>
      <label class="field">
        <span class="lbl">Descrizione</span>
        <HtmlEditor v-model="nuovo.descrizione" :rows="6" :disabled="creating"/>
      </label>
      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
      <div class="form-actions">
        <button type="button" class="btn ghost" :disabled="creating" @click="closeForm">Annulla</button>
        <button type="button" class="btn primary" :disabled="creating || !nuovo.nome.trim()" @click="salvaNuovo">Salva effetto</button>
      </div>
    </div>

    <button v-else type="button" class="btn-create" :disabled="disabled" @click="openForm">
      + Aggiungi effetto
    </button>
  </div>
</template>

<style scoped>
.effetti-editor { display: grid; gap: .4rem; }
.empty { font-size: .85rem; opacity: .6; }

.effetto-row {
  display: grid; grid-template-columns: auto auto 1fr auto auto; gap: .4rem; align-items: center;
  padding: .35rem .5rem; border: 1px solid #e5e7eb; border-radius: .5rem; background: #fff;
}
.condizione-input {
  width: 9rem; padding: .25rem .4rem; border: 1px solid #d0d5dd; border-radius: .4rem; font-size: .8rem;
}
.colon { opacity: .6; }
.nome { white-space: normal; word-break: break-word; }

.btn-edit {
  border: 1px solid #bfdbfe; background: #eff6ff; color: #1d4ed8;
  border-radius: .5rem; padding: .3rem .55rem; cursor: pointer;
}
.btn-edit:hover { background: #dbeafe; }

.btn-del {
  border: 1px solid #fecaca; background: #fef2f2; color: #991b1b;
  border-radius: .5rem; padding: .3rem .55rem; cursor: pointer;
}

.new-form {
  display: grid; gap: .5rem; padding: .6rem; border: 1px dashed #93c5fd; border-radius: .5rem; background: #f8fafc;
}
.field { display: grid; gap: .3rem; }
.lbl { font-size: .8rem; font-weight: 600; opacity: .85; }
.field input {
  width: 100%; padding: .45rem .55rem; border: 1px solid #d0d5dd; border-radius: .5rem; background: #fff;
}
.form-actions { display: flex; justify-content: flex-end; gap: .5rem; }
.error {
  margin: 0; padding: .4rem .6rem; border-radius: .4rem;
  color: #991b1b; background: #fef2f2; border: 1px solid #fecaca; font-size: .82rem;
}

.btn-create {
  justify-self: start; margin-top: .15rem;
  border: 1px dashed #93c5fd; background: #eff6ff; color: #1d4ed8;
  border-radius: .5rem; padding: .4rem .7rem; font-weight: 600; cursor: pointer;
}
.btn-create:hover { background: #dbeafe; }

.btn { padding: .5rem .9rem; border-radius: .5rem; border: 1px solid transparent; cursor: pointer; }
.btn.ghost { border-color: #d0d5dd; background: #fff; }
.btn.primary { background: #2563eb; color: white; }
button:disabled { opacity: .6; cursor: default; }
</style>
