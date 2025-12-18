package com.checkin.exception;

// 自定义业务异常（简化版）
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}