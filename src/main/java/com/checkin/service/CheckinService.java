package com.checkin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.checkin.entity.Checkin;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 学习健康打卡服务接口
 * 补全createStudyCheckin方法声明，统一类型匹配
 */
public interface CheckinService extends IService<Checkin> {

    /**
     * 学习打卡提交（核心方法）
     * @param checkin 打卡实体
     * @return 提示信息（String类型，匹配控制器调用）
     */
    String createStudyCheckin(Checkin checkin);

    /**
     * 根据用户ID和日期查询打卡记录
     */
    Checkin getCheckinByUserAndDate(Long userId, LocalDate date);

    /**
     * 根据用户ID和日期范围查询打卡记录
     */
    List<Checkin> listCheckinByUserAndDateRange(Long userId, LocalDate start, LocalDate end);

    /**
     * 月度学习打卡统计
     */
    Map<String, Object> statMonthlyCheckin(Long userId, int year, int month);

    /**
     * 管理员分页查询所有打卡记录
     */
    IPage<Checkin> getAllCheckinByPage(Page<Checkin> page, String username, Integer status);
}