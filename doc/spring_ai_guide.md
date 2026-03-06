# Spring AI 使用指南（适配 Spring AI 1.0.0-M6）

## 1. 依赖配置

### pom.xml 完整依赖
```xml
<properties>
    <java.version>17</java.version>
    <spring-boot.version>3.4.3</spring-boot.version>
    <spring-ai.version>1.0.0-M6</spring-ai.version>
</properties>

<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Spring AI Ollama（M6 新命名规范） -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-ollama</artifactId>
    </dependency>

    <!-- Spring AI PgVector Store（M6 新命名规范） -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-vector-store-pgvector</artifactId>
    </dependency>

    <!-- MyBatis-Plus（Spring Boot 3.x 专用 starter） -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.10.1</version>
    </dependency>

    <!-- PostgreSQL 驱动 -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- BCrypt 加密（仅引入 spring-security-crypto，不启用完整安全过滤链） -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-crypto</artifactId>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.3</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>

    <!-- iText 7 PDF 生成（注意：AGPL 许可证，毕业设计可用） -->
    <dependency>
        <groupId>com.itextpdf</groupId>
        <artifactId>itext7-core</artifactId>
        <version>7.2.5</version>
        <type>pom</type>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>

<!-- Spring AI Milestone 仓库（M6 不在 Maven Central，必须配置） -->
<repositories>
    <repository>
        <id>spring-milestones</id>
        <name>Spring Milestones</name>
        <url>https://repo.spring.io/milestone</url>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
</repositories>
```

> **重要说明：**
> - `mybatis-plus-spring-boot3-starter` 是 Spring Boot 3.x 专用版本，不能用 `mybatis-plus-boot-starter`（那是 Boot 2.x 版本，会导致自动配置失败）
> - `spring-security-crypto` 仅引入 BCrypt 工具类，不会激活 Spring Security 的认证过滤链

---

## 2. 配置文件

### application.yml
```yaml
spring:
  application:
    name: health-diagnosis-system

  # 数据源配置
  datasource:
    url: jdbc:postgresql://localhost:5432/health_diagnosis
    username: postgres
    password: yourpassword
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5

  # Spring AI 配置
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: qwen3:7b
          temperature: 0.7
          num-ctx: 4096
      embedding:
        options:
          model: bge-base-zh-v1.5
    vectorstore:
      pgvector:
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 768
        initialize-schema: true  # 启动时自动创建 vector_store 表

  # 文件上传限制（注意：不能有第二个 spring: 根键）
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

# MyBatis-Plus 配置
mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.health.healthdiagnosis.entity   # 与实际包路径一致
  configuration:
    map-underscore-to-camel-case: true
    # log-impl 仅在 dev 环境启用，见 application-dev.yml

# 服务器配置
server:
  port: 8080
  tomcat:
    connection-timeout: 60s  # 为 AI 同步接口预留足够超时时间

# JWT 配置
jwt:
  secret: your-256-bit-secret-key-here-must-be-at-least-32-characters
  expiration: 86400  # 24 小时（秒）

# 应用自定义配置
app:
  report:
    base-dir: ./reports/  # PDF 文件存储根目录（相对路径，启动目录为基准）
```

### application-dev.yml（开发环境额外配置）
```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 开发时打印 SQL

logging:
  level:
    com.health.healthdiagnosis: DEBUG
```

> **YAML 注意事项：**
> YAML 文件中同一层级的 key 不能重复出现（如两个 `spring:`），后者会覆盖前者导致配置丢失。
> 所有 `spring.*` 配置必须合并在同一个 `spring:` 根块下。

---

## 3. Spring AI 核心组件使用（M6 API）

### 3.1 ChatClient（对话生成）

> **M6 重大变更：** `ChatClient` 在 M6 中变为 fluent builder API，不能直接 `chatClient.call(prompt)`。
> 推荐通过注入 `ChatClient.Builder` 来构建，或直接注入 `ChatModel`。

```java
@Service
@Slf4j
public class RagService {

    private final ChatClient chatClient;

    // 推荐：注入 ChatClient.Builder（Spring Boot 自动配置提供）
    public RagService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 同步调用（适合简单场景）
     */
    public String generateResponse(String userInput) {
        return chatClient.prompt()
                .user(userInput)
                .call()
                .content();
    }

    /**
     * 带运行时选项的调用
     */
    public String generateWithOptions(String userInput) {
        return chatClient.prompt()
                .user(userInput)
                .options(OllamaOptions.builder()
                        .model("qwen3:7b")
                        .temperature(0.7)    // M6 已去掉 with 前缀
                        .topP(0.9)
                        .build())
                .call()
                .content();
    }

    /**
     * 流式输出（SSE 接口使用）
     * 返回 Flux<String>，配合 @RequestMapping produces = text/event-stream 使用
     */
    public Flux<String> streamResponse(String userInput) {
        return chatClient.prompt()
                .user(userInput)
                .stream()
                .content();
    }
}
```

---

### 3.2 EmbeddingModel（文本向量化）

> **M6 重大变更：** `EmbeddingClient` 已更名为 `EmbeddingModel`。

```java
@Service
public class KnowledgeService {

    @Autowired
    private EmbeddingModel embeddingModel;  // 注意：不是 EmbeddingClient

    /**
     * 单条文本向量化
     */
    public float[] encode(String text) {
        return embeddingModel.embed(text);
    }

    /**
     * 批量向量化
     */
    public List<float[]> encodeBatch(List<String> texts) {
        EmbeddingResponse response = embeddingModel.embedForResponse(texts);
        return response.getResults().stream()
                .map(embedding -> embedding.getOutput())
                .toList();
    }
}
```

---

### 3.3 VectorStore（向量存储与检索）

> **重要说明：** Spring AI 的 `PgVectorStore`（配合 `initialize-schema: true`）会自动创建名为
> `vector_store` 的表（字段：`id UUID, content TEXT, metadata JSONB, embedding vector(768)`），
> 与 `database_design.md` 中的 `medical_knowledge` 表**相互独立**。
>
> **集成策略：**
> - `medical_knowledge` 表：存储原始知识数据（title、category、source），便于管理和展示
> - `vector_store` 表：Spring AI 管理的向量索引，用于相似度检索
> - 导入时，从 `medical_knowledge` 读数据 → 转为 `Document` → 写入 `vector_store`

```java
@Service
@Slf4j
public class KnowledgeService {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private MedicalKnowledgeMapper knowledgeMapper;

    /**
     * 批量导入医学知识到向量库
     * 将 medical_knowledge 表中的数据向量化后存入 Spring AI 的 vector_store 表
     */
    public int importKnowledge(List<MedicalKnowledge> knowledgeList) {
        List<Document> documents = knowledgeList.stream()
                .map(k -> new Document(
                        k.getContent(),
                        Map.of(
                                "id",       String.valueOf(k.getId()),
                                "title",    k.getTitle(),
                                "category", k.getCategory() != null ? k.getCategory() : "",
                                "source",   k.getSource() != null ? k.getSource() : ""
                        )
                ))
                .toList();

        // Spring AI 自动向量化并存入 vector_store 表
        vectorStore.add(documents);
        log.info("成功导入 {} 条医学知识到向量库", documents.size());
        return documents.size();
    }

    /**
     * 相似度检索（M6 SearchRequest Builder 风格）
     */
    public List<Document> searchSimilar(String query, int topK) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(0.5)
                        .build()
        );
        log.info("检索到 {} 条相关知识", results.size());
        return results;
    }
}
```

---

## 4. 完整 RAG 流程示例

```java
@Service
@Slf4j
public class RagService {

    private final ChatClient chatClient;

    @Autowired
    private VectorStore vectorStore;

    public RagService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * RAG 完整流程：检索 + 生成（同步版本）
     *
     * @param userInput           用户当前输入
     * @param conversationHistory 历史消息（从 messages 表查询）
     * @return AI 回复文本
     */
    public String chat(String userInput, List<Message> conversationHistory) {

        // 1. 向量检索相关知识（Spring AI 自动向量化 userInput）
        List<Document> similarDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userInput)
                        .topK(3)
                        .similarityThreshold(0.6)
                        .build()
        );

        if (similarDocs.isEmpty()) {
            log.warn("未检索到相关医学知识，将仅基于对话历史回复");
        }

        // 2. 构造知识上下文
        String knowledgeContext = similarDocs.stream()
                .map(doc -> {
                    String title = doc.getMetadata().getOrDefault("title", "").toString();
                    return title + ":\n" + doc.getText();
                })
                .collect(Collectors.joining("\n\n"));

        // 3. 构造对话历史（取最近 N 条）
        String historyContext = conversationHistory.stream()
                .map(msg -> msg.getRole() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));

        // 4. 构造完整 Prompt
        String prompt = PromptBuilder.build(knowledgeContext, historyContext, userInput);

        // 5. 调用大模型生成回复
        String aiReply = chatClient.prompt()
                .user(prompt)
                .options(OllamaOptions.builder()
                        .model("qwen3:7b")
                        .temperature(0.7)
                        .build())
                .call()
                .content();

        log.info("AI 回复生成完成，长度: {}", aiReply.length());
        return aiReply;
    }

    /**
     * RAG 流式版本（SSE 接口使用）
     *
     * @return Flux<String> token 流
     */
    public Flux<String> chatStream(String userInput, List<Message> conversationHistory) {

        List<Document> similarDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userInput)
                        .topK(3)
                        .similarityThreshold(0.6)
                        .build()
        );

        String knowledgeContext = similarDocs.stream()
                .map(doc -> doc.getMetadata().getOrDefault("title", "") + ":\n" + doc.getText())
                .collect(Collectors.joining("\n\n"));

        String historyContext = conversationHistory.stream()
                .map(msg -> msg.getRole() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));

        String prompt = PromptBuilder.build(knowledgeContext, historyContext, userInput);

        return chatClient.prompt()
                .user(prompt)
                .options(OllamaOptions.builder()
                        .model("qwen3:7b")
                        .temperature(0.7)
                        .build())
                .stream()
                .content();
    }
}
```

---

## 5. Prompt 构造最佳实践

```java
@Component
public class PromptBuilder {

    public static String build(String knowledge, String history, String userInput) {
        return """
                你是一个专业的健康咨询助手，基于以下医学知识回答患者问题。

                【医学知识参考】
                %s

                【对话历史】
                %s

                【患者当前问题】
                %s

                【回答要求】
                1. 基于提供的医学知识进行分析，不要编造不在知识库中的信息
                2. 如果信息不足以判断，主动追问关键信息（症状部位、持续时间、严重程度）
                3. 在回复末尾单独一行给出风险评估，格式严格为：风险等级：low/medium/high/urgent
                4. 给出就医建议（是否需要就医、建议挂什么科室）
                5. 使用通俗易懂的语言，避免过多医学术语

                【免责声明提示】
                如给出任何诊断建议，必须在末尾注明：
                "以上内容仅供参考，不构成医疗诊断意见，如有不适请及时就医。"

                请回答：
                """.formatted(
                knowledge.isBlank() ? "（暂无匹配的医学知识，请基于通用医学常识谨慎回答）" : knowledge,
                history.isBlank() ? "（首次对话）" : history,
                userInput
        );
    }
}
```

---

## 6. SSE 流式接口实现示例

```java
@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    @Autowired
    private ConsultationService consultationService;

    /**
     * 发送消息 - SSE 流式版本
     * 前端使用 EventSource 或 fetch + ReadableStream 接收
     */
    @PostMapping(value = "/{consultationId}/messages/stream",
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> sendMessageStream(
            @PathVariable Long consultationId,
            @RequestBody @Valid SendMessageRequest request,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");
        return consultationService.sendMessageStream(consultationId, userId, request.getContent());
    }
}
```

---

## 7. 常见问题排查

### Q1: Ollama 服务连接失败
```
错误：Connection refused: localhost:11434
解决：
1. 启动 Ollama：ollama serve
2. 检查已下载模型：ollama list
3. 验证端口：curl http://localhost:11434/api/tags
4. 拉取模型：ollama pull qwen3:7b && ollama pull bge-base-zh-v1.5
```

### Q2: pgvector 检索很慢或索引不生效
```
解决：
1. 确认 pgvector 扩展已安装：SELECT * FROM pg_extension WHERE extname = 'vector';
2. 确认 HNSW 索引已创建（Spring AI 的 vector_store 表）：
   \d vector_store
3. 如需手动重建索引：
   DROP INDEX IF EXISTS vector_store_embedding_idx;
   CREATE INDEX vector_store_embedding_idx ON vector_store
   USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
   （注意：HNSW 比 ivfflat 更适合小数据集，无需预先填充数据）
```

### Q3: 向量维度不匹配
```
错误：vector dimension mismatch (expected 768, got xxx)
解决：
1. 确认 embedding 模型输出维度（bge-base-zh-v1.5 是 768 维）
2. 确认 application.yml 中 dimensions: 768
3. 确认 database_design.md 中 vector(768) 与模型维度一致
4. 如果更换了 embedding 模型，需要删除 vector_store 表重新建表并重新导入
```

### Q4: ChatClient.call() 报错 NoSuchMethodError
```
原因：Spring AI M6 中 ChatClient 已改为 fluent builder API，
      不能再用 chatClient.call(prompt) 方式调用。
解决：
  旧（M3）：chatClient.call(prompt)
  新（M6）：chatClient.prompt().user(prompt).call().content()
```

### Q5: 依赖下载失败（Could not find artifact）
```
原因：Spring AI M6 不在 Maven Central，需要 Spring Milestone 仓库。
解决：确认 pom.xml 中已添加 Spring Milestones 仓库配置（见第1节）。
```
