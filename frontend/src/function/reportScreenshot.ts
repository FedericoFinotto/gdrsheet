import {ref} from 'vue'

// Singleton a livello di modulo: sopravvive alla navigazione (la SPA non ricarica il JS).
export const pendingScreenshot = ref<Blob | null>(null)

/** Cattura uno screenshot della pagina corrente "sottobanco", prima di navigare via. */
export async function catturaScreenshot(): Promise<void> {
    try {
        const {default: html2canvas} = await import('html2canvas')
        const canvas = await html2canvas(document.body)
        pendingScreenshot.value = await new Promise<Blob | null>(resolve => canvas.toBlob(resolve, 'image/png'))
    } catch (e) {
        console.error('Errore cattura screenshot:', e)
        pendingScreenshot.value = null
    }
}

export function scartaScreenshotPendente(): void {
    pendingScreenshot.value = null
}
