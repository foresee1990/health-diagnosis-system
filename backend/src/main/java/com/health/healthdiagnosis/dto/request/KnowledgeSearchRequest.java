package com.health.healthdiagnosis.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KnowledgeSearchRequest {

    @NotBlank(message = "查询内容不能为空")
    private String query;

    @Min(1)
    @Max(20)
    private int topK = 5;

    @DecimalMin(value = "0.0", message = "相似度阈值不能小于0")
    @DecimalMax(value = "1.0", message = "相似度阈值不能大于1")
    private double similarityThreshold = 0.5;
}
