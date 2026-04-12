package com.health.healthdiagnosis.dto.request;

/**
 * 保存/更新健康档案请求体（所有字段可选，upsert 语义）
 *
 * @author WU,Rowan
 * @date 2026/4/12
 */
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveHealthProfileRequest {

    @Min(value = 1, message = "年龄不合法")
    @Max(value = 150, message = "年龄不合法")
    private Integer age;

    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "性别只能为 MALE、FEMALE 或 OTHER")
    private String gender;

    @Size(max = 200, message = "过敏史不超过200字")
    private String allergies;

    @Size(max = 200, message = "基础疾病不超过200字")
    private String chronicDiseases;

    @Size(max = 200, message = "当前用药不超过200字")
    private String currentMedications;
}
