package com.checkin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.checkin.common.Result;
import com.checkin.entity.CheckinRecord;
import com.checkin.entity.User;
import com.checkin.enums.CheckinTypeEnum;
import com.checkin.mapper.CheckinRecordMapper;
import com.checkin.mapper.UserMapper;
import com.checkin.service.CheckinRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 学习健康打卡服务实现类（完整逻辑，无编译错误）
 */
@Slf4j
@Service
public class CheckinRecordServiceImpl extends ServiceImpl<CheckinRecordMapper, CheckinRecord> implements CheckinRecordService {

    @Autowired
    private UserMapper userMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // 打卡方式常量
    public static final String CHECKIN_WAY_TEXT = "普通文字";
    public static final String CHECKIN_WAY_PHOTO = "拍照打卡";
    public static final String CHECKIN_WAY_LOCATION = "定位打卡";

    /**
     * 学习打卡提交（核心方法，无编译错误）
     */
    @Override
    public Result<?> createCheckin(CheckinRecord record) {
        // 1. 字段校验
        if (record.getCheckinType() == null || record.getCheckinType().trim().isEmpty()) {
            return Result.error("打卡类型不能为空（学习/阅读/冥想/自定义）");
        }
        List<String> validTypes = Arrays.asList(
                CheckinTypeEnum.STUDY.getDesc(),
                CheckinTypeEnum.READ.getDesc(),
                CheckinTypeEnum.MEDITATION.getDesc(),
                CheckinTypeEnum.CUSTOM.getDesc()
        );
        if (!validTypes.contains(record.getCheckinType())) {
            return Result.error("打卡类型仅支持：学习、阅读、冥想、自定义");
        }

        if (record.getCheckinWay() == null || record.getCheckinWay().trim().isEmpty()) {
            return Result.error("打卡方式不能为空（普通文字/拍照打卡/定位打卡）");
        }
        List<String> validWays = Arrays.asList(CHECKIN_WAY_TEXT, CHECKIN_WAY_PHOTO, CHECKIN_WAY_LOCATION);
        if (!validWays.contains(record.getCheckinWay())) {
            return Result.error("打卡方式仅支持：普通文字、拍照打卡、定位打卡");
        }

        if (record.getRemark() != null && record.getRemark().length() > 500) {
            return Result.error("打卡备注不能超过500个字符");
        }

        // 2. 用户校验
        Long userId = record.getUserId();
        Result<?> userValidResult = validateUserExists(userId);
        if (!userValidResult.isSuccess()) {
            return userValidResult;
        }

        // 3. 防重复打卡
        if (hasCheckedInToday(userId)) {
            return Result.error("今日已打卡，请勿重复操作");
        }

        // 4. 定位打卡校验
        if (CHECKIN_WAY_LOCATION.equals(record.getCheckinWay())) {
            if (record.getLocation() == null || record.getLocation().trim().isEmpty()) {
                return Result.error("定位打卡时，地点不能为空");
            }
            if (record.getLatitude() == null || record.getLongitude() == null) {
                return Result.error("定位打卡时，经纬度不能为空");
            }
        }

        // 5. 拍照打卡校验
        if (CHECKIN_WAY_PHOTO.equals(record.getCheckinWay()) && (record.getPhotoUrl() == null || record.getPhotoUrl().trim().isEmpty())) {
            return Result.error("拍照打卡时，照片链接不能为空");
        }

        // 6. 填充数据（status为Integer类型，赋值1）
        record.setCheckinTime(LocalDateTime.now());
        record.setStatus(1); // 1=正常打卡（Integer类型，无类型错误）
        record.setIsReissue(0);

        // 7. 保存
        try {
            baseMapper.insert(record);
            // 清除缓存
            if (redisTemplate != null) {
                redisTemplate.delete("checkin:continuous:" + userId);
            }
            return Result.success(record.getCheckinType() + "打卡成功", record);
        } catch (Exception e) {
            log.error("打卡失败", e);
            return Result.error("打卡失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> getUserCheckins(Long userId) {
        Result<?> userValidResult = validateUserExists(userId);
        if (!userValidResult.isSuccess()) {
            return userValidResult;
        }

        List<CheckinRecord> records = baseMapper.selectList(new QueryWrapper<CheckinRecord>()
                .eq("user_id", userId)
                .orderByDesc("checkin_time")
        );
        return Result.success(records);
    }

    @Override
    public Result<?> getTodayCheckinStatus(Long userId) {
        Result<?> userValidResult = validateUserExists(userId);
        if (!userValidResult.isSuccess()) {
            return userValidResult;
        }
        return Result.success(hasCheckedInToday(userId));
    }

    @Override
    public Result<?> getContinuousCheckinDays(Long userId) {
        String cacheKey = "checkin:continuous:" + userId;
        Object cachedDays = redisTemplate != null ? redisTemplate.opsForValue().get(cacheKey) : null;
        if (cachedDays != null) {
            return Result.success(cachedDays);
        }

        Result<?> userValidResult = validateUserExists(userId);
        if (!userValidResult.isSuccess()) {
            return userValidResult;
        }

        List<CheckinRecord> records = baseMapper.selectList(new QueryWrapper<CheckinRecord>()
                .eq("user_id", userId)
                .orderByDesc("checkin_time")
        );
        if (records.isEmpty()) {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(cacheKey, 0, 2, TimeUnit.HOURS);
            }
            return Result.success(0);
        }

        int continuousDays = 1;
        LocalDate lastCheckinDate = records.get(0).getCheckinTime().toLocalDate();
        LocalDate today = LocalDate.now();

        if (!lastCheckinDate.isEqual(today)) {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(cacheKey, continuousDays, 2, TimeUnit.HOURS);
            }
            return Result.success(continuousDays);
        }

        for (int i = 1; i < records.size(); i++) {
            LocalDate currentDate = records.get(i).getCheckinTime().toLocalDate();
            LocalDate expectedDate = today.minusDays(continuousDays);
            if (currentDate.isEqual(expectedDate)) {
                continuousDays++;
                today = currentDate;
            } else {
                break;
            }
        }

        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(cacheKey, continuousDays, 2, TimeUnit.HOURS);
        }
        return Result.success(continuousDays);
    }

    @Override
    public Result<?> getUserCheckinsByPage(Long userId, int pageNum, int pageSize) {
        Result<?> userValidResult = validateUserExists(userId);
        if (!userValidResult.isSuccess()) {
            return userValidResult;
        }

        pageNum = Math.max(pageNum, 1);
        pageSize = Math.max(1, Math.min(pageSize, 100));

        IPage<CheckinRecord> page = new Page<>(pageNum, pageSize);
        IPage<CheckinRecord> resultPage = baseMapper.selectPage(page, new QueryWrapper<CheckinRecord>()
                .eq("user_id", userId)
                .orderByDesc("checkin_time")
        );
        return Result.success(resultPage);
    }

    @Override
    public Result<?> getMonthlyCheckinStats(Long userId, int year, int month) {
        Result<?> userValidResult = validateUserExists(userId);
        if (!userValidResult.isSuccess()) {
            return userValidResult;
        }

        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.plusMonths(1).minusDays(1);
        int totalDays = lastDay.getDayOfMonth();

        List<CheckinRecord> records = baseMapper.selectList(new QueryWrapper<CheckinRecord>()
                .eq("user_id", userId)
                .ge("checkin_time", firstDay.atStartOfDay())
                .le("checkin_time", lastDay.atTime(23, 59, 59))
        );

        Set<LocalDate> checkinDates = records.stream()
                .map(r -> r.getCheckinTime().toLocalDate())
                .collect(Collectors.toSet());
        int checkinDays = checkinDates.size();
        double checkinRate = totalDays == 0 ? 0 : (double) checkinDays / totalDays * 100;

        // 按类型统计
        long studyCount = records.stream().filter(r -> CheckinTypeEnum.STUDY.getDesc().equals(r.getCheckinType())).count();
        long readCount = records.stream().filter(r -> CheckinTypeEnum.READ.getDesc().equals(r.getCheckinType())).count();
        long meditationCount = records.stream().filter(r -> CheckinTypeEnum.MEDITATION.getDesc().equals(r.getCheckinType())).count();
        long customCount = records.stream().filter(r -> CheckinTypeEnum.CUSTOM.getDesc().equals(r.getCheckinType())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("year", year);
        stats.put("month", month);
        stats.put("totalDays", totalDays);
        stats.put("checkinDays", checkinDays);
        stats.put("checkinRate", String.format("%.1f%%", checkinRate));
        stats.put("studyCount", studyCount);
        stats.put("readCount", readCount);
        stats.put("meditationCount", meditationCount);
        stats.put("customCount", customCount);
        stats.put("records", records);

        return Result.success(stats);
    }

    @Override
    public Result<?> getCheckinStats(Long userId) {
        Result<?> userValidResult = validateUserExists(userId);
        if (!userValidResult.isSuccess()) {
            return userValidResult;
        }

        List<CheckinRecord> records = baseMapper.selectList(new QueryWrapper<CheckinRecord>()
                .eq("user_id", userId)
                .orderByAsc("checkin_time")
        );

        long totalDays = records.stream()
                .map(r -> r.getCheckinTime().toLocalDate())
                .distinct()
                .count();

        Result<?> continuousResult = getContinuousCheckinDays(userId);
        int continuousDays = continuousResult.isSuccess() ? (Integer) continuousResult.getData() : 0;

        LocalDate now = LocalDate.now();
        LocalDate firstDayOfMonth = now.withDayOfMonth(1);
        LocalDate lastDayOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        long monthlyDays = records.stream()
                .map(r -> r.getCheckinTime().toLocalDate())
                .filter(date -> date.isAfter(firstDayOfMonth.minusDays(1)) && date.isBefore(lastDayOfMonth.plusDays(1)))
                .distinct()
                .count();

        double monthlyRate = 0.0;
        int totalDaysOfMonth = now.lengthOfMonth();
        if (totalDaysOfMonth > 0) {
            monthlyRate = Math.round(((double) monthlyDays / totalDaysOfMonth) * 100) / 100.0;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDays", totalDays);
        stats.put("continuousDays", continuousDays);
        stats.put("userId", userId);
        stats.put("monthlyDays", monthlyDays);
        stats.put("monthlyRate", monthlyRate);
        stats.put("currentMonth", now.getMonthValue());

        return Result.success(stats);
    }

    // 私有工具方法
    private Result<?> validateUserExists(Long userId) {
        if (userId == null) {
            return Result.error("用户ID不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success();
    }

    private boolean hasCheckedInToday(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        Long count = baseMapper.selectCount(new QueryWrapper<CheckinRecord>()
                .eq("user_id", userId)
                .ge("checkin_time", todayStart)
                .lt("checkin_time", tomorrowStart)
        );
        return count > 0;
    }
}