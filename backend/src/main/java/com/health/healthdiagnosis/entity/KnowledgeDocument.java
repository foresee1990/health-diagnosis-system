package com.health.healthdiagnosis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_documents")
public class KnowledgeDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String originalName;

    private String storedName;

    private String filePath;

    private Long fileSize;

    /** 逗号分隔的标签字符串，如 "心血管,高血压" */
    private String tags;

    /** PENDING / DONE / FAILED */
    private String status;

    private Integer chunkCount;

    private Long uploadedBy;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;
}
