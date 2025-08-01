<template>

  <div class="onMobile">
    <router-view/>
  </div>
  <UpperBar/>
  <HamburgerMenu/>
</template>

<script setup>
import UpperBar from './components/UpperBar.vue';
import HamburgerMenu from "./components/HamburgerMenu.vue";
</script>

<style>
.onMobile {
  margin-top: 4rem;
  height: calc(100vh - 3rem);
}
</style>
