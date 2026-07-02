package com.food.common.exception;

import com.food.common.result.ErrorCode;
import lombok.Getter;

/**
 * 业务异常类
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 构造函数（默认业务错误码）
     *
     * @param message 错误信息
     */
    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.BUSINESS_ERROR.getCode();
    }

    /**
     * 构造函数（自定义错误码）
     *
     * @param code    错误码
     * @param message 错误信息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造函数（使用错误码枚举）
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 构造函数（使用错误码枚举，自定义消息）
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

}