package com.health.healthdiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SystemLogResponse {

    private Long id;

    private Long operatorId;

    private String operatorUsername;

    private String action;

    private Long targetUserId;

    private String targetUsername;

    private String details;

    private LocalDateTime createdAt;
}
