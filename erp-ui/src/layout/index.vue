<template>
  <el-container class="layout-container">
    <el-aside width="240px" class="aside">
      <div class="logo">
        <el-icon size="24"><Platform /></el-icon>
        <span>ERP 系统</span>
      </div>
      <el-menu
        active-text-color="#ffd04b"
        background-color="#304156"
        class="el-menu-vertical"
        :default-active="$route.path"
        text-color="#fff"
        router
      >
        <template v-for="menu in userStore.menuList" :key="menu.menuId">
          <!-- 有子菜单 -->
          <el-sub-menu v-if="menu.children && menu.children.length > 0" :index="menu.path || String(menu.menuId)">
            <template #title>
              <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
              <span>{{ menu.menuName }}</span>
            </template>
            <el-menu-item v-for="child in menu.children" :key="child.menuId" :index="child.path">
              <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>
              <span>{{ child.menuName }}</span>
            </el-menu-item>
          </el-sub-menu>
          <!-- 无子菜单 -->
          <el-menu-item v-else :index="menu.path">
            <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
            <span>{{ menu.menuName }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown>
            <span class="user-info">
              Admin <el-icon><ArrowDown /></el-icon>
            </span>
            <template #header>
              <el-dropdown-menu>
                <el-dropdown-item>个人中心</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { Platform, ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.layout-container {
  width: 100%;
  height: 100vh;
  background: linear-gradient(180deg, #f5f9ff 0%, #eef4fb 100%);
  
  .aside {
    background: linear-gradient(180deg, #0b2e67 0%, #0d4c8f 60%, #1175a0 100%);
    color: #fff;
    border-right: 1px solid rgba(255, 255, 255, 0.12);
    
    .logo {
      height: 60px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      font-size: 20px;
      font-weight: 700;
      letter-spacing: 0.02em;
      background: rgba(8, 31, 71, 0.35);
    }
    
    .el-menu-vertical {
      border-right: none;
      background: transparent;
    }

    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      color: rgba(243, 250, 255, 0.92) !important;
    }

    :deep(.el-menu-item.is-active) {
      background: rgba(255, 255, 255, 0.2) !important;
      color: #fff !important;
      border-radius: 10px;
      margin: 4px 8px;
    }
  }
  
  .header {
    margin: 14px 14px 0;
    border-radius: 14px;
    background: rgba(255, 255, 255, 0.88);
    border: 1px solid rgba(27, 62, 108, 0.12);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
    box-shadow: 0 10px 28px rgba(8, 40, 86, 0.1);
    
    .user-info {
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 5px;
      color: #1f2f46;
      font-weight: 600;
    }
  }
  
  .main {
    position: relative;
    padding: 18px 14px 14px;
    overflow: auto;
    background:
      linear-gradient(120deg, rgba(11, 46, 103, 0.08) 0%, rgba(17, 117, 160, 0.08) 100%),
      url('../assets/login-values-bg.svg') center/cover no-repeat;

    &::before {
      content: '';
      position: absolute;
      inset: 0;
      background:
        radial-gradient(circle at 15% 18%, rgba(255, 255, 255, 0.72), transparent 34%),
        radial-gradient(circle at 92% 8%, rgba(255, 158, 102, 0.18), transparent 28%);
      pointer-events: none;
    }

    > * {
      position: relative;
      z-index: 1;
    }
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
  .layout-container {
    .aside {
      width: 64px !important;

      .logo {
        span {
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
    }

    .main {
      padding: 12px 10px 10px;
    }
  }
}
</style>
