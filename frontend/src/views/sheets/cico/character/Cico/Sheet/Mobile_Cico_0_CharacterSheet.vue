<script setup lang="ts">
import {computed, nextTick, onBeforeUpdate, onMounted, ref, watch} from 'vue';
import {useRoute} from 'vue-router';
import {storeToRefs} from 'pinia';
import {useCharacterStore} from "../../../../../../stores/personaggio";

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

/* --- refs per autoscroll header --- */
const headerEl = ref<HTMLElement | null>(null);
const tabBtns = ref<HTMLButtonElement[]>([]);
onBeforeUpdate(() => {
  tabBtns.value = [];
}); // reset tra i render

function scrollHeaderToActive() {
  nextTick(() => {
    const btn = tabBtns.value[activeIndex.value];
    if (!btn || !headerEl.value) return;
    // centra il bottone attivo nella viewport orizzontale della header
    btn.scrollIntoView({behavior: 'smooth', inline: 'center', block: 'nearest'});
  });
}

/* --- Transizione direzionale --- */
const direction = ref<'none' | 'forward' | 'back'>('none');
const transitionName = computed(() => {
  if (direction.value === 'forward') return 'slide-left';
  if (direction.value === 'back') return 'slide-right';
  return 'fade';
});

// click su tab: setta direzione prima di cambiare indice
function setActive(i: number) {
  if (i === activeIndex.value) return;
  direction.value = i > activeIndex.value ? 'forward' : 'back';
  activeIndex.value = i;
}

// auto-scroll ogni volta che cambia tab
watch(activeIndex, () => {
  scrollHeaderToActive();
});

/* --- Swipe handling (solo orizzontale dentro la tab-content) --- */
let startX = 0, startY = 0, moved = false;
const THRESH_X = 60;
const THRESH_Y = 40;
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
  if (!moved && dx > 10 && dy < 10) {
    e.preventDefault();
  }
  moved = true;
};
const onTouchEnd = (e: TouchEvent) => {
  const t = e.changedTouches[0];
  const dx = t.screenX - startX;
  const dy = t.screenY - startY;
  if (Math.abs(dx) >= THRESH_X && Math.abs(dy) <= THRESH_Y) {
    if (dx < 0 && activeIndex.value < tabs.length - 1) {
      direction.value = 'forward';
      activeIndex.value++;
    } else if (dx > 0 && activeIndex.value > 0) {
      direction.value = 'back';
      activeIndex.value--;
    }
  }
};
</script>

<template>
  <div class="character-sheet" v-if="loaded">
    <!-- Header tab -->
    <div class="tab-header-wrapper" ref="headerEl">
      <div class="tab-header">
        <button
            v-for="(tab, i) in tabs"
            :key="i"
            :class="{ active: activeIndex === i }"
            @click="setActive(i)"
            :ref="el => el && (tabBtns[i] = el as HTMLButtonElement)"
        >
          {{ tab.label }}
        </button>
      </div>
    </div>

    <!-- Contenuto con transizione -->
    <div
        class="tab-content"
        @touchstart.passive="onTouchStart"
        @touchmove="onTouchMove"
        @touchend="onTouchEnd"
    >
      <Transition :name="transitionName" mode="out-in">
        <component :is="tabs[activeIndex].comp" :key="activeIndex" :id-personaggio="idPersonaggio"/>
      </Transition>
    </div>
  </div>
</template>

