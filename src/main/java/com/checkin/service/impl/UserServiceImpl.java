// com.checkin.service.impl.UserServiceImpl.java
package com.checkin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.checkin.common.Result;
import com.checkin.entity.User;
import com.checkin.mapper.UserMapper;
import com.checkin.service.UserService;
import com.checkin.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;


    // 1. 实现register方法（用户注册）
    @Override
    public Result<?> register(User user) {
        // 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 保存用户到数据库
        boolean saveSuccess = this.save(user);
        if (saveSuccess) {
            return Result.success("注册成功");
        }
        return Result.error(500, "注册失败");
    }


    // 2. 实现login方法（用户登录，返回JWT令牌）
    @Override
    public Result<?> login(String username, String password) {
        // ① 根据用户名查询用户
        User user = this.getByUsername(username);
        if (user == null) {
            return Result.error(401, "用户名不存在");
        }
        // ② 验证密码是否匹配
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.error(401, "密码错误");
        }
        // ③ 生成JWT令牌
        String token = jwtUtils.generateToken(username);
        return Result.success(token);
    }


    // 3. 实现getByUsername方法（根据用户名查询用户）
    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        // 调用MyBatis-Plus的getOne查询单条记录
        return this.getOne(queryWrapper);
    }
}