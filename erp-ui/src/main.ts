import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import hasPermi from './directive/permission/hasPermi'

// 导入全局样式
import './style.css'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './styles/palette.css'
import './styles/brand-theme.css'
import './styles/platform-modules.css'
import './styles/ui-preference.css'
import { useUiPreferenceStore } from './store/uiPreference'

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

const pinia = createPinia()
app.use(pinia)
app.use(router)

// 注册全局指令
app.directive('hasPermi', hasPermi)

// 初始化全局 UI 偏好（主色 / 外观 / 圆角 / 布局），挂载前完成以消除首屏闪烁
useUiPreferenceStore(pinia).init()

app.mount('#app')
