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
}