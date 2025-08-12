<template>
  <teleport to="body">
    <transition name="fade">
      <div v-if="modelValue" class="popup-overlay" @click.self="closable && close()">
        <div
            class="popup-content"
            :class="[{ 'is-fullscreen': size === 'fullscreen' }, contentClass]"
            :style="contentStyle"
        >
          <button
              v-if="closable"
              class="popup-close-btn"
              @click="close()"
              aria-label="Close"
          >×</button>

          <!-- componente dinamico -->
          <component
              :is="dynamicComponent"
              v-bind="dynamicProps"
              @close="close()"
          />
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import {computed, defineEmits, defineProps} from 'vue';

const props = defineProps({
  modelValue: Boolean,
  dynamicComponent: [Object, String],
  dynamicProps: { type: Object, default: () => ({}) },
  closable: { type: Boolean, default: true },

  /** ---- NUOVO: controllo dimensioni ---- */
  size: { type: String, default: 'md' }, // sm | md | lg | xl | fullscreen
  width: [String, Number],
  height: [String, Number],
  maxWidth: { type: [String, Number], default: '90vw' },
  maxHeight: { type: [String, Number], default: '90vh' },
  contentClass: { type: [String, Array, Object], default: '' },
});

const emit = defineEmits(['update:modelValue']);

function close() {
  emit('update:modelValue', false);
}

const toCssSize = (v) => (v == null ? undefined : (typeof v === 'number' ? `${v}px` : v));

const sizePresets = {
  sm: { width: '360px', maxWidth: '95vw' },
  md: { width: '640px', maxWidth: '95vw' },
  lg: { width: '920px', maxWidth: '95vw' },
  xl: { width: '1200px', maxWidth: '98vw' },
  fullscreen: { width: '100vw', height: '100vh', maxWidth: '100vw', maxHeight: '100vh' },
};

const contentStyle = computed(() => {
  const preset = sizePresets[props.size] || {};
  return {
    width: toCssSize(props.width ?? preset.width),
    height: toCssSize(props.height ?? preset.height),
    maxWidth: toCssSize(props.maxWidth ?? preset.maxWidth),
    maxHeight: toCssSize(props.maxHeight ?? preset.maxHeight),
  };
});
</script>

<style>
.popup-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, .5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

/* RIMOSSE width/height fisse: ora arrivano da :style */
.popup-content {
  position: relative;
  background: white;
  border-radius: .5rem;
  padding: 1.5rem;
  overflow: auto; /* il contenuto scrolla dentro il popup */
}

/* Fullscreen: niente raggio/padding per massimizzare spazio (puoi cambiare) */
.popup-content.is-fullscreen {
  border-radius: 0;
  padding: 1rem;
}

/* pulsante X */
.popup-close-btn {
  position: absolute;
  top: .5rem;
  right: .5rem;
  background: transparent;
  border: none;
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
}

.fade-enter-active, .fade-leave-active { transition: opacity .2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
