// stores/auth.js
import { defineStore } from 'pinia'
import request from '@/api/request'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null,
    permissions: [],
    menuVersion: 0,
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    userName: (state) => state.userInfo?.name || '',
    userRole: (state) => state.userInfo?.role || '',

    hasPermission: (state) => (permission) => {
      if (!permission) return true
      // 管理员拥有所有权限（如果您的业务逻辑是这样）
      if (state.userInfo?.role === 'admin') return true
      return state.permissions.includes(permission)
    }
  },

  actions: {
    // 登录
    async login(username, password) {
      try {
        const res = await request.post('/auth/login', { username, password })
        if (res.status === 'success') {
          this.token = res.data.token
          localStorage.setItem('token', this.token)
          this.userInfo = res.data.userInfo
          // 登录后加载权限
          await this.loadPermissions()
          return { success: true }
        }
        return { success: false, message: res.info || '登录失败' }
      } catch (error) {
        return { success: false, message: error.message || '登录失败' }
      }
    },

    // ========== 关键修改：使用 /permissions 接口 ==========
    async loadPermissions() {
      try {
        // 后端接口：GET /permissions 返回 List<String>
        const res = await request.get('/permissions')
        if (res.status === 'success' && res.data) {
          // res.data 是字符串数组，如 ['dashboard:view', 'user:list', ...]
          this.permissions = res.data
          // 增加版本号，触发菜单重新渲染
          this.menuVersion++
          console.log('权限已加载:', this.permissions)
        } else {
          this.permissions = []
        }
      } catch (error) {
        console.error('加载权限失败:', error)
        this.permissions = []
      }
    },

    // 刷新权限（供外部调用）
    async refreshPermissions() {
      await this.loadPermissions()
    },

    // 退出登录
    logout() {
      this.token = ''
      this.userInfo = null
      this.permissions = []
      this.menuVersion = 0
      localStorage.removeItem('token')
    }
  }
})