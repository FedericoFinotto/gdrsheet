<script setup lang="ts">
import {useRouter} from 'vue-router'

const router = useRouter()
const goHome = () => router.push({path: '/'})
</script>

<template>
  <header class="upperbar">
    <div class="title">
      <slot/>
    </div>

    <!-- hamburger a destra -->
    <button class="menu" @click="goHome" aria-label="Vai alla home">☰</button>
  </header>
</template>
