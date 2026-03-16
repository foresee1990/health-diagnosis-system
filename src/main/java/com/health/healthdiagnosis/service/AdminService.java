package com.health.healthdiagnosis.service;

import com.health.healthdiagnosis.dto.response.AdminConsultationSummary;
import com.health.healthdiagnosis.dto.response.AdminUserResponse;
import com.health.healthdiagnosis.dto.response.SystemLogResponse;

import java.util.List;
import java.util.Map;

public interface AdminService {

    /** 分页查询用户列表（脱敏，不含 messages 内容） */
    Map<String, Object> listUsers(int page, int size, String keyword, String status);

    /** 禁用用户 */
    void banUser(Long operatorId, Long targetUserId);

    /** 启用用户 */
    void unbanUser(Long operatorId, Long targetUserId);

    /** 重置用户密码，返回临时明文密码 */
    String resetPassword(Long operatorId, Long targetUserId);

    /** 查看用户会话元数据（不含 messages 内容） */
    List<AdminConsultationSummary> getUserConsultations(Long targetUserId);

    /** 分页查询系统操作日志 */
    Map<String, Object> listLogs(int page, int size, Long operatorId);
}
