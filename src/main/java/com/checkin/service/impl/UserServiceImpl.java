package com.checkin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.checkin.common.Result;
import com.checkin.entity.User;
import com.checkin.mapper.UserMapper;
import com.checkin.service.UserService;
import com.checkin.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils; // 注意变量名拼写正确

    @Override
    public Result<?> register(User user) {
        // 1. 检查用户名是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", user.getUsername());
        User existingUser = baseMapper.selectOne(queryWrapper);
        if (existingUser != null) {
            return Result.error("用户名已存在"); // 修正参数格式
        }

        // 2. 密码加密
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // 3. 设置默认角色(0-普通用户)和创建时间
        user.setRole(0);
        user.setCreateTime(LocalDateTime.now());

        // 4. 保存用户
        baseMapper.insert(user);
        return Result.success("注册成功");
    }

    @Override
    public Result<?> login(String username, String password) {
        // 1. 查找用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username); // 简化写法，无需column参数
        User user = baseMapper.selectOne(queryWrapper);
        if (user == null) {
            return Result.error("用户名不存在"); // 修正参数格式
        }

        // 2. 验证密码（解开注释时需确保passwordEncoder正确注入）
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.error("密码错误"); // 修正参数格式
        }

        // 3. 生成JWT令牌（修正String大小写和jwtUtils拼写）
        String token = jwtUtils.generateToken(username);

        // 4. 返回用户信息和令牌
        return Result.success("登录成功", new HashMap<String, Object>() {{
            put("token", token);
            put("user", new HashMap<String, Object>() {{
                put("id", user.getId());
                put("username", user.getUsername());
                put("role", user.getRole());
            }});
        }});
    }
}