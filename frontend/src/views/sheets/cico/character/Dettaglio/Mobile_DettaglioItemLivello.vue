<script setup lang="ts">
import {defineProps, onMounted, ref} from 'vue';
import {
  buildMappaItemAvanzamenti,
  buildMappaModificatoriAvanzamenti,
  mostraLabel,
  testoFormula,
  testoModificatore
} from "../../../../../function/Utils";
import {getItem} from "../../../../../service/PersonaggioService";
import {TIPO_ITEM} from "../../../../../function/Constants";
import {Avanzamento, ItemDB, Modificatore} from "../../../../../models/ItemDB";
import {useCharacterStore} from "../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import {useRouter} from "vue-router";
import usePopup from "../../../../../function/usePopup";
import Mobile_DettaglioItem from "./Mobile_DettaglioItem.vue";

const router = useRouter()
const {openPopup} = usePopup()

interface PropsData {
  idItem: number;            // l'oggetto item con id e tipo
  livello: number[];
  idPersonaggio: number;
  entity: ItemDB;
}

const props = defineProps<{ data: PropsData }>();

const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);

const mappaAvanzamenti = ref<Record<number, Avanzamento[]>>({});
const mappaModificatori = ref<Record<number, Modificatore[]>>({});
const listaAvanzamenti = ref<Avanzamento[]>([]);

const itemDetail = ref<ItemDB | null>(null);
const loading = ref(true);

// Mappe per labels
const labelMap = ref<Record<string, string>>({});

onMounted(async () => {
  try {
    if (props.data.idItem !== undefined) {
      const response = await getItem(props.data.idItem);
      itemDetail.value = response.data;
    }

    if (props.data.entity !== undefined) {
      itemDetail.value = props.data.entity;
    }

    console.log(itemDetail);
    // Popola labelMap con tutte le labels
    itemDetail.value.labels?.forEach(lbl => {
      // assumiamo lbl.label come key e lbl.valore come value
      labelMap.value[lbl.label] = lbl.valore;
    });
    console.log(labelMap);

    // Lista dei children
    const avanzamenti = itemDetail.value.avanzamento ?? [];
    listaAvanzamenti.value = avanzamenti;
    mappaAvanzamenti.value = buildMappaItemAvanzamenti(avanzamenti, props.data.livello)
    mappaModificatori.value = buildMappaModificatoriAvanzamenti(avanzamenti, props.data.livello)

  } catch (e) {
    console.error('Errore caricamento item:', e);
  } finally {
    loading.value = false;
  }
});

function showInfoItemPopup(itm) {
  openPopup(
      Mobile_DettaglioItem,
      {data: {item: {...itm}, personaggio: {id: props.data.idPersonaggio}}},
      {closable: true, autoClose: 0}
  )
}
</script>

<template>
  <div class="abilita-detail-card" v-if="!loading && itemDetail">
    <div v-if="Object.keys(mappaAvanzamenti).length">
      <table class="custom-table">
        <tbody>
        <tr>
          <td><strong>Livello</strong></td>
          <td><strong>Item derivati</strong></td>
        </tr>
        <tr v-for="(val, key) in mappaAvanzamenti" :key="`key`">
          <td>{{ key }}</td>
          <td>
            <template v-for="av in val.filter(x => x.itemTarget.tipo !== TIPO_ITEM.AVANZAMENTO)">
              <button
                  v-if="av.itemTarget"
                  type="button"
                  class="pill"
                  @click.stop="showInfoItemPopup(av.itemTarget)"
                  :title="`${av.itemTarget.nome}`"
              >
                {{ av.itemTarget.nome }}
              </button>
            </template>
          </td>
        </tr>
        </tbody>
      </table>
      <div class="spazietto"/>
    </div>

    <div v-if="Object.keys(mappaModificatori).length">
      <table class="custom-table">
        <tbody>
        <tr>
          <td><strong>Livello</strong></td>
          <td><strong>Modificatori</strong></td>
        </tr>
        <tr v-for="(val, key) in mappaModificatori" :key="`key`">
          <td>{{ key }}</td>
          <td>
            <template v-for="av in val">
              <span
              >
                <p>{{ testoFormula(testoModificatore(av.valore)) }} <strong>{{ av.stat.label }}</strong></p>
              </span>
            </template>
          </td>
        </tr>
        </tbody>
      </table>
      <div class="spazietto"/>
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
