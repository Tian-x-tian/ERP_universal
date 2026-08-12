<template>
  <el-config-provider :locale="elementLocale" :size="componentSize">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import { useUiPreferenceStore } from '@/store/uiPreference'
import { resolveComponentSize } from '@/utils/theme'

const uiStore = useUiPreferenceStore()

/** 组件尺寸由信息密度联动 */
const componentSize = computed(() => resolveComponentSize(uiStore.density))

/** Element Plus 语言包 */
const elementLocale = computed(() => (uiStore.locale === 'en-US' ? en : zhCn))
</script>

<style>
#app {
  width: 100%;
  min-height: 100vh;
  height: 100vh;
  margin: 0;
  padding: 0;
}
</style>
