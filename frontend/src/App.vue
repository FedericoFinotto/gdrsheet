<template>
  <div
      class="onMobile"
      :style="{
      '--primary-color': PRIMARY_COLOR,
      '--secondary-color': SECONDARY_COLOR,
      '--tertiary-color': TERTIARY_COLOR,
      '--accent-color': ACCENT_COLOR,
      '--border-color': BORDER_COLOR
    }"
  >
    <router-view/>
  </div>
  <UpperBar/>
  <HamburgerMenu/>

  <!-- Popup dinamico -->
  <Popup
      v-model="isVisible"
      :dynamic-component="dynamicComp"
      :dynamic-props="dynamicProps"
      :closable="isClosable"
  />
</template>

<script setup>
import UpperBar from './components/UpperBar.vue'
import HamburgerMenu from './components/HamburgerMenu.vue'
import Popup from './components/Popup.vue'
import usePopup from './function/usePopup'

import {ACCENT_COLOR, BORDER_COLOR, PRIMARY_COLOR, SECONDARY_COLOR, TERTIARY_COLOR} from './function/Constants'

// Prendi SOLO le proprietà di stato e la close dal composable
const {
  isVisible,
  dynamicComp,
  dynamicProps,
  isClosable,
  closePopup
} = usePopup()
</script>

<style>
.onMobile {
  margin-top: 4rem;
  height: calc(100vh - 3rem);
  background-color: var(--tertiary-color);
}
</style>
