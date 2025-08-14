<script setup lang="ts">
import {computed, defineProps, markRaw, onMounted, watch} from 'vue'
import Tabella from '../../../../../components/Tabella.vue'
import {storeToRefs} from 'pinia'
import {useCharacterStore} from "../../../../../stores/personaggio";
import {testoModificatore} from "../../../../../function/Utils";
import Mobile_DettaglioAbilita from "../Dettaglio/Mobile_DettaglioAbilita.vue";

const props = defineProps<{ idPersonaggio: number }>()

const characterStore = useCharacterStore()
const {cache} = storeToRefs(characterStore)

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
      valore: testoModificatore(abilita.abilita.modificatore),
      caratteristica: abilita?.base?.id ?? '',
      expandedComponent: markRaw(Mobile_DettaglioAbilita),
      expandedProps: {data: {...abilita}}
    }
  }).sort((a, b) => a.nome.localeCompare(b.nome));
})
</script>

<template>
  <div>

    <Tabella
        :columns="[
      { field: 'nome', label: 'Abilità' },
      { field: 'valore', label: '' },
      { field: 'caratteristica', label: '' }
    ]"
        :expandable="true"
        :items="abilita"
    />
  </div>
</template>
