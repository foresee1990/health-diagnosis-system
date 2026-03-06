package com.health.healthdiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReportVO {
    private Long reportId;
    private String downloadUrl;
    private Integer fileSize;
    private LocalDateTime createdAt;
}
