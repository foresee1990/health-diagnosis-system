package com.health.healthdiagnosis.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_config")
public class KnowledgeConfig {

    /** 固定为 1，全局单行配置 */
    @TableId
    private Integer id;

    private Integer chunkSize;

    private Integer overlap;

    private LocalDateTime updatedAt;
}
