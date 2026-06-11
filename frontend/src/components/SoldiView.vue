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

// mostra solo i tagli con valore > 0
const MONETE: Array<{ key: keyof Soldi; sigla: string; cls: string; titolo: string }> = [
  {key: 'mp', sigla: 'MP', cls: 'mp', titolo: 'Monete di Platino'},
  {key: 'mo', sigla: 'MO', cls: 'mo', titolo: "Monete d'Oro"},
  {key: 'ma', sigla: 'MA', cls: 'ma', titolo: "Monete d'Argento"},
  {key: 'mr', sigla: 'MR', cls: 'mr', titolo: 'Monete di Rame'},
]
const moneteVisibili = computed(() => MONETE.filter(m => (props.soldi[m.key] ?? 0) > 0))
</script>

<template>
  <div class="soldi" :class="{ compatto }">
    <span v-for="m in moneteVisibili" :key="m.key" class="coin" :class="m.cls" :title="m.titolo">
      {{ soldi[m.key] }} <small>{{ m.sigla }}</small>
    </span>
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
