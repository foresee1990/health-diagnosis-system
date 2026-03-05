package com.health.healthdiagnosis.mapper;

import com.health.healthdiagnosis.entity.User;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;


/**
 * @author WU,Rowan
 * @date 2026/3/5
 */




/**
 * 用户 Mapper 接口
 * 继承 BaseMapper 以获得基本的 CRUD 功能
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 如果需要自定义 SQL，可以在此处添加方法
    // 例如：User selectByUsername(@Param("username") String username);
}