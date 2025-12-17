package com.checkin.controller;

import com.checkin.entity.User;
import com.checkin.service.UserService;
import com.checkin.common.Result;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.security.core.context.SecurityContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
@RestController // 标记为REST接口控制器
@RequestMapping("/user") // 统一接口前缀
public class UserController {

    @Autowired // 自动注入UserService
    private UserService userService;

    // 注册接口：POST /user/register
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody User user) { // @RequestBody接收JSON参数
        return userService.register(user);
    }

    // 登录接口：POST /user/login
    @PostMapping("/login")
    public Result<?> login(@RequestBody User user) { // 从请求体获取username和password
        return userService.login(user.getUsername(), user.getPassword());
    }

    @GetMapping("/info")
    public Result<?> getUserInfo() {
        // 从SecurityContext获取当前登录用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            return Result.error("用户不存在");
        }
        // 隐藏密码，返回安全信息
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/update")
    public Result<?> updateUser(@RequestBody User user) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.getOne(new QueryWrapper<User>().eq("username", username));
        if (currentUser == null) {
            return Result.error("用户不存在");
        }
        // 仅允许修改用户名（需查重）和角色（可选，根据业务需求）
        if (user.getUsername() != null && !user.getUsername().equals(currentUser.getUsername())) {
            User existing = userService.getOne(new QueryWrapper<User>().eq("username", user.getUsername()));
            if (existing != null) {
                return Result.error("用户名已存在");
            }
            currentUser.setUsername(user.getUsername());
        }
        userService.updateById(currentUser);
        return Result.success("更新成功");
    }
}