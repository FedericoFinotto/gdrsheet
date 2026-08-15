<script setup lang="ts">
import {computed, ref} from 'vue'
import {setSlotUsatiPerLivello} from '../../../../../service/PersonaggioService'
import {useCharacterStore} from '../../../../../stores/personaggio'

// Popup aperto tenendo premuto su "slot: X" in Mobile_Cico_4_SpellBook.vue quando la sezione
// traccia gli slot con contatore (SPELL_<n>_SLOT_CONTATORE) — mostra la scomposizione slot base/
// bonus/totale e permette di modificare gli slot usati, sia ±1 sia in blocco con un valore libero.
const props = defineProps<{
  itemId: number
  personaggioId: number
  sezioneIndice: number
  livello: number
  slot: number     // slot base (senza bonus)
  bonus: number     // bonus da formula/caratteristica
  usati: number     // slot già usati, all'apertura
}>()

const characterStore = useCharacterStore()

const totale = computed(() => props.slot + props.bonus)
const usatiLocale = ref(props.usati)
// "Attuale": slot ancora disponibili (Totale - Usati) — è quello che l'utente vede/modifica,
// gli "usati" sono un dettaglio di persistenza interno.
const attuale = computed(() => Math.max(0, totale.value - usatiLocale.value))

const saving = ref(false)
const valoreInput = ref<number | null>(null)

async function salva(nuoviUsati: number) {
  if (saving.value) return
  const clamped = Math.max(0, Math.min(totale.value, nuoviUsati))
  if (clamped === usatiLocale.value) return
  saving.value = true
  usatiLocale.value = clamped
  try {
    await setSlotUsatiPerLivello(props.itemId, props.personaggioId, props.sezioneIndice, props.livello, clamped)
    await characterStore.fetchCharacter(props.personaggioId, true)
  } catch (e) {
    console.error('Errore salvataggio slot usati:', e)
  } finally {
    saving.value = false
  }
}

// -1 su Attuale = +1 usato; +1 su Attuale = -1 usato (rigenera uno slot)
function decrementaAttuale() { salva(usatiLocale.value + 1) }
function incrementaAttuale() { salva(usatiLocale.value - 1) }

function aggiungi() {
  const n = Number(valoreInput.value)
  if (!Number.isFinite(n) || n <= 0) return
  salva(usatiLocale.value - n)
}
function sottrai() {
  const n = Number(valoreInput.value)
  if (!Number.isFinite(n) || n <= 0) return
  salva(usatiLocale.value + n)
}
</script>

<template>
  <div class="slot-popup">
    <div class="riga">
      <span class="lbl">Slot</span>
      <span class="val">{{ slot }}</span>
    </div>
    <div class="riga">
      <span class="lbl">Bonus</span>
      <span class="val">{{ bonus }}</span>
    </div>
    <div class="riga riga-totale">
      <span class="lbl">Totale</span>
      <span class="val">{{ totale }}</span>
    </div>

    <div class="stepper">
      <button class="btn" :disabled="saving || attuale <= 0" @click="decrementaAttuale">−</button>
      <span class="conteggio">{{ attuale }} / {{ totale }}</span>
      <button class="btn" :disabled="saving || attuale >= totale" @click="incrementaAttuale">+</button>
    </div>

    <div class="blocco">
      <input class="input-valore" type="number" min="1" v-model.number="valoreInput"
             placeholder="Valore" :disabled="saving"/>
      <button class="btn" :disabled="saving" @click="aggiungi">Aggiungi</button>
      <button class="btn" :disabled="saving" @click="sottrai">Sottrai</button>
    </div>
  </div>
</template>

<style scoped>
.slot-popup { display: grid; gap: .6rem; min-width: 14rem; }
.riga { display: flex; justify-content: space-between; align-items: baseline; gap: .5rem; }
.riga .lbl { font-weight: 600; opacity: .85; }
.riga .val { font-weight: 700; font-variant-numeric: tabular-nums; }
.riga-totale { border-top: 1px solid var(--hairline); padding-top: .4rem; }
.riga-totale .val { font-weight: 800; }

.stepper {
  display: flex; align-items: center; justify-content: center; gap: .6rem;
  border: 1px solid var(--info-border); background: var(--info-bg);
  border-radius: .6rem; padding: .4rem .6rem;
}
.conteggio { font-weight: 800; font-variant-numeric: tabular-nums; min-width: 4rem; text-align: center; }

.blocco { display: flex; align-items: center; gap: .4rem; flex-wrap: wrap; }
.input-valore {
  width: 5rem; text-align: center; border: 1px solid var(--hairline); border-radius: .5rem;
  padding: .3rem; font-weight: 600;
}

.btn {
  border: 1px solid var(--hairline); background: var(--surface-0); border-radius: .5rem;
  padding: .35rem .65rem; cursor: pointer; font-weight: 600; font-size: .85rem;
}
.btn:disabled { opacity: .5; cursor: default; }
.btn:not(:disabled):hover { background: var(--btn-bg-hover); }
</style>
