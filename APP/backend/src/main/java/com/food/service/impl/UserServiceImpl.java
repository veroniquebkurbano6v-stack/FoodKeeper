package com.food.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.food.common.convert.UserConvertor;
import com.food.common.exception.BusinessException;
import com.food.common.result.ErrorCode;
import com.food.common.util.UserContext;
import com.food.dto.UserAddDTO;
import com.food.dto.UserPageDTO;
import com.food.dto.UserResetPasswordDTO;
import com.food.dto.UserUpdateDTO;
import com.food.entity.UserDO;
import com.food.mapper.UserMapper;
import com.food.service.UserService;
import com.food.vo.UserDetailVO;
import com.food.vo.UserListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 用户服务实现类
 * <p>
 * 提供用户增删改查、状态变更、密码重置等业务逻辑
 * 所有写操作添加事务控制，确保数据一致性
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 超级管理员用户ID
     */
    private static final Long SUPER_ADMIN_ID = 1L;

    /**
     * 最大分页条数
     */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 用户状态：禁用
     */
    private static final int STATUS_DISABLED = 0;

    /**
     * 用户状态：启用
     */
    private static final int STATUS_ENABLED = 1;

    @Override
    public Page<UserListVO> pageUserList(UserPageDTO dto) {
        log.debug("分页查询用户列表，操作人={}, pageNum={}, pageSize={}, keyword={}, status={}",
                UserContext.getUserId(), dto.getPageNum(), dto.getPageSize(), dto.getKeyword(), dto.getStatus());

        // 校验分页参数，防止越权获取大量数据
        int pageNum = dto.getPageNum() != null && dto.getPageNum() > 0 ? dto.getPageNum() : 1;
        int pageSize = dto.getPageSize() != null && dto.getPageSize() > 0
                ? Math.min(dto.getPageSize(), MAX_PAGE_SIZE) : 10;

        // 构建分页参数
        Page<UserDO> page = new Page<>(pageNum, pageSize);

        // 执行分页查询
        IPage<UserDO> result = userMapper.selectUserPage(page, dto.getKeyword(), dto.getStatus());

        // 转换为 VO（敏感字段脱敏）
        Page<UserListVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(UserConvertor::toListVO)
                .toList());

        return voPage;
    }

    @Override
    public UserDetailVO getUserDetail(Long id) {
        log.debug("获取用户详情，操作人={}, 用户ID={}", UserContext.getUserId(), id);

        // 参数校验
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不合法");
        }

        // 查询用户
        UserDO user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 转换为 VO（敏感字段脱敏）
        return UserConvertor.toDetailVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addUser(UserAddDTO dto) {
        log.info("添加用户，操作人={}, 用户名={}", UserContext.getUserId(), dto.getUsername());

        // 校验用户名唯一性
        UserDO existByUsername = userMapper.selectByUsername(dto.getUsername());
        if (existByUsername != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // 校验手机号唯一性（手机号非空时校验）
        if (StringUtils.hasText(dto.getPhone())) {
            UserDO existByPhone = userMapper.selectByPhone(dto.getPhone());
            if (existByPhone != null) {
                throw new BusinessException(ErrorCode.PHONE_EXISTS);
            }
        }

        // DTO 转 DO（密码加密在 Convertor 中完成）
        UserDO user = UserConvertor.toDO(dto, passwordEncoder);

        // 保存用户
        int rows = userMapper.insert(user);
        if (rows <= 0) {
            log.error("用户添加失败，用户名={}", dto.getUsername());
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "用户创建失败");
        }

        log.info("用户添加成功，ID={}, 用户名={}", user.getId(), user.getUsername());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateUser(UserUpdateDTO dto) {
        log.info("更新用户信息，操作人={}, 用户ID={}", UserContext.getUserId(), dto.getId());

        // 参数校验
        if (dto.getId() == null || dto.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不合法");
        }

        // 查询用户是否存在
        UserDO user = userMapper.selectById(dto.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 越权校验：普通管理员不能修改超级管理员信息
        if (Objects.equals(user.getId(), SUPER_ADMIN_ID) && !UserContext.isSuperAdmin()) {
            log.warn("越权操作：普通管理员尝试修改超级管理员信息，操作人={}", UserContext.getUserId());
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限修改超级管理员信息");
        }

        // 校验手机号唯一性（排除自身）
        if (StringUtils.hasText(dto.getPhone())
                && !dto.getPhone().equals(user.getPhone())) {
            UserDO existByPhone = userMapper.selectByPhone(dto.getPhone());
            if (existByPhone != null && !existByPhone.getId().equals(dto.getId())) {
                throw new BusinessException(ErrorCode.PHONE_EXISTS);
            }
            user.setPhone(dto.getPhone());
        }

        // 更新非空字段
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        if (dto.getRemark() != null) {
            user.setRemark(dto.getRemark());
        }

        // 执行更新
        int rows = userMapper.updateById(user);
        if (rows <= 0) {
            log.warn("用户信息更新未生效，用户ID={}", dto.getId());
        }

        log.info("用户信息更新成功，用户ID={}", dto.getId());
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteUser(Long id) {
        log.info("删除用户，操作人={}, 用户ID={}", UserContext.getUserId(), id);

        // 参数校验
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不合法");
        }

        // 禁止删除超级管理员
        if (Objects.equals(id, SUPER_ADMIN_ID)) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_ADMIN);
        }

        // 越权校验：普通管理员不能删除其他管理员
        UserDO user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 执行逻辑删除
        int rows = userMapper.deleteById(id);
        if (rows <= 0) {
            log.warn("用户删除未生效，用户ID={}", id);
        }

        log.info("用户删除成功，用户ID={}", id);
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateUserStatus(Long id, Integer status) {
        log.info("更新用户状态，操作人={}, 用户ID={}, 目标状态={}", UserContext.getUserId(), id, status);

        // 参数校验
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不合法");
        }
        if (status == null || (status != STATUS_DISABLED && status != STATUS_ENABLED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态值不合法");
        }

        // 禁止禁用超级管理员
        if (Objects.equals(id, SUPER_ADMIN_ID) && status == STATUS_DISABLED) {
            throw new BusinessException(ErrorCode.CANNOT_DISABLE_ADMIN);
        }

        // 越权校验：普通管理员不能禁用其他管理员
        if (!UserContext.isSuperAdmin() && status == STATUS_DISABLED) {
            UserDO targetUser = userMapper.selectById(id);
            if (targetUser != null && Objects.equals(targetUser.getId(), SUPER_ADMIN_ID)) {
                log.warn("越权操作：普通管理员尝试禁用超级管理员，操作人={}", UserContext.getUserId());
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权限禁用超级管理员");
            }
        }

        // 查询用户是否存在
        UserDO user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 状态未变化，直接返回成功
        if (Objects.equals(user.getStatus(), status)) {
            log.debug("用户状态未变化，用户ID={}, 当前状态={}", id, status);
            return true;
        }

        // 更新状态
        user.setStatus(status);
        int rows = userMapper.updateById(user);
        if (rows <= 0) {
            log.warn("用户状态更新未生效，用户ID={}", id);
        }

        log.info("用户状态更新成功，用户ID={}, 状态={}", id, status);
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean resetPassword(Long id, UserResetPasswordDTO dto) {
        log.info("重置用户密码，操作人={}, 用户ID={}", UserContext.getUserId(), id);

        // 参数校验
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不合法");
        }
        if (!StringUtils.hasText(dto.getNewPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码不能为空");
        }

        // 越权校验：普通管理员不能重置超级管理员密码
        if (Objects.equals(id, SUPER_ADMIN_ID) && !UserContext.isSuperAdmin()) {
            log.warn("越权操作：普通管理员尝试重置超级管理员密码，操作人={}", UserContext.getUserId());
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限重置超级管理员密码");
        }

        // 查询用户是否存在
        UserDO user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // BCrypt 加密新密码
        String encodedPassword = passwordEncoder.encode(dto.getNewPassword());

        // 更新密码
        int rows = userMapper.updatePassword(id, encodedPassword);
        if (rows <= 0) {
            log.warn("用户密码重置未生效，用户ID={}", id);
        }

        log.info("用户密码重置成功，用户ID={}", id);
        return rows > 0;
    }
}
