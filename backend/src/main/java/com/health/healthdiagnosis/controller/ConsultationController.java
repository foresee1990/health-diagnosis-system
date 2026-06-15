package com.health.healthdiagnosis.controller;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */
import com.health.healthdiagnosis.common.Result;
import com.health.healthdiagnosis.dto.request.CreateConsultationRequest;
import com.health.healthdiagnosis.dto.request.SendMessageRequest;
import com.health.healthdiagnosis.dto.response.*;
import com.health.healthdiagnosis.service.ConsultationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationController {
    private final ConsultationService consultationService;

    /**
     * 创建问诊会话
     * POST /api/consultations
     */
    @PostMapping
    public Result<ConsultationResponse> createConsultation(
            @Valid @RequestBody CreateConsultationRequest request,
            HttpServletRequest httpRequest) {

        // 从 Request 属性中获取 userId (由 AuthInterceptor 设置)
        Long userId = (Long) httpRequest.getAttribute("userId");

        if (userId == null) {
            // 理论上拦截器应该已经拦截了未登录请求，这里是双重保险
            throw new RuntimeException("用户未登录或无法获取用户信息");
        }

        ConsultationResponse response = consultationService.createConsultation(userId, request);

        return Result.success("会话创建成功",response);
    }

    /**
     * 发送消息
     * POST /api/consultations/{consultationId}/messages
     */
    @PostMapping("/{consultationId}/messages")
    public Result<SendMessageResponse> sendMessage(
            @PathVariable Long consultationId,
            @Valid @RequestBody SendMessageRequest request,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");

        SendMessageResponse data =
                consultationService.sendMessage(
                        consultationId,
                        userId,
                        request.getContent()
                );

        return Result.success("消息发送成功", data);
    }

    /**
     * 发送消息（SSE 流式）
     * POST /api/consultations/{consultationId}/messages/stream
     */
    @PostMapping(value = "/{consultationId}/messages/stream",
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> sendMessageStream(
            @PathVariable Long consultationId,
            @Valid @RequestBody SendMessageRequest request,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");
        return consultationService.sendMessageStream(consultationId, userId, request.getContent());
    }

    @GetMapping("/{consultationId}/messages")
    public Result<ConsultationMessagesResponse> getMessages(
            @PathVariable Long consultationId,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        ConsultationMessagesResponse data =
                consultationService.getMessages(consultationId, userId);

        return Result.success("获取成功", data);
    }
    /**
     * 获取问诊会话列表
     * GET /api/consultations
     */
    @GetMapping
    public Result<ConsultationPageResponse> getConsultationList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        ConsultationPageResponse data =
                consultationService.getConsultationList(userId, page, size);

        return Result.success("获取成功", data);
    }
    /**
     * 结束问诊会话
     * PATCH /api/consultations/{consultationId}/status
     */
    @PatchMapping("/{consultationId}/status")
    public Result<ConsultationItemResponse> completeConsultation(
            @PathVariable Long consultationId,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        ConsultationItemResponse data =
                consultationService.completeConsultation(consultationId, userId);
        return Result.success("问诊已结束", data);
    }

    /**
     * 删除问诊会话（含消息）
     * DELETE /api/consultations/{consultationId}
     */
    @DeleteMapping("/{consultationId}")
    public Result<?> deleteConsultation(
            @PathVariable Long consultationId,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        consultationService.deleteConsultation(consultationId, userId);
        return Result.success("删除成功", null);
    }

    /**
     * 重命名会话标题
     * PATCH /api/consultations/{consultationId}/title
     */
    @PatchMapping("/{consultationId}/title")
    public Result<ConsultationItemResponse> renameConsultation(
            @PathVariable Long consultationId,
            @RequestBody java.util.Map<String, String> body,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        String title = body.getOrDefault("title", "").trim();
        ConsultationItemResponse data =
                consultationService.renameConsultation(consultationId, userId, title);
        return Result.success("重命名成功", data);
    }
}