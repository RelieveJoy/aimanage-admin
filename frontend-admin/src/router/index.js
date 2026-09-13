import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: { name: 'dashboard' },
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '概览' },
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('@/views/Users.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'org',
        name: 'org',
        component: () => import('@/views/Org.vue'),
        meta: { title: '组织架构' },
      },
      {
        path: 'projects',
        name: 'projects',
        component: () => import('@/views/Projects.vue'),
        meta: { title: '项目管理' },
      },
      {
        path: 'projects/:id',
        name: 'project-detail',
        component: () => import('@/views/ProjectDetail.vue'),
        meta: { title: '项目详情' },
      },
      {
        path: 'audit',
        name: 'audit',
        component: () => import('@/views/Audit.vue'),
        meta: { title: '审计账本' },
      },
      {
        path: 'requests',
        name: 'requests',
        component: () => import('@/views/Requests.vue'),
        meta: { title: '审批与通知' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFound.vue'),
    meta: { public: true, title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/**
 * ★ 路由守卫 —— 注意：这只是体验层，安全边界在后端 AuthInterceptor。
 * 这里的判断能挡住普通用户误入管理端界面，但挡不住直接调接口。
 */
router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    return true
  }

  if (!auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  // 刷新页面后 store 里只有 token 没有 user，需要换回来
  if (!auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      auth.clear()
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }

  // 非管理员一律轰出去
  if (!auth.isAdmin) {
    auth.clear()
    return { name: 'login' }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 爱管理管理员端` : '爱管理 · 管理员端'
})

export default router
