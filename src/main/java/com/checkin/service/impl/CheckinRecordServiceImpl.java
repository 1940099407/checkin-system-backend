package com.checkin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.checkin.common.Result;
import com.checkin.entity.CheckinRecord;
import com.checkin.mapper.CheckinRecordMapper;
import com.checkin.service.CheckinRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j  // 用于日志输出
@Service
public class CheckinRecordServiceImpl extends ServiceImpl<CheckinRecordMapper, CheckinRecord> implements CheckinRecordService {

    @Override
    public Result<?> createCheckin(CheckinRecord record) {
        // 1. 校验用户ID不能为空
        if (record.getUserId() == null) {
            log.warn("打卡失败：用户ID为空");
            return Result.error("用户ID不能为空");
        }

        // 2. 检查今日是否已打卡（时间范围：今日00:00:00 至 明日00:00:00）
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();  // 今日00:00:00
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();  // 明日00:00:00

        QueryWrapper<CheckinRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", record.getUserId())
                .ge("checkin_time", todayStart)  // 大于等于今日0点
                .lt("checkin_time", tomorrowStart);  // 小于明日0点（避免包含明日0点整）

        long todayCheckinCount = baseMapper.selectCount(queryWrapper);
        if (todayCheckinCount > 0) {
            log.info("用户[{}]今日已打卡，无需重复提交", record.getUserId());
            return Result.error("今日已打卡，无需重复提交");
        }

        // 3. 设置打卡时间为当前时间（核心修复：解决checkin_time字段无值问题）
        record.setCheckinTime(LocalDateTime.now());

        // 4. 设置默认状态（1-正常打卡，避免status为null）
        if (record.getStatus() == null) {
            record.setStatus(1);
        }

        // 5. 保存打卡记录到数据库
        try {
            baseMapper.insert(record);
            log.info("用户[{}]打卡成功，记录ID：{}", record.getUserId(), record.getId());
            return Result.success("打卡成功", record);
        } catch (Exception e) {
            log.error("用户[{}]打卡失败", record.getUserId(), e);
            return Result.error("打卡失败，请重试");
        }
    }

    @Override
    public Result<?> getUserCheckins(Long userId) {
        // 校验用户ID
        if (userId == null) {
            return Result.error("用户ID不能为空");
        }

        // 查询用户所有打卡记录，按打卡时间倒序（最新的在前）
        QueryWrapper<CheckinRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .orderByDesc("checkin_time");

        return Result.success(baseMapper.selectList(queryWrapper));
    }

    @Override
    public Result<?> getTodayCheckinStatus(Long userId) {
        // 校验用户ID
        if (userId == null) {
            return Result.error("用户ID不能为空");
        }

        // 检查今日是否已打卡（逻辑同createCheckin）
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        QueryWrapper<CheckinRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .ge("checkin_time", todayStart)
                .lt("checkin_time", tomorrowStart);

        long count = baseMapper.selectCount(queryWrapper);
        return Result.success(count > 0);  // 返回true（已打卡）或false（未打卡）
    }
}