<script setup lang="ts">
import {ref, onMounted} from 'vue';
import {testoModificatore} from "../../../../../function/Utils";
import {getStats, updateStatValue} from "../../../../../service/PersonaggioService";
import {useCharacterStore} from "../../../../../stores/personaggio";
import {Stat} from "../../../../../models/entity/Stat";

const props = defineProps<{
  data: any;
  idPersonaggio?: number;
}>();

const characterStore = useCharacterStore()

const editing = ref(false)
const saving = ref(false)
const formValore = ref('')
const formFormula = ref('')
const formModStatId = ref('')
const stats = ref<Stat[]>([])

onMounted(async () => {
  try {
    stats.value = await getStats()
  } catch (e) {
    console.error('Errore caricamento stats:', e)
  }
})

function startEdit() {
  formValore.value = props.data.statValore ?? ''
  formFormula.value = props.data.statFormula ?? ''
  formModStatId.value = props.data.statModId ?? ''
  editing.value = true
}

async function saveEdit() {
  if (!props.idPersonaggio) return
  saving.value = true
  try {
    await updateStatValue({
      idPersonaggio: props.idPersonaggio,
      idStat: props.data.abilita.id,
      valore: formValore.value,
      formula: formFormula.value || null,
      modStatId: formModStatId.value || null,
    })
    await characterStore.fetchCharacter(props.idPersonaggio)
    editing.value = false
  } catch (e) {
    console.error('Errore salvataggio stat:', e)
  } finally {
    saving.value = false
  }
}

function modLabel(mod: any): {prefix: string; suffix: string} | string {
  if (mod.tipo === 'NEGA') return 'Nega'
  if (mod.tipo === 'SBLOCCA') return 'Sblocca'
  if (mod.item === 'Formula') return {prefix: `Formula [${mod.formula}]: `, suffix: String(mod.valore)}
  return testoModificatore(mod.valore) + (mod.nota ? ` — ${mod.nota}` : '')
}
</script>

<template>
  <div class="abilita-detail-card">

    <!-- Barra azioni -->
    <div class="action-bar">
      <button v-if="idPersonaggio && !editing" type="button" class="action-btn edit" @click="startEdit">
        ✎ Modifica
      </button>
    </div>

    <!-- Contenuto -->
    <template v-if="!editing">
      <p><strong>Grado {{ data.rank.valore }}: </strong> {{ testoModificatore(data.rank.modificatore) }}</p>
      <p v-if="data.base?.id"><strong>{{ data.base.label }}: </strong>{{ testoModificatore(data.base.modificatore) }}</p>

      <div v-if="data.abilita.modificatori?.length">
        <p v-for="(mod, index) in data.abilita.modificatori" :key="index">
          <template v-if="typeof modLabel(mod) === 'object'">
            <strong>{{ (modLabel(mod) as any).prefix }}</strong>{{ (modLabel(mod) as any).suffix }}
          </template>
          <template v-else>
            <strong>{{ (mod.item || 'Sconosciuto') + ': ' }}</strong>{{ modLabel(mod) }}
          </template>
        </p>
      </div>
    </template>

    <!-- Form modifica -->
    <div v-else class="edit-form">
      <label class="field">
        <span>Valore</span>
        <input type="text" v-model="formValore" placeholder="es. 0" />
      </label>
      <label class="field">
        <span>Formula</span>
        <input type="text" v-model="formFormula" placeholder="es. 10+@LVL+@INT (opzionale)" />
      </label>
      <label class="field">
        <span>Mod (caratteristica)</span>
        <select v-model="formModStatId">
          <option value="">— nessuna —</option>
          <option v-for="s in stats.filter(s => s.tipo === 'CAR')" :key="s.id" :value="s.id">{{ s.label }}</option>
        </select>
      </label>
      <div class="edit-actions">
        <button type="button" class="btn-save" :disabled="saving" @click="saveEdit">
          {{ saving ? 'Salvo…' : 'Salva' }}
        </button>
        <button type="button" class="btn-cancel" :disabled="saving" @click="editing = false">Annulla</button>
      </div>
    </div>

  </div>
</template>

<style scoped>
.action-bar {
  display: flex;
  flex-wrap: wrap;
  gap: .5rem;
  align-items: center;
  padding-bottom: .6rem;
  margin-bottom: .6rem;
  border-bottom: 1px solid #e5e7eb;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: .35rem;
  border-radius: .5rem;
  padding: .4rem .75rem;
  font-weight: 600;
  font-size: .85rem;
  line-height: 1;
  cursor: pointer;
  border: 1px solid transparent;
}

.action-btn.edit {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
  margin-left: auto;
}

.action-btn:hover { filter: brightness(.97); }

.edit-form {
  display: grid;
  gap: .45rem;
}

.field {
  display: grid;
  grid-template-columns: 8rem 1fr;
  align-items: center;
  gap: .4rem;
  font-size: .85rem;
}

.field input, .field select {
  padding: .35rem .5rem;
  border: 1px solid #d0d5dd;
  border-radius: .4rem;
  background: #fff;
  font-size: .85rem;
  width: 100%;
}

.edit-actions {
  display: flex;
  gap: .5rem;
  margin-top: .2rem;
}

.btn-save {
  border: 1px solid #6ee7b7;
  background: #ecfdf5;
  color: #065f46;
  border-radius: .5rem;
  padding: .35rem .8rem;
  cursor: pointer;
  font-size: .82rem;
}

.btn-save:hover { background: #d1fae5; }

.btn-cancel {
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  color: #374151;
  border-radius: .5rem;
  padding: .35rem .8rem;
  cursor: pointer;
  font-size: .82rem;
}

button:disabled { opacity: .6; cursor: default; }
</style>
