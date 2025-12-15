package com.checkin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.checkin.entity.User;
import com.checkin.common.Result;
import com.checkin.mapper.UserMapper;
import com.checkin.service.UserService;
import org.springframework.stereotype.Service;

@Service // 必须加，让Spring创建Bean
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public Result<?> register(User user) {
        // 临时实现，后续替换为你的业务逻辑
        return Result.success("注册成功");
    }

    @Override
    public Result<?> login(String username, String password) {
        // 临时实现，后续替换为你的业务逻辑
        return Result.success("登录成功");
    }
}