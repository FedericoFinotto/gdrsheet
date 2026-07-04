<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {getParty} from '../service/PartyService'
import {createGruppo, deleteGruppo, getGruppi, saveGruppo} from '../service/PartyService'
import {PartyDetail, PersonaggioSoldi} from '../models/dto/Party'

const route = useRoute()
const router = useRouter()

const partyId = Number(route.params.id)
const party = ref<PartyDetail | null>(null)
const loading = ref(true)
const errorMsg = ref<string | null>(null)

// stato locale editabile per gruppo: nome, membri selezionati, capogruppo
interface GruppoEdit {
  id: number
  nome: string
  membri: Set<number>
  capogruppoId: number | null
  saving: boolean
}
const gruppi = reactive<GruppoEdit[]>([])

// stato apertura accordion (per id gruppo)
const aperti = ref<Set<number>>(new Set())
function toggleAperto(id: number) {
  if (aperti.value.has(id)) aperti.value.delete(id)
  else aperti.value.add(id)
}

// personaggi assegnabili (tutti tranne le banche)
const assegnabili = computed<PersonaggioSoldi[]>(() =>
    (party.value?.personaggi ?? []).filter(p => p.tipoPersonaggio !== 'BANCA'))

function tipoLabel(p: PersonaggioSoldi): string {
  switch (p.tipoPersonaggio) {
    case 'NAVE': return 'Barca'
    case 'STELLA': return 'Stella'
    case 'BASE': return 'Base'
    case 'NPC': return 'NPC'
    case null: case undefined: case '': return 'PG'
    default: return String(p.tipoPersonaggio)
  }
}

async function carica() {
  loading.value = true
  try {
    party.value = (await getParty(partyId)).data
    const gs = (await getGruppi(partyId)).data
    gruppi.splice(0, gruppi.length, ...gs.map(g => ({
      id: g.id,
      nome: g.nome,
      membri: new Set(g.membriIds),
      capogruppoId: g.capogruppoId,
      saving: false,
    })))
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 403
        ? 'Solo il master del party può gestire i gruppi'
        : 'Errore nel caricamento'
  } finally {
    loading.value = false
  }
}

onMounted(carica)

// creazione gruppo
const nuovoNome = ref('')
const creando = ref(false)
async function creaGruppo() {
  const nome = nuovoNome.value.trim()
  if (!nome || creando.value) return
  creando.value = true
  try {
    const g = (await createGruppo(partyId, nome)).data
    gruppi.push({id: g.id, nome: g.nome, membri: new Set(), capogruppoId: null, saving: false})
    aperti.value.add(g.id) // il nuovo gruppo parte aperto, pronto per l'assegnazione
    nuovoNome.value = ''
  } catch (e) {
    console.error('Errore creazione gruppo:', e)
  } finally {
    creando.value = false
  }
}

function toggleMembro(g: GruppoEdit, id: number) {
  if (g.membri.has(id)) {
    g.membri.delete(id)
    if (g.capogruppoId === id) g.capogruppoId = null
  } else {
    g.membri.add(id)
  }
}

// in quale altro gruppo sta un personaggio (per avvisare che verrà spostato)
function altroGruppo(g: GruppoEdit, id: number): string | null {
  const other = gruppi.find(x => x.id !== g.id && x.membri.has(id))
  return other ? other.nome : null
}

async function salva(g: GruppoEdit) {
  if (g.saving) return
  g.saving = true
  try {
    await saveGruppo(g.id, g.nome.trim(), [...g.membri], g.capogruppoId)
    await carica()
  } catch (e) {
    console.error('Errore salvataggio gruppo:', e)
  } finally {
    g.saving = false
  }
}

async function elimina(g: GruppoEdit) {
  if (!window.confirm(`Eliminare il gruppo "${g.nome}"?`)) return
  try {
    await deleteGruppo(g.id)
    const idx = gruppi.findIndex(x => x.id === g.id)
    if (idx >= 0) gruppi.splice(idx, 1)
  } catch (e) {
    console.error('Errore eliminazione gruppo:', e)
  }
}
</script>

<template>
  <div class="gruppi-page">
    <header class="head">
      <button class="btn ghost" @click="router.back()">←</button>
      <h1>Gestisci gruppi</h1>
    </header>

    <div v-if="loading" class="state">Caricamento…</div>
    <div v-else-if="errorMsg" class="state error">{{ errorMsg }}</div>

    <template v-else>
      <!-- Crea gruppo -->
      <section class="crea">
        <input v-model="nuovoNome" type="text" placeholder="Nome nuovo gruppo…" @keyup.enter="creaGruppo"/>
        <button class="btn primary" :disabled="creando || !nuovoNome.trim()" @click="creaGruppo">
          {{ creando ? '…' : '+ Crea gruppo' }}
        </button>
      </section>

      <div v-if="!gruppi.length" class="state">Nessun gruppo. Creane uno per iniziare.</div>

      <!-- Gruppi (accordion) -->
      <section v-for="g in gruppi" :key="g.id" class="gruppo-card">
        <div class="gruppo-head" @click="toggleAperto(g.id)">
          <span class="chev" :class="{open: aperti.has(g.id)}">▸</span>
          <input v-model="g.nome" type="text" class="nome-input" placeholder="Nome gruppo" @click.stop/>
          <span class="conteggio">{{ g.membri.size }}</span>
          <button class="btn danger" @click.stop="elimina(g)">Elimina</button>
        </div>

        <template v-if="aperti.has(g.id)">
          <ul class="membri">
            <li v-for="p in assegnabili" :key="p.id" class="membro-riga"
                :class="{'membro-riga--bloccato': !g.membri.has(p.id) && altroGruppo(g, p.id)}">
              <label class="check">
                <input type="checkbox" :checked="g.membri.has(p.id)"
                       :disabled="!g.membri.has(p.id) && !!altroGruppo(g, p.id)"
                       @change="toggleMembro(g, p.id)"/>
                <span class="nome">{{ p.nome }}</span>
                <span class="tipo-tag">{{ tipoLabel(p) }}</span>
              </label>
              <span v-if="g.membri.has(p.id)" class="capo">
                <label class="check-capo" title="Capogruppo">
                  <input type="radio" :name="`capo-${g.id}`" :checked="g.capogruppoId === p.id"
                         @change="g.capogruppoId = p.id"/>
                  Capo
                </label>
              </span>
              <span v-else-if="altroGruppo(g, p.id)" class="in-altro">in «{{ altroGruppo(g, p.id) }}»</span>
            </li>
          </ul>

          <div class="gruppo-actions">
            <button class="btn primary" :disabled="g.saving || !g.nome.trim()" @click="salva(g)">
              {{ g.saving ? 'Salvataggio…' : 'Salva gruppo' }}
            </button>
          </div>
        </template>
      </section>
    </template>
  </div>
</template>

<style scoped>
.gruppi-page {
  width: 100%;
  max-width: 44rem;
  margin: 0 auto;
  padding: 1rem;
  display: grid;
  gap: 1rem;
  align-content: start;
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}
.head { display: flex; align-items: center; gap: .5rem; }
.head h1 { margin: 0; font-size: 1.25rem; }

.crea { display: flex; gap: .5rem; }
.crea input { flex: 1; padding: .45rem .6rem; border: 1px solid #d0d5dd; border-radius: .5rem; }

.gruppo-card {
  display: grid;
  gap: .6rem;
  padding: .75rem;
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  background: #fff;
}
.gruppo-head { display: flex; gap: .5rem; align-items: center; cursor: pointer; }
.gruppo-head .chev { font-size: .8rem; opacity: .6; transition: transform .15s; flex-shrink: 0; }
.gruppo-head .chev.open { transform: rotate(90deg); }
.gruppo-head .conteggio {
  font-size: .75rem; font-weight: 700; color: #6b7280;
  background: #f3f4f6; border: 1px solid #e5e7eb; border-radius: 999px;
  min-width: 1.4rem; text-align: center; padding: 0 .35rem; flex-shrink: 0;
}
.nome-input { flex: 1; padding: .45rem .6rem; border: 1px solid #d0d5dd; border-radius: .5rem; font-weight: 700; }

.membri { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; }
.membro-riga {
  display: flex; align-items: flex-start; justify-content: space-between; gap: .5rem;
  padding: .55rem .4rem; border-bottom: 1px solid #f1f3f5;
}
.membro-riga:last-child { border-bottom: 0; }
.membro-riga:hover { background: #f9fafb; }
.membro-riga--bloccato { opacity: .55; }
.membro-riga--bloccato .check { cursor: not-allowed; }
.membro-riga--bloccato .check input { cursor: not-allowed; }
.check { display: flex; align-items: flex-start; gap: .5rem; cursor: pointer; flex: 1; min-width: 0; line-height: 1.35; }
.check .nome { flex: 1; }
.check input[type="checkbox"] { flex-shrink: 0; }
.check .nome { font-weight: 600; min-width: 0; overflow-wrap: anywhere; word-break: break-word; }
.tipo-tag {
  font-size: .7rem; padding: .1rem .4rem; border-radius: .4rem;
  background: #eef2ff; color: #3730a3; flex-shrink: 0; white-space: nowrap;
}
.check-capo { display: inline-flex; align-items: center; gap: .3rem; font-size: .78rem; color: #92400e; cursor: pointer; flex-shrink: 0; white-space: nowrap; }
.in-altro { font-size: .72rem; color: #9ca3af; font-style: italic; flex-shrink: 0; white-space: nowrap; text-align: right; }

.gruppo-actions { display: flex; justify-content: flex-end; }

.btn {
  padding: .45rem .8rem; border-radius: .5rem; border: 1px solid #d0d5dd; background: #fff; cursor: pointer;
}
.btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.btn.danger { background: #fef2f2; color: #991b1b; border-color: #fecaca; }
.btn.ghost { background: #fff; }
.btn:disabled { opacity: .6; cursor: default; }

.state { padding: .75rem; border: 1px dashed #e5e7eb; border-radius: .5rem; }
.state.error { color: #991b1b; background: #fef2f2; border-color: #fecaca; }
</style>
