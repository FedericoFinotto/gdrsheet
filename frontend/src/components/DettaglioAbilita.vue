<script setup>
import { defineProps, onMounted } from 'vue';

const props = defineProps({
  data: {
    type: Object,
    required: true
  }
});

onMounted(() => {
});
</script>

<template>
  <div class="abilita-detail-card">

    <p><strong>Rank:  </strong> {{data.base > 0 ? '+' : ''}}{{ data.base }}</p>
    <p v-if="data.caratteristica"><strong>{{data.caratteristica}}:  </strong>{{(data.bonusCaratteristica > 0 ? '+' : '')+data.bonusCaratteristica}}</p>

    <div v-if="data.modificatori && data.modificatori.length > 0">
      <p v-for="(mod, index) in data.modificatori" :key="index">
        <strong>{{ mod.nome || 'Sconosciuto' }}:</strong>
        {{ mod.valore > 0 ? '+' : '' }}{{ mod.valore }}
        <span v-if="mod.descrizione">({{ mod.descrizione }})</span>
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