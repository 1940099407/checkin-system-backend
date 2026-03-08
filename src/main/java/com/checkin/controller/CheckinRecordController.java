package com.checkin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.checkin.common.Result;
import com.checkin.entity.CheckinRecord;
import com.checkin.entity.User;
import com.checkin.service.CheckinRecordService;
import com.checkin.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 学习健康打卡控制器（修正setStatus调用，适配学习打卡接口）
 */
@Slf4j
@RestController
@RequestMapping("/api/checkin-record")
public class CheckinRecordController {

    @Autowired
    private CheckinRecordService checkinRecordService;

    @Autowired
    private UserService userService;

    // 获取当前登录用户
    private User getCurrentUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userService.getByUsername(userDetails.getUsername());
    }

    /**
     * 学习打卡提交（核心接口，适配前端表单）
     */
    @PostMapping("/create")
    public Result<?> createCheckin(Authentication authentication,
                                   @RequestBody CheckinRecord record) {
        try {
            User currentUser = getCurrentUser(authentication);
            record.setUserId(currentUser.getId()); // 绑定当前用户ID
            Result<?> result = checkinRecordService.createCheckin(record);
            return result;
        } catch (Exception e) {
            log.error("打卡提交失败", e);
            return Result.error("打卡失败：" + e.getMessage());
        }
    }

    /**
     * 查询今日打卡状态
     */
    @GetMapping("/today-status")
    public Result<?> getTodayStatus(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return checkinRecordService.getTodayCheckinStatus(currentUser.getId());
    }

    /**
     * 查询用户所有打卡记录
     */
    @GetMapping("/user-all")
    public Result<?> getUserAllCheckins(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return checkinRecordService.getUserCheckins(currentUser.getId());
    }

    /**
     * 分页查询用户打卡记录
     */
    @GetMapping("/user-page")
    public Result<?> getUserCheckinsByPage(Authentication authentication,
                                           @RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize) {
        User currentUser = getCurrentUser(authentication);
        return checkinRecordService.getUserCheckinsByPage(currentUser.getId(), pageNum, pageSize);
    }

    /**
     * 统计连续打卡天数
     */
    @GetMapping("/continuous-days")
    public Result<?> getContinuousDays(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return checkinRecordService.getContinuousCheckinDays(currentUser.getId());
    }

    /**
     * 月度打卡统计（按学习类型统计）
     */
    @GetMapping("/monthly-stats")
    public Result<?> getMonthlyStats(Authentication authentication,
                                     @RequestParam int year,
                                     @RequestParam int month) {
        User currentUser = getCurrentUser(authentication);
        return checkinRecordService.getMonthlyCheckinStats(currentUser.getId(), year, month);
    }

    /**
     * 核心打卡统计（总天数+连续天数+本月统计）
     */
    @GetMapping("/core-stats")
    public Result<?> getCoreStats(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return checkinRecordService.getCheckinStats(currentUser.getId());
    }

    /**
     * 【兼容修正】更新打卡状态（解决setStatus编译错误）
     * 学习打卡场景下仅支持设置1=正常打卡
     */
    @PostMapping("/update-status/{id}")
    public Result<?> updateStatus(@PathVariable Long id,
                                  @RequestParam String statusStr) {
        try {
            CheckinRecord checkin = checkinRecordService.getById(id);
            if (checkin == null) {
                return Result.error("打卡记录不存在");
            }
            // 修正：String转Integer，避免类型不匹配
            Integer status = Integer.parseInt(statusStr);
            // 学习打卡仅允许设置1=正常打卡
            if (status != 1) {
                return Result.error("学习打卡仅支持设置正常状态（1）");
            }
            checkin.setStatus(status); // 此时参数类型匹配，无编译错误
            checkinRecordService.updateById(checkin);
            return Result.success("状态更新成功");
        } catch (NumberFormatException e) {
            return Result.error("状态值必须为数字（1=正常打卡）");
        } catch (Exception e) {
            log.error("更新状态失败", e);
            return Result.error("状态更新失败：" + e.getMessage());
        }
    }
}