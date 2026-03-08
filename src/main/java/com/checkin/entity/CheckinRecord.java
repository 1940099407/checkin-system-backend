package com.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 学习健康打卡记录实体（修正status为Integer类型，补充学习打卡字段）
 */
@Data
@TableName("checkin_record") // 数据库表名
public class CheckinRecord {
    @TableId(type = IdType.AUTO)
    private Long id; // 主键ID

    private Long userId; // 用户ID

    private LocalDateTime checkinTime; // 打卡时间

    // 新增：学习打卡类型（学习/阅读/冥想/自定义）
    private String checkinType;

    // 新增：打卡方式（普通文字/拍照打卡/定位打卡）
    private String checkinWay;

    // 新增：打卡备注（用户输入的收获/感受）
    private String remark;

    // 新增：照片链接（拍照打卡时使用）
    private String photoUrl;

    private String location; // 打卡地点（定位打卡时用）

    private Double latitude; // 纬度

    private Double longitude; // 经度

    // 修正：status为Integer类型（1=正常打卡，2=补卡，改造后仅用1）
    private Integer status;

    private Integer isReissue; // 是否补卡（0=否，1=是）

    private String reissueReason; // 补卡理由（改造后无用，保留兼容）

    private LocalDate reissueDate; // 补卡日期（改造后无用，保留兼容）

    private LocalDateTime reissueTime; // 补卡提交时间（改造后无用，保留兼容）
}