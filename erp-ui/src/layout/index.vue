<template>
  <div class="app-wrapper">
    <el-container v-if="layoutStyle === 'classic'" :class="['layout-container', `menu-theme-${menuTheme}`]">
    <el-aside :width="asideWidth" :class="['aside', { 'is-collapsed': isSidebarCollapsed }]">
      <div class="logo">
        <div class="logo__brand">
          <el-icon size="24"><Platform /></el-icon>
          <span class="logo__title">ERP 系统</span>
        </div>
        <button
          type="button"
          class="sidebar-toggle"
          :aria-label="isSidebarCollapsed ? '展开侧栏' : '收起侧栏'"
          @click="toggleSidebar"
        >
          <el-icon>
            <component :is="isSidebarCollapsed ? Expand : Fold" />
          </el-icon>
        </button>
      </div>
      <div class="menu-scroll">
        <el-menu
          :key="expandedMenuRenderKey"
          v-show="!isSidebarCollapsed"
          :active-text-color="currentMenuTheme.activeTextColor"
          :background-color="currentMenuTheme.menuBgColor"
          class="el-menu-vertical"
          :default-active="$route.path"
          :text-color="currentMenuTheme.textColor"
          router
        >
          <template v-for="menu in sideMenus" :key="menu.menuId">
            <SidebarMenuNode :menu="menu" />
          </template>
        </el-menu>
        <div v-show="isSidebarCollapsed" class="collapsed-menu">
          <template v-for="menu in collapsedSideMenus" :key="menu.menuId">
            <el-dropdown
              v-if="hasCollapsedChildren(menu)"
              placement="right-start"
              trigger="hover"
              :show-timeout="90"
              :hide-timeout="120"
              popper-class="collapsed-submenu-dropdown"
              @command="handleCollapsedDropdownCommand"
            >
              <button
                type="button"
                class="collapsed-menu__item"
                :class="{ 'is-active': isCollapsedMenuActive(menu), 'is-disabled': !resolveMenuTargetPath(menu) }"
                :title="menu.menuName"
                @click="handleCollapsedMenuClick(menu)"
              >
                <el-icon><component :is="resolveMenuIcon(menu)" /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu class="collapsed-submenu-dropdown__menu">
                  <el-dropdown-item class="collapsed-submenu-dropdown__title" disabled>
                    {{ menu.menuName }}
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-for="child in getCollapsedChildren(menu)"
                    :key="`${menu.menuId}-${child.menuId}-${child.targetPath}`"
                    :command="child.targetPath"
                    class="collapsed-submenu-dropdown__item"
                    :class="{ 'is-active': route.path === child.targetPath }"
                  >
                    <span class="collapsed-submenu-dropdown__label">{{ child.menuName }}</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <button
              v-else
              type="button"
              class="collapsed-menu__item"
              :class="{ 'is-active': isCollapsedMenuActive(menu), 'is-disabled': !resolveMenuTargetPath(menu) }"
              :title="menu.menuName"
              @click="handleCollapsedMenuClick(menu)"
            >
              <el-icon><component :is="resolveMenuIcon(menu)" /></el-icon>
            </button>
          </template>
        </div>
      </div>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <div class="page-nav">
            <button type="button" class="page-nav__home" :class="{ 'is-active': isHomePage }" @click="goHome">
              <el-icon><House /></el-icon>
              <span>首页</span>
            </button>
            <template v-if="!isHomePage">
              <el-icon class="page-nav__arrow"><ArrowRightBold /></el-icon>
              <span class="page-nav__current">{{ currentPageTitle }}</span>
            </template>
            <span v-else class="page-nav__current is-home">当前：首页</span>
          </div>
        </div>
        <div class="header-right">
          <button type="button" class="notice-entry" aria-label="待办事项入口" @click="goNotice">
            <el-badge :value="pendingTodoCount" :max="99" :hidden="pendingTodoCount <= 0" class="notice-entry__badge">
              <el-icon><BellFilled /></el-icon>
            </el-badge>
          </button>
          <el-dropdown placement="bottom-end" popper-class="user-dropdown-menu" @command="handleUserCommand">
            <span class="user-trigger">
              <span class="user-trigger__avatar">{{ userInitial }}</span>
              <span class="user-trigger__name">{{ displayUserName }}</span>
              <span class="user-trigger__tenant">租户 {{ displayTenantId }}</span>
              <el-icon class="user-trigger__arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="theme">菜单风格</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition :name="pageTransitionName" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>

  <ExecutiveLayout 
    v-else
      :user-name="displayUserName"
      :tenant-id="displayTenantId"
      :pending-todo-count="pendingTodoCount"
      :side-menus="sideMenus"
      @go-notice="goNotice"
      @open-theme="openThemeDialog"
      @user-command="handleUserCommand"
    />

    <el-dialog v-model="themeDialogVisible" title="系统设置" width="560px" append-to-body>
      <div class="setting-section" style="margin-bottom: 24px;">
        <h3 style="margin-top: 0; font-size: calc(15px * var(--erp-font-scale, 1)); color: var(--erp-s-text); margin-bottom: 12px;">界面架构</h3>
        <el-radio-group :model-value="layoutStyle" @change="applyLayoutStyle">
          <el-radio label="classic">经典侧边栏菜单</el-radio>
          <el-radio label="executive">现代级顶部导航 (Executive版)</el-radio>
        </el-radio-group>
      </div>
      
      <el-divider v-if="layoutStyle === 'classic'" style="margin: 16px 0;" />
      
      <div class="setting-section" v-if="layoutStyle === 'classic'">
        <h3 style="margin-top: 0; font-size: calc(15px * var(--erp-font-scale, 1)); color: var(--erp-s-text); margin-bottom: 12px;">侧栏颜色主题 (仅经典布局)</h3>
        <div class="theme-grid">
          <button
            v-for="item in menuThemeOptions"
            :key="item.id"
            type="button"
            class="theme-card"
            :class="{ 'is-active': item.id === menuTheme }"
            @click="applyMenuTheme(item.id)"
          >
            <span class="theme-card__swatch" :style="{ background: item.preview }"></span>
            <span class="theme-card__name">{{ item.name }}</span>
            <span class="theme-card__desc">{{ item.description }}</span>
          </button>
        </div>
      </div>
    </el-dialog>
    <AiAssistantEntry />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useUiPreferenceStore } from '@/store/uiPreference'
import { ArrowDown, ArrowRightBold, BellFilled, Expand, Fold, House, Platform } from '@element-plus/icons-vue'
import { defaultMenuList, HOME_MENU_PATH } from '@/constants/default-menu'
import SidebarMenuNode from '@/layout/components/SidebarMenuNode.vue'
import AiAssistantEntry from '@/components/ai/AiAssistantEntry.vue'
import ExecutiveLayout from './ExecutiveLayout.vue'
import { logout as logoutApi } from '@/api/system/profile'
import { listWorkflowTask } from '@/api/workflow/workflow'
import { hasPermi } from '@/utils/permission'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const uiStore = useUiPreferenceStore()
const sideMenus = computed(() => (userStore.menuList && userStore.menuList.length > 0 ? userStore.menuList : defaultMenuList))
const collapsedSideMenus = computed(() =>
  sideMenus.value.filter((menu: any) => {
    const hasDirectPath = resolveMenuTargetPath(menu).length > 0
    const hasChildren = Array.isArray(menu?.children) && menu.children.length > 0
    return hasDirectPath || hasChildren
  })
)
const collapsedChildrenMap = computed(() => {
  const menuChildrenMap = new Map<string, CollapsedChildMenuItem[]>()
  for (const menu of collapsedSideMenus.value) {
    const menuKey = String(menu?.menuId ?? menu?.path ?? '')
    menuChildrenMap.set(menuKey, collectCollapsedLeafMenus(menu))
  }
  return menuChildrenMap
})
const entryHomePath = computed(() => userStore.homeEntryPath || HOME_MENU_PATH)
const NOTICE_ROUTE_PATH = '/workbench/process-todo'
const themeDialogVisible = ref(false)

const layoutStyle = computed(() => uiStore.layoutStyle)
const applyLayoutStyle = (val: string | number | boolean) => {
  uiStore.setLayoutStyle(String(val))
}

interface MenuThemeOption {
  id: string
  name: string
  description: string
  preview: string
  textColor: string
  activeTextColor: string
  menuBgColor: string
}

interface CollapsedChildMenuItem {
  menuId: string | number
  menuName: string
  icon?: string
  targetPath: string
}

/**
 * 递归收集收起态菜单的所有叶子菜单，支持三级及以上菜单。
 *
 * @param menu 菜单节点
 * @param parentLabels 祖先菜单文案
 * @returns 叶子菜单列表
 */
const collectCollapsedLeafMenus = (menu: any, parentLabels: string[] = []): CollapsedChildMenuItem[] => {
  const childList = Array.isArray(menu?.children) ? menu.children : []
  if (childList.length === 0) {
    const targetPath = resolveMenuTargetPath(menu)
    if (!targetPath) {
      return []
    }
    const labelParts = [...parentLabels, String(menu?.menuName || '').trim()].filter((item) => !!item)
    const displayParts = labelParts.length > 1 ? labelParts.slice(1) : labelParts
    return [{
      menuId: menu?.menuId ?? `${targetPath}-${labelParts.join('-')}`,
      menuName: displayParts.join(' / '),
      icon: menu?.icon,
      targetPath,
    }]
  }

  return childList.flatMap((child: any) => {
    return collectCollapsedLeafMenus(child, [...parentLabels, String(childList.length > 0 ? menu?.menuName || '' : '').trim()].filter((item) => !!item))
  })
}

const menuThemeOptions: MenuThemeOption[] = [
  {
    id: 'azure',
    name: '极简蓝',
    description: '白色底板 + 蓝色强调，轻量清晰。',
    preview: 'linear-gradient(135deg, #ffffff 0%, #f5f9ff 65%, #ecf4ff 100%)',
    textColor: 'var(--erp-s-menu-text)',
    activeTextColor: 'var(--erp-s-accent)',
    menuBgColor: 'var(--erp-s-surface)',
  },
  {
    id: 'mint',
    name: '极简青',
    description: '白色底板 + 青色强调，柔和商务。',
    preview: 'linear-gradient(135deg, #ffffff 0%, #f3fbfa 66%, #eaf7f5 100%)',
    textColor: 'var(--erp-s-menu-text)',
    activeTextColor: 'var(--erp-s-v-text)',
    menuBgColor: 'var(--erp-s-surface)',
  },
  {
    id: 'sunrise',
    name: '极简橙',
    description: '白色底板 + 橙色强调，温和有活力。',
    preview: 'linear-gradient(135deg, #ffffff 0%, #fff8f1 66%, #fff1e5 100%)',
    textColor: 'var(--erp-s-menu-text)',
    activeTextColor: 'var(--erp-s-v-text)',
    menuBgColor: 'var(--erp-s-surface)',
  },
  {
    id: 'teal',
    name: '极简灰',
    description: '白色底板 + 中性灰强调，克制耐看。',
    preview: 'linear-gradient(135deg, #ffffff 0%, #f6f7f9 66%, #eef1f5 100%)',
    textColor: 'var(--erp-s-menu-text)',
    activeTextColor: 'var(--erp-s-v-text)',
    menuBgColor: 'var(--erp-s-surface)',
  },
]

/**
 * 获取有效的菜单主题编码。
 * @param themeId 菜单主题编码
 * @returns 有效菜单主题编码
 */
const normalizeMenuTheme = (themeId: string | null): string => {
  if (!themeId) {
    return menuThemeOptions[0].id
  }
  const exists = menuThemeOptions.some((item) => item.id === themeId)
  return exists ? themeId : menuThemeOptions[0].id
}

const menuTheme = computed(() => normalizeMenuTheme(uiStore.menuTheme))
/** 页面切换动画名称，reduceMotion 或选择“无”时禁用 */
const pageTransitionName = computed(() => {
  const mode = uiStore.preference.pageTransition
  if (mode === 'none' || uiStore.preference.reduceMotion) {
    return ''
  }
  return `erp-page-${mode}`
})
const isSidebarCollapsed = computed<boolean>({
  get: () => uiStore.sidebarCollapsed,
  set: (val) => uiStore.setSidebarCollapsed(val),
})
const pendingTodoCount = ref(0)
const expandedMenuRenderKey = ref(0)
let refreshExpandedMenuTimer: number | null = null
let unreadNoticeRefreshTimer: number | null = null
const asideWidth = computed(() => (isSidebarCollapsed.value ? '76px' : `${uiStore.sidebarWidth}px`))
const currentMenuTheme = computed(
  () => menuThemeOptions.find((item) => item.id === menuTheme.value) || menuThemeOptions[0]
)
const displayUserName = computed(() => {
  const nickName = String(userStore.nickName || '').trim()
  const userName = String(userStore.userName || '').trim()
  return nickName || userName || '管理员'
})
const userInitial = computed(() => displayUserName.value.slice(0, 1).toUpperCase())
const displayTenantId = computed(() => {
  const tenantId = String(userStore.tenantId || localStorage.getItem('tenantId') || '000000').trim()
  return tenantId || '000000'
})
const isHomePage = computed(() => {
  if (route.path === entryHomePath.value) {
    return true
  }
  return route.path === '/' && entryHomePath.value === HOME_MENU_PATH
})
const currentPageTitle = computed(() => {
  const title = route.meta?.title
  if (typeof title === 'string' && title.trim().length > 0) {
    return title
  }
  return '当前页面'
})

/**
 * 应用并持久化菜单主题
 * @param themeId 菜单主题编码
 */
const applyMenuTheme = (themeId: string) => {
  uiStore.setMenuTheme(normalizeMenuTheme(themeId))
}

/**
 * 切换左侧菜单展开/收起状态，并持久化到本地缓存。
 */
const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
  if (!isSidebarCollapsed.value) {
    if (refreshExpandedMenuTimer !== null) {
      window.clearTimeout(refreshExpandedMenuTimer)
    }
    // 展开后延迟刷新一次菜单实例，避免收起/展开后菜单状态错乱
    refreshExpandedMenuTimer = window.setTimeout(() => {
      expandedMenuRenderKey.value += 1
      refreshExpandedMenuTimer = null
    }, 180)
  }
}

/**
 * 返回菜单图标名称，确保每个菜单均有图标。
 * @param menu 菜单节点
 * @returns 图标组件名
 */
const resolveMenuIcon = (menu: any) => {
  const iconName = String(menu?.icon || '').trim()
  return (iconName && iconName !== '#') ? iconName : 'Grid'
}

/**
 * 标准化菜单 path，确保收起态点击始终可导航。
 * @param path 原始路径
 * @returns 标准化路径，非法值返回空字符串
 */
const normalizeMenuPath = (path: string): string => {
  const rawPath = String(path || '').trim()
  if (!rawPath || rawPath === '#') {
    return ''
  }
  if (/^(https?:)?\/\//i.test(rawPath)) {
    return rawPath
  }
  return rawPath.startsWith('/') ? rawPath : `/${rawPath}`
}

/**
 * 判断路径是否为有效可导航目标。
 * @param path 标准化前或标准化后的路径
 * @returns 是否可导航
 */
const isNavigablePath = (path: string): boolean => {
  const normalizedPath = normalizeMenuPath(path)
  if (!normalizedPath) {
    return false
  }
  if (/^https?:\/\//i.test(normalizedPath)) {
    return true
  }
  return router.resolve(normalizedPath).matched.length > 0
}

/**
 * 获取菜单节点可导航路径。
 * 优先递归查找子节点有效路由，再回退到自身有效路由。
 * @param menu 菜单节点
 * @returns 可导航路径，若不存在则返回空字符串
 */
const resolveMenuTargetPath = (menu: any): string => {
  const children = Array.isArray(menu?.children) ? menu.children : []
  for (const child of children) {
    const childPath = resolveMenuTargetPath(child)
    if (childPath) {
      return childPath
    }
  }
  const directPath = normalizeMenuPath(menu?.path)
  if (isNavigablePath(directPath)) {
    return directPath
  }
  return ''
}

/**
 * 获取收起态一级菜单对应的可跳转子菜单列表。
 * @param menu 一级菜单
 * @returns 子菜单列表
 */
const getCollapsedChildren = (menu: any): CollapsedChildMenuItem[] => {
  const menuKey = String(menu?.menuId ?? menu?.path ?? '')
  return collapsedChildrenMap.value.get(menuKey) || []
}

/**
 * 判断收起态菜单是否存在可展示子菜单。
 * @param menu 一级菜单
 * @returns 是否存在子菜单
 */
const hasCollapsedChildren = (menu: any): boolean => {
  return getCollapsedChildren(menu).length > 0
}

/**
 * 根据目标路径执行导航，统一处理内外链与重复跳转。
 * @param targetPath 目标路径
 */
const navigateByTargetPath = (targetPath: string) => {
  if (!targetPath) {
    return
  }
  if (/^https?:\/\//i.test(targetPath)) {
    window.open(targetPath, '_blank')
    return
  }
  if (route.path === targetPath) {
    return
  }
  router.push(targetPath).catch((error) => {
    console.error(error)
  })
}

/**
 * 收起态菜单点击跳转。
 * @param menu 菜单节点
 */
const handleCollapsedMenuClick = (menu: any) => {
  const targetPath = resolveMenuTargetPath(menu)
  navigateByTargetPath(targetPath)
}

/**
 * 收起态悬停菜单项命令处理。
 * @param command 下拉命令，约定为子菜单目标路径
 */
const handleCollapsedDropdownCommand = (command: string | number | object) => {
  if (typeof command !== 'string') {
    return
  }
  navigateByTargetPath(command)
}

/**
 * 判断收起态菜单是否激活。
 * @param menu 菜单节点
 * @returns 是否命中当前路由
 */
const isCollapsedMenuActive = (menu: any): boolean => {
  const currentPath = route.path
  const ownPath = String(menu?.path || '').trim()
  if (ownPath && ownPath === currentPath) {
    return true
  }
  const children = Array.isArray(menu?.children) ? menu.children : []
  return children.some((child: any) => isCollapsedMenuActive(child))
}

/**
 * 解析流程待办接口返回的数量。
 * 优先读取分页 total，若后端未分页则回退为列表长度。
 *
 * @param response 流程待办接口响应
 * @returns 待办数量
 */
const resolvePendingTodoCount = (response: any): number => {
  const total = Number(response?.total ?? response?.data?.total)
  if (Number.isFinite(total) && total >= 0) {
    return total
  }
  const rowList = Array.isArray(response?.rows)
    ? response.rows
    : Array.isArray(response?.data?.rows)
      ? response.data.rows
      : Array.isArray(response?.data)
        ? response.data
        : []
  return rowList.length
}

/**
 * 加载头部待办事项数量。
 * 无权限或异常时统一回退为 0，避免影响主流程。
 */
const loadPendingTodoCount = async () => {
  if (!hasPermi('workflow:todo:list')) {
    pendingTodoCount.value = 0
    return
  }
  try {
    const response: any = await listWorkflowTask(undefined, { skipAuthReset: true })
    pendingTodoCount.value = resolvePendingTodoCount(response)
  } catch (error) {
    console.error('获取头部待办事项数量失败', error)
    pendingTodoCount.value = 0
  }
}

/**
 * 跳转到待办事项页面。
 */
const goNotice = () => {
  if (route.path === NOTICE_ROUTE_PATH) {
    return
  }
  router.push(NOTICE_ROUTE_PATH).catch(() => {})
}

/**
 * 跳转到首页。
 * 若已在首页则不重复跳转。
 */
const goHome = () => {
  if (isHomePage.value) {
    return
  }
  router.push(entryHomePath.value).catch(() => {})
}

/**
 * 跳转个人中心页面
 */
const goProfile = () => {
  router.push('/system/profile')
}

/**
 * 打开菜单风格切换弹窗
 */
const openThemeDialog = () => {
  themeDialogVisible.value = true
}

/**
 * 跳转到登录页。
 * 使用 replace 避免浏览器回退返回到系统内页。
 */
const redirectToLogin = () => {
  router.replace('/login').catch((error) => {
    console.error(error)
  })
}

/**
 * 执行退出登录。
 * 先清理本地登录态并立即跳转登录页，后端退出接口异步通知，避免页面停留在系统内部。
 */
const handleLogout = () => {
  const logoutRequest = logoutApi().catch((error) => {
    console.error(error)
  })
  userStore.logout()
  // 退出后停止向服务端回写偏好，避免无 token 请求
  uiStore.serverLoaded = false
  redirectToLogin()
  return logoutRequest
}

/**
 * 处理右上角用户下拉菜单命令
 * @param command 菜单命令
 */
const handleUserCommand = (command: string | number | object) => {
  if (command === 'profile') {
    goProfile()
    return
  }
  if (command === 'theme') {
    openThemeDialog()
    return
  }
  if (command === 'logout') {
    handleLogout()
  }
}

watch(
  () => route.path,
  (currentPath) => {
    if (
      currentPath.startsWith('/workbench/system-notice')
      || currentPath.startsWith('/workbench/process-todo')
      || currentPath.startsWith('/workbench/message')
      || currentPath.startsWith('/system/notice')
    ) {
      void loadPendingTodoCount()
    }
  }
)

onMounted(() => {
  void loadPendingTodoCount()
  unreadNoticeRefreshTimer = window.setInterval(() => {
    void loadPendingTodoCount()
  }, 60000)
})

onBeforeUnmount(() => {
  if (refreshExpandedMenuTimer !== null) {
    window.clearTimeout(refreshExpandedMenuTimer)
    refreshExpandedMenuTimer = null
  }
  if (unreadNoticeRefreshTimer !== null) {
    window.clearInterval(unreadNoticeRefreshTimer)
    unreadNoticeRefreshTimer = null
  }
})
</script>

<style scoped lang="scss">
.layout-container {
  --menu-border: var(--erp-s-border);
  --menu-logo-bg: var(--erp-s-raised);
  --menu-text: var(--erp-s-text-3);
  --menu-item-bg: transparent;
  --menu-hover-bg: var(--erp-s-tint-1);
  --menu-active-bg: var(--erp-s-v-bg);
  --menu-active-text: var(--erp-s-v-text);
  --menu-branch-line: var(--erp-s-border-soft);
  --menu-branch-dot: var(--erp-s-v-dot);
  --menu-accent: var(--erp-s-v-accent);
  width: 100%;
  height: 100vh;
  background: var(--erp-s-page);
  
  .aside {
    position: relative;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    background: var(--erp-s-surface);
    color: var(--erp-s-text);
    border-right: 1px solid var(--menu-border);
    transition: width 0.18s cubic-bezier(0.2, 0.8, 0.2, 1);
    will-change: width;
    contain: layout paint;

    &::before {
      display: none;
    }

    &::after {
      display: none;
    }
    
    .logo {
      position: relative;
      z-index: 1;
      height: 60px;
      margin: 10px 12px 8px;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      padding: 0 8px 0 12px;
      background: var(--menu-logo-bg);
      border: 1px solid var(--menu-border);
      color: var(--erp-s-text);
      box-shadow: 0 2px 6px rgba(18, 35, 66, 0.06);
      transition: padding 0.2s ease, justify-content 0.2s ease;
    }

    .logo__brand {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      min-width: 0;
      opacity: 1;
      transform: translateX(0);
      transition:
        opacity 0.16s ease,
        transform 0.2s ease;
    }

    .logo__title {
      font-size: calc(18px * var(--erp-font-scale, 1));
      font-weight: 700;
      letter-spacing: 0.02em;
      white-space: nowrap;
    }

    .sidebar-toggle {
      width: 28px;
      height: 28px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: 9px;
      border: 1px solid rgba(23, 81, 146, 0.2);
      background: linear-gradient(180deg, var(--erp-s-surface) 0%, var(--erp-s-tint-1) 100%);
      color: var(--erp-s-accent);
      cursor: pointer;
      transition: all 0.18s ease;
      flex-shrink: 0;

      &:hover {
        border-color: rgba(13, 111, 190, 0.38);
        background: linear-gradient(180deg, var(--erp-s-surface) 0%, var(--erp-s-tint-2) 100%);
        color: var(--erp-s-accent-strong);
      }

      &:active {
        transform: scale(0.98);
      }
    }
    
    .el-menu-vertical {
      position: relative;
      z-index: 1;
      border-right: none;
      background: var(--erp-s-surface);
      padding-bottom: 10px;
      min-height: 100%;
    }

    .menu-scroll {
      position: relative;
      z-index: 1;
      flex: 1;
      min-height: 0;
      overflow-y: auto;
      overflow-x: hidden;
      scroll-behavior: smooth;
      padding-bottom: 10px;
      scrollbar-width: thin;
      scrollbar-color: var(--erp-s-scroll) transparent;
    }

    .menu-scroll::-webkit-scrollbar {
      width: 6px;
    }

    .menu-scroll::-webkit-scrollbar-track {
      background: transparent;
    }

    .menu-scroll::-webkit-scrollbar-thumb {
      background: var(--erp-s-scroll);
      border-radius: 999px;
    }

    .menu-scroll:hover::-webkit-scrollbar-thumb {
      background: var(--erp-s-scroll-hover);
    }

    .collapsed-menu {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 8px 0 12px;
    }

    .collapsed-menu__item {
      width: 46px;
      height: 46px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: 14px;
      border: 1px solid var(--erp-s-border-soft);
      background: linear-gradient(180deg, var(--erp-s-surface) 0%, var(--erp-s-tint-1) 100%);
      color: var(--erp-s-text-3);
      cursor: pointer;
      box-shadow:
        inset 0 1px 0 rgba(255, 255, 255, 0.88),
        0 4px 10px rgba(34, 71, 118, 0.08);
      transition:
        background-color 0.18s ease,
        border-color 0.18s ease,
        color 0.18s ease,
        transform 0.18s ease,
        box-shadow 0.18s ease;

      &:hover {
        background: linear-gradient(180deg, var(--erp-s-tint-1) 0%, var(--erp-s-tint-1) 100%);
        border-color: var(--erp-s-border-strong);
        color: var(--erp-s-accent);
        transform: translateX(1px);
      }

      &.is-active {
        background: linear-gradient(180deg, var(--erp-s-tint-2) 0%, var(--erp-s-tint-3) 100%);
        border-color: var(--erp-s-border-strong);
        color: var(--erp-s-accent);
        box-shadow:
          0 8px 18px rgba(45, 107, 177, 0.2),
          inset 0 1px 0 rgba(255, 255, 255, 0.7);
      }

      &.is-disabled {
        opacity: 0.5;
        cursor: default;
        pointer-events: none;
        box-shadow: none;
      }

      .el-icon {
        font-size: calc(17px * var(--erp-font-scale, 1));
      }
    }

    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      position: relative;
      overflow: hidden;
      color: var(--menu-text) !important;
      background: var(--menu-item-bg) !important;
      margin: 4px 8px;
      border-radius: 12px;
      border: 1px solid transparent;
      font-weight: 600;
      transform: translateX(0);
      will-change: transform, background-color, border-color, box-shadow;
      transition:
        background-color 0.24s cubic-bezier(0.22, 0.61, 0.36, 1),
        border-color 0.24s cubic-bezier(0.22, 0.61, 0.36, 1),
        box-shadow 0.24s cubic-bezier(0.22, 0.61, 0.36, 1),
        transform 0.24s cubic-bezier(0.22, 0.61, 0.36, 1);
    }

    :deep(.el-menu-item::before),
    :deep(.el-sub-menu__title::before) {
      content: '';
      position: absolute;
      left: 7px;
      top: 8px;
      bottom: 8px;
      width: 3px;
      border-radius: 999px;
      background: var(--menu-accent);
      opacity: 0;
      transform: scaleY(0.5);
      transform-origin: center;
      transition: opacity 0.22s ease, transform 0.22s ease;
      pointer-events: none;
    }

    :deep(.el-menu-item .el-icon),
    :deep(.el-sub-menu__title .el-icon) {
      transition: transform 0.22s ease, color 0.22s ease;
    }

    :deep(.el-menu--vertical > .el-menu-item),
    :deep(.el-menu--vertical > .el-sub-menu > .el-sub-menu__title) {
      min-height: 44px;
      margin: 6px 8px;
      font-size: calc(14px * var(--erp-font-scale, 1));
      font-weight: 700;
      background: var(--erp-s-surface) !important;
      border-color: transparent;
      box-shadow: none;
    }

    :deep(.el-menu--vertical > .el-menu-item .el-icon),
    :deep(.el-menu--vertical > .el-sub-menu > .el-sub-menu__title .el-icon) {
      font-size: calc(16px * var(--erp-font-scale, 1));
      color: var(--erp-s-text-3);
    }

    :deep(.el-sub-menu .el-menu) {
      background: transparent !important;
      padding: 2px 0 8px;
      margin: 0 8px 6px 24px;
      border-left: 1px solid var(--menu-branch-line);
    }

    :deep(.el-sub-menu .el-menu-item) {
      min-height: 36px;
      height: 36px;
      margin: 2px 0 2px 10px;
      padding-left: 24px !important;
      border-radius: 10px;
      font-size: calc(13px * var(--erp-font-scale, 1));
      font-weight: 500;
      letter-spacing: 0.01em;
      color: var(--erp-s-text-2) !important;
      background: transparent !important;
      border: 1px solid transparent;
      box-shadow: none;
    }

    :deep(.el-sub-menu .el-menu-item::before) {
      left: -11px;
      top: 50%;
      bottom: auto;
      width: 10px;
      height: 1px;
      border-radius: 0;
      opacity: 1;
      transform: translateY(-50%);
      background: var(--menu-branch-line);
      box-shadow: none;
    }

    :deep(.el-sub-menu .el-menu-item::after) {
      content: '';
      position: absolute;
      left: -14px;
      top: 50%;
      width: 5px;
      height: 5px;
      border-radius: 50%;
      background: var(--menu-branch-dot);
      transform: translateY(-50%);
      transition: transform 0.2s ease, background-color 0.2s ease;
      pointer-events: none;
    }

    :deep(.el-sub-menu .el-menu-item .el-icon) {
      font-size: calc(13px * var(--erp-font-scale, 1));
      opacity: 0.9;
    }

    :deep(.el-menu-item:hover),
    :deep(.el-sub-menu__title:hover) {
      background: var(--menu-hover-bg) !important;
      color: var(--menu-active-text) !important;
      border-color: var(--erp-s-border-soft);
      transform: translateX(2px);
    }

    :deep(.el-menu-item:hover::before),
    :deep(.el-sub-menu__title:hover::before) {
      opacity: 0.58;
      transform: scaleY(1);
    }

    :deep(.el-menu-item:hover .el-icon),
    :deep(.el-sub-menu__title:hover .el-icon) {
      transform: translateX(1px);
      color: var(--menu-active-text);
    }

    :deep(.el-menu-item.is-active) {
      background: var(--menu-active-bg) !important;
      color: var(--menu-active-text) !important;
      border-color: var(--erp-s-border-active);
      box-shadow: 0 6px 14px rgba(47, 125, 226, 0.14);
      transform: translateX(3px);
    }

    :deep(.el-menu-item.is-active::before) {
      opacity: 1;
      transform: scaleY(1);
    }

    :deep(.el-sub-menu .el-menu-item:hover) {
      background: var(--erp-s-tint-1) !important;
      border-color: var(--erp-s-border-soft);
      transform: translateX(1px);
    }

    :deep(.el-sub-menu .el-menu-item:hover::after),
    :deep(.el-sub-menu .el-menu-item.is-active::after) {
      background: var(--menu-active-text);
      transform: translateY(-50%) scale(1.08);
    }

    :deep(.el-sub-menu .el-menu-item.is-active) {
      background: var(--erp-s-tint-2) !important;
      border-color: var(--erp-s-border-active);
      color: var(--menu-active-text) !important;
      box-shadow: 0 4px 10px rgba(47, 125, 226, 0.12);
      transform: translateX(2px);
    }

    :deep(.el-menu-item-group__title) {
      transition: opacity 0.2s ease;
    }

    &.is-collapsed {
      .logo {
        justify-content: center;
        padding: 0;
      }

      .logo__brand {
        opacity: 0;
        pointer-events: none;
        position: absolute;
        left: 12px;
        top: 50%;
        transform: translateY(-50%) translateX(-6px);
      }

      .sidebar-toggle {
        position: static;
      }
    }
  }
  
  .header {
    margin: 14px 14px 0;
    border-radius: 18px;
    background: var(--erp-s-surface);
    border: 1px solid var(--menu-border);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
    box-shadow: 0 10px 28px rgba(8, 40, 86, 0.1);

    .header-left {
      display: flex;
      align-items: center;
      min-width: 0;
    }

    .page-nav {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      min-height: 38px;
      padding: 4px 12px 4px 6px;
      border-radius: 16px;
      border: 1px solid rgba(22, 78, 142, 0.16);
      background: linear-gradient(135deg, var(--erp-c-glass-strong), var(--erp-c-glass));
      box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.56);
    }

    .page-nav__home {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 30px;
      padding: 0 12px;
      border: 1px solid transparent;
      border-radius: 9px;
      background: transparent;
      color: var(--erp-s-accent);
      font-size: calc(13px * var(--erp-font-scale, 1));
      font-weight: 700;
      cursor: pointer;
      transition: all 0.18s ease;

      &:hover {
        border-color: rgba(17, 112, 197, 0.24);
        background: rgba(17, 112, 197, 0.1);
      }

      &.is-active {
        border-color: rgba(13, 111, 190, 0.34);
        background: linear-gradient(135deg, rgba(13, 111, 190, 0.14), rgba(63, 151, 222, 0.16));
        color: var(--erp-s-accent-strong);
      }
    }

    .page-nav__arrow {
      color: var(--erp-s-text-4);
      font-size: calc(12px * var(--erp-font-scale, 1));
    }

    .page-nav__current {
      max-width: 260px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      padding: 0 2px;
      color: var(--erp-s-heading);
      font-size: calc(14px * var(--erp-font-scale, 1));
      font-weight: 700;

      &.is-home {
        color: var(--erp-s-text-3);
        font-weight: 600;
      }
    }

    .header-right {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      flex-shrink: 0;
    }

    .notice-entry {
      width: 34px;
      height: 34px;
      border: none;
      border-radius: 10px;
      background: linear-gradient(180deg, var(--erp-s-notice-bg) 0%, var(--erp-s-notice-bg-2) 100%);
      color: var(--erp-s-notice-ink);
      display: inline-flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.18s ease;
      box-shadow: 0 6px 14px rgba(199, 148, 16, 0.22);

      &:hover {
        background: linear-gradient(180deg, var(--erp-s-notice-bg) 0%, var(--erp-s-notice-bg-2) 100%);
        transform: translateY(-1px);
      }

      &:active {
        transform: translateY(0);
      }
    }

    .notice-entry__badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      line-height: 1;

      .el-icon {
        font-size: calc(16px * var(--erp-font-scale, 1));
      }
    }
    
    .user-trigger {
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 4px 8px;
      border-radius: 12px;
      border: 1px solid transparent;
      transition: all 0.18s ease;

      &:hover {
        border-color: rgba(29, 120, 201, 0.2);
        background: rgba(29, 120, 201, 0.08);
      }
    }

    .user-trigger__avatar {
      width: 26px;
      height: 26px;
      border-radius: 50%;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      background: var(--erp-s-avatar);
      color: var(--erp-s-accent);
      border: 1px solid rgba(35, 87, 139, 0.24);
      font-size: calc(12px * var(--erp-font-scale, 1));
      font-weight: 700;
      flex-shrink: 0;
    }

    .user-trigger__name {
      color: var(--erp-s-heading);
      font-weight: 600;
      font-size: calc(14px * var(--erp-font-scale, 1));
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      max-width: 96px;
    }

    .user-trigger__tenant {
      color: var(--erp-s-text-3);
      font-size: calc(12px * var(--erp-font-scale, 1));
      padding: 2px 8px;
      border-radius: 999px;
      border: 1px solid rgba(29, 120, 201, 0.16);
      background: rgba(29, 120, 201, 0.08);
      white-space: nowrap;
      flex-shrink: 0;
    }

    .user-trigger__arrow {
      color: var(--erp-s-text-4);
      font-size: calc(12px * var(--erp-font-scale, 1));
      flex-shrink: 0;
    }

  }
  
  .main {
    position: relative;
    isolation: isolate;
    padding: 18px 14px 14px;
    overflow: auto;
    background: var(--erp-s-main);

    &::before {
      display: none;
    }

    &::after {
      display: none;
    }

    > * {
      position: relative;
      z-index: 1;
    }
  }
}

.theme-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.theme-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  text-align: left;
  border: 1px solid rgba(31, 74, 124, 0.16);
  border-radius: 12px;
  padding: 12px;
  background: var(--erp-s-surface);
  color: var(--erp-s-text);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: rgba(13, 111, 190, 0.5);
    transform: translateY(-1px);
  }

  &.is-active {
    border-color: var(--erp-s-accent);
    box-shadow: 0 0 0 2px rgba(13, 111, 190, 0.15);
  }

  .theme-card__swatch {
    display: block;
    width: 100%;
    height: 50px;
    border-radius: 10px;
    border: 1px solid rgba(255, 255, 255, 0.48);
  }

  .theme-card__name {
    font-size: calc(15px * var(--erp-font-scale, 1));
    font-weight: 700;
    line-height: 1.3;
  }

  .theme-card__desc {
    font-size: calc(12px * var(--erp-font-scale, 1));
    color: var(--erp-s-text-3);
    line-height: 1.4;
  }
}

@media (max-width: 992px) {
  .layout-container {
    .aside {
      width: 200px !important;
    }
  }
}

@media (max-width: 768px) {
  .theme-grid {
    grid-template-columns: 1fr;
  }

  .layout-container {
    .aside {
      width: 64px !important;

      .logo {
        .logo__title {
          display: none;
        }
      }

      :deep(.el-sub-menu__title span),
      :deep(.el-menu-item span) {
        display: none;
      }
    }

    .header {
      margin: 10px 10px 0;
      padding: 0 12px;

      .page-nav {
        padding-right: 10px;
      }

      .page-nav__current {
        max-width: 150px;
      }

      .user-trigger {
        padding: 4px 6px;
      }

      .notice-entry {
        width: 30px;
        height: 30px;
        border-radius: 9px;
      }

      .user-trigger__tenant {
        display: none;
      }

      .user-trigger__name {
        max-width: 72px;
      }
    }

    .main {
      padding: 12px 10px 10px;

      &::after {
        right: -210px;
        top: 72px;
        width: 78vw;
        height: 62vw;
        opacity: 0.1;
      }
    }
  }
}
:deep(.user-dropdown-menu) {
  min-width: 160px;
  padding: 4px;
}

:deep(.user-dropdown-menu .el-dropdown-menu__item) {
  display: flex;
  justify-content: flex-end;
  text-align: right;
}

:deep(.notice-entry__badge .el-badge__content) {
  border: 1px solid var(--erp-s-surface);
  min-width: 14px;
  height: 14px;
  padding: 0 3px;
  font-size: calc(10px * var(--erp-font-scale, 1));
  line-height: 12px;
  box-shadow: 0 2px 6px rgba(210, 33, 57, 0.26);
}

:deep(.collapsed-submenu-dropdown.el-popper) {
  position: relative;
  min-width: 312px;
  max-width: min(360px, calc(100vw - 110px));
  padding: 0;
  border-radius: 18px;
  border: 1px solid var(--erp-s-border-soft);
  background: var(--erp-s-surface);
  box-shadow:
    0 16px 36px rgba(20, 52, 92, 0.18),
    0 2px 8px rgba(20, 52, 92, 0.08);
  overflow: visible;
}

:deep(.collapsed-submenu-dropdown.el-popper::before) {
  content: '';
  position: absolute;
  left: -12px;
  top: 26px;
  width: 12px;
  height: 22px;
  clip-path: polygon(100% 0, 0 50%, 100% 100%);
  background: var(--erp-s-surface);
  filter: drop-shadow(-1px 1px 0 rgba(223, 232, 243, 0.96));
}

:deep(.collapsed-submenu-dropdown .el-dropdown-menu) {
  padding: 8px 0;
  border: none;
  border-radius: 18px;
  box-shadow: none;
  background: transparent;
  max-height: min(66vh, 520px);
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: var(--erp-s-scroll) transparent;
}

:deep(.collapsed-submenu-dropdown .collapsed-submenu-dropdown__title) {
  min-height: 50px;
  justify-content: center;
  font-size: calc(21px * var(--erp-font-scale, 1));
  font-weight: 500;
  color: var(--erp-s-text-4);
  border-bottom: 1px solid var(--erp-s-border-soft);
  margin: 0 16px 4px;
  padding: 8px 0 14px;
  cursor: default;
}

:deep(.collapsed-submenu-dropdown .collapsed-submenu-dropdown__title.is-disabled) {
  color: var(--erp-s-text-4);
  opacity: 1;
}

:deep(.collapsed-submenu-dropdown .collapsed-submenu-dropdown__item) {
  min-height: 56px;
  margin: 0 14px;
  border-radius: 12px;
  justify-content: center;
  color: var(--erp-s-text-3);
  font-size: calc(15px * var(--erp-font-scale, 1));
  font-weight: 500;
  border-top: 1px solid var(--erp-s-border-soft);
  transition: background-color 0.18s ease, color 0.18s ease;
}

:deep(.collapsed-submenu-dropdown .collapsed-submenu-dropdown__item:first-of-type) {
  border-top: none;
}

:deep(.collapsed-submenu-dropdown .collapsed-submenu-dropdown__label) {
  width: 100%;
  text-align: center;
  letter-spacing: 0.02em;
}

:deep(.collapsed-submenu-dropdown .collapsed-submenu-dropdown__item:hover),
:deep(.collapsed-submenu-dropdown .collapsed-submenu-dropdown__item:focus-visible) {
  background: linear-gradient(180deg, var(--erp-s-tint-1) 0%, var(--erp-s-tint-1) 100%);
  color: var(--erp-s-accent);
}

:deep(.collapsed-submenu-dropdown .collapsed-submenu-dropdown__item.is-active) {
  background: linear-gradient(180deg, var(--erp-s-tint-2) 0%, var(--erp-s-tint-2) 100%);
  color: var(--erp-s-accent);
}

:deep(.collapsed-submenu-dropdown .el-dropdown-menu::-webkit-scrollbar) {
  width: 6px;
}

:deep(.collapsed-submenu-dropdown .el-dropdown-menu::-webkit-scrollbar-track) {
  background: transparent;
}

:deep(.collapsed-submenu-dropdown .el-dropdown-menu::-webkit-scrollbar-thumb) {
  background: var(--erp-s-scroll);
  border-radius: 999px;
}

</style>




