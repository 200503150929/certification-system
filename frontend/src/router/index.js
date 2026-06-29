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
// 教师/学生端只读查看
import CurriculumView from '../views/curriculum/CurriculumView.vue'
import ProgramView from '../views/curriculum/ProgramView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // ============ 登录页（无需登录） ============
    {
      path: '/login',
      name: 'Login',
      component: Login,
      meta: {
        title: '登录',
        requiresAuth: false
      }
    },

    // ============ 根路径重定向 ============
    {
      path: '/',
      redirect: '/dashboard'
    },

    // ============ 仪表盘（需要登录） ============
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

    // ============ 主布局（需要登录） ============
    {
      path: '/app',
      component: Layout,
      meta: { requiresAuth: true },
      children: [
        // ----- 修改密码（所有角色） -----
        {
          path: 'changePassword',
          name: 'ChangePassword',
          component: ChangePassword,
          meta: {
            title: '修改密码',
            icon: 'Lock'
          }
        },

        // ----- 个人信息（学生/教师） -----
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

        // ----- 我的课程（学生/教师共用） -----
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

        // ----- 课程管理（管理员按专业管理） -----
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

        // ----- 课程详情（学生端） -----
        {
          path: 'course/:id',
          name: 'CourseDetail',
          component: CourseDetail,
          meta: {
            title: '课程详情',
            permissions: ['course:detail']
          }
        },

        // ----- 课程管理详情（教师端） -----
        {
          path: 'teacher/course/:id',
          name: 'TeacherCourseManage',
          component: TeacherCourseManage,
          meta: {
            title: '课程管理',
            permissions: ['course:teach']
          }
        },

        // ----- 课程学生名单（教师端） -----
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

        // ----- 用户管理（仅管理员） -----
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

        // ----- 角色权限（仅管理员） -----
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

        // ============ 模块二：人才培养方案管理（仅管理员） ============

        // ----- 【管理员】专业详情页（包含 Tab：培养目标、毕业要求、支撑矩阵） -----
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

        // ----- 【管理员】专业管理（列表页） -----
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

        // ============ 模块二-查看：人才培养方案查看（教师/学生只读） ============

        // ----- 【教师/学生】专业详情查看页 -----
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

        // ----- 【教师/学生】人才培养方案查看列表 -----
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

    // ============ 403 无权限页面 ============
    {
      path: '/403',
      name: 'Forbidden',
      component: () => import('../views/Forbidden.vue'),
      meta: {
        title: '无权限访问',
        requiresAuth: false
      }
    },

    // ============ 404 页面 ============
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
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // 1. 如果访问的是登录页
  if (to.path === '/login') {
    if (authStore.isLoggedIn) {
      // 已登录，跳转到首页
      return next(authStore.getHomePath())
    }
    return next()
  }

  // 2. 如果访问的是公开页面（不需要登录）
  if (to.meta.requiresAuth === false) {
    return next()
  }

  // 3. 检查是否需要登录
  if (to.meta.requiresAuth !== false) {
    // 未登录，跳转到登录页
    if (!authStore.isLoggedIn) {
      return next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
    }

    // 4. 检查权限（基于权限标识符）
    const routePermissions = to.meta.permissions
    if (routePermissions && routePermissions.length > 0) {
      // 确保用户权限已加载
      if (!authStore.permissions || authStore.permissions.length === 0) {
        // 如果权限还没加载，等待加载完成
        authStore.loadPermissions().then(() => {
          // 重新检查权限
          const hasRoutePermission = routePermissions.some(p =>
              authStore.permissions.includes(p)
          )

          if (!hasRoutePermission) {
            console.warn('您没有权限访问该页面:', to.path)
            return next('/403')
          }
          return next()
        }).catch(() => {
          // 加载权限失败，跳转到登录页
          return next('/login')
        })
        return
      }

      // 权限已加载，直接检查
      const hasRoutePermission = routePermissions.some(p =>
          authStore.permissions.includes(p)
      )

      if (!hasRoutePermission) {
        console.warn('您没有权限访问该页面:', to.path)
        return next('/403')
      }
    }

    // 5. 所有检查通过，正常访问
    next()
  } else {
    // 默认放行
    next()
  }
})

export default router