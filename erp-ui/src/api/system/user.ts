import request from '@/utils/request'

/**
 * 查询用户列表
 */
export function listUser() {
    return request({
        url: '/system/user/list',
        method: 'get'
    })
}

/**
 * 查询用户详细
 */
export function getUser(userId: number | string) {
    return request({
        url: '/system/user/' + userId,
        method: 'get'
    })
}

/**
 * 新增用户
 */
export function addUser(data: any) {
    return request({
        url: '/system/user',
        method: 'post',
        data: data
    })
}

/**
 * 修改用户
 */
export function updateUser(data: any) {
    return request({
        url: '/system/user',
        method: 'put',
        data: data
    })
}

/**
 * 删除用户
 */
export function delUser(userId: number | string) {
    return request({
        url: '/system/user/' + userId,
        method: 'delete'
    })
}
