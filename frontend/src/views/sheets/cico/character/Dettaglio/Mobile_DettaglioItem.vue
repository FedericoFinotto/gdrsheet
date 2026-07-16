<script setup lang="ts">
import {computed, defineProps, onMounted, ref} from 'vue';
import {
  buildMappaItemAvanzamenti,
  buildMappaModificatoriAvanzamenti,
  flattenTrasformazioni,
  iconForComponent,
  mostraLabel,
  testoFormula,
  testoModificatore,
  testoModificatoreConTipo
} from "../../../../../function/Utils";
import {getItem, switchItemState} from "../../../../../service/PersonaggioService";
import {ItemDB, TIPO_ITEM} from "../../../../../models/entity/ItemDB";
import {useCharacterStore} from "../../../../../stores/personaggio";
import {storeToRefs} from "pinia";
import Icona from "../../../../../components/Icona/Icona.vue";
import {useRouter} from "vue-router";
import usePopup from "../../../../../function/usePopup";
import Mobile_DettaglioItem from "./Mobile_DettaglioItem.vue";
import {Modificatore} from "../../../../../models/entity/Modificatore";
import {Avanzamento} from "../../../../../models/entity/Avanzamento";
import {Item} from "../../../../../models/dto/Item";
import {getItemLabel, getItemLabels, LABELS, thereIsValoreLabel} from "../../../../../models/entity/ItemLabel";
import {useHp} from "../../../../../function/useHp";

const router = useRouter()
const {openPopup} = usePopup()

const props = defineProps<{
  data: {
    item: Item;            // l'oggetto item con id e tipo
    personaggio: any;
  };
  // true quando il popup è usato come semplice "vedi info" fuori dal contesto scheda/inventario
  // (es. editor livello): nasconde la action-bar (Attiva/Disattiva, Modifica), dato che lì lo stato
  // "disabled" dell'item passato non è affidabile e le due azioni non hanno senso nel contesto.
  readonly?: boolean;
}>();
const {item, personaggio} = props.data;

const characterStore = useCharacterStore();
const {cache} = storeToRefs(characterStore);

// --- barriera: controlli +/- reset/distruggi sul talento ---
const idPersonaggioCorrente = personaggio?.modificatori?.id;
const hpApi = idPersonaggioCorrente ? useHp(idPersonaggioCorrente) : null;
const barriera = computed(() =>
    hpApi?.barriere.value.find((b: any) => b.id === item.id) ?? null);


const listaAbilita = ref<ItemDB[]>([]);
const listaAttacchi = ref<ItemDB[]>([]);
const listaMaledizioni = ref<ItemDB[]>([]);
const mappaAvanzamenti = ref<Record<number, Avanzamento[]>>({});
const mappaModificatori = ref<Record<number, Modificatore[]>>({});
const listaAvanzamenti = ref<Avanzamento[]>([]);

const itemDetail = ref<ItemDB | null>(null);
const loading = ref(true);

// Mappe per labels
const labelMap = ref<Record<string, string>>({});

// Mappe per TPC/TPD
const tpcMap = ref<Record<number, string>>({});
const tpdMap = ref<Record<number, string>>({});

async function switchState() {
  let itemIdToSwitch = [];
  if (item.disabled) {
    switch (item.tipo) {
      case 'TRASFORMAZIONE':
        // Le trasformazioni sono raggruppate dal backend (indipendenti + quelle di un frutto):
        // le appiattiamo per cercare i "sibling" con lo stesso gruppo a prescindere da dove si trovino.
        itemIdToSwitch.push(...flattenTrasformazioni(personaggio.items).filter(x => !x.disabled && x.gruppo === item.gruppo).map(x => x.id));
        break;
      case 'IDOLO':
        itemIdToSwitch.push(...personaggio.items.idoli.filter(x => !x.disabled).map(x => x.id));
        break;
    }
  }
  itemIdToSwitch.push(item.id);
  console.log("switchState", itemIdToSwitch);
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
  switch (item?.tipo) {
    case TIPO_ITEM.EQUIPAGGIAMENTO:
    case TIPO_ITEM.ARMA:
    case TIPO_ITEM.MUNIZIONE:
      return item.disabled ? 'Equipaggia' : 'Togli';
    case TIPO_ITEM.TRASFORMAZIONE:
      return item.disabled ? 'Trasformati' : 'Torna alla forma Base';
    case TIPO_ITEM.IDOLO:
      return item.disabled ? 'Prega' : 'Disabilita';
    case TIPO_ITEM.FRUTTO:
      return item.disabled ? 'Mangia' : 'Disattiva';
    case TIPO_ITEM.INCANTESIMO:
      return null;
    default:
      return item.disabled ? 'Attiva' : 'Disattiva';
  }
});

function goToEditor() {
  router.push(`/itemeditor/${itemDetail.value?.id}?personaggio=${personaggio.modificatori.id}`);
}

onMounted(async () => {
  try {
    const response = await getItem(item.id);
    const data = response.data;
    itemDetail.value = data;

    // Popola labelMap con tutte le labels
    data.labels?.forEach(lbl => {
      // assumiamo lbl.label come key e lbl.valore come value
      labelMap.value[lbl.label] = lbl.valore;
    });

    // Lista dei children
    const children = data.child ?? [];
    const avanzamenti = data.avanzamento ?? [];
    listaAbilita.value = children
        .filter(c => c.itemTarget.tipo === TIPO_ITEM.ABILITA)
        .map(c => c.itemTarget);
    listaAttacchi.value = children
        .filter(c => c.itemTarget.tipo === TIPO_ITEM.ATTACCO)
        .map(c => c.itemTarget);
    listaMaledizioni.value = children
        .filter(c => c.itemTarget.tipo === TIPO_ITEM.MALEDIZIONE)
        .map(c => c.itemTarget);
    listaAvanzamenti.value = avanzamenti;
    mappaAvanzamenti.value = buildMappaItemAvanzamenti(avanzamenti)
    mappaModificatori.value = buildMappaModificatoriAvanzamenti(avanzamenti)

    // Popola mappe TPC/TPD in modo asincrono
    for (const atk of listaAttacchi.value) {
      if (thereIsValoreLabel(atk, LABELS.TIRO_COLPIRE)) {
        tpcMap.value[atk.id] = testoFormula(getItemLabel(atk, LABELS.TIRO_COLPIRE))
      }
      if (thereIsValoreLabel(atk, LABELS.TIRO_DANNI)) {
        tpdMap.value[atk.id] = testoFormula(getItemLabel(atk, LABELS.TIRO_DANNI));
      }
    }
  } catch (e) {
    console.error('Errore caricamento item:', e);
  } finally {
    loading.value = false;
  }
});

// Calcola il contenuto del contenitore corrente (solo se tipo=CONTENITORE)
const contenitorInfo = computed(() => {
  if (item.tipo !== TIPO_ITEM.CONTENITORE) return null
  const allItems = personaggio?.items
  if (!allItems) return null

  const capienza = item.capienza ?? 0
  const pesoMassimo = item.peso ?? 0
  const includiArmi       = !!item.includiArmiAbilitate
  const includiOggetti    = !!item.includiOggettiAbilitati
  const includiConsumabili = !!item.includiConsumabiliAbilitati
  const includiTutti      = !!item.includiTuttiAbilitati

  // Peso monete: arriva direttamente dal backend come campo dedicato
  const coinWeight = personaggio?.modificatori?.pesoMonete ?? 0

  // Tutti i contenitori ordinati per capienza decrescente
  type ContInfo = {id: number; capienza: number; pesoMassimo: number; includiArmi: boolean; includiOggetti: boolean; includiConsumabili: boolean; includiTutti: boolean}
  const contenitori: ContInfo[] =
    [...(allItems.contenitori ?? [])]
      .filter(c => (c.capienza ?? 0) > 0)
      .map(c => ({
        id: c.id,
        capienza: c.capienza ?? 0,
        pesoMassimo: c.peso ?? 0,
        includiArmi: !!c.includiArmiAbilitate,
        includiOggetti: !!c.includiOggettiAbilitati,
        includiConsumabili: !!c.includiConsumabiliAbilitati,
        includiTutti: !!c.includiTuttiAbilitati,
      }))
      .sort((a, b) => b.capienza - a.capienza)

  // Tutti gli item con peso
  const allWithPeso = [
    ...(allItems.oggetti ?? []),
    ...(allItems.consumabili ?? []),
    ...(allItems.munizioni ?? []),
    ...(allItems.armi ?? []),
    ...(allItems.equipaggiamento ?? []),
    ...(allItems.frutti ?? []),
    ...(allItems.idoli ?? []),
    ...(allItems.patti ?? []),
  ].filter(i => (i.peso ?? 0) > 0)

  // item disabilitati → sempre contenibili (indipendentemente dal tipo)
  const containable      = allWithPeso.filter(i => !!i.disabled)
  // pool separati per item abilitati, per tipo
  const armeAbilitate        = allWithPeso.filter(i => !i.disabled && i.tipo === 'ARMA')
  const oggettiAbilitati     = allWithPeso.filter(i => !i.disabled && i.tipo === 'OGGETTO')
  const consumabiliAbilitati = allWithPeso.filter(i => !i.disabled && i.tipo === 'CONSUMABILE')
  // qualsiasi altro tipo abilitato con peso → entra solo nei contenitori con "includi tutti"
  const altroAbilitato = allWithPeso.filter(i => !i.disabled && !['ARMA', 'OGGETTO', 'CONSUMABILE'].includes(i.tipo))

  // Distribuzione greedy identica al backend
  type Slot = {id: number; capienza: number; includiArmi: boolean; includiOggetti: boolean; includiConsumabili: boolean; includiTutti: boolean;
               items: any[]; armiItems: any[]; oggettiItems: any[]; consumabiliItems: any[]; altroItems: any[]; filled: number}
  const slots: Slot[] = contenitori.map(c => ({
    ...c, items: [], armiItems: [], oggettiItems: [], consumabiliItems: [], altroItems: [], filled: 0
  }))

  // Helper: distribuisci un pool nei slot che accettano quel tipo
  function distribuisci(pool: any[], getSlot: (s: Slot) => boolean, pushTo: (s: Slot, i: any) => void) {
    const rem = [...pool]
    for (const slot of slots) {
      if (!getSlot(slot)) continue
      const toRemove: any[] = []
      for (const itm of rem) {
        const itemPeso = (itm.peso ?? 0) * (itm.quantita ?? 1)
        if (slot.filled + itemPeso <= slot.capienza + 1e-9) {
          slot.filled += itemPeso
          pushTo(slot, itm)
          toRemove.push(itm)
        }
      }
      toRemove.forEach(i => rem.splice(rem.indexOf(i), 1))
    }
  }

  // 1. item disabilitati → tutti i slot
  distribuisci(containable,      () => true,              (s, i) => s.items.push(i))
  // 2. armi abilitate → slot con flag armi o "tutti"
  distribuisci(armeAbilitate,    s => s.includiArmi || s.includiTutti,      (s, i) => s.armiItems.push(i))
  // 3. oggetti abilitati → slot con flag oggetti o "tutti"
  distribuisci(oggettiAbilitati, s => s.includiOggetti || s.includiTutti,   (s, i) => s.oggettiItems.push(i))
  // 4. consumabili abilitati → slot con flag consumabili o "tutti"
  distribuisci(consumabiliAbilitati, s => s.includiConsumabili || s.includiTutti, (s, i) => s.consumabiliItems.push(i))
  // 5. qualsiasi altro tipo abilitato con peso → slot solo con "tutti"
  distribuisci(altroAbilitato, s => s.includiTutti, (s, i) => s.altroItems.push(i))

  // Distribuisci monete negli spazi rimasti
  let remainingCoins = coinWeight
  const slotsCoins: number[] = slots.map(() => 0)
  for (let i = 0; i < slots.length; i++) {
    const space = slots[i].capienza - slots[i].filled
    const coinIn = Math.min(space, remainingCoins)
    slotsCoins[i] = coinIn
    slots[i].filled += coinIn
    remainingCoins -= coinIn
  }

  const myIdx = slots.findIndex(s => s.id === item.id)
  if (myIdx === -1) return {items: [], armiItems: [], oggettiItems: [], consumabiliItems: [], altroItems: [],
                            filled: 0, capienza, pesoMassimo, pesoEffettivo: 0, coinInside: 0}

  const mySlot = slots[myIdx]
  const coinInside = slotsCoins[myIdx]
  const pesoEffettivo = capienza > 0 ? pesoMassimo * Math.min(1, mySlot.filled / capienza) : 0

  return {
    items: mySlot.items,
    armiItems: mySlot.armiItems,
    oggettiItems: mySlot.oggettiItems,
    consumabiliItems: mySlot.consumabiliItems,
    altroItems: mySlot.altroItems,
    filled: mySlot.filled,
    capienza,
    pesoMassimo,
    pesoEffettivo,
    coinInside,
    includiArmi,
    includiOggetti,
    includiConsumabili,
    includiTutti,
  }
})

// Talento: dati stile dndtools.org (vedi scripts/dndtools-scraper), da item_label
const talentoInfo = computed(() => {
  if (!itemDetail.value || itemDetail.value.tipo !== TIPO_ITEM.TALENTO) return null
  const d = itemDetail.value
  return {
    manuale: getItemLabel(d, LABELS.MANUALE),
    pagina: getItemLabel(d, LABELS.PAGE),
    link: getItemLabel(d, LABELS.LINK),
    categorie: getItemLabels(d, LABELS.CATEGORY) ?? [],
    prerequisito: getItemLabel(d, LABELS.PREREQUISITE),
    richiestoPer: getItemLabel(d, LABELS.REQUIRED_FOR),
    beneficio: getItemLabel(d, LABELS.BENEFIT),
    normale: getItemLabel(d, LABELS.NORMAL),
    speciale: getItemLabel(d, LABELS.SPECIAL),
    extra: getItemLabels(d, LABELS.EXTRA) ?? [],
  }
})

function showInfoItemPopup(itm) {
  openPopup(
      Mobile_DettaglioItem,
      {data: {item: {...itm}, personaggio: props.data.personaggio}},
      {closable: true, autoClose: 0}
  )
}
</script>

<template>
  <div class="abilita-detail-card" v-if="!loading && itemDetail">
    <!-- Barra azioni: togli/metti + modifica -->
    <div v-if="!readonly" class="action-bar">
      <button
          v-if="disableLabel"
          type="button"
          class="action-btn toggle"
          :class="item.disabled ? 'enable' : 'disable'"
          @click="switchState"
      >{{ disableLabel }}</button>
      <button type="button" class="action-btn edit" @click.stop="goToEditor">
        <Icona name="EDIT"/>
        <span>Modifica</span>
      </button>
    </div>

    <!-- Barriera: controlli HP temporanei "blu" -->
    <div v-if="barriera && hpApi" class="barriera-box">
      <div class="barr-head">
        <span class="titolo">🛡 Barriera</span>
        <span class="val">{{ barriera.current }} / {{ barriera.max }}</span>
      </div>
      <div class="barr-track">
        <div class="barr-fill" :style="{ width: (barriera.max > 0 ? (barriera.current / barriera.max) * 100 : 0) + '%' }"/>
      </div>
      <div class="barr-actions">
        <button class="btn" :disabled="barriera.current <= 0" @click="hpApi.modifyBarriera(barriera.id, -1)">−1</button>
        <button class="btn" :disabled="barriera.current >= barriera.max" @click="hpApi.modifyBarriera(barriera.id, +1)">+1</button>
        <button class="btn" :disabled="barriera.current >= barriera.max" @click="hpApi.resetBarriera(barriera.id)">Reset</button>
        <button class="btn danger" :disabled="barriera.current <= 0" @click="hpApi.distruggiBarriera(barriera.id)">Distruggi</button>
      </div>
    </div>

    <div v-if="itemDetail?.labels?.length" style="display: flex">
      <div v-for="comp in itemDetail.labels">
        <Icona v-if="comp.label==='COMP_SP'" :name="iconForComponent(comp.valore)"></Icona>
      </div>
    </div>

    <div v-if="Object.keys(mappaAvanzamenti).length">
      <table class="custom-table">
        <tbody>
        <tr>
          <td><strong>Livello</strong></td>
          <td><strong>Item derivati</strong></td>
        </tr>
        <tr v-for="(val, key) in mappaAvanzamenti" :key="`key`">
          <td>{{ key }}</td>
          <td>
            <template v-for="av in val.filter(x => x.itemTarget.tipo !== TIPO_ITEM.AVANZAMENTO)">
              <button
                  v-if="av.itemTarget"
                  type="button"
                  class="pill"
                  @click.stop="showInfoItemPopup(av.itemTarget)"
                  :title="`${av.itemTarget.nome}`"
              >
                {{ av.itemTarget.nome }}
              </button>
            </template>
          </td>
        </tr>
        </tbody>
      </table>
      <div class="spazietto"/>
    </div>

    <div v-if="Object.keys(mappaModificatori).length">
      <table class="custom-table">
        <tbody>
        <tr>
          <td><strong>Livello</strong></td>
          <td><strong>Modificatori</strong></td>
        </tr>
        <tr v-for="(val, key) in mappaModificatori" :key="`key`">
          <td>{{ key }}</td>
          <td>
            <template v-for="av in val">
              <span
              >
                <p>{{ testoFormula(testoModificatore(av.valore)) }} <strong>{{ av.stat.label }}</strong></p>
              </span>
            </template>
          </td>
        </tr>
        </tbody>
      </table>
      <div class="spazietto"/>
    </div>

    <!-- Labels dinamiche -->
    <div v-if="Object.keys(labelMap).length">
      <div v-for="(val, key) in labelMap" :key="key">
        <span v-if="mostraLabel(key, val)"><strong>{{
            mostraLabel(key, val).label
          }}:</strong> {{ mostraLabel(key, val).value }}</span>
      </div>
      <div class="spazietto"/>
    </div>

    <!-- Contenuto del contenitore -->
    <div v-if="contenitorInfo" class="contenitore-box">
      <div class="contenitore-header">
        <span>Contenuto</span>
        <span class="contenitore-peso">
          {{ contenitorInfo.filled.toFixed(2) }} / {{ contenitorInfo.capienza }} kg
          &nbsp;→&nbsp;
          <strong>{{ contenitorInfo.pesoEffettivo.toFixed(2) }} kg</strong>
        </span>
      </div>
      <div v-if="contenitorInfo.items.length || contenitorInfo.armiItems.length
                 || contenitorInfo.oggettiItems.length || contenitorInfo.consumabiliItems.length
                 || contenitorInfo.altroItems.length || contenitorInfo.coinInside > 0"
           class="contenitore-list">
        <!-- item disabilitati (tutti i tipi) -->
        <div v-for="itm in contenitorInfo.items" :key="itm.id" class="contenitore-item">
          <span class="contenitore-item-nome">{{ itm.nome }}</span>
          <span class="contenitore-item-peso muted">
            {{ ((itm.peso ?? 0) * (itm.quantita ?? 1)).toFixed(2) }} kg
            <template v-if="(itm.quantita ?? 1) > 1">(×{{ itm.quantita }})</template>
          </span>
        </div>
        <!-- armi abilitate (flag) -->
        <div v-for="itm in contenitorInfo.armiItems" :key="'a'+itm.id" class="contenitore-item arma-row">
          <span class="contenitore-item-nome">{{ itm.nome }}
            <span class="muted" style="font-size:.72rem">(arma equipaggiata)</span>
          </span>
          <span class="contenitore-item-peso muted">
            {{ ((itm.peso ?? 0) * (itm.quantita ?? 1)).toFixed(2) }} kg
          </span>
        </div>
        <!-- oggetti abilitati (flag) -->
        <div v-for="itm in contenitorInfo.oggettiItems" :key="'o'+itm.id" class="contenitore-item oggetto-row">
          <span class="contenitore-item-nome">{{ itm.nome }}
            <span class="muted" style="font-size:.72rem">(oggetto equipaggiato)</span>
          </span>
          <span class="contenitore-item-peso muted">
            {{ ((itm.peso ?? 0) * (itm.quantita ?? 1)).toFixed(2) }} kg
          </span>
        </div>
        <!-- consumabili abilitati (flag) -->
        <div v-for="itm in contenitorInfo.consumabiliItems" :key="'c'+itm.id" class="contenitore-item consumabile-row">
          <span class="contenitore-item-nome">{{ itm.nome }}
            <span class="muted" style="font-size:.72rem">(consumabile equipaggiato)</span>
          </span>
          <span class="contenitore-item-peso muted">
            {{ ((itm.peso ?? 0) * (itm.quantita ?? 1)).toFixed(2) }} kg
          </span>
        </div>
        <!-- qualsiasi altro tipo con peso (flag "Tutto quello che pesa") -->
        <div v-for="itm in contenitorInfo.altroItems" :key="'x'+itm.id" class="contenitore-item altro-row">
          <span class="contenitore-item-nome">{{ itm.nome }}</span>
          <span class="contenitore-item-peso muted">
            {{ ((itm.peso ?? 0) * (itm.quantita ?? 1)).toFixed(2) }} kg
          </span>
        </div>
        <!-- monete -->
        <div v-if="contenitorInfo.coinInside > 0" class="contenitore-item monete-row">
          <span class="contenitore-item-nome">Monete</span>
          <span class="contenitore-item-peso muted">{{ contenitorInfo.coinInside.toFixed(2) }} kg</span>
        </div>
      </div>
      <div v-else class="contenitore-empty muted">Vuoto</div>
    </div>

    <!-- Talento: header manuale/pagina/categorie in stile dndtools -->
    <div v-if="talentoInfo && (talentoInfo.manuale || talentoInfo.categorie.length)" class="talento-header">
      <span v-if="talentoInfo.manuale" class="talento-manuale">
        ({{ talentoInfo.manuale }}<template v-if="talentoInfo.pagina">, p. {{ talentoInfo.pagina }}</template>)
      </span>
      <span v-for="cat in talentoInfo.categorie" :key="cat" class="talento-categoria">{{ cat }}</span>
    </div>

    <!-- Descrizione -->
    <div v-if="itemDetail.descrizione">
      <strong>Descrizione</strong><br>
      <div class="descrizione-html" v-safe-html="itemDetail.descrizione"></div>
      <div style="height: 20px"></div>
      <div class="spazietto"/>
    </div>

    <!-- Talento: sezioni stile dndtools (una card per sezione presente) -->
    <template v-if="talentoInfo">
      <div v-if="talentoInfo.prerequisito" class="section-card">
        <div class="section-card-header">Prerequisito</div>
        <div class="section-card-body">{{ talentoInfo.prerequisito }}</div>
      </div>
      <div v-if="talentoInfo.richiestoPer" class="section-card">
        <div class="section-card-header">Richiesto per</div>
        <div class="section-card-body">{{ talentoInfo.richiestoPer }}</div>
      </div>
      <div v-if="talentoInfo.beneficio" class="section-card">
        <div class="section-card-header">Beneficio</div>
        <div class="section-card-body">{{ talentoInfo.beneficio }}</div>
      </div>
      <div v-if="talentoInfo.normale" class="section-card">
        <div class="section-card-header">Normale</div>
        <div class="section-card-body">{{ talentoInfo.normale }}</div>
      </div>
      <div v-if="talentoInfo.speciale" class="section-card">
        <div class="section-card-header">Speciale</div>
        <div class="section-card-body">{{ talentoInfo.speciale }}</div>
      </div>
      <div v-for="(ex, i) in talentoInfo.extra" :key="'extra'+i" class="section-card">
        <div class="section-card-header">Altro</div>
        <div class="section-card-body">{{ ex }}</div>
      </div>
      <div v-if="talentoInfo.link" class="talento-link">
        <a :href="talentoInfo.link" target="_blank" rel="noopener noreferrer">Fonte ↗</a>
      </div>
    </template>

    <!-- Attacchi -->
    <div v-if="listaAttacchi.length">
      <p><strong>Attacco:</strong></p>
      <p v-for="atk in listaAttacchi" :key="atk.id">
        {{ atk.nome }}
        <span v-if="tpcMap[atk.id]">(TPC: {{ tpcMap[atk.id] }})</span>
        <span v-if="tpdMap[atk.id]">(TPD: {{ tpdMap[atk.id] }})</span>
      </p>
      <div class="spazietto"/>
    </div>

    <!-- Abilità -->
    <div v-if="listaAbilita && listaAbilita.length">
      <p><strong>Abilità:</strong></p>

      <p v-for="ability in listaAbilita" :key="ability.id">
        {{ ability.nome }}
        <Icona name="INFO" @click.stop="showInfoItemPopup(ability)"/>
      </p>

      <div class="spazietto"></div>
    </div>


    <!-- Maledizioni -->
    <div v-if="listaMaledizioni.length">
      <p><strong>Maledizioni:</strong></p>
      <p v-for="mal in listaMaledizioni" :key="mal.id">
        {{ mal.nome }}
      </p>
      <div class="spazietto"/>
    </div>

    <!-- Modificatori -->
    <div v-if="itemDetail.modificatori?.length">
      <strong>Modificatori:</strong><br>
      <span v-for="mod in itemDetail.modificatori" :key="mod.id">
        <strong>{{ mod.stat.label }}:</strong>
        {{ testoModificatoreConTipo(mod.valore, mod.tipo) }}
        <span v-if="mod.nota">- {{ mod.nota }}</span><br>
      </span>
      <div class="spazietto"/>
    </div>
  </div>
</template>

<style scoped>
.descrizione-html { display: inline-block; width: 100%; white-space: pre-wrap; }
.descrizione-html :deep(ul),
.descrizione-html :deep(ol) { margin: .3rem 0 .3rem 1.2rem; padding: 0; }
.descrizione-html :deep(h3) { margin: .4rem 0 .2rem; font-size: 1rem; }
.descrizione-html :deep(p) { margin: .3rem 0; }

.action-bar {
  display: flex;
  flex-wrap: wrap;
  gap: .5rem;
  align-items: center;
  padding-bottom: .6rem;
  margin-bottom: .6rem;
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
  line-height: 1;
  cursor: pointer;
  border: 1px solid transparent;
}

.action-btn:hover { filter: brightness(.97); }

.action-btn.toggle.enable {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #166534;
}

.action-btn.toggle.disable {
  border-color: #fed7aa;
  background: #fff7ed;
  color: #9a3412;
}

.action-btn.edit {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
  margin-left: auto;
}

.barriera-box {
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  border-radius: .6rem;
  padding: .5rem .6rem;
  display: grid;
  gap: .4rem;
  margin: .5rem 0;
}
.barr-head { display: flex; justify-content: space-between; align-items: baseline; gap: .5rem; }
.barr-head .titolo { font-weight: 700; }
.barr-head .val { font-weight: 800; color: #1d4ed8; font-variant-numeric: tabular-nums; }
.barr-track { height: .55rem; background: #dbeafe; border-radius: 999px; overflow: hidden; }
.barr-fill { height: 100%; background: #3b82f6; }
.barr-actions { display: flex; flex-wrap: wrap; gap: .35rem; }
.barr-actions .btn {
  border: 1px solid #d0d5dd; background: #fff; border-radius: .5rem;
  padding: .35rem .65rem; cursor: pointer; font-weight: 600; font-size: .85rem;
}
.barr-actions .btn:disabled { opacity: .5; cursor: default; }
.barr-actions .btn.danger { border-color: #fecaca; background: #fef2f2; color: #991b1b; }
.contenitore-box {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: .6rem;
  overflow: hidden;
  margin: .5rem 0;
}
.contenitore-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  background: var(--color-surface-2, #f3f4f6);
  padding: .4rem .75rem;
  font-size: .78rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: .04em;
  color: var(--color-text-secondary, #6b7280);
}
.contenitore-peso { font-variant-numeric: tabular-nums; }
.contenitore-list { padding: .25rem 0; }
.contenitore-item {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: .25rem .75rem;
  font-size: .85rem;
  gap: .5rem;
}
.contenitore-item:nth-child(even) { background: var(--color-surface-1, #fafafa); }
.contenitore-item-nome { flex: 1; }
.contenitore-item-peso { font-size: .75rem; font-variant-numeric: tabular-nums; white-space: nowrap; }
.contenitore-empty { padding: .5rem .75rem; font-size: .85rem; }
.arma-row        { background: #fefce8 !important; }
.oggetto-row     { background: #eff6ff !important; }
.consumabile-row { background: #fdf4ff !important; }
.altro-row       { background: #f3f4f6 !important; }
.monete-row      { background: #f0fdf4 !important; }
.muted { color: var(--color-text-secondary, #6b7280); }

/* Talento: header manuale/pagina/categorie + sezioni stile dndtools */
.talento-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: .4rem;
  margin-bottom: .5rem;
  font-size: .8rem;
}
.talento-manuale { color: var(--color-text-secondary, #6b7280); font-style: italic; }
.talento-categoria {
  background: var(--color-surface-2, #f3f4f6);
  color: var(--color-text-secondary, #6b7280);
  border-radius: .4rem;
  padding: .1rem .5rem;
  font-weight: 600;
  text-transform: uppercase;
  font-size: .7rem;
  letter-spacing: .03em;
}
.section-card {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: .5rem;
  overflow: hidden;
  margin: .5rem 0;
}
.section-card-header {
  background: var(--color-surface-2, #f3f4f6);
  padding: .4rem .75rem;
  font-size: .78rem;
  font-weight: 600;
  color: var(--color-text-secondary, #6b7280);
  text-transform: uppercase;
  letter-spacing: .04em;
}
.section-card-body {
  padding: .6rem .75rem;
  font-size: .88rem;
  white-space: pre-wrap;
}
.talento-link { margin: .5rem 0; font-size: .85rem; }
.talento-link a { color: #1d4ed8; }
</style>
