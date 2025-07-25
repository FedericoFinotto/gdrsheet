<template>
  <SharedDataProvider>
    <div class="onMobile">
    <router-view/>

    </div>
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
.onMobile {
  margin-top: 4rem;
}
</style>
