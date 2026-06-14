<script setup lang="ts">
import {defineAsyncComponent} from 'vue'
import {useRouter} from 'vue-router'
import Icona from "./Icona/Icona.vue";
import usePopup from "../function/usePopup";

// caricato on-demand: porta con sé three.js, lo teniamo fuori dal bundle principale
const DiceRollerPopup = defineAsyncComponent(() => import('./DiceRollerPopup.vue'))

const router = useRouter()
const {openPopup} = usePopup()
const goHome = () => router.push({path: '/'})
const goCompendio = () => router.push({path: '/compendio'})
const apriDadi = () => openPopup(DiceRollerPopup, {}, {closable: true, autoClose: 0})
</script>

<template>
  <header class="upperbar">
    <div class="title">
      <slot/>
    </div>

    <!-- icone a destra -->
    <div class="bar-icons">
      <Icona name="DADO" title="Lancia un dado" @click="apriDadi"></Icona>
      <Icona name="COMPENDIO" @click="goCompendio"></Icona>
      <Icona name="HAMBURGER" @click="goHome"></Icona>
    </div>
  </header>
</template>

<style scoped>
.bar-icons {
  display: inline-flex;
  align-items: center;
  gap: 1rem;
}
</style>
