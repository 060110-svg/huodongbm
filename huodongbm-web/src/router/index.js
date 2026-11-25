import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    redirect: (to) => {
      const token = localStorage.getItem('token') || ''
      const user = JSON.parse(localStorage.getItem('user') || 'null')
      if (!token) return '/login'
      return user?.role === 'admin' ? '/dashboard' : '/home'
    },
  },
  { path: '/home', name: 'Home', component: () => import('../views/Home.vue') },
  { path: '/dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { admin: true } },
  { path: '/activities', name: 'ActivityList', component: () => import('../views/ActivityList.vue') },
  { path: '/admin/activities', name: 'AdminActivityList', component: () => import('../views/ActivityList.vue'), meta: { admin: true } },
  { path: '/activities/:id', name: 'ActivityDetail', component: () => import('../views/ActivityDetail.vue') },
  { path: '/my-registrations', name: 'MyRegistrations', component: () => import('../views/MyRegistrations.vue') },
  { path: '/admin/activities/new', name: 'ActivityCreate', component: () => import('../views/ActivityEdit.vue'), meta: { admin: true } },
  { path: '/admin/activities/:id/edit', name: 'ActivityEdit', component: () => import('../views/ActivityEdit.vue'), meta: { admin: true } },
  { path: '/admin/registrations', name: 'RegistrationList', component: () => import('../views/RegistrationList.vue'), meta: { admin: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() { return { top: 0 } },
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.public) {
    if (userStore.isLogin) {
      return userStore.isAdmin ? '/dashboard' : '/home'
    }
    return true
  }
  if (!userStore.isLogin) {
    return '/login'
  }
  if (to.meta.admin && !userStore.isAdmin) {
    return '/home'
  }
  return true
})

export default router