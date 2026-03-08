package com.checkin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.checkin.entity.Checkin;
import com.checkin.entity.User;
import com.checkin.mapper.CheckinMapper;
import com.checkin.mapper.UserMapper;
import com.checkin.service.CheckinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 打卡服务实现类（完整CRUD）
 */
@Service
public class CheckinServiceImpl extends ServiceImpl<CheckinMapper, Checkin> implements CheckinService {

    // 打卡时间阈值：9点后迟到，18点前早退
    private static final LocalTime CHECKIN_LATE = LocalTime.of(9, 0);
    private static final LocalTime CHECKOUT_EARLY = LocalTime.of(18, 0);

    @Autowired
    private UserMapper userMapper;

    /**
     * 上班打卡核心逻辑
     */
    @Override
    public String doCheckin(Long userId, String location) {
        LocalDate today = LocalDate.now();
        Checkin existCheckin = getCheckinByUserAndDate(userId, today);
        if (existCheckin != null) {
            return "今日已打卡，无需重复打卡";
        }

        Checkin checkin = new Checkin();
        checkin.setUserId(userId);
        checkin.setCheckinTime(java.time.LocalDateTime.now());
        checkin.setLocation(location);
        checkin.setStatus(LocalTime.now().isAfter(CHECKIN_LATE) ? "LATE" : "NORMAL");
        save(checkin);
        return "打卡成功";
    }

    /**
     * 下班签退核心逻辑
     */
    @Override
    public String doCheckout(Long userId, String location) {
        LocalDate today = LocalDate.now();
        Checkin checkin = getCheckinByUserAndDate(userId, today);
        if (checkin == null) {
            return "今日未打卡，无法签退";
        }
        if (checkin.getCheckoutTime() != null) {
            return "今日已签退，无需重复操作";
        }

        checkin.setCheckoutTime(java.time.LocalDateTime.now());
        checkin.setStatus(LocalTime.now().isBefore(CHECKOUT_EARLY) ? "EARLY_LEAVE" : checkin.getStatus());
        updateById(checkin);
        return "签退成功";
    }

    /**
     * 查询用户指定日期打卡记录
     */
    @Override
    public Checkin getCheckinByUserAndDate(Long userId, LocalDate date) {
        LambdaQueryWrapper<Checkin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Checkin::getUserId, userId)
                .apply("DATE(checkin_time) = {0}", date.toString());
        return getOne(wrapper);
    }

    /**
     * 查询用户指定时间段打卡记录
     */
    @Override
    public List<Checkin> listCheckinByUserAndDateRange(Long userId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<Checkin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Checkin::getUserId, userId)
                // 核心修正：apply 替代 between 处理数据库函数
                .apply("DATE(checkin_time) BETWEEN {0} AND {1}", start.toString(), end.toString())
                .orderByDesc(Checkin::getCheckinTime);
        return list(wrapper);
    }

    /**
     * 月度打卡统计
     */
    @Override
    public Map<String, Object> statMonthlyCheckin(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<Checkin> list = listCheckinByUserAndDateRange(userId, start, end);
        int total = list.size();
        int late = 0;
        int earlyLeave = 0;
        int normal = 0;

        for (Checkin checkin : list) {
            String status = checkin.getStatus();
            if ("LATE".equals(status)) {
                late++;
            } else if ("EARLY_LEAVE".equals(status)) {
                earlyLeave++;
            } else if ("NORMAL".equals(status)) {
                normal++;
            }
        }

        Map<String, Object> stat = new HashMap<>();
        stat.put("totalDays", total);
        stat.put("lateDays", late);
        stat.put("earlyLeaveDays", earlyLeave);
        stat.put("normalDays", normal);
        return stat;
    }

    /**
     * 管理员分页查询所有打卡记录（支持用户名/状态筛选）
     */
    @Override
    public IPage<Checkin> getAllCheckinByPage(Page<Checkin> page, String username, String status) {
        LambdaQueryWrapper<Checkin> queryWrapper = new LambdaQueryWrapper<>();

        // 按用户名筛选（关联User表）
        if (username != null && !username.isEmpty()) {
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
            if (user != null) {
                queryWrapper.eq(Checkin::getUserId, user.getId());
            } else {
                // 无该用户，返回空分页
                return new Page<>();
            }
        }

        // 按打卡状态筛选
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(Checkin::getStatus, status);
        }

        // 按打卡时间降序排序
        queryWrapper.orderByDesc(Checkin::getCheckinTime);

        // 执行分页查询
        return this.page(page, queryWrapper);
    }
}