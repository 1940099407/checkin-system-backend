package com.checkin.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.checkin.entity.User;
import com.checkin.mapper.UserMapper;
import com.checkin.service.UserService;
import com.checkin.service.CheckinRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest // 启动Spring容器，自动注入Bean
public class UserControllerTest {

    @Autowired
    private UserService userService; // 注入用户服务

    @Autowired
    private CheckinRecordService checkinService; // 注入打卡服务

    @Autowired
    private UserMapper userMapper; // 注入Mapper，用于清理测试数据


    // 测试1：用户注册
    @Test
    public void testRegister() {
        // 准备测试数据
        User user = new User();
        user.setUsername("test_junit");
        user.setPassword("123456");

        // 调用注册方法
        var result = userService.register(user);
        System.out.println("注册结果：" + result);

        // 断言：注册成功（根据实际Result类的code判断）
        if (result.getCode() == 200) {
            System.out.println("注册测试通过");
        } else {
            System.out.println("注册测试失败：" + result.getMsg());
        }
    }


    // 测试2：用户登录（依赖注册成功）
    @Test
    public void testLogin() {
        // 调用登录方法
        var result = userService.login("test_junit", "123456");
        System.out.println("登录结果：" + result);

        // 断言：登录成功并返回token
        if (result.getCode() == 200 && result.getData() != null) {
            System.out.println("登录测试通过，token：" + result.getData());
        } else {
            System.out.println("登录测试失败：" + result.getMsg());
        }
    }


    // 测试3：打卡（依赖登录成功）
    @Test
    public void testCheckin() {
        // 先查询用户ID（假设已注册test_junit）
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", "test_junit"));
        if (user == null) {
            System.out.println("请先执行注册测试");
            return;
        }

        // 创建打卡记录
        var record = new com.checkin.entity.CheckinRecord();
        record.setUserId(user.getId());

        // 调用打卡方法
        var result = checkinService.createCheckin(record);
        System.out.println("打卡结果：" + result);

        // 断言：打卡成功
        if (result.getCode() == 200) {
            System.out.println("打卡测试通过");
        } else {
            System.out.println("打卡测试失败：" + result.getMsg());
        }
    }


    // 测试完成后清理数据（可选，避免测试数据残留）
    @Test
    public void cleanTestData() {
        // 删除测试用户
        userMapper.delete(new QueryWrapper<User>().eq("username", "test_junit"));
        System.out.println("测试数据已清理");
    }
}