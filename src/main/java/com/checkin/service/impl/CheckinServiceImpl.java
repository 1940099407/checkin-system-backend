package com.checkin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import java.time.LocalDateTime;
import java.util.*;

/**
 * 学习健康打卡服务实现类
 * 实现createStudyCheckin方法，解决类型不匹配+方法找不到错误
 */
@Service
public class CheckinServiceImpl extends ServiceImpl<CheckinMapper, Checkin> implements CheckinService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 学习打卡核心实现（返回String，匹配接口声明）
     */
    @Override
    public String createStudyCheckin(Checkin checkin) {
        // 1. 基础校验
        if (checkin.getUserId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        if (checkin.getCheckinType() == null || checkin.getCheckinType().trim().isEmpty()) {
            throw new RuntimeException("打卡类型不能为空（学习/阅读/冥想/自定义）");
        }
        // 2. 校验打卡类型有效性
        List<String> validTypes = Arrays.asList("学习", "阅读", "冥想", "自定义");
        if (!validTypes.contains(checkin.getCheckinType())) {
            throw new RuntimeException("打卡类型仅支持：学习、阅读、冥想、自定义");
        }
        // 3. 校验今日是否重复打卡
        LocalDate today = LocalDate.now();
        long count = this.count(new QueryWrapper<Checkin>()
                .eq("user_id", checkin.getUserId())
                .ge("checkin_time", today.atStartOfDay())
                .lt("checkin_time", today.plusDays(1).atStartOfDay()));
        if (count > 0) {
            throw new RuntimeException("今日已完成" + checkin.getCheckinType() + "打卡，请勿重复提交");
        }
        // 4. 填充默认值
        if (checkin.getCheckinWay() == null) {
            checkin.setCheckinWay("普通文字"); // 默认打卡方式
        }
        if (checkin.getStatus() == null) {
            checkin.setStatus(1); // 默认正常状态（Integer类型）
        }
        if (checkin.getCheckinTime() == null) {
            checkin.setCheckinTime(LocalDateTime.now());
        }
        // 5. 保存记录
        boolean saveSuccess = this.save(checkin);
        if (!saveSuccess) {
            throw new RuntimeException("打卡记录保存失败");
        }
        return checkin.getCheckinType() + "打卡成功";
    }

    @Override
    public Checkin getCheckinByUserAndDate(Long userId, LocalDate date) {
        return this.getOne(new QueryWrapper<Checkin>()
                .eq("user_id", userId)
                .ge("checkin_time", date.atStartOfDay())
                .lt("checkin_time", date.plusDays(1).atStartOfDay())
                .last("LIMIT 1"));
    }

    @Override
    public List<Checkin> listCheckinByUserAndDateRange(Long userId, LocalDate start, LocalDate end) {
        return this.list(new QueryWrapper<Checkin>()
                .eq("user_id", userId)
                .ge("checkin_time", start.atStartOfDay())
                .le("checkin_time", end.atTime(23, 59, 59))
                .orderByDesc("checkin_time"));
    }

    @Override
    public Map<String, Object> statMonthlyCheckin(Long userId, int year, int month) {
        Map<String, Object> stat = new HashMap<>();
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.plusMonths(1).minusDays(1);

        // 统计总打卡天数
        long totalDays = this.count(new QueryWrapper<Checkin>()
                .eq("user_id", userId)
                .ge("checkin_time", firstDay.atStartOfDay())
                .le("checkin_time", lastDay.atTime(23, 59, 59)));
        stat.put("totalDays", totalDays);

        // 按类型统计
        long studyCount = this.count(new QueryWrapper<Checkin>()
                .eq("user_id", userId)
                .eq("checkin_type", "学习")
                .ge("checkin_time", firstDay.atStartOfDay())
                .le("checkin_time", lastDay.atTime(23, 59, 59)));
        stat.put("studyCount", studyCount);

        long readCount = this.count(new QueryWrapper<Checkin>()
                .eq("user_id", userId)
                .eq("checkin_type", "阅读")
                .ge("checkin_time", firstDay.atStartOfDay())
                .le("checkin_time", lastDay.atTime(23, 59, 59)));
        stat.put("readCount", readCount);

        long meditationCount = this.count(new QueryWrapper<Checkin>()
                .eq("user_id", userId)
                .eq("checkin_type", "冥想")
                .ge("checkin_time", firstDay.atStartOfDay())
                .le("checkin_time", lastDay.atTime(23, 59, 59)));
        stat.put("meditationCount", meditationCount);

        long customCount = this.count(new QueryWrapper<Checkin>()
                .eq("user_id", userId)
                .eq("checkin_type", "自定义")
                .ge("checkin_time", firstDay.atStartOfDay())
                .le("checkin_time", lastDay.atTime(23, 59, 59)));
        stat.put("customCount", customCount);

        // 打卡率
        int totalDaysOfMonth = lastDay.getDayOfMonth();
        double rate = totalDaysOfMonth == 0 ? 0 : (double) totalDays / totalDaysOfMonth * 100;
        stat.put("checkinRate", String.format("%.1f%%", rate));

        return stat;
    }

    @Override
    public IPage<Checkin> getAllCheckinByPage(Page<Checkin> page, String username, Integer status) {
        QueryWrapper<Checkin> wrapper = new QueryWrapper<>();
        // 按用户名筛选（关联用户表）
        if (username != null && !username.trim().isEmpty()) {
            List<User> users = userMapper.selectList(new QueryWrapper<User>().like("username", username));
            if (!users.isEmpty()) {
                List<Long> userIds = users.stream().map(User::getId).toList();
                wrapper.in("user_id", userIds);
            } else {
                // 无匹配用户，返回空
                return new Page<>();
            }
        }
        // 按状态筛选（Integer类型，匹配实体类）
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("checkin_time");
        return this.page(page, wrapper);
    }
}