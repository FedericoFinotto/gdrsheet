<template>
  <SharedDataProvider>
    <router-view/>
    <UpperBar/>
    <HamburgerMenu/>
  </SharedDataProvider>
</template>

<script setup>
import SharedDataProvider from './components/SharedDataProvider.vue';
import UpperBar from './components/UpperBar.vue';
import HamburgerMenu from "./components/HamburgerMenu.vue";
</script>

<style>
/* global styles */
</style>
