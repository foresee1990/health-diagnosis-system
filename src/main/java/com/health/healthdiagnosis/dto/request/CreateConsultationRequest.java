package com.health.healthdiagnosis.dto.request;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateConsultationRequest {

    /**
     * 主诉内容
     * 规则：不能为空，长度 1~500 字符
     */
    @NotBlank(message = "主诉内容不能为空")
    @Size(max = 500, message = "主诉内容长度不能超过 500 字符")
    private String chiefComplaint;
}