import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const http = axios.create({
  baseURL: '/api',
  timeout: 60000
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

http.interceptors.response.use(
  (res) => {
    const result = res.data
    // 后端用 HTTP 200 承载业务错误时，将非 200 code 转为 rejected Promise
    if (result?.code !== undefined && result.code !== 200) {
      const err = new Error(result.message || '请求失败')
      err.response = { data: result }   // 保持与 axios error 相同的结构
      return Promise.reject(err)
    }
    return result
  },
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore().logout()
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default http
