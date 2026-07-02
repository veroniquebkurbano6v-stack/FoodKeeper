package com.food.common.util;

import java.util.Collections;
import java.util.Set;

/**
 * 用户上下文工具类
 * <p>
 * 使用 ThreadLocal 存储当前登录用户信息
 * 包含用户ID、角色编码集合、权限标识集合
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
public class UserContext {

    /**
     * ThreadLocal 存储用户ID
     */
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    /**
     * ThreadLocal 存储角色编码集合
     */
    private static final ThreadLocal<Set<String>> ROLE_CODES_HOLDER = new ThreadLocal<>();

    /**
     * ThreadLocal 存储权限标识集合
     */
    private static final ThreadLocal<Set<String>> PERMISSIONS_HOLDER = new ThreadLocal<>();

    /**
     * 设置用户ID
     *
     * @param userId 用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取用户ID
     *
     * @return 用户ID
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 设置角色编码集合
     *
     * @param roleCodes 角色编码集合
     */
    public static void setRoleCodes(Set<String> roleCodes) {
        ROLE_CODES_HOLDER.set(roleCodes);
    }

    /**
     * 获取角色编码集合
     *
     * @return 角色编码集合
     */
    public static Set<String> getRoleCodes() {
        Set<String> roles = ROLE_CODES_HOLDER.get();
        return roles != null ? roles : Collections.emptySet();
    }

    /**
     * 设置权限标识集合
     *
     * @param permissions 权限标识集合
     */
    public static void setPermissions(Set<String> permissions) {
        PERMISSIONS_HOLDER.set(permissions);
    }

    /**
     * 获取权限标识集合
     *
     * @return 权限标识集合
     */
    public static Set<String> getPermissions() {
        Set<String> perms = PERMISSIONS_HOLDER.get();
        return perms != null ? perms : Collections.emptySet();
    }

    /**
     * 判断是否拥有指定角色
     *
     * @param roleCode 角色编码
     * @return 是否拥有该角色
     */
    public static boolean hasRole(String roleCode) {
        return getRoleCodes().contains(roleCode);
    }

    /**
     * 判断是否拥有任意一个指定角色
     *
     * @param roleCodes 角色编码数组
     * @return 是否拥有任意角色
     */
    public static boolean hasAnyRole(String... roleCodes) {
        Set<String> userRoles = getRoleCodes();
        for (String role : roleCodes) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否拥有所有指定角色
     *
     * @param roleCodes 角色编码数组
     * @return 是否拥有所有角色
     */
    public static boolean hasAllRoles(String... roleCodes) {
        Set<String> userRoles = getRoleCodes();
        for (String role : roleCodes) {
            if (!userRoles.contains(role)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断是否拥有指定权限
     *
     * @param permission 权限标识
     * @return 是否拥有该权限
     */
    public static boolean hasPermission(String permission) {
        Set<String> perms = getPermissions();
        return perms.contains("*:*:*") || perms.contains(permission);
    }

    /**
     * 判断是否拥有任意一个指定权限
     *
     * @param permissions 权限标识数组
     * @return 是否拥有任意权限
     */
    public static boolean hasAnyPermission(String... permissions) {
        Set<String> userPerms = getPermissions();
        // 超级管理员通配符
        if (userPerms.contains("*:*:*")) {
            return true;
        }
        for (String perm : permissions) {
            if (userPerms.contains(perm)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否拥有所有指定权限
     *
     * @param permissions 权限标识数组
     * @return 是否拥有所有权限
     */
    public static boolean hasAllPermissions(String... permissions) {
        Set<String> userPerms = getPermissions();
        // 超级管理员通配符
        if (userPerms.contains("*:*:*")) {
            return true;
        }
        for (String perm : permissions) {
            if (!userPerms.contains(perm)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断是否为超级管理员
     *
     * @return 是否为超级管理员
     */
    public static boolean isSuperAdmin() {
        return hasRole("SUPER_ADMIN") || getPermissions().contains("*:*:*");
    }

    /**
     * 清理所有 ThreadLocal
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
        ROLE_CODES_HOLDER.remove();
        PERMISSIONS_HOLDER.remove();
    }

    /**
     * 判断是否登录
     *
     * @return 是否登录
     */
    public static boolean isLogin() {
        return USER_ID_HOLDER.get() != null;
    }

}