<script setup lang="ts">
import {computed, defineProps, onMounted, ref} from 'vue';
import {
  buildMappaItemAvanzamenti,
  buildMappaModificatoriAvanzamenti,
  iconForComponent,
  mostraLabel,
  testoFormula,
  testoModificatore
} from "../../../../../function/Utils";
import {getItem, switchItemState} from "../../../../../service/PersonaggioService";
import {ItemDB, TIPO_ITEM} from "../../../../../models/entity/ItemDB";
import {useCharacterStore} from "../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import Icona from "../../../../../components/Icona/Icona.vue";
import {useRouter} from "vue-router";
import usePopup from "../../../../../function/usePopup";
import Mobile_DettaglioItem from "./Mobile_DettaglioItem.vue";
import {Modificatore} from "../../../../../models/entity/Modificatore";
import {Avanzamento} from "../../../../../models/entity/Avanzamento";
import {Item} from "../../../../../models/dto/Item";
import {getItemLabel, LABELS, thereIsValoreLabel} from "../../../../../models/entity/ItemLabel";

const router = useRouter()
const {openPopup} = usePopup()

const props = defineProps<{
  data: {
    item: Item;            // l'oggetto item con id e tipo
    personaggio: any;
  }
}>();
const {item, personaggio} = props.data;

const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);

const listaAbilita = ref<ItemDB[]>([]);
const listaAttacchi = ref<ItemDB[]>([]);
const listaMaledizioni = ref<ItemDB[]>([]);
const mappaAvanzamenti = ref<Record<number, Avanzamento[]>>({});
const mappaModificatori = ref<Record<number, Modificatore[]>>({});
const listaAvanzamenti = ref<Avanzamento[]>([]);

const itemDetail = ref<ItemDB | null>(null);
const loading = ref(true);

// Mappe per labels
const labelMap = ref<Record<string, string>>({});

// Mappe per TPC/TPD
const tpcMap = ref<Record<number, string>>({});
const tpdMap = ref<Record<number, string>>({});

async function switchState() {
  let itemIdToSwitch = [];
  if (item.disabled) {
    switch (item.tipo) {
      case 'TRASFORMAZIONE':
        itemIdToSwitch.push(...personaggio.items.trasformazioni.filter(x => !x.disabled).map(x => x.id));
        break;
      case 'IDOLO':
        itemIdToSwitch.push(...personaggio.items.idoli.filter(x => !x.disabled).map(x => x.id));
        break;
    }
  }
  itemIdToSwitch.push(item.id);
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
  switch (item?.tipo) {
    case TIPO_ITEM.EQUIPAGGIAMENTO:
      return item.disabled ? 'Equipaggia' : 'Togli';
    case TIPO_ITEM.TRASFORMAZIONE:
      return item.disabled ? 'Trasformati' : 'Torna alla forma Base';
    case TIPO_ITEM.IDOLO:
      return item.disabled ? 'Prega' : 'Disabilita';
    default:
      return null;
  }
});


onMounted(async () => {
  try {
    const response = await getItem(item.id);
    const data = response.data;
    itemDetail.value = data;

    // Popola labelMap con tutte le labels
    data.labels?.forEach(lbl => {
      // assumiamo lbl.label come key e lbl.valore come value
      labelMap.value[lbl.label] = lbl.valore;
    });

    // Lista dei children
    const children = data.child ?? [];
    const avanzamenti = data.avanzamento ?? [];
    listaAbilita.value = children
        .filter(c => c.itemTarget.tipo === TIPO_ITEM.ABILITA)
        .map(c => c.itemTarget);
    listaAttacchi.value = children
        .filter(c => c.itemTarget.tipo === TIPO_ITEM.ATTACCO)
        .map(c => c.itemTarget);
    listaMaledizioni.value = children
        .filter(c => c.itemTarget.tipo === TIPO_ITEM.MALEDIZIONE)
        .map(c => c.itemTarget);
    listaAvanzamenti.value = avanzamenti;
    mappaAvanzamenti.value = buildMappaItemAvanzamenti(avanzamenti)
    mappaModificatori.value = buildMappaModificatoriAvanzamenti(avanzamenti)

    // Popola mappe TPC/TPD in modo asincrono
    for (const atk of listaAttacchi.value) {
      if (thereIsValoreLabel(atk, LABELS.TIRO_COLPIRE)) {
        tpcMap.value[atk.id] = testoFormula(getItemLabel(atk, LABELS.TIRO_COLPIRE))
      }
      if (thereIsValoreLabel(atk, LABELS.TIRO_DANNI)) {
        tpdMap.value[atk.id] = testoFormula(getItemLabel(atk, LABELS.TIRO_DANNI));
      }
    }
  } catch (e) {
    console.error('Errore caricamento item:', e);
  } finally {
    loading.value = false;
  }
});

function showInfoItemPopup(itm) {
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

    <div v-if="itemDetail?.labels?.length" style="display: flex">
      <div v-for="comp in itemDetail.labels">
        <Icona v-if="comp.label==='COMP_SP'" :name="iconForComponent(comp.valore)"></Icona>
      </div>
    </div>

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
    <div v-if="listaAbilita && listaAbilita.length">
      <p><strong>Abilità:</strong></p>

      <p v-for="ability in listaAbilita" :key="ability.id">
        {{ ability.nome }}
        <Icona name="INFO" @click.stop="showInfoItemPopup(ability)"/>
      </p>

      <div class="spazietto"></div>
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
