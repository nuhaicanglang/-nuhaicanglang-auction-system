import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import { router } from './router'
import { pinia } from './stores'

const browserGlobal = globalThis as typeof globalThis & { global?: typeof globalThis }
browserGlobal.global ??= browserGlobal

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.mount('#app')
