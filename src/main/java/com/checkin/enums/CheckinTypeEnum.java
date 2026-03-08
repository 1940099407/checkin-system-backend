package com.checkin.enums;

/**
 * 学习健康打卡类型枚举
 * 解决语法错误，避免与entity下Checkin类命名冲突
 */
public enum CheckinTypeEnum {
    // 枚举项：枚举值(中文描述)
    STUDY("学习"),
    READ("阅读"),
    MEDITATION("冥想"),
    CUSTOM("自定义");

    // 中文描述字段
    private final String desc;

    // 构造方法（枚举类构造方法必须私有，默认private）
    CheckinTypeEnum(String desc) {
        this.desc = desc;
    }

    // 获取中文描述的方法
    public String getDesc() {
        return desc;
    }
}