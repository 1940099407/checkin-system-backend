package com.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学习健康打卡核心实体类
 * 适配学习/阅读/冥想打卡场景，修正字段类型，补充前端所需字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("checkin") // 数据库表名（需与实际表名一致）
public class Checkin {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（关联用户表）
     */
    private Long userId;

    /**
     * 打卡时间（精确到时分秒）
     */
    private LocalDateTime checkinTime;

    /**
     * 学习打卡类型（学习/阅读/冥想/自定义）
     * 匹配前端下拉框选项，关联 CheckinTypeEnum 枚举
     */
    private String checkinType;

    /**
     * 打卡方式（普通文字/拍照打卡/定位打卡）
     */
    private String checkinWay;

    /**
     * 打卡备注（用户输入的学习收获/感受，最长500字）
     */
    private String remark;

    /**
     * 照片链接（拍照打卡时存储图片URL）
     */
    private String photoUrl;

    /**
     * 打卡地点（定位打卡时的地址描述）
     */
    private String location;

    /**
     * 纬度（定位打卡时的经纬度）
     */
    private Double latitude;

    /**
     * 经度（定位打卡时的经纬度）
     */
    private Double longitude;

    /**
     * 打卡状态（修正为 Integer 类型，解决 setStatus 类型不匹配问题）
     * 1=正常打卡（学习打卡仅保留该状态），2=补卡（已废弃）
     */
    private Integer status;

    /**
     * 是否补卡（0=否，1=是）- 学习打卡场景固定为0
     */
    private Integer isReissue;

    /**
     * 补卡理由（考勤场景字段，学习打卡无实际用途，保留兼容）
     */
    private String reissueReason;

    /**
     * 补卡日期（考勤场景字段，学习打卡无实际用途，保留兼容）
     */
    private LocalDate reissueDate;

    /**
     * 补卡提交时间（考勤场景字段，学习打卡无实际用途，保留兼容）
     */
    private LocalDateTime reissueTime;

    /**
     * 创建时间（自动填充）
     */
    private LocalDateTime createTime;

    /**
     * 更新时间（自动填充）
     */
    private LocalDateTime updateTime;
}