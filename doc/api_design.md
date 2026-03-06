# API接口设计文档

## 接口规范

### 统一响应格式
所有接口返回统一的JSON格式：
```json
{
  "code": 200,
  "message": "成功",
  "data": { ... },
  "timestamp": 1710468600000
}
```

**状态码说明：**
- 200：成功
- 400：客户端请求错误（参数错误、业务逻辑错误）
- 401：未授权（token无效或过期）
- 404：资源不存在
- 500：服务器内部错误

### 认证方式
除注册、登录接口外，所有接口需要在 Header 中携带 JWT Token：
```
Authorization: Bearer {token}
```

### 注意事项
- 所有路径使用**复数名词**，不在 URL 中使用动词
- AI 对话接口提供**同步**和 **SSE 流式**两种版本，推荐流式
- 同步接口需配置超时：`server.tomcat.connection-timeout=60s`

---

## 接口清单

### 1. 用户认证接口

#### 1.1 用户注册
**接口：** `POST /api/auth/register`

**请求体：**
```json
{
  "username": "testuser",
  "password": "123456",
  "email": "test@example.com"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 1,
    "username": "testuser"
  },
  "timestamp": 1710468600000
}
```

**业务规则：**
- 用户名长度 3~50 字符，唯一；重复时返回 400
- 密码长度 6~20 字符，BCrypt 加密存储
- 邮箱可选

---

#### 1.2 用户登录
**接口：** `POST /api/auth/login`

**请求体：**
```json
{
  "username": "testuser",
  "password": "123456"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "username": "testuser"
  },
  "timestamp": 1710468600000
}
```

**业务规则：**
- 验证用户名与密码
- 生成 JWT Token，有效期 24 小时
- Token Payload 包含 userId 和 username

---

#### 1.3 获取当前用户信息
**接口：** `GET /api/users/me`

**请求 Header：**
```
Authorization: Bearer {token}
```

**响应：**
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "userId": 1,
    "username": "testuser",
    "email": "test@example.com",
    "createdAt": "2024-01-01T00:00:00"
  },
  "timestamp": 1710468600000
}
```

**业务规则：**
- 从 Token 中解析 userId，查询并返回用户信息
- 用于前端页面刷新后恢复登录状态

---

### 2. 问诊会话接口

#### 2.1 创建问诊会话
**接口：** `POST /api/consultations`

**请求 Header：**
```
Authorization: Bearer {token}
```

**请求体：**
```json
{
  "chiefComplaint": "我头痛发热三天了"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "会话创建成功",
  "data": {
    "consultationId": 1,
    "status": "ongoing",
    "createdAt": "2024-03-15T10:30:00"
  },
  "timestamp": 1710468600000
}
```

**业务规则：**
- 主诉内容不能为空，长度限制 1~500 字符
- 创建时自动将主诉作为第一条 user 消息写入 messages 表

---

#### 2.2 发送消息（同步）
**接口：** `POST /api/consultations/{consultationId}/messages`

**请求 Header：**
```
Authorization: Bearer {token}
```

**请求体：**
```json
{
  "content": "头痛，还伴有恶心"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "消息发送成功",
  "data": {
    "userMessage": {
      "id": 1,
      "role": "user",
      "content": "头痛，还伴有恶心",
      "createdAt": "2024-03-15T10:31:00"
    },
    "assistantReply": {
      "id": 2,
      "role": "assistant",
      "content": "根据您的症状，可能是偏头痛或感冒引起的。请问：\n1. 头痛是持续性的还是阵发性的？\n2. 疼痛具体在头部哪个位置？",
      "createdAt": "2024-03-15T10:31:05"
    }
  },
  "timestamp": 1710468600000
}
```

**业务规则：**
- 校验会话属于当前登录用户，否则返回 403
- 校验会话状态为 `ongoing`，已结束的会话不允许继续发消息（返回 400）
- 消息内容长度限制：1~500 字符
- 保存用户消息到 messages 表
- 调用 RAG 检索相关医学知识（Top-3）
- 调用 Qwen 生成回复并保存到 messages 表
- 若检测到高风险症状关键词，更新 consultations.risk_level

---

#### 2.3 发送消息（SSE 流式，推荐）
**接口：** `POST /api/consultations/{consultationId}/messages/stream`

**请求 Header：**
```
Authorization: Bearer {token}
```

**请求体：**
```json
{
  "content": "头痛，还伴有恶心"
}
```

**响应：**
- `Content-Type: text/event-stream`
- 先保存用户消息，随后以 SSE 事件逐 token 推送 AI 回复，最后推送结束事件

```
data: {"token": "根据"}

data: {"token": "您的症状"}

data: {"token": "，可能是偏头痛"}

data: {"type": "done", "messageId": 2, "riskLevel": "medium"}
```

**业务规则：**
- 校验逻辑与同步接口相同
- AI 回复完整内容在流结束后统一保存到 messages 表
- `done` 事件中携带最终 messageId 和 riskLevel

---

#### 2.4 获取会话消息历史
**接口：** `GET /api/consultations/{consultationId}/messages`

**请求 Header：**
```
Authorization: Bearer {token}
```

**响应：**
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "consultationId": 1,
    "status": "ongoing",
    "riskLevel": "medium",
    "messages": [
      {
        "id": 1,
        "role": "user",
        "content": "我头痛发热三天了",
        "createdAt": "2024-03-15T10:30:00"
      },
      {
        "id": 2,
        "role": "assistant",
        "content": "请问除了头痛发热，还有其他症状吗？",
        "createdAt": "2024-03-15T10:30:05"
      }
    ]
  },
  "timestamp": 1710468600000
}
```

**业务规则：**
- 校验会话属于当前登录用户，否则返回 403
- 消息按 created_at 升序返回

---

#### 2.5 结束问诊
**接口：** `PATCH /api/consultations/{consultationId}/status`

**请求 Header：**
```
Authorization: Bearer {token}
```

**请求体：**
```json
{
  "status": "completed"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "问诊已结束",
  "data": {
    "consultationId": 1,
    "status": "completed",
    "riskLevel": "medium",
    "completedAt": "2024-03-15T10:45:00"
  },
  "timestamp": 1710468600000
}
```

**业务规则：**
- 校验会话属于当前登录用户，否则返回 403
- 更新 consultations.status 为 `completed`
- 更新 consultations.completed_at 为当前时间
- 已结束的会话重复请求直接返回当前状态，不报错

---

#### 2.6 获取问诊列表
**接口：** `GET /api/consultations`

**请求 Header：**
```
Authorization: Bearer {token}
```

**查询参数：**
- `page`：页码（默认 1）
- `size`：每页数量（默认 10）

**响应：**
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "total": 25,
    "page": 1,
    "size": 10,
    "pages": 3,
    "consultations": [
      {
        "id": 5,
        "chiefComplaint": "胸闷气短",
        "status": "completed",
        "riskLevel": "high",
        "createdAt": "2024-03-15T10:30:00",
        "completedAt": "2024-03-15T10:45:00"
      },
      {
        "id": 4,
        "chiefComplaint": "头痛发热",
        "status": "completed",
        "riskLevel": "medium",
        "createdAt": "2024-03-14T15:20:00",
        "completedAt": "2024-03-14T15:35:00"
      }
    ]
  },
  "timestamp": 1710468600000
}
```

**业务规则：**
- 只返回当前登录用户自己的问诊记录
- 按 created_at 降序排列

---

### 3. 报告接口

#### 3.1 生成报告
**接口：** `POST /api/consultations/{consultationId}/report`

**请求 Header：**
```
Authorization: Bearer {token}
```

**响应：**
```json
{
  "code": 200,
  "message": "报告生成成功",
  "data": {
    "reportId": 1,
    "consultationId": 1,
    "downloadUrl": "/api/reports/1/file",
    "fileSize": 245678,
    "createdAt": "2024-03-15T10:50:00"
  },
  "timestamp": 1710468600000
}
```

**业务规则：**
- 校验会话属于当前登录用户，否则返回 403
- 只能为 `completed` 状态的问诊生成报告，否则返回 400
- 同一问诊重复请求直接返回已有报告信息，不重复生成
- PDF 内容包含：用户信息、主诉、对话记录、诊断建议、风险等级、就医建议、免责声明

---

#### 3.2 查询报告信息
**接口：** `GET /api/consultations/{consultationId}/report`

**请求 Header：**
```
Authorization: Bearer {token}
```

**响应（报告存在时）：**
```json
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "reportId": 1,
    "downloadUrl": "/api/reports/1/file",
    "fileSize": 245678,
    "createdAt": "2024-03-15T10:50:00"
  },
  "timestamp": 1710468600000
}
```

**响应（报告不存在时）：**
```json
{
  "code": 404,
  "message": "报告尚未生成",
  "data": null,
  "timestamp": 1710468600000
}
```

**业务规则：**
- 校验会话属于当前登录用户，否则返回 403
- 前端用此接口判断是否显示"生成报告"或"下载报告"按钮

---

#### 3.3 下载报告文件
**接口：** `GET /api/reports/{reportId}/file`

**请求 Header：**
```
Authorization: Bearer {token}
```

**响应：**
- `Content-Type: application/pdf`
- `Content-Disposition: attachment; filename="report_{reportId}.pdf"`
- 响应体为 PDF 文件流

**业务规则：**
- 校验报告属于当前登录用户，否则返回 403
- 从配置的存储目录读取文件并以流的形式返回

---

## 错误响应示例

```json
{
  "code": 400,
  "message": "用户名已存在",
  "data": null,
  "timestamp": 1710468600000
}
```

```json
{
  "code": 401,
  "message": "Token 无效或已过期",
  "data": null,
  "timestamp": 1710468600000
}
```

```json
{
  "code": 403,
  "message": "无权访问该资源",
  "data": null,
  "timestamp": 1710468600000
}
```

```json
{
  "code": 404,
  "message": "资源不存在",
  "data": null,
  "timestamp": 1710468600000
}
```

```json
{
  "code": 500,
  "message": "服务器内部错误",
  "data": null,
  "timestamp": 1710468600000
}
```

---

## 接口汇总

| 方法 | 路径 | 说明 | 需要认证 |
|------|------|------|----------|
| POST | /api/auth/register | 用户注册 | 否 |
| POST | /api/auth/login | 用户登录 | 否 |
| GET | /api/users/me | 获取当前用户信息 | 是 |
| POST | /api/consultations | 创建问诊会话 | 是 |
| GET | /api/consultations | 获取问诊列表 | 是 |
| POST | /api/consultations/{id}/messages | 发送消息（同步） | 是 |
| POST | /api/consultations/{id}/messages/stream | 发送消息（SSE流式） | 是 |
| GET | /api/consultations/{id}/messages | 获取会话消息历史 | 是 |
| PATCH | /api/consultations/{id}/status | 结束问诊 | 是 |
| POST | /api/consultations/{id}/report | 生成报告 | 是 |
| GET | /api/consultations/{id}/report | 查询报告信息 | 是 |
| GET | /api/reports/{reportId}/file | 下载报告文件 | 是 |