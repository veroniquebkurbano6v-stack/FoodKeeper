package com.food.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色校验注解
 * <p>
 * 标注在 Controller 方法上，指定访问该接口所需的角色编码
 * 由 PermissionCheckAspect 切面进行校验
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRoles {

    /**
     * 角色编码列表
     * <p>
     * 例如：{"SUPER_ADMIN", "ADMIN"}
     */
    String[] value();

    /**
     * 校验模式
     * <p>
     * AND：必须拥有所有角色才能访问
     * OR：拥有任意一个角色即可访问
     */
    Logical logical() default Logical.OR;

    /**
     * 逻辑枚举
     */
    enum Logical {
        AND,
        OR
    }

}