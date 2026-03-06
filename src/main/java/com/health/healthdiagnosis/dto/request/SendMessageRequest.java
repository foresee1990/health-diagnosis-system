package com.health.healthdiagnosis.dto.request;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "消息长度不能超过500字符")
    private String content;
}