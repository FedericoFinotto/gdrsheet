<script setup lang="ts">
import {useRouter} from 'vue-router'

const router = useRouter()

function vaiAllaScheda() {
  router.push('/scheda/1')
}
</script>

<template>
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">

  <div class="home">
    <button @click="vaiAllaScheda">Vai alla Scheda 1</button>
  </div>
</template>