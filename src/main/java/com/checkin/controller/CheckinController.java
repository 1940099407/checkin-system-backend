package com.checkin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.checkin.entity.Checkin;
import com.checkin.entity.User;
import com.checkin.service.CheckinService;
import com.checkin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 打卡接口控制器（需登录认证）- 完善CRUD版本
 * 保留原有核心逻辑，新增修改、删除、管理员查询接口
 */
@RestController
@RequestMapping("/checkin")
public class CheckinController {

    @Autowired
    private CheckinService checkinService;
    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户（复用原有逻辑）
     */
    private User getCurrentUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userService.getByUsername(userDetails.getUsername());
    }

    /**
     * 权限校验：普通用户只能操作自己的记录，管理员可操作所有记录
     */
    private boolean checkPermission(User currentUser, Checkin checkin) {
        // 管理员拥有所有权限
        if ("ADMIN".equals(currentUser.getRole())) {
            return true;
        }
        // 普通用户仅能操作自己的记录
        return currentUser.getId().equals(checkin.getUserId());
    }

    // ===================== 原有接口（完全保留，无改动）=====================
    /**
     * 上班打卡接口
     */
    @PostMapping("/in")
    public Map<String, Object> doCheckin(Authentication authentication,
                                         @RequestParam(defaultValue = "默认地点") String location) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = getCurrentUser(authentication);
            String msg = checkinService.doCheckin(user.getId(), location);
            result.put("code", 200);
            result.put("msg", msg);
            result.put("success", true);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "打卡失败：" + e.getMessage());
            result.put("success", false);
        }
        return result;
    }

    /**
     * 下班签退接口
     */
    @PostMapping("/out")
    public Map<String, Object> doCheckout(Authentication authentication,
                                          @RequestParam(defaultValue = "默认地点") String location) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = getCurrentUser(authentication);
            String msg = checkinService.doCheckout(user.getId(), location);
            result.put("code", 200);
            result.put("msg", msg);
            result.put("success", true);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "签退失败：" + e.getMessage());
            result.put("success", false);
        }
        return result;
    }

    /**
     * 查询今日打卡记录
     */
    @GetMapping("/today")
    public Map<String, Object> getTodayCheckin(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = getCurrentUser(authentication);
            Checkin checkin = checkinService.getCheckinByUserAndDate(user.getId(), LocalDate.now());
            result.put("code", 200);
            result.put("data", checkin);
            result.put("success", true);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("success", false);
        }
        return result;
    }

    /**
     * 查询指定时间段打卡记录（参数：startDate/endDate，格式yyyy-MM-dd）
     */
    @GetMapping("/range")
    public Map<String, Object> listCheckinRange(Authentication authentication,
                                                @RequestParam String startDate,
                                                @RequestParam String endDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = getCurrentUser(authentication);
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            List<Checkin> list = checkinService.listCheckinByUserAndDateRange(user.getId(), start, end);
            result.put("code", 200);
            result.put("data", list);
            result.put("success", true);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("success", false);
        }
        return result;
    }

    /**
     * 月度打卡统计（参数：year/month）
     */
    @GetMapping("/stat/month")
    public Map<String, Object> statMonthly(Authentication authentication,
                                           @RequestParam int year,
                                           @RequestParam int month) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = getCurrentUser(authentication);
            Map<String, Object> stat = checkinService.statMonthlyCheckin(user.getId(), year, month);
            result.put("code", 200);
            result.put("data", stat);
            result.put("success", true);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "统计失败：" + e.getMessage());
            result.put("success", false);
        }
        return result;
    }

    // ===================== 新增CRUD接口（兼容原有返回格式）=====================
    /**
     * 修改：修改打卡记录备注（普通用户/管理员）
     * 参数：id（打卡记录ID）、remark（新备注）
     */
    @PutMapping("/update/remark")
    public Map<String, Object> updateCheckinRemark(Authentication authentication,
                                                   @RequestParam Long id,
                                                   @RequestParam String remark) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = getCurrentUser(authentication);
            // 查询打卡记录
            Checkin checkin = checkinService.getById(id);
            if (checkin == null) {
                result.put("code", 400);
                result.put("msg", "打卡记录不存在");
                result.put("success", false);
                return result;
            }
            // 权限校验
            if (!checkPermission(user, checkin)) {
                result.put("code", 403);
                result.put("msg", "无权修改该记录");
                result.put("success", false);
                return result;
            }
            // 更新备注
            checkin.setRemark(remark);
            checkinService.updateById(checkin);
            result.put("code", 200);
            result.put("msg", "备注修改成功");
            result.put("success", true);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "修改失败：" + e.getMessage());
            result.put("success", false);
        }
        return result;
    }

    /**
     * 删除：删除单条打卡记录（普通用户/管理员）
     * 参数：id（打卡记录ID，路径参数）
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteCheckin(Authentication authentication,
                                             @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = getCurrentUser(authentication);
            // 查询打卡记录
            Checkin checkin = checkinService.getById(id);
            if (checkin == null) {
                result.put("code", 400);
                result.put("msg", "打卡记录不存在");
                result.put("success", false);
                return result;
            }
            // 权限校验
            if (!checkPermission(user, checkin)) {
                result.put("code", 403);
                result.put("msg", "无权删除该记录");
                result.put("success", false);
                return result;
            }
            // 删除记录
            checkinService.removeById(id);
            result.put("code", 200);
            result.put("msg", "打卡记录删除成功");
            result.put("success", true);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "删除失败：" + e.getMessage());
            result.put("success", false);
        }
        return result;
    }

    /**
     * 查询：管理员分页查询所有用户打卡记录（支持筛选）
     * 参数：pageNum（页码）、pageSize（页大小）、username（用户名筛选）、status（状态筛选）
     */
    @GetMapping("/admin/list")
    public Map<String, Object> getAllCheckin(Authentication authentication,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                             @RequestParam(required = false) String username,
                                             @RequestParam(required = false) String status) {
        Map<String, Object> result = new HashMap<>();
        try {
            User currentUser = getCurrentUser(authentication);
            // 校验管理员权限
            if (!"ADMIN".equals(currentUser.getRole())) {
                result.put("code", 403);
                result.put("msg", "仅管理员可访问该接口");
                result.put("success", false);
                return result;
            }
            // 构建分页对象
            Page<Checkin> page = new Page<>(pageNum, pageSize);
            // 调用Service分页查询（需在CheckinService中声明并实现该方法）
            IPage<Checkin> checkinPage = checkinService.getAllCheckinByPage(page, username, status);
            result.put("code", 200);
            result.put("data", checkinPage);
            result.put("success", true);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
            result.put("success", false);
        }
        return result;
    }

    /**
     * 修改：管理员修改打卡记录状态（正常/迟到/早退/缺勤）
     * 参数：id（打卡记录ID）、status（新状态）
     */
    @PutMapping("/admin/update/status")
    public Map<String, Object> updateCheckinStatus(Authentication authentication,
                                                   @RequestParam Long id,
                                                   @RequestParam String status) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = getCurrentUser(authentication);
            // 校验管理员权限
            if (!"ADMIN".equals(user.getRole())) {
                result.put("code", 403);
                result.put("msg", "仅管理员可修改打卡状态");
                result.put("success", false);
                return result;
            }
            // 查询打卡记录
            Checkin checkin = checkinService.getById(id);
            if (checkin == null) {
                result.put("code", 400);
                result.put("msg", "打卡记录不存在");
                result.put("success", false);
                return result;
            }
            // 更新状态
            checkin.setStatus(status);
            checkinService.updateById(checkin);
            result.put("code", 200);
            result.put("msg", "打卡状态修改成功");
            result.put("success", true);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "修改失败：" + e.getMessage());
            result.put("success", false);
        }
        return result;
    }

    /**
     * 删除：管理员批量删除打卡记录
     * 参数：ids（记录ID列表，逗号分隔）
     */
    @DeleteMapping("/admin/batch")
    public Map<String, Object> batchDeleteCheckin(Authentication authentication,
                                                  @RequestParam List<Long> ids) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = getCurrentUser(authentication);
            // 校验管理员权限
            if (!"ADMIN".equals(user.getRole())) {
                result.put("code", 403);
                result.put("msg", "仅管理员可批量删除");
                result.put("success", false);
                return result;
            }
            // 批量删除
            checkinService.removeByIds(ids);
            result.put("code", 200);
            result.put("msg", "批量删除成功");
            result.put("success", true);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "批量删除失败：" + e.getMessage());
            result.put("success", false);
        }
        return result;
    }
}