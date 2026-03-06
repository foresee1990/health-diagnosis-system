package com.health.healthdiagnosis.dto.response;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConsultationItemResponse {

    private Long id;

    private String chiefComplaint;

    private String status;

    private String riskLevel;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}