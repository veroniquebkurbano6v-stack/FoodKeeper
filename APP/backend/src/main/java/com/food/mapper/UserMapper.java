package com.food.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.food.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供基础 CRUD 方法
 * 自定义查询通过 XML 映射文件实现，使用参数化查询
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

    /**
     * BaseMapper 已提供以下基础方法：
     * - insert(T entity)
     * - deleteById(Serializable id)
     * - updateById(T entity)
     * - selectById(Serializable id)
     * - selectList(Wrapper<T> queryWrapper)
     * - selectPage(IPage<T> page, Wrapper<T> queryWrapper)
     * - selectCount(Wrapper<T> queryWrapper)
     */

    /**
     * 根据用户名查询用户（含密码字段）
     * <p>
     * 用于登录认证，返回密码字段以校验
     *
     * @param username 用户名
     * @return 用户信息（含密码）
     */
    UserDO selectByUsernameWithPassword(@Param("username") String username);

    /**
     * 根据用户名查询用户（不含密码）
     * <p>
     * 用于重复校验，不返回密码字段
     *
     * @param username 用户名
     * @return 用户信息（不含密码）
     */
    UserDO selectByUsername(@Param("username") String username);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户信息
     */
    UserDO selectByPhone(@Param("phone") String phone);

    /**
     * 分页查询用户列表
     * <p>
     * 支持关键词搜索（用户名/昵称/手机号）、状态筛选
     * 不返回密码字段
     *
     * @param page     分页参数
     * @param keyword  搜索关键词（可选）
     * @param status   用户状态（可选）
     * @return 分页用户列表
     */
    IPage<UserDO> selectUserPage(
            IPage<UserDO> page,
            @Param("keyword") String keyword,
            @Param("status") Integer status
    );

    /**
     * 批量更新用户状态
     *
     * @param ids    用户ID列表
     * @param status 目标状态
     * @return 影响行数
     */
    int batchUpdateStatus(
            @Param("ids") List<Long> ids,
            @Param("status") Integer status
    );

    /**
     * 更新用户密码
     *
     * @param userId      用户ID
     * @param newPassword 新密码（加密后）
     * @return 影响行数
     */
    int updatePassword(
            @Param("userId") Long userId,
            @Param("newPassword") String newPassword
    );

    /**
     * 更新最后登录信息
     *
     * @param userId      用户ID
     * @param loginTime   登录时间
     * @param loginIp     登录IP
     * @return 影响行数
     */
    int updateLastLoginInfo(
            @Param("userId") Long userId,
            @Param("loginTime") java.time.LocalDateTime loginTime,
            @Param("loginIp") String loginIp
    );

}