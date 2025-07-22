<template>
  <div class="bottom-bar">
    <!-- bottom navigation or status -->
    <button class="hamburger" @click="toggle">
      <i class="pi pi-bars"></i>
    </button>
  </div>
</template>

<script setup>
import {ref} from 'vue';

const open = ref(false);

function toggle() {
  open.value = !open.value;
  // open menu logic
}
</script>

<style scoped>
.bottom-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 3rem;
  background: black;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 -1px 4px rgba(0, 0, 0, 0.1);
}

.hamburger {
  position: fixed;
  top: 0.1rem;
  right: 0;
  border: none;
  background: black;
  color: white;
  padding: 0;
  width: 5rem;
  border-radius: 0;
  font-size: 2rem;
}
</style>
