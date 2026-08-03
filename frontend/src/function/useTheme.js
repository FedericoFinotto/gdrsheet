// Gestione del tema chiaro/scuro dell'applicazione.
// Il tema è applicato tramite l'attributo [data-theme] su <html> (letto dalle
// variabili CSS in styles/global.css) più lo swap del foglio di stile PrimeVue,
// che essendo un tema statico non segue le CSS variable e va cambiato a parte.

const STORAGE_KEY = 'theme'
const THEME_LINK_ID = 'primevue-theme-link'

let currentTheme = 'light'
let lightThemeUrl = null
let darkThemeUrl = null

function systemPrefersDark() {
  return typeof window !== 'undefined'
      && window.matchMedia
      && window.matchMedia('(prefers-color-scheme: dark)').matches
}

function readStoredTheme() {
  try {
    return localStorage.getItem(STORAGE_KEY)
  } catch {
    return null
  }
}

function applyPrimeVueLink(theme) {
  if (!lightThemeUrl || !darkThemeUrl) return
  let link = document.getElementById(THEME_LINK_ID)
  if (!link) {
    link = document.createElement('link')
    link.id = THEME_LINK_ID
    link.rel = 'stylesheet'
    // Il tema PrimeVue definisce le proprie variabili CSS su :root con GLI STESSI
    // NOMI usati dall'app (es. --primary-color), con valori diversi (la palette
    // di PrimeVue, es. blu #2196F3). Va quindi inserito PRIMA di global.css nel
    // <head> (non appeso in coda), altrimenti per pari specificità vincerebbe lui
    // sulle variabili dell'app invece del contrario.
    document.head.insertBefore(link, document.head.firstChild)
  }
  link.href = theme === 'dark' ? darkThemeUrl : lightThemeUrl
}

export function setTheme(theme) {
  currentTheme = theme === 'dark' ? 'dark' : 'light'
  document.documentElement.setAttribute('data-theme', currentTheme)
  applyPrimeVueLink(currentTheme)
  try {
    localStorage.setItem(STORAGE_KEY, currentTheme)
  } catch {
    // storage non disponibile (es. modalità privata): il tema semplicemente non persiste
  }
}

export function toggleTheme() {
  setTheme(currentTheme === 'dark' ? 'light' : 'dark')
}

export function getTheme() {
  return currentTheme
}

// Da chiamare una volta all'avvio (main.js), passando gli url risolti da Vite
// dei due fogli di stile PrimeVue (import '...theme.css?url').
export function initTheme(primeVueLightUrl, primeVueDarkUrl) {
  lightThemeUrl = primeVueLightUrl
  darkThemeUrl = primeVueDarkUrl
  const stored = readStoredTheme()
  setTheme(stored ?? (systemPrefersDark() ? 'dark' : 'light'))
}
