<script setup lang="ts">
// Editor del campo "visibilita" (item intero o singola nota: stesso significato, stesso formato
// stringa). Valori: '' = tutti, 'OWNER' = proprietario del personaggio, 'MASTER' = master/admin,
// oppure un formato a tag ";P<idParty>;...;U<idUtente>;" per "N party specifici" / "un giocatore
// specifico" — i delimitatori ';' su entrambi i lati evitano falsi positivi tra id che
// condividono le stesse cifre (party 1 non deve combaciare con party 12). La lettura/interpretazione
// gemella di questo formato è in AuthzService#canViewVisibilita (backend).
import {computed, onMounted, ref, watch} from 'vue'
import SearchSelect from './SearchSelect.vue'
import {useMondoStore} from '../stores/mondo'
import {getMembri, getPartyGiocanti, MembroParty, PartySelect} from '../service/PartyService'

const props = withDefaults(defineProps<{ modelValue: string; disabled?: boolean }>(), {disabled: false})
const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const mondoStore = useMondoStore()

type Categoria = 'TUTTI' | 'OWNER' | 'MASTER' | 'PARTY' | 'GIOCATORE'

const CATEGORIE_OPT: { value: Categoria; label: string }[] = [
  {value: 'TUTTI', label: 'Tutti'},
  {value: 'OWNER', label: 'Proprietario del personaggio'},
  {value: 'MASTER', label: 'Master'},
  {value: 'PARTY', label: 'Party specifici'},
  {value: 'GIOCATORE', label: 'Un giocatore specifico'},
]

function parse(v: string | null | undefined): { categoria: Categoria; partyIds: number[]; utenteId: number | null } {
  const t = (v ?? '').trim()
  if (!t) return {categoria: 'TUTTI', partyIds: [], utenteId: null}
  const up = t.toUpperCase()
  if (up === 'OWNER') return {categoria: 'OWNER', partyIds: [], utenteId: null}
  if (up === 'MASTER') return {categoria: 'MASTER', partyIds: [], utenteId: null}
  // (?=;) invece di consumare il ';' finale: due tag consecutivi condividono lo stesso ';' di
  // confine (es. ";P1;P4;"), e matchAll non produce match sovrapposti — con un pattern che
  // consuma il ';' condiviso, il secondo tag non verrebbe mai trovato (stesso bug corretto lato
  // backend in AuthzService#canViewVisibilita/descriviVisibilitaChips).
  const partyIds = [...up.matchAll(/;P(\d+)(?=;)/g)].map(m => Number(m[1]))
  const uMatch = up.match(/;U(\d+)(?=;)/)
  if (uMatch) return {categoria: 'GIOCATORE', partyIds, utenteId: Number(uMatch[1])}
  if (partyIds.length) return {categoria: 'PARTY', partyIds, utenteId: null}
  return {categoria: 'TUTTI', partyIds: [], utenteId: null} // valore non riconosciuto (dato legacy): tratta come "tutti"
}

const stato = ref(parse(props.modelValue))
// Non risincronizzare mentre l'utente sta ancora scegliendo (es. ha selezionato PARTY ma non
// ancora nessun party: build() torna '' e un watch ingenuo su modelValue lo farebbe ripartire da
// TUTTI). Si aggiorna solo se il valore esterno cambia per un motivo diverso dal nostro stesso emit.
let ultimoEmesso = build()
watch(() => props.modelValue, v => {
  if (v === ultimoEmesso) return
  stato.value = parse(v)
})

function build(): string {
  const s = stato.value
  if (s.categoria === 'TUTTI') return ''
  if (s.categoria === 'OWNER') return 'OWNER'
  if (s.categoria === 'MASTER') return 'MASTER'
  if (s.categoria === 'PARTY') return s.partyIds.length ? ';' + s.partyIds.map(id => `P${id};`).join('') : ''
  if (s.categoria === 'GIOCATORE') return s.utenteId != null ? `;U${s.utenteId};` : ''
  return ''
}

function emitUpdate() {
  ultimoEmesso = build()
  emit('update:modelValue', ultimoEmesso)
}

function onCategoria(v: string) {
  stato.value = {categoria: v as Categoria, partyIds: [], utenteId: null}
  emitUpdate()
}

function togglePartyId(id: number) {
  const set = new Set(stato.value.partyIds)
  if (set.has(id)) set.delete(id); else set.add(id)
  stato.value = {...stato.value, partyIds: [...set]}
  emitUpdate()
}

function onGiocatore(utenteId: number) {
  stato.value = {...stato.value, utenteId}
  emitUpdate()
}

// Party giocanti del mondo corrente (switcher globale): niente selettore locale qui, si segue
// sempre il mondo attivo, stesso principio già applicato al Compendio.
const partyGiocanti = ref<PartySelect[]>([])
async function caricaPartyGiocanti() {
  if (mondoStore.corrente === null) { partyGiocanti.value = []; return }
  try {
    partyGiocanti.value = (await getPartyGiocanti(mondoStore.corrente)).data ?? []
  } catch (e) {
    console.error('Errore caricamento party giocanti:', e)
    partyGiocanti.value = []
  }
}
onMounted(caricaPartyGiocanti)
watch(() => mondoStore.corrente, caricaPartyGiocanti)

// Membri aggregati dei party giocanti, per "un giocatore specifico": tollerante ai party a cui
// l'utente corrente non ha accesso (getMembri richiede di essere membro di QUEL party — per un
// giocatore normale, party diversi dal proprio verranno semplicemente saltati invece di rompere
// l'intero elenco).
const membri = ref<MembroParty[]>([])
const membriOptions = computed(() => membri.value.map(m => ({value: m.utenteId, label: m.name})))
async function caricaMembri() {
  if (stato.value.categoria !== 'GIOCATORE' || !partyGiocanti.value.length) { membri.value = []; return }
  const liste = await Promise.all(
      partyGiocanti.value.map(p => getMembri(p.id).then(r => r.data ?? []).catch(() => []))
  )
  const visti = new Map<number, MembroParty>()
  for (const lista of liste) for (const m of lista) visti.set(m.utenteId, m)
  membri.value = [...visti.values()]
}
watch([() => stato.value.categoria, partyGiocanti], caricaMembri, {immediate: true})
</script>

<template>
  <div class="visibilita-picker">
    <SearchSelect :model-value="stato.categoria" :options="CATEGORIE_OPT" :disabled="disabled" :sort="false"
                  @update:model-value="onCategoria($event as string)"/>

    <div v-if="stato.categoria === 'PARTY'" class="party-checks">
      <label v-for="p in partyGiocanti" :key="p.id" class="chk-row">
        <input type="checkbox" :checked="stato.partyIds.includes(p.id)" :disabled="disabled"
               @change="togglePartyId(p.id)"/>
        <span>{{ p.nome }}</span>
      </label>
      <p v-if="!partyGiocanti.length" class="hint">Nessun party giocante nel mondo corrente.</p>
    </div>

    <div v-else-if="stato.categoria === 'GIOCATORE'">
      <SearchSelect :model-value="stato.utenteId" :options="membriOptions" placeholder="Scegli un giocatore…"
                    :disabled="disabled" :sort="false" @update:model-value="onGiocatore($event as number)"/>
      <p v-if="!membriOptions.length" class="hint">Nessun giocatore trovato nei party giocanti del mondo corrente.</p>
    </div>
  </div>
</template>

<style scoped>
.visibilita-picker { display: grid; gap: .4rem; }
.party-checks { display: grid; gap: .3rem; padding: .3rem 0; }
.chk-row { display: flex; align-items: center; gap: .4rem; font-size: .85rem; }
.hint { margin: 0; font-size: .78rem; opacity: .65; }
</style>
