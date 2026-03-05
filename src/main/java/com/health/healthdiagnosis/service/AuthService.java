package com.health.healthdiagnosis.service;

import com.health.healthdiagnosis.common.Result;
import com.health.healthdiagnosis.dto.request.LoginRequest;
import com.health.healthdiagnosis.dto.request.RegisterRequest;
import com.health.healthdiagnosis.dto.response.LoginResponse;

public interface AuthService {

    /**
     * 用户注册
     */
    Result<?> register(RegisterRequest request);

    /**
     * 用户登录
     * @param request 登录请求参数
     * @return 登录响应（包含token、userId、username）
     */
    LoginResponse login(LoginRequest request);
}
