package com.health.healthdiagnosis.controller;

import com.health.healthdiagnosis.common.Result;
import com.health.healthdiagnosis.dto.response.AdminConsultationSummary;
import com.health.healthdiagnosis.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员中心接口
 * 所有路径均受 AdminInterceptor 保护（role == ADMIN 方可访问）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 用户列表查询（脱敏，不含 messages 内容）
     */
    @GetMapping("/users")
    public Result<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.success("获取成功", adminService.listUsers(page, size, keyword, status));
    }

    /**
     * 禁用用户
     */
    @PatchMapping("/users/{userId}/ban")
    public Result<?> banUser(@PathVariable Long userId, HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        adminService.banUser(operatorId, userId);
        return Result.success("用户已禁用", null);
    }

    /**
     * 启用用户
     */
    @PatchMapping("/users/{userId}/unban")
    public Result<?> unbanUser(@PathVariable Long userId, HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        adminService.unbanUser(operatorId, userId);
        return Result.success("用户已启用", null);
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/users/{userId}/password/reset")
    public Result<Map<String, String>> resetPassword(@PathVariable Long userId, HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        String tempPassword = adminService.resetPassword(operatorId, userId);
        return Result.success("密码重置成功", Map.of("newPassword", tempPassword));
    }

    /**
     * 查看用户会话元数据（不含消息内容）
     */
    @GetMapping("/users/{userId}/consultations")
    public Result<List<AdminConsultationSummary>> getUserConsultations(@PathVariable Long userId) {
        return Result.success("获取成功", adminService.getUserConsultations(userId));
    }

    /**
     * 查看系统操作日志
     */
    @GetMapping("/logs")
    public Result<Map<String, Object>> listLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long operatorId) {
        return Result.success("获取成功", adminService.listLogs(page, size, operatorId));
    }
}
