<script setup lang="ts">
import {computed, defineProps, markRaw, ref, watch} from 'vue';
import {useRouter} from 'vue-router';
import Tabella from "../../../../../../components/Tabella.vue";
import {useCharacterStore} from "../../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import Mobile_DettaglioLivello from "../../Dettaglio/Mobile_DettaglioLivello.vue";
import {Livello} from "../../../../../../models/dto/Livello";
import {createItem} from "../../../../../../service/PersonaggioService";

const router = useRouter();
const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);

const props = defineProps({
  idPersonaggio: {
    type: Number,
    required: true
  }
});
const itemsLivelli = ref<Livello[]>([]);

watch(
    () => cache.value[props.idPersonaggio]?.items,
    async (newChar) => {
      if (!newChar || !newChar.livelli) {
        itemsLivelli.value = [];
        return;
      }

      // Ordina prima
      itemsLivelli.value = newChar.livelli
          .map(itm => {
            return {
              ...itm,
              testoLivello: `Livello ${itm.livello}`,
              testoClasse: `${itm.classe ?? ''} ${itm.livelliClasse.join(' ')}`,
              expandedComponent: markRaw(Mobile_DettaglioLivello),
              expandedProps: {data: {item: {...itm}, personaggio: cache.value[props.idPersonaggio]}}
            };
          })
          .sort((a, b) => Number(a.livello ?? 0) - Number(b.livello ?? 0));
    },
    {immediate: true, deep: true}
);

const columnsAttacchi = [
  {field: 'testoLivello', subfield: '', label: 'Livello'},
  {field: 'testoClasse', subfield: 'maledizione', label: ''},
];

const creandoLivello = ref(false);
// il livello 0 è il record "razza" (caratteristiche di partenza): esiste al più una volta
const hasLivelloZero = computed(() => itemsLivelli.value.some(l => Number(l.livello) === 0));

async function aggiungiLivello() {
  if (creandoLivello.value) return;
  creandoLivello.value = true;
  try {
    const prossimo = itemsLivelli.value.reduce((max, l) => Math.max(max, Number(l.livello) || 0), 0) + 1;
    const res = await createItem({
      nome: `Livello ${prossimo}`,
      tipo: 'LIVELLO',
      idPersonaggio: props.idPersonaggio,
      labels: [{label: 'LVL', valore: String(prossimo)}],
    });
    await characterStore.fetchCharacter(props.idPersonaggio, true);
    router.push(`/itemeditor/${res.data.id}?personaggio=${props.idPersonaggio}`);
  } catch (e) {
    console.error('Errore creazione livello:', e);
  } finally {
    creandoLivello.value = false;
  }
}

async function aggiungiRazza() {
  if (creandoLivello.value || hasLivelloZero.value) return;
  creandoLivello.value = true;
  try {
    const res = await createItem({
      nome: `Livello 0`,
      tipo: 'LIVELLO',
      idPersonaggio: props.idPersonaggio,
      labels: [{label: 'LVL', valore: '0'}],
    });
    await characterStore.fetchCharacter(props.idPersonaggio, true);
    // apreCaratteristiche=1: segnala all'editor di aprire subito la tab "Caratteristiche"
    router.push(`/itemeditor/${res.data.id}?personaggio=${props.idPersonaggio}&apriCaratteristiche=1`);
  } catch (e) {
    console.error('Errore creazione livello 0 (razza):', e);
  } finally {
    creandoLivello.value = false;
  }
}

function gestisciGradi() {
  router.push(`/gestisci-gradi/${props.idPersonaggio}`);
}
</script>

<template>
  <div>
    <div class="livelli-actions">
      <button type="button" class="btn-add-livello" :disabled="creandoLivello" @click="aggiungiLivello">
        <span class="plus">+</span>
        <span>{{ creandoLivello ? 'Creazione…' : 'Aggiungi livello' }}</span>
      </button>
      <button v-if="!hasLivelloZero" type="button" class="btn-add-livello" :disabled="creandoLivello" @click="aggiungiRazza">
        <span class="plus">+</span>
        <span>{{ creandoLivello ? 'Creazione…' : 'Aggiungi Razza' }}</span>
      </button>
      <button type="button" class="btn-gestisci-gradi" @click="gestisciGradi">
        Gestisci gradi
      </button>
    </div>
    <div class="spazietto"/>
    <Tabella
        v-if="itemsLivelli.length > 0"
        :columns="columnsAttacchi"
        :expandable="true"
        :items="itemsLivelli"
    />
  </div>
</template>

<style scoped>
.btn-add-livello {
  display: inline-flex;
  align-items: center;
  gap: .35rem;
  padding: .4rem .8rem;
  border: 1px dashed var(--hairline);
  border-radius: .5rem;
  background: var(--surface-0);
  color: var(--text-muted);
  font-weight: 600;
  font-size: .85rem;
  cursor: pointer;
}

.btn-add-livello:hover { background: var(--btn-bg-hover); }
.btn-add-livello:disabled { opacity: .6; cursor: default; }
.plus { font-weight: 800; color: #2563eb; }

.livelli-actions {
  display: flex;
  gap: .5rem;
  flex-wrap: wrap;
  align-items: center;
}

.btn-gestisci-gradi {
  padding: .4rem .8rem;
  border: 1px solid #2563eb;
  border-radius: .5rem;
  background: var(--info-bg);
  color: var(--info-text);
  font-weight: 600;
  font-size: .85rem;
  cursor: pointer;
}

.btn-gestisci-gradi:hover { background: var(--info-border); }
</style>
