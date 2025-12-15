package com.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("checkin_record")
public class CheckinRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId; // 关联用户ID（外键）
    private LocalDateTime checkinTime; // 打卡时间
    private String location; // 打卡地点（可选）
    private Integer status; // 状态：1-正常，0-异常
}