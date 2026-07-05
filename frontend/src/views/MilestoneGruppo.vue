<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {applyMilestoneGruppo, getMilestoneGruppo, MilestonePersonaggio, saghePerLivello} from '../service/PartyService'

const route = useRoute()
const router = useRouter()
const gruppoId = Number(route.params.gruppoId)

const personaggi = ref<MilestonePersonaggio[]>([])
const selezionati = ref<Set<number>>(new Set())
const quantita = ref(1)
const loading = ref(true)
const errorMsg = ref<string | null>(null)
const applicando = ref(false)
const fatto = ref(false)

async function carica() {
  loading.value = true
  try {
    personaggi.value = (await getMilestoneGruppo(gruppoId)).data
    selezionati.value = new Set(personaggi.value.map(p => p.id)) // tutti selezionati di default
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 403
        ? 'Non fai parte di questo party'
        : 'Errore nel caricamento'
  } finally {
    loading.value = false
  }
}

onMounted(carica)

function toggle(id: number) {
  if (selezionati.value.has(id)) selezionati.value.delete(id)
  else selezionati.value.add(id)
}

// simula l'aggiunta di `q` milestone: quanti livelli sale e lo stato finale
function anteprima(p: MilestonePersonaggio, q: number) {
  let milestone = p.milestone + q
  let livello = p.livello
  let up = 0
  while (milestone >= saghePerLivello(livello)) {
    milestone -= saghePerLivello(livello)
    livello += 1
    up += 1
  }
  return {
    totale: p.milestone + q,
    saghe: p.saghe,
    up,
    restLivello: livello,
    restMilestone: milestone,
    restSaghe: saghePerLivello(livello),
  }
}

// PG (senza tipo) in cima, poi stella/altri; a parità per nome
const personaggiOrdinati = computed(() =>
    [...personaggi.value].sort((a, b) => {
      const ra = a.tipoPersonaggio ? 1 : 0
      const rb = b.tipoPersonaggio ? 1 : 0
      return ra - rb || a.nome.localeCompare(b.nome)
    }))

const selezionatiList = computed(() => personaggi.value.filter(p => selezionati.value.has(p.id)))

async function applica() {
  if (applicando.value || !selezionatiList.value.length) return
  applicando.value = true
  try {
    personaggi.value = (await applyMilestoneGruppo(gruppoId, [...selezionati.value], Math.max(0, quantita.value || 0))).data
    fatto.value = true
  } catch (e) {
    console.error('Errore applicazione milestone:', e)
    errorMsg.value = 'Errore nell\'applicazione'
  } finally {
    applicando.value = false
  }
}
</script>

<template>
  <div class="milestone-page">
    <header class="head">
      <button class="btn ghost" @click="router.back()">←</button>
      <h1>Livella gruppo</h1>
    </header>

    <div v-if="loading" class="state">Caricamento…</div>
    <div v-else-if="errorMsg" class="state error">{{ errorMsg }}</div>

    <template v-else>
      <div v-if="fatto" class="done">
        ✓ Milestone applicate.
        <button class="btn" @click="router.back()">Torna al party</button>
      </div>

      <template v-else>
        <div v-if="!personaggi.length" class="state">Nessun personaggio livellabile in questo gruppo.</div>

        <template v-else>
          <!-- quantità -->
          <div class="qta-row">
            <span class="lbl">Milestone da aggiungere</span>
            <div class="stepper">
              <button class="btn" :disabled="quantita <= 0" @click="quantita = Math.max(0, quantita - 1)">−</button>
              <input v-model.number="quantita" type="number" min="0" inputmode="numeric"/>
              <button class="btn" @click="quantita = (quantita || 0) + 1">+</button>
            </div>
          </div>

          <!-- selezione + precalcolo -->
          <ul class="lista">
            <li v-for="p in personaggiOrdinati" :key="p.id" class="riga" :class="{off: !selezionati.has(p.id)}">
              <label class="sel">
                <input type="checkbox" :checked="selezionati.has(p.id)" @change="toggle(p.id)"/>
                <span class="nome">{{ p.nome }}</span>
                <span class="tag">lv {{ p.livello }} · {{ p.milestone }}/{{ p.saghe }}</span>
              </label>
              <div v-if="selezionati.has(p.id)" class="calc">
                <span class="add">+{{ quantita }} Saga</span>
                <span class="arrow">→</span>
                <span class="prog">{{ anteprima(p, quantita).totale }}/{{ anteprima(p, quantita).saghe }}</span>
                <template v-if="anteprima(p, quantita).up > 0">
                  <span class="arrow">→</span>
                  <span class="up">+{{ anteprima(p, quantita).up }} Livello</span>
                  <span class="fin">(lv {{ anteprima(p, quantita).restLivello }}, {{ anteprima(p, quantita).restMilestone }}/{{ anteprima(p, quantita).restSaghe }})</span>
                </template>
              </div>
            </li>
          </ul>

          <button class="btn primary applica" :disabled="applicando || !selezionatiList.length" @click="applica">
            {{ applicando ? 'Applico…' : `Applica a ${selezionatiList.length} personagg${selezionatiList.length === 1 ? 'io' : 'i'}` }}
          </button>
        </template>
      </template>
    </template>
  </div>
</template>

<style scoped>
.milestone-page {
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

.qta-row { display: flex; align-items: center; justify-content: space-between; gap: .5rem; }
.qta-row .lbl { font-weight: 600; }
.stepper { display: inline-flex; align-items: center; gap: .3rem; }
.stepper input { width: 4rem; text-align: center; padding: .4rem; border: 1px solid #d0d5dd; border-radius: .5rem; }

.lista { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: .35rem; }
.riga {
  border: 1px solid #e5e7eb; border-radius: .6rem; background: #fff; padding: .55rem .7rem;
  display: grid; gap: .3rem;
}
.riga.off { opacity: .5; }
.sel { display: flex; align-items: center; gap: .5rem; cursor: pointer; }
.sel input { flex-shrink: 0; }
.sel .nome { font-weight: 600; flex: 1; min-width: 0; overflow-wrap: anywhere; }
.tag { font-size: .75rem; color: #6b7280; background: #f3f4f6; border-radius: .4rem; padding: .1rem .4rem; white-space: nowrap; flex-shrink: 0; }

.calc {
  display: flex; flex-wrap: wrap; align-items: center; gap: .35rem;
  font-size: .82rem; padding-left: 1.6rem;
}
.calc .add { font-weight: 700; color: #1d4ed8; }
.calc .arrow { color: #9ca3af; }
.calc .prog { font-variant-numeric: tabular-nums; }
.calc .up { font-weight: 700; color: #166534; background: #dcfce7; border-radius: .4rem; padding: .05rem .4rem; }
.calc .fin { color: #6b7280; }

.btn {
  padding: .45rem .8rem; border-radius: .5rem; border: 1px solid #d0d5dd; background: #fff; cursor: pointer;
}
.btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.btn.ghost { background: #fff; }
.btn:disabled { opacity: .6; cursor: default; }
.applica { justify-self: start; }

.done {
  display: flex; align-items: center; gap: .75rem;
  padding: .75rem; border: 1px solid #bbf7d0; background: #f0fdf4; color: #166534; border-radius: .6rem;
}

.state { padding: .75rem; border: 1px dashed #e5e7eb; border-radius: .5rem; }
.state.error { color: #991b1b; background: #fef2f2; border-color: #fecaca; }
</style>
