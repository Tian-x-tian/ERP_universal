import axios from 'axios'
import type { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

interface ApiResponse<T = any> {
    code: number
    message?: string
    data: T
    success?: boolean
    timestamp?: string
    traceId?: string
    path?: string
}

interface RequestConfig extends InternalAxiosRequestConfig {
    authMode?: 'required' | 'public'
    skipAuthReset?: boolean
}

const SUCCESS_CODE = 0
const PARAM_ERROR_CODE = 40001
const UNAUTHORIZED_CODE = 40101
const FORBIDDEN_CODE = 40301
const CONFLICT_CODE = 40901
const VALIDATE_FAILED_CODE = 42201
const SYSTEM_ERROR_CODE = 50001
const POST_LOGIN_INIT_FLAG = 'erpPostLoginInitPending'

// 创建 axios 实例
const service: any = axios.create({
    baseURL: import.meta.env.VITE_APP_BASE_API || '/api',
    timeout: 10000,
})

/**
 * 判断请求是否为登录接口，避免将“登录日志”等包含 login 字段的业务接口误判。
 *
 * @param requestUrl 请求地址
 * @returns 是否为登录接口
 */
function isLoginEndpoint(requestUrl: string) {
    if (!requestUrl) {
        return false
    }
    const [rawPath] = requestUrl.split('?')
    const normalizedPath = rawPath.replace(/\/+$/, '')
    return normalizedPath === '/login' || normalizedPath.endsWith('/login')
}

/**
 * 判断请求是否为登录后初始化接口。
 *
 * @param requestUrl 请求地址
 * @returns 是否为登录后初始化接口
 */
function isPostLoginInitEndpoint(requestUrl: string) {
    if (!requestUrl) {
        return false
    }
    const [rawPath] = requestUrl.split('?')
    const normalizedPath = rawPath.replace(/\/+$/, '')
    return normalizedPath === '/system/user/getInfo' || normalizedPath === '/system/user/getRouters'
}

/**
 * 根据业务码解析默认错误提示。
 *
 * @param code 业务码
 * @returns 默认错误提示
 */
function getDefaultMessageByCode(code: number): string {
    switch (code) {
        case PARAM_ERROR_CODE:
            return '请求参数错误'
        case UNAUTHORIZED_CODE:
            return '登录状态已失效，请重新登录'
        case FORBIDDEN_CODE:
            return '无权限执行该操作'
        case CONFLICT_CODE:
            return '当前状态冲突，请刷新后重试'
        case VALIDATE_FAILED_CODE:
            return '业务校验未通过'
        case SYSTEM_ERROR_CODE:
            return '系统异常，请稍后重试'
        default:
            return '请求失败，请稍后重试'
    }
}

/**
 * 将 HTTP 状态码映射为业务码兜底值。
 *
 * @param status HTTP 状态码
 * @returns 业务码
 */
function mapHttpStatusToBusinessCode(status: number): number {
    if (status === 401) {
        return UNAUTHORIZED_CODE
    }
    if (status === 403) {
        return FORBIDDEN_CODE
    }
    if (status === 409) {
        return CONFLICT_CODE
    }
    if (status >= 400 && status < 500) {
        return PARAM_ERROR_CODE
    }
    return SYSTEM_ERROR_CODE
}

/**
 * 判断请求是否为匿名公共请求。
 *
 * @param config 请求配置
 * @returns 是否为公共请求
 */
function isPublicRequest(config?: Partial<RequestConfig> | null) {
    return config?.authMode === 'public'
}

/**
 * 判断当前请求失败时是否需要清理登录态。
 *
 * @param config 请求配置
 * @returns true 表示需要清理登录态
 */
function shouldResetAuthState(config?: Partial<RequestConfig> | null) {
    if (isPublicRequest(config)) {
        return false
    }
    return config?.skipAuthReset !== true
}

/**
 * 统一处理业务错误提示与跳转。
 *
 * @param code 业务码
 * @param message 提示信息
 * @param requestUrl 请求地址
 * @param shouldResetAuth 是否需要清理当前登录态
 */
function handleBusinessError(code: number, message: string, requestUrl: string, shouldResetAuth: boolean) {
    const isLoginRequest = isLoginEndpoint(requestUrl)
    const isOnLoginPage = window.location.pathname === '/login'
    const isPostLoginInit = sessionStorage.getItem(POST_LOGIN_INIT_FLAG) === '1' && isPostLoginInitEndpoint(requestUrl)
    const shouldSuppressLoginPage401Hint = code === UNAUTHORIZED_CODE && isOnLoginPage && !isLoginRequest
    const finalMessage = isPostLoginInit ? `登录成功后初始化失败：${message}` : message

    if (!shouldSuppressLoginPage401Hint) {
        ElMessage.error(finalMessage)
    }

    if (code === UNAUTHORIZED_CODE && shouldResetAuth) {
        if (isPostLoginInit) {
            sessionStorage.removeItem(POST_LOGIN_INIT_FLAG)
        }
        localStorage.clear()
        if (!isOnLoginPage) {
            window.location.replace('/login')
        }
    }
}

// 请求拦截器
service.interceptors.request.use(
    (config: RequestConfig) => {
        const requestUrl = typeof config.url === 'string' ? config.url : ''
        const isLoginRequest = isLoginEndpoint(requestUrl)
        const publicRequest = isPublicRequest(config)

        const token = localStorage.getItem('token')
        const tenantId = localStorage.getItem('tenantId')

        // 1. 非登录请求统一注入 Token + tenantId，避免登录接口被旧会话污染
        if (!isLoginRequest && !publicRequest) {
            if (token && tenantId) {
                config.headers['Authorization'] = `Bearer ${token}`
                if (!config.headers['tenantId'] && !config.headers['Tenantid']) {
                    config.headers['tenantId'] = tenantId
                }
            } else if (token && !tenantId) {
                localStorage.clear()
                window.location.replace('/login')
                return Promise.reject(new Error('租户信息缺失，请重新登录'))
            }
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
        const res = response.data as ApiResponse
        if (!res || typeof res.code !== 'number') {
            const formatErrorMessage = '响应格式异常，请联系管理员'
            ElMessage.error(formatErrorMessage)
            return Promise.reject(new Error(formatErrorMessage))
        }
        if (res.code !== SUCCESS_CODE) {
            const requestUrl = typeof response.config?.url === 'string' ? response.config.url : ''
            const shouldResetAuth = shouldResetAuthState(response.config as RequestConfig)
            const message = res.message || getDefaultMessageByCode(res.code)
            handleBusinessError(res.code, message, requestUrl, shouldResetAuth)
            return Promise.reject(new Error(message))
        }
        return res
    },
    (error: AxiosError<ApiResponse>) => {
        const response = error.response
        if (response) {
            const requestUrl = typeof response.config?.url === 'string' ? response.config.url : ''
            const shouldResetAuth = shouldResetAuthState(response.config as RequestConfig)
            const responseCode = typeof response.data?.code === 'number' ? response.data.code : undefined
            const code = responseCode ?? mapHttpStatusToBusinessCode(response.status)
            const message = response.data?.message || getDefaultMessageByCode(code)
            handleBusinessError(code, message, requestUrl, shouldResetAuth)
            return Promise.reject(new Error(message))
        }

        ElMessage.error(error.message || '网络异常')
        return Promise.reject(error)
    }
)

export default service
