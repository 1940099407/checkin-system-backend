package com.checkin.common;

import com.checkin.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：统一返回前端友好的错误格式，避免暴露系统栈信息
 */
@Slf4j  // 解决log无法解析问题
@RestControllerAdvice  // 全局生效，覆盖所有Controller
public class GlobalExceptionHandler {

    // 处理自定义业务异常（如：用户不存在、补卡次数超限）
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("业务异常 -> {}", e.getMessage());
        return Result.error(500, e.getMessage());  // 适配你的Result格式（code=500）
    }

    // 处理参数校验异常（如：@NotNull/@Min注解校验失败）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder errorMsg = new StringBuilder();
        // 收集所有字段的校验错误信息
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errorMsg.append(fieldError.getDefaultMessage()).append(",");
        }
        log.error("参数校验异常 -> {}", errorMsg);
        return Result.error(400, errorMsg.toString());  // 400=参数错误
    }

    // 处理所有未捕获的系统异常（如：空指针、数据库连接失败）
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        e.printStackTrace();  // 开发环境打印栈信息，方便调试
        log.error("系统异常 -> ", e);  // 日志记录完整异常栈
        return Result.error(500, "服务器内部错误，请稍后再试");
    }
}