package com.health.healthdiagnosis.controller;

/**
 * @author WU,Rowan
 * @date 2026/3/5
 */
import com.health.healthdiagnosis.common.Result;
import com.health.healthdiagnosis.dto.request.LoginRequest;
import com.health.healthdiagnosis.dto.request.RegisterRequest;
import com.health.healthdiagnosis.dto.response.LoginResponse;
import com.health.healthdiagnosis.dto.response.UserInfoResponse;
import com.health.healthdiagnosis.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor // 注入AuthService
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册接口
     * @param request 注册请求体（带@Valid校验）
     * @return 注册结果
     */
    @PostMapping("/auth/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }
    /**
     * 用户登录接口
     * @param request 登录请求体（带参数校验）
     * @return 登录结果（包含token）
     */
    @PostMapping("/auth/login")
    public Result<?> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.login(request);
        return Result.success("登录成功", loginResponse);
    }
    /**
     * 获取用户信息接口
     * @param request 用于获取拦截器存入的userId
     * @return 用户信息结果
     */
    @GetMapping("/users/me")
    public Result<?> getCurrentUserInfo(HttpServletRequest request) {
        // 从request中获取拦截器解析Token后存入的userId（需确保拦截器已将userId放入request属性）
        Long userId = (Long) request.getAttribute("userId");
        UserInfoResponse userInfoResponse = authService.getUserInfo(userId);
        return Result.success("获取成功", userInfoResponse);
    }
}