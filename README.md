# 项目背景

## 项目名称
基于大模型的健康问诊与辅助诊断系统

## 项目目标
构建一个面向普通用户的在线健康问诊系统，通过本地部署的大模型和RAG技术，
提供智能问诊对话、症状分析、初步诊断建议和就医指导。

## 核心功能
1. 用户注册登录
2. 多轮问诊对话（用户描述症状→系统追问→获取完整信息）
3. 基于RAG检索医学知识，调用Qwen生成诊断建议
4. 生成问诊报告（PDF）
5. 历史问诊记录查询
6. 医学知识库管理（PDF上传、分块配置、向量化重建、切分可视化）
7. 管理员功能（用户管理、角色分配、系统日志）
8. 风险评估与预警（low/medium/high/urgent四级，urgent强制提示120）

## 技术栈
- 后端：Spring Boot 3.4.3 + Spring WebFlux（SSE流式响应）+ springAi 1.0.0-M6 + MyBatis-Plus 3.5.10.1
- 数据库：PostgreSQL 16 + pgvector 0.8.2
- AI：Ollama + Qwen3-4B（对话生成）+ bge-base-zh-v1.5（embedding向量化，维度768，chunk_size=400-600字符，overlap≈15%，Top-K=4，余弦相似度检索）
- PDF生成：iText 7
- 前端：Vue 3 + Element Plus
- 认证：采用JWT无状态认证，服务器不存储Token，通过验证签名和过期时间完成校验，同时用HashMap维护Token与会话的映射关系

## 系统边界（不做什么）
-  不做药物推荐
-  不做医生端功能
-  不做实时监测
-  不做移动端（只做Web）
-  不做确定性诊断（所有建议仅供参考）
## 免责声明设计
- 系统所有诊断建议仅供参考，不构成医疗诊断意见
- 每次对话结果页和PDF报告均需展示免责声明
- 严重症状（如胸痛、意识障碍等）系统应主动提示立即就医


## 目标用户
普通患者（无医学背景）

## 核心架构
AI推理层采用Spring AI Advisor链式调用：ChatClient → MessageChatMemoryAdvisor（多轮上下文管理）+ QuestionAnswerAdvisor（RAG检索注入）→ Ollama(Qwen3-4B)