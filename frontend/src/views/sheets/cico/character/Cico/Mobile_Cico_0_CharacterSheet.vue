<script setup lang="ts">
import {onMounted, ref} from 'vue';
import {useRoute} from 'vue-router';
import {storeToRefs} from 'pinia';
import {useCharacterStore} from "../../../../../stores/personaggio";

import Mobile_Cico_Info from './Mobile_Cico_1_Info.vue';
import Mobile_Cico_Abilita from './Mobile_Cico_2_Abilita.vue';
import Mobile_Cico_Items from './Mobile_Cico_3_Items.vue';
import Mobile_Cico_SpellBook from './Mobile_Cico_4_SpellBook.vue';
import Mobile_Cico_Talenti from "./Mobile_Cico_5_Talenti.vue";
import Mobile_Cico_Attacchi from "./Mobile_Cico_6_Attacchi.vue";

const route = useRoute();
const idPersonaggio = Number(route.params.id);
if (isNaN(idPersonaggio)) throw new Error('Parametro id non valido');

const tabs = [
  {label: 'Info', comp: Mobile_Cico_Info},
  {label: 'Abilità', comp: Mobile_Cico_Abilita},
  {label: 'Inventario', comp: Mobile_Cico_Items},
  {label: 'Incantesimi', comp: Mobile_Cico_SpellBook},
  {label: 'Talenti', comp: Mobile_Cico_Talenti},
  {label: 'Attacchi', comp: Mobile_Cico_Attacchi},
];

const activeIndex = ref(0);
const loaded = ref(false);

const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);

onMounted(() => {
  characterStore.fetchCharacter(idPersonaggio).then(() => loaded.value = true);
});

/* --- Swipe handling (solo orizzontale dentro la tab-content) --- */
let startX = 0, startY = 0, moved = false;
const THRESH_X = 60;   // px minimi per considerare swipe
const THRESH_Y = 40;   // filtro: se muovi troppo in verticale, non è swipe orizz.
const onTouchStart = (e: TouchEvent) => {
  const t = e.changedTouches[0];
  startX = t.screenX;
  startY = t.screenY;
  moved = false;
};
const onTouchMove = (e: TouchEvent) => {
  const t = e.changedTouches[0];
  const dx = Math.abs(t.screenX - startX);
  const dy = Math.abs(t.screenY - startY);
  // se sta iniziando un gesto orizzontale netto, preveniamo il pan-x che darebbe scatti
  if (!moved && dx > 10 && dy < 10) {
    e.preventDefault();    // non blocca lo scroll verticale (dy piccolo)
  }
  moved = true;
};
const onTouchEnd = (e: TouchEvent) => {
  const t = e.changedTouches[0];
  const dx = t.screenX - startX;
  const dy = t.screenY - startY;
  // cambia tab solo se swipe orizzontale "pulito"
  if (Math.abs(dx) >= THRESH_X && Math.abs(dy) <= THRESH_Y) {
    if (dx < 0 && activeIndex.value < tabs.length - 1) {
      activeIndex.value++;
    } else if (dx > 0 && activeIndex.value > 0) {
      activeIndex.value--;
    }
  }
};
</script>

<template>
  <div class="character-sheet" v-if="loaded">
    <!-- Header tab: fisso sotto UpperBar, scrollabile orizzontalmente -->
    <div class="tab-header-wrapper">
      <div class="tab-header">
        <button
            v-for="(tab, i) in tabs"
            :key="i"
            :class="{ active: activeIndex === i }"
            @click="activeIndex = i"
        >
          {{ tab.label }}
        </button>
      </div>
    </div>

    <!-- Contenuto: scroll verticale, NO scroll orizzontale, swipe cambia tab -->
    <div
        class="tab-content"
        @touchstart.passive="onTouchStart"
        @touchmove="onTouchMove"
        @touchend="onTouchEnd"
    >
      <component :is="tabs[activeIndex].comp" :id-personaggio="idPersonaggio"/>
    </div>
  </div>
</template>

<style scoped>
.character-sheet {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0; /* evita gonfiaggio */
}

/* header fisso nel componente, scorrevole in orizzontale */
.tab-header-wrapper {
  flex: 0 0 auto;
  position: sticky; /* resta sotto la UpperBar */
  top: 0; /* App.vue ha già margin-top della topbar */
  background: var(--primary-color);
  border-bottom: 1px solid var(--border-color);
  z-index: 2;
  overflow-x: auto; /* 👈 scroll orizzontale se non ci stanno */
  -webkit-overflow-scrolling: touch;
}

.tab-header {
  display: flex;
  flex-wrap: nowrap;
  min-width: max-content;
}

.tab-header button {
  flex: 0 0 auto;
  padding: 1rem;
  border: none;
  background: transparent;
  font-weight: 600;
  cursor: pointer;
  color: #000;
  white-space: nowrap; /* niente a capo */
}

.tab-header button.active {
  background: var(--secondary-color);
}

/* contenuto tab: unico scroll verticale, orizzontale bloccato */
.tab-content {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto; /* ✅ scroll solo verticale */
  overflow-x: hidden; /* 🔒 blocca orizzontale */
  -webkit-overflow-scrolling: touch;
  overscroll-behavior-y: contain; /* riduce rimbalzi su mobile */
}
</style>
