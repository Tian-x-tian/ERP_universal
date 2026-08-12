import { reactive, toRefs } from 'vue'
import { getDicts } from '@/api/system/dict'

/**
 * 获取字典数据
 * @param args 字典类型数组
 */
export function useDict(...args: string[]) {
    const dictMap = reactive<Record<string, any[]>>({})

    args.forEach((dictType) => {
        dictMap[dictType] = []
        getDicts(dictType)
            .then((response: any) => {
                const rows = Array.isArray(response?.data) ? response.data : []
                dictMap[dictType] = rows.map((p: any) => ({
                    // 兼容下划线和驼峰两种字段风格
                    label: p.dict_label ?? p.dictLabel,
                    value: p.dict_value ?? p.dictValue,
                    elTagType: p.list_class ?? p.listClass,
                    elTagClass: p.css_class ?? p.cssClass
                }))
            })
            .catch(() => {
                dictMap[dictType] = []
            })
    })

    return toRefs(dictMap) as Record<string, any>
}
