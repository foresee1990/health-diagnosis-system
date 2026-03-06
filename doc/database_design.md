# 数据库设计文档

## 数据库信息
- 数据库类型：PostgreSQL 16
- 字符集：UTF8
- 扩展：pgvector

## 核心表设计

### 1. users - 用户表
存储用户基本信息和认证数据。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | SERIAL | PRIMARY KEY | 用户ID，主键自增 |
| username | VARCHAR(50) | UNIQUE NOT NULL | 用户名，唯一 |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt加密后的密码 |
| email | VARCHAR(100) | | 邮箱（可选） |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 注册时间 |

**索引：**
- PRIMARY KEY (id)
- UNIQUE INDEX idx_username (username)

---

### 2. consultations - 问诊会话表
记录每次问诊的基本信息。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | SERIAL | PRIMARY KEY | 会话ID |
| user_id | INTEGER | NOT NULL REFERENCES users(id) | 所属用户 |
| status | VARCHAR(20) | DEFAULT 'ongoing' | 状态：ongoing/completed |
| risk_level | VARCHAR(20) | | 风险等级：low/medium/high/urgent |
| chief_complaint | TEXT | | 主诉（用户第一句话） |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| completed_at | TIMESTAMP | | 结束时间 |

**索引：**
- PRIMARY KEY (id)
- INDEX idx_user_consultations (user_id, created_at DESC)

---

### 3. messages - 对话消息表
存储问诊过程中的每条对话。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | SERIAL | PRIMARY KEY | 消息ID |
| consultation_id | INTEGER | NOT NULL REFERENCES consultations(id) | 所属会话 |
| role | VARCHAR(20) | NOT NULL | 角色：user/assistant |
| content | TEXT | NOT NULL | 消息内容 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 发送时间 |

**索引：**
- PRIMARY KEY (id)
- INDEX idx_consultation_messages (consultation_id, created_at)

**注意：** 系统追问也是assistant角色发送，通过内容区分。

---




### 4. reports - 问诊报告表
记录生成的PDF报告元数据。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | SERIAL | PRIMARY KEY | 报告ID |
| consultation_id | INTEGER | UNIQUE NOT NULL REFERENCES consultations(id) | 对应会话（一对一） |
| file_path | VARCHAR(255) | NOT NULL | PDF相对路径|
| file_size | INTEGER | | 文件大小（字节） |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 生成时间 |

其中，PDF相对路径：（相对于 app.report.base-dir 配置）|
  同时在 application.yml 中配置基础目录：
  app:
    report:
      base-dir: ./reports/

**索引：**
- PRIMARY KEY (id)
- UNIQUE INDEX idx_consultation_report (consultation_id)

---

## 表关系图
 users (1) ──────< (N) consultations (1) ──────< (N) messages
                                      (1) ──────< (1) reports


## 初始化SQL脚本
```sql
-- 创建数据库
CREATE DATABASE health_diagnosis;
CREATE EXTENSION IF NOT EXISTS vector;-- 创建表


CREATE TABLE users (
id SERIAL PRIMARY KEY,
username VARCHAR(50) UNIQUE NOT NULL,
password_hash VARCHAR(255) NOT NULL,
email VARCHAR(100),
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE consultations (
id SERIAL PRIMARY KEY,
user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
status VARCHAR(20) DEFAULT 'ongoing'
          CHECK (status IN ('ongoing', 'completed')),
risk_level VARCHAR(20)
          CHECK (risk_level IN ('low', 'medium', 'high', 'urgent') OR risk_level IS NULL),
chief_complaint TEXT,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
completed_at TIMESTAMP
);


CREATE TABLE messages (
id SERIAL PRIMARY KEY,
consultation_id INTEGER NOT NULL REFERENCES consultations(id) ON DELETE CASCADE,
role VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant')),
content TEXT NOT NULL,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);



CREATE TABLE reports (
id SERIAL PRIMARY KEY,
consultation_id INTEGER UNIQUE NOT NULL REFERENCES consultations(id) ON DELETE CASCADE,
file_path VARCHAR(255) NOT NULL,
file_size INTEGER,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- 创建索引
CREATE INDEX idx_user_consultations ON consultations(user_id, created_at DESC);


CREATE INDEX idx_consultation_messages ON messages(consultation_id, created_at);


CREATE INDEX idx_consultation_report ON reports(consultation_id);


CREATE INDEX idx_embedding ON medical_knowledge
  USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);


## 测试数据插入
```sql
-- 插入测试用户
INSERT INTO users (username, password_hash, email)
-- 密码 'password123' 的 BCrypt hash（可通过工具生成）
  VALUES ('testuser', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'test@example.com');

