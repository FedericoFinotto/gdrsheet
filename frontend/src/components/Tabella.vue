<script setup>
import { ref, defineProps, onMounted } from 'vue';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';

// Props
const props = defineProps({
  items: {
    type: Array,
    required: true
  },
  columns: {
    type: Array,
    required: true // [{ field: 'nome', label: 'Nome' }, ...]
  },
  expandable: {
    type: Boolean,
    default: false
  }
});

onMounted(() => {
  // Questo log mostra le props ricevute dal componente padre
  // e può essere utile per il debug iniziale.
  console.log("Tabella.vue montato con props:", props);
});

// Espansione righe
// `expandedRows` sarà un array degli oggetti riga che sono attualmente espanse.
// PrimeVue lo gestisce internamente, ma noi lo inizializziamo.
const expandedRows = ref([]);

// Funzione per espandere/comprimere la riga cliccata
const toggleRow = (event) => {
  // `event.data` è l'oggetto della riga cliccata
  const row = event.data;
  // Trova l'indice della riga nell'array delle righe espanse
  const index = expandedRows.value.findIndex(r => r.id === row.id); // Confronta per `id`

  if (index >= 0) {
    // Se la riga è già espansa, la rimuovi dall'array per comprimerla
    expandedRows.value.splice(index, 1);
    console.log("Riga compressa:", row.id);
  } else {
    // Se la riga non è espansa, la aggiungi all'array per espanderla
    // Se vuoi permettere una sola riga espansa alla volta, puoi fare così:
    expandedRows.value = [row]; // Questa linea assicura che solo una riga sia espansa
    // Se vuoi più righe espanse, usa: expandedRows.value.push(row);
    console.log("Riga espansa:", row.id);
  }
};
</script>

<template>
  <DataTable
      :value="items"
      dataKey="id"
      tableStyle="width: 100%"
      class="p-datatable-sm"
      v-model:expandedRows="expandedRows"
      selectionMode="single"
      @rowClick="expandable ? toggleRow : undefined"
  >
    <Column
        v-if="expandable"
        expander
        style="width: 3rem;"
    />

    <Column
        v-for="col in columns"
        :key="col.field"
        :field="col.field"
        :header="col.label"
    >
      <template v-if="$slots[`col-${col.field}`]" #body="slotProps">
        <slot :name="`col-${col.field}`" :data="slotProps.data" />
      </template>
      <template v-else #body="slotProps">
        {{ slotProps.data[col.field] }}
      </template>
    </Column>

    <template v-if="expandable" #expansion="slotProps">
      <slot name="expansion" :data="slotProps.data" />
    </template>
  </DataTable>
</template>

<style scoped>
/* Stili specifici per il componente Tabella.vue */
/* Puoi aggiungere stili per le righe espanse o altre parti della tabella */
</style>