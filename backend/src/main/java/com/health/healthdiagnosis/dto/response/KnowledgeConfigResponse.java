package com.health.healthdiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class KnowledgeConfigResponse {

    private Integer chunkSize;
    private Integer overlap;
    private LocalDateTime updatedAt;
}
