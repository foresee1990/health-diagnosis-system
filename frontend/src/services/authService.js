import http from './http'

export function register(username, password, email) {
  return http.post('/auth/register', { username, password, email })
}

export function login(username, password) {
  return http.post('/auth/login', { username, password })
}

export function getUserInfo() {
  return http.get('/users/me')
}

export function updatePassword(oldPassword, newPassword) {
  return http.put('/users/me/password', { oldPassword, newPassword })
}

export function updateProfile(email) {
  return http.put('/users/me/profile', { email })
}

export function getHealthProfile() {
  return http.get('/users/me/health-profile')
}

export function updateHealthProfile(data) {
  return http.put('/users/me/health-profile', data)
}
