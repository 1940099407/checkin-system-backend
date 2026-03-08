package com.checkin.controller;

import com.checkin.common.Result;
import com.checkin.entity.User;
import com.checkin.service.UserService;
import com.checkin.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 用户管理控制器（登录/注册）
 */
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

    /**
     * 用户注册接口
     * @param user 注册信息（username/password 为必填）
     * @return 注册结果
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册，密码自动加密存储")
    public Result<?> register(
            @Parameter(description = "注册信息（必填username/password）", required = true)
            @RequestBody User user) {
        // 1. 前置参数校验：避免空指针
        if (user == null) {
            log.warn("注册请求体为空");
            return Result.error(400, "请求参数不能为空");
        }
        if (!StringUtils.hasText(user.getUsername())) {
            log.warn("注册用户名为空");
            return Result.error(400, "用户名不能为空");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            log.warn("注册密码为空，用户名：{}", user.getUsername());
            return Result.error(400, "密码不能为空");
        }

        try {
            // 新增：用户名查重（核心优化点）
            User existUser = userService.getByUsername(user.getUsername());
            if (existUser != null) {
                log.warn("注册失败：用户名已存在，用户名：{}", user.getUsername());
                return Result.error(400, "用户名已存在，请更换用户名");
            }

            // 2. 密码加密
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            // 3. 保存用户
            boolean saveSuccess = userService.save(user);
            if (saveSuccess) {
                log.info("用户注册成功，用户名：{}", user.getUsername());
                return Result.success("注册成功");
            } else {
                log.error("用户注册失败，保存数据库失败，用户名：{}", user.getUsername());
                return Result.error(500, "注册失败：数据库保存失败");
            }
        } catch (Exception e) {
            log.error("用户注册异常，用户名：{}", user.getUsername(), e);
            return Result.error(500, "注册异常：" + e.getMessage());
        }
    }

    /**
     * 用户登录接口
     * @param user 登录信息（username/password）
     * @return 登录结果（成功返回JWT令牌+用户基础信息）
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "登录成功返回JWT令牌")
    public Result<?> login(
            @Parameter(description = "登录信息（username/password）", required = true)
            @RequestBody User user) {
        // 1. 前置参数校验：提前拦截无效请求，避免后续空指针
        if (user == null) {
            log.warn("登录请求体为空");
            return Result.error(400, "请求参数不能为空");
        }
        String username = user.getUsername();
        String password = user.getPassword();
        if (!StringUtils.hasText(username)) {
            log.warn("登录用户名为空");
            return Result.error(400, "用户名不能为空");
        }
        if (!StringUtils.hasText(password)) {
            log.warn("登录密码为空，用户名：{}", username);
            return Result.error(400, "密码不能为空");
        }

        try {
            // 2. 查询数据库用户
            User dbUser = userService.getByUsername(username);
            if (dbUser == null) {
                log.warn("登录失败：用户名不存在，用户名：{}", username);
                // 优化：统一错误提示，避免泄露用户信息
                return Result.error(401, "用户名或密码错误");
            }

            // 3. 校验密码（前端明文 vs 数据库密文）
            boolean passwordMatch = passwordEncoder.matches(password, dbUser.getPassword());
            if (!passwordMatch) {
                log.warn("登录失败：密码错误，用户名：{}", username);
                return Result.error(401, "用户名或密码错误");
            }

            // 修复核心：适配JwtUtils的参数要求，构造空权限集合（无权限控制时用）
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            // 生成token（用户名 + 权限集合，适配Spring Security规范）
            String token = jwtUtils.generateToken(username, authorities);

            log.info("用户登录成功，用户名：{}", username);

            // 优化：返回token+用户基础信息，前端无需额外查库
            LoginResponse response = new LoginResponse(token, dbUser.getId(), username);
            return Result.success("登录成功", response);

        } catch (NullPointerException e) {
            // 针对性捕获空指针（比如JwtUtils注入失败、dbUser字段为空等）
            log.error("登录空指针异常，用户名：{}", username, e);
            return Result.error(500, "登录异常：系统配置错误，请联系管理员");
        } catch (Exception e) {
            // 捕获所有其他异常
            log.error("登录未知异常，用户名：{}", username, e);
            return Result.error(500, "登录异常：" + e.getMessage());
        }
    }

    /**
     * 测试接口（验证后端连通性）
     */
    @GetMapping("/test")
    @Operation(summary = "测试接口", description = "验证后端服务是否正常")
    public Result<?> test() {
        log.info("测试接口被调用，后端服务正常");
        return Result.success("后端接口正常！");
    }

    /**
     * 登录响应实体（返回token+用户基础信息）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class LoginResponse {
        private String token;       // JWT令牌
        private Long userId;        // 用户ID
        private String username;    // 用户名
    }
}