import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layout/Layout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页' }
      },
      {
        path: '/food',
        name: 'Food',
        component: () => import('@/views/Food/FoodList.vue'),
        meta: { title: '食材管理' }
      },
      {
        path: '/food/add',
        name: 'FoodAdd',
        component: () => import('@/views/Food/FoodAdd.vue'),
        meta: { title: '添加食材' }
      },
      {
        path: '/food/edit/:id',
        name: 'FoodEdit',
        component: () => import('@/views/Food/FoodEdit.vue'),
        meta: { title: '编辑食材' }
      },
      {
        path: '/expiring',
        name: 'Expiring',
        component: () => import('@/views/Expiring.vue'),
        meta: { title: '临期预警' }
      },
      {
        path: '/reports',
        name: 'Reports',
        component: () => import('@/views/Reports.vue'),
        meta: { title: '数据报表' }
      },
      {
        path: '/settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: { title: '系统设置' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const isLogin = !!userStore.token

  if (to.meta.requiresAuth && !isLogin) {
    next('/login')
  } else if (to.path === '/login' && isLogin) {
    next('/')
  } else {
    next()
  }
})

export default router