package com.checkin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.checkin.common.Result;
import com.checkin.entity.CheckinRecord;
import com.checkin.entity.User;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 打卡记录服务实现类
 * 完整实现所有打卡相关业务逻辑，包含基础打卡、补卡、统计、缓存等功能
 */
@Slf4j
@Service
public class CheckinRecordServiceImpl extends ServiceImpl<CheckinRecordMapper, CheckinRecord> implements CheckinRecordService {

    // 依赖注入
    @Autowired
    private UserMapper userMapper;

    // RedisTemplate非强制注入，避免无Redis时启动失败
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // 打卡有效区域配置（从配置文件读取）
    @Value("${checkin.valid.latitude.min}")
    private Double minLatitude;
    @Value("${checkin.valid.latitude.max}")
    private Double maxLatitude;
    @Value("${checkin.valid.longitude.min}")
    private Double minLongitude;
    @Value("${checkin.valid.longitude.max}")
    private Double maxLongitude;

    // 补卡规则配置
    @Value("${checkin.reissue.max-days:3}") // 最多补3天内的卡
    private Integer maxReissueDays;
    @Value("${checkin.reissue.max-count:1}") // 每月最多补1次
    private Integer maxReissueCount;


    /**
     * 正常打卡功能
     */
    @Override
    public Result<?> createCheckin(CheckinRecord record) {
        // 1. 地点非空+长度校验
        if (record.getLocation() == null || record.getLocation().trim().isEmpty()) {
            return Result.error("打卡地点不能为空");
        }
        if (record.getLocation().length() > 100) {
            return Result.error("打卡地点不能超过100个字符");
        }

        // 2. 校验用户存在性
        Long userId = record.getUserId();
        Result<?> userValidResult = validateUserExists(userId);
        if (!userValidResult.isSuccess()) {
            return userValidResult;
        }

        // 3. 校验今日是否已打卡
        if (hasCheckedInToday(userId)) {
            log.info("用户[{}]今日已打卡，拒绝重复提交", userId);
            return Result.error("今日已打卡，请勿重复操作");
        }

        // 4. 校验打卡地点（经纬度）
        if (record.getLatitude() != null && record.getLongitude() != null) {
            Result<?> locationResult = validateCheckinLocation(record.getLatitude(), record.getLongitude());
            if (!locationResult.isSuccess()) {
                return locationResult;
            }
        }

        // 5. 填充打卡信息
        record.setCheckinTime(LocalDateTime.now()); // 强制当前时间，防止篡改
        record.setStatus(1); // 1-正常打卡
        record.setIsReissue(0); // 0-非补卡

        // 6. 保存记录
        try {
            baseMapper.insert(record);
            // 清除连续打卡缓存（添加Redis空指针保护）
            if (redisTemplate != null) {
                redisTemplate.delete("checkin:continuous:" + userId);
                log.debug("用户[{}]的连续打卡缓存已清除", userId);
            } else {
                log.warn("RedisTemplate未注入，跳过缓存清除操作");
            }
            log.info("用户[{}]打卡成功，记录ID:{}", userId, record.getId());
            return Result.success("打卡成功", record);
        } catch (Exception e) {
            log.error("用户[{}]打卡失败", userId, e);
            return Result.error("打卡失败，请重试");
        }
    }


    /**
     * 查询用户所有打卡记录（按时间倒序）
     */
    @Override
    public Result<?> getUserCheckins(Long userId) {
        // 校验用户
        Result<?> userValidResult = validateUserExists(userId);
        if (!userValidResult.isSuccess()) {
            return userValidResult;
        }

        // 查询所有记录
        List<CheckinRecord> records = baseMapper.selectList(new QueryWrapper<CheckinRecord>()
                .eq("user_id", userId)
                .orderByDesc("checkin_time")
        );
        log.info("用户[{}]的打卡记录共{}条", userId, records.size());
        return Result.success(records);
    }


    /**
     * 查询用户今日打卡状态
     */
    @Override
    public Result<?> getTodayCheckinStatus(Long userId) {
        // 校验用户
        Result<?> userValidResult = validateUserExists(userId);
        if (!userValidResult.isSuccess()) {
            return userValidResult;
        }

        // 检查今日是否已打卡
        boolean hasChecked = hasCheckedInToday(userId);
        log.info("用户[{}]今日打卡状态：{}", userId, hasChecked ? "已打卡" : "未打卡");
        return Result.success(hasChecked);
    }


    /**
     * 统计用户连续打卡天数（带Redis缓存）
     */
    @Override
    public Result<?> getContinuousCheckinDays(Long userId) {
        // 1. 先查缓存（添加Redis空指针保护）
        String cacheKey = "checkin:continuous:" + userId;
        Object cachedDays = null;
        if (redisTemplate != null) {
            cachedDays = redisTemplate.opsForValue().get(cacheKey);
        }
        if (cachedDays != null) {
            return Result.success(cachedDays);
        }

        // 2. 缓存未命中，查数据库
        Result<?> userValidResult = validateUserExists(userId);
        if (!userValidResult.isSuccess()) {
            return userValidResult;
        }

        List<CheckinRecord> records = baseMapper.selectList(new QueryWrapper<CheckinRecord>()
                .eq("user_id", userId)
                .orderByDesc("checkin_time")
        );
        if (records.isEmpty()) {
            // 缓存结果（添加Redis空指针保护）
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(cacheKey, 0, 2, TimeUnit.HOURS);
            }
            return Result.success(0);
        }

        // 3. 计算连续天数（优化逻辑，兼容补卡场景）
        int continuousDays = 1;
        LocalDate lastCheckinDate = records.get(0).getCheckinTime().toLocalDate();
        LocalDate today = LocalDate.now();

        // 若最近一次打卡不是今天，连续天数仅算1天
        if (!lastCheckinDate.isEqual(today)) {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(cacheKey, continuousDays, 2, TimeUnit.HOURS);
            }
            return Result.success(continuousDays);
        }

        // 遍历历史记录检查连续性
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

        // 4. 缓存结果（有效期2小时，添加Redis空指针保护）
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(cacheKey, continuousDays, 2, TimeUnit.HOURS);
        }
        log.info("用户[{}]连续打卡天数：{}（已缓存）", userId, continuousDays);
        return Result.success(continuousDays);
    }


    /**
     * 分页查询用户打卡记录
     */
    @Override
    public Result<?> getUserCheckinsByPage(Long userId, int pageNum, int pageSize) {
        // 校验用户
        Result<?> userValidResult = validateUserExists(userId);
        if (!userValidResult.isSuccess()) {
            return userValidResult;
        }

        // 校正分页参数
        pageNum = Math.max(pageNum, 1);
        pageSize = Math.max(1, Math.min(pageSize, 100)); // 限制每页1-100条

        // 分页查询
        IPage<CheckinRecord> page = new Page<>(pageNum, pageSize);
        IPage<CheckinRecord> resultPage = baseMapper.selectPage(page, new QueryWrapper<CheckinRecord>()
                .eq("user_id", userId)
                .orderByDesc("checkin_time")
        );

        log.info("用户[{}]分页查询：第{}页，共{}条记录", userId, pageNum, resultPage.getTotal());
        return Result.success(resultPage);
    }


    /**
     * 校验打卡地点是否在有效区域
     */
    @Override
    public Result<?> validateCheckinLocation(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return Result.error("打卡地点不能为空");
        }

        // 校验经纬度范围
        boolean isLatValid = latitude >= minLatitude && latitude <= maxLatitude;
        boolean isLngValid = longitude >= minLongitude && longitude <= maxLongitude;

        if (!isLatValid || !isLngValid) {
            return Result.error(String.format(
                    "打卡地点超出有效范围（纬度：%s-%s，经度：%s-%s），请在指定区域内打卡",
                    minLatitude, maxLatitude, minLongitude, maxLongitude
            ));
        }
        return Result.success("打卡地点有效");
    }


    /**
     * 补卡功能
     */
    @Override
    public Result<?> reissueCheckin(Long userId, LocalDate reissueDate, String reason) {
        // 1. 校验用户
        Result<?> userValidResult = validateUserExists(userId);
        if (!userValidResult.isSuccess()) {
            return userValidResult;
        }

        // 2. 校验补卡日期
        LocalDate today = LocalDate.now();
        if (reissueDate.isAfter(today)) {
            return Result.error("不能补未来的卡");
        }
        long daysBetween = ChronoUnit.DAYS.between(reissueDate, today);
        if (daysBetween > maxReissueDays) {
            return Result.error("仅支持" + maxReissueDays + "天内的补卡（当前超出" + daysBetween + "天）");
        }

        // 3. 校验该日期是否已打卡
        if (hasCheckedInDate(userId, reissueDate)) {
            return Result.error("该日期已打卡，无需补卡");
        }

        // 4. 校验本月补卡次数
        Long reissueCount = countMonthlyReissues(userId, today.getYear(), today.getMonthValue());
        if (reissueCount >= maxReissueCount) {
            return Result.error("本月补卡次数已达上限（最多" + maxReissueCount + "次，已使用" + reissueCount + "次）");
        }

        // 5. 校验补卡理由
        if (reason == null || reason.trim().isEmpty() || reason.length() > 500) {
            return Result.error("补卡理由不能为空且不超过500字");
        }

        // 6. 创建补卡记录
        CheckinRecord record = new CheckinRecord();
        record.setUserId(userId);
        record.setCheckinTime(reissueDate.atStartOfDay()); // 补卡日期
        record.setLocation("补卡");
        record.setStatus(2); // 2-补卡状态
        record.setIsReissue(1); // 1-补卡标记
        record.setReissueReason(reason); // 修复：使用补卡理由专用字段，而非remark
        record.setReissueTime(LocalDateTime.now()); // 补充：记录补卡提交时间

        try {
            baseMapper.insert(record);
            // 清除缓存（添加Redis空指针保护）
            if (redisTemplate != null) {
                redisTemplate.delete("checkin:continuous:" + userId);
                log.debug("用户[{}]补卡后，连续打卡缓存已清除", userId);
            } else {
                log.warn("RedisTemplate未注入，跳过补卡缓存清除操作");
            }
            log.info("用户[{}]补卡成功，日期：{}，本月剩余补卡次数：{}",
                    userId, reissueDate, maxReissueCount - reissueCount - 1);
            return Result.success("补卡成功", record);
        } catch (Exception e) {
            log.error("用户[{}]补卡失败", userId, e);
            return Result.error("补卡失败，请重试");
        }
    }


    /**
     * 月度打卡统计
     */
    @Override
    public Result<?> getMonthlyCheckinStats(Long userId, int year, int month) {
        // 校验用户
        Result<?> userValidResult = validateUserExists(userId);
        if (!userValidResult.isSuccess()) {
            return userValidResult;
        }

        // 当月时间范围
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.plusMonths(1).minusDays(1);
        int totalDays = lastDay.getDayOfMonth();

        // 查询当月记录
        List<CheckinRecord> records = baseMapper.selectList(new QueryWrapper<CheckinRecord>()
                .eq("user_id", userId)
                .ge("checkin_time", firstDay.atStartOfDay())
                .le("checkin_time", lastDay.atTime(23, 59, 59))
                .orderByAsc("checkin_time")
        );

        // 统计数据（去重，同一天多次打卡算1天）
        Set<LocalDate> checkinDates = records.stream()
                .map(r -> r.getCheckinTime().toLocalDate())
                .collect(Collectors.toSet());
        int checkinDays = checkinDates.size();
        double checkinRate = totalDays == 0 ? 0 : (double) checkinDays / totalDays * 100;

        // 组装结果
        Map<String, Object> stats = new HashMap<>();
        stats.put("year", year);
        stats.put("month", month);
        stats.put("totalDays", totalDays);
        stats.put("checkinDays", checkinDays);
        stats.put("checkinRate", String.format("%.1f%%", checkinRate));
        stats.put("checkinDates", checkinDates);
        stats.put("records", records);

        log.info("用户[{}]{}年{}月打卡统计：总天数{}，打卡天数{}，打卡率{}",
                userId, year, month, totalDays, checkinDays, stats.get("checkinRate"));
        return Result.success(stats);
    }


    /**
     * 查询指定日期未打卡的用户（管理员功能）
     */
    @Override
    public Result<?> getUncheckedUsers(LocalDate date) {
        // 所有用户
        List<User> allUsers = userMapper.selectList(null);
        if (allUsers.isEmpty()) {
            return Result.success("无用户数据");
        }

        // 已打卡用户ID
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.plusDays(1).atStartOfDay();
        List<Long> checkedUserIds = baseMapper.selectCheckedUserIds(startTime, endTime);

        // 筛选未打卡用户
        List<User> uncheckedUsers = allUsers.stream()
                .filter(user -> !checkedUserIds.contains(user.getId()))
                .peek(user -> user.setPassword(null)) // 隐藏密码，安全返回
                .collect(Collectors.toList());

        log.info("{}未打卡用户共{}人", date, uncheckedUsers.size());
        return Result.success(uncheckedUsers);
    }

    /**
     * 打卡核心统计（总天数+连续天数+本月统计）
     */
    @Override
    public Result<?> getCheckinStats(Long userId) {
        // 1. 校验用户
        Result<?> userValidResult = validateUserExists(userId);
        if (!userValidResult.isSuccess()) {
            return userValidResult;
        }

        // 2. 查询所有打卡记录
        List<CheckinRecord> records = baseMapper.selectList(new QueryWrapper<CheckinRecord>()
                .eq("user_id", userId)
                .orderByAsc("checkin_time")
        );

        // 3. 统计总打卡天数（去重）
        long totalDays = records.stream()
                .map(record -> record.getCheckinTime().toLocalDate())
                .distinct()
                .count();

        // 4. 统计连续打卡天数（复用已有逻辑）
        Result<?> continuousResult = getContinuousCheckinDays(userId);
        int continuousDays = continuousResult.isSuccess() ? (Integer) continuousResult.getData() : 0;

        // 5. 本月统计
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfMonth = now.withDayOfMonth(1);
        LocalDate lastDayOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        // 本月打卡天数（去重）
        long monthlyDays = records.stream()
                .map(record -> record.getCheckinTime().toLocalDate())
                .filter(date -> date.isAfter(firstDayOfMonth.minusDays(1)) && date.isBefore(lastDayOfMonth.plusDays(1)))
                .distinct()
                .count();

        // 本月打卡率（保留1位小数）
        double monthlyRate = 0.0;
        int totalDaysOfMonth = now.lengthOfMonth();
        if (totalDaysOfMonth > 0) {
            monthlyRate = Math.round(((double) monthlyDays / totalDaysOfMonth) * 100) / 100.0;
        }

        // 6. 组装统计结果
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDays", totalDays);
        stats.put("continuousDays", continuousDays);
        stats.put("userId", userId);
        stats.put("monthlyDays", monthlyDays);
        stats.put("monthlyRate", monthlyRate);
        stats.put("currentMonth", now.getMonthValue());

        log.info("用户[{}]打卡统计：总天数{}，连续天数{}，本月天数{}，本月打卡率{}",
                userId, totalDays, continuousDays, monthlyDays, monthlyRate);

        return Result.success(stats);
    }

    // ========== 私有工具方法 ==========

    /**
     * 校验用户是否存在
     */
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

    /**
     * 检查今日是否已打卡
     */
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

    /**
     * 检查指定日期是否已打卡（含补卡）
     */
    private boolean hasCheckedInDate(Long userId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        Long count = baseMapper.selectCount(new QueryWrapper<CheckinRecord>()
                .eq("user_id", userId)
                .ge("checkin_time", start)
                .lt("checkin_time", end)
        );
        return count > 0;
    }

    /**
     * 统计当月补卡次数（修复：查询补卡提交时间reissue_time）
     */
    private Long countMonthlyReissues(Long userId, int year, int month) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.plusMonths(1).minusDays(1);

        return baseMapper.selectCount(new QueryWrapper<CheckinRecord>()
                .eq("user_id", userId)
                .eq("is_reissue", 1)
                .ge("reissue_time", firstDay.atStartOfDay())
                .le("reissue_time", lastDay.atTime(23, 59, 59))
        );
    }
}