import { useAuthStore } from '@/stores/auth'

/**
 * 权限控制自定义指令 v-permission
 *
 * 用法：
 *   <el-button v-permission="'user:add'">添加用户</el-button>
 *   <el-menu-item v-permission="'dashboard:view'" index="/dashboard">仪表盘</el-menu-item>
 *   <div v-permission:or="['user:add', 'user:update']">有其一即可见</div>
 *
 * 不带修饰符：所有指定权限都必须拥有（AND 逻辑）
 * 修饰符 .or：拥有任一指定权限即可见
 */
export default {
  mounted(el, binding) {
    const authStore = useAuthStore()
    const value = binding.value
    const hasPermission = authStore.hasPermission

    let hasPerm = false

    if (typeof value === 'string') {
      // 单个权限标识符
      hasPerm = hasPermission(value)
    } else if (Array.isArray(value)) {
      if (binding.modifiers.or) {
        // OR 逻辑：任一权限即可
        hasPerm = value.some(p => hasPermission(p))
      } else {
        // AND 逻辑：所有权限都必须拥有
        hasPerm = value.every(p => hasPermission(p))
      }
    } else {
      // 无参数：默认显示
      hasPerm = true
    }

    if (!hasPerm) {
      el.parentNode?.removeChild(el)
    }
  }
}