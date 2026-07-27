// Evidenzia un termine di ricerca dentro un testo che può contenere HTML (es. da HtmlEditor):
// va reso con v-html (altrimenti si vedrebbero i tag letterali), quindi l'evidenziazione deve
// rispettare i tag esistenti — sostituisce solo nei segmenti di testo, mai dentro "<...>", per
// non spezzare il markup.
function escapeRegExp(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

export function highlightMatch(html: string | null | undefined, needle: string): string {
  if (!html) return ''
  const q = needle.trim()
  if (!q) return html
  const re = new RegExp(`(${escapeRegExp(q)})`, 'ig')
  return html
      .split(/(<[^>]+>)/g)
      .map(part => part.startsWith('<') ? part : part.replace(re, '<mark class="hl">$1</mark>'))
      .join('')
}
