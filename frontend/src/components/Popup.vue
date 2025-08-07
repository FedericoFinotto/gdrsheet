<template>
  <teleport to="body">
    <transition name="fade">
      <div v-if="modelValue" class="popup-overlay" @click.self="closable && close()">
        <div class="popup-content">
          <!-- pulsante X in alto a destra, solo se closable -->
          <button
              v-if="closable"
              class="popup-close-btn"
              @click="close()"
              aria-label="Close"
          >×
          </button>

          <!-- qui renderizziamo il componente dinamico -->
          <component
              :is="dynamicComponent"
              v-bind="dynamicProps"
              @close="close()"/>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import {defineEmits, defineProps} from 'vue'

const props = defineProps({
  modelValue: Boolean,
  dynamicComponent: [Object, String],
  dynamicProps: {
    type: Object,
    default: () => ({})
  },
  closable: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue'])

function close() {
  emit('update:modelValue', false)
}
</script>

<style>
.popup-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.popup-content {
  position: relative;
  background: white;
  border-radius: .5rem;
  padding: 1.5rem;
  max-width: 90vw;
  max-height: 90vh;
  overflow: auto;
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

.fade-enter-active, .fade-leave-active {
  transition: opacity .2s;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
