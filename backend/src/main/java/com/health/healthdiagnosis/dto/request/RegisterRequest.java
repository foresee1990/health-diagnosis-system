package com.health.healthdiagnosis.dto.request;

/*
  @author WU,Rowan
 * @date 2026/3/5
 */
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class RegisterRequest {
    /**
     * 用户名：非空，长度3-50
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;

    /**
     * 密码：非空，长度6-20
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;

    /**
     * 邮箱：可选，格式验证
     */
    @Email(message = "邮箱格式不正确")
    private String email;
}
