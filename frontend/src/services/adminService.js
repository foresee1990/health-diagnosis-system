import http from './http'

export function getAdminUsers(params = {}) {
  return http.get('/admin/users', { params })
}

export function banUser(userId) {
  return http.patch(`/admin/users/${userId}/ban`)
}

export function unbanUser(userId) {
  return http.patch(`/admin/users/${userId}/unban`)
}

export function resetUserPassword(userId) {
  return http.post(`/admin/users/${userId}/password/reset`)
}

export function getUserConsultations(userId) {
  return http.get(`/admin/users/${userId}/consultations`)
}

export function getAdminLogs(params = {}) {
  return http.get('/admin/logs', { params })
}

export function assignRole(userId, role) {
  return http.patch(`/admin/users/${userId}/role`, { role })
}
