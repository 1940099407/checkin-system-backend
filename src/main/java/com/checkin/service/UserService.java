package com.checkin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.checkin.entity.User;
import com.checkin.common.Result;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public interface UserService extends IService<User> {
    Result<?> register(User user);
    Result<?> login(String username, String password);

    User getByUsername(@NotBlank(message = "用户名不能为空") @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间") String username);
}