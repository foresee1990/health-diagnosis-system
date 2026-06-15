import { defineStore } from 'pinia'
import { login as loginApi } from '@/services/authService'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    userId: Number(localStorage.getItem('userId')) || null,
    username: localStorage.getItem('username') || null,
    role: localStorage.getItem('role') || null,
  }),

  getters: {
    isAdmin: (state) => state.role === 'ADMIN',
    isKnowledgeEngineer: (state) => state.role === 'KNOWLEDGE_ENGINEER',
    canAccessKnowledge: (state) => state.role === 'KNOWLEDGE_ENGINEER' || state.role === 'ADMIN',
  },

  actions: {
    async login(username, password) {
      const res = await loginApi(username, password)
      this.token = res.data.token
      this.userId = res.data.userId
      this.username = res.data.username
      this.role = res.data.role || 'USER'
      localStorage.setItem('token', this.token)
      localStorage.setItem('userId', this.userId)
      localStorage.setItem('username', this.username)
      localStorage.setItem('role', this.role)
    },

    logout() {
      this.token = null
      this.userId = null
      this.username = null
      this.role = null
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      localStorage.removeItem('role')
    }
  }
})
