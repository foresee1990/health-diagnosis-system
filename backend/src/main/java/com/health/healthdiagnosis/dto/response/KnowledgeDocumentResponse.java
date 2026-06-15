package com.health.healthdiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class KnowledgeDocumentResponse {

    private Long id;
    private String originalName;
    private Long fileSize;
    private String tags;
    private String status;
    private Integer chunkCount;
    private String uploadedByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
