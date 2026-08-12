import { hasPermi } from '@/utils/permission'

/**
 * 权限指令
 * v-hasPermi="['system:user:add']"
 */
export default {
    mounted(el: HTMLElement, binding: any) {
        const { value } = binding
        if (value && value instanceof Array && value.length > 0) {
            const permissionFlag = value
            const hasPermissions = hasPermi(permissionFlag)
            if (!hasPermissions) {
                el.parentNode && el.parentNode.removeChild(el)
            }
        } else {
            throw new Error(`请设置操作权限标签值`)
        }
    }
}
