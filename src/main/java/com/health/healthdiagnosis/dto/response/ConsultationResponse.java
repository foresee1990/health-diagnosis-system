package com.health.healthdiagnosis.dto.response;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */
import com.health.healthdiagnosis.entity.Consultation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationResponse {

    private Long consultationId;
    private String status;
    private LocalDateTime createdAt;

    /**
     * 从实体类转换为响应 DTO
     */
    public static ConsultationResponse fromEntity(Consultation consultation) {
        return ConsultationResponse.builder()
                .consultationId(consultation.getId())
                .status(consultation.getStatus())
                .createdAt(consultation.getCreatedAt())
                .build();
    }
}