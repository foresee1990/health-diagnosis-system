package com.health.healthdiagnosis.service;

/**
 * @author WU,Rowan
 * @date 2026/4/12
 */
import com.health.healthdiagnosis.dto.request.SaveHealthProfileRequest;
import com.health.healthdiagnosis.dto.response.HealthProfileResponse;

public interface HealthProfileService {

    /**
     * 获取用户健康档案。档案未填写时返回 empty 响应，不报错。
     */
    HealthProfileResponse getProfile(Long userId);

    /**
     * 创建或更新健康档案（upsert）。
     */
    HealthProfileResponse saveProfile(Long userId, SaveHealthProfileRequest request);

    /**
     * 将健康档案格式化为注入 LLM 系统提示词的患者背景字符串。
     * 若用户未填写档案，返回 null（调用方跳过注入）。
     */
    String buildPatientContext(Long userId);
}
