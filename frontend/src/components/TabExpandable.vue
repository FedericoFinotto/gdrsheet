<script setup lang="ts">
import {ref} from 'vue'

const props = defineProps<{
  title?: string
  defaultOpen?: boolean
  // alternativa a slot: componenti da rendere
  summaryIs?: any
  summaryProps?: Record<string, any>
  contentIs?: any
  contentProps?: Record<string, any>
}>()

const open = ref(!!props.defaultOpen)

function toggle() {
  open.value = !open.value
}

// per a11y id unici minimali
const panelId = `tabexp-${Math.random().toString(36).slice(2)}`
</script>

<template>
  <section class="fold">
    <button type="button" class="fold-head" @click="toggle"
            :aria-expanded="open ? 'true' : 'false'"
            :aria-controls="panelId">
      <span class="fold-title">{{ title }}</span>
      <span class="fold-summary">
        <slot name="summary">
          <component v-if="summaryIs" :is="summaryIs" v-bind="summaryProps"/>
        </slot>
      </span>
      <span class="chev" :class="{ open }">▸</span>
    </button>

    <div class="fold-body" :id="panelId" v-show="open">
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
</style>
