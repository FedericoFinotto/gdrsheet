<script setup lang="ts">
import {computed, defineProps, markRaw, onMounted, watch} from 'vue'
import Tabella from '../../../../../../components/Tabella.vue'
import {storeToRefs} from 'pinia'
import {useCharacterStore} from "../../../../../../stores/personaggio";
import {applicaBonusDado, testoModificatore} from "../../../../../../function/Utils";
import useDiceRoll from "../../../../../../function/useDiceRoll";
import Mobile_DettaglioAbilita from "../../Dettaglio/Mobile_DettaglioAbilita.vue";

const props = defineProps<{ idPersonaggio: number }>()

const characterStore = useCharacterStore()
const {cache} = storeToRefs(characterStore)
const {risultato} = useDiceRoll()

onMounted(() => {
  characterStore.fetchCharacter(props.idPersonaggio)
})
watch(() => props.idPersonaggio, id => {
  characterStore.fetchCharacter(id)
})

const abilita = computed(() => {
  return (cache.value[props.idPersonaggio]?.modificatori?.abilita ?? []).filter(x => x.show).map(abilita => {
    return {
      ...abilita,
      id: abilita.abilita.id,
      nome: abilita.abilita.nome,
      valore: risultato.value !== null
          ? applicaBonusDado(testoModificatore(abilita.abilita.modificatore), risultato.value)
          : testoModificatore(abilita.abilita.modificatore),
      caratteristica: abilita?.base?.id ?? '',
      negata: abilita.negata ?? false,
      expandedComponent: markRaw(Mobile_DettaglioAbilita),
      expandedProps: {data: {...abilita}, idPersonaggio: props.idPersonaggio}
    }
  }).sort((a, b) => a.nome.localeCompare(b.nome));
})

// Abilità/Conoscenze/Intrattenere/Artigianato/Professioni: distinte solo per convenzione
// sull'id (tutte tipo='AB' a DB) — stessa suddivisione usata in Gestisci Gradi.
type Famiglia = 'AB' | 'CO' | 'IN' | 'AR' | 'PR'
const FAMIGLIA_LABEL: Record<Famiglia, string> = {
  AB: 'Abilità', CO: 'Conoscenze', IN: 'Intrattenere', AR: 'Artigianato', PR: 'Professioni',
}
const FAMIGLIE_ORDINATE: Famiglia[] = ['AB', 'CO', 'IN', 'AR', 'PR']

function famigliaDi(id: string): Famiglia {
  const up = String(id ?? '').toUpperCase()
  if (up.startsWith('PR')) return 'PR'
  if (up.startsWith('CO')) return 'CO'
  if (up.startsWith('IN')) return 'IN'
  if (up.startsWith('AR')) return 'AR'
  return 'AB'
}

const abilitaPerFamiglia = computed(() =>
    FAMIGLIE_ORDINATE
        .map(f => ({famiglia: f, label: FAMIGLIA_LABEL[f], righe: abilita.value.filter(a => famigliaDi(a.id) === f)}))
        .filter(g => g.righe.length > 0))
</script>

<template>
  <div class="abilita-page">
    <section v-for="grp in abilitaPerFamiglia" :key="grp.famiglia" class="abilita-section">
      <div class="section-header">
        <span class="section-title">{{ grp.label }}</span>
      </div>
      <Tabella
          :columns="[
        { field: 'nome', label: '' },
        { field: 'valore', label: '' },
        { field: 'caratteristica', label: '' }
      ]"
          :expandable="true"
          :items="grp.righe"
      />
    </section>
  </div>
</template>

<style scoped>
.abilita-page { display: flex; flex-direction: column; gap: 1rem; }
.abilita-section { display: flex; flex-direction: column; gap: .4rem; }
.section-header { display: flex; align-items: center; gap: .5rem; }
.section-title { font-size: .75rem; font-weight: 700; text-transform: uppercase; letter-spacing: .06em; color: #6b7280; }
</style>
