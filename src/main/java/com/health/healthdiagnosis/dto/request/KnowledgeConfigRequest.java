package com.health.healthdiagnosis.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KnowledgeConfigRequest {

    @NotNull(message = "分段长度不能为空")
    @Min(value = 100, message = "分段长度不能小于100")
    @Max(value = 2000, message = "分段长度不能大于2000")
    private Integer chunkSize;

    @NotNull(message = "重叠长度不能为空")
    @Min(value = 0, message = "重叠长度不能为负数")
    @Max(value = 500, message = "重叠长度不能大于500")
    private Integer overlap;
}
