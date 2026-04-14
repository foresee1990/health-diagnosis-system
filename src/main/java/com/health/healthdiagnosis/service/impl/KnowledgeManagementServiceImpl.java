package com.health.healthdiagnosis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.health.healthdiagnosis.common.ErrorCode;
import com.health.healthdiagnosis.dto.request.KnowledgeConfigRequest;
import com.health.healthdiagnosis.dto.request.KnowledgeSearchRequest;
import com.health.healthdiagnosis.dto.response.KnowledgeChunkResponse;
import com.health.healthdiagnosis.dto.response.KnowledgeConfigResponse;
import com.health.healthdiagnosis.dto.response.KnowledgeDocumentResponse;
import com.health.healthdiagnosis.dto.response.KnowledgeSearchResult;
import com.health.healthdiagnosis.entity.KnowledgeConfig;
import com.health.healthdiagnosis.entity.KnowledgeDocument;
import com.health.healthdiagnosis.entity.User;
import com.health.healthdiagnosis.exception.BusinessException;
import com.health.healthdiagnosis.mapper.KnowledgeConfigMapper;
import com.health.healthdiagnosis.mapper.KnowledgeDocumentMapper;
import com.health.healthdiagnosis.mapper.UserMapper;
import com.health.healthdiagnosis.service.KnowledgeManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeManagementServiceImpl implements KnowledgeManagementService {

    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeConfigMapper configMapper;
    private final UserMapper userMapper;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.knowledge.upload-dir}")
    private String uploadDir;

    // ==================== 上传与处理 ====================

    @Override
    public KnowledgeDocumentResponse uploadAndProcess(MultipartFile file, String tags, Long uploaderId) {
        ensureUploadDir();

        // 1. 生成唯一存储文件名，保存到磁盘
        String storedName = UUID.randomUUID() + ".pdf";
        Path filePath = Paths.get(uploadDir, storedName);
        try {
            Files.write(filePath, file.getBytes());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "文件保存失败：" + e.getMessage());
        }

        // 2. 插入 DB 记录（状态 PENDING）
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setOriginalName(file.getOriginalFilename());
        doc.setStoredName(storedName);
        doc.setFilePath(filePath.toAbsolutePath().toString());
        doc.setFileSize(file.getSize());
        doc.setTags(tags != null ? tags.trim() : "");
        doc.setStatus("PENDING");
        doc.setChunkCount(0);
        doc.setUploadedBy(uploaderId);
        doc.setCreatedAt(LocalDateTime.now());
        documentMapper.insert(doc);

        // 3. 处理 PDF（提取文本 → 分块 → Embedding）
        try {
            int chunkCount = processDocument(doc);
            doc.setStatus("DONE");
            doc.setChunkCount(chunkCount);
            doc.setProcessedAt(LocalDateTime.now());
        } catch (Exception e) {
            doc.setStatus("FAILED");
            log.error("PDF处理失败：docId={}, file={}", doc.getId(), file.getOriginalFilename(), e);
        }
        documentMapper.updateById(doc);

        return toResponse(doc);
    }

    // ==================== 查询 ====================

    @Override
    public Map<String, Object> listDocuments(int page, int size) {
        Page<KnowledgeDocument> pageResult = documentMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<KnowledgeDocument>().orderByDesc(KnowledgeDocument::getCreatedAt)
        );

        List<KnowledgeDocumentResponse> docs = pageResult.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);
        result.put("pages", pageResult.getPages());
        result.put("documents", docs);
        return result;
    }

    @Override
    public List<KnowledgeChunkResponse> getDocumentChunks(Long docId) {
        ensureDocumentExists(docId);
        // 直接查 vector_store，按 doc_id 过滤，按 chunk_index 排序
        return jdbcTemplate.query(
                "SELECT content, metadata->>'chunk_index' AS chunk_index " +
                "FROM vector_store WHERE metadata->>'doc_id' = ? " +
                "ORDER BY (metadata->>'chunk_index')::int",
                (rs, rowNum) -> KnowledgeChunkResponse.builder()
                        .chunkIndex(Integer.parseInt(rs.getString("chunk_index")))
                        .content(rs.getString("content"))
                        .build(),
                String.valueOf(docId)
        );
    }

    // ==================== 删除 ====================

    @Override
    public void deleteDocument(Long docId) {
        KnowledgeDocument doc = documentMapper.selectById(docId);
        if (doc == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在");
        }

        // 删除向量
        jdbcTemplate.update(
                "DELETE FROM vector_store WHERE metadata->>'doc_id' = ?",
                String.valueOf(docId)
        );

        // 删除磁盘文件
        try {
            Files.deleteIfExists(Paths.get(doc.getFilePath()));
        } catch (IOException e) {
            log.warn("删除磁盘文件失败：{}", doc.getFilePath(), e);
        }

        // 删除 DB 记录
        documentMapper.deleteById(docId);
        log.info("文档已删除：docId={}, file={}", docId, doc.getOriginalName());
    }

    // ==================== 配置 ====================

    @Override
    public KnowledgeConfigResponse getConfig() {
        KnowledgeConfig config = configMapper.selectById(1);
        return toConfigResponse(config);
    }

    @Override
    public KnowledgeConfigResponse updateConfig(KnowledgeConfigRequest request) {
        KnowledgeConfig config = configMapper.selectById(1);
        config.setChunkSize(request.getChunkSize());
        config.setOverlap(request.getOverlap());
        config.setUpdatedAt(LocalDateTime.now());
        configMapper.updateById(config);
        return toConfigResponse(config);
    }

    // ==================== 重新处理 ====================

    @Override
    public void reprocessAll() {
        // 1. 删除所有 PDF 上传来源的向量
        jdbcTemplate.update("DELETE FROM vector_store WHERE metadata->>'source' = 'pdf_upload'");
        log.info("已清空 PDF 来源的向量数据");

        // 2. 查所有 DONE 状态的文档，重新处理
        List<KnowledgeDocument> docs = documentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocument>().eq(KnowledgeDocument::getStatus, "DONE")
        );

        for (KnowledgeDocument doc : docs) {
            try {
                int chunkCount = processDocument(doc);
                doc.setChunkCount(chunkCount);
                doc.setProcessedAt(LocalDateTime.now());
                documentMapper.updateById(doc);
                log.info("重新处理完成：docId={}, chunks={}", doc.getId(), chunkCount);
            } catch (Exception e) {
                doc.setStatus("FAILED");
                documentMapper.updateById(doc);
                log.error("重新处理失败：docId={}", doc.getId(), e);
            }
        }
        log.info("全部文档重新处理完毕，共处理 {} 个", docs.size());
    }

    // ==================== 检索测试 ====================

    @Override
    public List<KnowledgeSearchResult> searchTest(KnowledgeSearchRequest request) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(request.getQuery())
                        .topK(request.getTopK())
                        .similarityThreshold(request.getSimilarityThreshold())
                        .build()
        );

        return results.stream()
                .map(doc -> {
                    Map<String, Object> meta = doc.getMetadata();
                    return KnowledgeSearchResult.builder()
                            .content(doc.getText())
                            .score(doc.getScore())
                            .docId(getString(meta, "doc_id"))
                            .docName(getString(meta, "doc_name"))
                            .tags(getString(meta, "tags"))
                            .chunkIndex(getInt(meta, "chunk_index"))
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ==================== 私有工具方法 ====================

    /**
     * 从磁盘读取 PDF，提取文本，按当前全局配置分块后写入 vector_store。
     * @return 实际写入的 chunk 数
     */
    private int processDocument(KnowledgeDocument doc) throws IOException {
        KnowledgeConfig config = configMapper.selectById(1);
        int chunkSize = config.getChunkSize();
        int overlap = config.getOverlap();

        String text;
        try (PDDocument pdf = Loader.loadPDF(new File(doc.getFilePath()))) {
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(pdf);
        }
        text = text.trim();
        if (text.isEmpty()) {
            log.warn("PDF 文本为空：{}", doc.getOriginalName());
            return 0;
        }

        // 滑动窗口分块
        List<String> chunks = chunkText(text, chunkSize, overlap);

        // 构建 Document 列表，metadata 含 doc_id 用于后续过滤
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("doc_id", String.valueOf(doc.getId()));
            metadata.put("doc_name", doc.getOriginalName());
            metadata.put("tags", doc.getTags() != null ? doc.getTags() : "");
            metadata.put("source", "pdf_upload");
            metadata.put("chunk_index", String.valueOf(i));
            documents.add(new Document(chunks.get(i), metadata));
        }

        vectorStore.add(documents);
        log.info("文档已写入向量库：docId={}, chunks={}", doc.getId(), documents.size());
        return documents.size();
    }

    /** 滑动窗口字符级分块 */
    private List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int step = chunkSize - overlap;
        if (step <= 0) step = chunkSize; // overlap 过大时降级处理
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end).trim());
            if (end == text.length()) break;
            start += step;
        }
        return chunks;
    }

    private void ensureUploadDir() {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void ensureDocumentExists(Long docId) {
        if (documentMapper.selectById(docId) == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在");
        }
    }

    private KnowledgeDocumentResponse toResponse(KnowledgeDocument doc) {
        String uploaderName = null;
        if (doc.getUploadedBy() != null) {
            User uploader = userMapper.selectById(doc.getUploadedBy());
            if (uploader != null) uploaderName = uploader.getUsername();
        }
        return KnowledgeDocumentResponse.builder()
                .id(doc.getId())
                .originalName(doc.getOriginalName())
                .fileSize(doc.getFileSize())
                .tags(doc.getTags())
                .status(doc.getStatus())
                .chunkCount(doc.getChunkCount())
                .uploadedByUsername(uploaderName)
                .createdAt(doc.getCreatedAt())
                .processedAt(doc.getProcessedAt())
                .build();
    }

    private KnowledgeConfigResponse toConfigResponse(KnowledgeConfig config) {
        return KnowledgeConfigResponse.builder()
                .chunkSize(config.getChunkSize())
                .overlap(config.getOverlap())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private String getString(Map<String, Object> meta, String key) {
        Object val = meta.get(key);
        return val != null ? val.toString() : null;
    }

    private Integer getInt(Map<String, Object> meta, String key) {
        Object val = meta.get(key);
        if (val == null) return null;
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return null; }
    }
}
