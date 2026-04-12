package com.health.healthdiagnosis.config;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AiConfig {

    // VectorStore 无需手动定义：pgvector starter + yaml 已自动配置。

    /**
     * 基础系统提示词，暴露为常量供 RagService 在注入患者上下文时拼接使用。
     */
    public static final String BASE_SYSTEM_PROMPT = """
            请不要输出思考过程，直接给出结论。
            你是一名专业的 AI 辅助问诊助手，请严格遵守以下规则：

            【内容规则】
            - 只能基于系统提供的【参考知识】回答；若知识库中无相关内容，在"分析"部分说明"暂无相关医学资料，建议就医"
            - 不做明确诊断结论，始终建议用户就医确认
            - urgent 级别必须明确建议"立即拨打120或前往急诊"

            【回答格式】（严格按此结构，总字数控制在200字以内）
            **分析**：<根据症状简要说明可能原因，1-2句>
            **建议**：
            - <具体处理或就医建议，2-3条>
            风险等级：<low/medium/high/urgent>

            【风险等级定义】
            - low：症状轻微，可自行观察
            - medium：需尽快就医（24-48小时内）
            - high：需当天就医
            - urgent：需立即急诊
            """;

    /**
     * ChatClient 单例 Bean（绑定 application.yaml 中的 Ollama 配置）。
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(BASE_SYSTEM_PROMPT)
                .build();
    }

    /**
     * LLM 上下文记忆（ChatMemory，非 ChatHistory）。
     * JdbcChatMemory：持久化到 PostgreSQL，服务重启后对话记忆不丢失。
     * conversationId = consultationId.toString()，由 MessageChatMemoryAdvisor 自动管理。
     */
    @Bean
    public ChatMemory chatMemory(JdbcTemplate jdbcTemplate) {
        JdbcChatMemory memory = new JdbcChatMemory(jdbcTemplate);
        memory.initSchema();
        return memory;
    }
}
