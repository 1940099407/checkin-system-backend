package com.checkin.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.checkin.entity.User;
import com.checkin.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 从数据库查询用户
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            throw new UsernameNotFoundException("用户名不存在");
        }

        // 构建Spring Security所需的用户对象（包含用户名、密码、权限）
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword()) // 数据库中已加密的密码
                // 关键修改：适配String类型的role字段，取消数字比较
                // 方式1（推荐）：直接使用数据库中的role字符串（USER/ADMIN）
                .authorities(Collections.singletonList(() -> "ROLE_" + user.getRole()))
                // 方式2（备选）：如果数据库role存数字字符串（如"1"/"0"），用以下逻辑
                // .authorities(Collections.singletonList(() -> "ROLE_" + ("1".equals(user.getRole()) ? "ADMIN" : "USER")))
                .build();
    }
}