import {createApp} from 'vue'
import PrimeVue from 'primevue/config'

// Import PrimeVue components you are actually using
import Toast from 'primevue/toast'
import Button from 'primevue/button'
import TabView from 'primevue/tabview' // Correct import for the tab container
import TabPanel from 'primevue/tabpanel' // Correct import for individual tabs within TabView
// THEME IMPORTS: risolti come url da Vite così possiamo scambiarli a runtime
// in base al tema chiaro/scuro (vedi function/useTheme.js) invece di importare
// staticamente un solo tema.
import primeVueLightThemeUrl from 'primevue/resources/themes/saga-blue/theme.css?url'
import primeVueDarkThemeUrl from 'primevue/resources/themes/arya-blue/theme.css?url'
import 'primevue/resources/primevue.min.css' // PrimeVue core CSS
import 'primeicons/primeicons.css' // PrimeIcons for icons
import './styles/global.css'
import '@fortawesome/fontawesome-free/css/all.min.css'
import App from './App.vue'
import router from './router'
import {createPinia} from "pinia";
import {initTheme} from "./function/useTheme";
import {vSafeHtml} from "./directives/safeHtml";

const app = createApp(App)
initTheme(primeVueLightThemeUrl, primeVueDarkThemeUrl)

// Use router and PrimeVue
app.use(router)
app.use(PrimeVue)
app.use(createPinia())

// Direttiva globale per HTML sanificato (descrizioni ecc.)
app.directive('safe-html', vSafeHtml)

// Register global components
app.component('Toast', Toast)
app.component('Button', Button)

// Register the CORRECT Tab components
app.component('TabView', TabView) // Register TabView, not 'Tabs'
app.component('TabPanel', TabPanel) // Register TabPanel (used inside TabView)

// Remove these lines if you don't have corresponding valid PrimeVue components:
// app.component('Tabs', Tabs)
// app.component('TabList', TabList)
// app.component('TabPanels', TabPanels) // TabPanels is often a concept, not a directly imported component
// app.component('Tab', Tab)

// Mount the app
app.mount('#app')