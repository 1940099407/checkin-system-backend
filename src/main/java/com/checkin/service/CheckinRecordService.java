package com.checkin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.checkin.common.Result;
import com.checkin.entity.CheckinRecord;
import java.time.LocalDate;

/**
 * 学习健康打卡记录服务接口
 * 适配学习/阅读/冥想打卡场景，删除考勤相关冗余方法
 */
public interface CheckinRecordService extends IService<CheckinRecord> {

    /**
     * 创建学习打卡记录（含类型校验、防重复、打卡方式校验）
     * @param record 打卡记录实体（需包含用户ID、打卡类型、打卡方式等）
     * @return 响应结果
     */
    Result<?> createCheckin(CheckinRecord record);

    /**
     * 查询用户所有学习打卡记录（按时间倒序）
     * @param userId 用户ID
     * @return 打卡记录列表
     */
    Result<?> getUserCheckins(Long userId);

    /**
     * 查询用户今日学习打卡状态
     * @param userId 用户ID
     * @return 布尔值（true=已打卡，false=未打卡）
     */
    Result<?> getTodayCheckinStatus(Long userId);

    /**
     * 统计用户连续学习打卡天数（带Redis缓存优化）
     * @param userId 用户ID
     * @return 连续打卡天数
     */
    Result<?> getContinuousCheckinDays(Long userId);

    /**
     * 分页查询用户学习打卡记录
     * @param userId 用户ID
     * @param pageNum 页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @return 分页结果
     */
    Result<?> getUserCheckinsByPage(Long userId, int pageNum, int pageSize);

    /**
     * 月度学习打卡统计（按类型统计次数、打卡率、总天数）
     * @param userId 用户ID
     * @param year 年份
     * @param month 月份
     * @return 统计数据（含学习/阅读/冥想等类型次数）
     */
    Result<?> getMonthlyCheckinStats(Long userId, int year, int month);

    /**
     * 学习打卡核心统计（总天数+连续天数+本月统计）
     * @param userId 用户ID
     * @return 统计结果（总天数、连续天数、本月打卡率等）
     */
    Result<?> getCheckinStats(Long userId);
}