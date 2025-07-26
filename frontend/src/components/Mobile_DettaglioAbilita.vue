<script setup>
import {defineProps, onMounted} from 'vue';
import {testoModificatore} from "../function/Utils";

const props = defineProps({
  data: {
    type: Object,
    required: true
  }
});

onMounted(() => {
  console.log(props);
});
</script>

<template>
  <div class="abilita-detail-card">

    <p><strong>Grado {{ data.rank.valore }} : </strong> {{ testoModificatore(data.rank.modificatore) }}</p>
    <p v-if="data.base"><strong>{{ data.base.label }}: </strong>{{ testoModificatore(data.base.modificatore) }}</p>

    <div v-if="data.statistica.modificatori && data.statistica.modificatori.length > 0">
      <p v-for="(mod, index) in data.statistica.modificatori" :key="index">
        <strong>{{ mod.item_nome || 'Sconosciuto' }}:</strong>
        {{ mod.valore }}
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