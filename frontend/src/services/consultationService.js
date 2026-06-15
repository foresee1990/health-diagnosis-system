import http from './http'
import { useAuthStore } from '@/stores/auth'

export function createConsultation(chiefComplaint) {
  return http.post('/consultations', { chiefComplaint })
}

export function getConsultationList(page = 1, size = 20) {
  return http.get('/consultations', { params: { page, size } })
}

export function getMessages(consultationId) {
  return http.get(`/consultations/${consultationId}/messages`)
}

export function completeConsultation(consultationId) {
  return http.patch(`/consultations/${consultationId}/status`)
}

export function deleteConsultation(consultationId) {
  return http.delete(`/consultations/${consultationId}`)
}

export function renameConsultation(consultationId, title) {
  return http.patch(`/consultations/${consultationId}/title`, { title })
}

export async function sendMessageStream(consultationId, content, onThinkingToken, onAnswerToken, onThinkDone, onDone) {
  const token = useAuthStore().token

  const response = await fetch(`/api/consultations/${consultationId}/messages/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ content })
  })

  if (!response.ok) {
    throw new Error(`Stream request failed: ${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop()

    for (const line of lines) {
      if (!line.startsWith('data:')) continue
      const data = line.slice(5).trim()
      if (!data) continue

      try {
        const parsed = JSON.parse(data)
        if (parsed.type === 'thinking') onThinkingToken(parsed.token)
        else if (parsed.type === 'answer') onAnswerToken(parsed.token)
        else if (parsed.type === 'thinkDone') onThinkDone()
        else if (parsed.type === 'done') onDone(parsed.riskLevel || null)
      } catch {
        // 兜底：按普通 answer token 处理
        onAnswerToken(data)
      }
    }
  }
}
