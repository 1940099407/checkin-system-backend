package com.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableField;
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
    // 新增字段：纬度和经度
    private Double latitude; // 打卡地点纬度
    private Double longitude; // 打卡地点经度

    // 新增补卡相关字段
    @TableField(value = "is_reissue") // 是否补卡（0-正常，1-补卡）
    private Integer isReissue;
    @TableField(value = "reissue_time") // 补卡提交时间（null表示非补卡）
    private LocalDateTime reissueTime;
    @TableField(value = "reissue_reason") // 补卡理由
    private String reissueReason;

    public void setRemark(String reason) {
    }
}