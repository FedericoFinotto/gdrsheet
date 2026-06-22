<script setup lang="ts">
import {ref} from 'vue'

const props = defineProps<{
  title?: string
  defaultOpen?: boolean
  loading?: boolean
  // alternativa a slot: componenti da rendere
  summaryIs?: any
  summaryProps?: Record<string, any>
  contentIs?: any
  contentProps?: Record<string, any>
}>()

const open = ref(!!props.defaultOpen)

function toggle() {
  if (props.loading) return
  open.value = !open.value
}

// per a11y id unici minimali
const panelId = `tabexp-${Math.random().toString(36).slice(2)}`
</script>

<template>
  <section class="fold">
    <button type="button" class="fold-head" @click="toggle"
            :class="{ 'fold-head--loading': loading }"
            :aria-expanded="open ? 'true' : 'false'"
            :aria-controls="panelId">
      <span class="fold-title">{{ title }}</span>
      <span class="fold-summary">
        <span v-if="loading" class="spinner" aria-label="Caricamento…"/>
        <slot v-else name="summary">
          <component v-if="summaryIs" :is="summaryIs" v-bind="summaryProps"/>
        </slot>
      </span>
      <span class="chev" :class="{ open: open && !loading }">▸</span>
    </button>

    <div class="fold-body" :id="panelId" v-show="open && !loading">
      <slot name="content">
        <component v-if="contentIs" :is="contentIs" v-bind="contentProps"/>
      </slot>
    </div>
  </section>
</template>

<style scoped>
.fold {
  border: 1px solid #e5e7eb;
  border-radius: .5rem;
  background: #fff;
}

.fold + .fold {
  margin-top: .25rem;
}

.fold-head {
  width: 100%;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: .5rem;
  padding: .5rem .75rem;
  background: #f9fafb;
  border: 0;
  border-bottom: 1px solid #e5e7eb;
  cursor: pointer;
  text-align: left;
}

.fold-head--loading {
  cursor: default;
  opacity: .7;
}

.fold-title {
  font-weight: 600;
}

.fold-summary {
  color: #374151;
  opacity: .8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chev {
  transition: transform .15s ease;
  transform: rotate(0deg);
}

.chev.open {
  transform: rotate(90deg);
}

.fold-body {
  padding: .6rem .75rem;
}

/* spinner */
@keyframes spin {
  to { transform: rotate(360deg); }
}

.spinner {
  display: inline-block;
  width: .9rem;
  height: .9rem;
  border: 2px solid #d1d5db;
  border-top-color: #6b7280;
  border-radius: 50%;
  animation: spin .7s linear infinite;
  vertical-align: middle;
}
</style>
