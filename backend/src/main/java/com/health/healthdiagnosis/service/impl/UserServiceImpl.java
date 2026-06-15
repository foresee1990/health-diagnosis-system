package com.health.healthdiagnosis.service.impl;

import com.health.healthdiagnosis.common.ErrorCode;
import com.health.healthdiagnosis.dto.request.ChangePasswordRequest;
import com.health.healthdiagnosis.dto.request.UpdateProfileRequest;
import com.health.healthdiagnosis.dto.response.UserInfoResponse;
import com.health.healthdiagnosis.entity.User;
import com.health.healthdiagnosis.exception.BusinessException;
import com.health.healthdiagnosis.mapper.UserMapper;
import com.health.healthdiagnosis.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        if (!bCryptPasswordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.WRONG_OLD_PASSWORD, "旧密码不正确");
        }
        user.setPasswordHash(bCryptPasswordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        log.info("用户修改密码成功：userId={}", userId);
    }

    @Override
    public UserInfoResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        user.setEmail(request.getEmail());
        userMapper.updateById(user);
        log.info("用户更新个人信息成功：userId={}", userId);
        return UserInfoResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
