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
@RequestMapping("/user")
@Tag(name = "用户管理", description = "用户登录、注册接口")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册，密码自动加密存储")
    public Result<?> register(
            @Parameter(description = "注册信息（必填username/password）", required = true)
            @RequestBody User user) {
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            boolean saveSuccess = userService.save(user);
            return saveSuccess ? Result.success("注册成功") : Result.error(500, "注册失败");
        } catch (Exception e) {
            log.error("注册异常", e);
            return Result.error(500, "注册异常：" + e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "登录成功返回JWT令牌")
    public Result<?> login(
            @Parameter(description = "登录信息（username/password）", required = true)
            @RequestBody User user) {
        try {
            User dbUser = userService.getByUsername(user.getUsername());
            if (dbUser == null) {
                return Result.error(401, "用户名不存在");
            }
            if (!passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
                return Result.error(401, "密码错误");
            }
            String token = jwtUtils.generateToken(dbUser.getUsername());
            return Result.success("登录成功", token);
        } catch (Exception e) {
            log.error("登录异常", e);
            return Result.error(500, "登录异常：" + e.getMessage());
        }
    }
}