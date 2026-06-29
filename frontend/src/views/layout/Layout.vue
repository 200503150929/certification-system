<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'">
      <div class="sidebar-header">
        <el-icon><School /></el-icon>
        <span v-if="!isCollapse">工程系统</span>
      </div>

      <!-- 关键：添加 :key="authStore.menuVersion" -->
      <el-menu
          :key="authStore.menuVersion"
          active-text-color="#ffd04b"
          background-color="#001529"
          class="el-menu-vertical-demo"
          :default-active="activeMenu"
          text-color="#fff"
          router
          :collapse="isCollapse"
          :collapse-transition="false"
      >
        <el-menu-item v-permission="'dashboard:view'" index="/dashboard">
          <el-icon><Monitor /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>

        <el-menu-item v-permission="'profile:view'" index="/app/profile">
          <el-icon><User /></el-icon>
          <span>个人信息</span>
        </el-menu-item>

        <el-menu-item
            v-if="userRole !== 'admin' && userRole !== 'ADMIN'"
            v-permission="'course:list'"
            index="/app/my-courses"
        >
          <el-icon><Document /></el-icon>
          <span>{{ userRole === 'student' ? '我的课程' : '课程管理' }}</span>
        </el-menu-item>

        <el-menu-item v-permission="'program:manage'" index="/app/curriculum/management">
          <el-icon><DocumentCopy /></el-icon>
          <span>人才培养方案管理</span>
        </el-menu-item>

        <el-menu-item
            v-if="userRole !== 'admin' && userRole !== 'ADMIN'"
            v-permission="'program:view'"
            index="/app/curriculum/view"
        >
          <el-icon><DocumentCopy /></el-icon>
          <span>人才培养方案</span>
        </el-menu-item>

        <el-menu-item v-permission="'user:list'" index="/app/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>

        <el-menu-item v-permission="'role:list'" index="/app/roles">
          <el-icon><Connection /></el-icon>
          <span>角色权限管理</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <div class="user-info-wrapper">
          <el-avatar :size="isCollapse ? 32 : 40" :icon="UserFilled" />
          <div v-if="!isCollapse" class="user-info">
            <p class="username">{{ userName }}</p>
            <p class="user-role">{{ userRole }}</p>
          </div>
        </div>
        <el-button class="collapse-btn" text @click="toggleCollapse">
          <el-icon v-if="isCollapse"><Expand /></el-icon>
          <el-icon v-else><Fold /></el-icon>
        </el-button>
      </div>
    </el-aside>

    <el-container>
      <el-header>
        <div class="header-left">
          <el-button text @click="toggleCollapse" style="padding: 0 8px">
            <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item
                v-for="(item, index) in breadcrumbItems"
                :key="index"
                :to="item.path ? { path: item.path } : undefined"
            >
              {{ item.name }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-badge :value="notificationCount" :hidden="notificationCount === 0" class="notification-badge">
            <el-icon class="header-icon"><Bell /></el-icon>
          </el-badge>

          <el-dropdown @command="handleCommand">
            <span class="el-dropdown-link">
              {{ userInfo.name || '用户' }}
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="changePassword">
                  <el-icon><Lock /></el-icon>
                  修改密码
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon style="color: #f56c6c"><SwitchButton /></el-icon>
                  <span style="color: #f56c6c">退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main>
        <router-view v-slot="{ Component }">
          <keep-alive :include="['CurriculumManagement', 'ProgramDetail', 'CurriculumView', 'ProgramView']">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  School, Monitor, Connection, User, Bell, ArrowDown,
  UserFilled, Document, DocumentCopy, Expand, Fold,
  Lock, SwitchButton
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isCollapse = ref(false)
const notificationCount = ref(3)

const userInfo = ref({ name: '', role: '' })

const userRole = computed(() => authStore.userInfo?.role || userInfo.value.role)
const userName = computed(() => authStore.userInfo?.name || userInfo.value.name)

const breadcrumbConfig = {
  '/dashboard': { items: [{ name: '仪表盘' }] },
  '/app/profile': { items: [{ name: '个人信息' }] },
  '/app/my-courses': { items: [{ name: '我的课程' }] },
  '/app/courses/program': { items: [{ name: '课程管理' }] },
  '/app/course': { items: [{ name: '我的课程', path: '/app/my-courses' }, { name: '课程详情' }] },
  '/app/teacher/course': { items: [{ name: '课程管理', path: '/app/my-courses' }, { name: '课程管理' }] },
  '/app/course/:id/students': { items: [{ name: '课程管理', path: '/app/my-courses' }, { name: '学生名单' }] },
  '/app/curriculum/management': { items: [{ name: '人才培养方案管理' }] },
  '/app/curriculum/detail': { items: [{ name: '人才培养方案管理', path: '/app/curriculum/management' }, { name: '专业详情' }] },
  '/app/curriculum/view': { items: [{ name: '人才培养方案' }] },
  '/app/curriculum/view/:id': { items: [{ name: '人才培养方案', path: '/app/curriculum/view' }, { name: '专业详情' }] },
  '/app/users': { items: [{ name: '用户管理' }] },
  '/app/roles': { items: [{ name: '角色权限管理' }] },
  '/app/changePassword': { items: [{ name: '个人信息', path: '/app/profile' }, { name: '修改密码' }] }
}

const getBreadcrumbConfig = (path) => {
  if (breadcrumbConfig[path]) return breadcrumbConfig[path]
  if (path.startsWith('/app/courses/program/')) return { items: [{ name: '课程管理' }] }
  if (path.startsWith('/app/course/') && !path.includes('/students')) {
    return { items: [{ name: '我的课程', path: '/app/my-courses' }, { name: '课程详情' }] }
  }
  if (path.startsWith('/app/teacher/course/')) {
    return { items: [{ name: '课程管理', path: '/app/my-courses' }, { name: '课程管理' }] }
  }
  if (path.includes('/students')) {
    return { items: [{ name: '课程管理', path: '/app/my-courses' }, { name: '学生名单' }] }
  }
  if (path.startsWith('/app/curriculum/detail/')) {
    return { items: [{ name: '人才培养方案管理', path: '/app/curriculum/management' }, { name: '专业详情' }] }
  }
  if (path.startsWith('/app/curriculum/view/')) {
    return { items: [{ name: '人才培养方案', path: '/app/curriculum/view' }, { name: '专业详情' }] }
  }
  return { items: [{ name: route.meta?.title || route.name || '未知页面' }] }
}

const breadcrumbItems = computed(() => {
  const config = getBreadcrumbConfig(route.path)
  return config.items || [{ name: route.meta?.title || route.name || '未知页面' }]
})

const activeMenu = computed(() => {
  if (route.path === '/dashboard') return '/dashboard'
  if (route.path === '/app/profile') return '/app/profile'
  if (route.path === '/app/my-courses') return '/app/my-courses'
  if (route.path.startsWith('/app/courses/')) return '/app/my-courses'
  if (route.path.startsWith('/app/course/')) return '/app/my-courses'
  if (route.path.startsWith('/app/teacher/')) return '/app/my-courses'
  if (route.path.startsWith('/app/curriculum/detail/')) return '/app/curriculum/management'
  if (route.path.startsWith('/app/curriculum/management')) return '/app/curriculum/management'
  if (route.path.startsWith('/app/curriculum/view/')) return '/app/curriculum/view'
  if (route.path === '/app/users') return '/app/users'
  if (route.path === '/app/roles') return '/app/roles'
  if (route.path === '/app/changePassword') return '/app/profile'
  return route.path
})

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const handleCommand = (command) => {
  switch (command) {
    case 'changePassword':
      router.push('/app/changePassword')
      break
    case 'logout':
      handleLogout()
      break
    default:
      break
  }
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
    confirmButtonText: '确定退出',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    authStore.logout()
    ElMessage.success('已安全退出')
    router.push('/login')
  }).catch(() => {})
}

const loadUserInfo = () => {
  userInfo.value.name = authStore.userInfo?.name || ''
  userInfo.value.role = authStore.userInfo?.role || ''
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.el-aside {
  background-color: #001529;
  color: white;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  overflow: hidden;
  flex-shrink: 0;
}
.sidebar-header {
  display: flex;
  align-items: center;
  padding: 18px 20px;
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  min-height: 60px;
  white-space: nowrap;
}
.sidebar-header .el-icon {
  font-size: 28px;
  margin-right: 10px;
  color: #409EFF;
}
.sidebar-header span {
  transition: opacity 0.3s;
}
.el-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}
.el-menu:not(.el-menu--collapse) {
  width: 100%;
}
.el-menu .el-menu-item.is-active {
  background-color: rgba(64, 158, 255, 0.2) !important;
}
.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 64px;
  flex-shrink: 0;
}
.user-info-wrapper {
  display: flex;
  align-items: center;
  overflow: hidden;
}
.user-info {
  margin-left: 10px;
  overflow: hidden;
}
.user-info .username {
  font-size: 14px;
  color: #fff;
  margin: 0;
  white-space: nowrap;
}
.user-info .user-role {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  white-space: nowrap;
}
.collapse-btn {
  color: rgba(255, 255, 255, 0.6);
  padding: 4px;
  font-size: 16px;
  flex-shrink: 0;
}
.collapse-btn:hover {
  color: #fff;
}
.el-header {
  background-color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  border-bottom: 1px solid #e8ecf1;
  height: 60px;
  flex-shrink: 0;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-left .el-button {
  font-size: 18px;
  color: #5a5e66;
}
.header-left .el-button:hover {
  color: #409EFF;
}
.el-breadcrumb {
  font-size: 14px;
}
.el-breadcrumb :deep(.el-breadcrumb__inner) {
  color: #5a5e66;
}
.el-breadcrumb :deep(.el-breadcrumb__inner:last-child) {
  color: #333;
  font-weight: 600;
  cursor: default;
}
.el-breadcrumb :deep(.el-breadcrumb__inner:not(:last-child)) {
  color: #409EFF;
}
.el-breadcrumb :deep(.el-breadcrumb__inner:not(:last-child):hover) {
  color: #66b1ff;
  text-decoration: underline;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}
.header-icon {
  font-size: 20px;
  color: #5a5e66;
  cursor: pointer;
}
.header-icon:hover {
  color: #409EFF;
}
.notification-badge :deep(.el-badge__content) {
  margin-top: 4px;
}
.el-dropdown-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #5a5e66;
  cursor: pointer;
  font-size: 14px;
}
.el-dropdown-link:hover {
  color: #409EFF;
}
.el-main {
  background-color: #f0f2f5;
  padding: 10px;
  overflow-y: auto;
  flex: 1;
}
.el-dropdown-menu .el-dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.el-dropdown-menu .el-dropdown-item .el-icon {
  font-size: 16px;
}
.el-main::-webkit-scrollbar,
.el-menu::-webkit-scrollbar {
  width: 4px;
}
.el-main::-webkit-scrollbar-thumb,
.el-menu::-webkit-scrollbar-thumb {
  background: #c1c7cd;
  border-radius: 4px;
}
.el-main::-webkit-scrollbar-thumb:hover,
.el-menu::-webkit-scrollbar-thumb:hover {
  background: #a8b0b8;
}
@media (max-width: 768px) {
  .el-aside {
    width: 64px !important;
  }
  .el-aside .sidebar-header span,
  .el-aside .user-info {
    display: none;
  }
}
</style>