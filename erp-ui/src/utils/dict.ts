import { ref } from 'vue'
import { getDicts } from '@/api/system/dict'

/**
 * 获取字典数据
 * @param args 字典类型数组
 */
export function useDict(...args: string[]) {
    const res = ref<{ [key: string]: any[] }>({})

    return (() => {
        args.forEach((dictType) => {
            res.value[dictType] = []
            getDicts(dictType).then(response => {
                res.value[dictType] = response.data.map((p: any) => ({
                    label: p.dict_label,
                    value: p.dict_value,
                    elTagType: p.list_class,
                    elTagClass: p.css_class
                }))
            })
        })
        return res.value
    })()
}
