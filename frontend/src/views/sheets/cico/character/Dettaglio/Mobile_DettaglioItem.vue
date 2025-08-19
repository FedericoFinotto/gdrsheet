<script setup lang="ts">
import {computed, defineProps, onMounted, ref} from 'vue';
import {iconForComponent, mostraLabel, testoFormula, testoModificatore} from "../../../../../function/Utils";
import {getItem, switchItemState} from "../../../../../service/PersonaggioService";
import {TIPO_ITEM} from "../../../../../function/Constants";
import type {ItemDB} from "../../../../../models/ItemDB";
import {getLabel, thereIsValoreLabel} from "../../../../../function/Calcolo";
import {useCharacterStore} from "../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import Icona from "../../../../../components/Icona/Icona.vue";

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

async function switchState() {
  let itemIdToSwitch = [];
  if (itemInfo.disabled) {
    switch (itemInfo.tipo) {
      case 'TRASFORMAZIONE':
        itemIdToSwitch.push(...personaggio.items.trasformazioni.filter(x => !x.disabled).map(x => x.id));
        break;
      case 'IDOLO':
        itemIdToSwitch.push(...personaggio.items.idoli.filter(x => !x.disabled).map(x => x.id));
        break;
    }
  }
  itemIdToSwitch.push(itemInfo.id);
  console.log("switchState", itemIdToSwitch);
  try {
    for (const id of itemIdToSwitch) {
      await switchItemState(Number(id), personaggio.modificatori.id);
    }
    await characterStore.fetchCharacter(personaggio.modificatori.id, true);
  } catch (e) {
    console.error('Errore nello switch dello stato:', e);
  }

}

const disableLabel = computed(() => {
  console.log(itemInfo);
  switch (itemInfo?.tipo) {
    case TIPO_ITEM.EQUIPAGGIAMENTO:
      return itemInfo.disabled ? 'Equipaggia' : 'Togli';
    case TIPO_ITEM.TRASFORMAZIONE:
      return itemInfo.disabled ? 'Trasformati' : 'Torna alla forma Base';
    case TIPO_ITEM.IDOLO:
      return itemInfo.disabled ? 'Prega' : 'Disabilita';
    default:
      return null;
  }
});


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
      if (thereIsValoreLabel(personaggio, atk, 'TPC')) {
        tpcMap.value[atk.id] = testoFormula(getLabel(personaggio, atk, 'TPC'))
      }
      if (thereIsValoreLabel(personaggio, atk, 'TPD')) {
        tpdMap.value[atk.id] = testoFormula(getLabel(personaggio, atk, 'TPD'));
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
    <button class="bottone" @click="switchState" v-if="disableLabel">{{ disableLabel }}</button>

    <div v-if="itemDetail?.labels?.length" style="display: flex">
      <div v-for="comp in itemDetail.labels">
        <Icona v-if="comp.label==='COMP_SP'" :name="iconForComponent(comp.valore)"></Icona>
      </div>
    </div>

    <!-- Labels dinamiche -->
    <div v-if="Object.keys(labelMap).length">
      <div v-for="(val, key) in labelMap" :key="key">
        <span v-if="mostraLabel(key, val)"><strong>{{
            mostraLabel(key, val).label
          }}:</strong> {{ mostraLabel(key, val).value }}</span>
      </div>
      <div class="spazietto"/>
    </div>

    <!-- Descrizione -->
    <div v-if="itemDetail.descrizione">
      <strong>Descrizione</strong><br>
      {{ itemDetail.descrizione }}
      <div style="height: 20px"></div>
      <div class="spazietto"/>
    </div>

    <!-- Attacchi -->
    <div v-if="listaAttacchi.length">
      <p><strong>Attacco:</strong></p>
      <p v-for="atk in listaAttacchi" :key="atk.id">
        {{ atk.nome }}
        <span v-if="tpcMap[atk.id]">(TPC: {{ tpcMap[atk.id] }})</span>
        <span v-if="tpdMap[atk.id]">(TPD: {{ tpdMap[atk.id] }})</span>
      </p>
      <div class="spazietto"/>
    </div>

    <!-- Abilità -->
    <div v-if="listaAbilita.length">
      <p><strong>Abilità:</strong></p>
      <p v-for="ability in listaAbilita" :key="ability.id">
        {{ ability.nome }}
      </p>
      <div class="spazietto"/>
    </div>

    <!-- Maledizioni -->
    <div v-if="listaMaledizioni.length">
      <p><strong>Maledizioni:</strong></p>
      <p v-for="mal in listaMaledizioni" :key="mal.id">
        {{ mal.nome }}
      </p>
      <div class="spazietto"/>
    </div>

    <!-- Modificatori -->
    <div v-if="itemDetail.modificatori?.length">
      <strong>Modificatori:</strong><br>
      <span v-for="mod in itemDetail.modificatori" :key="mod.id">
        <strong>{{ mod.stat.label }}:</strong>
        {{ testoModificatore(mod.valore) }}
        <span v-if="mod.nota">- {{ mod.nota }}</span><br>
      </span>
      <div class="spazietto"/>
    </div>
  </div>
</template>
