<script setup lang="ts">
import {computed, markRaw, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useMondoStore} from '../stores/mondo'
import {getAlberoNodo, NodoAlbero} from '../service/PersonaggioService'
import usePopup from '../function/usePopup'
import Mobile_DettaglioItem from './sheets/cico/character/Dettaglio/Mobile_DettaglioItem.vue'

const route = useRoute()
const router = useRouter()
const mondoStore = useMondoStore()
const {openPopup} = usePopup()

const albero = computed(() => String(route.params.nome ?? ''))
const nodi = ref<NodoAlbero[]>([])
const loading = ref(true)
const errorMsg = ref<string | null>(null)

onMounted(async () => {
  await mondoStore.carica()
  try {
    if (mondoStore.corrente != null) nodi.value = (await getAlberoNodo(mondoStore.corrente, albero.value)).data ?? []
  } catch (e) {
    console.error('Errore caricamento albero NODO:', e)
    errorMsg.value = 'Errore nel caricamento dell\'albero'
  } finally {
    loading.value = false
  }
})

// Disposizione a livelli: radici (nessun genitore, cioè nessun nodo che le ha tra i "figli") in
// cima, poi via via i loro figli sotto. Un nodo con più genitori a livelli diversi si posiziona
// sotto il più profondo di loro — per questo si rilassano i livelli finché non si stabilizzano
// invece di fare una singola passata BFS, che basterebbe solo per un albero puro senza incroci.
const idIndex = computed(() => {
  const m = new Map<number, number>()
  nodi.value.forEach((n, i) => m.set(n.id, i))
  return m
})
const livelli = computed<number[]>(() => {
  const n = nodi.value.length
  const lvl = new Array(n).fill(0)
  const idx = idIndex.value
  for (let pass = 0; pass < n + 1; pass++) {
    let changed = false
    for (const nodo of nodi.value) {
      const pi = idx.get(nodo.id)
      if (pi === undefined) continue
      for (const f of nodo.figli) {
        const ci = idx.get(f)
        if (ci === undefined) continue
        if (lvl[ci] < lvl[pi] + 1) { lvl[ci] = lvl[pi] + 1; changed = true }
      }
    }
    if (!changed) break
  }
  return lvl
})
const NODE_W = 150
const NODE_H = 64
const GAP_X = 30
const GAP_Y = 70
const PAD = 30
const SLOT = NODE_W + GAP_X // distanza orizzontale minima fra due nodi allo stesso livello

// Ordine di visita in profondità (DFS) dalle radici, raggruppate per componente connessa (stesso
// grafo ignorando il verso degli archi): usato SOLO come criterio con cui ordinare i nodi da
// sinistra a destra ad ogni livello — così due alberi che condividono un nodo (es. due "Da"
// diversi che confluiscono nello stesso posto) vengono processati vicini invece di essere separati
// da un terzo albero indipendente capitato in mezzo nell'ordine naturale della lista.
const ordineVisita = computed(() => {
  const idx = idIndex.value
  const ordine = new Map<number, number>()
  let contatore = 0

  const adiacenza = new Map<number, number[]>()
  const aggiungiArco = (a: number, b: number) => {
    if (!adiacenza.has(a)) adiacenza.set(a, [])
    adiacenza.get(a)!.push(b)
  }
  for (const n of nodi.value) {
    if (!adiacenza.has(n.id)) adiacenza.set(n.id, [])
    for (const f of n.figli) {
      if (!idx.has(f)) continue
      aggiungiArco(n.id, f)
      aggiungiArco(f, n.id)
    }
  }
  const componente = new Map<number, number>()
  let nComponenti = 0
  for (const n of nodi.value) {
    if (componente.has(n.id)) continue
    const stack = [n.id]
    componente.set(n.id, nComponenti)
    while (stack.length) {
      const cur = stack.pop()!
      for (const vic of adiacenza.get(cur) ?? []) {
        if (!componente.has(vic)) { componente.set(vic, nComponenti); stack.push(vic) }
      }
    }
    nComponenti++
  }

  const haGenitore = new Set<number>()
  for (const n of nodi.value) for (const f of n.figli) if (idx.has(f)) haGenitore.add(f)
  const radici = nodi.value.filter(n => !haGenitore.has(n.id))
  const radiciOrdinate = [...radici].sort((a, b) => componente.get(a.id)! - componente.get(b.id)!)

  function visita(id: number) {
    if (ordine.has(id)) return
    ordine.set(id, contatore++)
    const i = idx.get(id)
    if (i === undefined) return
    for (const f of nodi.value[i].figli) if (idx.has(f)) visita(f)
  }
  for (const r of radiciOrdinate) visita(r.id)
  for (const n of nodi.value) if (!ordine.has(n.id)) visita(n.id) // stragglers: solo in un ciclo senza vere radici
  return ordine
})

// Disposizione orizzontale (X): variante semplificata dell'algoritmo di Reingold-Tilford per
// "tidy tree", elaborata per LIVELLI dal più profondo al meno profondo (non con una singola
// recursione top-down): un nodo con un solo figlio finisce esattamente sopra di lui (linea dritta,
// come richiesto), uno con più figli si centra sulla loro campata, e due sottoalberi indipendenti
// occupano blocchi distinti senza sovrapporsi. Il punto chiave dell'elaborare dal fondo verso l'alto
// livello per livello (invece che nodo per nodo) è che quando si centra un genitore sulla campata
// dei figli, quei figli hanno GIÀ la posizione DEFINITIVA (compresa l'eventuale correzione anti-
// sovrapposizione) — evita il bug per cui il genitore restava centrato su una posizione dei figli
// poi corretta subito dopo, finendo visibilmente sbilanciato rispetto alla campata reale.
const layout = computed(() => {
  const idx = idIndex.value
  const ordine = ordineVisita.value
  const xFinale = new Map<number, number>()

  // Quando un nodo viene spinto a destra rispetto al suo centro naturale (sotto), i suoi
  // discendenti — già definitivi, elaborati in un'iterazione precedente perché più in profondità
  // — vanno spostati della STESSA quantità: altrimenti resterebbero centrati sulla vecchia
  // posizione, non su quella reale dopo la correzione anti-sovrapposizione, e apparirebbero
  // sbilanciati rispetto al genitore. La cascata si ferma però su un nodo con PIÙ DI UN genitore
  // (un punto di merge, es. due "Da" diversi che confluiscono qui): se lo spostassimo seguendo
  // solo IL genitore che lo ha appena spinto, lo strapperemmo via dalla posizione che gli spetta
  // rispetto ai suoi altri genitori — meglio lasciarlo dove l'ha piazzato il calcolo per livelli.
  const contaGenitori = new Map<number, number>()
  for (const n of nodi.value) for (const f of n.figli) {
    if (!idx.has(f)) continue
    contaGenitori.set(f, (contaGenitori.get(f) ?? 0) + 1)
  }
  function spostaDiscendenti(id: number, delta: number) {
    const i = idx.get(id)
    if (i === undefined) return
    for (const f of nodi.value[i].figli ?? []) {
      if (!idx.has(f) || !xFinale.has(f)) continue
      if ((contaGenitori.get(f) ?? 0) > 1) continue // punto di merge: non lo si strappa via
      xFinale.set(f, xFinale.get(f)! + delta)
      spostaDiscendenti(f, delta)
    }
  }

  const maxLivello = Math.max(0, ...livelli.value)
  for (let l = maxLivello; l >= 0; l--) {
    const nodiLivello = nodi.value.filter((_, i) => livelli.value[i] === l)
    if (!nodiLivello.length) continue

    // centro preliminare: media tra il figlio più a sinistra e quello più a destra, usando le
    // loro x GIÀ definitive (livello più profondo, elaborato in un'iterazione precedente); un
    // nodo senza figli (foglia) non ha ancora una x, gliela dà la passata sotto in ordine.
    const preliminari = nodiLivello.map(n => {
      const figli = (n.figli ?? []).filter(f => idx.has(f) && xFinale.has(f))
      const xCentro = figli.length
          ? (Math.min(...figli.map(f => xFinale.get(f)!)) + Math.max(...figli.map(f => xFinale.get(f)!))) / 2
          : null
      return {id: n.id, xCentro}
    })
    preliminari.sort((a, b) => (ordine.get(a.id) ?? 0) - (ordine.get(b.id) ?? 0))

    // passata sinistra-destra: usa il centro preliminare se c'è, altrimenti il prossimo slot
    // libero; in entrambi i casi non permette mai di finire più a sinistra del nodo precedente +
    // uno slot, così due nodi allo stesso livello (stessa riga: unico modo in cui possono
    // sovrapporsi, dato che livelli diversi vivono su righe distinte) non si accavallano mai.
    let prevX = -Infinity
    let prossimoSlot = 0
    for (const p of preliminari) {
      const base = p.xCentro ?? prossimoSlot
      if (p.xCentro === null) prossimoSlot += SLOT
      const x = Math.max(base, prevX + SLOT)
      xFinale.set(p.id, x)
      prevX = x
      if (p.xCentro !== null && x !== p.xCentro) spostaDiscendenti(p.id, x - p.xCentro)
    }
  }

  // Seconda passata, dal livello più superficiale al più profondo (l'opposto della prima): un
  // nodo raggiunto da PIÙ di un "Da" (un punto di convergenza) si ricentra sulla campata dei suoi
  // genitori — già definitivi, essendo più in superficie — invece di restare dov'era finito
  // seguendo solo la passata bottom-up sul SUO (unico) ramo di figli, che lo mette sotto uno
  // qualunque dei genitori senza un criterio (nell'esempio: un nodo con 3 "Da" finiva sotto il
  // primo per ordine di elaborazione, non sotto quello centrale). Se cambia posizione, la cascata
  // sotto di lui (i suoi discendenti esclusivi, non altri punti di merge) lo segue.
  const genitoriDi = new Map<number, number[]>()
  for (const n of nodi.value) for (const f of n.figli) {
    if (!idx.has(f)) continue
    if (!genitoriDi.has(f)) genitoriDi.set(f, [])
    genitoriDi.get(f)!.push(n.id)
  }
  for (let l = 0; l <= maxLivello; l++) {
    for (const n of nodi.value) {
      const i = idx.get(n.id)
      if (i === undefined || livelli.value[i] !== l) continue
      const genitori = genitoriDi.get(n.id) ?? []
      if (genitori.length < 2) continue
      const xs = genitori.map(g => xFinale.get(g)).filter((x): x is number => x !== undefined)
      if (!xs.length) continue
      const nuovaX = (Math.min(...xs) + Math.max(...xs)) / 2
      const vecchiaX = xFinale.get(n.id)!
      if (nuovaX !== vecchiaX) {
        xFinale.set(n.id, nuovaX)
        spostaDiscendenti(n.id, nuovaX - vecchiaX)
      }
    }
  }

  const pos = new Map<number, { x: number; y: number }>()
  nodi.value.forEach((n, i) => {
    pos.set(n.id, {x: (xFinale.get(n.id) ?? 0) + PAD, y: livelli.value[i] * (NODE_H + GAP_Y) + PAD})
  })
  return pos
})
const larghezza = computed(() => {
  let max = 0
  for (const p of layout.value.values()) max = Math.max(max, p.x + NODE_W)
  return max + PAD
})
const altezza = computed(() => {
  let max = 0
  for (const p of layout.value.values()) max = Math.max(max, p.y + NODE_H)
  return max + PAD
})

const archi = computed(() => {
  const lin: { x1: number; y1: number; x2: number; y2: number; key: string }[] = []
  for (const n of nodi.value) {
    const da = layout.value.get(n.id)
    if (!da) continue
    for (const f of n.figli) {
      const a = layout.value.get(f)
      if (!a) continue
      lin.push({
        x1: da.x + NODE_W / 2, y1: da.y + NODE_H,
        x2: a.x + NODE_W / 2, y2: a.y,
        key: `${n.id}-${f}`,
      })
    }
  }
  return lin
})

function boxStyle(id: number) {
  const p = layout.value.get(id)
  if (!p) return {}
  return {left: `${p.x}px`, top: `${p.y}px`, width: `${NODE_W}px`, height: `${NODE_H}px`}
}

// Popup con il dettaglio dell'item invece di aprirne l'editor: qui si naviga l'albero, non si
// modifica — stesso componente/shim usati da Compendio.vue per il dettaglio da fuori scheda
// (nessun personaggio: solo consultazione).
const personaggioShim = {
  modificatori: {id: 0},
  items: {trasformazioni: [], idoli: []},
}
function vediDettaglio(n: NodoAlbero) {
  openPopup(
      markRaw(Mobile_DettaglioItem),
      {data: {item: {id: n.id, nome: n.nome, tipo: 'NODO'}, personaggio: personaggioShim}, hideToggle: true},
      {closable: true, title: n.nome},
  )
}

// Pan/zoom manuale (pointer events, unificano mouse e touch): un solo dito/puntatore trascina la
// vista, due dita fanno pinch-to-zoom. overflow:hidden sul viewport invece dello scroll nativo,
// perché lo scroll nativo non gestisce lo zoom — qui serve entrambi insieme sulla stessa area.
const scale = ref(1)
const tx = ref(0)
const ty = ref(0)
const viewportEl = ref<HTMLElement | null>(null)
const pointers = new Map<number, { x: number; y: number }>()
let panStart: { x: number; y: number } | null = null
let pinchStartDist = 0
let pinchStartScale = 1
let trascinato = false // true se il gesto in corso si è mosso abbastanza da contare come pan, non tap
// id del nodo su cui è iniziato il gesto (letto al pointerdown, PRIMA di setPointerCapture): serve
// per riconoscere un tap al pointerup invece di affidarsi all'evento "click" nativo, che
// setPointerCapture ri-targettizza sul contenitore che ha catturato il puntatore — sul dispositivo
// reale il click sul bottone-nodo semplicemente non partiva più (funzionava solo chiamando
// .click() via JS, che lo bypassa). I gesti pointer restano quindi l'unica fonte di verità.
let downNodeId: number | null = null

function clampScale(s: number) { return Math.min(2.5, Math.max(0.2, s)) }

function onPointerDown(e: PointerEvent) {
  const nodoEl = (e.target as HTMLElement)?.closest?.('.nodo-box') as HTMLElement | null
  viewportEl.value?.setPointerCapture(e.pointerId)
  pointers.set(e.pointerId, {x: e.clientX, y: e.clientY})
  trascinato = false
  if (pointers.size === 1) {
    panStart = {x: e.clientX - tx.value, y: e.clientY - ty.value}
    downNodeId = nodoEl ? Number(nodoEl.dataset.nodeId) : null
  } else if (pointers.size === 2) {
    downNodeId = null // un secondo dito annulla qualunque intenzione di tap
    const pts = [...pointers.values()]
    pinchStartDist = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y)
    pinchStartScale = scale.value
  }
}
function onPointerMove(e: PointerEvent) {
  if (!pointers.has(e.pointerId)) return
  pointers.set(e.pointerId, {x: e.clientX, y: e.clientY})
  if (pointers.size === 1 && panStart) {
    const nx = e.clientX - panStart.x
    const ny = e.clientY - panStart.y
    if (Math.abs(nx - tx.value) > 3 || Math.abs(ny - ty.value) > 3) trascinato = true
    tx.value = nx
    ty.value = ny
  } else if (pointers.size === 2 && pinchStartDist > 0) {
    trascinato = true
    const pts = [...pointers.values()]
    const dist = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y)
    scale.value = clampScale(pinchStartScale * (dist / pinchStartDist))
  }
}
function onPointerUp(e: PointerEvent) {
  pointers.delete(e.pointerId)
  if (pointers.size === 1) {
    const [p] = [...pointers.values()]
    panStart = {x: p.x - tx.value, y: p.y - ty.value}
  } else {
    panStart = null
    if (!trascinato && downNodeId != null) {
      const n = nodi.value.find(x => x.id === downNodeId)
      if (n) vediDettaglio(n)
    }
    downNodeId = null
  }
}
function onWheel(e: WheelEvent) {
  e.preventDefault()
  scale.value = clampScale(scale.value * (1 - e.deltaY * 0.001))
}
function zoomBtn(delta: number) { scale.value = clampScale(scale.value + delta) }
function resetView() { scale.value = 1; tx.value = 0; ty.value = 0 }
</script>

<template>
  <div class="albero-page">
    <header class="head">
      <button class="btn ghost" @click="router.back()">←</button>
      <div class="title">
        <h1>🌳 {{ albero }}</h1>
        <span v-if="nodi.length" class="muted">{{ nodi.length }} nodi</span>
      </div>
    </header>

    <div v-if="loading" class="state">Caricamento…</div>
    <div v-else-if="errorMsg" class="state error">{{ errorMsg }}</div>
    <div v-else-if="!nodi.length" class="state">Nessun nodo trovato per questo albero.</div>

    <div v-else
         ref="viewportEl"
         class="graph-viewport"
         @pointerdown="onPointerDown"
         @pointermove="onPointerMove"
         @pointerup="onPointerUp"
         @pointercancel="onPointerUp"
         @wheel="onWheel">
      <div class="graph" :style="{
             width: larghezza + 'px', height: altezza + 'px',
             transform: `translate(${tx}px, ${ty}px) scale(${scale})`,
           }">
        <svg class="edges" :width="larghezza" :height="altezza">
          <line v-for="e in archi" :key="e.key" :x1="e.x1" :y1="e.y1" :x2="e.x2" :y2="e.y2"/>
        </svg>
        <button v-for="n in nodi" :key="n.id" type="button" class="nodo-box" :data-node-id="n.id" :style="boxStyle(n.id)">
          <span class="nome">{{ n.nome }}</span>
          <span v-if="n.tipoNome" class="tipo">{{ n.tipoNome }}</span>
        </button>
      </div>
      <div class="zoom-controls" @pointerdown.stop>
        <button type="button" @click="zoomBtn(0.2)" title="Zoom avanti">+</button>
        <button type="button" @click="zoomBtn(-0.2)" title="Zoom indietro">−</button>
        <button type="button" @click="resetView" title="Ripristina vista">⟲</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.albero-page {
  width: 100%;
  padding: 1rem;
  display: grid;
  grid-template-rows: auto 1fr;
  gap: .75rem;
  height: 100%;
  min-height: 0;
}
.head { display: flex; align-items: center; gap: .5rem; }
.title { flex: 1; display: grid; min-width: 0; }
.title h1 { margin: 0; font-size: 1.2rem; overflow-wrap: break-word; }
.muted { opacity: .65; font-size: .85rem; }

.state { padding: .75rem; border: 1px dashed var(--hairline); border-radius: .5rem; }
.state.error { color: var(--danger-text); background: var(--danger-bg); border-color: var(--danger-border); }

.btn {
  padding: .45rem .8rem;
  border-radius: .5rem;
  border: 1px solid var(--hairline);
  background: var(--surface-0);
  cursor: pointer;
}

/* Area di pan/zoom: dimensione fissa (riempie lo spazio residuo della pagina), contenuto tenuto
   fuori dal flusso normale e mosso via transform — niente scroll nativo, lo gestisce onPointerMove
   sopra, insieme allo zoom, sulla stessa area e con lo stesso gesto (drag = pan, pinch = zoom).
   touch-action: none impedisce al browser di intercettare i gesti per lo scroll/zoom di pagina. */
.graph-viewport {
  position: relative;
  overflow: hidden;
  touch-action: none;
  border: 1px solid var(--hairline);
  border-radius: .6rem;
  background: var(--surface-0);
  min-height: 0;
  cursor: grab;
}
.graph { position: absolute; top: 0; left: 0; transform-origin: 0 0; }
.edges { position: absolute; top: 0; left: 0; pointer-events: none; }
.edges line { stroke: var(--info-border); stroke-width: 2; }

.nodo-box {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: .15rem;
  border: 1px solid var(--info-border);
  background: var(--info-bg);
  color: var(--info-text);
  border-radius: .6rem;
  padding: .3rem .5rem;
  cursor: pointer;
  text-align: center;
  overflow: hidden;
}
.nodo-box:hover { background: var(--info-border); }
.nodo-box .nome { font-weight: 700; font-size: .85rem; overflow-wrap: break-word; line-height: 1.15; }
.nodo-box .tipo { font-size: .68rem; opacity: .75; overflow-wrap: break-word; }

.zoom-controls {
  position: absolute;
  right: .6rem;
  bottom: .6rem;
  display: flex;
  flex-direction: column;
  gap: .3rem;
}
.zoom-controls button {
  width: 2.2rem;
  height: 2.2rem;
  border-radius: .5rem;
  border: 1px solid var(--hairline);
  background: var(--surface-0);
  color: var(--text-strong);
  font-size: 1.1rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(15, 23, 42, .12);
}
.zoom-controls button:hover { background: var(--btn-bg); }
</style>
