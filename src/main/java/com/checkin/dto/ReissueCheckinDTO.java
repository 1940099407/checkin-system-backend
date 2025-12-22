package com.checkin.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * 补打卡请求参数DTO
 */
@Data
public class ReissueCheckinDTO {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 补卡日期（格式：yyyy-MM-dd，如2025-12-21）
     */
    private LocalDate reissueDate;

    /**
     * 补卡理由（最多500字）
     */
    private String reason;
}