package com.food.common.exception;

import com.food.common.result.ErrorCode;
import lombok.Getter;

/**
 * 权限异常类
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Getter
public class UnauthorizedException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 构造函数（默认未登录错误码）
     *
     * @param message 错误信息
     */
    public UnauthorizedException(String message) {
        super(message);
        this.code = ErrorCode.UNAUTHORIZED.getCode();
    }

    /**
     * 构造函数（使用错误码枚举）
     *
     * @param errorCode 错误码枚举
     */
    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public UnauthorizedException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

}