import http from './http'

export function uploadDocument(formData) {
  return http.post('/knowledge/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getDocuments(params = {}) {
  return http.get('/knowledge/documents', { params })
}

export function getDocumentChunks(docId) {
  return http.get(`/knowledge/documents/${docId}/chunks`)
}

export function deleteDocument(docId) {
  return http.delete(`/knowledge/documents/${docId}`)
}

export function getKnowledgeConfig() {
  return http.get('/knowledge/config')
}

export function updateKnowledgeConfig(data) {
  return http.put('/knowledge/config', data)
}

export function reprocessAll() {
  return http.post('/knowledge/reprocess')
}

export function searchKnowledge(data) {
  return http.post('/knowledge/search', data)
}