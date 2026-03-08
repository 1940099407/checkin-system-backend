package com.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 打卡记录实体类
 */
@Data
@TableName("checkin")
public class Checkin {
    @TableId(type = IdType.AUTO)
    private Long id;          // 主键ID
    private Long userId;      // 关联用户ID
    private LocalDateTime checkinTime; // 打卡时间
    private LocalDateTime checkoutTime; // 签退时间（支持上下班打卡）
    private String location;  // 打卡地点
    private String status;    // 状态：NORMAL/LATE/ABSENT/EARLY_LEAVE
    private String remark;    // 备注（如迟到原因）
}