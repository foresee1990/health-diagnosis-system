package com.health.healthdiagnosis.service;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * @return AI 回复文本
     */
    public String chat(Long consultationId, String userInput) {
        log.info("RAG 同步请求, consultationId={}, 输入摘要={}",
                consultationId, userInput.substring(0, Math.min(50, userInput.length())));

        return chatClient.prompt()
                .advisors(
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .conversationId(consultationId.toString())
                                .build(),
                        new QuestionAnswerAdvisor(vectorStore, SearchRequest.builder()
                                        .topK(5)
                                        .similarityThreshold(0.55)
                                        .build())
                )
                .user(userInput)
                .call()
                .content();
    }

    /**
     * 流式 RAG 问答，返回 token Flux，供 SSE 接口使用。
     *
     * @param consultationId 会话ID
     * @param userInput      用户当前输入
     * @return token 流
     */
    public Flux<String> chatStream(Long consultationId, String userInput) {
        log.info("RAG 流式请求开始, consultationId={}", consultationId);

        return chatClient.prompt()
                .advisors(
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .conversationId(consultationId.toString())
                                .build(),
                        new QuestionAnswerAdvisor(vectorStore, SearchRequest.builder()
                                        .topK(5)
                                        .similarityThreshold(0.55)
                                        .build())
                )
                .user(userInput)
                .stream()
                .content();
    }
}
