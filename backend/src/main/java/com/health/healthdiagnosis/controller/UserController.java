package com.health.healthdiagnosis.controller;

import com.health.healthdiagnosis.common.Result;
import com.health.healthdiagnosis.dto.request.ChangePasswordRequest;
import com.health.healthdiagnosis.dto.request.SaveHealthProfileRequest;
import com.health.healthdiagnosis.dto.request.UpdateProfileRequest;
import com.health.healthdiagnosis.dto.response.HealthProfileResponse;
import com.health.healthdiagnosis.dto.response.UserInfoResponse;
import com.health.healthdiagnosis.service.HealthProfileService;
import com.health.healthdiagnosis.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户中心接口：修改密码、更新个人信息、健康档案
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final HealthProfileService healthProfileService;

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        userService.changePassword(userId, request);
        return Result.success("密码修改成功", null);
    }

    /**
     * 更新个人信息（邮箱）
     */
    @PutMapping("/profile")
    public Result<UserInfoResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        UserInfoResponse response = userService.updateProfile(userId, request);
        return Result.success("个人信息更新成功", response);
    }

    /**
     * 获取健康档案（未填写时返回空对象，不报 404）
     */
    @GetMapping("/health-profile")
    public Result<HealthProfileResponse> getHealthProfile(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success("获取成功", healthProfileService.getProfile(userId));
    }

    /**
     * 创建或更新健康档案（upsert）
     */
    @PutMapping("/health-profile")
    public Result<HealthProfileResponse> saveHealthProfile(
            @Valid @RequestBody SaveHealthProfileRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success("健康档案已保存", healthProfileService.saveProfile(userId, request));
    }
}
