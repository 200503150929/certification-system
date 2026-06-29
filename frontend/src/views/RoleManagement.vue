<template>
  <div class="role-management-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色权限管理</span>
        </div>
        <p class="header-description">维护系统角色及其对应的功能菜单、数据访问权限范围。</p>
      </template>

      <div class="role-cards-container">
        <el-card v-for="role in roles" :key="role.id" class="role-card" shadow="hover">
          <div class="role-card-header">
            <div class="role-info">
              <div class="role-icon" :style="{ backgroundColor: role.iconBg }">
                <el-icon><component :is="role.icon" /></el-icon>
              </div>
              <div>
                <span class="role-name">{{ getRoleLabel(role.roleName) }}</span>
              </div>
            </div>
          </div>
          <p class="role-description">{{ role.description }}</p>
          <div class="role-actions">
            <div class="role-users">
              <el-icon><User /></el-icon>
              <span>{{ role.userCount }} 人</span>
            </div>
            <div class="role-buttons">
              <el-button type="primary" size="small" @click="openPermissionDialog(role)">
                配置权限
              </el-button>
              <el-button size="small" @click="goToUserManagement(role.roleName)">
                管理用户
              </el-button>
            </div>
          </div>
        </el-card>
      </div>
    </el-card>

    <!-- 权限配置对话框 -->
    <el-dialog
        v-model="permDialogVisible"
        :title="'权限配置 - ' + getRoleLabel(currentRole?.roleName || '')"
        width="700px"
        @close="resetPermDialog"
    >
      <div v-loading="permLoading" class="perm-dialog-body">
        <div v-for="(group, moduleName) in groupedPermissions" :key="moduleName" class="perm-group">
          <h4 class="perm-group-title">{{ moduleLabels[moduleName] || moduleName }}</h4>
          <div class="perm-items">
            <el-checkbox
                v-for="perm in group"
                :key="perm.id"
                v-model="perm.assigned"
                :label="perm.permissionCode"
                :disabled="permDisabled"
            >
              {{ perm.permissionName }}
            </el-checkbox>
          </div>
        </div>
        <el-empty v-if="!permLoading && Object.keys(groupedPermissions).length === 0" description="暂无权限数据" />
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPermissionChanges" :loading="saving">
          保存配置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Avatar, School } from '@element-plus/icons-vue'
import request from '@/api/request'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

// ============ 角色配置 ============
const roles = ref([])
const loading = ref(false)

const roleTemplates = {
  admin: { icon: Avatar, iconBg: '#409EFF', description: '系统最高权限，拥有所有菜单和按钮的访问权限，负责系统基础维护。' },
  teacher: { icon: School, iconBg: '#67C23A', description: '教学相关核心权限，可管理本人授课的课程、成绩录入。' },
  student: { icon: User, iconBg: '#E6A23C', description: '基础学习功能权限，可查看专业人才培养方案、课程资源和成绩。' }
}

const moduleLabels = {
  dashboard: '仪表盘',
  user: '用户管理',
  role: '角色权限',
  program: '培养方案管理',
  course: '课程体系管理',
  teaching: '课程教学管理',
  profile: '个人信息',
  achievement: '达成度分析'
}

// ============ 权限对话框状态 ============
const permDialogVisible = ref(false)
const permLoading = ref(false)
const saving = ref(false)
const currentRole = ref(null)
const permDisabled = ref(false)
const permissions = ref([])

// 按模块分组的权限
const groupedPermissions = computed(() => {
  const groups = {}
  permissions.value.forEach(p => {
    const mod = p.module || 'other'
    if (!groups[mod]) groups[mod] = []
    groups[mod].push(p)
  })
  return groups
})

// ============ 显示映射 ============
const getRoleLabel = (role) => {
  const map = { admin: '管理员', teacher: '教师', student: '学生' }
  return map[role] || role
}

const goToUserManagement = (roleName) => {
  router.push({ path: '/app/users', query: { role: roleName } })
}

// ============ 数据加载 ============
const loadRoles = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/roles/list')
    if (res.status === 'success' && res.data) {
      roles.value = res.data.map(r => ({
        id: r.id,
        roleName: r.roleName,
        ...(roleTemplates[r.roleName] || { icon: User, iconBg: '#909399', description: '' }),
        userCount: r.userCount || 0
      }))
    }
  } catch {
    roles.value = Object.entries(roleTemplates).map(([name, tmpl], index) => ({
      id: index + 1,
      roleName: name,
      ...tmpl,
      userCount: 0
    }))
  } finally {
    loading.value = false
  }
}

// ============ 权限对话框 ============
const openPermissionDialog = async (role) => {
  currentRole.value = role
  permDialogVisible.value = true
  permLoading.value = true

  try {
    const res = await request.get('/permissions/all', {
      params: { roleId: role.id }
    })
    if (res.status === 'success' && res.data) {
      permissions.value = res.data
    }
  } catch {
    ElMessage.error('加载权限数据失败')
    permissions.value = []
  } finally {
    permLoading.value = false
  }
}

const resetPermDialog = () => {
  currentRole.value = null
  permissions.value = []
}

const submitPermissionChanges = async () => {
  if (!currentRole.value) return
  saving.value = true

  try {
    const assignedIds = permissions.value
        .filter(p => p.assigned)
        .map(p => p.id)

    const res = await request.put(`/permissions/role/${currentRole.value.id}`, {
      permissionIds: assignedIds
    })

    if (res.status === 'success') {
      ElMessage.success('权限配置已保存')
      permDialogVisible.value = false

      // ========== 核心修复：刷新当前用户权限 ==========
      const currentUserRole = authStore.userInfo?.role
      if (currentUserRole === currentRole.value.roleName) {
        try {
          // 调用 /permissions 接口重新加载当前用户权限
          await authStore.loadPermissions()
          ElMessage.success('当前用户权限已同步更新')

          // 延迟后自动刷新页面，让菜单完全重新渲染
          setTimeout(() => {
            window.location.reload()
          }, 800)
        } catch (permError) {
          console.error('刷新权限失败:', permError)
          ElMessage.warning('权限已保存，但刷新当前用户权限失败，请手动刷新页面')
        }
      } else {
        ElMessage.info(`权限已保存，${getRoleLabel(currentRole.value.roleName)}角色用户刷新页面后生效`)
      }
    } else {
      ElMessage.error(res.info || '保存失败')
    }
  } catch (error) {
    console.error('保存权限失败:', error)
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

// ============ 生命周期 ============
onMounted(() => {
  loadRoles()
  loadUserCounts()
})

const loadUserCounts = async () => {
  for (const role of roles.value) {
    try {
      const res = await request.get('/admin/users/list', {
        params: { role: role.roleName, pageSize: 1 }
      })
      if (res.status === 'success' && res.data) {
        role.userCount = res.data.total || 0
      }
    } catch {
      // 忽略
    }
  }
}
</script>

<style scoped>
.role-management-container {
  padding: 10px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-description {
  color: #909399;
  font-size: 14px;
  margin-top: 5px;
}
.role-cards-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 20px;
}
.role-card {
  padding: 15px;
}
.role-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}
.role-info {
  display: flex;
  align-items: center;
}
.role-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  color: white;
  font-size: 20px;
  margin-right: 10px;
}
.role-name {
  font-weight: bold;
  margin-right: 5px;
}
.role-description {
  font-size: 14px;
  color: #606266;
  margin-bottom: 15px;
  min-height: 40px;
}
.role-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.role-users {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #909399;
}
.role-users .el-icon {
  margin-right: 5px;
}
.role-buttons {
  display: flex;
  gap: 8px;
}
.perm-dialog-body {
  max-height: 500px;
  overflow-y: auto;
}
.perm-group {
  margin-bottom: 24px;
}
.perm-group-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 12px;
}
.perm-items {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.perm-items .el-checkbox {
  min-width: 160px;
  margin-right: 0;
}
</style>