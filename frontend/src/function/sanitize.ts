// Sanificazione HTML per le descrizioni (e qualsiasi contenuto mostrato con HTML).
// Evita injection: rimuove script, handler on*, javascript: ecc., lasciando solo
// markup di formattazione di base.
import DOMPurify from 'dompurify'

const CONFIG = {
    ALLOWED_TAGS: [
        'b', 'strong', 'i', 'em', 'u', 's', 'br', 'p', 'div', 'span',
        'ul', 'ol', 'li', 'h1', 'h2', 'h3', 'h4', 'blockquote', 'a', 'code', 'pre',
    ],
    ALLOWED_ATTR: ['href', 'target', 'rel'],
    ALLOW_DATA_ATTR: false,
}

export function sanitizeHtml(html: string | null | undefined): string {
    if (!html) return ''
    return DOMPurify.sanitize(String(html), CONFIG) as unknown as string
}
