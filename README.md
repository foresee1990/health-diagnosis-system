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

## 技术栈
- 后端：Spring Boot 3.4.3 + springAi 1.0.0-M6 + MyBatis-Plus 3.5.10.1
- 数据库：PostgreSQL 16 + pgvector 0.8.2
- AI：Ollama + Qwen3-7B（对话生成）+ bge-base-zh-v1.5（embedding向量化）
- PDF生成：iText 8 
- 前端：Vue 3 + Element Plus
- 认证：JWT 无状态认证,只用 JWT 进行身份认证。服务器不存储 Token，每次请求通过验证 JWT 签名和过期时间完成身份校验。

## 系统边界（不做什么）
-  不做药物推荐
-  不做医生端功能
-  不做实时监测
-  不做移动端（只做Web）
## 免责声明设计
- 系统所有诊断建议仅供参考，不构成医疗诊断意见
- 每次对话结果页和PDF报告均需展示免责声明
- 严重症状（如胸痛、意识障碍等）系统应主动提示立即就医


## 目标用户
普通患者（无医学背景）

## project rule
Do NOT implement PromptBuilder or manual prompt concatenation.

Use Spring AI RAG pipeline only:

SpringBoot → ChatClient → QuestionAnswerAdvisor → VectorStore(pgvector) → Ollama

Advisor already handles:
- vector search
- context injection
- prompt construction

Avoid reinventing the wheel in all future tasks.
