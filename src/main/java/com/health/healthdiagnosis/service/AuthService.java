package com.health.healthdiagnosis.service;

import com.health.healthdiagnosis.common.Result;
import com.health.healthdiagnosis.dto.request.LoginRequest;
import com.health.healthdiagnosis.dto.request.RegisterRequest;
import com.health.healthdiagnosis.dto.response.LoginResponse;
import com.health.healthdiagnosis.dto.response.UserInfoResponse;

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

    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return 用户信息响应（不含密码）
     */
    UserInfoResponse getUserInfo(Long userId);
}
