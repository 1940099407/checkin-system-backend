// src/main/java/com/checkin/entity/User.java
package com.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "用户名不能为空") // 非空校验
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间") // 长度校验
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    private String password;

    // 关键修改：将Integer改为String，匹配数据库的varchar类型
    private String role;

    // 补充数据库中存在的avatar字段（必填，否则查询时会报未知列错误）
    private String avatar;

    private LocalDateTime createTime;
}