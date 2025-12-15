package com.checkin.common;

import lombok.Data;

/**
 * 全局统一返回结果类
 * 所有接口返回数据都通过此类封装，保证前端接收格式统一
 */
@Data
public class Result<T> {
    // 响应状态码：200=成功 500=失败（可扩展：401=未登录 403=无权限等）
    private Integer code;
    // 响应提示信息（前端可直接展示给用户）
    private String msg;
    // 响应数据（泛型适配不同类型：列表、对象、布尔值等）
    private T data;

    // ========== 静态构造方法（简化调用，无需new） ==========

    /**
     * 成功响应（无返回数据）
     * 示例：新增/删除操作后，只返回“操作成功”，无需数据
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        return result;
    }

    /**
     * 成功响应（带返回数据）
     * 示例：查询用户信息、查询打卡记录，返回具体数据
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 成功响应（自定义提示语+数据）
     * 示例：打卡成功后，返回“打卡成功”+打卡记录
     */
    public static <T> Result<T> success(String msg, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    /**
     * 失败响应（自定义错误提示）
     * 示例：用户不存在、今日已打卡、参数错误等
     */
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMsg(msg);
        return result;
    }

    /**
     * 失败响应（自定义状态码+错误提示）
     * 扩展用：比如401未登录、403无权限
     */
    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}