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

// Swagger注解（前端联调必备）
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 打卡记录控制器（适配前端对接版）
 * 核心修改：统一接口前缀/api/checkin、补充Swagger注解、优化日志/参数提示、保留所有原有业务逻辑
 */
@Slf4j
@RestController
@RequestMapping("/checkin") // 统一前缀：适配前端对接规范
@Validated // 开启方法参数校验（保证@NotNull/@Min生效）
@Tag(name = "打卡记录管理", description = "打卡创建、查询、统计、地点校验等接口") // Swagger分类注解
public class CheckinRecordController {

    @Autowired
    private CheckinRecordService checkinRecordService;

    // 1. 创建打卡记录（核心接口：参数校验、移除时间设置、替换硬编码）
    @PostMapping
    @Operation(summary = "创建打卡记录", description = "用户打卡核心接口，自动设置正常打卡状态，打卡时间由服务层处理")
    public Result<?> createCheckin(
            @Parameter(description = "打卡记录信息（必填：userId，选填：location等）", required = true)
            @RequestBody CheckinRecord record) {
        // 手动校验用户ID非空
        if (record.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        log.info("【打卡创建】开始处理用户打卡请求，用户ID：{}", record.getUserId());

        // 替换硬编码：用枚举设置打卡状态
        record.setStatus(CheckinStatusEnum.NORMAL.getCode());

        // 打卡时间由Service层设置（分层解耦）
        Result<?> result = checkinRecordService.createCheckin(record);

        log.info("【打卡创建】用户打卡记录创建成功，用户ID：{}", record.getUserId());
        return result;
    }

    // 2. 获取用户所有打卡记录（保留@NotNull校验userId）
    @GetMapping("/user/{userId}")
    @Operation(summary = "查询用户所有打卡记录", description = "返回用户全部打卡记录列表，按打卡时间倒序")
    public Result<?> getUserCheckins(
            @Parameter(description = "用户ID", required = true)
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId) {
        log.info("【打卡查询】查询用户所有打卡记录，用户ID：{}", userId);
        return checkinRecordService.getUserCheckins(userId);
    }

    // 3. 获取用户今日打卡状态（保留@NotNull校验userId）
    @GetMapping("/today/{userId}")
    @Operation(summary = "查询用户今日打卡状态", description = "返回用户今日是否已打卡（true/false）及打卡时间")
    public Result<?> getTodayCheckinStatus(
            @Parameter(description = "用户ID", required = true)
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId) {
        log.info("【打卡状态】查询用户今日打卡状态，用户ID：{}", userId);
        return checkinRecordService.getTodayCheckinStatus(userId);
    }

    // 4. 获取用户连续打卡天数（保留@NotNull校验userId）
    @GetMapping("/continuous/{userId}")
    @Operation(summary = "查询用户连续打卡天数", description = "返回用户当前连续打卡的天数（无打卡则返回0）")
    public Result<?> getContinuousCheckinDays(
            @Parameter(description = "用户ID", required = true)
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId) {
        log.info("【打卡统计】查询用户连续打卡天数，用户ID：{}", userId);
        return checkinRecordService.getContinuousCheckinDays(userId);
    }

    // 5. 分页查询用户打卡记录（移除@Range，替换为手动校验pageSize）
    @GetMapping("/user/{userId}/page")
    @Operation(summary = "分页查询用户打卡记录", description = "分页返回用户打卡记录，页码≥1，页大小1-100")
    public Result<?> getUserCheckinsByPage(
            @Parameter(description = "用户ID", required = true)
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId,
            @Parameter(description = "页码（默认1）", required = false)
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于1") int pageNum,
            @Parameter(description = "页大小（默认10，范围1-100）", required = false)
            @RequestParam(defaultValue = "10") int pageSize) { // 删掉@Range注解

        // 手动校验pageSize范围（替代@Range）
        if (pageSize < 1 || pageSize > 100) {
            throw new BusinessException("页大小需在1-100之间");
        }

        log.info("【打卡分页】分页查询打卡记录，用户ID：{}，页码：{}，页大小：{}", userId, pageNum, pageSize);
        return checkinRecordService.getUserCheckinsByPage(userId, pageNum, pageSize);
    }

    // 6. 校验打卡地点合法性（移除@Range，替换为手动校验经纬度）
    @GetMapping("/location/validate")
    @Operation(summary = "校验打卡地点合法性", description = "校验经纬度是否在有效范围（纬度-90~90，经度-180~180）")
    public Result<?> validateLocation(
            @Parameter(description = "纬度（范围-90~90）", required = true)
            @RequestParam Double latitude,
            @Parameter(description = "经度（范围-180~180）", required = true)
            @RequestParam Double longitude) { // 删掉所有@Range注解

        // 手动校验纬度范围（替代@Range(min = -90, max = 90)）
        if (latitude == null || latitude < -90 || latitude > 90) {
            throw new BusinessException("纬度需在-90~90之间");
        }
        // 手动校验经度范围（替代@Range(min = -180, max = 180)）
        if (longitude == null || longitude < -180 || longitude > 180) {
            throw new BusinessException("经度需在-180~180之间");
        }

        log.info("【地点校验】校验打卡地点，纬度：{}，经度：{}", latitude, longitude);
        return checkinRecordService.validateCheckinLocation(latitude, longitude);
    }

    // 7. 获取用户打卡统计数据（保留@NotNull校验userId）
    @GetMapping("/stats/{userId}") // 修复注解：原@SetMapping→@GetMapping
    @Operation(summary = "查询用户打卡统计数据", description = "返回总打卡天数、连续天数、本月打卡天数/打卡率等统计信息")
    public Result<?> getCheckinStats(
            @Parameter(description = "用户ID", required = true)
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId) {
        log.info("【打卡统计】查询用户完整打卡统计数据，用户ID：{}", userId);
        return checkinRecordService.getCheckinStats(userId);
    }
}