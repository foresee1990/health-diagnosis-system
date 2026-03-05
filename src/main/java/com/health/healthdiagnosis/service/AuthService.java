package com.health.healthdiagnosis.service;

import com.health.healthdiagnosis.common.Result;
import com.health.healthdiagnosis.dto.request.RegisterRequest;

public interface AuthService {

    /**
     * 用户注册
     */
    Result<?> register(RegisterRequest request);
}
