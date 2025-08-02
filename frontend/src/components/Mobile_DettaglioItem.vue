<script setup lang='ts'>
import {defineProps, onMounted, ref} from 'vue';
import {testoModificatore} from "../function/Utils";
import {getValoreLabel} from "../function/Calcolo";
import {getItem} from "../service/PersonaggioService";
import {TIPO_ITEM} from "../function/Constants";

const props = defineProps({
  data: {
    type: Object,
    required: true
  }
});

const listaAbilita = ref<any[]>([]);
const listaAttacchi = ref<any[]>([]);
const listaMaledizioni = ref<any[]>([]);

const item = ref<Object>({});
const loading = ref(true);

onMounted(() => {
  getItem(props.data.item.id).then(resp => {
    item.value = resp.data;
    listaAbilita.value = resp.data.child.filter(x => x.tipo === TIPO_ITEM.ABILITA);
    listaAttacchi.value = resp.data.child.filter(x => x.tipo === TIPO_ITEM.ATTACCO);
    listaMaledizioni.value = resp.data.child.filter(x => x.tipo === TIPO_ITEM.MALEDIZIONE);
    loading.value = false;
  })
});
</script>

<template>
  <div class="abilita-detail-card" v-if="!loading">
    <div v-if="item.descrizione">
      {{ item.descrizione }}
      <div style="height: 20px"></div>
    </div>

    <div v-if="listaAttacchi.length > 0">
      <p><strong>Attacco:</strong></p>
      <p v-for="(itm, index) in listaAttacchi" :key="index">
        {{ itm.itemTarget.nome }}
        <span v-if="getValoreLabel(data.personaggio, itm.itemTarget, 'TPC')"> (TPC: {{
            getValoreLabel(data.personaggio, itm.itemTarget, 'TPC')
          }})</span>
        <span v-if="getValoreLabel(data.personaggio, itm.itemTarget, 'TPD')"> (TPD: {{
            getValoreLabel(data.personaggio, itm.itemTarget, 'TPD')
          }})</span>
      </p>
    </div>


    <div v-if="listaAbilita.length >0">
      <p><strong>Abilita: </strong></p>
      <p v-for="(itm, index) in listaAbilita" :key="index">
        {{ itm.itemTarget.nome }}
      </p>
    </div>

    <div v-if="listaMaledizioni.length >0">
      <p><strong>Maledizioni: </strong></p>
      <p v-for="(itm, index) in listaMaledizioni" :key="index">
        {{ itm.itemTarget.nome }}
      </p>
    </div>

    <div v-if="item.modificatori && item.modificatori.length > 0">
      <p><strong>Modificatori: </strong></p>
      <p v-for="(mod, index) in item.modificatori" :key="index">
        <strong>{{ mod.stat.label }}:</strong>
        {{ testoModificatore(mod.valore) }}
        <span v-if="mod.nota">{{ mod.nota }}</span>
      </p>
    </div>
  </div>
</template>

<style scoped>

.abilita-detail-card {
  margin: 0;
  margin-left: 30px;
}

p {
  margin: 0;
}

ul {
  padding: 0;
}

li {
  margin-bottom: 6px;
  border-radius: 4px;
}

</style>