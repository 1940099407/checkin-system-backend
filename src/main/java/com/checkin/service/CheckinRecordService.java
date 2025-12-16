package com.checkin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.checkin.common.Result;
import com.checkin.entity.CheckinRecord;

public interface CheckinRecordService extends IService<CheckinRecord> {
    Result<?> createCheckin(CheckinRecord record);
    Result<?> getUserCheckins(Long userId);
    Result<?> getTodayCheckinStatus(Long userId);
}