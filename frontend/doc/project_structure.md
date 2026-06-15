# 前端项目目录结构

## 一、初始化命令

```bash
npm create vite@latest health-frontend -- --template vue
cd health-frontend
npm install
npm install vue-router@4 pinia axios element-plus
npm install @element-plus/icons-vue
```

---

## 二、目录结构

```
health-frontend/
├── public/
│   └── favicon.ico
├── src/
│   ├── main.js                  # 应用入口，挂载 Vue、Router、Pinia、Element Plus
│   ├── App.vue                  # 根组件
│   │
│   ├── router/
│   │   └── index.js             # 路由配置（见 router.md）
│   │
│   ├── stores/                  # Pinia 状态管理（见 state_management.md）
│   │   ├── auth.js              # 用户认证 store（token、用户信息）
│   │   └── consultation.js     # 问诊 store（会话列表、当前会话、消息）
│   │
│   ├── services/                # API 请求封装（见 api_service.md）
│   │   ├── http.js              # axios 实例封装（统一拦截器）
│   │   ├── authService.js       # 认证相关接口
│   │   ├── consultationService.js  # 问诊相关接口
│   │   └── reportService.js     # 报告相关接口
│   │
│   ├── views/                   # 页面级组件（与路由对应）
│   │   ├── LoginView.vue        # 登录页
│   │   ├── RegisterView.vue     # 注册页
│   │   └── ChatView.vue         # 主聊天页（三栏布局容器）
│   │
│   ├── components/              # 可复用子组件
│   │   ├── layout/
│   │   │   ├── NavBar.vue       # 顶部导航栏
│   │   │   └── Sidebar.vue      # 左侧历史会话列表
│   │   ├── chat/
│   │   │   ├── MessageList.vue  # 消息列表（气泡展示）
│   │   │   ├── MessageItem.vue  # 单条消息气泡
│   │   │   ├── ChatInput.vue    # 底部输入框 + 发送按钮
│   │   │   └── ChatHeader.vue   # 聊天区顶部（会话名、风险等级、操作按钮）
│   │   └── common/
│   │       ├── RiskBadge.vue    # 风险等级标签组件（low/medium/high/urgent）
│   │       └── LoadingDots.vue  # AI 回复中的三点加载动画
│   │
│   └── assets/
│       └── styles/
│           └── main.css         # 全局样式（颜色变量、重置样式）
│
├── index.html
├── vite.config.js               # Vite 配置（含 API 代理）
└── package.json
```

---

## 三、各目录职责说明

| 目录 | 职责 |
|------|------|
| `views/` | 页面级组件，每个文件对应一个路由，不做细粒度拆分 |
| `components/` | 可复用的 UI 子组件，不直接调用 API |
| `services/` | 所有后端 API 调用集中在此，views 和 stores 通过 service 访问后端 |
| `stores/` | 跨组件共享的状态，如登录用户信息、当前会话消息列表 |
| `router/` | 路由配置和导航守卫 |

---

## 四、vite.config.js 代理配置

本地开发时，前端运行在 `localhost:5173`，后端运行在 `localhost:8080`。
为避免跨域问题，在 Vite 中配置代理：

```js
// vite.config.js
export default {
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}
```

> 生产环境部署时，由 Nginx 统一代理，无需修改代码中的请求路径。
