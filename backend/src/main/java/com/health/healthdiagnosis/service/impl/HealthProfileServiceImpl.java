package com.health.healthdiagnosis.service.impl;

/**
 * @author WU,Rowan
 * @date 2026/4/12
 */
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.healthdiagnosis.dto.request.SaveHealthProfileRequest;
import com.health.healthdiagnosis.dto.response.HealthProfileResponse;
import com.health.healthdiagnosis.entity.HealthProfile;
import com.health.healthdiagnosis.mapper.HealthProfileMapper;
import com.health.healthdiagnosis.service.HealthProfileService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthProfileServiceImpl implements HealthProfileService {

    private final HealthProfileMapper healthProfileMapper;
    private final JdbcTemplate jdbcTemplate;

    /** 启动时自动建表（幂等），无需手动执行 SQL */
    @PostConstruct
    public void initSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS health_profiles (
                    id                  BIGSERIAL PRIMARY KEY,
                    user_id             BIGINT       NOT NULL UNIQUE,
                    age                 INTEGER,
                    gender              VARCHAR(10),
                    allergies           TEXT,
                    chronic_diseases    TEXT,
                    current_medications TEXT,
                    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
                )
                """);
        log.info("health_profiles 表已就绪");
    }

    @Override
    public HealthProfileResponse getProfile(Long userId) {
        HealthProfile profile = findByUserId(userId);
        return profile != null
                ? HealthProfileResponse.fromEntity(profile)
                : HealthProfileResponse.empty(userId);
    }

    @Override
    public HealthProfileResponse saveProfile(Long userId, SaveHealthProfileRequest request) {
        HealthProfile profile = findByUserId(userId);
        if (profile == null) {
            profile = new HealthProfile();
            profile.setUserId(userId);
        }
        profile.setAge(request.getAge());
        profile.setGender(request.getGender());
        profile.setAllergies(request.getAllergies());
        profile.setChronicDiseases(request.getChronicDiseases());
        profile.setCurrentMedications(request.getCurrentMedications());
        profile.setUpdatedAt(LocalDateTime.now());

        if (profile.getId() == null) {
            healthProfileMapper.insert(profile);
        } else {
            healthProfileMapper.updateById(profile);
        }
        log.info("健康档案已保存, userId={}", userId);
        return HealthProfileResponse.fromEntity(profile);
    }

    @Override
    public String buildPatientContext(Long userId) {
        HealthProfile profile = findByUserId(userId);
        if (profile == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean hasAny = false;

        if (profile.getAge() != null) {
            sb.append("- 年龄：").append(profile.getAge()).append("岁");
            hasAny = true;
        }
        if (profile.getGender() != null) {
            sb.append(" | 性别：").append(translateGender(profile.getGender()));
            hasAny = true;
        }
        if (profile.getAllergies() != null && !profile.getAllergies().isBlank()) {
            sb.append("\n- 过敏史：").append(profile.getAllergies());
            hasAny = true;
        }
        if (profile.getChronicDiseases() != null && !profile.getChronicDiseases().isBlank()) {
            sb.append("\n- 基础疾病：").append(profile.getChronicDiseases());
            hasAny = true;
        }
        if (profile.getCurrentMedications() != null && !profile.getCurrentMedications().isBlank()) {
            sb.append("\n- 当前用药：").append(profile.getCurrentMedications());
            hasAny = true;
        }

        if (!hasAny) {
            return null;
        }

        return "【患者基本信息】（请在回答时充分考虑以下背景，注意用药禁忌和基础疾病影响）\n"
                + sb;
    }

    private HealthProfile findByUserId(Long userId) {
        return healthProfileMapper.selectOne(
                new LambdaQueryWrapper<HealthProfile>()
                        .eq(HealthProfile::getUserId, userId)
        );
    }

    private String translateGender(String gender) {
        return switch (gender) {
            case "MALE" -> "男";
            case "FEMALE" -> "女";
            default -> "其他";
        };
    }
}
