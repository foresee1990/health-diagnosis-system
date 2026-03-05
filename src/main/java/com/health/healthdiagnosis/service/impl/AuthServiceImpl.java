package com.health.healthdiagnosis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.healthdiagnosis.common.ErrorCode;
import com.health.healthdiagnosis.common.Result;
import com.health.healthdiagnosis.common.constants.UserMessageConstants;
import com.health.healthdiagnosis.dto.request.LoginRequest;
import com.health.healthdiagnosis.dto.request.RegisterRequest;
import com.health.healthdiagnosis.dto.response.LoginResponse;
import com.health.healthdiagnosis.entity.User;
import com.health.healthdiagnosis.exception.BusinessException;
import com.health.healthdiagnosis.mapper.UserMapper;
import com.health.healthdiagnosis.service.AuthService;
import com.health.healthdiagnosis.util.JwtUtil;
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
    private final JwtUtil jwtUtil;
    /**
     * 用户注册
     *
     * @param request 注册请求参数
     * @return 注册响应结果
     */
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

    /**
     * 用户登录
     *
     * @param request 登录请求参数
     * @return 登录响应结果
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 查询用户是否存在
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
        );
        if (user == null) {
            log.warn("用户登录失败：用户名不存在，username={}", request.getUsername());
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, UserMessageConstants.USER_NOT_FOUND);
        }

        // 2. 验证密码是否匹配
        if (!bCryptPasswordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("用户登录失败：密码错误，username={}", request.getUsername());
            throw new BusinessException(ErrorCode.WRONG_PASSWORD, UserMessageConstants.WRONG_PASSWORD);
        }

        // 3. 生成JWT Token（有效期24小时，Payload包含userId和username）
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 4. 构造并返回登录响应
        LoginResponse loginResponse = LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .build();

        log.info("用户登录成功：username={}, userId={}", user.getUsername(), user.getId());
        return loginResponse;
    }
}
