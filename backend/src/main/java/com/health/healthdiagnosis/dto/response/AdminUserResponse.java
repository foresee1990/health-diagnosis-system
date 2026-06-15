package com.health.healthdiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员视角的用户信息（脱敏）
 * 注意：严禁包含任何 messages 表字段，仅保留会话元数据
 */
@Data
@Builder
public class AdminUserResponse {

    private Long userId;

    private String username;

    /** 脱敏邮箱，如 t***@example.com */
    private String email;

    private String role;

    private String status;

    /** 问诊会话总数（元数据，不含消息内容） */
    private Long consultationCount;

    /** 最近一次问诊时间 */
    private LocalDateTime lastConsultationAt;

    private LocalDateTime createdAt;
}
