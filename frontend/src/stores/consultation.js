import { defineStore } from 'pinia'
import * as consultationService from '@/services/consultationService'

export const useConsultationStore = defineStore('consultation', {
  state: () => ({
    list: [],
    listTotal: 0,
    currentId: null,
    currentStatus: null,
    currentRiskLevel: null,
    messages: [],
    aiTyping: false,
  }),

  actions: {
    async fetchList(page = 1, size = 20) {
      const res = await consultationService.getConsultationList(page, size)
      this.list = res.data.consultations
      this.listTotal = res.data.total
    },

    async openConsultation(id) {
      this.currentId = id
      this.messages = []
      const res = await consultationService.getMessages(id)
      this.messages = res.data.messages
      this.currentStatus = res.data.status
      this.currentRiskLevel = res.data.riskLevel
    },

    async createConsultation(chiefComplaint) {
      const res = await consultationService.createConsultation(chiefComplaint)
      await this.fetchList()
      return res.data.consultationId
    },

    async sendMessage(content) {
      if (!this.currentId) return

      this.messages.push({ role: 'user', content, createdAt: new Date().toISOString() })

      this.messages.push({
        role: 'assistant',
        thinking: '',
        content: '',
        createdAt: new Date().toISOString()
      })
      this.aiTyping = true

      const idx = this.messages.length - 1

      try {
        await consultationService.sendMessageStream(
          this.currentId,
          content,
          (token) => { this.messages[idx].thinking += token },
          (token) => { this.messages[idx].content += token },
          () => {},
          (riskLevel) => {
            this.currentRiskLevel = riskLevel
            this.aiTyping = false
          }
        )
      } catch (e) {
        this.aiTyping = false
        throw e
      }
    },

    async completeConsultation() {
      if (!this.currentId) return
      await consultationService.completeConsultation(this.currentId)
      this.currentStatus = 'completed'
      const item = this.list.find(c => c.id === this.currentId)
      if (item) item.status = 'completed'
    },

    async deleteConsultation(id) {
      await consultationService.deleteConsultation(id)
      this.list = this.list.filter(c => c.id !== id)
      if (this.currentId === id) {
        this.currentId = null
        this.messages = []
        this.currentStatus = null
        this.currentRiskLevel = null
      }
    },

    async renameConsultation(id, title) {
      await consultationService.renameConsultation(id, title)
      const item = this.list.find(c => c.id === id)
      if (item) item.chiefComplaint = title
    }
  }
})
