<script setup lang="ts">
import {computed} from 'vue'
import {Soldi, totaleInMo} from '../models/dto/Party'

const props = defineProps<{
  soldi: Soldi
  compatto?: boolean
}>()

const totMo = computed(() => {
  const n = totaleInMo(props.soldi)
  return n.toLocaleString('it-IT', {maximumFractionDigits: 2})
})
</script>

<template>
  <div class="soldi" :class="{ compatto }">
    <span class="coin mp" title="Monete di Platino">{{ soldi.mp }} <small>MP</small></span>
    <span class="coin mo" title="Monete d'Oro">{{ soldi.mo }} <small>MO</small></span>
    <span class="coin ma" title="Monete d'Argento">{{ soldi.ma }} <small>MA</small></span>
    <span class="coin mr" title="Monete di Rame">{{ soldi.mr }} <small>MR</small></span>
    <span class="tot" title="Controvalore totale in monete d'oro">≈ {{ totMo }} MO</span>
  </div>
</template>

<style scoped>
.soldi {
  display: inline-flex;
  flex-wrap: wrap;
  gap: .35rem;
  align-items: center;
}

.coin {
  padding: .15rem .5rem;
  border-radius: .5rem;
  font-variant-numeric: tabular-nums;
  border: 1px solid transparent;
  font-size: .85rem;
}

.coin small { opacity: .75; font-size: .7em; }

.coin.mp { background: #ecfeff; color: #155e75; border-color: #a5f3fc; }
.coin.mo { background: #fef9c3; color: #854d0e; border-color: #fde68a; }
.coin.ma { background: #f1f5f9; color: #334155; border-color: #cbd5e1; }
.coin.mr { background: #ffedd5; color: #9a3412; border-color: #fed7aa; }

.tot {
  margin-left: .25rem;
  font-size: .85rem;
  font-weight: 600;
  opacity: .8;
}

.compatto .coin { padding: .05rem .35rem; font-size: .78rem; }
.compatto .tot { font-size: .78rem; }
</style>
