<template>
  <header class="upperbar">
    <div class="title">
      <slot/>
    </div>

    <!-- hamburger a destra -->
    <button class="menu" @click="goHome" aria-label="Vai alla home">☰</button>
  </header>
</template>

<script setup lang="ts">
import {useRouter} from 'vue-router'

const router = useRouter()
const goHome = () => router.push({path: '/'})
</script>

<style scoped>
.upperbar {
  position: fixed;
  inset: 0 0 auto 0; /* top:0; left:0; right:0 */
  height: var(--topbar-h);
  display: flex;
  align-items: center;
  padding: 0 .75rem;
  background: var(--primary-color);
  border-bottom: 1px solid var(--border-color);
  z-index: 1000;
}

.title {
  font-weight: 600;
  margin-right: auto; /* spinge il bottone a destra */
}

.menu {
  background: transparent;
  border: 0;
  font-size: 1.25rem;
  cursor: pointer;
}
</style>
