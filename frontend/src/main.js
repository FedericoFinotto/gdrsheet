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
import App from './App.vue'
import router from './router'
import {createPinia} from "pinia";

const app = createApp(App)

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