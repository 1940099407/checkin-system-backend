package com.checkin.controller;

import com.checkin.common.Result;
import com.checkin.entity.User;
import com.checkin.service.UserService;
import com.checkin.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user") // 路径必须是“/user”（配合context-path后实际是/api/user）
@Tag(name = "用户管理", description = "用户登录、注册接口") // Swagger分类注解（必须加）
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;

    // 注册接口：必须加@Operation（Swagger识别接口的关键）
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册，密码自动加密存储")
    public Result<?> register(
            @Parameter(description = "注册信息（必填username/password）", required = true)
            @RequestBody User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userService.save(user) ? Result.success("注册成功") : Result.error(500, "注册失败");
    }

    // 登录接口：必须加@Operation
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "登录成功返回JWT令牌")
    public Result<?> login(
            @Parameter(description = "登录信息（username/password）", required = true)
            @RequestBody User user) {
        User dbUser = userService.getByUsername(user.getUsername());
        if (dbUser == null) return Result.error(401, "用户名不存在");
        if (!passwordEncoder.matches(user.getPassword(), dbUser.getPassword()))
            return Result.error(401, "密码错误");
        return Result.success(jwtUtils.generateToken(dbUser.getUsername()));
    }
}