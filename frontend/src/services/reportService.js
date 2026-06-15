import http from './http'
import { useAuthStore } from '@/stores/auth'

export function generateReport(consultationId) {
  return http.post(`/consultations/${consultationId}/report`)
}

export function getReport(consultationId) {
  return http.get(`/consultations/${consultationId}/report`)
}

export function getReportDownloadUrl(reportId) {
  return `/api/reports/${reportId}/file`
}

export async function downloadReport(reportId) {
  const token = useAuthStore().token
  const response = await fetch(`/api/reports/${reportId}/file`, {
    headers: { 'Authorization': `Bearer ${token}` }
  })
  if (!response.ok) throw new Error('Download failed')
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `health-report-${reportId}.pdf`
  a.click()
  URL.revokeObjectURL(url)
}
