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
| role | VARCHAR(20) | NOT NULL DEFAULT 'USER' | 角色：USER / ADMIN |
| status | VARCHAR(20) | NOT NULL DEFAULT 'ACTIVE' | 状态：ACTIVE / BANNED |
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

**隐私说明：** `messages` 表（问诊对话内容）属于用户隐私数据，**管理员不可见**。管理员仅可查看 `consultations` 表的元数据（时间、状态、风险等级），不可访问具体对话内容。

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

### 5. system_logs - 系统操作日志表
记录管理员的操作行为，用于审计和追溯。

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | SERIAL | PRIMARY KEY | 日志ID |
| operator_id | INTEGER | NOT NULL REFERENCES users(id) | 操作人ID（管理员） |
| action | VARCHAR(50) | NOT NULL | 操作类型：BAN_USER / UNBAN_USER / RESET_PASSWORD 等 |
| target_user_id | INTEGER | REFERENCES users(id) | 目标用户ID（可为空，如查询列表时） |
| details | TEXT | | 操作详情（JSON格式，如变更前后的值） |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 操作时间 |

**索引：**
- PRIMARY KEY (id)
- INDEX idx_operator_logs (operator_id, created_at DESC)
- INDEX idx_target_user_logs (target_user_id)

---

## 表关系图
 users (1) ──────< (N) consultations (1) ──────< (N) messages
                                      (1) ──────< (1) reports
 users (1) ──────< (N) system_logs (operator)
 users (1) ──────< (N) system_logs (target_user)


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
role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'BANNED')),
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


CREATE TABLE system_logs (
id SERIAL PRIMARY KEY,
operator_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
action VARCHAR(50) NOT NULL,
target_user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
details TEXT,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_operator_logs ON system_logs(operator_id, created_at DESC);
CREATE INDEX idx_target_user_logs ON system_logs(target_user_id);

-- 迁移现有数据库（已存在 users 表时执行）
-- ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN'));
-- ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'BANNED'));


CREATE INDEX idx_embedding ON medical_knowledge
  USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);


## 测试数据插入
```sql
-- 插入测试用户
INSERT INTO users (username, password_hash, email)
-- 密码 'password123' 的 BCrypt hash（可通过工具生成）
  VALUES ('testuser', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'test@example.com');

