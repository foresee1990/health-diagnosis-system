package com.health.healthdiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员视角的会话元数据（不含消息内容）
 * 严禁包含 messages 表任何字段
 */
@Data
@Builder
public class AdminConsultationSummary {

    private Long consultationId;

    private String status;

    private String riskLevel;

    /** 主诉（用户第一句话，非对话内容） */
    private String chiefComplaint;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}
