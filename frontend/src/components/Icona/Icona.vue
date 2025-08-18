<script setup lang="ts">
import {computed, useAttrs} from 'vue'
import {type IconDef, type IconKey, ICONS} from './ListaIcone'

const emit = defineEmits<{ (e: 'click', ev: MouseEvent): void }>()

const props = withDefaults(defineProps<{
  name: IconKey
  title?: string
  // Solo per FA:
  spin?: boolean
  fixedWidth?: boolean
  size?: 'xs' | 'sm' | 'lg' | 'xl' | '2x' | '3x' | '4x' | '5x' | '6x' | '7x' | '8x' | '9x' | '10x'
}>(), {spin: false, fixedWidth: false})

const def = computed<IconDef>(() => ICONS[props.name])

// ⬇️ Prendi gli attributi passati al componente ma rimuovi i listener (onClick, onMouseup, ecc.)
const attrs = useAttrs()
const passthrough = computed(() => {
  const out: Record<string, unknown> = {}
  for (const [k, v] of Object.entries(attrs)) {
    if (!k.startsWith('on')) out[k] = v
  }
  return out
})

const faClass = computed(() => {
  if (def.value.kind !== 'fa') return ''
  const parts: string[] = [def.value.classes]
  if (props.spin) parts.push('fa-spin')
  if (props.fixedWidth) parts.push('fa-fw')
  if (props.size) parts.push(`fa-${props.size}`)
  return parts.join(' ')
})
</script>

<template>
  <!-- Font Awesome -->
  <i v-if="def.kind === 'fa'"
     :class="faClass"
     :title="title"
     v-bind="passthrough"
     @click="emit('click', $event)"
     aria-hidden="true"/>

  <!-- Icona da /public -->
  <img v-else-if="def.kind === 'img'"
       :src="def.src"
       :alt="def.alt || title || name"
       :title="title"
       v-bind="passthrough"
       @click="emit('click', $event)"
       style="height: 100%"/>
</template>
