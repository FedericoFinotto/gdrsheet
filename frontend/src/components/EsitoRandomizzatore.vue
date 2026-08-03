<script setup lang="ts">
/**
 * Un nodo dell'albero di estrazione. Si richiama da solo per i figli, cioè per i
 * randomizzatori innescati dall'oggetto estratto.
 */
import type {Esito} from '../service/RandomizzatoreService'

defineProps<{ esito: Esito; livello?: number }>()
</script>

<template>
  <div class="nodo" :class="{ annidato: (livello ?? 0) > 0, vuoto: !esito.idItem }">
    <div v-if="esito.randomizzatore && (livello ?? 0) > 0" class="da-randomizzatore">
      da <strong>{{ esito.randomizzatore }}</strong>
    </div>

    <template v-if="esito.idItem">
      <div class="testa">
        <span class="nome">{{ esito.nome }}</span>
        <span class="muted candidati">tra {{ esito.candidati }} candidat{{ esito.candidati === 1 ? 'o' : 'i' }}</span>
      </div>

      <div v-if="esito.scelti.length || esito.estratti.length" class="tag-riga">
        <span v-for="t in esito.scelti" :key="'s'+t.idTag" class="chip scelto"
              :title="`${t.categoria}${t.automatico ? ' (sorteggiato d\'ufficio)' : ''}`">
          {{ t.tag }}<span v-if="t.automatico" class="auto">*</span>
        </span>
        <span v-for="t in esito.estratti" :key="'e'+t.idTag" class="chip estratto" :title="t.categoria">
          {{ t.tag }}
        </span>
      </div>

      <div v-if="esito.descrizione" class="descrizione" v-safe-html="esito.descrizione"></div>
    </template>

    <div v-if="esito.avviso" class="avviso">{{ esito.avviso }}</div>

    <!-- randomizzatori innescati da questo oggetto -->
    <div v-if="esito.figli && esito.figli.length" class="figli">
      <EsitoRandomizzatore v-for="(f, i) in esito.figli" :key="i" :esito="f" :livello="(livello ?? 0) + 1"/>
    </div>
  </div>
</template>

<style scoped>
.nodo {
  border: 1px solid var(--success-border); background: var(--success-bg);
  border-radius: .6rem; padding: .6rem .75rem; display: grid; gap: .4rem;
}
.nodo.annidato { border-color: var(--hairline); background: var(--surface-0); }
.nodo.vuoto { border-color: var(--coin-mr-border); background: var(--coin-mr-bg); }

.da-randomizzatore { font-size: .7rem; text-transform: uppercase; letter-spacing: .04em; opacity: .6; }
.testa { display: flex; align-items: baseline; justify-content: space-between; gap: .5rem; flex-wrap: wrap; }
.nome { font-weight: 700; font-size: 1.05rem; }
.muted { opacity: .65; font-size: .8rem; }
.candidati { white-space: nowrap; }

.tag-riga { display: flex; flex-wrap: wrap; gap: .3rem; }
.chip {
  font-size: .72rem; font-weight: 600; padding: .12rem .5rem;
  border-radius: 999px; background: var(--btn-bg); color: var(--text-muted); white-space: nowrap;
}
.chip.scelto { background: var(--info-bg); color: var(--info-text); }
.chip.estratto { background: var(--success-bg); color: var(--success-text); }
.auto { opacity: .7; margin-left: .1rem; }

.descrizione { font-size: .88rem; white-space: pre-wrap; }
.descrizione :deep(p) { margin: .25rem 0; }

.avviso { font-size: .8rem; color: var(--coin-mr-text); }

.figli {
  display: grid; gap: .4rem;
  margin-top: .2rem; padding-left: .6rem;
  border-left: 2px solid var(--hairline);
}
</style>
