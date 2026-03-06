# 开发任务分解

## 重要开发原则
```
1. 不要一次性生成所有代码
2. 每个任务独立完成后，等待确认再继续
3. 先生成接口框架，再实现业务逻辑
4. 先实现核心功能，再添加错误处理
5. 每完成一个模块，提供测试命令
6. 严格遵循 doc/ 目录下的所有文档规范
7. 所有接口路径以 api_design.md 为准（复数 RESTful 风格）
```

---

## 阶段0：项目初始化

### 任务0.1：检查项目骨架与依赖配置

**Claude Code 提示词：**
```
项目已使用 Spring Boot 3.4.3 + Spring AI 1.0.0-M6 初始化。

请完成以下工作：
1. 检查 pom.xml，按照 doc/spring_ai_guide.md 第1节补全所有缺失依赖：
   - spring-ai-starter-vector-store-pgvector
   - mybatis-plus-spring-boot3-starter 3.5.10.1
   - spring-security-crypto
   - jjwt-api / jjwt-impl / jjwt-jackson 0.12.3
   - spring-boot-starter-validation
   - itext7-core 7.2.5
2. 按照 doc/spring_ai_guide.md 第2节创建完整的 src/main/resources/application.yml
3. 创建 src/main/resources/application-dev.yml
4. 按照 doc/tech_spec.md 的项目结构创建所有空包（controller/service/mapper/entity/dto/common/util/config/interceptor/exception）

不要生成任何业务代码，只完成依赖和配置。
完成后告诉我如何验证项目能启动。
```

**验证：**
```bash
mvn clean compile
mvn spring-boot:run
# 访问 http://localhost:8080，返回 404 即正常（还没写接口）
```

---

### 任务0.2：初始化数据库

**手动执行（分两步）：**

第一步：创建数据库（在默认 postgres 库执行）
```sql
CREATE DATABASE health_diagnosis;
```

第二步：连接到 health_diagnosis 后执行
```sql
-- 安装 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 执行 doc/database_design.md 中的所有建表 SQL
-- 建议使用修正后的版本（CHECK 约束、HNSW 索引）
```

**验证：**
```sql
\dt                              -- 查看所有表，应有5张
SELECT extname FROM pg_extension WHERE extname = 'vector';  -- 应返回 'vector'
```

---

## 阶段1：认证功能

### 任务1.1：生成 User 实体类和 Mapper

**Claude Code 提示词：**
```
参考 doc/database_design.md 的 users 表，生成：

1. entity/User.java
   - 使用 @TableName("users") 注解
   - 使用 Lombok 的 @Data 注解
   - 字段使用驼峰命名（id, username, passwordHash, email, createdAt）

2. mapper/UserMapper.java
   - 继承 BaseMapper<User>
   - 使用 @Mapper 注解

只生成这两个文件。
```

**验证：**
```bash
mvn compile  # 无编译错误即可
```

---

### 任务1.2：实现统一响应封装与异常处理

**Claude Code 提示词：**
```
参考 doc/api_design.md 的统一响应格式，生成：

1. common/Result.java（注意：在 common 包，不是 util 包）
   - 字段：int code, String message, T data, long timestamp
   - timestamp 默认值：System.currentTimeMillis()
   - 提供静态工厂方法：success(T data)、error(int code, String message)

2. common/ErrorCode.java
   - 定义常量：SUCCESS=200, BAD_REQUEST=400, UNAUTHORIZED=401,
     FORBIDDEN=403, NOT_FOUND=404, SERVER_ERROR=500
   - 业务码：USER_NOT_FOUND=1001, WRONG_PASSWORD=1002,
     USERNAME_EXISTS=1003, TOKEN_EXPIRED=1004,
     CONSULTATION_NOT_FOUND=1005, ACCESS_DENIED=1006,
     CONSULTATION_ALREADY_COMPLETED=1007

3. exception/BusinessException.java
   - 包含 int code 和 String message 字段
   - 提供构造方法 BusinessException(int code, String message)

4. exception/GlobalExceptionHandler.java
   - 使用 @RestControllerAdvice
   - 捕获 BusinessException，返回对应 code 和 message
   - 捕获 MethodArgumentNotValidException（参数校验失败），返回 400
   - 捕获其他 Exception，返回 500，日志记录堆栈

只生成这四个文件。
```

**验证：**
```bash
mvn compile
```

---

### 任务1.3：实现 JWT 工具类

**Claude Code 提示词：**
```
生成 util/JwtUtil.java：

1. 使用 JJWT 0.12.x 库（注意：0.12.x API 与旧版不同）
2. 从 application.yml 读取 jwt.secret 和 jwt.expiration
3. 提供方法：
   - generateToken(Long userId, String username) -> String
   - parseToken(String token) -> Claims
   - getUserId(String token) -> Long（从 Claims 提取）
   - isTokenExpired(String token) -> boolean

使用 @Component 注入，不要用静态方法（便于测试）。
只生成这一个文件。
```

**验证：**
```java
// 在测试类中临时验证
String token = jwtUtil.generateToken(1L, "test");
System.out.println(token);  // 应输出三段式 JWT 字符串
```

---

### 任务1.4：实现 Token 认证拦截器

**Claude Code 提示词：**
```
实现 JWT Token 认证机制（纯 JWT 无状态方案，不使用 HashMap）：

1. interceptor/AuthInterceptor.java
   - 实现 HandlerInterceptor
   - 从 Header 提取 Authorization: Bearer {token}
   - 调用 JwtUtil.parseToken() 验证 Token
   - Token 无效或过期时返回 401（直接写 response，不走 Controller）
   - 验证通过后将 userId 存入 request.setAttribute("userId", userId)

2. config/WebConfig.java
   - 注册 AuthInterceptor
   - 排除路径：/api/auth/**

只生成这两个文件。
```

**验证：**
```bash
mvn spring-boot:run

# 不带 Token（应返回 401）
curl -i http://localhost:8080/api/consultations

# 带无效 Token（应返回 401）
curl -i -H "Authorization: Bearer invalid_token" http://localhost:8080/api/consultations
```

---

### 任务1.5：实现用户注册接口

**Claude Code 提示词：**
```
参考 doc/api_design.md 1.1节，实现用户注册功能：

1. dto/request/RegisterRequest.java
   - username（@NotBlank, @Size(min=3, max=50)）
   - password（@NotBlank, @Size(min=6, max=20)）
   - email（@Email，可为空）

2. service/AuthService.java（接口 + 实现类）
   - register(RegisterRequest request) -> Result
   - 检查用户名是否已存在，存在则抛出 BusinessException(USERNAME_EXISTS)
   - 使用 BCryptPasswordEncoder 加密密码
   - 插入 users 表

3. controller/AuthController.java
   - POST /api/auth/register
   - 使用 @Valid 校验请求体

完成后提供 curl 测试命令。
```

**测试：**
```bash
# 正常注册
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456","email":"test@qq.com"}'
# 期望：{"code":200,"message":"注册成功","data":{"userId":1,"username":"testuser"}}

# 重复注册（应返回 1003）
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}'
```

---

### 任务1.6：实现用户登录接口

**Claude Code 提示词：**
```
参考 doc/api_design.md 1.2节，实现用户登录功能：

1. dto/request/LoginRequest.java
   - username, password 字段（均 @NotBlank）

2. dto/response/LoginResponse.java
   - token, userId, username 字段

3. AuthService.login(LoginRequest request) -> LoginResponse
   - 查询用户，不存在则抛 BusinessException(USER_NOT_FOUND)
   - 验证密码，不匹配则抛 BusinessException(WRONG_PASSWORD)
   - 调用 JwtUtil.generateToken() 生成 Token
   - 返回 LoginResponse（纯 JWT 无状态，无需 HashMap 存储）

4. AuthController 添加 POST /api/auth/login

完成后提供 curl 测试命令。
```

**测试：**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}'
# 复制返回的 token，后续测试使用
# 导出为变量：TOKEN=<粘贴 token 值>
```

---

### 任务1.7：实现获取当前用户信息接口

**Claude Code 提示词：**
```
参考 doc/api_design.md 1.3节，实现：

1. AuthService.getUserInfo(Long userId) -> UserInfoResponse
   - 根据 userId 查询 users 表
   - 返回 userId, username, email, createdAt（不返回密码）

2. AuthController 添加 GET /api/users/me
   - 从 request.getAttribute("userId") 获取当前用户 ID
   - 调用 getUserInfo()

完成后提供 curl 测试命令。
```

**测试：**
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users/me
# 期望返回用户信息
```

---

## 阶段2：问诊功能（不含 AI）

### 任务2.1：生成问诊相关实体类

**Claude Code 提示词：**
```
参考 doc/database_design.md，生成：

1. entity/Consultation.java（对应 consultations 表）
2. entity/Message.java（对应 messages 表）
3. mapper/ConsultationMapper.java（继承 BaseMapper<Consultation>）
4. mapper/MessageMapper.java（继承 BaseMapper<Message>）

只生成这四个文件，不生成业务逻辑。
```

---

### 任务2.2：实现创建问诊接口

**Claude Code 提示词：**
```
参考 doc/api_design.md 2.1节，实现创建问诊：

1. dto/request/CreateConsultationRequest.java
   - chiefComplaint（@NotBlank, @Size(max=500)）

2. dto/response/ConsultationResponse.java
   - consultationId, status, createdAt

3. service/ConsultationService.java（接口 + 实现类）
   - createConsultation(Long userId, CreateConsultationRequest req)
   - 插入 consultations 表，status='ongoing'

4. controller/ConsultationController.java
   - POST /api/consultations
   - 从 request.getAttribute("userId") 获取 userId

提供 curl 测试命令。
```

**测试：**
```bash
curl -X POST http://localhost:8080/api/consultations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"chiefComplaint":"我头痛发热三天了"}'
# 记录返回的 consultationId，导出：CUID=<consultationId>
```

---

### 任务2.3：实现发送消息接口（暂不集成 AI）

**Claude Code 提示词：**
```
参考 doc/api_design.md 2.2节，实现发送消息（暂用固定回复代替 AI）：

1. dto/request/SendMessageRequest.java
   - content（@NotBlank, @Size(max=500)）

2. dto/response/MessageResponse.java
   - id, role, content, createdAt

3. ConsultationService.sendMessage(Long consultationId, Long userId, String content)
   - 校验会话属于该用户，否则抛 BusinessException(ACCESS_DENIED)
   - 校验会话状态为 ongoing，否则抛 BusinessException(CONSULTATION_ALREADY_COMPLETED)
   - 保存用户消息到 messages 表
   - 暂时返回固定回复："收到您的消息，正在分析中..."
   - 保存助手回复到 messages 表
   - 返回 userMessage + assistantReply

4. ConsultationController 添加 POST /api/consultations/{consultationId}/messages

提供 curl 测试命令。
```

**测试：**
```bash
curl -X POST http://localhost:8080/api/consultations/$CUID/messages \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"头痛，还伴有恶心"}'
```

---

### 任务2.4：实现获取会话消息历史接口

**Claude Code 提示词：**
```
参考 doc/api_design.md 2.4节，实现：

ConsultationService.getMessages(Long consultationId, Long userId)
- 校验会话属于该用户
- 查询 messages 表，按 created_at 升序
- 返回包含 consultationId、status、riskLevel、messages 列表的响应

ConsultationController 添加 GET /api/consultations/{consultationId}/messages

提供 curl 测试命令。
```

---

### 任务2.5：实现获取问诊列表接口

**Claude Code 提示词：**
```
参考 doc/api_design.md 2.6节，实现：

ConsultationService.getConsultationList(Long userId, int page, int size)
- 使用 MyBatis-Plus 的 Page 分页
- 只查当前用户的问诊
- 按 created_at 倒序
- 返回包含 total、page、size、pages、consultations 的分页响应

ConsultationController 添加 GET /api/consultations?page=1&size=10

提供 curl 测试命令。
```

---

### 任务2.6：实现结束问诊接口

**Claude Code 提示词：**
```
参考 doc/api_design.md 2.5节，实现：

ConsultationService.completeConsultation(Long consultationId, Long userId)
- 校验会话属于该用户
- 若已是 completed 状态，直接返回当前数据（幂等）
- 更新 status='completed'，completed_at=NOW()

ConsultationController 添加 PATCH /api/consultations/{consultationId}/status
无请求体（接口语义固定为"结束"，status 值由服务端写死）

提供 curl 测试命令。
```

**测试：**
```bash
curl -X PATCH http://localhost:8080/api/consultations/$CUID/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"completed"}'
```

---

## 阶段3：Spring AI 集成

> **前置说明（Spring AI 1.0.0-M6 关键 API）**
> - 注入：`ChatClient.Builder` → 手动 build 一个 `ChatClient` Bean；勿直接 `@Autowired ChatClient`
> - 调用：`chatClient.prompt().user(x).call().content()`（同步）；`.stream().content()` 返回 `Flux<String>`（流式）
> - 向量检索：`SearchRequest.builder().query(q).topK(3).similarityThreshold(0.5).build()`
> - 嵌入模型：注入 `EmbeddingModel`（旧版叫 `EmbeddingClient`，M6 已改名）
> - artifact：`spring-ai-ollama-spring-boot-starter`（勿用新命名，M6 BOM 不存在）

---

### 任务3.1：配置 Spring AI

**Claude Code 提示词：**
```
参考 doc/spring_ai_guide.md，创建 config/AiConfig.java：

Spring AI 1.0.0-M6 自动配置说明：
- Spring Boot 自动配置根据 application.yaml 创建 ChatModel 和 EmbeddingModel，无需手动声明
- 若需自定义 ChatClient，注入 ChatClient.Builder 而不是直接 @Autowired ChatClient

AiConfig.java 只需要：
1. 注入 ChatClient.Builder，build() 后声明为 @Bean ChatClient
2. 配置 @Bean PgVectorStore（注入 JdbcTemplate 和 EmbeddingModel）
   - initializeSchema = true（自动建 vector_store 表）
   - dimensions = 768（与 bge-base-zh-v1.5 模型输出维度一致）

只生成这一个文件。
```

**验证：**
```bash
mvn spring-boot:run
# 数据库中自动创建 vector_store 表：\dt
psql -d health_diagnosis -c "\dt"
```

---

### 任务3.2：实现知识库服务

**Claude Code 提示词：**
```
创建 service/KnowledgeService.java（不依赖任何自定义表，直接操作 Spring AI PgVectorStore）：

1. importKnowledgeFromResources() -> int
   - 读取 classpath:knowledge.json，反序列化为 List（每项含 title/content/category/source）
   - 转换为 Spring AI Document（content=正文，metadata 存其余字段）
   - 调用 vectorStore.add(documents) 批量写入
   - 返回导入数量

2. searchSimilar(String query, int topK) -> List<Document>
   - 使用 SearchRequest.builder().query(query).topK(topK).similarityThreshold(0.5).build()
   - 返回 vectorStore.similaritySearch(request)

3. @PostConstruct initKnowledge()
   - 用 JdbcTemplate 检查 vector_store 是否为空（SELECT COUNT(*)）
   - 若为空则调用 importKnowledgeFromResources()，避免重复导入

注意：不要创建 MedicalKnowledge 实体或 Mapper。

同步创建 src/main/resources/knowledge.json（含3条以上医学条目，参考 task_breakdown 中的示例）。

只生成 KnowledgeService.java 和 knowledge.json。
```

**knowledge.json 参考内容：**
```json
[
  {
    "title": "感冒的常见症状",
    "content": "感冒通常表现为流鼻涕、咳嗽、发热（一般不超过38.5℃）、乏力等症状。通常在1周内自愈，建议多休息、多饮水。",
    "category": "呼吸系统",
    "source": "WHO"
  },
  {
    "title": "偏头痛的诊断",
    "content": "偏头痛典型表现为单侧搏动性头痛，伴有恶心、呕吐、畏光、畏声等症状，持续4-72小时。可能诱因包括压力、睡眠不足等。",
    "category": "神经系统",
    "source": "中华医学会"
  },
  {
    "title": "发热的处理建议",
    "content": "体温超过38.5℃时建议服用退烧药。多饮水，注意休息。如持续高热超过3天或伴有剧烈头痛、呼吸困难，应立即就医。",
    "category": "症状处理",
    "source": "CDC"
  }
]
```

**验证：**
```bash
# 启动后 @PostConstruct 自动导入，验证：
psql -d health_diagnosis -c "SELECT COUNT(*) FROM vector_store;"
# 应返回 3

# 可选：临时测试接口（POST /api/admin/knowledge/import，排除在拦截器之外）
curl -X POST http://localhost:8080/api/admin/knowledge/import
```

---

### 任务3.3：创建 RagService（同步版本）

**Claude Code 提示词：**
```
创建 service/RagService.java 和 util/PromptBuilder.java：

RagService.chat(String userInput, List<Message> history) -> String
流程：
1. 调用 KnowledgeService.searchSimilar(userInput, 3) 获取相关文档
2. 构造知识上下文字符串（拼接每条 Document 的 title + content）
3. 将 history 最近5条转为对话历史字符串（role: content 格式）
4. 调用 PromptBuilder.build(userInput, knowledgeContext, historyContext) 构造完整 Prompt
5. 调用 chatClient.prompt().user(prompt).call().content() 获取 AI 回复
6. 返回回复字符串

RagService.chatStream(String userInput, List<Message> history) -> Flux<String>
- 步骤1-4 与同步版本相同
- 步骤5 改为：chatClient.prompt().user(prompt).stream().content()
- 返回 Flux<String>

PromptBuilder.build(String userInput, String knowledge, String history) -> String
- 系统角色：你是一名专业的健康顾问助手，根据以下医学知识库内容和对话历史回答用户问题
- 拼接知识上下文和对话历史
- 末尾追加免责声明提示：请在回复末尾注明"免责声明：本内容仅供参考，不构成医疗诊断，如有不适请及时就医"
- 在回复中如检测到高风险症状，请在回复末尾单独一行注明"风险等级：[low/medium/high/urgent]"

注意：RagService 注入 ChatClient（来自 AiConfig 的 @Bean），不要用 ChatClient.Builder 再次 build。

只生成这两个文件。
```

**验证：**
```bash
# 启动后临时在测试中调用 RagService.chat()，观察日志和回复内容
```

---

### 任务3.4：集成 RAG 到 sendMessage 并实现 SSE 流式接口

**Claude Code 提示词：**
```
在已有代码基础上做两处修改：

修改一：ConsultationServiceImpl.sendMessage() 集成 RAG
- 注入 RagService
- 查询该会话 messages 表最近5条记录作为 history
- 将固定回复 "收到您的消息，正在分析中..." 替换为 ragService.chat(content, history)
- 解析 AI 回复中的"风险等级："关键词（正则或 contains），更新 Consultation.riskLevel
  有效值：low / medium / high / urgent；未找到时不更新
- 其余逻辑（权限校验、消息保存）不变

修改二：新增流式接口
在 ConsultationService 接口新增：
  Flux<ServerSentEvent<String>> sendMessageStream(Long consultationId, Long userId, String content)

ConsultationServiceImpl 实现：
1. 权限校验（同 sendMessage）
2. 保存用户消息（同步写数据库）
3. 查询 history（最近5条）
4. 调用 ragService.chatStream(content, history) 获取 Flux<String>
5. 用 StringBuilder 收集完整回复，流结束时（doOnComplete）：
   - 保存 assistantMessage 到 messages 表
   - 解析并更新 riskLevel
6. 将每个 token 包装为 ServerSentEvent.builder().data(token).build()
7. 追加结束事件：data={"type":"done","riskLevel":"xxx"}

ConsultationController 新增：
  POST /api/consultations/{consultationId}/messages/stream
  @RequestHeader userId 统一从 httpRequest.getAttribute("userId") 获取（不要直接解析 JWT）
  produces = MediaType.TEXT_EVENT_STREAM_VALUE
  返回 Flux<ServerSentEvent<String>>

注意：WebFlux 依赖已有，使用 reactor-core 的 Flux 即可；不需要引入额外依赖。

只修改/新增涉及的方法，不动其他代码。
```

**测试：**
```bash
# 同步 RAG 接口
curl -X POST http://localhost:8080/api/consultations/$CUID/messages \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"头痛发热三天了，有点恶心"}'
# 应返回基于医学知识的专业回复，末尾含"风险等级："字样

# SSE 流式接口
curl -X POST http://localhost:8080/api/consultations/$CUID/messages/stream \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"发烧39度怎么办"}' \
  --no-buffer
# 应看到逐 token 的 SSE data 事件，最后一条为 done 事件
```

---

## 阶段4：报告生成

### 任务4.1：生成 Report 实体

**Claude Code 提示词：**
```
生成 entity/Report.java（对应 reports 表）和 mapper/ReportMapper.java。
只生成这两个文件。
```

---

### 任务4.2：实现 PDF 生成服务

**Claude Code 提示词：**
```
使用 iText 7 实现 service/ReportService.java：

1. generateReport(Long consultationId, Long userId) -> ReportVO
   - 校验会话属于该用户
   - 校验会话状态为 completed，否则抛 BusinessException
   - 若报告已存在，直接返回（幂等）
   - 查询 consultations、messages、users 表数据
   - 使用 iText 7 生成 PDF，存储路径：{app.report.base-dir}/{year}/{month}/
   - 文件名：report_{consultationId}_{timestamp}.pdf（存相对路径到 reports 表）
   - 插入 reports 表
   - 返回 ReportVO（reportId, downloadUrl, fileSize, createdAt）

PDF 内容（按顺序）：
- 标题：健康问诊报告
- 报告编号：R{consultationId}
- 用户信息（username，不显示密码）
- 主诉
- 完整对话记录（user/assistant 角色区分）
- 风险评估等级
- 就医建议
- 免责声明：本报告内容仅供参考，不构成医疗诊断意见，如有不适请及时就医

注意：iText 7 中文字符需要加载中文字体（如使用系统中的 simsun.ttf 或 NotoSansCJK 字体）。

只生成 ReportService.java，不生成 Controller。
```

---

### 任务4.3：实现报告接口

**Claude Code 提示词：**
```
参考 doc/api_design.md 第3节，创建 controller/ReportController.java：

1. POST /api/consultations/{consultationId}/report
   - 调用 ReportService.generateReport()
   - 响应中返回 reportId、downloadUrl（格式：/api/reports/{id}/file）、fileSize、createdAt
   - 不返回服务器本地文件路径

2. GET /api/consultations/{consultationId}/report
   - 查询该会话的报告信息
   - 不存在返回 404

3. GET /api/reports/{reportId}/file
   - 校验报告属于当前用户
   - 读取 PDF 文件流返回
   - 设置响应头：
     Content-Type: application/pdf
     Content-Disposition: attachment; filename="report_{reportId}.pdf"

提供测试命令。
```

**测试：**
```bash
# 先结束问诊（报告只能为 completed 状态生成）
curl -X PATCH http://localhost:8080/api/consultations/$CUID/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"completed"}'

# 生成报告
curl -X POST http://localhost:8080/api/consultations/$CUID/report \
  -H "Authorization: Bearer $TOKEN"

# 查询报告信息
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/consultations/$CUID/report

# 下载报告
curl -H "Authorization: Bearer $TOKEN" \
  -o report.pdf \
  http://localhost:8080/api/reports/1/file

open report.pdf  # macOS；Windows 用 start report.pdf
```

---

## 阶段5：测试与优化

### 任务5.1：完整流程集成测试

**测试脚本：**
```bash
#!/bin/bash
BASE_URL="http://localhost:8080"

# 1. 注册
curl -s -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"patient1","password":"123456"}' | jq .

# 2. 登录，提取 token
TOKEN=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"patient1","password":"123456"}' \
  | jq -r '.data.token')
echo "TOKEN: $TOKEN"

# 3. 获取当前用户信息
curl -s -H "Authorization: Bearer $TOKEN" $BASE_URL/api/users/me | jq .

# 4. 创建问诊
CUID=$(curl -s -X POST $BASE_URL/api/consultations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"chiefComplaint":"头痛发热三天"}' \
  | jq -r '.data.consultationId')
echo "CONSULTATION_ID: $CUID"

# 5. 发送消息
curl -s -X POST $BASE_URL/api/consultations/$CUID/messages \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"头痛，还伴有恶心，体温38.8度"}' | jq .

# 6. 再发一条消息
curl -s -X POST $BASE_URL/api/consultations/$CUID/messages \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"头痛是持续性的，主要在前额"}' | jq .

# 7. 查看消息历史
curl -s -H "Authorization: Bearer $TOKEN" \
  $BASE_URL/api/consultations/$CUID/messages | jq .

# 8. 获取问诊列表
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE_URL/api/consultations?page=1&size=10" | jq .

# 9. 结束问诊
curl -s -X PATCH $BASE_URL/api/consultations/$CUID/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"completed"}' | jq .

# 10. 生成报告
curl -s -X POST $BASE_URL/api/consultations/$CUID/report \
  -H "Authorization: Bearer $TOKEN" | jq .

# 11. 下载报告
curl -H "Authorization: Bearer $TOKEN" \
  -o report.pdf \
  $BASE_URL/api/reports/1/file

echo "测试完成，请查看 report.pdf"
```

---

### 任务5.2：错误处理优化

**Claude Code 提示词：**
```
优化 GlobalExceptionHandler.java，补充处理：

1. MethodArgumentNotValidException → 400，提取第一条 field 校验错误信息
2. HttpMessageNotReadableException → 400，提示"请求体格式错误"
3. MissingServletRequestParameterException → 400，提示缺少参数名
4. NoHandlerFoundException → 404
5. DataAccessException（数据库错误）→ 500，只记录日志，不暴露详情

所有异常返回统一 Result 格式，500 级别异常使用 log.error() 记录完整堆栈。
```

---

### 任务5.3：日志优化

**Claude Code 提示词：**
```
在关键位置添加日志（使用 @Slf4j）：

1. RagService.chat()：log.info 记录检索到的知识条数、AI 回复长度
2. RagService.chatStream()：log.info 记录流式请求开始
3. ConsultationService.sendMessage()：log.info 记录用户输入摘要（前50字）和最终风险等级
4. AuthInterceptor：log.debug 记录请求路径和解析到的 userId
5. GlobalExceptionHandler：BusinessException 用 log.warn，其他用 log.error

不要在日志中打印用户密码或完整 Token。
```

---

## 开发检查清单

每完成一个任务：
- [ ] 代码无编译错误（`mvn compile`）
- [ ] 接口测试通过（curl / Postman）
- [ ] 数据正确存入数据库（psql 验证）
- [ ] 日志输出正常，无异常堆栈
- [ ] 接口路径与 `doc/api_design.md` 完全一致

---

## 常见问题排查

### Q1: Spring AI 连接 Ollama 失败
```bash
# 检查 Ollama 是否启动
curl http://localhost:11434/api/tags

# 拉取所需模型
ollama pull qwen3:7b
ollama pull bge-base-zh-v1.5

# 重启 Ollama
ollama serve
```

### Q2: pgvector 检索很慢
```sql
-- 检查 vector_store 表的 HNSW 索引
\d vector_store

-- 手动创建 HNSW 索引（如未自动创建）
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
ON vector_store USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);
-- 注意：使用 HNSW，不用 ivfflat（小数据集 HNSW 效果更好）
```

### Q3: Token 验证失败
```
检查：
1. jwt.secret 是否在 application.yml 中配置（至少32字符）
2. Token 是否过期（有效期 24 小时，由 jwt.expiration 控制）
3. Authorization Header 格式是否为 "Bearer {token}"（注意有空格）
4. 登录接口是否已加入拦截器排除列表（/api/auth/**）
```

### Q4: PDF 中文乱码
```
iText 7 需要加载中文字体，在 ReportService 中添加：
PdfFont font = PdfFontFactory.createFont("path/to/SimSun.ttf",
    PdfEncodings.IDENTITY_H);
Windows 系统字体路径：C:/Windows/Fonts/simsun.ttc,0
```
