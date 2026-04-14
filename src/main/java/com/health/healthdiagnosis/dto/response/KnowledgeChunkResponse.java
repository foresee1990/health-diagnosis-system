package com.health.healthdiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeChunkResponse {

    private int chunkIndex;
    private String content;
}
