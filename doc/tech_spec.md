# 技术规范

## 项目结构
```
health-diagnosis-system/
├── docs/                                    # 文档目录
│   ├── project_context.md                   # 项目背景（已有）
│   ├── tech_spec.md                        # 技术规范（已有）
│   ├── database_design.md                  # 数据库设计（已有）
│   ├── api_design.md                       # API设计（下面给出）
│   ├── spring_ai_guide.md                  # Spring AI使用指南（下面给出）
│   └── task_breakdown.md                   # 任务分解（下面给出）
│
├── src/main/java/com/health/healthdiagnosis/
│   ├── HealthDiagnosisSystemApplication.java     # 启动类
│   │
│   ├── config/                             # 配置类
│   │   ├── AiConfig.java                   # Spring AI配置（Ollama、Embedding、VectorStore）
│   │   ├── WebConfig.java                  # Web配置（拦截器、CORS）
│   │   └── SecurityConfig.java             # 安全配置（可选）
│   │
│   ├── controller/                         # 控制器层
│   │   ├── AuthController.java             # 认证接口（注册、登录）
│   │   ├── ConsultationController.java     # 问诊接口
│   │   └── ReportController.java           # 报告接口
│   │
│   ├── service/                            # 业务层
│   │   ├── AuthService.java                # 认证业务
│   │   ├── ConsultationService.java        # 问诊业务（核心）
│   │   ├── RagService.java                 # RAG服务（核心，Spring AI集成）
│   │   ├── KnowledgeService.java           # 知识库管理
│   │   └── ReportService.java              # 报告生成
│   │
│   ├── mapper/                             # MyBatis-Plus Mapper
│   │   ├── UserMapper.java
│   │   ├── ConsultationMapper.java
│   │   ├── MessageMapper.java
│   │   ├── MedicalKnowledgeMapper.java
│   │   └── ReportMapper.java
│   │
│   ├── entity/                             # 实体类
│   │   ├── User.java
│   │   ├── Consultation.java
│   │   ├── Message.java
│   │   ├── MedicalKnowledge.java
│   │   └── Report.java
│   │
│   ├── dto/                                # 数据传输对象
│   │   ├── request/
│   │   │   ├── RegisterRequest.java
│   │   │   ├── LoginRequest.java
│   │   │   ├── CreateConsultationRequest.java
│   │   │   └── SendMessageRequest.java
│   │   └── response/
│   │       ├── LoginResponse.java
│   │       ├── ConsultationResponse.java
│   │       └── MessageResponse.java
│   │
│   ├── interceptor/                        # 拦截器
│   │   └── AuthInterceptor.java            # Token认证拦截器
│   │
│   ├── common/
│   │   ├── Result.java                     # 统一响应封装
│   │   ├── ErrorCode.java                  # 统一错误码常量

│   ├── util/                               # 工具类
│   │   ├── JwtUtil.java                    # JWT工具
│   │   └── PromptBuilder.java              # Prompt构造器
│   │
│   └── exception/                          # 异常处理
│       ├── GlobalExceptionHandler.java     # 全局异常处理
│       └── BusinessException.java          # 业务异常
│
├── src/main/resources/
│   ├── application.yml                     # 主配置文件
│   ├── application-dev.yml                 # 开发环境配置
│   ├── mapper/                             # MyBatis XML映射文件（如果需要）
│   └── static/                             # 静态资源
│
├── src/test/java/com/health/              # 测试代码
│   ├── service/
│   │   ├── RagServiceTest.java
│   │   └── ConsultationServiceTest.java
│   └── controller/
│       └── ConsultationControllerTest.java
│
├── scripts/                                # 脚本目录
│   ├── init_database.sql                   # 数据库初始化脚本
│
├── reports/                                # 生成的报告存储目录（运行时创建）
│
├── pom.xml                                 # Maven配置
└── README.md                               # 项目说明
```

## 统一响应格式
所有Controller返回统一的Result对象：
```java
public class Result<T> {
      private int code;
      private String message;
      private T data;
      private long timestamp = System.currentTimeMillis();

      // 静态工厂方法
      public static <T> Result<T> success(T data) { ... }
      public static <T> Result<T> error(int code, String message) { ... }
  }

```

## 依赖版本
- Spring Boot:3.4.3
- 要用上Spring AI框架 1.0.0-M6
- JDK: 17
- MyBatis-Plus: 3.5.10.1
- PostgreSQL Driver: 42.x
- JWT: jjwt 0.12.x

## 命名规范
- 类名：大驼峰（UserController）
- 方法名：小驼峰（getUserById）
- 常量：全大写下划线（MAX_RETRY_COUNT）
- 数据库表名：小写下划线（user_info）


## code required
- 如果可以并且需要的话，规范使用@RequiredArgsConstructor注解等