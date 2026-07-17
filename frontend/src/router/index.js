import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '../store/auth'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  { path: '/admin', name: 'admin', component: () => import('../views/Admin.vue'), meta: { role: 'ADMIN' } },
  { path: '/applications', name: 'applications', component: () => import('../views/Application.vue'), meta: { role: 'ADMIN' } },
  { path: '/student', name: 'student', component: () => import('../views/Student.vue'), meta: { role: 'STUDENT' } },
  { path: '/generator', name: 'generator', component: () => import('../views/Generator.vue') },
  { path: '/', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuth()
  if (to.path !== '/login' && !auth.token) {
    return '/login'
  }
  if (to.meta.role && auth.role !== to.meta.role) {
    return auth.role === 'ADMIN' ? '/admin' : '/student'
  }
})

export default router
