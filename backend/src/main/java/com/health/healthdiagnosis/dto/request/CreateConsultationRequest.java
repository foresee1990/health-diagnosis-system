package com.health.healthdiagnosis.dto.request;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateConsultationRequest {

    @Size(max = 500, message = "主诉内容长度不能超过 500 字符")
    private String chiefComplaint;
}