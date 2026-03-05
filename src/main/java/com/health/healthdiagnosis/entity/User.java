package com.health.healthdiagnosis.entity;

/**
 * @author WU,Rowan
 * @date 2026/3/5
 */

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表：users
 */
@Data
@TableName("users")
public class User {

    /**
     * 用户ID (对应字段: id)
     * 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名 (对应字段: username)
     * 唯一且非空
     */
    private String username;

    /**
     * 密码哈希 (对应字段: password_hash)
     * BCrypt加密后的密码
     * MyBatis-Plus 默认会将驼峰命名自动映射为下划线命名
     */
    private String passwordHash;

    /**
     * 邮箱 (对应字段: email)
     * 可选
     */
    private String email;

    /**
     * 注册时间 (对应字段: created_at)
     * 默认当前时间戳
     */
    private LocalDateTime createdAt;
}
