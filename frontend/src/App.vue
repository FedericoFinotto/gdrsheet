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
import UpperBar from '@/components/UpperBar.vue'
import Popup from "./components/Popup.vue";
import usePopup from './function/usePopup'

const {
  isVisible,
  dynamicComp,
  dynamicProps,
  isClosable,
  closePopup
} = usePopup()
</script>

<style scoped>
/* layout a tutta altezza */
.layout {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* contenuto principale: parte sotto la UpperBar fissa */
.content {
  margin-top: var(--topbar-h); /* compensa la barra fissa */
  flex: 1 1 auto;
  min-height: 0; /* evita overflow nei figli flex */
  overflow: hidden; /* niente scroll qui */
  padding: 0.1rem;
}

/* wrapper della pagina: prende tutto lo spazio della content */
.page {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0; /* fondamentale per far funzionare overflow interno */
}
</style>
