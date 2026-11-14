import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', component: () => import('../views/Home.vue') },
  { path: '/login', component: () => import('../views/Login.vue') },
  { path: '/product/:id', component: () => import('../views/ProductDetail.vue') },
  { path: '/store/:id', component: () => import('../views/Store.vue') },
  { path: '/cart', component: () => import('../views/Cart.vue'), meta: { auth: true } },
  { path: '/orders', component: () => import('../views/Orders.vue'), meta: { auth: true } },
  { path: '/wallet', component: () => import('../views/Wallet.vue'), meta: { auth: true } },
  { path: '/merchant', component: () => import('../views/Merchant.vue'), meta: { auth: true, role: 'MERCHANT' } },
  { path: '/admin', component: () => import('../views/Admin.vue'), meta: { auth: true, role: 'ADMIN' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  if (to.meta.auth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.role && user && user.role !== to.meta.role) {
    return { path: '/' }
  }
  return true
})

export default router
