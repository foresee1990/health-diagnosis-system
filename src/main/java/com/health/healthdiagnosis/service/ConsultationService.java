package com.health.healthdiagnosis.service;

import com.health.healthdiagnosis.dto.request.CreateConsultationRequest;
import com.health.healthdiagnosis.dto.response.*;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */
public interface ConsultationService {

    /**
     * 创建问诊会话
     * @param userId 当前登录用户ID (从拦截器/过滤器传入)
     * @param request 请求体
     * @return 创建成功会话信息
     */
    ConsultationResponse createConsultation(Long userId, CreateConsultationRequest request);

    /**
     * 发送消息
     *
     * @param consultationId 会话ID
     * @param userId         当前登录用户ID
     * @param content        用户发送的消息内容
     * @return 用户消息 + 助手回复
     */
    SendMessageResponse sendMessage(Long consultationId,
                                    Long userId,
                                    String content);

    /**
     * 获取会话消息历史
     */
    ConsultationMessagesResponse getMessages(Long consultationId, Long userId);

    /**
     * 获取会话列表
     */
    ConsultationPageResponse getConsultationList(Long userId, int page, int size);

    /*
     *结束会话
     */
    ConsultationItemResponse completeConsultation(Long consultationId, Long userId);
}