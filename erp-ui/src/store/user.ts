import { defineStore } from 'pinia'
import request from '@/utils/request'

interface UserState {
    token: string | null
    tenantId: string | null
    userName: string | null
    nickName: string | null
    roles: string[]
    permissions: string[]
    menuList: any[]
}

export const useUserStore = defineStore('user', {
    state: (): UserState => ({
        token: localStorage.getItem('token'),
        tenantId: localStorage.getItem('tenantId'),
        userName: null,
        nickName: null,
        roles: [],
        permissions: [],
        menuList: [],
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
                    this.userName = user.userName
                    this.nickName = user.nickName
                    this.roles = res.data.roles
                    this.permissions = res.data.permissions
                    resolve(res)
                }).catch(error => {
                    reject(error)
                })
            })
        },
        /** 获取路由信息 */
        getRouters() {
            return new Promise((resolve, reject) => {
                request({
                    url: '/system/user/getRouters',
                    method: 'get'
                }).then((res: any) => {
                    this.menuList = res.data
                    resolve(res.data)
                }).catch(error => {
                    reject(error)
                })
            })
        },
        logout() {
            this.token = null
            this.tenantId = null
            this.userName = null
            this.nickName = null
            this.roles = []
            this.permissions = []
            this.menuList = []
            localStorage.removeItem('token')
            localStorage.removeItem('tenantId')
        },
    },
})
