<script setup lang="ts">
import {computed, defineProps, onMounted, ref} from 'vue';
import {mostraLabel, testoModificatore} from "../../../../../function/Utils";
import {getItem, switchItemState} from "../../../../../service/PersonaggioService";
import {ItemDB, TIPO_ITEM} from "../../../../../models/entity/ItemDB";
import {useCharacterStore} from "../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import Icona from "../../../../../components/Icona/Icona.vue";
import {useRouter} from "vue-router";
import usePopup from "../../../../../function/usePopup";
import Mobile_DettaglioItem from "./Mobile_DettaglioItem.vue";
import {Item} from "../../../../../models/dto/Item";

const router = useRouter()
const {openPopup} = usePopup()

const props = defineProps<{
  data: {
    item: Item;            // l'oggetto item con id e tipo
    personaggio: any;        // Statistiche del personaggio
  }
}>();
const {item: itemInfo, personaggio} = props.data;

const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);

const listaClassi = ref<ItemDB[]>([]);
const listaMaledizioni = ref<ItemDB[]>([]);
const itemDetail = ref<ItemDB | null>(null);
const loading = ref(true);

// Mappe per labels
const labelMap = ref<Record<string, string>>({});

// Mappe per TPC/TPD
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
  // console.log(itemInfo);
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
    listaClassi.value = children
        .filter(c => c.itemTarget.tipo === TIPO_ITEM.CLASSE || c.itemTarget.tipo === TIPO_ITEM.RAZZA)
        .map(c => c.itemTarget);
    listaMaledizioni.value = children
        .filter(c => c.itemTarget.tipo === TIPO_ITEM.MALEDIZIONE)
        .map(c => c.itemTarget);

  } catch (e) {
    console.error('Errore caricamento item:', e);
  } finally {
    loading.value = false;
  }
});

function showInfoAbilitaPopup(itm) {
  openPopup(
      Mobile_DettaglioItem,
      {data: {item: {...itm}, personaggio: props.data.personaggio}},
      {closable: true, autoClose: 0}
  )
}
</script>

<template>
  <div class="abilita-detail-card" v-if="!loading && itemDetail">
    <button class="bottone" @click="switchState" v-if="disableLabel">{{ disableLabel }}</button>
    <div class="icona-wrapper">
      <Icona
          name="EDIT"
          @click.stop="router.push(`/itemeditor/${itemDetail.id}`)"
      />
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
    <!--    <div v-if="itemDetail.descrizione">-->
    <!--      <strong>Descrizione</strong><br>-->
    <!--      {{ itemDetail.descrizione }}-->
    <!--      <div style="height: 20px"></div>-->
    <!--      <div class="spazietto"/>-->
    <!--    </div>-->

    <div v-if="listaClassi && listaClassi.length">
      <p><strong>Classe:</strong></p>

      <p v-for="ability in listaClassi" :key="ability.id">
        {{ ability.nome }}
        <Icona name="INFO" @click.stop="showInfoAbilitaPopup(ability)"/>
      </p>

      <div class="spazietto"></div>
    </div>

    <div v-if="listaMaledizioni && listaMaledizioni.length">
      <p><strong>Maledizione:</strong></p>

      <p v-for="ability in listaMaledizioni" :key="ability.id">
        {{ ability.nome }}
        <Icona name="INFO" @click.stop="showInfoAbilitaPopup(ability)"/>
      </p>

      <div class="spazietto"></div>
    </div>

    <!-- Modificatori -->
    <div v-if="itemDetail.modificatori?.filter(x=> x.tipo === 'BASE').length">
      <strong>BASE:</strong><br>
      <span v-for="mod in itemDetail.modificatori.filter(x=> x.tipo === 'BASE')" :key="mod.id">
        <strong>{{ mod.stat.label }}:</strong>
        {{ mod.valore }}
        <span v-if="mod.nota">- {{ mod.nota }}</span><br>
      </span>
      <div class="spazietto"/>
    </div>

    <!-- Modificatori -->
    <div v-if="itemDetail.modificatori?.filter(x=> x.tipo !== 'RANK' && x.tipo !== 'BASE').length">
      <strong>Modificatori:</strong><br>
      <span v-for="mod in itemDetail.modificatori.filter(x=> x.tipo !== 'RANK' && x.tipo !== 'BASE')" :key="mod.id">
        <strong>{{ mod.stat.label }}:</strong>
        {{ testoModificatore(mod.valore) }}
        <span v-if="mod.nota">- {{ mod.nota }}</span><br>
      </span>
      <div class="spazietto"/>
    </div>

    <div v-if="itemDetail.modificatori?.filter(x=> x.tipo === 'RANK').length">
      <strong>Gradi:</strong><br>
      <span v-for="mod in itemDetail.modificatori.filter(x=> x.tipo === 'RANK')" :key="mod.id">
        <strong>{{ mod.stat.label }}:</strong>
        {{ testoModificatore(mod.valore) }}
        <span v-if="mod.nota">- {{ mod.nota }}</span><br>
      </span>
      <div class="spazietto"/>
    </div>
  </div>
</template>
