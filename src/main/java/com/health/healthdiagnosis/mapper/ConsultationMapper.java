package com.health.healthdiagnosis.mapper;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.health.healthdiagnosis.entity.Consultation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConsultationMapper extends BaseMapper<Consultation> {
    // MyBatis-Plus 已提供基础 CRUD 方法
    // 如需复杂查询（如联查 messages），可在此定义额外方法或使用 Service 层组装
}