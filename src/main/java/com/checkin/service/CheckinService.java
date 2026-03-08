package com.checkin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.checkin.entity.Checkin;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 打卡业务接口
 */
public interface CheckinService extends IService<Checkin> {
    // 上班打卡（防重复）
    String doCheckin(Long userId, String location);

    // 下班签退
    String doCheckout(Long userId, String location);

    // 查询某日打卡记录
    Checkin getCheckinByUserAndDate(Long userId, LocalDate date);

    // 查询时间段打卡记录
    List<Checkin> listCheckinByUserAndDateRange(Long userId, LocalDate start, LocalDate end);

    // 月度打卡统计
    Map<String, Object> statMonthlyCheckin(Long userId, int year, int month);

    IPage<Checkin> getAllCheckinByPage(Page<Checkin> page, String username, String status);
}