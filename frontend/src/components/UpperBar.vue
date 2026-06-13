<script setup lang="ts">
import {useRouter} from 'vue-router'
import Icona from "./Icona/Icona.vue";

const router = useRouter()
const goHome = () => router.push({path: '/'})
const goCompendio = () => router.push({path: '/compendio'})
</script>

<template>
  <header class="upperbar">
    <div class="title">
      <slot/>
    </div>

    <!-- icone a destra -->
    <div class="bar-icons">
      <Icona name="COMPENDIO" @click="goCompendio"></Icona>
      <Icona name="HAMBURGER" @click="goHome"></Icona>
    </div>
  </header>
</template>

<style scoped>
.bar-icons {
  display: inline-flex;
  align-items: center;
  gap: 1rem;
}
</style>
