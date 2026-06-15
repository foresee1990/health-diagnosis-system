# API Service 设计

## 一、axios 封装（src/services/http.js）

统一封装 axios 实例，处理：
- 请求拦截：自动添加 `Authorization: Bearer {token}` Header
- 响应拦截：统一处理错误（401 跳登录、其他弹错误提示）

```js
// src/services/http.js  — 设计示意，非完整代码
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const http = axios.create({
  baseURL: '/api',         // 配合 vite.config.js 代理
  timeout: 60000           // 超时 60s（AI 接口响应慢）
})

// 请求拦截：注入 token
http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

// 响应拦截：统一处理 401
http.interceptors.response.use(
  (res) => res.data,       // 直接返回 res.data（即后端 Result 对象）
  (error) => {
    if (error.response?.status === 401) {
      // token 失效，跳登录
      useAuthStore().logout()
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default http
```

> **注意**：响应拦截返回 `res.data`，所以在 service 层拿到的直接是 `{ code, message, data, timestamp }` 对象，使用时取 `.data` 字段。

---

## 二、authService.js

```js
// src/services/authService.js — 方法签名示意
import http from './http'

// 用户注册
// 返回: Promise<{ code, message, data: null }>
export function register(username, password, email) {
  return http.post('/auth/register', { username, password, email })
}

// 用户登录
// 返回: Promise<{ code, message, data: { token, userId, username, role } }>
export function login(username, password) {
  return http.post('/auth/login', { username, password })
}

// 获取当前用户信息
// 返回: Promise<{ code, message, data: { userId, username, email, role, createdAt } }>
export function getUserInfo() {
  return http.get('/users/me')
}

// 修改密码
// 返回: Promise<{ code, message, data: null }>
export function updatePassword(oldPassword, newPassword) {
  return http.put('/users/me/password', { oldPassword, newPassword })
}

// 更新个人信息（邮箱）
// 返回: Promise<{ code, message, data: { userId, username, email, createdAt } }>
export function updateProfile(email) {
  return http.put('/users/me/profile', { email })
}
```

---

## 六、adminService.js

```js
// src/services/adminService.js — 方法签名示意
import http from './http'

// 获取用户列表（脱敏）
// 返回: Promise<{ code, message, data: { total, page, size, pages, users[] } }>
export function getAdminUsers(params = {}) {
  return http.get('/admin/users', { params })
}

// 禁用用户
// 返回: Promise<{ code, message, data: null }>
export function banUser(userId) {
  return http.patch(`/admin/users/${userId}/ban`)
}

// 启用用户
// 返回: Promise<{ code, message, data: null }>
export function unbanUser(userId) {
  return http.patch(`/admin/users/${userId}/unban`)
}

// 重置用户密码（返回临时密码）
// 返回: Promise<{ code, message, data: { newPassword } }>
export function resetUserPassword(userId) {
  return http.post(`/admin/users/${userId}/password/reset`)
}

// 查看用户会话元数据（严禁含 messages 内容）
// 返回: Promise<{ code, message, data: ConsultationMeta[] }>
export function getUserConsultations(userId) {
  return http.get(`/admin/users/${userId}/consultations`)
}

// 获取系统操作日志
// 返回: Promise<{ code, message, data: { total, page, size, pages, logs[] } }>
export function getAdminLogs(params = {}) {
  return http.get('/admin/logs', { params })
}
```

---

## 三、consultationService.js

```js
// src/services/consultationService.js — 方法签名示意
import http from './http'

// 创建问诊会话
// 返回: Promise<{ code, message, data: { consultationId, status, createdAt } }>
export function createConsultation(chiefComplaint) {
  return http.post('/consultations', { chiefComplaint })
}

// 获取问诊列表
// 返回: Promise<{ code, message, data: { total, page, size, pages, consultations[] } }>
export function getConsultationList(page = 1, size = 20) {
  return http.get('/consultations', { params: { page, size } })
}

// 获取指定会话的消息历史
// 返回: Promise<{ code, message, data: { consultationId, status, riskLevel, messages[] } }>
export function getMessages(consultationId) {
  return http.get(`/consultations/${consultationId}/messages`)
}

// 结束问诊会话
// 返回: Promise<{ code, message, data: ConsultationItemResponse }>
export function completeConsultation(consultationId) {
  return http.patch(`/consultations/${consultationId}/status`)
}

// 发送消息（SSE 流式）— 使用原生 fetch + EventSource 模式
// 见下方 SSE 章节
export function sendMessageStream(consultationId, content, onToken, onDone) {
  // 见 SSE 处理方式
}
```

---

## 四、SSE 流式消息处理

后端流式接口为 `POST /api/consultations/{id}/messages/stream`，使用 SSE 格式推送。

由于 `EventSource` 原生只支持 GET 请求，需要用 **fetch + ReadableStream** 手动处理 SSE。

**设计思路：**

```js
// src/services/consultationService.js 中的 sendMessageStream 示意
export async function sendMessageStream(consultationId, content, onToken, onDone) {
  const token = useAuthStore().token

  const response = await fetch(`/api/consultations/${consultationId}/messages/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ content })
  })

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop()   // 保留最后不完整的行

    for (const line of lines) {
      if (line.startsWith('data:')) {
        const data = line.slice(5).trim()
        if (!data) continue

        // 检测结束事件
        if (data.startsWith('{') && data.includes('"type":"done"')) {
          const parsed = JSON.parse(data)
          onDone(parsed.riskLevel || null)
        } else {
          onToken(data)   // 每个 token 回调给组件更新 UI
        }
      }
    }
  }
}
```

**调用方式（组件中）：**

```js
let aiReply = ''
await sendMessageStream(
  consultationId,
  userInput,
  (token) => { aiReply += token  /* 逐字更新 AI 消息气泡 */ },
  (riskLevel) => { /* 更新风险等级展示 */ }
)
```

---

## 五、reportService.js

```js
// src/services/reportService.js — 方法签名示意
import http from './http'

// 生成报告（POST，幂等）
// 返回: Promise<{ code, message, data: { reportId, downloadUrl, fileSize, createdAt } }>
export function generateReport(consultationId) {
  return http.post(`/consultations/${consultationId}/report`)
}

// 查询报告信息（GET）
// 返回: Promise<{ code, message, data: { reportId, downloadUrl, fileSize, createdAt } }>
export function getReport(consultationId) {
  return http.get(`/consultations/${consultationId}/report`)
}

// 下载报告文件 URL（直接在浏览器中打开/下载，不走 axios）
// 使用方式: window.open(getReportDownloadUrl(reportId))
export function getReportDownloadUrl(reportId) {
  return `/api/reports/${reportId}/file`
}
```

> 下载 PDF 文件时，直接用 `window.open(url)` 打开后端文件流接口。但该接口需要鉴权，
> 需要在 URL 中附带 Token，或由后端临时放行该接口。
> 最简单的方案：前端发起带 Authorization Header 的 fetch 请求，获取 blob 后用
> `URL.createObjectURL` 创建本地 URL 再触发下载。
