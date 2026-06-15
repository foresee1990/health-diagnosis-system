package com.health.healthdiagnosis.mapper;

/**
 * @author WU,Rowan
 * @date 2026/4/12
 */
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.health.healthdiagnosis.entity.HealthProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthProfileMapper extends BaseMapper<HealthProfile> {
}
