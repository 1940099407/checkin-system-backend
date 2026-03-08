package com.checkin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.checkin.entity.Checkin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 打卡Mapper（MyBatis-Plus自动实现CRUD）
 */
@Mapper
public interface CheckinMapper extends BaseMapper<Checkin> {
}