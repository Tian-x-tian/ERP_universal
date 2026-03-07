import request from '@/utils/request'

/**
 * 查询租户列表
 */
export function listTenant() {
    return request({
        url: '/system/tenant/list',
        method: 'get'
    })
}

/**
 * 查询租户详细
 */
export function getTenant(id: number | string) {
    return request({
        url: '/system/tenant/' + id,
        method: 'get'
    })
}

/**
 * 新增租户
 */
export function addTenant(data: any) {
    return request({
        url: '/system/tenant',
        method: 'post',
        data: data
    })
}

/**
 * 修改租户
 */
export function updateTenant(data: any) {
    return request({
        url: '/system/tenant',
        method: 'put',
        data: data
    })
}

/**
 * 删除租户
 */
export function delTenant(id: number | string) {
    return request({
        url: '/system/tenant/' + id,
        method: 'delete'
    })
}
