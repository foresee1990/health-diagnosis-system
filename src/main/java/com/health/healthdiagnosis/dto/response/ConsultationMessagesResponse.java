package com.health.healthdiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationMessagesResponse {

    private Long consultationId;

    private String status;

    private String riskLevel;

    private List<MessageResponse> messages;
}