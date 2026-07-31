// Sanificazione HTML per le descrizioni (e qualsiasi contenuto mostrato con HTML).
// Evita injection: rimuove script, handler on*, javascript: ecc., lasciando solo
// markup di formattazione di base.
import DOMPurify from 'dompurify'

const CONFIG = {
    ALLOWED_TAGS: [
        'b', 'strong', 'i', 'em', 'u', 'ins', 's', 'strike', 'del', 'br', 'p', 'div', 'span',
        'ul', 'ol', 'li', 'h1', 'h2', 'h3', 'h4', 'blockquote', 'a', 'code', 'pre', 'font',
    ],
    // "style" serve al colore del testo (l'editor produce <span style="color:…">); DOMPurify
    // analizza e ripulisce comunque il CSS, quindi restano fuori url(javascript:), expression()
    // e simili. "color" copre il <font color> che alcuni browser generano ancora al suo posto.
    ALLOWED_ATTR: ['href', 'target', 'rel', 'style', 'color'],
    ALLOW_DATA_ATTR: false,
}

export function sanitizeHtml(html: string | null | undefined): string {
    if (!html) return ''
    return DOMPurify.sanitize(String(html), CONFIG) as unknown as string
}
