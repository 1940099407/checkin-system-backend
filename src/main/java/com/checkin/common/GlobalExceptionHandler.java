// src/main/java/com/checkin/common/GlobalExceptionHandler.java
package com.checkin.common;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // 全局异常处理注解
public class GlobalExceptionHandler {

    // 处理参数校验异常（如用户名太短、密码为空）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder errorMsg = new StringBuilder();
        // 收集所有校验失败的信息
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errorMsg.append(fieldError.getDefaultMessage()).append(";");
        }
        return Result.error(400, errorMsg.toString()); // 400表示参数错误
    }

    // 处理其他未知异常（如数据库错误）
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        e.printStackTrace(); // 开发环境打印错误堆栈，方便调试
        return Result.error("服务器内部错误，请稍后再试");
    }
}