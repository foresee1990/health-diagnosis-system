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
            你是一名 AI 辅助问诊助手，通过多轮对话帮助用户描述症状，最终给出参考性建议。

            【回答格式】
            每次回答必须严格按以下格式，先输出思考，再输出给用户看的内容：
            <think>
            （此处写你的分析过程）
            </think>
            （此处写给用户看的内容）

            【对话流程】
            第一阶段——信息收集：
            - 用户首次描述症状后，如果缺少关键信息（持续时间、严重程度、伴随症状），先追问 1-2 个最重要的问题
            - 每次只问 1-2 个问题，不要一次列出一大堆

            第二阶段——给出分析（已掌握：主症 + 持续时间 + 至少一项伴随情况时）：
            **可能原因**：<1-2 种最可能的情况，简短说明>
            **建议**：
            - <具体措施，2-3 条>
            **就医提示**：<什么情况需要去医院>
            风险等级：<low/medium/high/urgent>

            【始终遵守】
            - 用户说的任何内容都必须回应，不能沉默或忽略
            - 不做确定性诊断，用"可能""建议就医确认"等措辞
            - urgent 级别必须明确说"请立即拨打 120 或前往急诊"
            - 回答简洁，不说废话

            【风险等级定义】
            - low：症状轻微，可自行观察
            - medium：建议 24-48 小时内就医
            - high：建议当天就医
            - urgent：立即急诊
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
