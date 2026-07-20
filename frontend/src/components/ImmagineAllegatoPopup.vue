<script setup lang="ts">
import {onUnmounted, ref} from 'vue'

const props = defineProps<{ src: string }>()

// il blob url è creato al momento del download: va rilasciato quando il popup si chiude
onUnmounted(() => URL.revokeObjectURL(props.src))

const scale = ref(1)
const MIN = 1
const MAX = 4

function onWheel(e: WheelEvent) {
  e.preventDefault()
  const delta = e.deltaY > 0 ? -0.2 : 0.2
  scale.value = Math.min(MAX, Math.max(MIN, scale.value + delta))
}

function onClick() {
  scale.value = scale.value > MIN ? MIN : 2.5
}
</script>

<template>
  <div class="img-popup" @wheel="onWheel">
    <img :src="src" alt="Allegato" :class="{zoomed: scale > MIN}"
         :style="{transform: `scale(${scale})`}" @click="onClick"/>
  </div>
</template>

<style scoped>
.img-popup {
  max-width: 80vw;
  max-height: 80vh;
  overflow: auto;
  display: flex;
}
.img-popup img {
  max-width: min(80vw, 60rem);
  max-height: 80vh;
  border-radius: .5rem;
  display: block;
  cursor: zoom-in;
  transition: transform .15s ease;
  transform-origin: center center;
}
.img-popup img.zoomed { cursor: zoom-out; }
</style>
