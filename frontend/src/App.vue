<template>
  <div class="onMobile" :style="{
      '--primary-color': PRIMARY_COLOR,
      '--secondary-color': SECONDARY_COLOR,
      '--tertiary-color': TERTIARY_COLOR,
      '--accent-color': ACCENT_COLOR,
      '--border-color': BORDER_COLOR
    }">
    <router-view/>
  </div>
  <UpperBar/>
  <HamburgerMenu/>
</template>

<script setup>
import UpperBar from './components/UpperBar.vue';
import HamburgerMenu from "./components/HamburgerMenu.vue";
import {ACCENT_COLOR, BORDER_COLOR, PRIMARY_COLOR, SECONDARY_COLOR, TERTIARY_COLOR} from "./function/Constants";
</script>

<style>
.onMobile {
  margin-top: 4rem;
  height: calc(100vh - 3rem);
  background-color: var(--tertiary-color);
}
</style>
