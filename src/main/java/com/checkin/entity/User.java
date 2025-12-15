package com.checkin.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("user") // 确保数据库存在该表
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password; // 补全password字段
    private Integer role;
    private LocalDateTime createTime;
}