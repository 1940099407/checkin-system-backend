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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习健康打卡控制器（最终版，无编译错误）
 */
@RestController
@RequestMapping("/checkin")
public class CheckinController {

    @Autowired
    private CheckinService checkinService;
    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户
     */
    private User getCurrentUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userService.getByUsername(userDetails.getUsername());
    }

    /**
     * 权限校验：普通用户只能操作自己的记录，管理员可操作所有记录
     */
    private boolean checkPermission(User currentUser, Checkin checkin) {
        if ("ADMIN".equals(currentUser.getRole())) {
            return true;
        }
        return currentUser.getId().equals(checkin.getUserId());
    }

    // ===================== 学习打卡核心接口 =====================
    /**
     * 学习打卡提交（无编译错误，匹配Service方法）
     */
    @PostMapping("/create")
    public Map<String, Object> createStudyCheckin(Authentication authentication,
                                                  @RequestBody Checkin checkin) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = getCurrentUser(authentication);
            checkin.setUserId(user.getId()); // 绑定当前用户
            checkin.setStatus(1); // 正常状态（Integer类型）
            checkin.setCheckinTime(LocalDateTime.now()); // 填充打卡时间
            // 调用Service方法（返回String，无类型错误）
            String msg = checkinService.createStudyCheckin(checkin);
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
     * 查询今日学习打卡记录
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
     * 查询指定时间段学习打卡记录
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
     * 月度学习打卡统计
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

    // ===================== 修复后的CRUD接口 =====================
    /**
     * 修改打卡备注
     */
    @PutMapping("/update/remark")
    public Map<String, Object> updateCheckinRemark(Authentication authentication,
                                                   @RequestParam Long id,
                                                   @RequestParam String remark) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = getCurrentUser(authentication);
            Checkin checkin = checkinService.getById(id);
            if (checkin == null) {
                result.put("code", 400);
                result.put("msg", "打卡记录不存在");
                result.put("success", false);
                return result;
            }
            if (!checkPermission(user, checkin)) {
                result.put("code", 403);
                result.put("msg", "无权修改该记录");
                result.put("success", false);
                return result;
            }
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
     * 删除单条打卡记录
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteCheckin(Authentication authentication,
                                             @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = getCurrentUser(authentication);
            Checkin checkin = checkinService.getById(id);
            if (checkin == null) {
                result.put("code", 400);
                result.put("msg", "打卡记录不存在");
                result.put("success", false);
                return result;
            }
            if (!checkPermission(user, checkin)) {
                result.put("code", 403);
                result.put("msg", "无权删除该记录");
                result.put("success", false);
                return result;
            }
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
     * 管理员分页查询所有打卡记录
     */
    @GetMapping("/admin/list")
    public Map<String, Object> getAllCheckin(Authentication authentication,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                             @RequestParam(required = false) String username,
                                             @RequestParam(required = false) Integer status) {
        Map<String, Object> result = new HashMap<>();
        try {
            User currentUser = getCurrentUser(authentication);
            if (!"ADMIN".equals(currentUser.getRole())) {
                result.put("code", 403);
                result.put("msg", "仅管理员可访问该接口");
                result.put("success", false);
                return result;
            }
            Page<Checkin> page = new Page<>(pageNum, pageSize);
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
     * 管理员修改打卡状态（无类型错误）
     */
    @PutMapping("/admin/update/status")
    public Map<String, Object> updateCheckinStatus(Authentication authentication,
                                                   @RequestParam Long id,
                                                   @RequestParam String statusStr) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = getCurrentUser(authentication);
            if (!"ADMIN".equals(user.getRole())) {
                result.put("code", 403);
                result.put("msg", "仅管理员可修改打卡状态");
                result.put("success", false);
                return result;
            }
            Checkin checkin = checkinService.getById(id);
            if (checkin == null) {
                result.put("code", 400);
                result.put("msg", "打卡记录不存在");
                result.put("success", false);
                return result;
            }
            // String转Integer，避免类型不匹配
            Integer status;
            try {
                status = Integer.parseInt(statusStr);
            } catch (NumberFormatException e) {
                result.put("code", 400);
                result.put("msg", "状态值必须为数字（仅支持1=正常打卡）");
                result.put("success", false);
                return result;
            }
            if (status != 1) {
                result.put("code", 400);
                result.put("msg", "学习打卡仅支持设置正常状态（值为1）");
                result.put("success", false);
                return result;
            }
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
     * 管理员批量删除打卡记录
     */
    @DeleteMapping("/admin/batch")
    public Map<String, Object> batchDeleteCheckin(Authentication authentication,
                                                  @RequestParam List<Long> ids) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = getCurrentUser(authentication);
            if (!"ADMIN".equals(user.getRole())) {
                result.put("code", 403);
                result.put("msg", "仅管理员可批量删除");
                result.put("success", false);
                return result;
            }
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