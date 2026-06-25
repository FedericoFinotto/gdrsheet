<script setup lang="ts">
import {computed, nextTick, onBeforeUpdate, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {storeToRefs} from 'pinia';
import {useCharacterStore} from "../../../../../../stores/personaggio";

import Mobile_Cico_Info from './Mobile_Cico_1_Info.vue';
import Mobile_Cico_Abilita from './Mobile_Cico_2_Abilita.vue';
import Mobile_Cico_Items from './Mobile_Cico_3_Items.vue';
import Mobile_Cico_SpellBook from './Mobile_Cico_4_SpellBook.vue';
import Mobile_Cico_Attacchi from "./Mobile_Cico_6_Attacchi.vue";
import Mobile_Cico_Livelli from "./Mobile_Cico_7_Livelli.vue";
import Mobile_Cico_Soldi from "./Mobile_Cico_8_Soldi.vue";
import Mobile_Cico_Competenze from "./Mobile_Cico_9_Competenze.vue";

const route = useRoute();
const router = useRouter();
const idPersonaggio = Number(route.params.id);
if (isNaN(idPersonaggio)) throw new Error('Parametro id non valido');

const tabs = [
  {label: 'Info', comp: Mobile_Cico_Info},
  {label: 'Abilità', comp: Mobile_Cico_Abilita},
  {label: 'Inventario', comp: Mobile_Cico_Items},
  {label: 'Soldi', comp: Mobile_Cico_Soldi},
  {label: 'Incantesimi', comp: Mobile_Cico_SpellBook},
  {label: 'Attacchi', comp: Mobile_Cico_Attacchi},
  {label: 'Livelli', comp: Mobile_Cico_Livelli},
  {label: 'Competenze', comp: Mobile_Cico_Competenze}
];

// tab iniziale dalla query (?tab=N), così tornando indietro si riapre la stessa pagina
function tabDaQuery(): number {
  const n = Number(route.query.tab);
  return Number.isInteger(n) && n >= 0 && n < tabs.length ? n : 0;
}

const activeIndex = ref(tabDaQuery());
const loaded = ref(false);

const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);

onMounted(() => {
  characterStore.fetchCharacter(idPersonaggio).then(() => {
    loaded.value = true;
    scrollHeaderToActive();
  });
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

// auto-scroll + sync della query ogni volta che cambia tab
watch(activeIndex, (i) => {
  scrollHeaderToActive();
  router.replace({query: {...route.query, tab: i > 0 ? String(i) : undefined}});
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
  <div class="fullpage-loading" v-if="!loaded">
    <span class="loading-spinner"/>
  </div>

  <div class="character-sheet" v-else>
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

<style scoped>
.fullpage-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9fafb;
}


@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-spinner {
  display: inline-block;
  width: 2.5rem;
  height: 2.5rem;
  border: 3px solid #d1d5db;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin .8s linear infinite;
}
</style>
