<template>
  <div class="card">
    <DataTable v-model:expandedRows="expandedRows" :value="products" dataKey="id"
               @rowExpand="onRowExpand" @rowCollapse="onRowCollapse" tableStyle="min-width: 60rem">
      <template #header>
        <div class="flex flex-wrap justify-end gap-2">
          <Button text icon="pi pi-plus" label="Expand All" @click="expandAll" />
          <Button text icon="pi pi-minus" label="Collapse All" @click="collapseAll" />
        </div>
      </template>
      <Column expander style="width: 5rem" />
      <Column field="name" header="Name"></Column>
      <Column header="Image">
        <template #body="slotProps">
          <img :src="`https://primefaces.org/cdn/primevue/images/product/${slotProps.data.image}`" :alt="slotProps.data.image" class="shadow-lg" width="64" />
        </template>
      </Column>
      <Column field="price" header="Price">
        <template #body="slotProps">
          {{ formatCurrency(slotProps.data.price) }}
        </template>
      </Column>
      <Column field="category" header="Category"></Column>
      <Column field="rating" header="Reviews">
        <template #body="slotProps">
          <Rating :modelValue="slotProps.data.rating" readonly />
        </template>
      </Column>
      <Column header="Status">
        <template #body="slotProps">
          <Tag :value="slotProps.data.inventoryStatus" :severity="getSeverity(slotProps.data)" />
        </template>
      </Column>
      <template #expansion="slotProps">
        <div class="p-4">
          <h5>Orders for {{ slotProps.data.name }}</h5>
          <DataTable :value="slotProps.data.orders">
            <Column field="id" header="Id" sortable></Column>
            <Column field="customer" header="Customer" sortable></Column>
            <Column field="date" header="Date" sortable></Column>
            <Column field="amount" header="Amount" sortable>
              <template #body="slotProps">
                {{ formatCurrency(slotProps.data.amount) }}
              </template>
            </Column>
            <Column field="status" header="Status" sortable>
              <template #body="slotProps">
                <Tag :value="slotProps.data.status.toLowerCase()" :severity="getOrderSeverity(slotProps.data)" />
              </template>
            </Column>
            <Column headerStyle="width:4rem">
              <template #body>
                <Button icon="pi pi-search" />
              </template>
            </Column>
          </DataTable>
        </div>
      </template>
    </DataTable>
    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useToast } from 'primevue/usetoast';

// Generazione di dati casuali
const products = ref(generateMockData());
const expandedRows = ref({});
const toast = useToast();

onMounted(() => {
  console.log('Mock data loaded:', products.value); // Verifica che i dati siano stati caricati correttamente
});

const onRowExpand = (event) => {
  toast.add({ severity: 'info', summary: 'Product Expanded', detail: event.data.name, life: 3000 });
};

const onRowCollapse = (event) => {
  toast.add({ severity: 'success', summary: 'Product Collapsed', detail: event.data.name, life: 3000 });
};

const expandAll = () => {
  expandedRows.value = products.value.reduce((acc, p) => (acc[p.id] = true) && acc, {});
};

const collapseAll = () => {
  expandedRows.value = null;
};

const formatCurrency = (value) => {
  return value.toLocaleString('en-US', { style: 'currency', currency: 'USD' });
};

const getSeverity = (product) => {
  switch (product.inventoryStatus) {
    case 'INSTOCK':
      return 'success';

    case 'LOWSTOCK':
      return 'warn';

    case 'OUTOFSTOCK':
      return 'danger';

    default:
      return null;
  }
};

const getOrderSeverity = (order) => {
  switch (order.status) {
    case 'DELIVERED':
      return 'success';

    case 'CANCELLED':
      return 'danger';

    case 'PENDING':
      return 'warn';

    case 'RETURNED':
      return 'info';

    default:
      return null;
  }
};

// Funzione per generare dati casuali per i prodotti
function generateMockData() {
  const mockProducts = [];
  const categories = ['Electronics', 'Apparel', 'Home', 'Toys', 'Sports'];
  const statuses = ['INSTOCK', 'LOWSTOCK', 'OUTOFSTOCK'];
  const ratings = [1, 2, 3, 4, 5];

  for (let i = 0; i < 10; i++) {
    const product = {
      id: i + 1,
      name: `Product ${i + 1}`,
      image: `image_${Math.floor(Math.random() * 10) + 1}.jpg`,
      price: (Math.random() * 100).toFixed(2),
      category: categories[Math.floor(Math.random() * categories.length)],
      inventoryStatus: statuses[Math.floor(Math.random() * statuses.length)],
      rating: ratings[Math.floor(Math.random() * ratings.length)],
      orders: generateMockOrders(i + 1)
    };
    mockProducts.push(product);
  }

  return mockProducts;
}

// Funzione per generare ordini casuali per ogni prodotto
function generateMockOrders(productId) {
  const mockOrders = [];
  const statuses = ['DELIVERED', 'CANCELLED', 'PENDING', 'RETURNED'];

  for (let i = 0; i < 3; i++) {
    mockOrders.push({
      id: `${productId}-${i + 1}`,
      customer: `Customer ${Math.floor(Math.random() * 10) + 1}`,
      date: new Date().toISOString().split('T')[0],
      amount: (Math.random() * 50).toFixed(2),
      status: statuses[Math.floor(Math.random() * statuses.length)]
    });
  }

  return mockOrders;
}
</script>
