package com.checkin.controller;

import com.checkin.entity.User;
import com.checkin.service.UserService;
import com.checkin.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // 标记为REST接口控制器
@RequestMapping("/user") // 统一接口前缀
public class UserController {

    @Autowired // 自动注入UserService
    private UserService userService;

    // 注册接口：POST /user/register
    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) { // @RequestBody接收JSON参数
        return userService.register(user);
    }

    // 登录接口：POST /user/login
    @PostMapping("/login")
    public Result<?> login(@RequestBody User user) { // 从请求体获取username和password
        return userService.login(user.getUsername(), user.getPassword());
    }
}