package com.food.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回结果类
 *
 * @param <T> 数据类型
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一响应体")
public class Result<T> {

    /**
     * 状态码：200成功，其他失败
     */
    @Schema(description = "状态码：200成功，其他失败")
    private Integer code;

    /**
     * 提示信息
     */
    @Schema(description = "提示信息")
    private String message;

    /**
     * 业务数据
     */
    @Schema(description = "业务数据")
    private T data;

    /**
     * 时间戳
     */
    @Schema(description = "时间戳")
    private Long timestamp;

    /**
     * 成功响应（带数据）
     *
     * @param data 业务数据
     * @return Result
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 成功响应（带消息和数据）
     *
     * @param message 提示信息
     * @param data    业务数据
     * @return Result
     */
    public static <T> Result<T> success(String message, T data) {
        return Result.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 成功响应（无数据）
     *
     * @return Result
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 失败响应（自定义错误码和消息）
     *
     * @param code    错误码
     * @param message 提示信息
     * @return Result
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 失败响应（使用错误码枚举）
     *
     * @param errorCode 错误码枚举
     * @return Result
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return Result.<T>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 失败响应（使用业务错误码，自定义消息）
     *
     * @param errorCode 错误码枚举
     * @param message   自定义提示信息
     * @return Result
     */
    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return Result.<T>builder()
                .code(errorCode.getCode())
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

}