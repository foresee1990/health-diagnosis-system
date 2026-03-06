package com.health.healthdiagnosis.entity;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("messages")
public class Message {

    /**
     * 消息ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属会话ID
     */
    private Long consultationId;

    /**
     * 角色：user/assistant
     * 数据库层有 CHECK 约束
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 发送时间
     */
    private LocalDateTime createdAt;
}