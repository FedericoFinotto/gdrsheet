<script setup lang="ts">
import {ref} from 'vue'
import {useHp} from '../../../../../function/useHp'

const props = defineProps<{ idPersonaggio: number }>()

const {
  hp, hpMax, pfTemp, barriere, barriereTotal,
  modifyHp, modifyTemp, setTemp,
  modifyBarriera, resetBarriera, distruggiBarriera,
} = useHp(props.idPersonaggio)

// input rapido per danno/cura
const importo = ref<number | null>(null)

function applica(segno: 1 | -1) {
  const v = Math.abs(Math.floor(Number(importo.value) || 0))
  if (v <= 0) return
  modifyHp(segno * v)
  importo.value = null
}

const tempImporto = ref<number | null>(null)

function applicaTemp(segno: 1 | -1) {
  const v = Math.abs(Math.floor(Number(tempImporto.value) || 0))
  if (v <= 0) return
  modifyTemp(segno * v)
  tempImporto.value = null
}
</script>

<template>
  <div class="hp-popup">
    <h2>Punti Ferita</h2>

    <!-- riepilogo -->
    <div class="riepilogo">
      <div class="big">
        <span class="cur">{{ hp }}</span><span class="max">/ {{ hpMax }}</span>
      </div>
      <div class="extra">
        <span v-if="pfTemp > 0" class="pill temp">+{{ pfTemp }} temp</span>
        <span v-if="barriereTotal > 0" class="pill barr">+{{ barriereTotal }} barriere</span>
      </div>
    </div>

    <!-- danno / cura -->
    <section class="block">
      <span class="lbl">Danno / Cura</span>
      <div class="quick">
        <button class="btn dmg" @click="modifyHp(-10)">−10</button>
        <button class="btn dmg" @click="modifyHp(-1)">−1</button>
        <input v-model.number="importo" type="number" min="0" inputmode="numeric" placeholder="valore"/>
        <button class="btn heal" @click="modifyHp(+1)">+1</button>
        <button class="btn heal" @click="modifyHp(+10)">+10</button>
      </div>
      <div v-if="importo" class="apply">
        <button class="btn dmg wide" @click="applica(-1)">Applica danno −{{ importo }}</button>
        <button class="btn heal wide" @click="applica(+1)">Cura +{{ importo }}</button>
      </div>
    </section>

    <!-- HP temporanei -->
    <section class="block">
      <span class="lbl">HP temporanei <b class="temp-val">{{ pfTemp }}</b></span>
      <div class="quick">
        <button class="btn" @click="modifyTemp(-5)">−5</button>
        <button class="btn" @click="modifyTemp(-1)">−1</button>
        <input v-model.number="tempImporto" type="number" min="0" inputmode="numeric" placeholder="aggiungi"/>
        <button class="btn" @click="modifyTemp(+1)">+1</button>
        <button class="btn" @click="modifyTemp(+5)">+5</button>
      </div>
      <div v-if="tempImporto" class="apply">
        <button class="btn ghost wide" @click="applicaTemp(-1)">− {{ tempImporto }}</button>
        <button class="btn primary wide" @click="applicaTemp(+1)">+ {{ tempImporto }} temp</button>
      </div>
    </section>

    <!-- Barriere -->
    <section class="block">
      <span class="lbl">Barriere</span>
      <div v-if="!barriere.length" class="empty">Nessuna barriera attiva.</div>

      <div v-for="b in barriere" :key="b.id" class="barriera">
        <div class="barr-head">
          <span class="nome">{{ b.nome }}</span>
          <span class="val">{{ b.current }} / {{ b.max }}</span>
        </div>
        <div class="barr-track">
          <div class="barr-fill" :style="{ width: (b.max > 0 ? (b.current / b.max) * 100 : 0) + '%' }"/>
        </div>
        <div class="barr-actions">
          <button class="btn" :disabled="b.current <= 0" @click="modifyBarriera(b.id, -1)">−1</button>
          <button class="btn" :disabled="b.current >= b.max" @click="modifyBarriera(b.id, +1)">+1</button>
          <button class="btn ghost" :disabled="b.current >= b.max" @click="resetBarriera(b.id)">Reset</button>
          <button class="btn danger" :disabled="b.current <= 0" @click="distruggiBarriera(b.id)">Distruggi</button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.hp-popup { display: grid; gap: .75rem; }
h2 { margin: 0; font-size: 1.1rem; }

.riepilogo { display: grid; gap: .25rem; justify-items: center; padding: .25rem 0 .5rem; }
.big { font-variant-numeric: tabular-nums; }
.big .cur { font-size: 2rem; font-weight: 800; color: #16a34a; }
.big .max { font-size: 1.1rem; font-weight: 700; opacity: .6; margin-left: .35rem; }
.extra { display: flex; gap: .4rem; flex-wrap: wrap; justify-content: center; }
.pill { font-size: .78rem; font-weight: 700; padding: .15rem .6rem; border-radius: 999px; }
.pill.temp { background: #ccfbf1; color: #0f766e; }
.pill.barr { background: #dbeafe; color: #1d4ed8; }

.block { display: grid; gap: .4rem; }
.lbl { font-size: .8rem; font-weight: 700; opacity: .8; }
.temp-val { color: #0f766e; }

.quick {
  display: grid;
  grid-template-columns: auto auto 1fr auto auto;
  gap: .35rem;
  align-items: center;
}
.quick input {
  min-width: 0; text-align: center; padding: .45rem .3rem;
  border: 1px solid #d0d5dd; border-radius: .5rem; font-variant-numeric: tabular-nums;
}

.apply { display: grid; grid-template-columns: 1fr 1fr; gap: .4rem; }

.empty { font-size: .85rem; opacity: .6; }

.barriera {
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  border-radius: .6rem;
  padding: .5rem .6rem;
  display: grid;
  gap: .4rem;
}
.barr-head { display: flex; justify-content: space-between; align-items: baseline; gap: .5rem; }
.barr-head .nome { font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.barr-head .val { font-weight: 800; color: #1d4ed8; font-variant-numeric: tabular-nums; white-space: nowrap; }
.barr-track { height: .55rem; background: #dbeafe; border-radius: 999px; overflow: hidden; }
.barr-fill { height: 100%; background: #3b82f6; }
.barr-actions { display: flex; flex-wrap: wrap; gap: .35rem; }

.btn {
  border: 1px solid #d0d5dd; background: #fff; border-radius: .5rem;
  padding: .4rem .7rem; cursor: pointer; font-weight: 600; font-size: .85rem;
}
.btn:disabled { opacity: .5; cursor: default; }
.btn.wide { width: 100%; }
.btn.dmg { border-color: #fecaca; background: #fef2f2; color: #991b1b; }
.btn.heal { border-color: #bbf7d0; background: #f0fdf4; color: #166534; }
.btn.primary { background: #2563eb; border-color: #2563eb; color: #fff; }
.btn.ghost { background: #f8fafc; }
.btn.danger { border-color: #fecaca; background: #fef2f2; color: #991b1b; }
</style>
