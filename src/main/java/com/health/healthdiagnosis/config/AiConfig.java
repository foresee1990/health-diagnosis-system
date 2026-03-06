package com.health.healthdiagnosis.config;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    /**
     * 构建全局 ChatClient Bean。
     * Spring Boot 自动配置提供 ChatClient.Builder（已绑定 application.yaml 中的 Ollama 配置），
     * 此处统一 build 为单例，供 RagService 等直接注入。
     *
     * VectorStore 无需手动定义：spring-ai-pgvector-store-spring-boot-starter 已根据
     * application.yaml（initialize-schema/dimensions/index-type）自动注册 vectorStore Bean。
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
