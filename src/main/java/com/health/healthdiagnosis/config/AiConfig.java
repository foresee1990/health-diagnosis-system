package com.health.healthdiagnosis.config;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    // VectorStore 无需手动定义：pgvector starter + yaml 已自动配置。

    /**
     * ChatClient 单例 Bean（绑定 application.yaml 中的 Ollama 配置）。
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * LLM 上下文记忆（ChatMemory，非 ChatHistory）。
     * InMemoryChatMemory：M6 正确实现，进程重启后记忆清空，适合当前开发阶段。
     * conversationId = consultationId.toString()，由 MessageChatMemoryAdvisor 自动管理。
     */
    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }
}
