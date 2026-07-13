<script setup>
import {computed, defineProps} from 'vue'
import {useCharacterStore} from "../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import {applicaBonusDado, testoModificatore} from "../../../../../function/Utils";
import usePopup from "../../../../../function/usePopup";
import useDiceRoll from "../../../../../function/useDiceRoll";
import Mobile_DettaglioCaratteristica from "../Dettaglio/Mobile_DettaglioCaratteristica.vue";

const {openPopup} = usePopup()
const {risultato} = useDiceRoll()

// stat a cui va sommato il risultato del tiro globale del d20:
// caratteristiche, tiri salvezza, CA/contatto/sorpreso, BAB/lotta/mischia/distanza
const STAT_BONUS_D20 = new Set([
  'FOR', 'DES', 'COS', 'INT', 'SAG', 'CAR',
  'TMP', 'RFL', 'VLT',
  'CA', 'CAC', 'CAS',
  'BAB', 'LTT', 'MSC', 'GTT',
])

const characterStore = useCharacterStore()
const {cache} = storeToRefs(characterStore);

const props = defineProps({
  idPersonaggio: {
    type: Number,
    required: true
  },
  id: {
    type: String,
    required: true
  },
  label: {
    type: String,
    default: undefined
  }
});

function showPopup() {
  openPopup(
      Mobile_DettaglioCaratteristica,
      {stat, idPersonaggio: props.idPersonaggio},
      {closable: true, autoClose: 0}
  )
}

const stat = computed(() => {
  const statistiche = cache.value[props.idPersonaggio]?.modificatori;

  if (statistiche) {
    const caratteristica = statistiche.caratteristiche.find(x => x.id === props.id);
    const classeArmatura = statistiche.classeArmatura.find(x => x.id === props.id);
    const tiroSalvezza = statistiche.tiriSalvezza.find(x => x.id === props.id);
    const bonusAttacco = statistiche.bonusAttacco.find(x => x.id === props.id);
    const attributo = statistiche.attributi.find(x => x.id === props.id);

    if (caratteristica) {
      return caratteristica;
    }
    if (classeArmatura) {
      return classeArmatura;
    }
    if (tiroSalvezza) {
      return tiroSalvezza;
    }
    if (bonusAttacco) {
      const values = bonusAttacco.attacchiMultipli;
      bonusAttacco.modificatore = values
          .map(v => testoModificatore(v))
          .join(' / ');
      return bonusAttacco;
    }
    if (attributo) {
      const infinito = attributo.modificatori.filter(x => x.nota === null && (x.formula === '+INF' || x.formula === '+∞'));
      const perc = attributo.percentuale ?? 0;
      let mod = attributo.modificatore;

      if (infinito && infinito.length > 0) {
        mod = '+∞';
      } else if (mod !== 0 && perc !== 0) {
        mod = `${mod} - ${perc}%`;
      } else if (mod === 0 && perc !== 0) {
        mod = `${perc}%`;
      } else if (mod === 0) {
        mod = attributo.modificatori
            .filter(v => v.nota === null)
            .map(v => v.formula)
            .join(' + ');
      }
      return {
        ...attributo,
        modificatore: mod
      };
    }
    return null;
  }
});

// conteggio modificatori situazionali (con nota) per il badge
const notaCount = computed(() => {
  const mods = stat.value?.modificatori
  if (!Array.isArray(mods)) return 0
  return mods.filter(m => m.nota != null && m.nota !== '').length
})

// evidenzia la caratteristica quando ha un modificatore "Temporaneo" attivo (non zero),
// visibile anche da fuori senza aprire il dettaglio (vedi Mobile_DettaglioCaratteristica.vue)
const hasTemp = computed(() => {
  const mods = stat.value?.modificatori
  if (!Array.isArray(mods)) return false
  return mods.some(m => String(m?.item ?? '').toLowerCase() === 'temporaneo' && Number(m?.valore) !== 0)
})

// modificatore mostrato: se è in corso un tiro globale e la stat è idonea,
// somma il risultato del d20 al valore.
const modificatoreVisualizzato = computed(() => {
  if (!stat.value) return '';
  const base = testoModificatore(stat.value.modificatore) ?? '';
  if (risultato.value !== null && STAT_BONUS_D20.has(props.id)) {
    return applicaBonusDado(base, risultato.value);
  }
  return base;
});
</script>

<template>
  <div class="stat-box" :class="{'has-temp': hasTemp}" :title="hasTemp ? 'Modificatore temporaneo attivo' : undefined">
    <div class="label">{{ label ?? id }}</div>
    <div class="modifier-wrap" @click="showPopup">
      <span class="modifier">{{ modificatoreVisualizzato }}</span>
      <span v-if="notaCount > 0" class="nota-badge" :title="`${notaCount} modificator${notaCount === 1 ? 'e situazionale' : 'i situazionali'}`">{{ notaCount }}</span>
    </div>
    <div class="base">{{ stat ? (stat.valore ?? '') : '' }}</div>
  </div>
</template>