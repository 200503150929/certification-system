import { createRouter, createWebHistory } from 'vue-router'
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

// ============ 模块二：人才培养方案管理 ============
import CurriculumManagement from '../views/curriculum/CurriculumManagement.vue'
import CurriculumGoals from '../views/curriculum/CurriculumGoals.vue'
import CurriculumRequirements from '../views/curriculum/CurriculumRequirements.vue'
import CurriculumIndicators from '../views/curriculum/CurriculumIndicators.vue'
import CurriculumMatrix from '../views/curriculum/CurriculumMatrix.vue'

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

    // ============ 主布局（需要登录） ============
    {
      path: '/',
      component: Layout,
      redirect: () => {
        const token = localStorage.getItem('token');
        // 如果没登录，直接去登录页，别去尝试触发子路由的守卫！
        if (!token) return '/login';
        // 如果登录了，再根据角色跳转
        return getUserRole() === 'student' ? '/my-courses' : '/dashboard';
      },
      meta: { requiresAuth: true },
      children: [
        // ----- 仪表盘（管理员/教师） -----
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: Dashboard,
          meta: {
            title: '数据看板',
            icon: 'Monitor',
            roles: ['admin', 'teacher']
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
            roles: ['teacher', 'student']
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
            roles: ['teacher', 'student']
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
            roles: ['admin']
          }
        },

        // ----- 课程详情（学生端） -----
        {
          path: 'course/:id',
          name: 'CourseDetail',
          component: CourseDetail,
          meta: {
            title: '课程详情',
            roles: ['student']
          }
        },

        // ----- 课程管理详情（教师端） -----
        {
          path: 'teacher/course/:id',
          name: 'TeacherCourseManage',
          component: TeacherCourseManage,
          meta: {
            title: '课程管理',
            roles: ['teacher']
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
            roles: ['teacher']
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
            roles: ['admin']
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
            roles: ['admin']
          }
        },

        // ============ 模块二：人才培养方案管理（仅管理员） ============
        // ----- 专业管理 -----
        {
          path: '/curriculum/management',
          name: 'CurriculumManagement',
          component: CurriculumManagement,
          meta: {
            title: '专业管理',
            icon: 'DocumentCopy',
            roles: ['admin']
          }
        },

        // ----- 培养目标管理 -----
        {
          path: '/curriculum/goals',
          name: 'CurriculumGoals',
          component: CurriculumGoals,
          meta: {
            title: '培养目标管理',
            icon: 'DocumentAdd',
            roles: ['admin']
          }
        },

        // ----- 毕业要求管理 -----
        {
          path: '/curriculum/requirements',
          name: 'CurriculumRequirements',
          component: CurriculumRequirements,
          meta: {
            title: '毕业要求管理',
            icon: 'DocumentChecked',
            roles: ['admin']
          }
        },

        // ----- 指标点管理 -----
        {
          path: '/curriculum/indicators',
          name: 'CurriculumIndicators',
          component: CurriculumIndicators,
          meta: {
            title: '指标点管理',
            icon: 'List',
            roles: ['admin']
          }
        },

        // ----- 支撑矩阵 -----
        {
          path: '/curriculum/matrix',
          name: 'CurriculumMatrix',
          component: CurriculumMatrix,
          meta: {
            title: '支撑矩阵',
            icon: 'Grid',
            roles: ['admin']
          }
        },

        // ----- 系统设置（仅管理员，暂时注释） -----
        // {
        //   path: 'settings/system',
        //   name: 'SystemSettings',
        //   component: () => import('../views/SystemSettings.vue'),
        //   meta: {
        //     title: '系统参数',
        //     icon: 'Setting',
        //     roles: ['管理员']
        //   }
        // },
        // {
        //   path: 'settings/logging',
        //   name: 'Logging',
        //   component: () => import('../views/Logging.vue'),
        //   meta: {
        //     title: '日志审计',
        //     icon: 'Document',
        //     roles: ['管理员']
        //   }
        // }
      ]
    },

    // ============ 404 页面（暂时注释） ============
    // {
    //   path: '/:pathMatch(.*)*',
    //   name: 'NotFound',
    //   component: () => import('../views/NotFound.vue'),
    //   meta: { title: '404' }
    // }
  ]
})

// ============ 路由守卫 ============
// 获取用户角色（从 localStorage 获取，后端返回英文角色名）
const getUserRole = () => {
  return localStorage.getItem('userRole') || 'student'
}

const getHomePath = (userRole) => {
  // userRole 是英文: admin, teacher, student
  if (userRole === 'student') return '/my-courses'
  return '/dashboard'
}

// 检查是否有权限访问
const hasPermission = (routeRoles, userRole) => {
  if (!routeRoles || routeRoles.length === 0) return true
  return routeRoles.includes(userRole)
}

router.beforeEach((to, from) => { // 【修改点1】：删除 next 参数
                                  // 获取 token
  const token = localStorage.getItem('token')
  const userRole = getUserRole()

  // 1. 如果访问的是登录页
  if (to.path === '/login') {
    if (token) {
      // 已登录，跳转到首页
      return getHomePath(userRole) // 【修改点2】：return 路径字符串
    } else {
      return true // 【修改点3】：return true 表示放行
    }
  }

  // 2. 检查是否需要登录
  if (to.meta.requiresAuth !== false) {
    if (!token) {
      // 未登录，跳转到登录页
      return { // 【修改点4】：return 一个包含 path 和 query 的对象
        path: '/login',
        query: { redirect: to.fullPath }
      }
    }

    // 3. 检查角色权限
    const routeRoles = to.meta.roles
    if (routeRoles && !hasPermission(routeRoles, userRole)) {
      // 无权限，跳转到首页
      console.warn('您没有权限访问该页面')
      return getHomePath(userRole) // 【修改点5】：return 路径字符串
    }
  }

  // 最后，如果所有检查都通过，直接放行
  return true // 【修改点6】：结尾统一 return true
})

export default router
