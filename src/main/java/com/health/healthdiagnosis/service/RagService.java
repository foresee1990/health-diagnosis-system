package com.health.healthdiagnosis.service;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.health.healthdiagnosis.config.AiConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ChatMemory chatMemory;

    /**
     * 同步 RAG 问答。
     * Advisor 流水线：MessageChatMemoryAdvisor（LLM上下文） → QuestionAnswerAdvisor（向量检索+注入）
     *
     * @param consultationId 会话ID，用作 ChatMemory 的 conversationId
     * @param userInput      用户当前输入
     * @param patientContext 患者健康档案上下文（可为 null，null 时使用默认系统提示词）
     * @return AI 回复文本
     */
    public String chat(Long consultationId, String userInput, String patientContext) {
        log.info("RAG 同步请求, consultationId={}, 输入摘要={}, 有患者档案={}",
                consultationId, userInput.substring(0, Math.min(50, userInput.length())),
                patientContext != null);

        return chatClient.prompt()
                .system(buildSystemPrompt(patientContext))
                .advisors(
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .conversationId(consultationId.toString())
                                .build(),
                        new QuestionAnswerAdvisor(vectorStore, SearchRequest.builder()
                                        .topK(5)
                                        .similarityThreshold(0.55)
                                        .build())
                )
                .user("/think\n" + userInput)
                .call()
                .content();
    }

    /**
     * 流式 RAG 问答，返回 token Flux，供 SSE 接口使用。
     *
     * @param consultationId 会话ID
     * @param userInput      用户当前输入
     * @param patientContext 患者健康档案上下文（可为 null）
     * @return token 流
     */
    public Flux<String> chatStream(Long consultationId, String userInput, String patientContext) {
        log.info("RAG 流式请求开始, consultationId={}, 有患者档案={}", consultationId, patientContext != null);

        return chatClient.prompt()
                .system(buildSystemPrompt(patientContext))
                .advisors(
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .conversationId(consultationId.toString())
                                .build(),
                        new QuestionAnswerAdvisor(vectorStore, SearchRequest.builder()
                                        .topK(5)
                                        .similarityThreshold(0.55)
                                        .build())
                )
                .user("/think\n" + userInput)
                .stream()
                .content();
    }

    /**
     * 构建最终系统提示词：基础提示词 + 患者档案上下文（如有）。
     */
    private String buildSystemPrompt(String patientContext) {
        if (patientContext == null) {
            return AiConfig.BASE_SYSTEM_PROMPT;
        }
        return AiConfig.BASE_SYSTEM_PROMPT + "\n" + patientContext;
    }
}
