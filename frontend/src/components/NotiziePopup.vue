<script setup lang="ts">
import {computed, ref} from 'vue'
import type {NotiziaDTO} from '../service/NotizieService'

const props = defineProps<{ notizie: NotiziaDTO[] }>()
const emit = defineEmits<{ (e: 'close'): void }>()

const attive = computed(() => props.notizie.filter(n => !n.archiviata))
const archiviate = computed(() => props.notizie.filter(n => n.archiviata))
const mostraArchiviate = ref(false)

const aperta = ref<number | null>(null)

function toggle(id: number) {
  aperta.value = aperta.value === id ? null : id
}

function formattaData(s?: string) {
  if (!s) return ''
  try {
    return new Date(s).toLocaleString('it-IT', {day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'})
  } catch {
    return s
  }
}
</script>

<template>
  <div class="notizie-panel">
    <div class="notizie-header">
      <span class="notizie-title">
        <i class="fa-solid fa-bell"></i> Notizie
      </span>
      <button class="close-btn" @click="emit('close')">✕</button>
    </div>

    <div class="notizie-list">
      <div v-if="notizie.length === 0" class="notizie-empty">
        Nessuna notizia disponibile.
      </div>
      <div v-for="n in attive" :key="n.id" class="notizia-card">
        <button class="notizia-toggle" @click="toggle(n.id)">
          <span class="notizia-nome">{{ n.nome }}</span>
          <i class="fa-solid" :class="aperta === n.id ? 'fa-chevron-up' : 'fa-chevron-down'"></i>
        </button>
        <div v-if="aperta === n.id" class="notizia-body">
          <div v-if="n.descrizione" class="notizia-desc" v-html="n.descrizione"/>
          <div v-if="n.dataInizio || n.dataFine" class="notizia-date">
            <span v-if="n.dataInizio">Dal {{ formattaData(n.dataInizio) }}</span>
            <span v-if="n.dataFine">al {{ formattaData(n.dataFine) }}</span>
          </div>
        </div>
      </div>

      <button v-if="archiviate.length > 0" class="archiviate-toggle" @click="mostraArchiviate = !mostraArchiviate">
        <i class="fa-solid fa-box-archive"></i> Archiviate ({{ archiviate.length }})
        <i class="fa-solid" :class="mostraArchiviate ? 'fa-chevron-up' : 'fa-chevron-down'"></i>
      </button>

      <template v-if="mostraArchiviate">
        <div v-for="n in archiviate" :key="n.id" class="notizia-card archiviata">
          <button class="notizia-toggle" @click="toggle(n.id)">
            <span class="notizia-nome">{{ n.nome }}</span>
            <i class="fa-solid" :class="aperta === n.id ? 'fa-chevron-up' : 'fa-chevron-down'"></i>
          </button>
          <div v-if="aperta === n.id" class="notizia-body">
            <div v-if="n.descrizione" class="notizia-desc" v-html="n.descrizione"/>
            <div v-if="n.dataInizio || n.dataFine" class="notizia-date">
              <span v-if="n.dataInizio">Dal {{ formattaData(n.dataInizio) }}</span>
              <span v-if="n.dataFine">al {{ formattaData(n.dataFine) }}</span>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.notizie-panel {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1100;
  background: #fff;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
}

.notizie-header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: .85rem 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.notizie-title {
  font-weight: 700;
  font-size: 1rem;
  display: flex;
  align-items: center;
  gap: .4rem;
  color: #1e40af;
}

.close-btn {
  background: transparent;
  border: 0;
  cursor: pointer;
  font-size: 1rem;
  color: #6b7280;
  padding: .25rem .4rem;
  border-radius: .35rem;
}
.close-btn:hover { background: #f3f4f6; }

.notizie-list {
  padding: .75rem;
  display: flex;
  flex-direction: column;
  gap: .5rem;
}

.notizie-empty {
  text-align: center;
  color: #6b7280;
  padding: 1.5rem 0;
  font-size: .95rem;
}

.notizia-card {
  border: 1px solid #bae6fd;
  border-radius: .5rem;
  overflow: hidden;
}
.notizia-card.archiviata { border-color: #e5e7eb; opacity: .75; }
.notizia-card.archiviata .notizia-toggle { background: #f9fafb; }
.notizia-card.archiviata .notizia-nome { color: #4b5563; }

.archiviate-toggle {
  display: flex;
  align-items: center;
  gap: .5rem;
  width: 100%;
  padding: .6rem 1rem;
  margin-top: .25rem;
  background: transparent;
  border: 1px dashed #d1d5db;
  border-radius: .5rem;
  cursor: pointer;
  font-size: .85rem;
  color: #6b7280;
}
.archiviate-toggle:hover { background: #f9fafb; }
.archiviate-toggle i:last-child { margin-left: auto; }

.notizia-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: .5rem;
  padding: .75rem 1rem;
  background: #f0f9ff;
  border: 0;
  cursor: pointer;
  text-align: left;
}
.notizia-toggle:hover { background: #e0f2fe; }

.notizia-nome {
  font-size: 1rem;
  font-weight: 600;
  color: #0c4a6e;
  flex: 1;
}

.notizia-body {
  padding: .75rem 1rem;
  border-top: 1px solid #bae6fd;
  background: #fff;
}

.notizia-desc {
  font-size: .9rem;
  color: #374151;
  white-space: pre-wrap;
  margin-bottom: .5rem;
}

.notizia-date {
  font-size: .78rem;
  color: #6b7280;
  display: flex;
  gap: .5rem;
  flex-wrap: wrap;
}
</style>
