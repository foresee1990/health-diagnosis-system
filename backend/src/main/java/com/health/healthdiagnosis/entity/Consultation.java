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
@TableName("consultations")
public class Consultation {

    /**
     * 会话ID (SERIAL PRIMARY KEY)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 状态：ongoing/completed
     * 数据库层有 CHECK 约束
     */
    private String status;

    /**
     * 风险等级：low/medium/high/urgent
     * 数据库层有 CHECK 约束，可为 null
     */
    private String riskLevel;

    /**
     * 主诉（用户第一句话）
     */
    private String chiefComplaint;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 结束时间
     */
    private LocalDateTime completedAt;
}