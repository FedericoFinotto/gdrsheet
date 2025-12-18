<script setup lang="ts">

import TabExpandable from "../../../../../../../components/TabExpandable.vue";

type SkillRow = {
  uid: string; name: string; isClass: boolean; isOtherClass: boolean;
  spent: number; effect: number; current: number; total: number; max: number
}

const props = defineProps<{
  disabled: boolean
  rows: SkillRow[]
  sumAbil: string
  canInc: (r: SkillRow) => boolean
  defaultOpen: boolean
}>()

const emit = defineEmits<{
  (e: 'inc', uid: string): void
  (e: 'dec', uid: string): void
  (e: 'direct-change', uid: string, value: string): void
}>()

function onInc(uid: string) {
  emit('inc', uid)
}

function onDec(uid: string) {
  emit('dec', uid)
}

function onDirect(uid: string, ev: Event) {
  const val = (ev.target as HTMLInputElement).value
  emit('direct-change', uid, val)
}
</script>

<template>
  <TabExpandable title="Abilità & Ranghi" :defaultOpen="defaultOpen">
    <template #summary>{{ sumAbil }}</template>
    <template #content>
      <div class="skills">
        <div class="skill-row" v-for="r in rows" :key="r.uid">
          <div class="skill-main">
            <div class="skill-badges">
              <span v-if="r.isClass" class="pill blue">CLASSE</span>
              <span v-else-if="r.isOtherClass" class="pill blue">CROSS</span>
              <span v-else class="pill red">CROSS</span>
            </div>
            <div class="skill-name">
              <ins v-if="r.isClass || r.isOtherClass">{{ r.name }}</ins>
              <span v-else>{{ r.name }}</span>
            </div>
          </div>
          <div class="skill-stats">
            <div class="stat">
              <div class="stat-label">Attuale</div>
              <div class="stat-value">{{ r.current }}</div>
            </div>
            <div class="stat">
              <div class="stat-label">Punti</div>
              <div class="counter">
                <button type="button" class="btn" @click.stop="onDec(r.uid)" :disabled="disabled || r.spent<=0">−
                </button>
                <input type="number"
                       inputmode="numeric"
                       min="0"
                       step="1"
                       :value="r.spent"
                       @input="onDirect(r.uid, $event)"
                       :disabled="disabled"/>
                <button type="button" class="btn" @click.stop="onInc(r.uid)" :disabled="disabled || !canInc(r)">+
                </button>
              </div>
            </div>
            <div class="stat tight">
              <div class="stat-label">Effetto</div>
              <div class="stat-value">+{{ r.effect }}</div>
            </div>
            <div class="stat">
              <div class="stat-label">Totale</div>
              <div class="stat-value">{{ r.total }}/{{ r.max }}</div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </TabExpandable>
</template>

<style scoped>
.skills {
  display: grid;
  gap: .45rem;
}

.skill-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: .5rem;
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  padding: .5rem .6rem;
  background: #fff;
}

@media (min-width: 700px) {
  .skill-row {
    grid-template-columns: 1fr auto;
    align-items: center;
  }
}

.skill-main {
  display: flex;
  flex-direction: row;
  gap: .25rem;
  min-width: 0;
}

.skill-name {
  font-weight: 600;
  line-height: 1.2;
  white-space: normal;
  word-break: break-word;
}

.skill-badges {
  display: flex;
  gap: .35rem;
  flex-wrap: wrap;
}

.pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: .1rem .45rem;
  border-radius: .5rem;
  font-size: .8rem;
}

.pill.blue {
  background: #dbeafe;
  color: #1e3a8a;
}

.pill.red {
  background: #fee2e2;
  color: #b91c1c;
}

.skill-stats {
  display: grid;
  grid-auto-flow: column;
  gap: .6rem;
  align-items: center;
  justify-content: space-between;
}

@media (max-width: 699px) {
  .skill-stats {
    grid-auto-flow: column;
    justify-items: center;
  }
}

.stat {
  display: grid;
  gap: .2rem;
  justify-items: start;
}

.stat.tight .stat-value {
  min-width: 2.5rem;
  text-align: left;
}

.stat-label {
  font-size: .76rem;
  opacity: .75;
}

.stat-value {
  font-variant-numeric: tabular-nums;
}

.counter {
  display: inline-flex;
  align-items: center;
  gap: .35rem;
}

.counter .btn {
  border: 1px solid #d1d5db;
  background: #fff;
  border-radius: .4rem;
  padding: .25rem .55rem;
  cursor: pointer;
  min-width: 2.1rem;
  min-height: 2.1rem;
}

.counter input[type="number"] {
  width: 3.2rem;
  text-align: center;
}

.btn:disabled {
  opacity: .6;
  cursor: default;
}
</style>
