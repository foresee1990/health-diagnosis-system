package com.health.healthdiagnosis.dto.response;

/**
 * 健康档案响应 DTO
 *
 * @author WU,Rowan
 * @date 2026/4/12
 */
import com.health.healthdiagnosis.entity.HealthProfile;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HealthProfileResponse {

    private Long userId;
    private Integer age;
    private String gender;
    private String allergies;
    private String chronicDiseases;
    private String currentMedications;
    private LocalDateTime updatedAt;
    /** 档案是否已填写（所有字段均为空时为 false） */
    private boolean filled;

    public static HealthProfileResponse fromEntity(HealthProfile profile) {
        HealthProfileResponse resp = new HealthProfileResponse();
        resp.setUserId(profile.getUserId());
        resp.setAge(profile.getAge());
        resp.setGender(profile.getGender());
        resp.setAllergies(profile.getAllergies());
        resp.setChronicDiseases(profile.getChronicDiseases());
        resp.setCurrentMedications(profile.getCurrentMedications());
        resp.setUpdatedAt(profile.getUpdatedAt());
        resp.setFilled(profile.getAge() != null
                || profile.getGender() != null
                || profile.getAllergies() != null
                || profile.getChronicDiseases() != null
                || profile.getCurrentMedications() != null);
        return resp;
    }

    /** 未填写时返回空响应（不报 404） */
    public static HealthProfileResponse empty(Long userId) {
        HealthProfileResponse resp = new HealthProfileResponse();
        resp.setUserId(userId);
        resp.setFilled(false);
        return resp;
    }
}
