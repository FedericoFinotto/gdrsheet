<script setup lang="ts">
import {onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import type {AxiosResponse} from 'axios'
import {getItem} from "../../../../../../service/PersonaggioService"
import SpellEditor from "./SpellEditor.vue"

const route = useRoute();
const idItem = Number(route.params.id);
if (isNaN(idItem)) throw new Error('Parametro id non valido');

interface ItemDB {
  id: number
  tipo: string
  nome?: string
  descrizione?: string
  componenti?: string[]
  scuola?: string
  tempo?: string
  ts?: string
  range?: string
  durata?: string
}

const router = useRouter()
const loading = ref(false)
const errorMsg = ref<string | null>(null)
const item = ref<ItemDB | null>(null)

function parseId(v: unknown): number | null {
  const n = Number(v);
  return Number.isFinite(n) ? n : null
}

async function load(id: number) {
  loading.value = true;
  errorMsg.value = null;
  item.value = null
  try {
    const res: AxiosResponse<ItemDB> = await getItem(id)
    item.value = res.data
  } catch (e: any) {
    errorMsg.value = e?.message ?? 'Errore nel caricamento'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load(idItem)
})

// 🔧 qui ascolto il param giusto
watch(() => route.params.id, v => {
  const id = parseId(v);
  if (id != null) load(id)
})

function goBack() {
  router.back()
}
</script>

<template>
  <section class="item-editor">
    <header class="head">
      <h1>Editor Oggetto</h1>
      <div class="meta">
        <span v-if="item">#{{ item.id }}</span>
        <span v-if="item?.tipo" class="pill">{{ item.tipo }}</span>
      </div>
    </header>

    <div class="editor-scroll">
      <div v-if="loading" class="state loading">Caricamento…</div>
      <div v-else-if="errorMsg" class="state error">{{ errorMsg }}</div>
      <div v-else-if="!item" class="state empty">Nessun dato.</div>

      <template v-else>
        <SpellEditor
            v-if="item.tipo === 'INCANTESIMO'"
            :item="item"
            @cancel="goBack"
            @saved="goBack"
        />
        <div v-else class="state unsupported">
          <p><strong>Tipo non gestito:</strong> {{ item.tipo }}</p>
          <button class="btn" @click="goBack">Torna indietro</button>
        </div>
      </template>
    </div>
  </section>
</template>

<style scoped>
.item-editor {
  height: 100dvh;
  max-height: 100dvh;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.head {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: .75rem;
  padding: .75rem 1rem;
  background: inherit;
  border-bottom: 1px solid #e5e7eb;
}

.head h1 {
  margin: 0;
  font-size: 1.1rem;
}

.meta {
  display: inline-flex;
  gap: .5rem;
  align-items: center;
}

.pill {
  font-size: .8rem;
  padding: .1rem .5rem;
  border-radius: .5rem;
  background: #eef2ff;
  color: #3730a3;
}

/* 👇 scroll area con spazio extra in fondo per evitare tagli su mobile */
.editor-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  padding: .75rem 1rem calc(2rem + env(safe-area-inset-bottom, 0px));
  display: grid;
  gap: .75rem;
}

.state {
  padding: .75rem;
  border: 1px dashed #e5e7eb;
  border-radius: .5rem;
  margin: 0;
}

.state.error {
  color: #991b1b;
  background: #fef2f2;
  border-color: #fecaca;
}

.unsupported .btn {
  margin-top: .5rem;
}

.btn {
  padding: .5rem .9rem;
  border-radius: .5rem;
  border: 1px solid #d0d5dd;
  background: #fff;
  cursor: pointer;
}
</style>
