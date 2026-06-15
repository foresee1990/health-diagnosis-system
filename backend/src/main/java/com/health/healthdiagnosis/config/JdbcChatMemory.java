package com.health.healthdiagnosis.config;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
/**
 * 基于 PostgreSQL 的持久化 ChatMemory 实现。
 * 替代 InMemoryChatMemory，保证服务重启后 LLM 上下文不丢失。
 *
 * @author WU,Rowan
 * @date 2026/4/12
 */
@Slf4j
@RequiredArgsConstructor
public class JdbcChatMemory implements ChatMemory {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 自动建表（幂等）。由 AiConfig 工厂方法在 Bean 初始化时调用。
     */
    public void initSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS spring_ai_chat_memory (
                    id          BIGSERIAL PRIMARY KEY,
                    conversation_id VARCHAR(255) NOT NULL,
                    role        VARCHAR(20)  NOT NULL,
                    content     TEXT         NOT NULL,
                    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_chat_memory_conv_time
                    ON spring_ai_chat_memory (conversation_id, created_at)
                """);
        log.info("spring_ai_chat_memory 表已就绪");
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        for (Message message : messages) {
            String role;
            if (message instanceof UserMessage) {
                role = "user";
            } else if (message instanceof AssistantMessage) {
                role = "assistant";
            } else {
                role = "system";
            }
            jdbcTemplate.update(
                    "INSERT INTO spring_ai_chat_memory (conversation_id, role, content) VALUES (?, ?, ?)",
                    conversationId, role, message.getText()
            );
        }
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
       // 取最近 lastN 条（内层倒序），再按时间正序排列还原对话顺序（外层正序）

        String sql = """
                SELECT role, content FROM (
                    SELECT role, content, created_at
                    FROM spring_ai_chat_memory
                    WHERE conversation_id = ?
                    ORDER BY created_at DESC
                    LIMIT ?
                ) sub ORDER BY created_at ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String role = rs.getString("role");
            String content = rs.getString("content");
            return "user".equals(role) ? new UserMessage(content) : new AssistantMessage(content);
        }, conversationId, lastN);
    }

    @Override
    public void clear(String conversationId) {
        jdbcTemplate.update(
                "DELETE FROM spring_ai_chat_memory WHERE conversation_id = ?",
                conversationId
        );
    }
}
