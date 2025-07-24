<script setup>
import { defineProps, onMounted } from 'vue';

// Definisce le props accettate da questo componente.
// Si aspetta un oggetto 'data' che conterrà tutte le proprietà della riga espansa
// che sono state passate tramite `expandedData` dal componente padre.
const props = defineProps({
  data: {
    type: Object,
    required: true
  }
});

onMounted(() => {
  // Debugging: conferma che il componente è montato e quali dati ha ricevuto.
  console.log("DettaglioAbilita è stato montato con dati:", props.data);
});
</script>

<template>
  <div class="abilita-detail-card">

    <p><strong>Valore Abilità Finale:</strong> {{ data.valore }}</p>
    <p v-if="data.caratteristica"><strong>Caratteristica Associata:</strong> {{ data.caratteristica }}</p>
    <p v-if="data.valore_caratteristica !== undefined">
      <strong>Valore Base Caratteristica:</strong> {{ data.valore_caratteristica }}
    </p>

    <h5>Modificatori Applicati:</h5>
    <ul v-if="data.modificatori && data.modificatori.length > 0">
      <li v-for="(mod, index) in data.modificatori" :key="index">
        <strong>{{ mod.sorgente || 'Fonte Sconosciuta' }}:</strong>
        {{ mod.valore > 0 ? '+' : '' }}{{ mod.valore }}
        <span v-if="mod.descrizione">({{ mod.descrizione }})</span>
      </li>
    </ul>
    <p v-else>Nessun modificatore specifico è stato applicato a questa abilità.</p>

  </div>
</template>

<style scoped>
/* Stili specifici per il componente DettaglioAbilita.vue */
.abilita-detail-card {
  background-color: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 5px;
  padding: 20px;
  margin-top: 15px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}

h4 {
  color: #333;
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 1.3em;
}

h5 {
  color: #555;
  margin-top: 20px;
  margin-bottom: 10px;
  font-size: 1.1em;
}

p {
  margin-bottom: 8px;
  line-height: 1.5;
}

ul {
  list-style-type: none; /* Rimuove i bullet point di default */
  padding: 0;
  margin-left: 15px; /* Indenta la lista per chiarezza */
}

li {
  background-color: #f9f9f9;
  border-left: 3px solid #007bff;
  padding: 8px 12px;
  margin-bottom: 6px;
  border-radius: 4px;
}

li strong {
  color: #0056b3;
}
</style>