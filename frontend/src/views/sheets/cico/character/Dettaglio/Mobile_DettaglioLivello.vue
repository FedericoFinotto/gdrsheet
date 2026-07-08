<script setup lang="ts">
import {computed, defineProps, onMounted, ref} from 'vue';
import {flattenTrasformazioni, mostraLabel, testoModificatore} from "../../../../../function/Utils";
import {getItem, switchItemState} from "../../../../../service/PersonaggioService";
import {ItemDB, TIPO_ITEM} from "../../../../../models/entity/ItemDB";
import {useCharacterStore} from "../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import Icona from "../../../../../components/Icona/Icona.vue";
import {useRouter} from "vue-router";
import usePopup from "../../../../../function/usePopup";
import Mobile_DettaglioItem from "./Mobile_DettaglioItem.vue";
import {Item} from "../../../../../models/dto/Item";

const router = useRouter()
const {openPopup} = usePopup()

const props = defineProps<{
  data: {
    item: Item;
    personaggio: any;
  }
}>();
const {item: itemInfo, personaggio} = props.data;

const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);

const itemDetail = ref<ItemDB | null>(null);
const loading = ref(true);
const labelMap = ref<Record<string, string>>({});

async function switchState() {
  let itemIdToSwitch = [];
  if (itemInfo.disabled) {
    switch (itemInfo.tipo) {
      case 'TRASFORMAZIONE':
        itemIdToSwitch.push(...flattenTrasformazioni(personaggio.items).filter(x => !x.disabled).map(x => x.id));
        break;
      case 'IDOLO':
        itemIdToSwitch.push(...personaggio.items.idoli.filter(x => !x.disabled).map(x => x.id));
        break;
    }
  }
  itemIdToSwitch.push(itemInfo.id);
  try {
    for (const id of itemIdToSwitch) {
      await switchItemState(Number(id), personaggio.modificatori.id);
    }
    await characterStore.fetchCharacter(personaggio.modificatori.id, true);
  } catch (e) {
    console.error('Errore nello switch dello stato:', e);
  }
}

const disableLabel = computed(() => {
  switch (itemInfo?.tipo) {
    case TIPO_ITEM.EQUIPAGGIAMENTO: return itemInfo.disabled ? 'Equipaggia' : 'Togli';
    case TIPO_ITEM.TRASFORMAZIONE:  return itemInfo.disabled ? 'Trasformati' : 'Torna alla forma Base';
    case TIPO_ITEM.IDOLO:           return itemInfo.disabled ? 'Prega' : 'Disabilita';
    case TIPO_ITEM.LIVELLO:         return itemInfo.disabled ? 'Abilita livello' : 'Disabilita livello';
    default: return null;
  }
});

// -- Computed sezioni --

const listaClassi = computed(() =>
    (itemDetail.value?.child ?? [])
        .filter(c => c.itemTarget.tipo === TIPO_ITEM.CLASSE || c.itemTarget.tipo === TIPO_ITEM.RAZZA)
        .map(c => c.itemTarget)
)

const listaMaledizioni = computed(() =>
    (itemDetail.value?.child ?? [])
        .filter(c => c.itemTarget.tipo === TIPO_ITEM.MALEDIZIONE)
        .map(c => c.itemTarget)
)

const TIPI_ITEM_OTTENUTI = new Set([
  TIPO_ITEM.ABILITA, TIPO_ITEM.TALENTO, TIPO_ITEM.COMPETENZA,
  TIPO_ITEM.LINGUA, TIPO_ITEM.INCANTESIMO, TIPO_ITEM.FRUTTO,
  TIPO_ITEM.FORMA, TIPO_ITEM.ALTRO, TIPO_ITEM.PRIVILEGIO,
])

const listaItemOttenuti = computed(() =>
    (itemDetail.value?.child ?? [])
        .filter(c => TIPI_ITEM_OTTENUTI.has(c.itemTarget.tipo as any))
        .map(c => c.itemTarget)
)

const modsBase = computed(() =>
    (itemDetail.value?.modificatori ?? []).filter(m => m.tipo === 'BASE')
)
const modsAltri = computed(() =>
    (itemDetail.value?.modificatori ?? []).filter(m => m.tipo !== 'RANK' && m.tipo !== 'BASE')
)
const ranksAbilita = computed(() =>
    (itemDetail.value?.modificatori ?? []).filter(m => m.tipo === 'RANK' && !m.stat.id.toUpperCase().startsWith('PR'))
)
const ranksProfessioni = computed(() =>
    (itemDetail.value?.modificatori ?? []).filter(m => m.tipo === 'RANK' && m.stat.id.toUpperCase().startsWith('PR'))
)

const labelEntries = computed(() =>
    Object.entries(labelMap.value)
        .map(([key, val]) => ({ key, val, resolved: mostraLabel(key, val) }))
        .filter(x => x.resolved !== null)
)

const TIPO_LABEL: Record<string, string> = {
  ABILITA: 'Abilità', TALENTO: 'Talento', COMP: 'Competenza',
  LINGUA: 'Lingua', INCANTESIMO: 'Incantesimo',
  FRUTTO: 'Frutto', FORMA: 'Forma', ALTRO: 'Altro',
}
function tipoLabel(tipo: string) { return TIPO_LABEL[tipo] ?? tipo }

onMounted(async () => {
  try {
    const response = await getItem(itemInfo.id);
    const data = response.data;
    itemDetail.value = data;
    data.labels?.forEach(lbl => { labelMap.value[lbl.label] = lbl.valore; });
  } catch (e) {
    console.error('Errore caricamento item:', e);
  } finally {
    loading.value = false;
  }
});

function showInfoAbilitaPopup(itm) {
  openPopup(
      Mobile_DettaglioItem,
      {data: {item: {...itm}, personaggio: props.data.personaggio}},
      {closable: true, autoClose: 0}
  )
}
</script>

<template>
  <div class="livello-detail" v-if="!loading && itemDetail">

    <!-- Action bar -->
    <div class="action-bar">
      <button v-if="disableLabel" type="button" class="action-btn toggle"
          :class="itemInfo.disabled ? 'enable' : 'disable'" @click="switchState">
        {{ disableLabel }}
      </button>
      <button type="button" class="action-btn edit"
          @click.stop="router.push(`/itemeditor/${itemDetail.id}?personaggio=${personaggio.modificatori.id}`)">
        <Icona name="EDIT"/><span>Modifica</span>
      </button>
    </div>

    <!-- Labels dinamiche -->
    <div v-if="labelEntries.length" class="detail-section">
      <div v-for="entry in labelEntries" :key="entry.key">
        <strong>{{ entry.resolved.label }}:</strong> {{ entry.resolved.value }}
      </div>
    </div>

    <!-- Classe / Razza -->
    <div v-if="listaClassi.length" class="detail-section">
      <div class="section-title">Classe</div>
      <div class="item-list">
        <div v-for="item in listaClassi" :key="item.id" class="item-row" @click.stop="showInfoAbilitaPopup(item)">
          <span class="item-nome">{{ item.nome }}</span>
          <Icona name="INFO" class="item-info-ico"/>
        </div>
      </div>
    </div>

    <!-- Maledizioni -->
    <div v-if="listaMaledizioni.length" class="detail-section">
      <div class="section-title">Maledizione</div>
      <div class="item-list">
        <div v-for="item in listaMaledizioni" :key="item.id" class="item-row" @click.stop="showInfoAbilitaPopup(item)">
          <span class="item-nome">{{ item.nome }}</span>
          <Icona name="INFO" class="item-info-ico"/>
        </div>
      </div>
    </div>

    <!-- Abilità / Talenti / altri item ottenuti -->
    <div v-if="listaItemOttenuti.length" class="detail-section">
      <div class="section-title">Abilità & Talenti</div>
      <div class="item-list">
        <div v-for="item in listaItemOttenuti" :key="item.id" class="item-row item-row--ottenuto"
            @click.stop="showInfoAbilitaPopup(item)">
          <span class="item-tipo-badge">{{ tipoLabel(item.tipo) }}</span>
          <span class="item-nome">{{ item.nome }}</span>
          <Icona name="INFO" class="item-info-ico"/>
        </div>
      </div>
    </div>

    <!-- Modificatori BASE -->
    <div v-if="modsBase.length" class="detail-section">
      <div class="section-title">Base</div>
      <div class="mod-grid">
        <div v-for="mod in modsBase" :key="mod.id" class="mod-chip mod-chip--base">
          <span class="mod-chip-label">{{ mod.stat.label }}</span>
          <span class="mod-chip-val">{{ mod.valore }}</span>
          <span v-if="mod.nota" class="mod-chip-nota">{{ mod.nota }}</span>
        </div>
      </div>
    </div>

    <!-- Modificatori -->
    <div v-if="modsAltri.length" class="detail-section">
      <div class="section-title">Modificatori</div>
      <div class="mod-grid">
        <div v-for="mod in modsAltri" :key="mod.id" class="mod-chip">
          <span class="mod-chip-label">{{ mod.stat.label }}</span>
          <span class="mod-chip-val">{{ testoModificatore(mod.valore) }}</span>
          <span v-if="mod.nota" class="mod-chip-nota">{{ mod.nota }}</span>
        </div>
      </div>
    </div>

    <!-- Gradi Abilità -->
    <div v-if="ranksAbilita.length" class="detail-section">
      <div class="section-title">Gradi Abilità</div>
      <div class="rank-list">
        <div v-for="mod in ranksAbilita" :key="mod.id" class="rank-row">
          <span class="rank-label">{{ mod.stat.label }}</span>
          <span class="rank-val">{{ testoModificatore(mod.valore) }}</span>
          <span v-if="mod.nota" class="rank-nota">{{ mod.nota }}</span>
        </div>
      </div>
    </div>

    <!-- Gradi Professioni -->
    <div v-if="ranksProfessioni.length" class="detail-section">
      <div class="section-title section-title--prof">Gradi Professioni</div>
      <div class="rank-list rank-list--prof">
        <div v-for="mod in ranksProfessioni" :key="mod.id" class="rank-row">
          <span class="rank-label">{{ mod.stat.label }}</span>
          <span class="rank-val">{{ testoModificatore(mod.valore) }}</span>
          <span v-if="mod.nota" class="rank-nota">{{ mod.nota }}</span>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.livello-detail {
  display: flex;
  flex-direction: column;
  gap: .9rem;
}

/* ---- Action bar ---- */
.action-bar {
  display: flex;
  flex-wrap: wrap;
  gap: .5rem;
  align-items: center;
  padding-bottom: .6rem;
  border-bottom: 1px solid #e5e7eb;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: .35rem;
  border-radius: .5rem;
  padding: .4rem .75rem;
  font-weight: 600;
  font-size: .85rem;
  cursor: pointer;
  border: 1px solid transparent;
}
.action-btn:hover { filter: brightness(.97); }
.action-btn.toggle.enable  { border-color: #bbf7d0; background: #f0fdf4; color: #166534; }
.action-btn.toggle.disable { border-color: #fed7aa; background: #fff7ed; color: #9a3412; }
.action-btn.edit           { border-color: #bfdbfe; background: #eff6ff; color: #1d4ed8; margin-left: auto; }

/* ---- Sezioni ---- */
.detail-section {
  display: flex;
  flex-direction: column;
  gap: .4rem;
}

.section-title {
  font-size: .7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: .06em;
  color: #6b7280;
}

.section-title--prof {
  color: #7c3aed;
}

/* ---- Item list (classe/maledizione/abilità) ---- */
.item-list {
  display: flex;
  flex-direction: column;
  gap: .25rem;
}

.item-row {
  display: flex;
  align-items: center;
  gap: .5rem;
  padding: .3rem .5rem;
  border-radius: .4rem;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  cursor: pointer;
}
.item-row:hover { background: #f3f4f6; }

.item-row--ottenuto {
  background: #f0fdf4;
  border-color: #bbf7d0;
}
.item-row--ottenuto:hover { background: #dcfce7; }

.item-tipo-badge {
  font-size: .65rem;
  font-weight: 700;
  padding: .1rem .35rem;
  border-radius: .3rem;
  background: #d1fae5;
  color: #065f46;
  white-space: nowrap;
}

.item-nome {
  flex: 1;
  font-size: .9rem;
  font-weight: 500;
}

.item-info-ico {
  font-size: .8rem;
  color: #9ca3af;
  margin-left: auto;
}

/* ---- Mod chips ---- */
.mod-grid {
  display: flex;
  flex-wrap: wrap;
  gap: .35rem;
}

.mod-chip {
  display: inline-flex;
  align-items: baseline;
  gap: .3rem;
  padding: .25rem .5rem;
  border-radius: .4rem;
  border: 1px solid #e0e7ff;
  background: #eef2ff;
  font-size: .85rem;
}

.mod-chip--base {
  border-color: #fde68a;
  background: #fffbeb;
}

.mod-chip-label {
  font-weight: 600;
  color: #374151;
}

.mod-chip-val {
  font-weight: 700;
  color: #4338ca;
}

.mod-chip--base .mod-chip-val {
  color: #92400e;
}

.mod-chip-nota {
  font-size: .75rem;
  color: #6b7280;
}

/* ---- Rank list ---- */
.rank-list {
  display: flex;
  flex-direction: column;
  gap: .2rem;
}

.rank-list--prof {
  border-left: 3px solid #a78bfa;
  padding-left: .5rem;
}

.rank-row {
  display: flex;
  align-items: baseline;
  gap: .5rem;
  padding: .2rem .4rem;
  border-radius: .3rem;
}

.rank-row:nth-child(even) {
  background: #f9fafb;
}

.rank-label {
  flex: 1;
  font-size: .88rem;
  color: #374151;
}

.rank-val {
  font-weight: 700;
  font-size: .9rem;
  color: #1d4ed8;
  min-width: 2rem;
  text-align: right;
}

.rank-list--prof .rank-val {
  color: #7c3aed;
}

.rank-nota {
  font-size: .75rem;
  color: #9ca3af;
}
</style>
