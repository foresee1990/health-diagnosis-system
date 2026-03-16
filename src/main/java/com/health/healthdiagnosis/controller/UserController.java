package com.health.healthdiagnosis.controller;

import com.health.healthdiagnosis.common.Result;
import com.health.healthdiagnosis.dto.request.ChangePasswordRequest;
import com.health.healthdiagnosis.dto.request.UpdateProfileRequest;
import com.health.healthdiagnosis.dto.response.UserInfoResponse;
import com.health.healthdiagnosis.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户中心接口：修改密码、更新个人信息
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
}
