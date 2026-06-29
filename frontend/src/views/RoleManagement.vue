<template>
  <div class="role-management-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色权限管理</span>
          <el-button type="primary" :icon="Plus">新增角色</el-button>
        </div>
        <p class="header-description">维护系统角色及其对应的功能菜单、数据访问权限范围。</p>
      </template>

      <div class="role-cards-container">
        <el-card v-for="role in roles" :key="role.name" class="role-card">
          <div class="role-card-header">
            <div class="role-info">
              <div class="role-icon" :style="{ backgroundColor: role.iconBg }">
                <el-icon><component :is="role.icon" /></el-icon>
              </div>
              <div>
                <span class="role-name">{{ getRoleLabel(role.name) }}</span>
                <el-tag size="small" type="info">系统预设</el-tag>
              </div>
            </div>
            <el-link type="primary" @click="goToUserManagement(role.name)">管理</el-link>
          </div>
          <p class="role-description">{{ role.description }}</p>
          <div class="role-users">
            <el-icon><User /></el-icon>
            <span>{{ role.userCount }} 人</span>
          </div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, User, Avatar, School, OfficeBuilding } from '@element-plus/icons-vue'
import request from '@/api/request'

const router = useRouter()

const roles = ref([
  {
    name: 'admin',
    icon: Avatar,
    iconBg: '#409EFF',
    description: '系统最高权限，拥有所有菜单和按钮的访问权限，负责系统基础维护。',
    userCount: 0
  },
  {
    name: 'teacher',
    icon: School,
    iconBg: '#67C23A',
    description: '教学相关核心权限，可管理本人授课的课程、成绩录入、学生考勤、查看报表。',
    userCount: 0
  },
  {
    name: 'student',
    icon: User,
    iconBg: '#E6A23C',
    description: '基础学习功能权限，可查看个人课表、报名选课、查看成绩、提交作业。',
    userCount: 0
  }
])

const getRoleLabel = (role) => {
  const map = { admin: '管理员', teacher: '教师', student: '学生' }
  return map[role] || role
}

const goToUserManagement = (roleName) => {
  router.push({ path: '/app/users', query: { role: roleName } })
}

// 加载各角色用户数量
const loadUserCounts = async () => {
  for (const role of roles.value) {
    try {
      const res = await request.get('/admin/users/list', {
        params: { role: role.name, pageSize: 1 }
      })
      if (res.status === 'success' && res.data) {
        role.userCount = res.data.total || 0
      }
    } catch (e) {
      // 忽略
    }
  }
}

onMounted(() => {
  loadUserCounts()
})
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
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
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
  height: 40px;
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
</style>
