package com.food.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 * <p>
 * 标注在 Controller 方法上，指定访问该接口所需的权限标识
 * 由 PermissionCheckAspect 切面进行校验
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermissions {

    /**
     * 权限标识列表
     * <p>
     * 例如：{"system:user:list", "system:user:add"}
     */
    String[] value();

    /**
     * 校验模式
     * <p>
     * AND：必须拥有所有权限才能访问
     * OR：拥有任意一个权限即可访问
     */
    Logical logical() default Logical.AND;

    /**
     * 逻辑枚举
     */
    enum Logical {
        AND,
        OR
    }

}