package com.health.healthdiagnosis.entity;

/**
 * 用户健康档案实体
 * 对应数据库表：health_profiles（与 users 1:1 关联）
 *
 * @author WU,Rowan
 * @date 2026/4/12
 */
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("health_profiles")
public class HealthProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联用户ID，唯一 */
    private Long userId;

    /** 年龄（可选） */
    private Integer age;

    /** 性别：MALE / FEMALE / OTHER（可选） */
    private String gender;

    /** 过敏史，自由文本（如"青霉素、花粉"，可选） */
    private String allergies;

    /** 基础疾病/慢性病（如"高血压、2型糖尿病"，可选） */
    private String chronicDiseases;

    /** 当前用药（如"氨氯地平5mg"，可选） */
    private String currentMedications;

    private LocalDateTime updatedAt;
}
