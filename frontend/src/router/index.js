// router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import Login from '../views/Login.vue'
import Layout from '../views/layout/Layout.vue'
import Dashboard from '../views/Dashboard.vue'
import UserManagement from '../views/UserManagement.vue'
import RoleManagement from '../views/RoleManagement.vue'
import CourseManagement from '../views/CourseManagement.vue'

// ============ 模块七：个人信息与课程查看 ============
import Profile from '../views/Profile.vue'
import MyCourses from '../views/MyCourses.vue'
import CourseDetail from '../views/CourseDetail.vue'
import CourseStudents from '../views/CourseStudents.vue'
import TeacherCourseManage from '../views/TeacherCourseManage.vue'
import ChangePassword from '../views/ChangePassword.vue'

// ============ 模块二：人才培养方案管理 ============
import CurriculumManagement from '../views/curriculum/CurriculumManagement.vue'
import ProgramDetail from '../views/curriculum/ProgramDetail.vue'
import CurriculumView from '../views/curriculum/CurriculumView.vue'
import ProgramView from '../views/curriculum/ProgramView.vue'

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

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: Login,
      meta: {
        title: '登录',
        requiresAuth: false
      }
    },
    {
      path: '/',
      redirect: '/dashboard'
    },
    {
      path: '/dashboard',
      component: Layout,
      meta: {
        requiresAuth: true,
        permissions: ['dashboard:view']
      },
      children: [
        {
          path: '',
          name: 'Dashboard',
          component: Dashboard,
          meta: {
            title: '数据看板',
            icon: 'Monitor',
            permissions: ['dashboard:view']
          }
        }
      ]
    },
    {
      path: '/app',
      component: Layout,
      meta: { requiresAuth: true },
      children: [
        {
          path: 'changePassword',
          name: 'ChangePassword',
          component: ChangePassword,
          meta: {
            title: '修改密码',
            icon: 'Lock'
          }
        },
        {
          path: 'profile',
          name: 'Profile',
          component: Profile,
          meta: {
            title: '个人信息',
            icon: 'User',
            permissions: ['profile:view']
          }
        },
        {
          path: 'my-courses',
          name: 'MyCourses',
          component: MyCourses,
          meta: {
            title: '我的课程',
            icon: 'Document',
            permissions: ['course:list']
          }
        },
        {
          path: 'courses/program/:programId',
          name: 'CourseManagement',
          component: CourseManagement,
          meta: {
            title: '课程体系管理',
            icon: 'Document',
            permissions: ['course:manage']
          }
        },
        {
          path: 'course/:id',
          name: 'CourseDetail',
          component: CourseDetail,
          meta: {
            title: '课程详情',
            permissions: ['course:detail']
          }
        },
        {
          path: 'teacher/course/:id',
          name: 'TeacherCourseManage',
          component: TeacherCourseManage,
          meta: {
            title: '课程管理',
            permissions: ['course:teach']
          }
        },
        {
          path: 'course/:id/students',
          name: 'CourseStudents',
          component: CourseStudents,
          meta: {
            title: '学生名单',
            icon: 'UserFilled',
            permissions: ['course:student-list']
          }
        },
        {
          path: 'users',
          name: 'UserManagement',
          component: UserManagement,
          meta: {
            title: '用户管理',
            icon: 'UserFilled',
            permissions: ['user:list']
          }
        },
        {
          path: 'roles',
          name: 'RoleManagement',
          component: RoleManagement,
          meta: {
            title: '角色权限',
            icon: 'Connection',
            permissions: ['role:list']
          }
        },
        {
          path: 'curriculum/detail/:id',
          name: 'ProgramDetail',
          component: ProgramDetail,
          meta: {
            title: '专业详情',
            icon: 'DocumentCopy',
            permissions: ['program:detail']
          }
        },
        {
          path: 'curriculum/management',
          name: 'CurriculumManagement',
          component: CurriculumManagement,
          meta: {
            title: '专业管理',
            icon: 'DocumentCopy',
            permissions: ['program:manage']
          }
        },
        {
          path: 'curriculum/view/:id',
          name: 'ProgramView',
          component: ProgramView,
          meta: {
            title: '专业详情',
            icon: 'DocumentCopy',
            permissions: ['program:view']
          }
        },
        {
          path: 'curriculum/view',
          name: 'CurriculumView',
          component: CurriculumView,
          meta: {
            title: '人才培养方案',
            icon: 'DocumentCopy',
            permissions: ['program:view']
          }
        }
      ]
    },
    {
      path: '/403',
      name: 'Forbidden',
      component: () => import('../views/Forbidden.vue'),
      meta: {
        title: '无权限访问',
        requiresAuth: false
      }
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('../views/NotFound.vue'),
      meta: {
        title: '404',
        requiresAuth: false
      }
    }
  ]
})

// ============ 路由守卫 ============
router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  console.log('路由守卫 - path:', to.path)
  console.log('路由守卫 - token:', authStore.token)
  console.log('路由守卫 - userInfo:', authStore.userInfo)

  // 1. 如果访问的是登录页
  if (to.path === '/login') {
    if (authStore.isLoggedIn) {
      // 如果 token 存在但 userInfo 不存在，尝试恢复
      if (!authStore.userInfo) {
        const savedUserInfo = localStorage.getItem('userInfo')
        if (savedUserInfo) {
          authStore.userInfo = safeJSONParse(savedUserInfo, null)
          console.log('从 localStorage 恢复 userInfo:', authStore.userInfo)
        }
        // 如果仍然没有 userInfo，尝试用 restoreUserInfo 方法
        if (!authStore.userInfo) {
          authStore.restoreUserInfo()
        }
        // 如果还是没有，退出登录
        if (!authStore.userInfo) {
          console.warn('userInfo 不存在，退出登录')
          authStore.logout()
          return true // 返回 true 表示继续导航到登录页
        }
      }
      // 返回首页路径，替代 next(homePath)
      return authStore.getHomePath()
    }
    return true // 继续导航到登录页
  }

  // 2. 如果访问的是公开页面（不需要登录）
  if (to.meta.requiresAuth === false) {
    return true
  }

  // 3. 检查是否需要登录
  if (to.meta.requiresAuth !== false) {
    // 未登录，跳转到登录页
    if (!authStore.isLoggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    // ========== 4. 确保 userInfo 存在 ==========
    if (!authStore.userInfo) {
      const savedUserInfo = localStorage.getItem('userInfo')
      if (savedUserInfo) {
        authStore.userInfo = safeJSONParse(savedUserInfo, null)
        console.log('从 localStorage 恢复 userInfo:', authStore.userInfo)
      }

      if (!authStore.userInfo) {
        console.warn('userInfo 不存在，退出登录')
        authStore.logout()
        return '/login'
      }
    }

    // ========== 5. 检查权限 ==========
    const routePermissions = to.meta.permissions

    // 如果路由没有定义权限要求，直接放行
    if (!routePermissions || routePermissions.length === 0) {
      return true
    }

    // 确保用户权限已加载
    if (authStore.permissions.length === 0) {
      try {
        await authStore.loadPermissions()
      } catch (error) {
        console.error('加载权限失败:', error)
        if (authStore.token && authStore.userInfo) {
          console.warn('权限加载失败，但用户已登录，继续访问')
          return true
        }
        return '/login'
      }
    }

    // 使用 hasPermission 检查权限
    const hasRoutePermission = routePermissions.some(p =>
        authStore.hasPermission(p)
    )

    if (!hasRoutePermission) {
      console.warn('您没有权限访问该页面:', to.path, '需要的权限:', routePermissions)
      return '/403'
    }

    // 6. 所有检查通过，正常访问
    return true
  }

  return true
})

export default router