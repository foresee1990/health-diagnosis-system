package com.health.healthdiagnosis.service;

import com.health.healthdiagnosis.dto.request.KnowledgeConfigRequest;
import com.health.healthdiagnosis.dto.request.KnowledgeSearchRequest;
import com.health.healthdiagnosis.dto.response.KnowledgeChunkResponse;
import com.health.healthdiagnosis.dto.response.KnowledgeConfigResponse;
import com.health.healthdiagnosis.dto.response.KnowledgeDocumentResponse;
import com.health.healthdiagnosis.dto.response.KnowledgeSearchResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface KnowledgeManagementService {

    /** 上传并处理 PDF，返回文档信息 */
    KnowledgeDocumentResponse uploadAndProcess(MultipartFile file, String tags, Long uploaderId);

    /** 分页查询文档列表 */
    Map<String, Object> listDocuments(int page, int size);

    /** 查看指定文档的切分结果 */
    List<KnowledgeChunkResponse> getDocumentChunks(Long docId);

    /** 删除文档（磁盘文件 + 向量 + DB记录） */
    void deleteDocument(Long docId);

    /** 获取全局分块配置 */
    KnowledgeConfigResponse getConfig();

    /** 更新全局分块配置 */
    KnowledgeConfigResponse updateConfig(KnowledgeConfigRequest request);

    /** 用当前配置重新处理所有文档 */
    void reprocessAll();

    /** 检索测试：返回匹配内容和相似度 */
    List<KnowledgeSearchResult> searchTest(KnowledgeSearchRequest request);
}
