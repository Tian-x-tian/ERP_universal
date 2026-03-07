import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import hasPermi from './directive/permission/hasPermi'

// 导入全局样式
import './style.css'
import 'element-plus/dist/index.css'
import './styles/brand-theme.css'

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.use(createPinia())
app.use(router)

// 注册全局指令
app.directive('hasPermi', hasPermi)

app.mount('#app')
