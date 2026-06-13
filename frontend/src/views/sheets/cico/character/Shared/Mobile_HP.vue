<script setup lang="ts">
import {computed} from 'vue'
import {useHp} from '../../../../../function/useHp'
import usePopup from '../../../../../function/usePopup'
import Mobile_HP_Popup from './Mobile_HP_Popup.vue'
import Icona from '../../../../../components/Icona/Icona.vue'

const props = defineProps<{ idPersonaggio: number }>()
const {openPopup} = usePopup()

const {hp, hpMax, pfTemp, barriere, barriereTotal, totalMax, remaining, modifyHp} = useHp(props.idPersonaggio)

const pct = (v: number) => (v / Math.max(1, totalMax.value)) * 100

// ordine di consumo (da destra): barriere → temp → vita.
// quindi sulla barra: vita (verde) | temp (arancio) | barriere (azzurro, separate) | grigio
const segments = computed(() => {
  const segs: Array<{ w: number; cls: string; sep: boolean }> = []
  if (hp.value > 0) segs.push({w: pct(hp.value), cls: 'seg-hp', sep: false})
  if (pfTemp.value > 0) segs.push({w: pct(pfTemp.value), cls: 'seg-temp', sep: true})
  barriere.value.forEach((b) => {
    if (b.current > 0) segs.push({w: pct(b.current), cls: 'seg-barr', sep: true})
  })
  const vuoto = totalMax.value - remaining.value
  if (vuoto > 0) segs.push({w: pct(vuoto), cls: 'seg-empty', sep: false})
  return segs
})

// dettaglio: vita [+ temp] [+ barriere]
const breakdown = computed(() => {
  const parts = [String(hp.value)]
  if (pfTemp.value > 0) parts.push(String(pfTemp.value))
  if (barriereTotal.value > 0) parts.push(String(barriereTotal.value))
  return parts.join(' + ')
})
const hasBreakdown = computed(() => pfTemp.value > 0 || barriereTotal.value > 0)

function apriPopup() {
  openPopup(Mobile_HP_Popup, {idPersonaggio: props.idPersonaggio}, {closable: true, autoClose: 0})
}
</script>

<template>
  <div class="hp-bar" @click="apriPopup" title="Gestisci HP e barriere">
    <div class="hp-track">
      <div v-for="(s, i) in segments" :key="i" class="hp-seg" :class="[s.cls, { sep: s.sep }]"
           :style="{ width: s.w + '%' }"/>
    </div>

    <div class="hp-overlay">
      <Icona name="SUB" class="bar-btn" @click.stop="modifyHp(-1)"/>
      <button class="hp-mod" @click.stop="modifyHp(-50)">-50</button>
      <div class="hp-text">
        {{ remaining }} / {{ totalMax }}<span v-if="hasBreakdown" class="bd">&nbsp;({{ breakdown }})</span>
      </div>
      <button class="hp-mod" @click.stop="modifyHp(+50)">+50</button>
      <Icona name="ADD" class="bar-btn" @click.stop="modifyHp(+1)"/>
    </div>
  </div>
</template>

<style scoped>
.hp-bar {
  position: relative;
  width: 100%;
  min-width: 0;
  height: 2rem;
  border: 1px solid #94a3b8;
  border-radius: 4px;
  overflow: hidden;
  background: #e5e7eb;
  cursor: pointer;
  padding: 0;
}

.hp-overlay {
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  align-items: center;
  gap: .3rem;
  padding: 0 .3rem;
}

.hp-mod {
  background: transparent;
  border: 0;
  font-weight: 700;
  font-size: .8rem;
  cursor: pointer;
  color: #334155;
  flex-shrink: 0;
}

.hp-track {
  position: absolute;
  inset: 0;
  display: flex;
}

.hp-seg {
  height: 100%;
}

.hp-seg.seg-hp {
  background: #22c55e;
}

.hp-seg.seg-temp {
  background: #fdba74;
}

/* arancione pastello */
.hp-seg.seg-barr {
  background: #93c5fd;
}

/* azzurro pastello */
.hp-seg.seg-empty {
  background: transparent;
}

/* separatore evidente tra segmenti contigui (in particolare tra barriere) */
.hp-seg.sep {
  box-shadow: inset 2px 0 0 #ffffff;
}

.hp-text {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: .85rem;
  color: #0f172a;
  text-shadow: 0 0 3px rgba(255, 255, 255, .9), 0 0 3px rgba(255, 255, 255, .9);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.extra {
  margin-left: 1px;
}

.extra.temp {
  color: #0f766e;
}

.extra.barr {
  color: #1d4ed8;
}

.bar-btn {
  flex-shrink: 0;
  cursor: pointer;
  font-size: .9rem;
  color: #475569;
}
</style>
