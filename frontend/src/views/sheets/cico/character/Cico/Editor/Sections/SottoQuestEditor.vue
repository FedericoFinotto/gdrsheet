<script setup lang="ts">
import {reactive, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ChildRef} from '../../../../../../../models/dto/UpdateItemRequest'
import {createItem} from '../../../../../../../service/PersonaggioService'
import HtmlEditor from '../../../../../../../components/HtmlEditor.vue'
import SearchSelect from '../../../../../../../components/SearchSelect.vue'

// Sezione dedicata alle sotto-quest: slegata dal meccanismo generico di "item collegati"
// (niente ricerca/collegamento di item esistenti, niente navigazione su un'altra pagina).
// Aggiungere una sotto-quest significa scegliere direttamente nome, descrizione e
// visibilità: la QUEST figlia viene creata al volo e collegata.
const props = defineProps<{
  modelValue: ChildRef[]
  disabled?: boolean
  idPersonaggio?: number
  idParty?: number
}>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: ChildRef[]): void
}>()

const router = useRouter()
const route = useRoute()

// in edit mode idPersonaggio/idParty non sono passati come prop: recupero dai query param
// della rotta (presenti quando l'editor è aperto dal contesto di un personaggio o di un party).
function resolveIdPersonaggio(): number | undefined {
  if (props.idPersonaggio != null) return props.idPersonaggio
  const q = route.query.personaggio
  const n = Number(Array.isArray(q) ? q[0] : q)
  return Number.isFinite(n) && n > 0 ? n : undefined
}

function resolveIdParty(): number | undefined {
  if (props.idParty != null) return props.idParty
  const q = route.query.party
  const n = Number(Array.isArray(q) ? q[0] : q)
  return Number.isFinite(n) && n > 0 ? n : undefined
}

function editSottoQuest(c: ChildRef) {
  const idPg = resolveIdPersonaggio()
  const idParty = resolveIdParty()
  const params = new URLSearchParams()
  if (idPg) params.set('personaggio', String(idPg))
  else if (idParty) params.set('party', String(idParty))
  const q = params.toString() ? `?${params.toString()}` : ''
  router.push(`/itemeditor/${c.id}${q}`)
}

function remove(idx: number) {
  emit('update:modelValue', props.modelValue.filter((_, i) => i !== idx))
}

const VISIBILITA_OPTS = [
  {value: '', label: 'Tutti'},
  {value: 'OWNER', label: 'Proprietario'},
  {value: 'MASTER', label: 'Master'},
]

const showForm = ref(false)
const creating = ref(false)
const errorMsg = ref<string | null>(null)
const nuovo = reactive({nome: '', descrizione: '', visibilita: ''})

function openForm() {
  nuovo.nome = ''
  nuovo.descrizione = ''
  nuovo.visibilita = ''
  errorMsg.value = null
  showForm.value = true
}

function closeForm() {
  showForm.value = false
}

async function salvaNuova() {
  const nome = nuovo.nome.trim()
  if (!nome || creating.value) return
  creating.value = true
  errorMsg.value = null
  try {
    const res = await createItem({
      nome,
      descrizione: nuovo.descrizione,
      tipo: 'QUEST' as any,
      idPersonaggio: resolveIdPersonaggio(),
      idParty: resolveIdParty(),
      skipFromCompendio: true,
      labels: nuovo.visibilita ? [{label: 'VISIBILITA', valore: nuovo.visibilita}] : [],
    })
    const created = res.data
    if (created) {
      emit('update:modelValue', [...props.modelValue, {
        id: created.id, nome: created.nome, tipo: created.tipo as any,
        qty: null, formulaQty: null, scelta: null, condizione: null,
      }])
      showForm.value = false
    }
  } catch (e: any) {
    errorMsg.value = e?.message ?? 'Errore nella creazione della sotto-quest'
  } finally {
    creating.value = false
  }
}
</script>

<template>
  <div class="sottoquest-editor">
    <div v-if="!modelValue.length && !showForm" class="empty">Nessuna sotto-quest.</div>

    <div v-for="(c, i) in modelValue" :key="c.id" class="sottoquest-row">
      <span class="nome">{{ c.nome }}</span>
      <button type="button" class="btn-edit" @click="editSottoQuest(c)" title="Modifica">✎</button>
      <button type="button" class="btn-del" :disabled="disabled" @click="remove(i)" title="Rimuovi">✕</button>
    </div>

    <div v-if="showForm" class="new-form">
      <label class="field">
        <span class="lbl">Nome</span>
        <input type="text" v-model.trim="nuovo.nome" :disabled="creating" placeholder="Nome della sotto-quest" required/>
      </label>
      <label class="field">
        <span class="lbl">Descrizione</span>
        <HtmlEditor v-model="nuovo.descrizione" :rows="6" :disabled="creating"/>
      </label>
      <label class="field">
        <span class="lbl">Visibilità</span>
        <SearchSelect v-model="nuovo.visibilita" :options="VISIBILITA_OPTS" :disabled="creating" :sort="false"/>
      </label>
      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
      <div class="form-actions">
        <button type="button" class="btn ghost" :disabled="creating" @click="closeForm">Annulla</button>
        <button type="button" class="btn primary" :disabled="creating || !nuovo.nome.trim()" @click="salvaNuova">Salva sotto-quest</button>
      </div>
    </div>

    <button v-else type="button" class="btn-create" :disabled="disabled" @click="openForm">
      + Aggiungi sotto-quest
    </button>
  </div>
</template>

<style scoped>
.sottoquest-editor { display: grid; gap: .4rem; }
.empty { font-size: .85rem; opacity: .6; }

.sottoquest-row {
  display: grid; grid-template-columns: 1fr auto auto; gap: .4rem; align-items: center;
  padding: .35rem .5rem; border: 1px solid #e5e7eb; border-radius: .5rem; background: #fff;
}
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
