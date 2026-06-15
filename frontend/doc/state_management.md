# 状态管理设计（Pinia）

## 一、整体说明

使用 Pinia 管理两类全局状态：

| Store | 文件 | 职责 |
|-------|------|------|
| `useAuthStore` | `stores/auth.js` | 用户登录状态、Token、用户基本信息 |
| `useConsultationStore` | `stores/consultation.js` | 问诊列表、当前会话、当前消息列表 |

---

## 二、useAuthStore（stores/auth.js）

### 2.1 状态（state）

| 字段 | 类型 | 说明 |
|------|------|------|
| `token` | `string \| null` | JWT Token，持久化到 localStorage |
| `userId` | `number \| null` | 当前登录用户 ID |
| `username` | `string \| null` | 当前登录用户名 |

### 2.2 操作（actions）

| 方法 | 说明 |
|------|------|
| `login(username, password)` | 调用登录接口，保存 token 和用户信息到 state 和 localStorage |
| `logout()` | 清除 state 和 localStorage 中的 token，跳转 `/login` |
| `loadFromStorage()` | 页面刷新时从 localStorage 恢复 token（在 `main.js` 调用） |

### 2.3 代码结构示意

```js
// src/stores/auth.js
import { defineStore } from 'pinia'
import { login as loginApi } from '@/services/authService'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    userId: Number(localStorage.getItem('userId')) || null,
    username: localStorage.getItem('username') || null,
  }),

  actions: {
    async login(username, password) {
      const res = await loginApi(username, password)
      // res.data 为 { token, userId, username }
      this.token = res.data.token
      this.userId = res.data.userId
      this.username = res.data.username
      // 持久化
      localStorage.setItem('token', this.token)
      localStorage.setItem('userId', this.userId)
      localStorage.setItem('username', this.username)
    },

    logout() {
      this.token = null
      this.userId = null
      this.username = null
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
    }
  }
})
```

---

## 三、useConsultationStore（stores/consultation.js）

### 3.1 状态（state）

| 字段 | 类型 | 说明 |
|------|------|------|
| `list` | `ConsultationItem[]` | 历史会话列表（左侧边栏数据源） |
| `listTotal` | `number` | 列表总数（用于分页） |
| `currentId` | `number \| null` | 当前打开的会话 ID |
| `currentStatus` | `string \| null` | 当前会话状态：`ongoing` / `completed` |
| `currentRiskLevel` | `string \| null` | 当前会话风险等级 |
| `messages` | `Message[]` | 当前会话的消息列表（聊天区数据源） |
| `aiTyping` | `boolean` | AI 是否正在输出（用于显示加载动画） |

### 3.2 操作（actions）

| 方法 | 说明 |
|------|------|
| `fetchList(page, size)` | 拉取问诊历史列表，更新 `list` |
| `openConsultation(id)` | 切换当前会话：拉取消息历史，更新 `currentId`、`messages`、状态等 |
| `createConsultation(chiefComplaint)` | 创建新会话，成功后刷新列表，返回新 consultationId |
| `sendMessage(content)` | 流式发送消息，实时追加 AI 回复 token 到 `messages`，完成后更新 `riskLevel` |
| `completeConsultation()` | 结束当前会话，更新 `currentStatus` 为 `completed` |

### 3.3 代码结构示意

```js
// src/stores/consultation.js
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

    async sendMessage(content) {
      if (!this.currentId) return

      // 立即追加用户消息到 UI
      this.messages.push({ role: 'user', content, createdAt: new Date().toISOString() })

      // 追加空 AI 消息占位
      const aiMsg = { role: 'assistant', content: '', createdAt: new Date().toISOString() }
      this.messages.push(aiMsg)
      this.aiTyping = true

      await consultationService.sendMessageStream(
        this.currentId,
        content,
        (token) => { aiMsg.content += token },      // 逐字追加
        (riskLevel) => {
          this.currentRiskLevel = riskLevel
          this.aiTyping = false
        }
      )
    },

    async completeConsultation() {
      if (!this.currentId) return
      await consultationService.completeConsultation(this.currentId)
      this.currentStatus = 'completed'
      // 同步更新列表中该条目状态
      const item = this.list.find(c => c.id === this.currentId)
      if (item) item.status = 'completed'
    }
  }
})
```

---

## 四、组件与 Store 的交互关系

```
ChatView.vue
  ├── 挂载时 → consultationStore.fetchList()
  ├── 路由参数变化 → consultationStore.openConsultation(id)
  │
  ├── Sidebar.vue
  │     ├── 读取 consultationStore.list
  │     ├── 点击会话 → router.push('/chat/:id')
  │     └── 点击新建 → consultationStore.createConsultation()
  │
  ├── ChatHeader.vue
  │     ├── 读取 consultationStore.currentStatus
  │     ├── 读取 consultationStore.currentRiskLevel
  │     └── 点击结束 → consultationStore.completeConsultation()
  │
  ├── MessageList.vue
  │     └── 读取 consultationStore.messages
  │
  └── ChatInput.vue
        └── 提交 → consultationStore.sendMessage(content)
```
