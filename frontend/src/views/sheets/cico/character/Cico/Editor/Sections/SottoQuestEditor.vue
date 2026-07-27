<script setup lang="ts">
import {computed, reactive, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ChildRef} from '../../../../../../../models/dto/UpdateItemRequest'
import {createItem} from '../../../../../../../service/PersonaggioService'
import {QuestScelta, searchQuestRadici} from '../../../../../../../service/QuestService'
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
  showLink.value = false
  showForm.value = true
}

function closeForm() {
  showForm.value = false
}

// --- Collega una quest già esistente -------------------------------------------------------
// La ricerca torna solo quest "radice" (non già figlie di un'altra): oltre a essere l'insieme
// sensato da offrire, è anche la garanzia anti-ciclo — i discendenti di questa quest non sono
// radici, quindi non possono comparire tra i risultati.
const showLink = ref(false)
const ricerca = ref('')
const risultati = ref<QuestScelta[]>([])
const cercando = ref(false)

// id della quest che si sta modificando: da escludere dai risultati (non può essere figlia di sé)
const idCorrente = computed<number | undefined>(() => {
  const n = Number(route.params.id)
  return Number.isFinite(n) && n > 0 ? n : undefined
})

function openLink() {
  ricerca.value = ''
  risultati.value = []
  errorMsg.value = null
  showForm.value = false
  showLink.value = true
}

function closeLink() {
  showLink.value = false
}

let ricercaTimer: any = null
watch(ricerca, () => {
  if (ricercaTimer) clearTimeout(ricercaTimer)
  const q = ricerca.value.trim()
  if (q.length < 2) {
    risultati.value = []
    return
  }
  ricercaTimer = setTimeout(async () => {
    cercando.value = true
    try {
      risultati.value = (await searchQuestRadici(q, idCorrente.value)).data ?? []
    } catch (e) {
      console.error('Errore ricerca quest:', e)
      risultati.value = []
    } finally {
      cercando.value = false
    }
  }, 300)
})

const giaCollegate = computed(() => new Set(props.modelValue.map(c => c.id)))

function collega(scelta: QuestScelta) {
  if (giaCollegate.value.has(scelta.id)) return
  emit('update:modelValue', [...props.modelValue, {
    id: scelta.id, nome: scelta.nome, tipo: 'QUEST' as any,
    qty: null, formulaQty: null, scelta: null, condizione: null,
  }])
  showLink.value = false
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
    <div v-if="!modelValue.length && !showForm && !showLink" class="empty">Nessuna sotto-quest.</div>

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

    <div v-if="showLink" class="new-form">
      <label class="field">
        <span class="lbl">Cerca una quest esistente</span>
        <input type="text" v-model="ricerca" placeholder="Almeno 2 caratteri…" autocomplete="off"/>
      </label>
      <p class="hint">Solo quest che non sono già sotto-quest di un'altra.</p>
      <div v-if="cercando" class="hint">Ricerca…</div>
      <ul v-else-if="risultati.length" class="risultati">
        <li v-for="r in risultati" :key="r.id">
          <button type="button" class="risultato" :disabled="giaCollegate.has(r.id)" @click="collega(r)">
            <span class="r-nome">{{ r.nome }}</span>
            <span v-if="r.ambito" class="r-ambito">{{ r.ambito }}</span>
            <span v-if="giaCollegate.has(r.id)" class="r-gia">già collegata</span>
          </button>
        </li>
      </ul>
      <div v-else-if="ricerca.trim().length >= 2" class="hint">Nessuna quest trovata.</div>
      <div class="form-actions">
        <button type="button" class="btn ghost" @click="closeLink">Chiudi</button>
      </div>
    </div>

    <div v-if="!showForm && !showLink" class="add-actions">
      <button type="button" class="btn-create" :disabled="disabled" @click="openForm">
        + Aggiungi sotto-quest
      </button>
      <button type="button" class="btn-create secondaria" :disabled="disabled" @click="openLink">
        ⇲ Collega quest esistente
      </button>
    </div>
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

.add-actions { display: flex; flex-wrap: wrap; gap: .4rem; margin-top: .15rem; }
.btn-create {
  border: 1px dashed #93c5fd; background: #eff6ff; color: #1d4ed8;
  border-radius: .5rem; padding: .4rem .7rem; font-weight: 600; cursor: pointer;
}
.btn-create:hover { background: #dbeafe; }
.btn-create.secondaria { border-color: #cbd5e1; background: #f8fafc; color: #475569; }
.btn-create.secondaria:hover { background: #f1f5f9; }

.hint { margin: 0; font-size: .78rem; color: #94a3b8; }

.risultati { list-style: none; margin: 0; padding: 0; display: grid; gap: .3rem; max-height: 16rem; overflow-y: auto; }
.risultato {
  width: 100%; display: flex; flex-wrap: wrap; align-items: baseline; gap: .4rem;
  text-align: left; cursor: pointer;
  border: 1px solid #e2e8f0; border-radius: .45rem; background: #fff; padding: .35rem .5rem;
}
.risultato:hover:not(:disabled) { background: #eff6ff; border-color: #bfdbfe; }
.risultato:disabled { opacity: .55; cursor: default; }
.r-nome { font-weight: 600; word-break: break-word; }
.r-ambito { font-size: .72rem; color: #64748b; }
.r-gia { font-size: .72rem; color: #b45309; }

.btn { padding: .5rem .9rem; border-radius: .5rem; border: 1px solid transparent; cursor: pointer; }
.btn.ghost { border-color: #d0d5dd; background: #fff; }
.btn.primary { background: #2563eb; color: white; }
button:disabled { opacity: .6; cursor: default; }
</style>
