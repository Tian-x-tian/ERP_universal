import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes: RouteRecordRaw[] = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/login/index.vue'),
        meta: { title: '登录', isPublic: true },
    },
    {
        path: '/',
        name: 'Layout',
        component: () => import('@/layout/index.vue'),
        redirect: '/home',
        children: [
            {
                path: 'home',
                name: 'Home',
                component: () => import('@/views/home/index.vue'),
                meta: { title: '首页' },
            },
            {
                path: 'system/tenant',
                name: 'Tenant',
                component: () => import('@/views/system/tenant/index.vue'),
                meta: { title: '租户管理' },
            },
            {
                path: 'system/user',
                name: 'User',
                component: () => import('@/views/system/user/index.vue'),
                meta: { title: '用户管理' },
            },
            {
                path: 'system/role',
                name: 'Role',
                component: () => import('@/views/system/role/index.vue'),
                meta: { title: '角色管理' },
            },
            {
                path: 'system/menu',
                name: 'Menu',
                component: () => import('@/views/system/menu/index.vue'),
                meta: { title: '菜单管理' },
            },
            {
                path: 'system/dept',
                name: 'Dept',
                component: () => import('@/views/system/dept/index.vue'),
                meta: { title: '部门管理' },
            },
            {
                path: 'system/dict',
                name: 'Dict',
                component: () => import('@/views/system/dict/index.vue'),
                meta: { title: '字典管理' },
            },
            {
                path: 'system/config',
                name: 'Config',
                component: () => import('@/views/system/config/index.vue'),
                meta: { title: '参数管理' },
            },
        ],
    },
]

const router = createRouter({
    history: createWebHistory(),
    routes,
})

// 路由守卫
router.beforeEach(async (to, _from, next) => {
    const userStore = useUserStore()
    const token = userStore.token

    if (token) {
        if (to.path === '/login') {
            next({ path: '/' })
        } else {
            if (userStore.roles.length === 0) {
                try {
                    await userStore.getInfo()
                    await userStore.getRouters()
                    next({ ...to, replace: true })
                } catch (err) {
                    userStore.logout()
                    next({ path: '/login' })
                }
            } else {
                next()
            }
        }
    } else {
        if (to.path === '/login') {
            next()
        } else {
            next(`/login?redirect=${to.fullPath}`)
        }
    }
})

export default router
