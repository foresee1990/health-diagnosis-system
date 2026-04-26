package com.health.healthdiagnosis.dto.response;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */

import com.health.healthdiagnosis.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class MessageResponse {

    private Long id;

    private String role;

    private String thinking;

    private String content;

    private LocalDateTime createdAt;
}
