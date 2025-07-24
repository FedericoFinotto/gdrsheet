import { createApp } from 'vue'
import PrimeVue from 'primevue/config'
import Toast from 'primevue/toast'  // Importa il componente Toast
import Button from 'primevue/button' // Se hai bisogno di altri componenti di PrimeVue

import 'primevue/resources/themes/saga-blue/theme.css'
import 'primevue/resources/primevue.min.css'
import 'primeicons/primeicons.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// Usa PrimeVue
app.use(router)
app.use(PrimeVue)

// Registra il componente Toast e altri componenti necessari
app.component('Toast', Toast)  // Registrazione di Toast
app.component('Button', Button)  // Registrazione di Button (se usato)

// Monta l'app
app.mount('#app')
