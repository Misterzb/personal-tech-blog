import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useThemeStore } from './stores/theme'
import './assets/main.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
useThemeStore(pinia).apply()
app.use(router)
app.mount('#app')
