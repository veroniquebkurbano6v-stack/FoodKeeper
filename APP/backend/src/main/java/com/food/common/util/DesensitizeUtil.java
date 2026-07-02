package com.food.common.util;

import org.springframework.util.StringUtils;

/**
 * 敏感信息脱敏工具类
 * <p>
 * 提供手机号、身份证、银行卡、邮箱、密码等敏感字段的脱敏处理
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
public class DesensitizeUtil {

    private DesensitizeUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 手机号脱敏
     * <p>
     * 规则：显示前3后4，中间用****代替
     * 示例：13812345678 -> 138****5678
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String phone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        int length = phone.length();
        return phone.substring(0, 3) + "****" + phone.substring(length - 4);
    }

    /**
     * 身份证号脱敏
     * <p>
     * 规则：显示前6后4，中间用********代替
     * 示例：320101199001011234 -> 320101********1234
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String idCard(String idCard) {
        if (!StringUtils.hasText(idCard) || idCard.length() < 10) {
            return idCard;
        }
        int length = idCard.length();
        return idCard.substring(0, 6) + "********" + idCard.substring(length - 4);
    }

    /**
     * 银行卡号脱敏
     * <p>
     * 规则：只显示后4位，前面用**** **** ****代替
     * 示例：6222021234567890123 -> **** **** **** 0123
     *
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String bankCard(String bankCard) {
        if (!StringUtils.hasText(bankCard) || bankCard.length() < 4) {
            return bankCard;
        }
        int length = bankCard.length();
        return "**** **** **** " + bankCard.substring(length - 4);
    }

    /**
     * 邮箱脱敏
     * <p>
     * 规则：显示前3字符和域名，中间用***代替
     * 示例：zhangsan@example.com -> zha***@example.com
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String email(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (username.length() <= 3) {
            return username + "***" + domain;
        }
        return username.substring(0, 3) + "***" + domain;
    }

    /**
     * 密码脱敏
     * <p>
     * 规则：全部用******代替
     *
     * @param password 密码
     * @return 脱敏后的密码
     */
    public static String password(String password) {
        if (!StringUtils.hasText(password)) {
            return password;
        }
        return "******";
    }

    /**
     * 真实姓名脱敏
     * <p>
     * 规则：只显示姓，名用*代替
     * 示例：张三 -> 张*，张三丰 -> 张**
     *
     * @param realName 真实姓名
     * @return 脱敏后的姓名
     */
    public static String realName(String realName) {
        if (!StringUtils.hasText(realName) || realName.length() <= 1) {
            return realName;
        }
        return realName.charAt(0) + "*".repeat(realName.length() - 1);
    }

    /**
     * IP地址脱敏
     * <p>
     * 规则：只显示前两段，后两段用.*.*代替
     * 示例：192.168.1.100 -> 192.168.*.*
     *
     * @param ip IP地址
     * @return 脱敏后的IP地址
     */
    public static String ip(String ip) {
        if (!StringUtils.hasText(ip)) {
            return ip;
        }
        String[] parts = ip.split("\\.");
        if (parts.length < 2) {
            return ip;
        }
        return parts[0] + "." + parts[1] + ".*.*";
    }
}
