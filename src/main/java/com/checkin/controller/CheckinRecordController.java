package com.checkin.controller;

import com.checkin.common.Result;
import com.checkin.entity.CheckinRecord;
import com.checkin.service.CheckinRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/checkin")
public class CheckinRecordController {

    @Autowired
    private CheckinRecordService checkinRecordService;

    // 创建打卡记录
    @PostMapping
    public Result<?> createCheckin(@RequestBody CheckinRecord record) {
        record.setCheckinTime(LocalDateTime.now());
        record.setStatus(1); // 默认正常状态
        return checkinRecordService.createCheckin(record);
    }

    // 获取用户打卡记录
    @GetMapping("/user/{userId}")
    public Result<?> getUserCheckins(@PathVariable Long userId) {
        return checkinRecordService.getUserCheckins(userId);
    }

    // 获取今日打卡状态
    @GetMapping("/today/{userId}")
    public Result<?> getTodayCheckinStatus(@PathVariable Long userId) {
        return checkinRecordService.getTodayCheckinStatus(userId);
    }
    // 新增接口：连续打卡天数
    @GetMapping("/continuous/{userId}")
    public Result<?> getContinuousCheckinDays(@PathVariable Long userId) {
        return checkinRecordService.getContinuousCheckinDays(userId);
    }

    // 新增接口：分页查询打卡记录
    @GetMapping("/user/{userId}/page")
    public Result<?> getUserCheckinsByPage(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return checkinRecordService.getUserCheckinsByPage(userId, pageNum, pageSize);
    }

    // 新增接口：校验打卡地点（可选，供前端提前验证）
    @GetMapping("/location/validate")
    public Result<?> validateLocation(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        return checkinRecordService.validateCheckinLocation(latitude, longitude);
    }

    // 控制器添加接口（CheckinRecordController）
    @GetMapping("/stats/{userId}")
    public Result<?> getCheckinStats(@PathVariable Long userId) {
        return checkinRecordService.getCheckinStats(userId);
    }
}