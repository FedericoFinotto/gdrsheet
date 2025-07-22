<template>
  <slot/>
</template>

<script setup>
import {onMounted, provide, reactive} from 'vue';
import {getPersonaggioById} from "../service/PersonaggioService.ts";

const data = reactive({
  character: null,
  items: []
});

onMounted(() => {
  getPersonaggioById(1).then((res) => {
    data.character = res.data;
    console.log(data.character);
  })
});

provide('sharedData', data);
</script>
