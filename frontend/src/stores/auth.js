// stores/auth.js
import { defineStore } from 'pinia'
import request from '@/api/request'

// ============ 安全解析 JSON 的辅助函数 ============
const safeJSONParse = (str, defaultValue = null) => {
  if (!str || str === 'undefined' || str === 'null' || str === '') {
    return defaultValue
  }
  try {
    return JSON.parse(str)
  } catch (e) {
    console.warn('JSON 解析失败:', str, e)
    return defaultValue
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: safeJSONParse(localStorage.getItem('userInfo'), null),
    permissions: [],
    menuVersion: 0,
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    userName: (state) => state.userInfo?.name || state.userInfo?.username || '',
    userRole: (state) => state.userInfo?.role || '',
    userId: (state) => state.userInfo?.id || state.userInfo?.userId || null,

    /**
     * 权限检查方法
     *
     * 核心逻辑：
     * 1. 如果 permission 为空，返回 true（无需权限）
     * 2. 如果权限列表为空（未加载），返回 true（避免误拦）
     * 3. admin 角色也基于权限列表判断，不再硬编码返回 true
     * 4. 检查权限列表中是否包含该权限
     */
    hasPermission: (state) => (permission) => {
      if (!permission) return true

      // 如果权限列表为空，可能是还没加载，保守返回 true 避免误拦
      if (state.permissions.length === 0) {
        console.warn('权限列表为空，默认允许访问:', permission)
        return true
      }

      // 所有角色（包括 admin）都基于权限列表判断
      return state.permissions.includes(permission)
    }
  },

  actions: {
    /**
     * 用户登录
     */
    async login(username, password) {
      try {
        const res = await request.post('/auth/login', { username, password })
        console.log('登录响应:', res)

        if (res.status === 'success') {
          const data = res.data

          // 保存 token
          this.token = data.token
          localStorage.setItem('token', this.token)

          // 保存用户信息
          this.userInfo = {
            id: data.userId || data.id,
            username: data.username,
            name: data.name || data.username,
            role: data.role,
            permissions: data.permissions || []
          }
          localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
          console.log('userInfo 已保存:', this.userInfo)

          // 加载权限
          if (data.permissions && data.permissions.length > 0) {
            this.permissions = [...data.permissions]
            this.menuVersion = Date.now()
            console.log('登录时从后端获取权限:', this.permissions.length, '条')
          } else {
            // 如果登录接口没有返回权限，单独加载
            await this.loadPermissions()
          }

          return { success: true }
        }
        return { success: false, message: res.info || '登录失败' }
      } catch (error) {
        console.error('登录异常:', error)
        return { success: false, message: error.message || '登录失败' }
      }
    },

    /**
     * 加载当前用户的权限列表
     * GET /api/permissions
     */
    async loadPermissions() {
      try {
        const res = await request.get('/permissions')
        console.log('loadPermissions 响应:', res)

        if (res.status === 'success' && res.data) {
          // 确保是数组
          const perms = Array.isArray(res.data) ? res.data : []
          this.permissions = [...perms]
          this.menuVersion = Date.now()
          console.log('权限已加载:', this.permissions.length, '条')
          console.log('权限列表:', this.permissions)

          // 同步更新 userInfo 中的权限（保持一致性）
          if (this.userInfo) {
            this.userInfo.permissions = this.permissions
            localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
          }
        } else {
          this.permissions = []
          this.menuVersion = Date.now()
          console.warn('loadPermissions: 响应数据异常', res)
        }
      } catch (error) {
        console.error('加载权限失败:', error)
        this.permissions = []
        this.menuVersion = Date.now()
      }
    },

    /**
     * 强制刷新权限（用于权限变更后同步）
     * 会清空缓存并重新从后端加载
     */
    async forceRefreshPermissions() {
      console.log('===== 强制刷新权限开始 =====')

      // 清空当前权限
      this.permissions = []
      this.menuVersion = Date.now()

      // 重新加载
      await this.loadPermissions()

      // 同步更新 localStorage 中的 userInfo 权限
      if (this.userInfo) {
        this.userInfo.permissions = this.permissions
        localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      }

      console.log('===== 强制刷新权限完成 =====')
      console.log('当前权限:', this.permissions)
      console.log('权限数量:', this.permissions.length)
      console.log('menuVersion:', this.menuVersion)

      return this.permissions
    },

    /**
     * 刷新权限（别名，兼容旧代码）
     */
    async refreshPermissions() {
      await this.loadPermissions()
    },

    /**
     * 根据用户角色和权限获取首页路径
     */
    getHomePath() {
      const role = this.userInfo?.role?.toLowerCase() || ''

      // 角色首页映射
      const roleHomeMap = {
        'admin': '/dashboard',
        'super_admin': '/dashboard',
        'teacher': '/app/my-courses',
        'student': '/app/my-courses'
      }

      if (roleHomeMap[role]) {
        return roleHomeMap[role]
      }

      // 根据权限动态判断
      if (this.hasPermission('dashboard:view')) {
        return '/dashboard'
      }
      if (this.hasPermission('course:list')) {
        return '/app/my-courses'
      }
      if (this.hasPermission('program:view')) {
        return '/app/curriculum/view'
      }

      return '/dashboard'
    },

    /**
     * 从 localStorage 恢复用户信息
     */
    restoreUserInfo() {
      const savedToken = localStorage.getItem('token')
      const savedUserInfo = localStorage.getItem('userInfo')

      if (savedToken) {
        this.token = savedToken
      }

      if (savedUserInfo) {
        const parsed = safeJSONParse(savedUserInfo, null)
        if (parsed) {
          this.userInfo = parsed
          // 如果 userInfo 中有权限，同步到 permissions
          if (parsed.permissions && Array.isArray(parsed.permissions)) {
            this.permissions = [...parsed.permissions]
            this.menuVersion = Date.now()
          }
          console.log('从 localStorage 恢复 userInfo:', this.userInfo)
          console.log('恢复的权限:', this.permissions)
          return true
        }
      }

      console.warn('无法恢复 userInfo')
      return false
    },

    /**
     * 退出登录
     */
    logout() {
      this.token = ''
      this.userInfo = null
      this.permissions = []
      this.menuVersion = 0
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      console.log('已退出登录，清除所有认证信息')
    }
  }
})