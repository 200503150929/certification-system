import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

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
    // 如果后端返回了业务错误，抛出异常让调用方处理
    if (data.status === 'error') {
      return Promise.reject(new Error(data.info || '请求失败'))
    }
    return data
  },
  (error) => {
    if (error.response) {
      const { status } = error.response
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