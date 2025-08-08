<template>
  <div class="layout">
    <UpperBar/>
    <main class="content">
      <router-view class="page"/>
    </main>
  </div>
</template>

<script setup lang="ts">
import UpperBar from '@/components/UpperBar.vue'
</script>

<style scoped>
/* layout a tutta altezza */
.layout {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* compensa la topbar fixed */
.tabs {

  flex: 0 0 auto;
  display: flex;
  gap: .5rem;
  align-items: center;
  padding: .5rem .75rem;
  border-bottom: 1px solid var(--border-color);
  background: var(--surface);
}

/* area contenuti: unica zona scrollabile */
.content {
  margin-top: var(--topbar-h);
  max-height: calc(100% - 2 * var(--topbar-h));
  padding: 0;
  flex: 1 1 auto;
  min-height: 0; /* evita overflow */
  overflow: auto; /* scroll solo qui */
}

.page {
  padding: .2rem;
  box-sizing: border-box;
}
</style>
