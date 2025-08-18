<script setup lang="ts">
import {useRouter} from 'vue-router'
import Icona from "./Icona/Icona.vue";

const router = useRouter()
const goHome = () => router.push({path: '/'})
</script>

<template>
  <header class="upperbar">
    <div class="title">
      <slot/>
    </div>

    <!-- hamburger a destra -->
    <Icona name="HAMBURGER" @click="goHome"></Icona>
  </header>
</template>
