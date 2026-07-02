package com.food.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.food.dto.UserAddDTO;
import com.food.dto.UserPageDTO;
import com.food.dto.UserResetPasswordDTO;
import com.food.dto.UserUpdateDTO;
import com.food.vo.UserDetailVO;
import com.food.vo.UserListVO;

/**
 * 用户服务接口
 * <p>
 * 提供用户增删改查、状态变更、密码重置等业务能力
 * 所有敏感字段在返回 VO 时自动脱敏
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
public interface UserService {

    /**
     * 分页查询用户列表
     * <p>
     * 支持关键词搜索（用户名/昵称/手机号）和状态筛选
     * 返回结果中手机号、邮箱等敏感字段已脱敏
     *
     * @param dto 分页查询参数
     * @return 分页用户列表
     */
    Page<UserListVO> pageUserList(UserPageDTO dto);

    /**
     * 获取用户详情
     * <p>
     * 返回结果中手机号、邮箱、登录IP等敏感字段已脱敏
     *
     * @param id 用户ID
     * @return 用户详情
     * @throws com.food.common.exception.BusinessException 用户不存在时抛出
     */
    UserDetailVO getUserDetail(Long id);

    /**
     * 添加用户
     * <p>
     * 校验用户名和手机号唯一性，密码使用 BCrypt 加密存储
     * 默认状态为启用，默认性别为未知
     *
     * @param dto 用户添加请求
     * @return 用户ID
     * @throws com.food.common.exception.BusinessException 用户名/手机号重复时抛出
     */
    Long addUser(UserAddDTO dto);

    /**
     * 更新用户信息
     * <p>
     * 仅更新非空字段，校验手机号唯一性（排除自身）
     * 普通管理员无法修改超级管理员信息
     *
     * @param dto 用户更新请求
     * @return 是否成功
     * @throws com.food.common.exception.BusinessException 用户不存在/手机号重复/越权时抛出
     */
    Boolean updateUser(UserUpdateDTO dto);

    /**
     * 删除用户（逻辑删除）
     * <p>
     * 禁止删除超级管理员（ID=1）
     *
     * @param id 用户ID
     * @return 是否成功
     * @throws com.food.common.exception.BusinessException 用户不存在/不能删除管理员时抛出
     */
    Boolean deleteUser(Long id);

    /**
     * 更新用户状态（启用/禁用）
     * <p>
     * 禁止禁用超级管理员（ID=1）
     * 状态未变化时直接返回成功
     *
     * @param id     用户ID
     * @param status 目标状态：0禁用 1启用
     * @return 是否成功
     * @throws com.food.common.exception.BusinessException 用户不存在/不能禁用管理员/状态不合法时抛出
     */
    Boolean updateUserStatus(Long id, Integer status);

    /**
     * 管理员重置用户密码
     * <p>
     * 新密码使用 BCrypt 加密存储
     * 普通管理员无法重置超级管理员密码
     *
     * @param id  用户ID
     * @param dto 重置密码请求
     * @return 是否成功
     * @throws com.food.common.exception.BusinessException 用户不存在/越权时抛出
     */
    Boolean resetPassword(Long id, UserResetPasswordDTO dto);

}
