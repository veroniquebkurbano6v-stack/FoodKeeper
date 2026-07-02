package com.food.config.interceptor;

import com.food.common.exception.UnauthorizedException;
import com.food.common.result.ErrorCode;
import com.food.common.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接口鉴权拦截器
 * <p>
 * 校验 Token 是否有效，优先从 Redis 中验证，Redis 不可用时使用内存存储作为后备
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    /**
     * 内存 Token 存储（Redis 不可用时的后备方案）
     */
    private static final Map<String, String> TOKEN_STORE = new ConcurrentHashMap<>();

    /**
     * Redis 是否可用
     */
    private volatile boolean redisAvailable = true;

    /**
     * Token 请求头名称
     */
    private static final String TOKEN_HEADER = "Authorization";

    /**
     * Redis Token Key 前缀
     */
    private static final String TOKEN_KEY_PREFIX = "token:";

    public AuthInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 前置处理：校验 Token
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param handler  处理器
     * @return 是否继续执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行 CORS 预检请求（OPTIONS）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.debug("放行 OPTIONS 预检请求，URI: {}", request.getRequestURI());
            return true;
        }

        // 获取 Token
        String token = request.getHeader(TOKEN_HEADER);

        // Token 为空
        if (!StringUtils.hasText(token)) {
            log.warn("Token 为空，URI: {}", request.getRequestURI());
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
        }

        // 去除 Bearer 前缀（如果有）
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 从 Redis 或内存中校验 Token
        String userId = validateToken(token);

        // Token 无效或已过期
        if (!StringUtils.hasText(userId)) {
            log.warn("Token 无效或已过期，Token: {}, URI: {}", token, request.getRequestURI());
            throw new UnauthorizedException(ErrorCode.TOKEN_EXPIRED);
        }

        // 将用户ID存入 ThreadLocal，供后续业务使用
        UserContext.setUserId(Long.parseLong(userId));

        // 设置默认角色和权限（开发模式下，实际应该从数据库查询）
        UserContext.setRoleCodes(java.util.Set.of("SUPER_ADMIN"));
        UserContext.setPermissions(java.util.Set.of("*:*:*"));

        log.debug("Token 校验成功，用户ID: {}, URI: {}", userId, request.getRequestURI());
        return true;
    }

    /**
     * 校验 Token（优先内存存储，后备 Redis）
     * <p>
     * 开发模式下优先使用内存存储（由登录接口直接存储）
     * 生产环境下使用 Redis 存储
     *
     * @param token Token
     * @return 用户ID，校验失败返回 null
     */
    private String validateToken(String token) {
        // 首先尝试从内存获取（开发模式下登录直接存储到这里）
        String userId = TOKEN_STORE.get(token);
        if (userId != null) {
            return userId;
        }

        // 内存中没有，尝试从 Redis 获取
        if (redisAvailable && redisTemplate != null) {
            try {
                userId = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + token);
                if (userId != null) {
                    return userId;
                }
            } catch (Exception e) {
                log.warn("Redis 连接失败: {}", e.getMessage());
                redisAvailable = false;
            }
        }

        return null;
    }

    /**
     * 存储 Token（开发模式下使用）
     *
     * @param token  Token
     * @param userId 用户ID
     */
    public static void storeToken(String token, String userId) {
        TOKEN_STORE.put(token, userId);
    }

    /**
     * 移除 Token
     *
     * @param token Token
     */
    public static void removeToken(String token) {
        TOKEN_STORE.remove(token);
    }

    /**
     * 后置处理：清理 ThreadLocal
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param handler  处理器
     * @param ex       异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理 ThreadLocal，防止内存泄漏
        UserContext.clear();
    }

}