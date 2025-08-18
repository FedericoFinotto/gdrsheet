import {createApp} from 'vue'
import PrimeVue from 'primevue/config'

// Import PrimeVue components you are actually using
import Toast from 'primevue/toast'
import Button from 'primevue/button'
import TabView from 'primevue/tabview' // Correct import for the tab container
import TabPanel from 'primevue/tabpanel' // Correct import for individual tabs within TabView
// THEME IMPORTS (These look correct)
import 'primevue/resources/themes/saga-blue/theme.css' // Your chosen theme
import 'primevue/resources/primevue.min.css' // PrimeVue core CSS
import 'primeicons/primeicons.css' // PrimeIcons for icons
import './styles/global.css'
import '@fortawesome/fontawesome-free/css/all.min.css'
import App from './App.vue'
import router from './router'
import {createPinia} from "pinia";
import {ACCENT_COLOR, BORDER_COLOR, PRIMARY_COLOR, SECONDARY_COLOR, TERTIARY_COLOR} from "./function/Constants";

const app = createApp(App)
document.documentElement.style.setProperty('--primary-color', PRIMARY_COLOR);
document.documentElement.style.setProperty('--secondary-color', SECONDARY_COLOR);
document.documentElement.style.setProperty('--tertiary-color', TERTIARY_COLOR);
document.documentElement.style.setProperty('--border-color', BORDER_COLOR);
document.documentElement.style.setProperty('--accent-color', ACCENT_COLOR);

// Use router and PrimeVue
app.use(router)
app.use(PrimeVue)
app.use(createPinia())

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