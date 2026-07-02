import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/inventory',
    name: 'Inventory',
    component: () => import('../views/Inventory.vue')
  },
  {
    path: '/add-food',
    name: 'AddFood',
    component: () => import('../views/AddFood.vue')
  },
  {
    path: '/edit-food/:id',
    name: 'EditFood',
    component: () => import('../views/EditFood.vue')
  },
  {
    path: '/outbound',
    name: 'Outbound',
    component: () => import('../views/Outbound.vue')
  },
  {
    path: '/expiring',
    name: 'Expiring',
    component: () => import('../views/Expiring.vue')
  },
  {
    path: '/recipes',
    name: 'Recipes',
    component: () => import('../views/Recipes.vue')
  },
  {
    path: '/preferences',
    name: 'Preferences',
    component: () => import('../views/Preferences.vue')
  },
  {
    path: '/shopping-plan',
    name: 'ShoppingPlan',
    component: () => import('../views/ShoppingPlan.vue')
  },
  {
    path: '/reports',
    name: 'Reports',
    component: () => import('../views/Reports.vue')
  },
  {
    path: '/logs',
    name: 'Logs',
    component: () => import('../views/Logs.vue')
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/Settings.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router