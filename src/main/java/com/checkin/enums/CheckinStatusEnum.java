package com.checkin.enums;

import lombok.Getter;

// 打卡状态枚举（替代硬编码1=正常）
@Getter
public enum CheckinStatusEnum {
    NORMAL(1, "正常打卡"); // 先只定义核心状态，后续可加迟到/补卡

    private final Integer code;
    private final String desc;

    CheckinStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}