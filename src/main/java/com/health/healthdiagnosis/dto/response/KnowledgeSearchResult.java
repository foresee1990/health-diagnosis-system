package com.health.healthdiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeSearchResult {

    private String content;
    /** 相似度分数，0~1，越高越相关 */
    private Double score;
    private String docId;
    private String docName;
    private String tags;
    private Integer chunkIndex;
}
