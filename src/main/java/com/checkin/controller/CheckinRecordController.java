package com.checkin.controller;

// 项目自定义类导入（根据你的实际包路径调整）
import com.checkin.common.Result;
import com.checkin.entity.CheckinRecord;
import com.checkin.enums.CheckinStatusEnum;
import com.checkin.exception.BusinessException;
import com.checkin.service.CheckinRecordService;

// Lombok日志
import lombok.extern.slf4j.Slf4j;

// Spring核心注解
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated; // @Validated的正确包
import org.springframework.web.bind.annotation.*;

// 参数校验注解（仅保留不报错的@Min/@NotNull，删除@Range）
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 打卡记录控制器（最终可运行版）
 * 修复点：移除@Range注解（解决报错），替换为手动参数校验；保留@Min/@NotNull；修复注解/变量名问题
 */
@Slf4j
@RestController
@RequestMapping("/checkin")
@Validated // 开启方法参数校验（保证@NotNull/@Min生效）
public class CheckinRecordController {

    @Autowired
    private CheckinRecordService checkinRecordService;

    // 1. 创建打卡记录（核心接口：参数校验、移除时间设置、替换硬编码）
    @PostMapping
    public Result<?> createCheckin(@RequestBody CheckinRecord record) {
        // 手动校验用户ID非空
        if (record.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        log.info("开始创建打卡记录，用户ID：{}", record.getUserId());

        // 替换硬编码：用枚举设置打卡状态
        record.setStatus(CheckinStatusEnum.NORMAL.getCode());

        // 打卡时间由Service层设置（分层解耦）
        Result<?> result = checkinRecordService.createCheckin(record);

        log.info("打卡记录创建成功，用户ID：{}", record.getUserId());
        return result;
    }

    // 2. 获取用户所有打卡记录（保留@NotNull校验userId）
    @GetMapping("/user/{userId}")
    public Result<?> getUserCheckins(
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId) {
        log.info("查询用户所有打卡记录，用户ID：{}", userId);
        return checkinRecordService.getUserCheckins(userId);
    }

    // 3. 获取用户今日打卡状态（保留@NotNull校验userId）
    @GetMapping("/today/{userId}")
    public Result<?> getTodayCheckinStatus(
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId) {
        log.info("查询用户今日打卡状态，用户ID：{}", userId);
        return checkinRecordService.getTodayCheckinStatus(userId);
    }

    // 4. 获取用户连续打卡天数（保留@NotNull校验userId）
    @GetMapping("/continuous/{userId}")
    public Result<?> getContinuousCheckinDays(
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId) {
        log.info("查询用户连续打卡天数，用户ID：{}", userId);
        return checkinRecordService.getContinuousCheckinDays(userId);
    }

    // 5. 分页查询用户打卡记录（移除@Range，替换为手动校验pageSize）
    @GetMapping("/user/{userId}/page")
    public Result<?> getUserCheckinsByPage(
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) { // 删掉@Range注解

        // 手动校验pageSize范围（替代@Range）
        if (pageSize < 1 || pageSize > 100) {
            throw new BusinessException("页大小需在1-100之间");
        }

        log.info("分页查询打卡记录，用户ID：{}，页码：{}，页大小：{}", userId, pageNum, pageSize);
        return checkinRecordService.getUserCheckinsByPage(userId, pageNum, pageSize);
    }

    // 6. 校验打卡地点合法性（移除@Range，替换为手动校验经纬度）
    @GetMapping("/location/validate")
    public Result<?> validateLocation(
            @RequestParam Double latitude,
            @RequestParam Double longitude) { // 删掉所有@Range注解

        // 手动校验纬度范围（替代@Range(min = -90, max = 90)）
        if (latitude == null || latitude < -90 || latitude > 90) {
            throw new BusinessException("纬度需在-90~90之间");
        }
        // 手动校验经度范围（替代@Range(min = -180, max = 180)）
        if (longitude == null || longitude < -180 || longitude > 180) {
            throw new BusinessException("经度需在-180~180之间");
        }

        log.info("校验打卡地点，纬度：{}，经度：{}", latitude, longitude);
        return checkinRecordService.validateCheckinLocation(latitude, longitude);
    }

    // 7. 获取用户打卡统计数据（保留@NotNull校验userId）
    @GetMapping("/stats/{userId}") // 修复注解：原@SetMapping→@GetMapping
    public Result<?> getCheckinStats(
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId) {
        log.info("查询用户打卡统计数据，用户ID：{}", userId);
        return checkinRecordService.getCheckinStats(userId);
    }
}