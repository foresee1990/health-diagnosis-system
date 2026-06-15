package com.health.healthdiagnosis.controller;

import com.health.healthdiagnosis.common.Result;
import com.health.healthdiagnosis.dto.request.KnowledgeConfigRequest;
import com.health.healthdiagnosis.dto.request.KnowledgeSearchRequest;
import com.health.healthdiagnosis.dto.response.KnowledgeChunkResponse;
import com.health.healthdiagnosis.dto.response.KnowledgeConfigResponse;
import com.health.healthdiagnosis.dto.response.KnowledgeDocumentResponse;
import com.health.healthdiagnosis.dto.response.KnowledgeSearchResult;
import com.health.healthdiagnosis.service.KnowledgeManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 知识库管理接口
 * 所有路径均受 KnowledgeInterceptor 保护（role IN KNOWLEDGE_ENGINEER, ADMIN）
 */
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeManagementService knowledgeService;

    /**
     * 上传 PDF 文档
     */
    @PostMapping("/documents")
    public Result<KnowledgeDocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "tags", required = false, defaultValue = "") String tags,
            HttpServletRequest request) {
        Long uploaderId = (Long) request.getAttribute("userId");
        KnowledgeDocumentResponse response = knowledgeService.uploadAndProcess(file, tags, uploaderId);
        return Result.success("上传成功", response);
    }

    /**
     * 文档列表（分页）
     */
    @GetMapping("/documents")
    public Result<Map<String, Object>> listDocuments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success("获取成功", knowledgeService.listDocuments(page, size));
    }

    /**
     * 查看文档切分结果
     */
    @GetMapping("/documents/{id}/chunks")
    public Result<List<KnowledgeChunkResponse>> getChunks(@PathVariable Long id) {
        return Result.success("获取成功", knowledgeService.getDocumentChunks(id));
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/documents/{id}")
    public Result<?> deleteDocument(@PathVariable Long id) {
        knowledgeService.deleteDocument(id);
        return Result.success("删除成功", null);
    }

    /**
     * 获取全局分块配置
     */
    @GetMapping("/config")
    public Result<KnowledgeConfigResponse> getConfig() {
        return Result.success("获取成功", knowledgeService.getConfig());
    }

    /**
     * 更新全局分块配置
     */
    @PutMapping("/config")
    public Result<KnowledgeConfigResponse> updateConfig(@Valid @RequestBody KnowledgeConfigRequest request) {
        return Result.success("配置已保存", knowledgeService.updateConfig(request));
    }

    /**
     * 用当前配置重新处理所有文档
     */
    @PostMapping("/reprocess")
    public Result<?> reprocessAll() {
        knowledgeService.reprocessAll();
        return Result.success("重新处理完成", null);
    }

    /**
     * 智能检索测试
     */
    @PostMapping("/search")
    public Result<List<KnowledgeSearchResult>> search(@Valid @RequestBody KnowledgeSearchRequest request) {
        return Result.success("检索成功", knowledgeService.searchTest(request));
    }
}
