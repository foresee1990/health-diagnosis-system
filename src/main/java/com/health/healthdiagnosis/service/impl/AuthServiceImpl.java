package com.health.healthdiagnosis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.healthdiagnosis.common.ErrorCode;
import com.health.healthdiagnosis.common.Result;
import com.health.healthdiagnosis.dto.request.RegisterRequest;
import com.health.healthdiagnosis.entity.User;
import com.health.healthdiagnosis.exception.BusinessException;
import com.health.healthdiagnosis.mapper.UserMapper;
import com.health.healthdiagnosis.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.health.healthdiagnosis.common.constants.UserMessageConstants.USER_ALREADY_EXISTS;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public Result<?> register(RegisterRequest request) {
        // 1. 检查用户名是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, USER_ALREADY_EXISTS);
        }

        // 2. 构建用户对象，密码 BCrypt 加密
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(bCryptPasswordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());

        // 3. 插入数据库（MyBatis-Plus 会自动将生成的 id 回填到 user 对象）
        userMapper.insert(user);

        log.info("新用户注册成功: username={}, id={}", user.getUsername(), user.getId());

        return Result.success("注册成功", Map.of(
                "userId", user.getId(),
                "username", user.getUsername()
        ));
    }
}
