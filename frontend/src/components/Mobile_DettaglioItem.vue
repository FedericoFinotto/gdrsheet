<script setup lang="ts">
import {defineProps, onMounted, ref} from 'vue';
import {testoModificatore} from "../function/Utils";
import {getItem} from "../service/PersonaggioService";
import {TIPO_ITEM} from "../function/Constants";
import type {ItemDB} from "../models/ItemDB";
import {getValoreLabel, thereIsValoreLabel} from "../function/Calcolo";

interface PropsData {
  item: ItemDB;            // l'oggetto item con id e tipo
  personaggio: any;        // Statistiche del personaggio (tipi a piacere)
}

const props = defineProps<{ data: PropsData }>();
const {item: itemInfo, personaggio} = props.data;

const listaAbilita = ref<ItemDB[]>([]);
const listaAttacchi = ref<ItemDB[]>([]);
const listaMaledizioni = ref<ItemDB[]>([]);
const itemDetail = ref<ItemDB | null>(null);
const loading = ref(true);
const tpcMap = ref<Record<number, string>>({});
const tpdMap = ref<Record<number, string>>({});

onMounted(async () => {
  try {
    // Carica l'item
    const data = await getItem(itemInfo.id);
    itemDetail.value = data.data;

    // Lista dei children
    const children = data.data.child ?? [];
    listaAbilita.value = children.filter(c => c.itemTarget.tipo === TIPO_ITEM.ABILITA).map(c => c.itemTarget);
    listaAttacchi.value = children.filter(c => c.itemTarget.tipo === TIPO_ITEM.ATTACCO).map(c => c.itemTarget);
    listaMaledizioni.value = children.filter(c => c.itemTarget.tipo === TIPO_ITEM.MALEDIZIONE).map(c => c.itemTarget);

    // Popola mappe TPC/TPD in modo asincrono
    for (const atk of listaAttacchi.value) {
      const hasTpc = await thereIsValoreLabel(personaggio, atk, 'TPC');
      if (hasTpc) {
        getValoreLabel(personaggio, atk, 'TPC').then(resp => {
          tpcMap.value[atk.id] = resp.data.risultato;
        });

      }
      const hasTpd = await thereIsValoreLabel(personaggio, atk, 'TPD');
      if (hasTpd) {
        getValoreLabel(personaggio, atk, 'TPD').then(resp => {
          tpdMap.value[atk.id] = resp.data.risultato;
        });
      }
    }
    console.log('AAA', tpcMap, tpdMap)
  } catch (e) {
    console.error('Errore caricamento item:', e);
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="abilita-detail-card" v-if="!loading && itemDetail">
    <!-- Descrizione -->
    <div v-if="itemDetail.descrizione">
      {{ itemDetail.descrizione }}
      <div style="height: 20px"></div>
    </div>

    <!-- Attacchi -->
    <div v-if="listaAttacchi.length > 0">
      <p><strong>Attacco:</strong></p>
      <p v-for="atk in listaAttacchi" :key="atk.id">
        {{ atk.nome }}
        <span v-if="tpcMap[atk.id]">(TPC: {{ tpcMap[atk.id] }})</span>
        <span v-if="tpdMap[atk.id]">(TPD: {{ tpdMap[atk.id] }})</span>
      </p>
    </div>

    <!-- Abilità -->
    <div v-if="listaAbilita.length > 0">
      <p><strong>Abilità:</strong></p>
      <p v-for="ability in listaAbilita" :key="ability.id">
        {{ ability.nome }}
      </p>
    </div>

    <!-- Maledizioni -->
    <div v-if="listaMaledizioni.length > 0">
      <p><strong>Maledizioni:</strong></p>
      <p v-for="mal in listaMaledizioni" :key="mal.id">
        {{ mal.nome }}
      </p>
    </div>

    <!-- Modificatori dell'item -->
    <div v-if="itemDetail.modificatori?.length > 0">
      <p><strong>Modificatori:</strong></p>
      <p v-for="mod in itemDetail.modificatori" :key="mod.id">
        <strong>{{ mod.stat.label }}:</strong>
        {{ testoModificatore(mod.valore) }}
        <span v-if="mod.nota">- {{ mod.nota }}</span>
      </p>
    </div>
  </div>
</template>

<style scoped>
.abilita-detail-card {
  margin-left: 30px;
}

.abilita-detail-card p {
  margin: 0;
}
</style>
