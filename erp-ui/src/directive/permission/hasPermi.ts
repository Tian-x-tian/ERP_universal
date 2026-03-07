import { useUserStore } from '@/store/user'

/**
 * 权限指令
 * v-hasPermi="['system:user:add']"
 */
export default {
    mounted(el: HTMLElement, binding: any) {
        const { value } = binding
        const userStore = useUserStore()
        const all_permission = "*:*:*"
        const permissions = userStore.permissions

        if (value && value instanceof Array && value.length > 0) {
            const permissionFlag = value
            const hasPermissions = permissions.some(permission => {
                return all_permission === permission || permissionFlag.includes(permission)
            })

            if (!hasPermissions) {
                el.parentNode && el.parentNode.removeChild(el)
            }
        } else {
            throw new Error(`请设置操作权限标签值`)
        }
    }
}
