package com.health.healthdiagnosis.controller;

/**
 * @author WU,Rowan
 * @date 2026/3/5
 */
import com.health.healthdiagnosis.common.Result;
import com.health.healthdiagnosis.dto.request.LoginRequest;
import com.health.healthdiagnosis.dto.request.RegisterRequest;
import com.health.healthdiagnosis.dto.response.LoginResponse;
import com.health.healthdiagnosis.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor // 注入AuthService
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册接口
     * @param request 注册请求体（带@Valid校验）
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }
    /**
     * 用户登录接口
     * @param request 登录请求体（带参数校验）
     * @return 登录结果（包含token）
     */
    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.login(request);
        return Result.success("登录成功", loginResponse);
    }
}