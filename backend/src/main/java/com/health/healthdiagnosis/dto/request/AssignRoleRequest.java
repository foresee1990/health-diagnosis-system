package com.health.healthdiagnosis.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignRoleRequest {

    @NotBlank(message = "角色不能为空")
    private String role;
}
