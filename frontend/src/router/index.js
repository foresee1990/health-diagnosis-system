import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const routes = [
  { path: '/', redirect: '/chat' },
  { path: '/login', component: () => import('@/views/LoginView.vue') },
  { path: '/register', component: () => import('@/views/RegisterView.vue') },
  {
    path: '/chat',
    component: () => import('@/views/ChatView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/chat/:consultationId',
    component: () => import('@/views/ChatView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    component: () => import('@/views/AdminView.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/knowledge',
    component: () => import('@/views/KnowledgeView.vue'),
    meta: { requiresAuth: true, requiresKnowledge: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.token) {
    return '/login'
  }
  if (to.meta.requiresAdmin && auth.role !== 'ADMIN') {
    ElMessage.warning('无权访问')
    return '/chat'
  }
  if (to.meta.requiresKnowledge && auth.role !== 'KNOWLEDGE_ENGINEER' && auth.role !== 'ADMIN') {
    ElMessage.warning('无权访问')
    return '/chat'
  }
})

export default router
