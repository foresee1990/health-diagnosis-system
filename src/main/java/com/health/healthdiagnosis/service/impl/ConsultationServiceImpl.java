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
import com.health.healthdiagnosis.service.HealthProfileService;
import com.health.healthdiagnosis.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.health.healthdiagnosis.common.ErrorCode.ACCESS_DENIED;
import static com.health.healthdiagnosis.common.ErrorCode.CONSULTATION_ALREADY_COMPLETED;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private static final String THINK_OPEN_TAG = "<think>";
    private static final String THINK_CLOSE_TAG = "</think>";
    private static final Pattern THINK_BLOCK_PATTERN =
            Pattern.compile("<think>([\\s\\S]*?)</think>", Pattern.CASE_INSENSITIVE);

    private static final Pattern RISK_LEVEL_PATTERN =
            Pattern.compile("风险等级[：:＝=]?\\s*(low|medium|high|urgent)", Pattern.CASE_INSENSITIVE);

    private static final String WELCOME_MESSAGE = """
            您好！我是 AI 辅助问诊助手，我会根据医学知识库为您提供参考建议。

            为了给您更准确的回复，请尽量描述：
            - **主要症状**（如头痛、发烧、咳嗽等）
            - **持续时间**（几小时/几天）
            - **严重程度**（轻微/明显/剧烈）
            - **伴随症状**（如有）

            请注意：本系统仅供参考，不能替代专业医生诊断，如症状严重请及时就医。
            """;

    private final ConsultationMapper consultationMapper;
    private final MessageMapper messageMapper;
    private final RagService ragService;
    private final HealthProfileService healthProfileService;


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

        // 3. 插入固定引导消息（assistant 角色），引导用户描述症状
        Message welcomeMessage = new Message();
        welcomeMessage.setConsultationId(consultationId);
        welcomeMessage.setRole("assistant");
        welcomeMessage.setContent(WELCOME_MESSAGE);
        welcomeMessage.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(welcomeMessage);

        log.info("会话创建成功, consultationId={}", consultationId);
        // 4. 返回响应
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

        // RAG 同步调用（ChatMemory 由 MessageChatMemoryAdvisor 自动维护）
        String patientContext = healthProfileService.buildPatientContext(userId);
        String reply = ragService.chat(consultationId, content, patientContext);

        // 解析风险等级并更新 consultation
        String riskLevel = parseRiskLevel(reply);
        if (riskLevel != null) {
            consultation.setRiskLevel(riskLevel);
            consultationMapper.updateById(consultation);
        }
        log.info("RAG 回复完成, consultationId={}, 风险等级={}", consultationId, riskLevel);

        Message assistantMessage = new Message();
        assistantMessage.setConsultationId(consultationId);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(reply);
        assistantMessage.setCreatedAt(LocalDateTime.now());

        messageMapper.insert(assistantMessage);

        return new SendMessageResponse(
                toMessageResponse(userMessage),
                toMessageResponse(assistantMessage)
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
                .map(this::toMessageResponse)
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

    @Override
    public Flux<ServerSentEvent<String>> sendMessageStream(Long consultationId,
                                                           Long userId,
                                                           String content) {
        // 1. 权限 & 状态校验
        Consultation consultation = consultationMapper.selectById(consultationId);
        if (consultation == null || !consultation.getUserId().equals(userId)) {
            throw new BusinessException(ACCESS_DENIED, "ACCESS_DENIED");
        }
        if (!"ongoing".equals(consultation.getStatus())) {
            throw new BusinessException(CONSULTATION_ALREADY_COMPLETED, "CONSULTATION_ALREADY_COMPLETED");
        }

        // 2. 写入 user 消息
        Message userMessage = new Message();
        userMessage.setConsultationId(consultationId);
        userMessage.setRole("user");
        userMessage.setContent(content);
        userMessage.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(userMessage);

        // 3. 流式 RAG（ChatMemory 由 Advisor 自动维护）
        String patientContext = healthProfileService.buildPatientContext(userId);
        StringBuilder rawReplyBuf = new StringBuilder();
        StreamingThinkParser parser = new StreamingThinkParser();

        return ragService.chatStream(consultationId, content, patientContext)
                .doOnNext(rawReplyBuf::append)
                .<ServerSentEvent<String>>handle((token, sink) -> parser.consume(token, sink))
                .concatWith(Flux.defer(() -> {
                    List<ServerSentEvent<String>> tailEvents = parser.finish();
                    String fullAnswer = rawReplyBuf.toString();

                    Message assistantMessage = new Message();
                    assistantMessage.setConsultationId(consultationId);
                    assistantMessage.setRole("assistant");
                    assistantMessage.setContent(fullAnswer);
                    assistantMessage.setCreatedAt(LocalDateTime.now());
                    messageMapper.insert(assistantMessage);

                    String riskLevel = parseRiskLevel(fullAnswer);
                    if (riskLevel != null) {
                        consultation.setRiskLevel(riskLevel);
                        consultationMapper.updateById(consultation);
                    }
                    log.info("流式回复完成, consultationId={}, 风险等级={}", consultationId, riskLevel);

                    String donePayload = "{\"type\":\"done\",\"riskLevel\":\""
                            + (riskLevel != null ? riskLevel : "") + "\"}";
                    tailEvents.add(ServerSentEvent.<String>builder().data(donePayload).build());
                    return Flux.fromIterable(tailEvents);
                }));
    }

    /** 构造 answer 类型的 SSE payload，对 token 内容做 JSON 转义 */
    private static String sseAnswerPayload(String token) {
        String escaped = token
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "{\"type\":\"answer\",\"token\":\"" + escaped + "\"}";
    }

    private MessageResponse toMessageResponse(Message message) {
        MessageSegments segments = splitMessageContent(message.getRole(), message.getContent());
        return new MessageResponse(
                message.getId(),
                message.getRole(),
                segments.thinking(),
                segments.answer(),
                message.getCreatedAt()
        );
    }

    private MessageSegments splitMessageContent(String role, String rawContent) {
        String safeContent = rawContent == null ? "" : rawContent;
        if (!"assistant".equals(role)) {
            return new MessageSegments(null, safeContent);
        }

        Matcher matcher = THINK_BLOCK_PATTERN.matcher(safeContent);
        StringBuilder thinking = new StringBuilder();
        StringBuffer answer = new StringBuffer();
        while (matcher.find()) {
            String block = matcher.group(1);
            if (block != null && !block.isBlank()) {
                if (!thinking.isEmpty()) {
                    thinking.append("\n\n");
                }
                thinking.append(block.trim());
            }
            matcher.appendReplacement(answer, "");
        }
        matcher.appendTail(answer);

        String answerText = answer.toString().trim();
        String thinkingText = thinking.isEmpty() ? null : thinking.toString();
        return new MessageSegments(thinkingText, answerText);
    }

    private static String sseThinkingPayload(String token) {
        String escaped = token
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "{\"type\":\"thinking\",\"token\":\"" + escaped + "\"}";
    }

    private static String sseThinkDonePayload() {
        return "{\"type\":\"thinkDone\"}";
    }

    private String parseRiskLevel(String reply) {
        Matcher m = RISK_LEVEL_PATTERN.matcher(reply);
        return m.find() ? m.group(1).toLowerCase() : null;
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

    private record MessageSegments(String thinking, String answer) {
    }

    private static final class StreamChunk {
        private final String type;
        private final String token;

        private StreamChunk(String type, String token) {
            this.type = type;
            this.token = token;
        }
    }

    private static final class StreamingThinkParser {
        private final StringBuilder markerBuffer = new StringBuilder();
        private final StringBuilder outputBuffer = new StringBuilder();
        private boolean inThinking;

        void consume(String token, SynchronousSink<ServerSentEvent<String>> sink) {
            List<StreamChunk> chunks = parse(token, false);
            for (StreamChunk chunk : chunks) {
                if ("thinking".equals(chunk.type)) {
                    sink.next(ServerSentEvent.<String>builder()
                            .data(sseThinkingPayload(chunk.token))
                            .build());
                } else if ("answer".equals(chunk.type)) {
                    sink.next(ServerSentEvent.<String>builder()
                            .data(sseAnswerPayload(chunk.token))
                            .build());
                } else if ("thinkDone".equals(chunk.type)) {
                    sink.next(ServerSentEvent.<String>builder()
                            .data(sseThinkDonePayload())
                            .build());
                }
            }
        }

        List<ServerSentEvent<String>> finish() {
            List<ServerSentEvent<String>> events = new ArrayList<>();
            List<StreamChunk> chunks = parse("", true);
            for (StreamChunk chunk : chunks) {
                if ("thinking".equals(chunk.type)) {
                    events.add(ServerSentEvent.<String>builder()
                            .data(sseThinkingPayload(chunk.token))
                            .build());
                } else if ("answer".equals(chunk.type)) {
                    events.add(ServerSentEvent.<String>builder()
                            .data(sseAnswerPayload(chunk.token))
                            .build());
                } else if ("thinkDone".equals(chunk.type)) {
                    events.add(ServerSentEvent.<String>builder()
                            .data(sseThinkDonePayload())
                            .build());
                }
            }
            return events;
        }

        private List<StreamChunk> parse(String token, boolean endOfStream) {
            List<StreamChunk> chunks = new ArrayList<>();
            for (int i = 0; i < token.length(); i++) {
                markerBuffer.append(token.charAt(i));
                drainBuffer(chunks);
            }
            if (endOfStream) {
                outputBuffer.append(markerBuffer);
                markerBuffer.setLength(0);
            }
            flushOutput(chunks);
            return chunks;
        }

        private void drainBuffer(List<StreamChunk> chunks) {
            String target = inThinking ? THINK_CLOSE_TAG : THINK_OPEN_TAG;
            while (!markerBuffer.isEmpty()) {
                if (markerBuffer.toString().equalsIgnoreCase(target)) {
                    flushOutput(chunks);
                    markerBuffer.setLength(0);
                    if (inThinking) {
                        chunks.add(new StreamChunk("thinkDone", ""));
                    }
                    inThinking = !inThinking;
                    return;
                }
                if (isPrefixIgnoreCase(markerBuffer, target)) {
                    return;
                }
                outputBuffer.append(markerBuffer.charAt(0));
                markerBuffer.deleteCharAt(0);
            }
        }

        private void flushOutput(List<StreamChunk> chunks) {
            if (outputBuffer.isEmpty()) {
                return;
            }
            chunks.add(new StreamChunk(inThinking ? "thinking" : "answer", outputBuffer.toString()));
            outputBuffer.setLength(0);
        }

        private boolean isPrefixIgnoreCase(StringBuilder buffer, String target) {
            if (buffer.length() > target.length()) {
                return false;
            }
            for (int i = 0; i < buffer.length(); i++) {
                if (Character.toLowerCase(buffer.charAt(i)) != Character.toLowerCase(target.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }
}
