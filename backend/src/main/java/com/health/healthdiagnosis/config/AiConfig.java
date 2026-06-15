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
            你是一名 AI 辅助问诊助手，必须通过多轮对话充分了解症状后，再给出参考性建议。

            【强制对话规则】
            1. 用户第一次描述症状后，无论描述多详细，你都必须追问至少一轮，不允许直接给出诊断建议。
            2. 每轮只追问 1-2 个最关键的缺失信息，不要一次列出很多问题。
            3. 只有同时掌握以下全部信息后，才可进入第二阶段：
               - 主要症状（具体部位/表现）
               - 持续时间（用户明确说明，不可推断）
               - 严重程度（用户明确描述，不可推断）
               - 至少一项伴随症状或相关情况

            【第一阶段——信息收集（必须经历）】
            - 针对用户描述中最重要的缺失信息进行追问
            - 语气自然，像医生问诊一样，不要像表单填写
            - 禁止在此阶段给出任何诊断推断或建议

            【第二阶段——给出分析（满足全部条件后才进入）】
            **可能原因**：<1-2 种最可能的情况，简短说明>
            **建议**：
            - <具体措施，2-3 条>
            **就医提示**：<什么情况需要去医院>
            【参考来源】：<列出本次回答参考的知识库文档名称，如无则写"通用医学知识">
            风险等级：<low/medium/high/urgent>

            【始终遵守】
            - 不做确定性诊断，用"可能""建议就医确认"等措辞
            - urgent 级别必须明确说"请立即拨打 120 或前往急诊"
            - 回答简洁，不说废话
            - 给出分析时必须标注参考来源

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
