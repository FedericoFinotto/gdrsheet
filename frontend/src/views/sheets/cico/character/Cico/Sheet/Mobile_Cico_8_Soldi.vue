<script setup lang="ts">
import {defineProps, onMounted, reactive, ref} from 'vue';
import Mobile_Borsellino from "../../Shared/Mobile_Borsellino.vue";
import {getContiPersonaggio} from "../../../../../../service/PersonaggioService";
import {apriConto, updateConto} from "../../../../../../service/PartyService";
import {Banca, Conto, Soldi, totaleInMo} from "../../../../../../models/dto/Party";
import Transazione from "../../../../../../components/Transazione.vue";

const props = defineProps({
  idPersonaggio: {
    type: Number,
    required: true
  }
});

const banche = ref<Banca[]>([]);
const loading = ref(true);
const errorMsg = ref<string | null>(null);

const drafts = reactive<Record<number, Soldi>>({});
const busySave = ref<number | null>(null);

const MONETE: Array<{ key: keyof Soldi; sigla: string; cls: string }> = [
  {key: 'mp', sigla: 'MP', cls: 'mp'},
  {key: 'mo', sigla: 'MO', cls: 'mo'},
  {key: 'ma', sigla: 'MA', cls: 'ma'},
  {key: 'mr', sigla: 'MR', cls: 'mr'},
];

onMounted(async () => {
  try {
    const res = await getContiPersonaggio(props.idPersonaggio);
    banche.value = res.data;
    for (const b of banche.value) {
      for (const c of b.conti) drafts[c.itemId] = {...c.soldi};
    }
  } catch (e) {
    errorMsg.value = 'Errore nel caricamento dei conti';
    console.error('Errore caricamento conti:', e);
  } finally {
    loading.value = false;
  }
});

function isDirty(c: Conto): boolean {
  const d = drafts[c.itemId];
  return !!d && MONETE.some(m => Number(d[m.key]) !== c.soldi[m.key]);
}

function step(c: Conto, key: keyof Soldi, delta: number) {
  const d = drafts[c.itemId];
  if (!d) return;
  d[key] = Math.max(0, (Number(d[key]) || 0) + delta);
}

function annulla(c: Conto) {
  drafts[c.itemId] = {...c.soldi};
}

function applicaTransazione(c: Conto, delta: Soldi) {
  const d = drafts[c.itemId];
  if (!d) return;
  for (const k of ['mr', 'ma', 'mo', 'mp'] as Array<keyof Soldi>) {
    d[k] = Math.max(0, (Number(d[k]) || 0) + delta[k]);
  }
}

function totaleDraft(c: Conto): string {
  const d = drafts[c.itemId] ?? c.soldi;
  return totaleInMo({
    mr: Number(d.mr) || 0, ma: Number(d.ma) || 0, mo: Number(d.mo) || 0, mp: Number(d.mp) || 0,
  }).toLocaleString('it-IT', {maximumFractionDigits: 2});
}

const busyApri = ref<number | null>(null);

async function onApriConto(b: Banca) {
  if (busyApri.value) return;
  busyApri.value = b.personaggioId;
  try {
    const res = await apriConto(b.personaggioId, `G${props.idPersonaggio}`);
    b.conti = [res.data];
    drafts[res.data.itemId] = {...res.data.soldi};
  } catch (e) {
    console.error('Errore apertura conto:', e);
    errorMsg.value = "Errore nell'apertura del conto";
  } finally {
    busyApri.value = null;
  }
}

async function salva(c: Conto) {
  const d = drafts[c.itemId];
  if (!d || busySave.value) return;
  busySave.value = c.itemId;
  try {
    const payload: Soldi = {
      mr: Math.max(0, Math.floor(Number(d.mr) || 0)),
      ma: Math.max(0, Math.floor(Number(d.ma) || 0)),
      mo: Math.max(0, Math.floor(Number(d.mo) || 0)),
      mp: Math.max(0, Math.floor(Number(d.mp) || 0)),
    };
    const res = await updateConto(c.itemId, payload);
    c.soldi = {...res.data};
    drafts[c.itemId] = {...res.data};
  } catch (e) {
    console.error('Errore salvataggio conto:', e);
    errorMsg.value = 'Errore nel salvataggio del conto';
  } finally {
    busySave.value = null;
  }
}
</script>

<template>
  <div class="soldi-page">
    <!-- soldi addosso -->
    <Mobile_Borsellino :id-personaggio="props.idPersonaggio"/>

    <div class="spazietto"/>

    <!-- conti banca -->
    <div v-if="loading" class="state">Caricamento conti…</div>
    <p v-else-if="errorMsg" class="error">{{ errorMsg }}</p>

    <template v-else>
      <section v-for="b in banche" :key="b.personaggioId" class="banca">
        <header class="banca-head">
          <span class="titolo">🏦 {{ b.nome }}</span>
          <span v-if="b.conti.length" class="tot">≈ {{ totaleDraft(b.conti[0]) }} MO</span>
        </header>

        <!-- nessun conto in questa banca: si può aprire -->
        <div v-if="!b.conti.length" class="apri-row">
          <span class="muted">Nessun conto in questa banca.</span>
          <button type="button" class="btn primary" :disabled="busyApri === b.personaggioId"
                  @click="onApriConto(b)">
            {{ busyApri === b.personaggioId ? 'Apertura…' : 'Apri conto' }}
          </button>
        </div>

        <div v-for="c in b.conti" :key="c.itemId" class="monete">
          <div v-for="m in MONETE" :key="m.key" class="moneta" :class="m.cls">
            <span class="sigla">{{ m.sigla }}</span>
            <div class="stepper">
              <button type="button" class="step-btn" :disabled="busySave === c.itemId"
                      @click="step(c, m.key, -1)">−</button>
              <input
                  v-if="drafts[c.itemId]"
                  type="number" min="0" step="1" inputmode="numeric"
                  v-model.number="drafts[c.itemId][m.key]"
                  :disabled="busySave === c.itemId"
              />
              <button type="button" class="step-btn" :disabled="busySave === c.itemId"
                      @click="step(c, m.key, 1)">+</button>
            </div>
          </div>
        </div>

        <Transazione v-if="b.conti.length" :disabled="busySave === b.conti[0].itemId"
                     @applica="delta => applicaTransazione(b.conti[0], delta)"/>

        <div v-if="b.conti.length && isDirty(b.conti[0])" class="actions">
          <button type="button" class="btn ghost" :disabled="busySave === b.conti[0].itemId"
                  @click="annulla(b.conti[0])">Annulla</button>
          <button type="button" class="btn primary" :disabled="busySave === b.conti[0].itemId"
                  @click="salva(b.conti[0])">
            {{ busySave === b.conti[0].itemId ? 'Salvataggio…' : 'Salva' }}
          </button>
        </div>
      </section>

      <div v-if="!banche.length" class="state">
        Nessuna banca disponibile.
      </div>
    </template>
  </div>
</template>

<style scoped>
.soldi-page {
  display: grid;
  gap: .5rem;
  align-content: start;
}

.state { font-size: .85rem; opacity: .7; padding: .5rem; }

.error {
  margin: 0;
  padding: .4rem .6rem;
  border-radius: .5rem;
  color: #991b1b;
  background: #fef2f2;
  border: 1px solid #fecaca;
  font-size: .8rem;
}

.banca {
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  background: #fff;
  padding: .6rem .75rem;
  display: grid;
  gap: .5rem;
}

.banca-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.titolo { font-weight: 700; }
.tot { font-size: .85rem; font-weight: 600; opacity: .75; }

.monete {
  display: grid;
  grid-template-columns: 1fr;
  gap: .4rem;
}

.moneta {
  display: grid;
  grid-template-columns: 2.2rem 1fr;
  align-items: center;
  gap: .5rem;
  padding: .3rem .4rem;
  border-radius: .5rem;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.moneta .sigla { font-size: .7rem; font-weight: 700; opacity: .8; }

.moneta.mp { background: #ecfeff; border-color: #a5f3fc; }
.moneta.mo { background: #fefce8; border-color: #fde68a; }
.moneta.ma { background: #f8fafc; border-color: #cbd5e1; }
.moneta.mr { background: #fff7ed; border-color: #fed7aa; }

.stepper {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: .2rem;
}

.stepper input {
  width: 100%;
  min-width: 0;
  text-align: center;
  padding: .3rem .15rem;
  border: 1px solid #d0d5dd;
  border-radius: .4rem;
  font-variant-numeric: tabular-nums;
}

.step-btn {
  width: 1.7rem;
  border: 1px solid #d0d5dd;
  border-radius: .4rem;
  background: #f9fafb;
  font-weight: 800;
  cursor: pointer;
}
.step-btn:disabled { opacity: .5; cursor: default; }

.actions {
  display: flex;
  justify-content: flex-end;
  gap: .5rem;
}

.apri-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: .5rem;
}

.muted { font-size: .85rem; opacity: .65; }

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
