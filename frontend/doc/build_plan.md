# Frontend Build Plan

## Task List

### Phase 1: Project Setup
1. Initialize Vite + Vue 3 project (`health-frontend/`)
2. Install dependencies: vue-router@4, pinia, axios, element-plus, @element-plus/icons-vue
3. Configure `vite.config.js` with `/api` proxy to `localhost:8080`

### Phase 2: Foundation Files
4. `src/assets/styles/main.css` — CSS variables, global resets, Inter font
5. `src/main.js` — Mount Vue, Router, Pinia, Element Plus
6. `src/App.vue` — Root component with `<RouterView />`

### Phase 3: Services Layer
7. `src/services/http.js` — Axios instance, request/response interceptors (JWT inject, 401 redirect)
8. `src/services/authService.js` — `register()`, `login()`, `getUserInfo()`
9. `src/services/consultationService.js` — CRUD + SSE stream (`sendMessageStream` via fetch + ReadableStream)
10. `src/services/reportService.js` — `generateReport()`, `getReport()`, `getReportDownloadUrl()`

### Phase 4: State Management (Pinia Stores)
11. `src/stores/auth.js` — token/userId/username, persisted in localStorage, `login()` / `logout()`
12. `src/stores/consultation.js` — list, currentId, messages, aiTyping; all actions wired to services

### Phase 5: Router
13. `src/router/index.js` — Routes for `/login`, `/register`, `/chat`, `/chat/:consultationId`; `beforeEach` guard

### Phase 6: Common Components
14. `src/components/common/RiskBadge.vue` — Colored tag for low/medium/high/urgent
15. `src/components/common/LoadingDots.vue` — Three-dot streaming animation

### Phase 7: Layout Components
16. `src/components/layout/NavBar.vue` — Logo, system name, user avatar dropdown (profile/logout)
17. `src/components/layout/Sidebar.vue` — New Consultation button, scrollable history list, active highlight

### Phase 8: Chat Components
18. `src/components/chat/MessageItem.vue` — Single bubble (user: right/#E8F3FF, AI: left/white)
19. `src/components/chat/MessageList.vue` — Renders bubbles, auto-scrolls to bottom
20. `src/components/chat/ChatInput.vue` — Text input + Send button, disabled during aiTyping
21. `src/components/chat/ChatHeader.vue` — Title, RiskBadge, End/Generate/Download buttons

### Phase 9: Views
22. `src/views/LoginView.vue` — Login form, calls `authStore.login()`, redirects to `/chat`
23. `src/views/RegisterView.vue` — Register form with validation, redirects to `/login`
24. `src/views/ChatView.vue` — Three-column layout, fetches list on mount, responds to route param

---

## Design Reference

| Token | Value |
|-------|-------|
| Primary color | `#2F80ED` |
| Background | `#F5FAFF` |
| Card/bubble background | `#FFFFFF` |
| Light surface | `#F3F4F6` |
| Border | `#E5E7EB` |
| Text | `#374151` |
| User bubble | `#E8F3FF` |
| Button border-radius | `10px` |
| Card border-radius | `12px` |
| Bubble border-radius | `14px` |
| Font | Inter, system-ui |
| Title size | `18px / 600` |
| Body size | `14px` |
| Navbar height | `64px` |
| Sidebar width | `260px` |
| Min supported width | `1280px` |

## API Endpoints Summary

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/auth/register` | Register |
| POST | `/api/auth/login` | Login |
| GET | `/api/users/me` | Get user info |
| POST | `/api/consultations` | Create consultation |
| GET | `/api/consultations` | List consultations (page, size) |
| GET | `/api/consultations/:id/messages` | Get messages + status + riskLevel |
| POST | `/api/consultations/:id/messages/stream` | SSE stream send message |
| PATCH | `/api/consultations/:id/status` | End consultation |
| POST | `/api/consultations/:id/report` | Generate report |
| GET | `/api/consultations/:id/report` | Query report info |
| GET | `/api/reports/:id/file` | Download PDF file |
