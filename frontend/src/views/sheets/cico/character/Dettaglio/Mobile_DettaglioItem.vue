<script setup lang="ts">
import {defineProps, onMounted, ref} from 'vue';
import {testoModificatore} from "../../../../../function/Utils";
import {getItem, switchItemState} from "../../../../../service/PersonaggioService";
import {TIPO_ITEM} from "../../../../../function/Constants";
import type {ItemDB} from "../../../../../models/ItemDB";
import {getValoreLabel, thereIsValoreLabel} from "../../../../../function/Calcolo";
import {useCharacterStore} from "../../../../../stores/personaggio";
import {storeToRefs} from "pinia";

interface PropsData {
  item: ItemDB;            // l'oggetto item con id e tipo
  personaggio: any;        // Statistiche del personaggio
}

const props = defineProps<{ data: PropsData }>();
const {item: itemInfo, personaggio} = props.data;

const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);

const listaAbilita = ref<ItemDB[]>([]);
const listaAttacchi = ref<ItemDB[]>([]);
const listaMaledizioni = ref<ItemDB[]>([]);
const itemDetail = ref<ItemDB | null>(null);
const loading = ref(true);

// Mappe per labels
const labelMap = ref<Record<string, string>>({});

// Mappe per TPC/TPD
const tpcMap = ref<Record<number, string>>({});
const tpdMap = ref<Record<number, string>>({});

const mostraLabel = (label, val) => {
  switch (label) {
    case "TEMPO_SP":
      return "Azione";
      break;
    case "RANGE_SP":
      return "Range";
      break;
    case "DURATA_SP":
      return "Durata";
      break;
    case "TS_SP":
      if (val !== 'None') {
        return "Tiro Salvezza";
      }
      break;
    default:
      return null;
  }
}

const switchState = async () => {
  try {
    await switchItemState(itemInfo.id);
    // usa personaggio.id, non personaggio.modificatori.id
    console.log('RICALCOLO', personaggio.modificatori.id)
    await characterStore.fetchCharacter(personaggio.modificatori.id, true);
  } catch (e) {
    console.error('Errore nello switch dello stato:', e);
  }
};

onMounted(async () => {
  try {
    const response = await getItem(itemInfo.id);
    const data = response.data;
    itemDetail.value = data;

    // Popola labelMap con tutte le labels
    data.labels?.forEach(lbl => {
      // assumiamo lbl.label come key e lbl.valore come value
      labelMap.value[lbl.label] = lbl.valore;
    });

    // Lista dei children
    const children = data.child ?? [];
    listaAbilita.value = children
        .filter(c => c.itemTarget.tipo === TIPO_ITEM.ABILITA)
        .map(c => c.itemTarget);
    listaAttacchi.value = children
        .filter(c => c.itemTarget.tipo === TIPO_ITEM.ATTACCO)
        .map(c => c.itemTarget);
    listaMaledizioni.value = children
        .filter(c => c.itemTarget.tipo === TIPO_ITEM.MALEDIZIONE)
        .map(c => c.itemTarget);

    // Popola mappe TPC/TPD in modo asincrono
    for (const atk of listaAttacchi.value) {
      if (await thereIsValoreLabel(personaggio, atk, 'TPC')) {
        const resp = await getValoreLabel(personaggio, atk, 'TPC');
        tpcMap.value[atk.id] = resp.data.risultato;
      }
      if (await thereIsValoreLabel(personaggio, atk, 'TPD')) {
        const resp = await getValoreLabel(personaggio, atk, 'TPD');
        tpdMap.value[atk.id] = resp.data.risultato;
      }
    }
  } catch (e) {
    console.error('Errore caricamento item:', e);
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="abilita-detail-card" v-if="!loading && itemDetail">
    <button class="bottone" @click="switchState">TOMARE</button>
    <!-- Labels dinamiche -->
    <div v-if="Object.keys(labelMap).length">
      <div v-for="(val, key) in labelMap" :key="key">
        <span v-if="mostraLabel(key, val)"><strong>{{ mostraLabel(key) }}:</strong> {{ val }}</span>
      </div>
      <br>
    </div>

    <!-- Descrizione -->
    <div v-if="itemDetail.descrizione">
      <strong>Descrizione</strong><br>
      {{ itemDetail.descrizione }}
      <div style="height: 20px"></div>
    </div>

    <!-- Attacchi -->
    <div v-if="listaAttacchi.length">
      <p><strong>Attacco:</strong></p>
      <p v-for="atk in listaAttacchi" :key="atk.id">
        {{ atk.nome }}
        <span v-if="tpcMap[atk.id]">(TPC: {{ tpcMap[atk.id] }})</span>
        <span v-if="tpdMap[atk.id]">(TPD: {{ tpdMap[atk.id] }})</span>
      </p>
    </div>

    <!-- Abilità -->
    <div v-if="listaAbilita.length">
      <p><strong>Abilità:</strong></p>
      <p v-for="ability in listaAbilita" :key="ability.id">
        {{ ability.nome }}
      </p>
    </div>

    <!-- Maledizioni -->
    <div v-if="listaMaledizioni.length">
      <p><strong>Maledizioni:</strong></p>
      <p v-for="mal in listaMaledizioni" :key="mal.id">
        {{ mal.nome }}
      </p>
    </div>

    <!-- Modificatori -->
    <div v-if="itemDetail.modificatori?.length">
      <strong>Modificatori:</strong><br>
      <span v-for="mod in itemDetail.modificatori" :key="mod.id">
        <strong>{{ mod.stat.label }}:</strong>
        {{ testoModificatore(mod.valore) }}
        <span v-if="mod.nota">- {{ mod.nota }}</span><br>
      </span>
    </div>
  </div>
</template>

<style scoped>
.abilita-detail-card {
  margin-left: 30px;
}

.abilita-detail-card div {
  margin-bottom: 4px;
}

.bottone {
  width: 30%;
  height: 30px;
}
</style>
