package com.food.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ==================== 系统级错误 1000-1999 ====================

    /**
     * 系统异常
     */
    SYSTEM_ERROR(1000, "系统异常，请稍后重试"),

    /**
     * 参数校验失败
     */
    PARAM_ERROR(1001, "参数校验失败"),

    /**
     * 资源不存在
     */
    NOT_FOUND(1002, "资源不存在"),

    /**
     * 请求方法不支持
     */
    METHOD_NOT_SUPPORTED(1003, "请求方法不支持"),

    /**
     * 请求频率过高
     */
    RATE_LIMIT_ERROR(1004, "请求频率过高，请稍后重试"),

    // ==================== 业务级错误 2000-2999 ====================

    /**
     * 业务处理失败
     */
    BUSINESS_ERROR(2000, "业务处理失败"),

    /**
     * 食材不存在
     */
    FOOD_NOT_FOUND(2001, "食材不存在"),

    /**
     * 分类不存在
     */
    CATEGORY_NOT_FOUND(2002, "分类不存在"),

    /**
     * 库存数量不足
     */
    QUANTITY_NOT_ENOUGH(2003, "库存数量不足"),

    /**
     * 食材已过期
     */
    FOOD_EXPIRED(2004, "食材已过期，不可使用"),

    /**
     * 食材名称重复
     */
    FOOD_NAME_DUPLICATE(2005, "食材名称已存在"),

    /**
     * 操作失败
     */
    OPERATION_FAILED(2006, "操作失败"),

    // ==================== 用户相关错误 2100-2199 ====================

    /**
     * 用户不存在
     */
    USER_NOT_FOUND(2100, "用户不存在"),

    /**
     * 用户名已存在
     */
    USERNAME_EXISTS(2101, "用户名已存在"),

    /**
     * 手机号已存在
     */
    PHONE_EXISTS(2102, "手机号已存在"),

    /**
     * 密码错误
     */
    PASSWORD_ERROR(2103, "密码错误"),

    /**
     * 新密码与旧密码相同
     */
    PASSWORD_SAME(2104, "新密码不能与旧密码相同"),

    /**
     * 不能禁用超级管理员
     */
    CANNOT_DISABLE_ADMIN(2105, "不能禁用超级管理员账号"),

    /**
     * 不能删除超级管理员
     */
    CANNOT_DELETE_ADMIN(2106, "不能删除超级管理员账号"),

    // ==================== 权限级错误 3000-3999 ====================

    /**
     * 未登录或Token过期
     */
    UNAUTHORIZED(3000, "未登录或Token已过期"),

    /**
     * 无操作权限
     */
    FORBIDDEN(3001, "无操作权限"),

    /**
     * Token已过期
     */
    TOKEN_EXPIRED(3002, "Token已过期，请重新登录"),

    /**
     * Token无效
     */
    TOKEN_INVALID(3003, "Token无效"),

    /**
     * 账号已被禁用
     */
    ACCOUNT_DISABLED(3004, "账号已被禁用"),

    // ==================== 数据库级错误 4000-4999 ====================

    /**
     * 数据库操作失败
     */
    DATABASE_ERROR(4000, "数据库操作失败"),

    /**
     * 数据重复
     */
    DATA_DUPLICATE(4001, "数据已存在"),

    /**
     * 数据删除失败
     */
    DATA_DELETE_FAILED(4002, "数据删除失败");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误信息
     */
    private final String message;

}