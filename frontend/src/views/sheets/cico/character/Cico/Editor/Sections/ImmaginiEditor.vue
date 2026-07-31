<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {
  caricaImmagine,
  eliminaImmagine,
  getImmagini,
  getStatoHost,
  ItemImmagine,
  riordinaImmagini,
} from '../../../../../../../service/ImmagineService'

/**
 * Immagini dell'item. A differenza delle label si salvano subito, una per una: il file viene
 * caricato sull'host esterno nel momento in cui lo scegli, quindi non ha senso legarlo al "Salva"
 * dell'editor (e in creazione l'item non ha ancora un id a cui agganciarlo).
 */
const props = defineProps<{
  idItem?: number | null
  disabled?: boolean
}>()

const immagini = ref<ItemImmagine[]>([])
const caricamento = ref(true)
const hostAttivo = ref(false)
const nomeHost = ref('')
const cancellazioneRemota = ref(false)
const inCorso = ref(false)
const errore = ref<string | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)

onMounted(async () => {
  try {
    const stato = (await getStatoHost()).data
    hostAttivo.value = stato.configurato
    nomeHost.value = stato.host
    cancellazioneRemota.value = stato.cancellazioneRemota
  } catch {
    hostAttivo.value = false
  }
  if (props.idItem) {
    try {
      immagini.value = (await getImmagini(props.idItem)).data
    } catch (e) {
      console.error('Errore caricamento immagini:', e)
    }
  }
  caricamento.value = false
})

async function onFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''  // così ricaricare lo stesso file rilancia il change
  if (!file || !props.idItem) return

  inCorso.value = true
  errore.value = null
  try {
    const {data} = await caricaImmagine(props.idItem, file)
    immagini.value.push(data)
  } catch (e: any) {
    errore.value = e?.response?.data?.message
        ?? (e?.response?.status === 403 ? 'Non hai i permessi per gestire le immagini di questo item'
            : 'Caricamento non riuscito')
  } finally {
    inCorso.value = false
  }
}

async function rimuovi(img: ItemImmagine) {
  if (props.disabled) return
  try {
    await eliminaImmagine(img.id)
    immagini.value = immagini.value.filter(i => i.id !== img.id)
  } catch (e) {
    errore.value = 'Rimozione non riuscita'
  }
}

async function sposta(idx: number, delta: number) {
  const dest = idx + delta
  if (dest < 0 || dest >= immagini.value.length) return
  const next = [...immagini.value]
  const [x] = next.splice(idx, 1)
  next.splice(dest, 0, x)
  immagini.value = next
  try {
    await riordinaImmagini(props.idItem!, next.map(i => i.id))
  } catch (e) {
    console.error('Errore riordino immagini:', e)
  }
}
</script>

<template>
  <div class="immagini-editor">
    <div v-if="caricamento" class="stato">Caricamento…</div>

    <template v-else>
      <div v-if="!idItem" class="stato">
        Salva l'item una prima volta: le immagini si caricano su un item già esistente.
      </div>
      <div v-else-if="!hostAttivo" class="stato">
        Caricamento immagini non disponibile: manca la configurazione dell'host lato server.
      </div>

      <div v-if="immagini.length" class="griglia">
        <div v-for="(img, i) in immagini" :key="img.id" class="miniatura">
          <img :src="img.url" :alt="img.titolo ?? ''" loading="lazy"/>
          <div class="azioni">
            <button type="button" :disabled="disabled || i === 0" title="Sposta prima"
                    @click="sposta(i, -1)">←</button>
            <a :href="img.url" target="_blank" rel="noopener noreferrer" title="Apri originale">↗</a>
            <button type="button" class="del" :disabled="disabled"
                    :title="cancellazioneRemota ? `Rimuovi (cancella anche da ${nomeHost})` : 'Rimuovi dall\'item'"
                    @click="rimuovi(img)">✕</button>
            <button type="button" :disabled="disabled || i === immagini.length - 1" title="Sposta dopo"
                    @click="sposta(i, 1)">→</button>
          </div>
        </div>
      </div>
      <div v-else-if="idItem && hostAttivo" class="stato">Nessuna immagine.</div>

      <div v-if="idItem && hostAttivo" class="carica">
        <input ref="fileInput" type="file" accept="image/*" :disabled="disabled || inCorso"
               @change="onFile"/>
        <span v-if="inCorso" class="stato">Caricamento su {{ nomeHost }}…</span>
      </div>

      <p v-if="errore" class="errore">{{ errore }}</p>

      <p class="nota">
        I file vengono caricati su {{ nomeHost || 'un host esterno' }}: sul database resta solo il
        collegamento.
        <template v-if="cancellazioneRemota">
          Rimuovendo un'immagine viene cancellata anche dall'host.
        </template>
        <template v-else>
          Rimuovendo un'immagine questa sparisce dall'item, ma il file resta sull'host.
        </template>
      </p>
    </template>
  </div>
</template>

<style scoped>
.immagini-editor { display: grid; gap: .5rem; }
.stato { font-size: .85rem; opacity: .7; }
.errore { margin: 0; font-size: .85rem; color: #991b1b; }
.nota { margin: 0; font-size: .75rem; opacity: .6; }
.nota.avviso { color: #9a3412; opacity: .9; }

.griglia {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(7rem, 1fr));
  gap: .5rem;
}
.miniatura {
  border: 1px solid #e5e7eb;
  border-radius: .5rem;
  overflow: hidden;
  background: #f9fafb;
  display: grid;
  gap: 0;
}
.miniatura img {
  width: 100%;
  height: 6rem;
  object-fit: cover;
  display: block;
}
.azioni {
  display: flex;
  justify-content: center;
  gap: .1rem;
  padding: .15rem;
  background: #fff;
  border-top: 1px solid #e5e7eb;
}
.azioni button, .azioni a {
  border: 0;
  background: transparent;
  cursor: pointer;
  font-size: .8rem;
  line-height: 1;
  padding: .2rem .3rem;
  border-radius: .3rem;
  color: #374151;
  text-decoration: none;
}
.azioni button:hover, .azioni a:hover { background: #f3f4f6; }
.azioni .del { color: #991b1b; }
.azioni button:disabled { opacity: .35; cursor: default; }

.carica input[type="file"] { font-size: .85rem; max-width: 100%; }
</style>
