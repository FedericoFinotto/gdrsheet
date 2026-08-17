<script setup lang="ts">
interface ClasseCustom { codice: string; livello: string }

const props = defineProps<{
  classi: Record<string, string>  // reactive del genitore, mutata per riferimento
  classiCustom: ClasseCustom[]  // reactive del genitore, cresciuto/ridotto per riferimento (push/splice)
  classCodes: string[]
  friendlyName: (code: string) => string
  disabled?: boolean
}>()

function addClasseCustom() {
  props.classiCustom.push({codice: '', livello: ''})
}
function removeClasseCustom(i: number) {
  props.classiCustom.splice(i, 1)
}
</script>

<template>
  <fieldset class="components">
    <legend class="sr-only">Classi / Domìni</legend>
    <div class="class-grid">
      <label v-for="code in classCodes" :key="code" class="class-row">
        <span class="class-name">{{ friendlyName(code) }}</span>
        <select v-model="classi[code]" :disabled="disabled" class="level-select">
          <option value="">—</option>
          <option v-for="n in 10" :key="n-1" :value="String(n-1)">{{ n - 1 }}</option>
        </select>
      </label>
    </div>
  </fieldset>

  <fieldset class="components custom-classi">
    <legend class="sr-only">Liste personalizzate</legend>
    <p class="muted">
      Liste/classi non nel catalogo qui sopra (es. per un oggetto con una lista incantesimi
      propria): il codice va scritto come lo useresti nella sezione "Liste incantesimi"
      dell'oggetto o della classe.
    </p>
    <div v-for="(c, i) in classiCustom" :key="i" class="class-row custom-row">
      <input v-model.trim="c.codice" type="text" placeholder="Es. SP_ANELLO_CUSTOM" :disabled="disabled" class="custom-code"/>
      <select v-model="c.livello" :disabled="disabled" class="level-select">
        <option value="">—</option>
        <option v-for="n in 10" :key="n-1" :value="String(n-1)">{{ n - 1 }}</option>
      </select>
      <button type="button" class="btn-del" :disabled="disabled" @click="removeClasseCustom(i)" title="Rimuovi">✕</button>
    </div>
    <button type="button" class="btn outline" :disabled="disabled" @click="addClasseCustom">+ Aggiungi lista personalizzata</button>
  </fieldset>
</template>

<style scoped>
.components { margin: 0; border: 0; padding: 0; }
.components legend { display: none; }
.muted { opacity: .7; font-size: .85rem; }

.class-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: .5rem .75rem; }
.class-row {
  display: grid; grid-template-columns: 1fr 5rem; align-items: center; gap: .5rem;
  padding: .25rem .4rem; border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0);
}
.class-name { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.level-select { width: 100%; padding: .5rem .6rem; border: 1px solid var(--hairline); border-radius: .5rem; background: var(--surface-0); }

.custom-classi { margin-top: .75rem; padding-top: .6rem; border-top: 1px dashed var(--hairline); display: grid; gap: .5rem; }
.custom-row { grid-template-columns: 1fr 5rem auto; }
.custom-code { width: 100%; padding: .35rem .5rem; border: 1px solid var(--hairline); border-radius: .4rem; }
.btn-del {
  border: 1px solid var(--danger-border); background: var(--danger-bg); color: var(--danger-text);
  border-radius: .5rem; padding: .25rem .5rem; cursor: pointer;
}
.btn { padding: .5rem .9rem; border-radius: .5rem; border: 1px solid transparent; cursor: pointer; }
.btn.outline { border-color: var(--info-border); background: var(--info-bg); color: var(--info-text); font-weight: 600; }
.btn:disabled { opacity: .6; cursor: default; }
.sr-only {
  position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); border: 0;
}
</style>
