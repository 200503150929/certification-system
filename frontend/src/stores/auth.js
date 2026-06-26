import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi } from '@/api/auth'
import router from '@/router'

/**
 * 认证状态管理 Store
 *
 * 集中管理 token、用户信息、登录/登出逻辑。
 * 其他页面统一从此 Store 读取当前用户信息，禁止直接从 localStorage 读取。
 */
export const useAuthStore = defineStore('auth', () => {
  // ============ State ============
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const username = ref(localStorage.getItem('username') || '')
  const name = ref(localStorage.getItem('displayName') || '')
  const role = ref(localStorage.getItem('userRole') || '')

  // ============ Getters ============
  const isLoggedIn = computed(() => !!token.value)

  const userRole = computed(() => role.value)

  const userName = computed(() => name.value || username.value)

  const displayName = computed(() => name.value || username.value)

  // ============ Actions ============

  /**
   * 登录
   * @param {Object} credentials - { username, password }
   * @returns {Promise<Object>} 登录响应数据
   */
  async function login(credentials) {
    const res = await loginApi(credentials.username, credentials.password)

    if (res.status === 'success' && res.data) {
      const { token: tk, userId: uid, username: uname, name: nm, role: rl } = res.data

      // 更新 state
      token.value = tk
      userId.value = String(uid || '')
      username.value = uname || ''
      name.value = nm || ''
      role.value = rl || ''

      // 持久化到 localStorage
      localStorage.setItem('token', tk)
      localStorage.setItem('userId', String(uid || ''))
      localStorage.setItem('username', uname || '')
      localStorage.setItem('displayName', nm || '')
      localStorage.setItem('userRole', rl || '')

      return res.data
    }

    throw new Error(res.info || '登录失败')
  }

  /**
   * 登出
   * 清除 state + localStorage 并跳转到登录页
   */
  function logout() {
    // 清除 state
    token.value = ''
    userId.value = ''
    username.value = ''
    name.value = ''
    role.value = ''

    // 清除 localStorage
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('displayName')
    localStorage.removeItem('userRole')


    router.push('/login')
  }

  /**
   * 获取当前用户角色对应的首页路径
   */
  function getHomePath() {
    return role.value === 'student' ? '/my-courses' : '/dashboard'
  }

  return {
    // state
    token,
    userId,
    username,
    name,
    role,
    // getters
    isLoggedIn,
    userRole,
    userName,
    displayName,
    // actions
    login,
    logout,
    getHomePath
  }
})