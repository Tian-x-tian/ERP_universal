import { defineStore } from 'pinia'
import request from '@/utils/request'
import {
    defaultMenuList,
    HOME_MENU_PATH,
    type MenuBlueprintNode,
    MENU_BLUEPRINT_GROUPS
} from '@/constants/default-menu'

const ADMIN_ROLE_KEY = 'admin'
const ALL_PERMISSION = '*:*:*'
const VIRTUAL_HOME_MENU_ID = -10001
const VIRTUAL_MENU_ID_START = -30000
const POST_LOGIN_INIT_FLAG = 'erpPostLoginInitPending'

interface UserState {
    userId: number | null
    token: string | null
    tenantId: string | null
    userName: string | null
    nickName: string | null
    infoLoaded: boolean
    roles: string[]
    permissions: string[]
    menuList: any[]
    homeEntryPath: string
}

interface MenuNormalizeResult {
    menuList: any[]
    homeEntryPath: string
}

/**
 * 深拷贝菜单树，避免直接修改接口返回对象导致视图联动异常。
 * @param menuList 原始菜单树
 * @returns 深拷贝后的菜单树
 */
function cloneMenuTree(menuList: any[]): any[] {
    if (!Array.isArray(menuList)) {
        return []
    }
    return menuList.map((item) => ({
        ...item,
        children: Array.isArray(item.children) ? cloneMenuTree(item.children) : undefined
    }))
}

/**
 * 递归遍历菜单树并执行回调。
 * @param menuList 菜单树
 * @param callback 节点处理回调
 * @returns void
 */
function walkMenuTree(menuList: any[], callback: (node: any) => void): void {
    for (const item of menuList) {
        callback(item)
        if (Array.isArray(item.children) && item.children.length > 0) {
            walkMenuTree(item.children, callback)
        }
    }
}

/**
 * 标准化 path，统一大小写与结尾斜杠，避免匹配异常。
 * @param value 原始路径
 * @returns 标准化路径
 */
function normalizePath(value: string): string {
    return String(value || '').trim().toLowerCase().replace(/\/+$/, '')
}

/**
 * 构建菜单 path 到节点的索引。
 * @param menuList 菜单树
 * @returns path 索引 Map
 */
function buildPathNodeMap(menuList: any[]): Map<string, any> {
    const pathNodeMap = new Map<string, any>()
    walkMenuTree(menuList, (node) => {
        const path = normalizePath(node?.path || '')
        if (!path) {
            return
        }
        if (!pathNodeMap.has(path)) {
            pathNodeMap.set(path, node)
        }
    })
    return pathNodeMap
}

/**
 * 根据候选路径从索引中命中首个菜单节点。
 * @param pathNodeMap path 索引
 * @param candidatePaths 候选路径列表
 * @returns 命中的菜单节点
 */
function pickMenuNodeByPaths(pathNodeMap: Map<string, any>, candidatePaths: string[]): any | undefined {
    for (const candidatePath of candidatePaths || []) {
        const normalizedPath = normalizePath(candidatePath)
        if (!normalizedPath) {
            continue
        }
        const node = pathNodeMap.get(normalizedPath)
        if (node) {
            return node
        }
    }
    return undefined
}

/**
 * 判断当前角色权限集合是否具备任一目标权限。
 * 支持精确匹配、`xxx:*` 前缀匹配及管理员角色。
 *
 * @param roleKeys 当前用户角色编码集合
 * @param permissions 当前用户权限标识集合
 * @param requiredPerms 目标权限集合
 * @returns 是否具备权限
 */
function hasMenuPermission(roleKeys: string[], permissions: string[], requiredPerms?: string[]): boolean {
    if (!Array.isArray(requiredPerms) || requiredPerms.length === 0) {
        return true
    }
    const normalizedRoles = Array.isArray(roleKeys)
        ? roleKeys.map((roleKey) => String(roleKey || '').trim().toLowerCase())
        : []
    if (normalizedRoles.includes(ADMIN_ROLE_KEY)) {
        return true
    }
    const permissionList = Array.isArray(permissions) ? permissions : []
    return requiredPerms.some((requiredPerm) => {
        return permissionList.some((permission) => {
            if (permission === ALL_PERMISSION || permission === requiredPerm) {
                return true
            }
            if (typeof permission === 'string' && permission.endsWith(':*')) {
                const prefix = permission.substring(0, permission.length - 1)
                return requiredPerm.startsWith(prefix)
            }
            return false
        })
    })
}

/**
 * 递归解析菜单节点的首个可进入路由。
 *
 * @param menuNode 菜单节点
 * @returns 首个可进入路由
 */
function resolveMenuEntryPath(menuNode: any): string {
    const childList = Array.isArray(menuNode?.children) ? menuNode.children : []
    for (const child of childList) {
        const childEntryPath = resolveMenuEntryPath(child)
        if (childEntryPath) {
            return childEntryPath
        }
    }
    const currentPath = String(menuNode?.path || '').trim()
    if (currentPath && currentPath !== HOME_MENU_PATH) {
        return currentPath
    }
    return ''
}

/**
 * 解析无首页权限时的默认进入路由。
 *
 * @param menuList 结构化菜单列表
 * @returns 首个可进入路由
 */
function resolveFallbackEntryPath(menuList: any[]): string {
    for (const item of menuList) {
        const entryPath = resolveMenuEntryPath(item)
        if (entryPath) {
            return entryPath
        }
    }
    return HOME_MENU_PATH
}

/**
 * 判断当前是否为普通用户（非 admin 角色）。
 * @param roleKeys 当前用户角色编码集合
 * @returns true 表示普通用户
 */
function isNormalUser(roleKeys: string[]): boolean {
    return !roleKeys.some((roleKey) => String(roleKey).trim().toLowerCase() === ADMIN_ROLE_KEY)
}

/**
 * 归一化菜单结构，统一首页与消息待办中心的展示与兜底行为。
 * @param menuList 原始菜单树
 * @param forceTodoCenter 是否强制挂载消息待办中心（普通用户默认启用）
 * @returns 归一化结果（菜单树 + 首页入口路径）
 */
function normalizeMenus(menuList: any[], roleKeys: string[], permissions: string[], forceTodoCenter: boolean): MenuNormalizeResult {
    const sourceMenuTree = cloneMenuTree(menuList)
    const pathNodeMap = buildPathNodeMap(sourceMenuTree)
    const hasHomePermission = !!pickMenuNodeByPaths(pathNodeMap, [HOME_MENU_PATH])
    const usedMenuIds = new Set<number | string>([VIRTUAL_HOME_MENU_ID])
    const structuredMenus: any[] = []
    const menuIdSeed = { current: VIRTUAL_MENU_ID_START }

    structuredMenus.push({
        menuId: VIRTUAL_HOME_MENU_ID,
        menuName: '首页',
        path: HOME_MENU_PATH,
        icon: 'House'
    })

    for (const group of MENU_BLUEPRINT_GROUPS) {
        const normalizedGroup = buildNormalizedMenuNode(group, {
            inheritedSourceVisible: false,
            parentPermissionGranted: true,
            roleKeys,
            permissions,
            forceTodoCenter,
            pathNodeMap,
            usedMenuIds,
            menuIdSeed,
        })
        if (normalizedGroup) {
            structuredMenus.push(normalizedGroup)
        }
    }

    const homeEntryPath = hasHomePermission
        ? HOME_MENU_PATH
        : resolveFallbackEntryPath(structuredMenus)
    return {
        menuList: structuredMenus,
        homeEntryPath
    }
}

interface MenuBuildContext {
    inheritedSourceVisible: boolean
    parentPermissionGranted: boolean
    roleKeys: string[]
    permissions: string[]
    forceTodoCenter: boolean
    pathNodeMap: Map<string, any>
    usedMenuIds: Set<number | string>
    menuIdSeed: { current: number }
}

/**
 * 生成一个当前上下文下唯一的虚拟菜单 ID。
 *
 * @param menuIdSeed 菜单 ID 游标
 * @returns 虚拟菜单 ID
 */
function nextVirtualMenuId(menuIdSeed: { current: number }): number {
    const nextId = menuIdSeed.current
    menuIdSeed.current -= 1
    return nextId
}

/**
 * 为菜单节点分配唯一菜单 ID，优先复用后端菜单 ID。
 *
 * @param preferredMenuId 候选菜单 ID
 * @param fallbackMenuId 蓝图默认菜单 ID
 * @param usedMenuIds 已占用菜单 ID 集合
 * @param menuIdSeed 虚拟菜单 ID 游标
 * @returns 唯一菜单 ID
 */
function resolveUniqueMenuId(
    preferredMenuId: number | string | undefined,
    fallbackMenuId: number | string | undefined,
    usedMenuIds: Set<number | string>,
    menuIdSeed: { current: number }
): number | string {
    const candidateIdList = [preferredMenuId, fallbackMenuId]
    for (const candidateId of candidateIdList) {
        if (candidateId === undefined || candidateId === null || usedMenuIds.has(candidateId)) {
            continue
        }
        usedMenuIds.add(candidateId)
        return candidateId
    }
    const virtualMenuId = nextVirtualMenuId(menuIdSeed)
    usedMenuIds.add(virtualMenuId)
    return virtualMenuId
}

/**
 * 基于菜单蓝图和当前权限上下文递归构建前端菜单树。
 *
 * @param blueprintNode 菜单蓝图节点
 * @param context 构建上下文
 * @returns 可见菜单节点，不可见时返回 null
 */
function buildNormalizedMenuNode(blueprintNode: MenuBlueprintNode, context: MenuBuildContext): any | null {
    const sourceNode = pickMenuNodeByPaths(
        context.pathNodeMap,
        Array.isArray(blueprintNode.sourcePaths) && blueprintNode.sourcePaths.length > 0
            ? blueprintNode.sourcePaths
            : [blueprintNode.path]
    )
    const sourceVisible = !!sourceNode || context.inheritedSourceVisible || (!!context.forceTodoCenter && blueprintNode.forceForNormalUser === true)
    const permissionGranted = blueprintNode.inheritParentPermission
        ? context.parentPermissionGranted
        : hasMenuPermission(context.roleKeys, context.permissions, blueprintNode.permissionKeys)
    const childList = Array.isArray(blueprintNode.children)
        ? blueprintNode.children
            .map((childNode) => buildNormalizedMenuNode(childNode, {
                ...context,
                inheritedSourceVisible: sourceVisible,
                parentPermissionGranted: permissionGranted,
            }))
            .filter((childNode): childNode is any => !!childNode)
        : []

    if (childList.length === 0 && (!sourceVisible || !permissionGranted)) {
        return null
    }

    return {
        ...sourceNode,
        menuId: resolveUniqueMenuId(sourceNode?.menuId, blueprintNode.menuId, context.usedMenuIds, context.menuIdSeed),
        menuName: blueprintNode.menuName,
        path: blueprintNode.path,
        icon: (() => {
            const raw = blueprintNode.icon || sourceNode?.icon || ''
            const sanitized = String(raw).trim()
            return (sanitized && sanitized !== '#') ? sanitized : 'Grid'
        })(),
        children: childList.length > 0 ? childList : undefined,
    }
}

export const useUserStore = defineStore('user', {
    state: (): UserState => ({
        userId: null,
        token: localStorage.getItem('token'),
        tenantId: localStorage.getItem('tenantId'),
        userName: null,
        nickName: null,
        infoLoaded: false,
        roles: [],
        permissions: [],
        menuList: [],
        homeEntryPath: HOME_MENU_PATH,
    }),
    actions: {
        setToken(token: string) {
            this.token = token
            localStorage.setItem('token', token)
        },
        setTenantId(tenantId: string) {
            this.tenantId = tenantId
            localStorage.setItem('tenantId', tenantId)
        },
        /** 获取用户信息 */
        getInfo() {
            return new Promise((resolve, reject) => {
                request({
                    url: '/system/user/getInfo',
                    method: 'get'
                }).then((res: any) => {
                    const user = res.data.user
                    const roles = Array.isArray(res?.data?.roles) ? res.data.roles : []
                    const permissions = Array.isArray(res?.data?.permissions) ? res.data.permissions : []
                    this.userId = typeof user?.userId === 'number' ? user.userId : null
                    this.userName = user.userName
                    this.nickName = user.nickName
                    this.roles = roles
                    this.permissions = permissions
                    this.infoLoaded = true
                    resolve(res)
                }).catch(error => {
                    reject(error)
                })
            })
        },
        /** 获取路由信息 */
        getRouters() {
            return new Promise((resolve) => {
                request({
                    url: '/system/user/getRouters',
                    method: 'get'
                }).then((res: any) => {
                    const rawMenus = Array.isArray(res.data) && res.data.length > 0 ? res.data : defaultMenuList
                    const normalizedResult = normalizeMenus(rawMenus, this.roles, this.permissions, isNormalUser(this.roles))
                    this.menuList = normalizedResult.menuList
                    this.homeEntryPath = normalizedResult.homeEntryPath
                    sessionStorage.removeItem(POST_LOGIN_INIT_FLAG)
                    resolve(this.menuList)
                }).catch(error => {
                    console.error(error)
                    const normalizedResult = normalizeMenus(defaultMenuList, this.roles, this.permissions, isNormalUser(this.roles))
                    this.menuList = normalizedResult.menuList
                    this.homeEntryPath = normalizedResult.homeEntryPath
                    resolve(this.menuList)
                })
            })
        },
        logout() {
            this.token = null
            this.tenantId = null
            this.userId = null
            this.userName = null
            this.nickName = null
            this.roles = []
            this.permissions = []
            this.menuList = []
            this.homeEntryPath = HOME_MENU_PATH
            this.infoLoaded = false
            sessionStorage.removeItem(POST_LOGIN_INIT_FLAG)
            localStorage.removeItem('token')
            localStorage.removeItem('tenantId')
        },
    },
})
