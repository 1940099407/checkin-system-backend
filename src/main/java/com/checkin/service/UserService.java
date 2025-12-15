package com.checkin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.checkin.entity.User;
import com.checkin.common.Result;

public interface UserService extends IService<User> {
    Result<?> register(User user);
    Result<?> login(String username, String password);
}