<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'">
      <div class="sidebar-header">
        <el-icon><School /></el-icon>
        <span v-if="!isCollapse">工程系统</span>
      </div>

      <el-menu
          active-text-color="#ffd04b"
          background-color="#001529"
          class="el-menu-vertical-demo"
          :default-active="activeMenu"
          text-color="#fff"
          router
          :collapse="isCollapse"
          :collapse-transition="false"
      >
        <!-- 仪表盘 -->
        <el-menu-item v-if="userRole !== 'student'" index="/dashboard">
          <el-icon><Monitor /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>

        <!-- 个人信息（学生/教师共用） -->
        <el-menu-item v-if="userRole !== 'admin'" index="/profile">
          <el-icon><User /></el-icon>
          <span>个人信息</span>
        </el-menu-item>

        <!-- 我的课程（学生端）/ 课程管理（教师端） -->
        <el-menu-item v-if="userRole !== 'admin'" index="/my-courses">
          <el-icon><Document /></el-icon>
          <span>{{ userRole === 'student' ? '我的课程' : '课程管理' }}</span>
        </el-menu-item>

        <!-- 人才培养方案管理（管理员） -->
        <el-menu-item v-if="userRole === 'admin'" index="/curriculum/management">
          <el-icon><DocumentCopy /></el-icon>
          <span>人才培养方案管理</span>
        </el-menu-item>

        <!-- 用户管理（管理员） -->
        <el-menu-item v-if="userRole === 'admin'" index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>

        <!-- 角色权限管理（管理员） -->
        <el-menu-item v-if="userRole === 'admin'" index="/roles">
          <el-icon><Connection /></el-icon>
          <span>角色权限管理</span>
        </el-menu-item>

      </el-menu>

      <!-- 侧边栏底部：用户信息 + 折叠按钮 -->
      <div class="sidebar-footer">
        <div class="user-info-wrapper">
          <el-avatar :size="isCollapse ? 32 : 40" :icon="UserFilled" />
          <div v-if="!isCollapse" class="user-info">
            <p class="username">{{ userInfo.name || '管理员' }}</p>
            <p class="user-role">{{ userInfo.role || 'admin' }}</p>
          </div>
        </div>
        <el-button
            class="collapse-btn"
            text
            @click="toggleCollapse"
        >
          <el-icon v-if="isCollapse"><Expand /></el-icon>
          <el-icon v-else><Fold /></el-icon>
        </el-button>
      </div>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <!-- 顶部导航 -->
      <el-header>
        <div class="header-left">
          <el-button text @click="toggleCollapse" style="padding: 0 8px">
            <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </el-button>
          <!-- 面包屑 -->
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
          <!-- 通知 -->
          <el-badge :value="notificationCount" :hidden="notificationCount === 0" class="notification-badge">
            <el-icon class="header-icon"><Bell /></el-icon>
          </el-badge>

          <!-- 用户下拉菜单 -->
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

      <!-- 内容区域 -->
      <el-main>
        <router-view v-slot="{ Component }">
          <keep-alive :include="['CurriculumManagement', 'ProgramDetail', 'CurriculumGoals', 'CurriculumRequirements', 'CurriculumIndicators', 'CurriculumMatrix']">
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
  School,
  Monitor,
  Connection,
  User,
  Bell,
  ArrowDown,
  UserFilled,
  Document,
  DocumentCopy,
  Expand,
  Fold,
  Lock,
  SwitchButton
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// ============ 状态 ============
const isCollapse = ref(false)
const notificationCount = ref(3)

// ============ 用户信息 ============
const userInfo = ref({
  name: '',
  role: ''
})

const userRole = computed(() => userInfo.value.role)

// ============ 面包屑配置 ============
const breadcrumbConfig = {
  // 人才培养方案模块
  '/curriculum/management': {
    items: [
      { name: '人才培养方案管理' }
    ]
  },
  '/curriculum/detail': {
    items: [
      { name: '人才培养方案管理', path: '/curriculum/management' },
      { name: '专业详情' }
    ]
  },

  // 系统管理模块
  '/users': {
    items: [
      { name: '系统管理' },
      { name: '用户管理' }
    ]
  },
  '/roles': {
    items: [
      { name: '系统管理' },
      { name: '角色权限管理' }
    ]
  },

  // 个人信息
  '/profile': {
    items: [
      { name: '个人中心' },
      { name: '个人信息' }
    ]
  },

  // 仪表盘
  '/dashboard': {
    items: [
      { name: '仪表盘' }
    ]
  },

  // 我的课程
  '/my-courses': {
    items: [
      { name: '我的课程' }
    ]
  },

  // 修改密码
  '/changePassword': {
    items: [
      { name: '个人中心' },
      { name: '修改密码' }
    ]
  }
}

// 动态匹配面包屑配置（支持带参数的路由）
const getBreadcrumbConfig = (path) => {
  // 精确匹配
  if (breadcrumbConfig[path]) {
    return breadcrumbConfig[path]
  }

  // 动态路由匹配（如 /curriculum/detail/1）
  if (path.startsWith('/curriculum/detail/')) {
    return {
      items: [
        { name: '人才培养方案管理', path: '/curriculum/management' },
        { name: '专业详情' }
      ]
    }
  }

  // 默认：只显示当前路由名称
  return {
    items: [{ name: route.meta?.title || route.name || '未知页面' }]
  }
}

// ============ 计算属性 ============
// 计算面包屑数据
const breadcrumbItems = computed(() => {
  const config = getBreadcrumbConfig(route.path)
  return config.items || [{ name: route.meta?.title || route.name || '未知页面' }]
})

// 当前激活菜单
const activeMenu = computed(() => {
  // 专业详情页高亮"人才培养方案管理"
  if (route.path.startsWith('/curriculum/detail/')) return '/curriculum/management'
  // 人才培养方案所有子页面都高亮"人才培养方案管理"
  if (route.path.startsWith('/curriculum/')) return '/curriculum/management'
  // 修改密码高亮"个人信息"
  if (route.path === '/changePassword') return '/profile'
  return route.path
})

// ============ 方法 ============
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

// 下拉菜单命令处理
const handleCommand = (command) => {
  switch (command) {
    case 'changePassword':
      router.push('/changePassword')
      break
    case 'logout':
      handleLogout()
      break
    default:
      break
  }
}

// 退出登录方法
const handleLogout = () => {
  ElMessageBox.confirm(
      '确定要退出登录吗？',
      '退出确认',
      {
        confirmButtonText: '确定退出',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(() => {
    authStore.logout()
    ElMessage.success('已安全退出')
  }).catch(() => {})
}

// ============ 加载用户信息 ============
const loadUserInfo = () => {
  const role = localStorage.getItem('userRole') || ''
  const name = localStorage.getItem('displayName') || ''

  userInfo.value.role = role
  userInfo.value.name = name
}

// ============ 生命周期 ============
onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
/* ========== 整体布局 ========== */
.layout-container {
  height: 100vh;
}

/* ========== 侧边栏 ========== */
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

/* 菜单样式 */
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

/* 侧边栏底部 */
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

/* ========== 顶部导航 ========== */
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

/* ========== 主内容 ========== */
.el-main {
  background-color: #f0f2f5;
  padding: 10px;
  overflow-y: auto;
  flex: 1;
}

/* ========== 下拉菜单图标样式 ========== */
.el-dropdown-menu .el-dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.el-dropdown-menu .el-dropdown-item .el-icon {
  font-size: 16px;
}

/* ========== 滚动条美化 ========== */
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

/* ========== 响应式 ========== */
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