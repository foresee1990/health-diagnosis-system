package com.health.healthdiagnosis.service;

import com.health.healthdiagnosis.dto.request.ChangePasswordRequest;
import com.health.healthdiagnosis.dto.request.UpdateProfileRequest;
import com.health.healthdiagnosis.dto.response.UserInfoResponse;

public interface UserService {

    /** 修改密码 */
    void changePassword(Long userId, ChangePasswordRequest request);

    /** 更新个人信息（邮箱） */
    UserInfoResponse updateProfile(Long userId, UpdateProfileRequest request);
}
