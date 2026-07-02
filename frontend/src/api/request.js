import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const request = axios.create({
    baseURL: '/api',
    timeout: 15000
})

// ========== 新增：需要静默处理的接口列表 ==========
// 这些接口返回错误时不弹窗，只输出日志
const SILENT_APIS = [
    '/admin/program/get',
    '/admin/program/objectives/list',
    '/admin/program/requirements/list',
    '/admin/program/indicators/list',
    '/teacher/objectives/indicators',
    '/teacher/objectives/matrix'
]

// 请求拦截器：添加 Token
request.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 响应拦截器：统一处理错误
request.interceptors.response.use(
    (response) => {
        const { data } = response
        const url = response.config.url || ''

        // 如果后端返回了业务错误
        if (data.status === 'error') {
            // ========== 修改：检查是否需要静默处理 ==========
            const isSilent = SILENT_APIS.some(api => url.includes(api))

            if (isSilent) {
                // 静默处理：只输出日志，不弹窗
                console.warn(`[静默] ${url}:`, data.info || data.message || '请求失败')
                // 仍然返回错误，让业务代码自己处理
                return Promise.reject(new Error(data.info || '请求失败'))
            }

            // 非静默接口：弹窗提示
            ElMessage.error(data.info || data.message || '请求失败')
            return Promise.reject(new Error(data.info || '请求失败'))
        }
        return data
    },
    (error) => {
        if (error.response) {
            const { status, config } = error.response
            const url = config?.url || ''

            // ========== 修改：HTTP错误也检查静默 ==========
            const isSilent = SILENT_APIS.some(api => url.includes(api))

            if (status === 401) {
                ElMessage.error('登录已过期，请重新登录')
                localStorage.removeItem('token')
                localStorage.removeItem('userRole')
                localStorage.removeItem('permissions')
                localStorage.removeItem('username')
                localStorage.removeItem('displayName')
                window.location.href = '/login'
                return Promise.reject(error)
            } else if (status === 403) {
                ElMessage.error('没有权限访问')
                return Promise.reject(error)
            }

            // 其他HTTP错误，提取后端返回的消息
            const backendMsg = error.response.data?.info || error.response.data?.message

            // 如果是静默接口，不弹窗
            if (isSilent) {
                console.warn(`[静默HTTP] ${url}:`, backendMsg || `请求失败 (${status})`)
                return Promise.reject(new Error(backendMsg || `请求失败 (${status})`))
            }

            // 非静默接口才弹窗
            return Promise.reject(new Error(backendMsg || `请求失败 (${status})`))
        }

        // 网络错误或业务异常
        if (error.message) {
            return Promise.reject(error)
        }
        ElMessage.error('网络错误，请检查网络连接')
        return Promise.reject(new Error('网络错误'))
    }
)

export default request