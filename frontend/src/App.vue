<template>
  <div class="layout">
    <UpperBar/>
    <main class="content">
      <router-view class="page"/>
    </main>
  </div>

  <Popup
      v-model="isVisible"
      :dynamic-component="dynamicComp"
      :dynamic-props="dynamicProps"
      :closable="isClosable"
  />
</template>

<script setup lang="ts">
import Popup from "./components/Popup.vue";
import usePopup from './function/usePopup'
import UpperBar from "./components/UpperBar.vue";

const {
  isVisible,
  dynamicComp,
  dynamicProps,
  isClosable,
  closePopup
} = usePopup()
</script>

