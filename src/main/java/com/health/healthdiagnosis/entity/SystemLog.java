package com.health.healthdiagnosis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统操作日志实体
 * 对应数据库表：system_logs
 */
@Data
@TableName("system_logs")
public class SystemLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人ID（管理员） */
    private Long operatorId;

    /** 操作类型，如 BAN_USER / UNBAN_USER / RESET_PASSWORD */
    private String action;

    /** 目标用户ID（可为空） */
    private Long targetUserId;

    /** 操作详情，JSON 字符串 */
    private String details;

    private LocalDateTime createdAt;
}
