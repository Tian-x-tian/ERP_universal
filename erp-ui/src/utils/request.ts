import axios from 'axios'
import type { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const service: AxiosInstance = axios.create({
    baseURL: import.meta.env.VITE_APP_BASE_API || '/api',
    timeout: 10000,
})

// 请求拦截器
service.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        // 1. 注入 Token
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }

        // 2. 注入多租户标识
        const tenantId = localStorage.getItem('tenantId')
        if (tenantId) {
            config.headers['tenantId'] = tenantId
        }

        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 响应拦截器
service.interceptors.response.use(
    (response: AxiosResponse) => {
        const res = response.data
        const message = res.message || res.msg || 'Error'
        // 如果 code 不为 200 则报错 (根据后端统一响应对象 R 结构)
        if (res.code !== 200) {
            ElMessage.error(message)
            if (res.code === 401) {
                // Token 过期或无效
                localStorage.clear()
                window.location.href = '/login'
            }
            return Promise.reject(new Error(message))
        }
        return res
    },
    (error) => {
        ElMessage.error(error.message || '网络异常')
        return Promise.reject(error)
    }
)

export default service
