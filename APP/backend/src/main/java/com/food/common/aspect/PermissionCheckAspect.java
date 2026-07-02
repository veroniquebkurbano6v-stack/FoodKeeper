package com.food.common.aspect;

import com.food.common.annotation.RequiresPermissions;
import com.food.common.annotation.RequiresRoles;
import com.food.common.exception.UnauthorizedException;
import com.food.common.result.ErrorCode;
import com.food.common.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 权限校验切面
 * <p>
 * 拦截标注了 @RequiresPermissions 或 @RequiresRoles 的方法，
 * 校验当前登录用户是否拥有对应的角色或权限
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Slf4j
@Aspect
@Component
public class PermissionCheckAspect {

    /**
     * 切面方法：拦截所有 Controller 方法
     * <p>
     * 优先校验角色注解，再校验权限注解
     *
     * @param joinPoint 连接点
     * @return 方法返回值
     * @throws Throwable 业务异常或原方法异常
     */
    @Around("@annotation(com.food.common.annotation.RequiresPermissions) " +
            "|| @annotation(com.food.common.annotation.RequiresRoles) " +
            "|| @within(com.food.common.annotation.RequiresPermissions) " +
            "|| @within(com.food.common.annotation.RequiresRoles)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        // 1. 获取方法上的角色注解，若没有则取类上的
        RequiresRoles methodRoles = method.getAnnotation(RequiresRoles.class);
        RequiresRoles classRoles = targetClass.getAnnotation(RequiresRoles.class);

        // 2. 获取方法上的权限注解，若没有则取类上的
        RequiresPermissions methodPerms = method.getAnnotation(RequiresPermissions.class);
        RequiresPermissions classPerms = targetClass.getAnnotation(RequiresPermissions.class);

        // 3. 优先使用方法上的注解，其次使用类上的
        RequiresRoles rolesAnnotation = methodRoles != null ? methodRoles : classRoles;
        RequiresPermissions permsAnnotation = methodPerms != null ? methodPerms : classPerms;

        // 4. 角色校验
        if (rolesAnnotation != null) {
            checkRoles(rolesAnnotation);
        }

        // 5. 权限校验
        if (permsAnnotation != null) {
            checkPermissions(permsAnnotation);
        }

        // 6. 校验通过，执行原方法
        return joinPoint.proceed();
    }

    /**
     * 校验角色
     *
     * @param annotation 角色注解
     */
    private void checkRoles(RequiresRoles annotation) {
        String[] roles = annotation.value();
        RequiresRoles.Logical logical = annotation.logical();

        if (roles == null || roles.length == 0) {
            return;
        }

        boolean hasRole;
        if (logical == RequiresRoles.Logical.AND) {
            hasRole = UserContext.hasAllRoles(roles);
        } else {
            hasRole = UserContext.hasAnyRole(roles);
        }

        if (!hasRole) {
            log.warn("角色权限不足，需要角色: {}, 用户角色: {}",
                    java.util.Arrays.toString(roles),
                    UserContext.getRoleCodes());
            throw new UnauthorizedException(ErrorCode.FORBIDDEN, "无角色权限，无法访问该资源");
        }
    }

    /**
     * 校验权限
     *
     * @param annotation 权限注解
     */
    private void checkPermissions(RequiresPermissions annotation) {
        String[] permissions = annotation.value();
        RequiresPermissions.Logical logical = annotation.logical();

        if (permissions == null || permissions.length == 0) {
            return;
        }

        boolean hasPermission;
        if (logical == RequiresPermissions.Logical.AND) {
            hasPermission = UserContext.hasAllPermissions(permissions);
        } else {
            hasPermission = UserContext.hasAnyPermission(permissions);
        }

        if (!hasPermission) {
            log.warn("权限不足，需要权限: {}, 用户权限: {}",
                    java.util.Arrays.toString(permissions),
                    UserContext.getPermissions());
            throw new UnauthorizedException(ErrorCode.FORBIDDEN, "无操作权限，无法访问该资源");
        }
    }

}