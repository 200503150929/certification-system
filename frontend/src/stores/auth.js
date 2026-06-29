// stores/auth.js - 完整版

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi } from '@/api/auth'
import router from '@/router'

export const useAuthStore = defineStore('auth', () => {
  // ============ State ============
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const username = ref(localStorage.getItem('username') || '')
  const name = ref(localStorage.getItem('displayName') || '')
  const role = ref(localStorage.getItem('userRole') || '')
  const permissions = ref(loadPermissionsFromStorage())
  const permissionsLoaded = ref(false)

  function loadPermissionsFromStorage() {
    try {
      const stored = localStorage.getItem('permissions')
      if (stored) {
        const parsed = JSON.parse(stored)
        if (Array.isArray(parsed) && parsed.length > 0) {
          return parsed
        }
      }
      return []
    } catch {
      return []
    }
  }

  // ============ Getters ============
  const isLoggedIn = computed(() => !!token.value)

  const userRole = computed(() => role.value)

  const userName = computed(() => name.value || username.value)

  const displayName = computed(() => name.value || username.value)

  const hasPermission = computed(() => (permissionCode) => {
    if (!permissionCode) return true
    if (!permissions.value || permissions.value.length === 0) return false
    return permissions.value.includes(permissionCode)
  })

  // ============ Actions ============

  async function login(credentials) {
    const res = await loginApi(credentials.username, credentials.password)

    if (res.status === 'success' && res.data) {
      const { token: tk, userId: uid, username: uname, name: nm, role: rl, permissions: perms } = res.data

      // ✅ 处理权限：如果后端没返回，根据角色设置默认权限
      let finalPermissions = perms || []
      if (finalPermissions.length === 0) {
        console.log('后端未返回权限，根据角色设置默认权限')
        const roleLower = (rl || '').toLowerCase()
        if (roleLower === 'admin') {
          finalPermissions = ['dashboard:view', 'user:list', 'role:list', 'course:manage', 'program:manage', 'program:detail']
        } else if (roleLower === 'teacher') {
          finalPermissions = ['dashboard:view', 'course:list', 'course:teach', 'course:student-list', 'program:view']
        } else if (roleLower === 'student') {
          finalPermissions = ['dashboard:view', 'course:list', 'course:detail', 'program:view']
        } else {
          finalPermissions = ['dashboard:view']
        }
        console.log('分配的默认权限:', finalPermissions)
      }

      // 更新 state
      token.value = tk
      userId.value = String(uid || '')
      username.value = uname || ''
      name.value = nm || ''
      role.value = rl || ''
      permissions.value = finalPermissions
      permissionsLoaded.value = true

      // 持久化到 localStorage
      localStorage.setItem('token', tk)
      localStorage.setItem('userId', String(uid || ''))
      localStorage.setItem('username', uname || '')
      localStorage.setItem('displayName', nm || '')
      localStorage.setItem('userRole', rl || '')
      localStorage.setItem('permissions', JSON.stringify(finalPermissions))

      return res.data
    }

    throw new Error(res.info || '登录失败')
  }

  async function loadPermissions() {
    if (permissionsLoaded.value && permissions.value.length > 0) {
      return Promise.resolve()
    }

    const stored = loadPermissionsFromStorage()
    if (stored && stored.length > 0) {
      permissions.value = stored
      permissionsLoaded.value = true
      return Promise.resolve()
    }

    permissions.value = []
    permissionsLoaded.value = true
    return Promise.resolve()
  }

  function logout() {
    token.value = ''
    userId.value = ''
    username.value = ''
    name.value = ''
    role.value = ''
    permissions.value = []
    permissionsLoaded.value = false

    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('displayName')
    localStorage.removeItem('userRole')
    localStorage.removeItem('permissions')

    router.push('/login')
  }

  function getHomePath() {
    if (!token.value) return '/login'

    const roleLower = (role.value || '').toLowerCase()
    if (roleLower === 'admin') {
      return '/dashboard'
    }
    if (roleLower === 'teacher' || roleLower === 'student') {
      return '/app/my-courses'
    }
    return '/dashboard'
  }

  return {
    token,
    userId,
    username,
    name,
    role,
    permissions,
    permissionsLoaded,
    isLoggedIn,
    userRole,
    userName,
    displayName,
    hasPermission,
    login,
    loadPermissions,
    logout,
    getHomePath
  }
})