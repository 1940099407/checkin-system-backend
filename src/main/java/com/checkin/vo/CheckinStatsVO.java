package com.checkin.vo;

import lombok.Data;

@Data
public class CheckinStatsVO {
    private int totalCount; // 总打卡次数
    private int continuousDays; // 连续打卡天数
    private int monthlyCount; // 本月打卡次数
    private double monthlyRate; // 本月打卡率（0-1）
}