<script setup>
import {defineProps, onMounted} from 'vue';
import {testoModificatore} from "../../../../../function/Utils";

const props = defineProps({
  stat: {
    type: Object,
    required: true
  }
});

onMounted(() => {
  console.log("Component mounted.", props.stat);
});
</script>

<template>
  <div class="abilita-detail-card">
    <div class="abilita-detail-card__header">
      <strong>{{ props.stat.label }}</strong>: {{ testoModificatore(props.stat.modificatore) }}
    </div>
    <br><br>
    <!--        <p><strong>Grado {{ data.rank.valore }}: </strong> {{ testoModificatore(data.rank.modificatore) }}</p>-->
    <!--        <p v-if="data.base?.id"><strong>{{ data.base.label }}: </strong>{{ testoModificatore(data.base.modificatore) }}</p>-->

    <div v-if="props.stat.modificatori.filter(x => x.sempreAttivo)">
      <p v-for="(mod, index) in props.stat.modificatori.filter(x => x.sempreAttivo)" :key="index">
        <strong>{{ mod.item || 'Sconosciuto' }}:</strong>
        {{ testoModificatore(mod.valore) }}
        <span v-if="mod.nota">{{ mod.nota }}</span>
      </p>
    </div>
    <div class="spazietto"
         v-if="props.stat.modificatori.filter(x => x.sempreAttivo).length > 0 && props.stat.modificatori.filter(x => !x.sempreAttivo).length > 0"></div>
    <div v-if="props.stat.modificatori">
      <p v-for="(mod, index) in props.stat.modificatori.filter(x => !x.sempreAttivo)" :key="index">
        <strong>{{ mod.item || 'Sconosciuto' }}:</strong>
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

.spazietto {
  height: 20px;
}
</style>