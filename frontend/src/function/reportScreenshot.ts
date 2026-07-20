import {ref} from 'vue'

// Singleton a livello di modulo: sopravvive alla navigazione (la SPA non ricarica il JS).
export const pendingScreenshot = ref<Blob | null>(null)

/** Cattura uno screenshot della pagina corrente "sottobanco", prima di navigare via. */
export async function catturaScreenshot(): Promise<void> {
    try {
        const {default: html2canvas} = await import('html2canvas')
        // elementi marcati con data-screenshot-ignore (es. il menu hamburger) non devono
        // comparire nello screenshot, anche se ancora presenti nel DOM al momento della cattura
        const canvas = await html2canvas(document.body, {
            ignoreElements: el => el.hasAttribute('data-screenshot-ignore'),
        })
        pendingScreenshot.value = await new Promise<Blob | null>(resolve => canvas.toBlob(resolve, 'image/png'))
    } catch (e) {
        console.error('Errore cattura screenshot:', e)
        pendingScreenshot.value = null
    }
}

export function scartaScreenshotPendente(): void {
    pendingScreenshot.value = null
}
