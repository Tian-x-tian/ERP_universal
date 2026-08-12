import request from '@/utils/request'

/**
 * 查询菜单列表
 */
export function listMenu() {
    return request({
        url: '/system/menu/list',
        method: 'get'
    })
}

/**
 * 查询菜单树节点
 */
export function listMenuTree(params?: { parentId?: number | string; keyword?: string }) {
    return request({
        url: '/system/menu/tree',
        method: 'get',
        params
    })
}

/**
 * 查询菜单详细
 */
export function getMenu(menuId: number | string) {
    return request({
        url: '/system/menu/' + menuId,
        method: 'get'
    })
}

/**
 * 新增菜单
 */
export function addMenu(data: any) {
    return request({
        url: '/system/menu',
        method: 'post',
        data: data
    })
}

/**
 * 修改菜单
 */
export function updateMenu(data: any) {
    return request({
        url: '/system/menu',
        method: 'put',
        data: data
    })
}

/**
 * 删除菜单
 */
export function delMenu(menuId: number | string) {
    return request({
        url: '/system/menu/' + menuId,
        method: 'delete'
    })
}

/**
 * 同步当前页面菜单结构到菜单管理并入库
 */
export function syncMenu(data: any[]) {
    return request({
        url: '/system/menu/sync',
        method: 'post',
        data
    })
}
