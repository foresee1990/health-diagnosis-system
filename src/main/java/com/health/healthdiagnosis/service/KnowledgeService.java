package com.health.healthdiagnosis.service;

/*
 * @author WU,Rowan
 * @date 2026/3/6
 */
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 启动时检查 vector_store 是否为空，为空则自动导入默认知识，避免重复导入。
     */
    @PostConstruct
    public void initKnowledge() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vector_store", Integer.class);
            if (count == null || count == 0) {
                int imported = importKnowledgeFromResources();
                log.info("知识库初始化完成，共导入 {} 条", imported);
            } else {
                log.info("知识库已有 {} 条数据，跳过初始化导入", count);
            }
        } catch (Exception e) {
            // 表可能还未创建，先尝试导入
            log.warn("检查知识库失败，尝试直接导入：{}", e.getMessage());
            int imported = importKnowledgeFromResources();
            log.info("知识库初始化完成，共导入 {} 条", imported);
        }
    }

    /**
     * 从 classpath:knowledge.json 读取医学知识，转为 Document 批量写入向量库。
     *
     * @return 导入条数
     */
    public int importKnowledgeFromResources() {
        try {
            ClassPathResource resource = new ClassPathResource("knowledge.json");
            List<KnowledgeItem> items = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<>() {}
            );

            List<Document> documents = items.stream()
                    .map(item -> new Document(
                            item.content(),
                            Map.of(
                                    "title",    item.title(),
                                    "category", item.category() != null ? item.category() : "",
                                    "source",   item.source()   != null ? item.source()   : ""
                            )
                    ))
                    .toList();

            vectorStore.add(documents);
            log.info("成功导入 {} 条医学知识到向量库", documents.size());
            return documents.size();

        } catch (Exception e) {
            log.error("导入知识库失败", e);
            throw new RuntimeException("导入知识库失败: " + e.getMessage(), e);
        }
    }

    /**
     * 向量相似度检索。
     *
     * @param query 检索文本
     * @param topK  返回最相似的前 K 条
     * @return 匹配的 Document 列表
     */
    public List<Document> searchSimilar(String query, int topK) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(0.5)
                        .build()
        );
    }

    private record KnowledgeItem(String title, String content, String category, String source) {}
}
