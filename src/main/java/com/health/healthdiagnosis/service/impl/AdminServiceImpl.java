package com.health.healthdiagnosis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.health.healthdiagnosis.common.ErrorCode;
import com.health.healthdiagnosis.dto.response.AdminConsultationSummary;
import com.health.healthdiagnosis.dto.response.AdminUserResponse;
import com.health.healthdiagnosis.dto.response.SystemLogResponse;
import com.health.healthdiagnosis.entity.Consultation;
import com.health.healthdiagnosis.entity.SystemLog;
import com.health.healthdiagnosis.entity.User;
import com.health.healthdiagnosis.exception.BusinessException;
import com.health.healthdiagnosis.mapper.ConsultationMapper;
import com.health.healthdiagnosis.mapper.SystemLogMapper;
import com.health.healthdiagnosis.mapper.UserMapper;
import com.health.healthdiagnosis.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final ConsultationMapper consultationMapper;
    private final SystemLogMapper systemLogMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public Map<String, Object> listUsers(int page, int size, String keyword, String status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreatedAt);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getUsername, keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(User::getStatus, status);
        }

        Page<User> pageResult = userMapper.selectPage(new Page<>(page, size), wrapper);

        List<AdminUserResponse> users = pageResult.getRecords().stream()
                .map(u -> {
                    // 查询该用户的会话统计（仅元数据，不查 messages）
                    Long count = consultationMapper.selectCount(
                            new LambdaQueryWrapper<Consultation>().eq(Consultation::getUserId, u.getId())
                    );
                    Consultation lastC = consultationMapper.selectOne(
                            new LambdaQueryWrapper<Consultation>()
                                    .eq(Consultation::getUserId, u.getId())
                                    .orderByDesc(Consultation::getCreatedAt)
                                    .last("LIMIT 1")
                    );
                    return AdminUserResponse.builder()
                            .userId(u.getId())
                            .username(u.getUsername())
                            .email(maskEmail(u.getEmail()))
                            .role(u.getRole())
                            .status(u.getStatus())
                            .consultationCount(count)
                            .lastConsultationAt(lastC != null ? lastC.getCreatedAt() : null)
                            .createdAt(u.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);
        result.put("pages", pageResult.getPages());
        result.put("users", users);
        return result;
    }

    @Override
    public void banUser(Long operatorId, Long targetUserId) {
        User target = getAndValidateTarget(targetUserId, operatorId);
        if ("BANNED".equals(target.getStatus())) {
            return; // 幂等，已经是 BANNED 就不重复操作
        }
        target.setStatus("BANNED");
        userMapper.updateById(target);
        saveLog(operatorId, "BAN_USER", targetUserId, "{\"status\":\"BANNED\"}");
        log.info("管理员禁用用户：operatorId={}, targetUserId={}", operatorId, targetUserId);
    }

    @Override
    public void unbanUser(Long operatorId, Long targetUserId) {
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "目标用户不存在");
        }
        if ("ACTIVE".equals(target.getStatus())) {
            return;
        }
        target.setStatus("ACTIVE");
        userMapper.updateById(target);
        saveLog(operatorId, "UNBAN_USER", targetUserId, "{\"status\":\"ACTIVE\"}");
        log.info("管理员启用用户：operatorId={}, targetUserId={}", operatorId, targetUserId);
    }

    @Override
    public String resetPassword(Long operatorId, Long targetUserId) {
        User target = getAndValidateTarget(targetUserId, operatorId);
        String tempPassword = generateTempPassword(8);
        target.setPasswordHash(bCryptPasswordEncoder.encode(tempPassword));
        userMapper.updateById(target);
        saveLog(operatorId, "RESET_PASSWORD", targetUserId, "{\"action\":\"password_reset\"}");
        log.info("管理员重置密码：operatorId={}, targetUserId={}", operatorId, targetUserId);
        return tempPassword;
    }

    @Override
    public List<AdminConsultationSummary> getUserConsultations(Long targetUserId) {
        // 仅查 consultations 表元数据，严禁关联 messages 表
        List<Consultation> list = consultationMapper.selectList(
                new LambdaQueryWrapper<Consultation>()
                        .eq(Consultation::getUserId, targetUserId)
                        .orderByDesc(Consultation::getCreatedAt)
        );
        return list.stream()
                .map(c -> AdminConsultationSummary.builder()
                        .consultationId(c.getId())
                        .status(c.getStatus())
                        .riskLevel(c.getRiskLevel())
                        .chiefComplaint(c.getChiefComplaint())
                        .createdAt(c.getCreatedAt())
                        .completedAt(c.getCompletedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> listLogs(int page, int size, Long operatorId) {
        LambdaQueryWrapper<SystemLog> wrapper = new LambdaQueryWrapper<SystemLog>()
                .orderByDesc(SystemLog::getCreatedAt);
        if (operatorId != null) {
            wrapper.eq(SystemLog::getOperatorId, operatorId);
        }

        Page<SystemLog> pageResult = systemLogMapper.selectPage(new Page<>(page, size), wrapper);

        List<SystemLogResponse> logs = pageResult.getRecords().stream()
                .map(l -> {
                    User operator = userMapper.selectById(l.getOperatorId());
                    User targetUser = l.getTargetUserId() != null ? userMapper.selectById(l.getTargetUserId()) : null;
                    return SystemLogResponse.builder()
                            .id(l.getId())
                            .operatorId(l.getOperatorId())
                            .operatorUsername(operator != null ? operator.getUsername() : null)
                            .action(l.getAction())
                            .targetUserId(l.getTargetUserId())
                            .targetUsername(targetUser != null ? targetUser.getUsername() : null)
                            .details(l.getDetails())
                            .createdAt(l.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);
        result.put("pages", pageResult.getPages());
        result.put("logs", logs);
        return result;
    }

    // ==================== 私有工具方法 ====================

    /**
     * 获取目标用户并校验：不允许操作 ADMIN 账号，不允许操作自身
     */
    private User getAndValidateTarget(Long targetUserId, Long operatorId) {
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "目标用户不存在");
        }
        if ("ADMIN".equals(target.getRole())) {
            throw new BusinessException(ErrorCode.CANNOT_OPERATE_ADMIN, "不允许操作管理员账号");
        }
        if (targetUserId.equals(operatorId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不允许操作自身账号");
        }
        return target;
    }

    /** 记录操作日志 */
    private void saveLog(Long operatorId, String action, Long targetUserId, String details) {
        SystemLog logEntry = new SystemLog();
        logEntry.setOperatorId(operatorId);
        logEntry.setAction(action);
        logEntry.setTargetUserId(targetUserId);
        logEntry.setDetails(details);
        logEntry.setCreatedAt(LocalDateTime.now());
        systemLogMapper.insert(logEntry);
    }

    /** 邮箱脱敏：t***@example.com */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf('@');
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 1) {
            return local + "***" + domain;
        }
        return local.charAt(0) + "***" + domain;
    }

    /** 随机生成指定长度的临时密码 */
    private String generateTempPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
