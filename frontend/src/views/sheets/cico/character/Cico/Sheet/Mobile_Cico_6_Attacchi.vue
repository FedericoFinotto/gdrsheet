<script setup lang="ts">
import {computed, defineAsyncComponent, defineProps} from 'vue';
import {useCharacterStore} from "../../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import {applicaBonusDado} from "../../../../../../function/Utils";
import {DannoRisolto} from "../../../../../../function/Calcolo";
import useDiceRoll from "../../../../../../function/useDiceRoll";
import usePopup from "../../../../../../function/usePopup";

const DiceRollerPopup = defineAsyncComponent(() => import('../../../../../../components/DiceRollerPopup.vue'))
const {openPopup} = usePopup()

const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);
const {risultato} = useDiceRoll()

const props = defineProps({
  idPersonaggio: {
    type: Number,
    required: true
  }
});

// Le formule sono già precalcolate in background dallo store (subito dopo il caricamento di
// items), quindi qui basta leggere dalla cache: niente chiamate di rete al primo render.
const itemsAttacchi = computed(() => cache.value[props.idPersonaggio]?.attacchi ?? []);

// applica il bonus d20 al valore atk quando il dado è attivo
const itemsDisplay = computed(() => {
  if (risultato.value === null) return itemsAttacchi.value
  return itemsAttacchi.value.map(itm => ({
    ...itm,
    atk: itm.atk != null ? applicaBonusDado(itm.atk, risultato.value!) : itm.atk
  }))
})

function apriDanno(danno: DannoRisolto) {
  if (!danno.valore) return
  openPopup(DiceRollerPopup, {initialFormula: danno.valore}, {closable: true, autoClose: 0})
}
</script>

<template>
  <div class="atk-table" v-if="itemsDisplay.length > 0">
    <template v-for="itm in itemsDisplay" :key="itm.id">
      <!-- Un attacco: a sinistra un solo TPC/TS (che copre tutte le righe danno), a destra
           una riga per ciascun danno — non vanno accodati in una sola cella. -->
      <div class="atk-block">
        <div class="atk-left">
          <div class="atk-nome">{{ itm.nome }}</div>
          <div class="atk-nomeitem muted">{{ itm.nomeItem }}</div>
          <div class="atk-valore">{{ itm.atk }}</div>
          <div v-if="itm.attacco" class="atk-sub muted">{{ itm.attacco }}</div>
        </div>
        <div class="atk-right">
          <div v-if="!(itm.danniRisolti ?? []).length" class="danno-row empty muted">Nessun danno</div>
          <div v-for="(d, di) in (itm.danniRisolti ?? [])" :key="di" class="danno-row" @click="apriDanno(d)">
            <div class="danno-testa">
              <span class="danno-valore">{{ d.valore }}</span>
              <span v-if="d.tipo" class="danno-tipo muted">{{ d.tipo }}</span>
            </div>
            <div v-if="d.formula" class="danno-formula muted">{{ d.formula }}</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.atk-table { display: flex; flex-direction: column; gap: .5rem; }
.atk-block {
  display: grid;
  grid-template-columns: 1fr 1fr;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: .5rem;
  overflow: hidden;
}
.atk-left {
  padding: .5rem .6rem;
  border-right: 1px solid var(--color-border, #e5e7eb);
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.atk-nome { font-weight: 700; font-size: .9rem; }
.atk-nomeitem { font-size: .75rem; }
.atk-valore { font-size: 1rem; font-weight: 700; margin-top: .2rem; }
.atk-sub { font-size: .75rem; }
.atk-right { display: flex; flex-direction: column; justify-content: center; }
.danno-row {
  padding: .4rem .6rem;
  cursor: pointer;
}
.danno-row:not(:last-child) { border-bottom: 1px solid var(--color-border, #e5e7eb); }
.danno-testa {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: .4rem;
}
.danno-formula { font-size: .72rem; margin-top: .1rem; }
.danno-row:hover { background: var(--color-surface-1, #f9fafb); }
.danno-row.empty { cursor: default; }
.danno-valore { font-weight: 600; }
.danno-tipo { font-size: .78rem; }
.muted { color: var(--color-text-secondary, #6b7280); }
</style>
