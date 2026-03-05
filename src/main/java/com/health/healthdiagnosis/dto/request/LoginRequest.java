package com.health.healthdiagnosis.dto.request;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录请求DTO
 */
@Data
public class LoginRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}