package com.health.healthdiagnosis.service.impl;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.health.healthdiagnosis.dto.request.CreateConsultationRequest;
import com.health.healthdiagnosis.dto.response.*;
import com.health.healthdiagnosis.entity.Consultation;
import com.health.healthdiagnosis.entity.Message;
import com.health.healthdiagnosis.exception.BusinessException;
import com.health.healthdiagnosis.mapper.ConsultationMapper;
import com.health.healthdiagnosis.mapper.MessageMapper;
import com.health.healthdiagnosis.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.health.healthdiagnosis.common.ErrorCode.ACCESS_DENIED;
import static com.health.healthdiagnosis.common.ErrorCode.CONSULTATION_ALREADY_COMPLETED;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationMapper consultationMapper;
    private final MessageMapper messageMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsultationResponse createConsultation(Long userId, CreateConsultationRequest request) {
        // 1. 创建问诊会话记录
        Consultation consultation = new Consultation();
        consultation.setUserId(userId);
        consultation.setStatus("ongoing"); // 初始状态
        consultation.setChiefComplaint(request.getChiefComplaint());
        consultation.setCreatedAt(LocalDateTime.now());
        // 如果有 updatedAt 字段也可以在此设置

        // 插入数据库，获取自增 ID (假设 MP 配置了回填 ID)
        consultationMapper.insert(consultation);
        Long consultationId = consultation.getId();

        // 2. 业务规则：自动将主诉作为第一条 user 消息写入 messages 表
        Message firstMessage = new Message();
        firstMessage.setConsultationId(consultationId);
        firstMessage.setRole("user");
        firstMessage.setContent(request.getChiefComplaint());
        firstMessage.setCreatedAt(LocalDateTime.now());

        messageMapper.insert(firstMessage);
        log.info("会话创建成功");
        // 3. 返回响应
        return ConsultationResponse.fromEntity(consultation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SendMessageResponse sendMessage(Long consultationId,
                                           Long userId,
                                           String content) {

        Consultation consultation = consultationMapper.selectById(consultationId);

        if (consultation == null || !consultation.getUserId().equals(userId)) {
            throw new BusinessException(ACCESS_DENIED,"ACCESS_DENIED");
        }

        //TODO:不要hardcode
        if (!"ongoing".equals(consultation.getStatus())) {
            throw new BusinessException(CONSULTATION_ALREADY_COMPLETED,"CONSULTATION_ALREADY_COMPLETED");
        }

        // 保存用户消息
        Message userMessage = new Message();
        userMessage.setConsultationId(consultationId);
        userMessage.setRole("user");
        userMessage.setContent(content);
        userMessage.setCreatedAt(LocalDateTime.now());

        messageMapper.insert(userMessage);

        // TODO: 固定回复（代替AI）
        String reply = "收到您的消息，正在分析中...";

        Message assistantMessage = new Message();
        assistantMessage.setConsultationId(consultationId);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(reply);
        assistantMessage.setCreatedAt(LocalDateTime.now());

        messageMapper.insert(assistantMessage);


        return new SendMessageResponse(
                MessageResponse.fromEntity(userMessage),
                MessageResponse.fromEntity(assistantMessage)
        );
    }


    @Override
    public ConsultationMessagesResponse getMessages(Long consultationId, Long userId) {
        // 1 查询会话
        Consultation consultation = consultationMapper.selectById(consultationId);

        if (consultation == null || !consultation.getUserId().equals(userId)) {
            throw new BusinessException(ACCESS_DENIED,"ACCESS_DENIED");
        }

        // 2 查询消息列表
        List<Message> messageList = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConsultationId, consultationId)
                        .orderByAsc(Message::getCreatedAt)
        );

        // 3 转换 DTO
        List<MessageResponse> responses = messageList.stream()
                .map(m -> new MessageResponse(
                        m.getId(),
                        m.getRole(),
                        m.getContent(),
                        m.getCreatedAt()
                ))
                .toList();

        // 4 构建返回对象
        ConsultationMessagesResponse result = new ConsultationMessagesResponse();
        result.setConsultationId(consultationId);
        result.setStatus(consultation.getStatus());
        result.setRiskLevel(consultation.getRiskLevel());
        result.setMessages(responses);

        return result;
    }
    /**
     * 获取问诊会话列表
     */
    @Override
    public ConsultationPageResponse getConsultationList(Long userId, int page, int size) {

        Page<Consultation> pageObj = new Page<>(page, size);

        Page<Consultation> result = consultationMapper.selectPage(
                pageObj,
                new LambdaQueryWrapper<Consultation>()
                        .eq(Consultation::getUserId, userId)
                        .orderByDesc(Consultation::getCreatedAt)
        );

        List<ConsultationItemResponse> list = result.getRecords().stream()
                .map(this::convertToItem)
                .toList();

        ConsultationPageResponse resp = new ConsultationPageResponse();
        resp.setTotal(result.getTotal());
        resp.setPage(result.getCurrent());
        resp.setSize(result.getSize());
        resp.setPages(result.getPages());
        resp.setConsultations(list);

        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsultationItemResponse completeConsultation(Long consultationId, Long userId) {

        Consultation consultation = consultationMapper.selectById(consultationId);

        if (consultation == null || !consultation.getUserId().equals(userId)) {
            throw new BusinessException(ACCESS_DENIED,"ACCESS_DENIED");
        }

        // 幂等处理
        if ("completed".equals(consultation.getStatus())) {
            return convertToItem(consultation);
        }

        consultation.setStatus("completed");
        consultation.setCompletedAt(LocalDateTime.now());

        consultationMapper.updateById(consultation);
        log.info("会话已结束, consultationId={}", consultationId);
        return convertToItem(consultation);
    }

    private ConsultationItemResponse convertToItem(Consultation c) {
        ConsultationItemResponse item = new ConsultationItemResponse();
        item.setId(c.getId());
        item.setChiefComplaint(c.getChiefComplaint());
        item.setStatus(c.getStatus());
        item.setRiskLevel(c.getRiskLevel());
        item.setCreatedAt(c.getCreatedAt());
        item.setCompletedAt(c.getCompletedAt());
        return item;
    }
}