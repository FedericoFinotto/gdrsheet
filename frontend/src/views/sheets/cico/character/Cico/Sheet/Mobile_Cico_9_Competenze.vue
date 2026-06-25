<script setup lang="ts">
import {computed, ref, watch} from 'vue';
import {useCharacterStore} from "../../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import {linkItem, searchItems, unlinkItem} from "../../../../../../service/PersonaggioService";
import Icona from "../../../../../../components/Icona/Icona.vue";
import {TIPO_ITEM} from "../../../../../../models/entity/ItemDB";
import SearchSelect from "../../../../../../components/SearchSelect.vue";

const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);

const props = defineProps({idPersonaggio: {type: Number, required: true}});

const itemsCompetenze = computed(() =>
    [...(cache.value[props.idPersonaggio]?.items?.competenze ?? [])]
        .sort((a: any, b: any) => a.nome.localeCompare(b.nome))
);
const itemsLingue = computed(() =>
    [...(cache.value[props.idPersonaggio]?.items?.lingue ?? [])]
        .sort((a: any, b: any) => a.nome.localeCompare(b.nome))
);

// -- Ricerca --
const query = ref('')
const tipoFiltro = ref<string>(TIPO_ITEM.COMPETENZA)
const risultati = ref<any[]>([])
const searching = ref(false)
const busy = ref(false)
let searchTimer: any = null

const TIPI = [
  {value: TIPO_ITEM.COMPETENZA, label: 'Competenza'},
  {value: TIPO_ITEM.LINGUA,     label: 'Lingua'},
]

watch([query, tipoFiltro], () => {
  clearTimeout(searchTimer)
  if (!query.value.trim()) { risultati.value = []; return }
  searching.value = true
  searchTimer = setTimeout(async () => {
    try {
      const res = await searchItems(query.value.trim(), tipoFiltro.value || undefined)
      risultati.value = res.data as any[]
    } catch { risultati.value = [] }
    finally { searching.value = false }
  }, 300)
})

async function aggiungi(item: any) {
  if (busy.value) return
  try {
    busy.value = true
    await linkItem(item.id, props.idPersonaggio)
    await characterStore.fetchCharacter(props.idPersonaggio, true)
    risultati.value = risultati.value.filter(r => r.id !== item.id)
  } catch (e) {
    console.error('Errore collegamento item:', e)
  } finally {
    busy.value = false
  }
}

async function rimuovi(item: any) {
  if (busy.value) return
  try {
    busy.value = true
    await unlinkItem(item.id, props.idPersonaggio)
    await characterStore.fetchCharacter(props.idPersonaggio, true)
  } catch (e) {
    console.error('Errore scollegamento item:', e)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="comp-page">

    <!-- Competenze -->
    <section class="comp-section">
      <div class="section-header">
        <span class="section-title">Competenze</span>
        <span class="section-count">{{ itemsCompetenze.length }}</span>
      </div>
      <div v-if="itemsCompetenze.length" class="item-list">
        <div v-for="item in itemsCompetenze" :key="item.id" class="item-row">
          <span class="item-nome">{{ item.nome }}</span>
          <button v-if="item.scollegabile" type="button" class="remove-btn" :disabled="busy" @click="rimuovi(item)" title="Scollega">
            <Icona name="XMARK"/>
          </button>
        </div>
      </div>
      <div v-else class="empty">Nessuna competenza.</div>
    </section>

    <!-- Lingue -->
    <section class="comp-section">
      <div class="section-header">
        <span class="section-title">Lingue</span>
        <span class="section-count">{{ itemsLingue.length }}</span>
      </div>
      <div v-if="itemsLingue.length" class="item-list">
        <div v-for="item in itemsLingue" :key="item.id" class="item-row">
          <span class="item-nome">{{ item.nome }}</span>
          <button v-if="item.scollegabile" type="button" class="remove-btn" :disabled="busy" @click="rimuovi(item)" title="Scollega">
            <Icona name="XMARK"/>
          </button>
        </div>
      </div>
      <div v-else class="empty">Nessuna lingua.</div>
    </section>

    <!-- Aggiungi -->
    <section class="comp-section add-section">
      <div class="section-title">Aggiungi</div>
      <div class="search-row">
        <SearchSelect v-model="tipoFiltro" class="tipo-select" :options="TIPI" :sort="false"/>
        <input v-model="query" type="text" placeholder="Cerca per nome…" class="search-input"/>
      </div>

      <div v-if="searching" class="empty">Ricerca…</div>
      <div v-else-if="risultati.length" class="risultati-list">
        <div v-for="r in risultati" :key="r.id" class="risultato-row" @click="aggiungi(r)">
          <span class="item-nome">{{ r.nome }}</span>
          <Icona name="ADD" class="add-ico"/>
        </div>
      </div>
      <div v-else-if="query" class="empty">Nessun risultato.</div>
    </section>

  </div>
</template>

<style scoped>
.comp-page {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.comp-section {
  display: flex;
  flex-direction: column;
  gap: .4rem;
}

.section-header {
  display: flex;
  align-items: center;
  gap: .5rem;
}

.section-title {
  font-size: .75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: .06em;
  color: #6b7280;
}

.section-count {
  font-size: .7rem;
  font-weight: 700;
  background: #e5e7eb;
  color: #374151;
  padding: .05rem .4rem;
  border-radius: 999px;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: .2rem;
}

.item-row, .risultato-row {
  display: flex;
  align-items: center;
  gap: .5rem;
  padding: .35rem .5rem;
  border-radius: .4rem;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
}

.risultato-row {
  cursor: pointer;
  background: #fff;
}
.risultato-row:hover { background: #f0fdf4; border-color: #bbf7d0; }

.item-nome { flex: 1; font-size: .9rem; }

.remove-btn {
  border: none;
  background: transparent;
  cursor: pointer;
  color: #ef4444;
  padding: .1rem .2rem;
  border-radius: .3rem;
  display: flex;
  align-items: center;
  font-size: .8rem;
}
.remove-btn:disabled { opacity: .4; cursor: default; }
.remove-btn:hover:not(:disabled) { background: #fee2e2; }

.add-ico {
  font-size: .8rem;
  color: #16a34a;
}

.empty {
  font-size: .85rem;
  color: #9ca3af;
}

.add-section {
  border-top: 1px solid #e5e7eb;
  padding-top: .6rem;
}

.search-row {
  display: flex;
  gap: .4rem;
}

.tipo-select {
  flex: 0 0 auto;
  padding: .4rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  font-size: .85rem;
  background: #fff;
}

.search-input {
  flex: 1;
  padding: .4rem .6rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  font-size: .9rem;
}

.risultati-list {
  display: flex;
  flex-direction: column;
  gap: .2rem;
  max-height: 14rem;
  overflow-y: auto;
}
</style>
