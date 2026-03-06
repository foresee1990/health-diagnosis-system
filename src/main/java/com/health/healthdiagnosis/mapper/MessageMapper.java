package com.health.healthdiagnosis.mapper;

/**
 * @author WU,Rowan
 * @date 2026/3/6
 */
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.health.healthdiagnosis.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    // MyBatis-Plus 已提供基础 CRUD 方法
    // 示例用法：
    // List<Message> list = messageMapper.selectList(
    //     new QueryWrapper<Message>()
    //         .eq("consultation_id", cid)
    //         .orderByAsc("created_at")
    // );
}