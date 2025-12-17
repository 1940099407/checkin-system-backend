package com.checkin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.checkin.common.Result;
import com.checkin.entity.CheckinRecord;
import java.time.LocalDate;

/**
 * 打卡记录服务接口
 * 定义所有打卡相关业务方法，包括基础打卡、补卡、统计等
 */
public interface CheckinRecordService extends IService<CheckinRecord> {

    /**
     * 创建正常打卡记录（含地点校验、防重复）
     * @param record 打卡记录实体（需包含用户ID、经纬度等）
     * @return 响应结果
     */
    Result<?> createCheckin(CheckinRecord record);

    /**
     * 查询用户所有打卡记录（按时间倒序）
     * @param userId 用户ID
     * @return 打卡记录列表
     */
    Result<?> getUserCheckins(Long userId);

    /**
     * 查询用户今日打卡状态
     * @param userId 用户ID
     * @return 布尔值（true=已打卡，false=未打卡）
     */
    Result<?> getTodayCheckinStatus(Long userId);

    /**
     * 统计用户连续打卡天数（带Redis缓存优化）
     * @param userId 用户ID
     * @return 连续打卡天数
     */
    Result<?> getContinuousCheckinDays(Long userId);

    /**
     * 分页查询用户打卡记录
     * @param userId 用户ID
     * @param pageNum 页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @return 分页结果
     */
    Result<?> getUserCheckinsByPage(Long userId, int pageNum, int pageSize);

    /**
     * 校验打卡地点是否在有效区域内
     * @param latitude 纬度
     * @param longitude 经度
     * @return 校验结果
     */
    Result<?> validateCheckinLocation(Double latitude, Double longitude);

    /**
     * 补卡功能（限制天数和每月次数）
     * @param userId 用户ID
     * @param reissueDate 补卡日期
     * @param reason 补卡理由
     * @return 补卡结果
     */
    Result<?> reissueCheckin(Long userId, LocalDate reissueDate, String reason);

    /**
     * 月度打卡统计（打卡次数、打卡率、详情）
     * @param userId 用户ID
     * @param year 年份
     * @param month 月份
     * @return 统计数据
     */
    Result<?> getMonthlyCheckinStats(Long userId, int year, int month);

    /**
     * 查询指定日期未打卡的用户（管理员功能）
     * @param date 日期
     * @return 未打卡用户列表
     */
    Result<?> getUncheckedUsers(LocalDate date);
}